package com.example.onlinemall.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProdComm {
    private Long prodCommId;
    private Long prodId;
    private String prodName;
    private Long orderItemId;
    private String customerId;
    private String content;
    private String replyContent;
    private LocalDateTime createTime;
    private LocalDateTime replyTime;
    private Integer replySts;
    private String postip;
    private Integer score;
    private Integer usefulCounts;
    private String pics;
    private Integer isAnonymous;
    private Integer status;
    private Integer evaluate;
    private Long shopId;
}
