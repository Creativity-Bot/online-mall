package com.example.onlinemall.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User{
    /** 主键 */
    private Long id;

    /** 手机号码 unique */
    private String phone;

    /** 密码，加密存储 */
    private String password;

    /** 昵称，默认是随机字符 */
    private String nickName;

    /** 用户头像 */
    private String icon;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
