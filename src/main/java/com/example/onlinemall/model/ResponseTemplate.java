package com.example.onlinemall.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class ResponseTemplate {
    private int code;
    private String msg;
    private Object object;
}
