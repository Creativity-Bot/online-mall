package com.example.onlinemall.controller;

import com.example.onlinemall.dto.LoginDTO;
import com.example.onlinemall.dto.ResponseTemplate;
import com.example.onlinemall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/sendcode")
    public ResponseTemplate<Void> sendOTPAction(@RequestParam("phone") String phone){
        return userService.sendCode(phone);
    }

    @PostMapping("/login")
    public ResponseTemplate<String> loginAction(@RequestBody LoginDTO loginDTO){
        return userService.phoneLogin(loginDTO);
    }
}
