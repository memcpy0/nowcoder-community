package com.nowcoder.community.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginTicket {
    private int id; // 凭证ID
    private int userId; // 用户ID
    private String ticket; // 凭证内容
    private int status; // 0有效,1无效
    private Date expired; // 凭证有效时长,也有时分秒
}