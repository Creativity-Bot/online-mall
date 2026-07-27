package com.example.onlinemall.dao;

import com.example.onlinemall.entity.SkuStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SkuStockDao {

    SkuStock selectById(@Param("id") Long id);

    List<SkuStock> selectAll();

    int insert(SkuStock entity);

    int updateById(SkuStock entity);

    /**
     * 原子预占：available 减少、reserved 增加；库存不足时更新行数为 0。
     */
    int reserveStock(@Param("skuId") Long skuId,
                     @Param("qty") Integer qty,
                     @Param("updateTime") java.time.LocalDateTime updateTime);

    /**
     * 原子释放：available 增加、reserved 减少；预占不足时更新行数为 0。
     */
    int releaseStock(@Param("skuId") Long skuId,
                     @Param("qty") Integer qty,
                     @Param("updateTime") java.time.LocalDateTime updateTime);

    int deleteById(@Param("id") Long id);
}
