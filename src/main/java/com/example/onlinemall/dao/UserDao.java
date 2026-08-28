package com.example.onlinemall.dao;

import com.example.onlinemall.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserDao {

    User selectById(@Param("id") Long id);

    User selectByPhone(@Param("phone") String phone);

    List<User> selectAll();

    int insert(User entity);

    int updateById(User entity);

    int deleteById(@Param("id") Long id);
}
