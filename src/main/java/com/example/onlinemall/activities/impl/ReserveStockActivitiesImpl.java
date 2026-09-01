package com.example.onlinemall.activities.impl;

import com.example.onlinemall.activities.ReserveStockActivities;
import com.example.onlinemall.constant.StockStatus;
import com.example.onlinemall.dao.OrderDao;
import com.example.onlinemall.dao.ProdDao;
import com.example.onlinemall.dao.SkuStockDao;
import com.example.onlinemall.dto.CheckoutRequestDTO;
import com.example.onlinemall.dto.ProductItem;
import com.example.onlinemall.dto.Shop;
import com.example.onlinemall.entity.Order;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public void reserveStock(CheckoutRequestDTO request) {
        log.info("reserveStock orderId={}", request.getOrderId());
        validateRequest(request);

        LocalDateTime now = LocalDateTime.now();
        Long orderId = request.getOrderId();

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

        for (ProductItem item : flattenItems(request)) {
            validateItem(item);
            int updated = skuStockDao.reserveStock(item.getSkuId(), item.getQuantity(), now);
            if (updated == 0) {
                throw ApplicationFailure.newNonRetryableFailure(
                        "insufficient stock for skuId=" + item.getSkuId() + ", qty=" + item.getQuantity(),
                        "InsufficientStock");
            }
        }

        int marked = orderDao.updateStockStatus(orderId, StockStatus.NONE, StockStatus.RESERVED, now);
        if (marked == 0) {
            throw ApplicationFailure.newFailure(
                    "mark stock reserved failed, orderId=" + orderId,
                    "MarkStockReservedFailed");
        }
    }

    @Override
    @Transactional
    public void confirmStock(CheckoutRequestDTO request) {
        log.info("confirmStock orderId={}", request.getOrderId());
        validateRequest(request);

        LocalDateTime now = LocalDateTime.now();
        Long orderId = request.getOrderId();

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

        Map<Long, Integer> prodQty = new LinkedHashMap<>();
        for (ProductItem item : flattenItems(request)) {
            validateItem(item);
            int updated = skuStockDao.confirmStock(item.getSkuId(), item.getQuantity(), now);
            if (updated == 0) {
                throw ApplicationFailure.newFailure(
                        "confirm stock failed for skuId=" + item.getSkuId() + ", qty=" + item.getQuantity(),
                        "ConfirmStockFailed");
            }
            if (item.getProdId() != null) {
                prodQty.merge(item.getProdId(), item.getQuantity(), Integer::sum);
            }
        }

        for (Map.Entry<Long, Integer> e : prodQty.entrySet()) {
            int prodUpdated = prodDao.deductStock(e.getKey(), e.getValue(), now);
            if (prodUpdated == 0) {
                throw ApplicationFailure.newFailure(
                        "deduct prod stock failed, prodId=" + e.getKey() + ", qty=" + e.getValue(),
                        "DeductProdStockFailed");
            }
        }

        int marked = orderDao.updateStockStatus(orderId, StockStatus.RESERVED, StockStatus.CONFIRMED, now);
        if (marked == 0) {
            throw ApplicationFailure.newFailure(
                    "mark stock confirmed failed, orderId=" + orderId,
                    "MarkStockConfirmedFailed");
        }
    }

    @Override
    @Transactional
    public void releaseStock(CheckoutRequestDTO request) {
        log.info("releaseStock orderId={}", request.getOrderId());
        validateRequest(request);

        LocalDateTime now = LocalDateTime.now();
        Long orderId = request.getOrderId();

        Order order = requireOrder(orderId);
        Integer stockStatus = order.getStockStatus();
        if (stockStatus != null && stockStatus == StockStatus.RELEASED) {
            log.info("releaseStock nothing to release, skip. orderId={} stockStatus={}",
                    orderId, stockStatus);
            return;
        }
        if (stockStatus == null || stockStatus != StockStatus.RESERVED) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "cannot release stock, orderId=" + orderId + ", stockStatus=" + stockStatus,
                    "InvalidStockStatus");
        }

        for (ProductItem item : flattenItems(request)) {
            validateItem(item);
            int updated = skuStockDao.releaseStock(item.getSkuId(), item.getQuantity(), now);
            if (updated == 0) {
                throw ApplicationFailure.newFailure(
                        "release stock failed for skuId=" + item.getSkuId() + ", qty=" + item.getQuantity(),
                        "ReleaseStockFailed");
            }
        }

        int marked = orderDao.updateStockStatus(orderId, StockStatus.RESERVED, StockStatus.RELEASED, now);
        if (marked == 0) {
            throw ApplicationFailure.newFailure(
                    "mark stock released failed, orderId=" + orderId,
                    "MarkStockReleasedFailed");
        }
    }

    private void validateRequest(CheckoutRequestDTO request) {
        if (CollectionUtils.isEmpty(flattenItems(request))) {
            throw ApplicationFailure.newNonRetryableFailure("items is empty", "InvalidStockChange");
        }
    }

    private List<ProductItem> flattenItems(CheckoutRequestDTO request) {
        List<ProductItem> items = new ArrayList<>();
        if (request == null || CollectionUtils.isEmpty(request.getItems())) {
            return items;
        }
        for (Shop shop : request.getItems()) {
            if (shop == null || CollectionUtils.isEmpty(shop.getProductItemList())) {
                continue;
            }
            items.addAll(shop.getProductItemList());
        }
        return items;
    }

    private void validateItem(ProductItem item) {
        if (item.getSkuId() == null) {
            throw ApplicationFailure.newNonRetryableFailure("skuId is required", "InvalidStockChange");
        }
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "quantity must be positive, skuId=" + item.getSkuId(), "InvalidStockChange");
        }
    }

    private Order requireOrder(Long orderId) {
        Order order = orderDao.selectById(orderId);
        if (order == null) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "order not found, orderId=" + orderId, "OrderNotFound");
        }
        return order;
    }
}
