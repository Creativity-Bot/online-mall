package com.example.onlinemall.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequestDTO {
    /** 业务侧预先生成，同时作为 Temporal WorkflowId，保证幂等 */
    private Long orderId;
    /** 前端幂等键，避免连点/重试重复下单 */
    private String idempotence;
    private Long customerId;
    /** 下单商品（按店铺分组） */
    private List<Shop> items = new ArrayList<>();
}
