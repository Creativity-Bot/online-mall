package com.example.onlinemall.dao;

import com.example.onlinemall.entity.OrderSettlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderSettlementDao {

    OrderSettlement selectById(@Param("id") Long id);

    List<OrderSettlement> selectAll();

    int insert(OrderSettlement entity);

    int updateById(OrderSettlement entity);

    int deleteById(@Param("id") Long id);
}
