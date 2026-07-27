package com.example.onlinemall.dao;

import com.example.onlinemall.entity.StockLedger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockLedgerDao {

    StockLedger selectById(@Param("id") Long id);

    StockLedger selectByOrderNumberAndTypeAndSkuId(@Param("orderNumber") String orderNumber,
                                                    @Param("type") Integer type,
                                                    @Param("skuId") Long skuId);

    StockLedger selectByOrderNumberAndSkuId(@Param("orderNumber") String orderNumber,
                                            @Param("skuId") Long skuId);

    List<StockLedger> selectByOrderNumberAndType(@Param("orderNumber") String orderNumber,
                                                 @Param("type") Integer type);

    List<StockLedger> selectAll();

    int insert(StockLedger entity);

    int updateById(StockLedger entity);

    /**
     * 将占用流水改为释放状态；仅当当前仍为 fromType 时更新成功（影响行数 0 表示已释放或不存在）。
     */
    int updateType(@Param("id") Long id,
                   @Param("fromType") Integer fromType,
                   @Param("toType") Integer toType,
                   @Param("updateTime") java.time.LocalDateTime updateTime);

    int deleteById(@Param("id") Long id);
}
