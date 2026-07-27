package com.example.onlinemall.service.impl;

import com.example.onlinemall.constant.SharedConstants;
import com.example.onlinemall.dao.OrderDao;
import com.example.onlinemall.entity.Order;
import com.example.onlinemall.model.checkout.CheckoutRequest;
import com.example.onlinemall.model.checkout.PayInfo;
import com.example.onlinemall.model.checkout.PaymentCallback;
import com.example.onlinemall.service.OrderService;
import com.example.onlinemall.workflow.OrderCheckoutWorkflow;
import io.temporal.api.enums.v1.WorkflowIdConflictPolicy;
import io.temporal.client.UpdateOptions;
import io.temporal.client.WithStartWorkflowOperation;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderDao orderDao;

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

    @Override
    public PayInfo checkout(CheckoutRequest request) {
        if (!StringUtils.hasText(request.getOrderId())) {
            request.setOrderId(UUID.randomUUID().toString().replace("-", ""));
        }

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(SharedConstants.ORDER_SERVICE_TASK_QUEUE)
                .setWorkflowId("order-" + request.getOrderId())
                .setWorkflowIdConflictPolicy(WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_FAIL)
                .build();

        OrderCheckoutWorkflow workflow = workflowClient.newWorkflowStub(OrderCheckoutWorkflow.class, options);

        // 非原子：Workflow 可能已 Start 而 Update 失败；失败时应用 orderId 查库/Query 兜底
        return WorkflowClient.executeUpdateWithStart(
                workflow::waitUntilPayable,
                UpdateOptions.<PayInfo>newBuilder()
                        .setUpdateName("waitUntilPayable")
                        .setUpdateId("waitUntilPayable-" + request.getOrderId())
                        .build(),
                new WithStartWorkflowOperation<>(workflow::run, request)
        );
    }

    @Override
    public void handlePaymentCallback(PaymentCallback callback) {
        OrderCheckoutWorkflow workflow = workflowClient.newWorkflowStub(
                OrderCheckoutWorkflow.class,
                "order-" + callback.getOrderId()
        );
        workflow.paymentCompleted(callback);
    }
}
