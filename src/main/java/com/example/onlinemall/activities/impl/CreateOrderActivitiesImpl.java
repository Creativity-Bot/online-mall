package com.example.onlinemall.activities.impl;

import com.example.onlinemall.activities.CreateOrderActivities;
import com.example.onlinemall.constant.OrderStatus;
import com.example.onlinemall.constant.StockStatus;
import com.example.onlinemall.dao.OrderDao;
import com.example.onlinemall.dao.OrderItemDao;
import com.example.onlinemall.entity.Order;
import com.example.onlinemall.entity.OrderItem;
import com.example.onlinemall.dto.CheckoutRequest;
import com.example.onlinemall.dto.ProductItem;
import com.example.onlinemall.dto.Shop;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@ActivityImpl(workers = "order-service-worker")
public class CreateOrderActivitiesImpl implements CreateOrderActivities {

    private static final Logger log = LoggerFactory.getLogger(CreateOrderActivitiesImpl.class);

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private OrderItemDao orderItemDao;

    @Override
    @Transactional
    public void createOrder(CheckoutRequest request) {
        log.info("createOrder orderId={} customerId={} items={}",
                request.getOrderId(), request.getCustomerId(),
                request.getItems() == null ? 0 : request.getItems().size());

        if (!StringUtils.hasText(request.getOrderId())) {
            throw ApplicationFailure.newNonRetryableFailure("orderId is required", "InvalidCheckoutRequest");
        }
        if (!StringUtils.hasText(request.getCustomerId())) {
            throw ApplicationFailure.newNonRetryableFailure("customerId is required", "InvalidCheckoutRequest");
        }
        if (CollectionUtils.isEmpty(request.getItems())) {
            throw ApplicationFailure.newNonRetryableFailure("items is empty", "InvalidCheckoutRequest");
        }

        // 1. 幂等：orderId 作为 order_number；已存在则视为成功（兼容 Activity 重试）
        Order existing = orderDao.selectByOrderNumber(request.getOrderId());
        if (existing != null) {
            log.info("createOrder idempotent hit, skip insert. orderNumber={}", request.getOrderId());
            return;
        }

        // 2. 创建订单 + 订单项，status=1 待付款
        LocalDateTime now = LocalDateTime.now();
        int productNums = request.getItems().stream()
                .filter(shop -> shop.getProductItemList() != null)
                .flatMap(shop -> shop.getProductItemList().stream())
                .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .sum();

        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .orderNumber(request.getOrderId())
                //.totalAmount(request.getTotalAmount())
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
                    "insert order failed, orderNumber=" + request.getOrderId(),
                    "OrderInsertFailed");
        }
        insertOrderItems(request.getOrderId(), request.getItems(), now);
    }

    private void insertOrderItems(String orderNumber, List<Shop> shops, LocalDateTime now) {
        for (Shop shop : shops) {
            if (CollectionUtils.isEmpty(shop.getProductItemList())) {
                continue;
            }
            for (ProductItem item : shop.getProductItemList()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderNumber(orderNumber);
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
                            "insert order_item failed, orderNumber=" + orderNumber
                                    + ", skuId=" + item.getSkuId(),
                            "OrderItemInsertFailed");
                }
            }
        }
    }

    @Override
    public void clearCart(String customerId) {
        log.info("clearCart customerId={}", customerId);
    }

    @Override
    @Transactional
    public void markOrderPaid(String orderId) {
        log.info("markOrderPaid orderId={}", orderId);
        if (!StringUtils.hasText(orderId)) {
            throw ApplicationFailure.newNonRetryableFailure("orderId is required", "InvalidOrderId");
        }

        // 1. 幂等：不存在则直接返回
        Order existing = orderDao.selectByOrderNumber(orderId);
        if (existing.getStatus() != null && existing.getStatus() == OrderStatus.PAID) {
            log.info("markOrderPaid already paid, skip. orderNumber={}", orderId);
            return;
        }

        // 2. 未支付则更新为已支付
        LocalDateTime now = LocalDateTime.now();
        int updated = orderDao.markPaidByOrderNumber(orderId, OrderStatus.PAID, now);
        if(updated == 0){
            throw ApplicationFailure.newFailure(
                    "update order failed, orderNumber=" + orderId,
                    "OrderUpdateFailed");
        }
        log.info("markOrderPaid done. orderNumber={} updated={}", orderId, updated);
    }

    @Override
    @Transactional
    public void markOrderCancelled(String orderId) {
        log.info("markOrderCancelled orderId={}", orderId);
        if (!StringUtils.hasText(orderId)) {
            throw ApplicationFailure.newNonRetryableFailure("orderId is required", "InvalidOrderId");
        }

        // 1. 幂等：不存在则直接返回
        Order existing = orderDao.selectByOrderNumber(orderId);
        if (existing.getStatus() != null && existing.getStatus() == OrderStatus.CANCELLED) {
            log.info("markOrderCancelled already cancelled, skip. orderNumber={}", orderId);
            return;
        }

        // 2. 未取消则更新为已取消
        LocalDateTime now = LocalDateTime.now();
        int updated = orderDao.markCancelledByOrderNumber(orderId, OrderStatus.CANCELLED, now);
        if(updated == 0){
            throw ApplicationFailure.newFailure(
                    "update order failed, orderNumber=" + orderId,
                    "OrderUpdateFailed");
        }
        log.info("markOrderCancelled done. orderNumber={} updated={}", orderId, updated);
    }
}
