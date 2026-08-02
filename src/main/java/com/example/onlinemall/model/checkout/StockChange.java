package com.example.onlinemall.model.checkout;

import lombok.Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Data
public class StockChange {

    private String orderId;
    private List<Sku> skuList = new ArrayList<>();
    private List<Spu> spuList = new ArrayList<>();

    public void sort() {
        if (!skuList.isEmpty()) {
            skuList.sort(Comparator.comparing(Sku::getSkuId, Comparator.nullsLast(Long::compareTo)));
        }
        if (!spuList.isEmpty()) {
            spuList.sort(Comparator.comparing(Spu::getProdId, Comparator.nullsLast(Long::compareTo)));
        }
    }

    public void addSku(Sku sku) {
        skuList.add(sku);
    }

    public void addOrMergeSpu(Spu spu) {
        spuList.stream()
                .filter(existing -> Objects.equals(existing.getProdId(), spu.getProdId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + spu.getQuantity()),
                        () -> spuList.add(spu)
                );
    }
}
