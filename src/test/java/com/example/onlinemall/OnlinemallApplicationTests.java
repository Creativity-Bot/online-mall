package com.example.onlinemall;

import cn.hutool.json.JSONUtil;
import com.example.onlinemall.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;

@SpringBootTest
class OnlinemallApplicationTests {

	@Autowired
	private StringRedisTemplate stringRedisTemplate;


	@Test
	void contextLoads() {
		User test = User.builder().nickName("test").build();
		//String json = JSONUtil.toJsonStr(test);
		//stringRedisTemplate.opsForValue().set("user", json);
		HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
		objectObjectHashMap.put(test, test);// wrong
		stringRedisTemplate.opsForHash().putAll("test", objectObjectHashMap);
	}

}
