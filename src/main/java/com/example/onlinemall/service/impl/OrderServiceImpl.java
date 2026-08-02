package com.example.onlinemall.service.impl;

import com.example.onlinemall.constant.SharedConstants;
import com.example.onlinemall.dao.OrderDao;
import com.example.onlinemall.entity.Order;
import com.example.onlinemall.exception.CheckoutWorkflowException;
import com.example.onlinemall.model.checkout.*;
import com.example.onlinemall.service.OrderService;
import com.example.onlinemall.workflow.OrderCheckoutWorkflow;
import io.temporal.api.enums.v1.WorkflowIdConflictPolicy;
import io.temporal.client.UpdateOptions;
import io.temporal.client.WithStartWorkflowOperation;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowUpdateException;
import io.temporal.client.WorkflowUpdateStage;
import io.temporal.failure.ApplicationFailure;
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

    /**
     * Update-with-Start：同步等到建单+预占完成（waitUntilPayable Update），主流程继续挂起等选支付。
     * Update-with-Start 非原子：若 Workflow 已 Start 而 Update 失败，对同一 workflowId 再发 waitUntilPayable。
     */
    @Override
    public CheckoutReady checkout(CheckoutRequest request) {
        if (!StringUtils.hasText(request.getOrderId())) {
            request.setOrderId(UUID.randomUUID().toString().replace("-", ""));
        }

        String workflowId = "order-" + request.getOrderId();
        String updateId = "waitUntilPayable-" + request.getOrderId();

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(SharedConstants.ORDER_SERVICE_TASK_QUEUE)
                .setWorkflowId(workflowId)
                .setWorkflowIdConflictPolicy(WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_FAIL)
                .build();

        OrderCheckoutWorkflow workflow = workflowClient.newWorkflowStub(OrderCheckoutWorkflow.class, options);

        try {
            return WorkflowClient.executeUpdateWithStart(
                    workflow::waitUntilPayable,
                    UpdateOptions.<CheckoutReady>newBuilder()
                            .setUpdateName("waitUntilPayable")
                            .setUpdateId(updateId)
                            .build(),
                    new WithStartWorkflowOperation<>(workflow::run, request)
            );
        } catch (Exception first) {
            // Workflow 可能已启动；对已存在执行再发同名 Update（不重新 Start）
            try {
                OrderCheckoutWorkflow existing = workflowClient.newWorkflowStub(
                        OrderCheckoutWorkflow.class, workflowId);
                return WorkflowClient.startUpdate(
                                existing::waitUntilPayable,
                                UpdateOptions.<CheckoutReady>newBuilder()
                                        .setUpdateName("waitUntilPayable")
                                        .setUpdateId(updateId)
                                        .setWaitForStage(WorkflowUpdateStage.COMPLETED)
                                        .build())
                        .getResult();
            } catch (Exception fallback) {
                throw mapCheckoutFailure(fallback, first);
            }
        }
    }

    /**
     * 选择支付方式：Workflow Update（非 Signal），阻塞直到 createPayment 完成并返回 PayInfo。
     */
    @Override
    public PayInfo selectPayment(SelectPaymentRequest request) {
        if (request == null || !StringUtils.hasText(request.getOrderId())) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (!StringUtils.hasText(request.getPayMethod())) {
            throw new IllegalArgumentException("payMethod is required");
        }

        OrderCheckoutWorkflow workflow = workflowClient.newWorkflowStub(
                OrderCheckoutWorkflow.class,
                "order-" + request.getOrderId()
        );
        try {
            return WorkflowClient.startUpdate(
                            workflow::selectPayment,
                            request,
                            UpdateOptions.<PayInfo>newBuilder()
                                    .setUpdateName("selectPayment")
                                    .setUpdateId("selectPayment-" + request.getOrderId())
                                    .setWaitForStage(WorkflowUpdateStage.COMPLETED)
                                    .build())
                    .getResult();
        } catch (Exception e) {
            throw mapCheckoutFailure(e, null);
        }
    }

    /** 支付渠道 webhook：Signal 唤醒挂起的 Workflow。 */
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

    /**
     * 优先从 fallback 异常提取 ApplicationFailure；否则用 primary。
     */
    private static CheckoutWorkflowException mapCheckoutFailure(Exception primary, Exception secondary) {
        CheckoutWorkflowException mapped = unwrapCheckoutException(primary);
        if (mapped != null) {
            return mapped;
        }
        if (secondary != null) {
            mapped = unwrapCheckoutException(secondary);
            if (mapped != null) {
                return mapped;
            }
        }
        String message = primary.getMessage() == null ? "checkout workflow failed" : primary.getMessage();
        return new CheckoutWorkflowException("CheckoutFailed", message, primary);
    }

    private static CheckoutWorkflowException unwrapCheckoutException(Throwable error) {
        Throwable cursor = error;
        while (cursor != null) {
            if (cursor instanceof WorkflowUpdateException) {
                Throwable cause = cursor.getCause();
                ApplicationFailure af = findApplicationFailure(cause);
                if (af != null) {
                    String msg = af.getOriginalMessage();
                    if (!StringUtils.hasText(msg)) {
                        msg = af.getMessage();
                    }
                    return new CheckoutWorkflowException(af.getType(), msg, error);
                }
                return new CheckoutWorkflowException(
                        "WorkflowUpdateFailed",
                        cursor.getMessage() == null ? "workflow update failed" : cursor.getMessage(),
                        error);
            }
            ApplicationFailure af = cursor instanceof ApplicationFailure
                    ? (ApplicationFailure) cursor
                    : null;
            if (af != null) {
                String msg = af.getOriginalMessage();
                if (!StringUtils.hasText(msg)) {
                    msg = af.getMessage();
                }
                return new CheckoutWorkflowException(af.getType(), msg, error);
            }
            cursor = cursor.getCause();
        }
        return null;
    }

    private static ApplicationFailure findApplicationFailure(Throwable error) {
        Throwable cursor = error;
        while (cursor != null) {
            if (cursor instanceof ApplicationFailure) {
                return (ApplicationFailure) cursor;
            }
            cursor = cursor.getCause();
        }
        return null;
    }
}
