package com.example.onlinemall.constant;

/**
 * 订单 stock_status 字段取值（与订单 status 分离）。
 */
public interface StockStatus {

    /** 未预占 */
    int NONE = 0;

    /** 已预占 */
    int RESERVED = 1;

    /** 已确认扣减 */
    int CONFIRMED = 2;

    /** 已释放 */
    int RELEASED = 3;
}
