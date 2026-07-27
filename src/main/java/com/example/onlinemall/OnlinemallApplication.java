package com.example.onlinemall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.onlinemall.dao")
public class OnlinemallApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnlinemallApplication.class, args);
	}
}
