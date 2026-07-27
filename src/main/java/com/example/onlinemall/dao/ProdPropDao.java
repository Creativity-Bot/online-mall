package com.example.onlinemall.dao;

import com.example.onlinemall.entity.ProdProp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProdPropDao {

    ProdProp selectById(@Param("id") Long id);

    List<ProdProp> selectAll();

    int insert(ProdProp entity);

    int updateById(ProdProp entity);

    int deleteById(@Param("id") Long id);
}
