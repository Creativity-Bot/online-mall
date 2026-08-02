package com.example.onlinemall.activities.impl;

import com.example.onlinemall.activities.ReserveStockActivities;
import com.example.onlinemall.constant.StockStatus;
import com.example.onlinemall.dao.OrderDao;
import com.example.onlinemall.dao.ProdDao;
import com.example.onlinemall.dao.SkuStockDao;
import com.example.onlinemall.entity.Order;
import com.example.onlinemall.model.checkout.Sku;
import com.example.onlinemall.model.checkout.Spu;
import com.example.onlinemall.model.checkout.StockChange;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Component
@ActivityImpl(workers = "order-service-worker")
public class ReserveStockActivitiesImpl implements ReserveStockActivities {

    private static final Logger log = LoggerFactory.getLogger(ReserveStockActivitiesImpl.class);

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private SkuStockDao skuStockDao;

    @Autowired
    private ProdDao prodDao;

    @Override
    @Transactional
    public void reserveStock(StockChange stockChange) {
        log.info("reserveStock orderId={}", stockChange.getOrderId());
        validateStockChange(stockChange);

        LocalDateTime now = LocalDateTime.now();
        String orderId = stockChange.getOrderId();

        Order order = requireOrder(orderId);
        Integer stockStatus = order.getStockStatus();
        if (stockStatus != null && stockStatus == StockStatus.RESERVED) {
            log.info("reserveStock idempotent hit, skip. orderId={} stockStatus={}",
                    orderId, stockStatus);
            return;
        }
        if (stockStatus != null && stockStatus != StockStatus.NONE) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "cannot reserve stock, orderId=" + orderId + ", stockStatus=" + stockStatus,
                    "InvalidStockStatus");
        }

        for (Sku sku : stockChange.getSkuList()) {
            validateSku(sku);
            int updated = skuStockDao.reserveStock(sku.getSkuId(), sku.getQuantity(), now);
            if (updated == 0) {
                throw ApplicationFailure.newNonRetryableFailure(
                        "insufficient stock for skuId=" + sku.getSkuId() + ", qty=" + sku.getQuantity(),
                        "InsufficientStock");
            }
        }

        int marked = orderDao.casStockStatus(orderId, StockStatus.NONE, StockStatus.RESERVED, now);
        if (marked == 0) {
            throw ApplicationFailure.newFailure(
                    "mark stock reserved failed, orderId=" + orderId,
                    "MarkStockReservedFailed");
        }
    }

    @Override
    @Transactional
    public void confirmStock(StockChange stockChange) {
        log.info("confirmStock orderId={}", stockChange.getOrderId());
        validateStockChange(stockChange);

        LocalDateTime now = LocalDateTime.now();
        String orderId = stockChange.getOrderId();

        Order order = requireOrder(orderId);
        Integer stockStatus = order.getStockStatus();
        if (stockStatus != null && stockStatus == StockStatus.CONFIRMED) {
            log.info("confirmStock idempotent hit, skip. orderId={}", orderId);
            return;
        }
        if (stockStatus == null || stockStatus != StockStatus.RESERVED) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "cannot confirm stock, orderId=" + orderId + ", stockStatus=" + stockStatus,
                    "InvalidStockStatus");
        }

        // 先按 sku 确认，再按已汇总的 spu 扣减 prod
        for (Sku sku : stockChange.getSkuList()) {
            validateSku(sku);
            int updated = skuStockDao.confirmStock(sku.getSkuId(), sku.getQuantity(), now);
            if (updated == 0) {
                throw ApplicationFailure.newFailure(
                        "confirm stock failed for skuId=" + sku.getSkuId() + ", qty=" + sku.getQuantity(),
                        "ConfirmStockFailed");
            }
        }

        for (Spu spu : stockChange.getSpuList()) {
            validateSpu(spu);
            int prodUpdated = prodDao.deductStock(spu.getProdId(), spu.getQuantity(), now);
            if (prodUpdated == 0) {
                throw ApplicationFailure.newFailure(
                        "deduct prod stock failed, prodId=" + spu.getProdId() + ", qty=" + spu.getQuantity(),
                        "DeductProdStockFailed");
            }
        }

        int marked = orderDao.casStockStatus(orderId, StockStatus.RESERVED, StockStatus.CONFIRMED, now);
        if (marked == 0) {
            throw ApplicationFailure.newFailure(
                    "mark stock confirmed failed, orderId=" + orderId,
                    "MarkStockConfirmedFailed");
        }
    }

    @Override
    @Transactional
    public void releaseStock(StockChange stockChange) {
        log.info("releaseStock orderId={}", stockChange.getOrderId());
        validateStockChange(stockChange);

        LocalDateTime now = LocalDateTime.now();
        String orderId = stockChange.getOrderId();

        Order order = requireOrder(orderId);
        Integer stockStatus = order.getStockStatus();
        if (stockStatus == StockStatus.RELEASED) {
            log.info("releaseStock nothing to release, skip. orderId={} stockStatus={}",
                    orderId, stockStatus);
            return;
        }
        if (stockStatus != StockStatus.RESERVED) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "cannot release stock, orderId=" + orderId + ", stockStatus=" + stockStatus,
                    "InvalidStockStatus");
        }

        for (Sku sku : stockChange.getSkuList()) {
            validateSku(sku);
            int updated = skuStockDao.releaseStock(sku.getSkuId(), sku.getQuantity(), now);
            if (updated == 0) {
                throw ApplicationFailure.newFailure(
                        "release stock failed for skuId=" + sku.getSkuId() + ", qty=" + sku.getQuantity(),
                        "ReleaseStockFailed");
            }
        }

        int marked = orderDao.casStockStatus(orderId, StockStatus.RESERVED, StockStatus.RELEASED, now);
        if (marked == 0) {
            throw ApplicationFailure.newFailure(
                    "mark stock released failed, orderId=" + orderId,
                    "MarkStockReleasedFailed");
        }
    }

    private void validateStockChange(StockChange stockChange) {
        if (stockChange == null || !StringUtils.hasText(stockChange.getOrderId())) {
            throw ApplicationFailure.newNonRetryableFailure("orderId is required", "InvalidStockChange");
        }
        if (CollectionUtils.isEmpty(stockChange.getSkuList())) {
            throw ApplicationFailure.newNonRetryableFailure("skuList is empty", "InvalidStockChange");
        }
    }

    private void validateSku(Sku sku) {
        if (sku.getSkuId() == null) {
            throw ApplicationFailure.newNonRetryableFailure("skuId is required", "InvalidStockChange");
        }
        if (sku.getQuantity() == null || sku.getQuantity() <= 0) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "quantity must be positive, skuId=" + sku.getSkuId(), "InvalidStockChange");
        }
    }

    private void validateSpu(Spu spu) {
        if (spu.getProdId() == null) {
            throw ApplicationFailure.newNonRetryableFailure("prodId is required", "InvalidStockChange");
        }
        if (spu.getQuantity() == null || spu.getQuantity() <= 0) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "quantity must be positive, prodId=" + spu.getProdId(), "InvalidStockChange");
        }
    }

    private Order requireOrder(String orderId) {
        Order order = orderDao.selectByOrderNumber(orderId);
        if (order == null) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "order not found, orderId=" + orderId, "OrderNotFound");
        }
        return order;
    }
}
