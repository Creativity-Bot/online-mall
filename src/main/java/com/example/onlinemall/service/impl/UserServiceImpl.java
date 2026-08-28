package com.example.onlinemall.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.example.onlinemall.dao.UserDao;
import com.example.onlinemall.dto.LoginDTO;
import com.example.onlinemall.dto.ResponseTemplate;
import com.example.onlinemall.dto.UserDTO;
import com.example.onlinemall.entity.User;
import com.example.onlinemall.exception.SQLException;
import com.example.onlinemall.exception.ValidationException;
import com.example.onlinemall.service.UserService;
import com.example.onlinemall.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static com.example.onlinemall.constant.RedisConstant.*;
import static com.example.onlinemall.constant.UserConstant.USER_NICK_NAME_PREFIX;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserDao userDao;

    @Override
    public ResponseTemplate<Void> sendCode(String phone) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            log.error("phone number is not correct : {}", phone);
            throw new IllegalArgumentException("phone number is not correct");
        }
        String code = RandomUtil.randomNumbers(6);

        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+ phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // call text service to send code
        log.info("code sent by text: {}", code);
        return ResponseTemplate.success();
    }

    @Override
    @Transactional
    public ResponseTemplate<String> phoneLogin(LoginDTO loginDTO) {
        String phone = loginDTO.getPhone();
        String code = loginDTO.getCode();

        if (RegexUtils.isPhoneInvalid(phone)) {
            throw new IllegalArgumentException("phone number is not correct");
        }
        if(StrUtil.isBlank(code)){
            throw new IllegalArgumentException("code is not correct");
        }

        String value = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        if (StrUtil.isBlank(value)) {
            throw new ValidationException("code is expired, pls retry");
        } else if (!code.equals(value)) {
            throw new ValidationException("code is not matched, pls retry");
        }

        User user = userDao.selectByPhone(phone);

        if (user == null) {
            user = User.builder()
                    .phone(phone)
                    .nickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10))
                    .build();
            if (userDao.insert(user) != 1) {
                throw new SQLException("DB unexpected error");
            }
        }

        String token = UUID.randomUUID().toString(true);

        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) ->
                                fieldValue == null ? null : fieldValue.toString()));

        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return ResponseTemplate.success(tokenKey);
    }
}
