package com.example.onlinemall.dao;

import com.example.onlinemall.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemDao {

    OrderItem selectById(@Param("id") Long id);

    List<OrderItem> selectAll();

    int insert(OrderItem entity);

    int updateById(OrderItem entity);

    int deleteById(@Param("id") Long id);
}
