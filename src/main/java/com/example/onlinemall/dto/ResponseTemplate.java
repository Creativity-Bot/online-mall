package com.example.onlinemall.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class ResponseTemplate<T> {
    private Boolean success;
    private String errorMsg;
    private T data;

    public static ResponseTemplate<Void> success(){
        return new ResponseTemplate(true, null, null);
    }
    public static <T> ResponseTemplate<T> success(T data){
        return new ResponseTemplate<>(true, null, data);
    }
    public static ResponseTemplate<Void> fail(String errorMsg){
        return new ResponseTemplate(false, errorMsg, null);
    }
}
