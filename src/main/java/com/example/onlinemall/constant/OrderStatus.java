package com.example.onlinemall.constant;

/**
 * 订单 status 字段取值。
 */
public interface OrderStatus {

    /** 待付款 */
    int WAIT_PAY = 1;

    int PAID = 2;

    /** 待发货 */
    int WAIT_SHIP = 3;

    /** 待收货 */
    int WAIT_RECEIVE = 4;

    /** 待评价 */
    int WAIT_EVALUATE = 5;

    /** 成功 */
    int SUCCESS = 6;

    /** 失败/关闭 */
    int FAILED = 7;
}
