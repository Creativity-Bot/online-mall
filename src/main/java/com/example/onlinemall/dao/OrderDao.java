package com.example.onlinemall.dao;

import com.example.onlinemall.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderDao {

    Order selectById(@Param("id") Long id);

    Order selectByOrderNumber(@Param("orderNumber") String orderNumber);

    List<Order> selectAll();

    int insert(Order entity);

    int updateById(Order entity);

    /** 仅更新取消相关字段，避免全量 update 覆盖其它列 */
    int markCancelledByOrderNumber(@Param("orderNumber") String orderNumber,
                                   @Param("status") Integer status,
                                   @Param("cancelTime") LocalDateTime cancelTime);

    int deleteById(@Param("id") Long id);

    int markWaitToPackByOrderNumber(@Param("orderNumber")String orderId,
                                    @Param("status") int status,
                                    @Param("payTime") LocalDateTime now);
}
