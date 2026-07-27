package com.example.onlinemall.dao;

import com.example.onlinemall.entity.ProdTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProdTagDao {

    ProdTag selectById(@Param("id") Long id);

    List<ProdTag> selectAll();

    int insert(ProdTag entity);

    int updateById(ProdTag entity);

    int deleteById(@Param("id") Long id);
}
