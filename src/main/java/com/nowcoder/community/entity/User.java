package com.nowcoder.community.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class User {
    private int id; // 用户ID
    private String username; // 用户名
    private String password; // 密码
    private String salt; // 加盐
    private String email; // 邮箱
    private int type; // 用户类型
    private int status; // 用户状态
    private String activationCode; // 激活码
    private String headerUrl; // 用户头像地址
    private LocalDateTime createTime;
}
