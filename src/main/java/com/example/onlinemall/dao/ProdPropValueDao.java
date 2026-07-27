package com.example.onlinemall.dao;

import com.example.onlinemall.entity.ProdPropValue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProdPropValueDao {

    ProdPropValue selectById(@Param("id") Long id);

    List<ProdPropValue> selectAll();

    int insert(ProdPropValue entity);

    int updateById(ProdPropValue entity);

    int deleteById(@Param("id") Long id);
}
