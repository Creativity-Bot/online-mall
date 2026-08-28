package com.example.onlinemall.service;

import com.example.onlinemall.dto.LoginDTO;
import com.example.onlinemall.dto.ResponseTemplate;

public interface UserService {
    ResponseTemplate<Void> sendCode(String phone);

    ResponseTemplate<String> phoneLogin(LoginDTO loginDTO);
}
