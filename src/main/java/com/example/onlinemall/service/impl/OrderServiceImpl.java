package com.example.onlinemall.service.impl;

import com.example.onlinemall.constant.SharedConstants;
import com.example.onlinemall.dao.CartItemDao;
import com.example.onlinemall.dao.OrderDao;
import com.example.onlinemall.dao.SkuDao;
import com.example.onlinemall.dto.*;
import com.example.onlinemall.entity.CartItem;
import com.example.onlinemall.entity.Order;
import com.example.onlinemall.entity.Sku;
import com.example.onlinemall.service.OrderService;
import com.example.onlinemall.utils.IDGenerator;
import com.example.onlinemall.utils.UserHolder;
import com.example.onlinemall.workflow.OrderCheckoutWorkflow;
import io.temporal.api.enums.v1.WorkflowIdConflictPolicy;
import io.temporal.client.UpdateOptions;
import io.temporal.client.WithStartWorkflowOperation;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowUpdateStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.example.onlinemall.constant.RedisConstant.ORDER_ID_KEY;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private CartItemDao cartItemDao;

    @Autowired
    private SkuDao skuDao;

    @Autowired
    private IDGenerator idGenerator;

    @Autowired
    private WorkflowClient workflowClient;

    @Override
    public void createOrder(Order order) {
        orderDao.insert(order);
    }

    @Override
    public void updateOrder(Order order) {
        orderDao.updateById(order);
    }

    @Override
    public void deleteOrder(Order order) {
        orderDao.deleteById(order.getOrderId());
    }

    @Override
    public Order getOrderById(Long id) {
        return orderDao.selectById(id);
    }

    @Override
    public List<Order> getAllOrders() {
        return orderDao.selectAll();
    }

    /**
     * Update-with-Start：同步等到建单+预占完成（waitUntilPayable Update），主流程继续挂起等选支付。
     * Update-with-Start 非原子：若 Workflow 已 Start 而 Update 失败，对同一 workflowId 再发 waitUntilPayable。
     */
    @Override
    public ResponseTemplate<?> checkout(CheckoutRequestDTO request) {

        if (request == null || !StringUtils.hasText(request.getIdempotence())) {
            throw new IllegalArgumentException("idempotence is required");
        }

        Long customerId = UserHolder.getUser().getId();
        List<CartItem> cartItems = cartItemDao.selectByCustomerId(customerId);
        if (CollectionUtils.isEmpty(cartItems)) {
            return ResponseTemplate.fail("cart is empty");
        }

        Map<Long, List<CartItem>> shopMap = cartItems.stream()
                .collect(Collectors.groupingBy(CartItem::getShopId));

        List<Shop> shops = new ArrayList<>();
        for (Map.Entry<Long, List<CartItem>> entry : shopMap.entrySet()) {
            List<ProductItem> productItems = entry.getValue().stream()
                    .map(this::toProductItem)
                    .collect(Collectors.toList());
            shops.add(Shop.builder()
                    .shopId(entry.getKey())
                    .productItemList(productItems)
                    .build());
        }

        request.setCustomerId(customerId);
        request.setItems(shops);

        long l = idGenerator.nextId(ORDER_ID_KEY);
        request.setOrderId(l);
        String idempotence = request.getIdempotence();
        String workflowId = "order-" + idempotence;
        String updateId = "waitUntilPayable-" + idempotence;

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(SharedConstants.ORDER_SERVICE_TASK_QUEUE)
                .setWorkflowId(workflowId)
                .setWorkflowIdConflictPolicy(WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_FAIL)
                .build();

        OrderCheckoutWorkflow workflow = workflowClient.newWorkflowStub(OrderCheckoutWorkflow.class, options);

        try {
            // early return mode to get orderId in the middle
            return WorkflowClient.startUpdateWithStart(
                            () -> workflow.waitUntilPayable(request),
                            UpdateOptions.<ResponseTemplate<?>>newBuilder()
                                    .setUpdateName("waitUntilPayable")
                                    .setUpdateId(updateId)
                                    .setWaitForStage(WorkflowUpdateStage.COMPLETED)
                                    .build(),
                            new WithStartWorkflowOperation<>(workflow::run, request))
                    .getResult(10L, TimeUnit.SECONDS);
        } catch (RuntimeException exception) {
            log.error("checkout failed due to {}", exception.getMessage());
            return ResponseTemplate.fail(exception.getMessage());
        }
    }

    /**
     * 选择支付方式：Workflow Update，阻塞直到 createPayment 完成并返回 PayInfo。
     */
    @Override
    public PayInfo pay(SelectPaymentRequest request) {
        if (request == null || !StringUtils.hasText(request.getOrderId())) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (!StringUtils.hasText(request.getPayMethod())) {
            throw new IllegalArgumentException("payMethod is required");
        }
        // call payment service with orderId and other required information
        // it will return prepaid information back to client
        return new PayInfo();
    }

    /**
     * 支付渠道 webhook：Signal 唤醒挂起的 Workflow。
     */
    @Override
    public void handlePaymentCallback(PaymentCallback callback) {
        if (callback == null || !StringUtils.hasText(callback.getOrderId())) {
            throw new IllegalArgumentException("orderId is required");
        }
        OrderCheckoutWorkflow workflow = workflowClient.newWorkflowStub(
                OrderCheckoutWorkflow.class,
                "order-" + callback.getOrderId()
        );
        workflow.paymentCompleted(callback);
    }

    private ProductItem toProductItem(CartItem cartItem) {
        Sku sku = skuDao.selectById(cartItem.getSkuId());
        if (sku == null) {
            throw new IllegalArgumentException("sku not found, skuId=" + cartItem.getSkuId());
        }
        return ProductItem.builder()
                .skuId(cartItem.getSkuId())
                .prodId(cartItem.getProdId())
                .quantity(cartItem.getProdCount())
                .unitPrice(sku.getCurrentPrice())
                .build();
    }
}

