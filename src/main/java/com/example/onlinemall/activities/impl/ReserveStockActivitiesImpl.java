package com.example.onlinemall.activities.impl;

import com.example.onlinemall.activities.ReserveStockActivities;
import com.example.onlinemall.dao.SkuStockDao;
import com.example.onlinemall.dao.StockLedgerDao;
import com.example.onlinemall.entity.StockLedger;
import com.example.onlinemall.model.checkout.CartItemSnapshot;
import com.example.onlinemall.model.checkout.CheckoutRequest;
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
import java.util.List;

@Component
@ActivityImpl(workers = "order-service-worker")
public class ReserveStockActivitiesImpl implements ReserveStockActivities {

    private static final Logger log = LoggerFactory.getLogger(ReserveStockActivitiesImpl.class);

    /** 预占库存 */
    private static final int LEDGER_TYPE_RESERVE = 1;

    /** 释放库存 */
    private static final int LEDGER_TYPE_RELEASE = 0;

    @Autowired
    private StockLedgerDao stockLedgerDao;

    @Autowired
    private SkuStockDao skuStockDao;

    @Override
    @Transactional
    public void reserveStock(CheckoutRequest request) {
        log.info("reserveStock orderId={}", request.getOrderId());

        if (!StringUtils.hasText(request.getOrderId())) {
            throw ApplicationFailure.newNonRetryableFailure("orderId is required", "InvalidCheckoutRequest");
        }
        if (CollectionUtils.isEmpty(request.getItems())) {
            throw ApplicationFailure.newNonRetryableFailure("items is empty", "InvalidCheckoutRequest");
        }

        LocalDateTime now = LocalDateTime.now();
        for (CartItemSnapshot item : request.getItems()) {
            reserveOneSku(request.getOrderId(), item, now);
        }
    }

    private void reserveOneSku(String orderNumber, CartItemSnapshot item, LocalDateTime now) {
        if (item.getSkuId() == null) {
            throw ApplicationFailure.newNonRetryableFailure("skuId is required", "InvalidCheckoutRequest");
        }
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "quantity must be positive, skuId=" + item.getSkuId(), "InvalidCheckoutRequest");
        }

        // 1. 幂等：同单同 SKU 已有流水则跳过（一行记录，type 表示占用/释放状态）
        StockLedger existing = stockLedgerDao.selectByOrderNumberAndSkuId(orderNumber, item.getSkuId());
        if (existing != null) {
            log.info("reserveStock idempotent hit, skip. orderNumber={} skuId={} type={}",
                    orderNumber, item.getSkuId(), existing.getType());
            return;
        }

        // 2. 先原子更新 sku_stock，再写 stock_ledger
        int updated = skuStockDao.reserveStock(item.getSkuId(), item.getQuantity(), now);
        if (updated == 0) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "insufficient stock for skuId=" + item.getSkuId() + ", qty=" + item.getQuantity(),
                    "InsufficientStock");
        }

        StockLedger ledger = StockLedger.builder()
                .orderNumber(orderNumber)
                .skuId(item.getSkuId())
                .qty(item.getQuantity())
                .type(LEDGER_TYPE_RESERVE)
                .createTime(now)
                .updateTime(now)
                .build();
        int inserted = stockLedgerDao.insert(ledger);
        if (inserted == 0) {
            throw ApplicationFailure.newFailure(
                    "insert stock_ledger failed, orderNumber=" + orderNumber + ", skuId=" + item.getSkuId(),
                    "StockLedgerInsertFailed");
        }
    }

    @Override
    public void confirmStock(String orderId) {
        log.info("confirmStock orderId={}", orderId);
    }

    @Override
    @Transactional
    public void releaseStock(String orderId) {
        log.info("releaseStock orderId={}", orderId);

        if (!StringUtils.hasText(orderId)) {
            throw ApplicationFailure.newNonRetryableFailure("orderId is required", "InvalidCheckoutRequest");
        }

        // 按该订单的预占流水逐 SKU 释放（无预占记录则无需释放）
        List<StockLedger> reservedLedgers =
                stockLedgerDao.selectByOrderNumberAndType(orderId, LEDGER_TYPE_RESERVE);
        if (CollectionUtils.isEmpty(reservedLedgers)) {
            log.info("releaseStock no reserve ledger, skip. orderId={}", orderId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (StockLedger reserved : reservedLedgers) {
            releaseOneSku(orderId, reserved, now);
        }
    }

    private void releaseOneSku(String orderNumber, StockLedger reserved, LocalDateTime now) {
        Long skuId = reserved.getSkuId();
        Integer qty = reserved.getQty();

        // 1. 先原子回滚 sku_stock
        int updated = skuStockDao.releaseStock(skuId, qty, now);
        if (updated == 0) {
            throw ApplicationFailure.newFailure(
                    "release stock failed for skuId=" + skuId + ", qty=" + qty,
                    "ReleaseStockFailed");
        }

        // 2. 再改同一条流水状态：占用(1)→释放(0)
        //    marked==0 说明状态已不是占用（异常/并发），抛错让事务回滚已改的库存
        int marked = stockLedgerDao.updateType(
                reserved.getId(), LEDGER_TYPE_RESERVE, LEDGER_TYPE_RELEASE, now);
        if (marked == 0) {
            throw ApplicationFailure.newFailure(
                    "release ledger mark failed, orderNumber=" + orderNumber + ", skuId=" + skuId,
                    "ReleaseLedgerMarkFailed");
        }
    }
}
