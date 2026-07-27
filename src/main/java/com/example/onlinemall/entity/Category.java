package com.example.onlinemall.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Category {
    private Long categoryId;
    private Long parentId;
    private String categoryName;
    private String icon;
    private String pic;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
