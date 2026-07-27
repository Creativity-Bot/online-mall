package com.example.onlinemall.dao;

import com.example.onlinemall.entity.Basket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BasketDao {

    Basket selectById(@Param("id") Long id);

    List<Basket> selectAll();

    int insert(Basket entity);

    int updateById(Basket entity);

    int deleteById(@Param("id") Long id);

    List<Basket> selectByCustomerId(@Param("id") String customerId);

}
