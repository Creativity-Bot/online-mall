package com.example.onlinemall.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProdTag {
    private Long id;
    private String title;
    private Integer status;
    private Integer style;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
