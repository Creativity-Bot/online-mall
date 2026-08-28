package com.example.onlinemall.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    /** 业务侧预先生成，同时作为 Temporal WorkflowId，保证幂等 */
    private String orderId;
    /** 前端幂等键，避免连点/重试重复下单 */
    private String idempotence;
    /** todo: 正式环境从 JWT 取，学习阶段可先由请求传入 */
    private String customerId;
    /** 下单瞬间的购物车快照，避免流程中途购物车被改 */
    private List<Shop> items = new ArrayList<>();
}
