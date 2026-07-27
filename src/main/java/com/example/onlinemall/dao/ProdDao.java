package com.example.onlinemall.dao;

import com.example.onlinemall.entity.Prod;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProdDao {

    Prod selectById(@Param("id") Long id);

    List<Prod> selectAll();

    int insert(Prod entity);

    int updateById(Prod entity);

    int deleteById(@Param("id") Long id);
}
