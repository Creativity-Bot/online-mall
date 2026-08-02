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

    /**
     * 支付确认后扣减商品总库存并增加销量；库存不足时更新行数为 0。
     */
    int deductStock(@Param("prodId") Long prodId,
                    @Param("qty") Integer qty,
                    @Param("updateTime") java.time.LocalDateTime updateTime);

    int deleteById(@Param("id") Long id);
}
