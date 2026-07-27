package com.example.onlinemall.dao;

import com.example.onlinemall.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryDao {

    Category selectById(@Param("id") Long id);

    List<Category> selectAll();

    int insert(Category entity);

    int updateById(Category entity);

    int deleteById(@Param("id") Long id);
}
