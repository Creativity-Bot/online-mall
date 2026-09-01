package com.example.onlinemall.dao;

import com.example.onlinemall.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartItemDao {

    CartItem selectById(@Param("id") Long id);

    List<CartItem> selectAll();

    int insert(CartItem entity);

    int updateById(CartItem entity);

    int deleteById(@Param("id") Long id);

    List<CartItem> selectByCustomerId(@Param("id") Long customerId);

}
