package com.example.onlinemall.activities.impl;

import com.example.onlinemall.activities.CreateOrderActivities;
import com.example.onlinemall.constant.OrderStatus;
import com.example.onlinemall.constant.StockStatus;
import com.example.onlinemall.dao.BasketDao;
import com.example.onlinemall.dao.OrderDao;
import com.example.onlinemall.dao.OrderItemDao;
import com.example.onlinemall.dto.CheckoutRequestDTO;
import com.example.onlinemall.dto.ProductItem;
import com.example.onlinemall.dto.Shop;
import com.example.onlinemall.entity.Order;
import com.example.onlinemall.entity.OrderItem;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@ActivityImpl(workers = "order-service-worker")
@Slf4j
public class CreateOrderActivitiesImpl implements CreateOrderActivities {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private OrderItemDao orderItemDao;

    @Autowired
    private BasketDao cartDao;

    @Override
    @Transactional
    public void createOrder(CheckoutRequestDTO request) {
        log.info("createOrder orderId={} customerId={}",
                request.getOrderId(), request.getCustomerId());

        // 1. 幂等：orderId 已存在则视为成功（兼容 Activity 重试）
        Order existing = orderDao.selectById(request.getOrderId());
        if (existing != null) {
            log.info("createOrder idempotent hit, skip insert. orderId={}", request.getOrderId());
            return;
        }

        // 2. 创建订单 + 订单项，status=待付款
        LocalDateTime now = LocalDateTime.now();
        List<Shop> shops = request.getItems() == null ? List.of() : request.getItems();
        int productNums = shops.stream()
                .filter(shop -> shop.getProductItemList() != null)
                .flatMap(shop -> shop.getProductItemList().stream())
                .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .sum();

        Order order = Order.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId() == null ? null : String.valueOf(request.getCustomerId()))
                .status(OrderStatus.WAIT_PAY)
                .stockStatus(StockStatus.NONE)
                .productNums(productNums)
                .isPayed(0)
                .deleteStatus(0)
                .payDeadline(now.plusMinutes(30))
                .createTime(now)
                .build();
        int inserted = orderDao.insert(order);
        if (inserted == 0) {
            throw ApplicationFailure.newFailure(
                    "insert order failed, orderId=" + request.getOrderId(),
                    "OrderInsertFailed");
        }
        insertOrderItems(request.getOrderId(), shops, now);
    }

    private void insertOrderItems(Long orderId, List<Shop> shops, LocalDateTime now) {
        for (Shop shop : shops) {
            if (CollectionUtils.isEmpty(shop.getProductItemList())) {
                continue;
            }
            for (ProductItem item : shop.getProductItemList()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(orderId);
                orderItem.setProdName("mock_data");
                orderItem.setProdId(item.getProdId());
                orderItem.setSkuId(item.getSkuId());
                orderItem.setProdCount(item.getQuantity());
                orderItem.setPrice(item.getUnitPrice());
                if (item.getUnitPrice() != null && item.getQuantity() != null) {
                    orderItem.setTotalAmount(
                            item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
                orderItem.setCreateTime(now);
                int inserted = orderItemDao.insert(orderItem);
                if (inserted == 0) {
                    throw ApplicationFailure.newFailure(
                            "insert order_item failed, orderId=" + orderId
                                    + ", skuId=" + item.getSkuId(),
                            "OrderItemInsertFailed");
                }
            }
        }
    }

    @Override
    @Transactional
    public void markOrderPaid(CheckoutRequestDTO request) {
        Long orderId = request.getOrderId();
        log.info("markOrderPaid orderId={}", orderId);

        Order existing = orderDao.selectById(orderId);
        if (existing == null) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "order not found, orderId=" + orderId, "OrderNotFound");
        }
        if (existing.getStatus() != null && existing.getStatus() == OrderStatus.PAID) {
            log.info("markOrderPaid already paid, skip. orderId={}", orderId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = orderDao.markPaidByOrderId(orderId, OrderStatus.PAID, now);
        if (updated == 0) {
            throw ApplicationFailure.newFailure(
                    "update order failed, orderId=" + orderId,
                    "OrderUpdateFailed");
        }
        log.info("markOrderPaid done. orderId={} updated={}", orderId, updated);
    }

    @Override
    @Transactional
    public void markOrderCancelled(CheckoutRequestDTO request) {
        Long orderId = request.getOrderId();
        log.info("markOrderCancelled orderId={}", orderId);

        Order existing = orderDao.selectById(orderId);
        if (existing == null) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "order not found, orderId=" + orderId, "OrderNotFound");
        }
        if (existing.getStatus() != null && existing.getStatus() == OrderStatus.CANCELLED) {
            log.info("markOrderCancelled already cancelled, skip. orderId={}", orderId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = orderDao.markCancelledByOrderId(orderId, OrderStatus.CANCELLED, now);
        if (updated == 0) {
            throw ApplicationFailure.newFailure(
                    "update order failed, orderId=" + orderId,
                    "OrderUpdateFailed");
        }
        log.info("markOrderCancelled done. orderId={} updated={}", orderId, updated);
    }
}
