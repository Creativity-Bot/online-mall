package com.example.onlinemall.dao;

import com.example.onlinemall.entity.ProdTagReference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProdTagReferenceDao {

    ProdTagReference selectById(@Param("id") Long id);

    List<ProdTagReference> selectAll();

    int insert(ProdTagReference entity);

    int updateById(ProdTagReference entity);

    int deleteById(@Param("id") Long id);
}
