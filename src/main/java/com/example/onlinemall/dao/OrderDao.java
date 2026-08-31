package com.example.onlinemall.dao;

import com.example.onlinemall.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderDao {

    Order selectById(@Param("id") Long id);

    List<Order> selectAll();

    int insert(Order entity);

    int updateById(Order entity);

    /** 仅更新取消相关字段，避免全量 update 覆盖其它列 */
    int markCancelledByOrderId(@Param("orderId") Long orderId,
                               @Param("fromStatus") int fromStatus,
                               @Param("toStatus") int toStatus,
                               @Param("cancelTime") LocalDateTime cancelTime);

    int deleteById(@Param("id") Long id);

    int markPaidByOrderId(@Param("orderId") Long orderId,
                          @Param("fromStatus") int fromStatus,
                          @Param("toStatus") int toStatus,
                          @Param("payTime") LocalDateTime now);

    /** CAS 更新库存状态：仅当当前为 fromStatus 时改为 toStatus */
    int casStockStatus(@Param("orderId") Long orderId,
                       @Param("fromStatus") int fromStatus,
                       @Param("toStatus") int toStatus,
                       @Param("updateTime") LocalDateTime updateTime);
}
