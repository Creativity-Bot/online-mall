package com.example.onlinemall.dao;

import com.example.onlinemall.entity.Sku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SkuDao {

    Sku selectById(@Param("id") Long id);

    List<Sku> selectAll();

    int insert(Sku entity);

    int updateById(Sku entity);

    int deleteById(@Param("id") Long id);
}
