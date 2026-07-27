package com.example.onlinemall.dao;

import com.example.onlinemall.entity.ProdComm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProdCommDao {

    ProdComm selectById(@Param("id") Long id);

    List<ProdComm> selectAll();

    int insert(ProdComm entity);

    int updateById(ProdComm entity);

    int deleteById(@Param("id") Long id);
}
