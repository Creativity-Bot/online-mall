package com.example.onlinemall.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProdTagReference {
    private Long referenceId;
    private Long shopId;
    private Long tagId;
    private Long prodId;
    private Integer status;
    private LocalDateTime createTime;
}
