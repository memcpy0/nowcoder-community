package com.nowcoder.community.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private int id;
    private int fromId;
    private int toId;
    private String conversationId; // 会话ID(将两个人的ID按字典序排列并拼接)
    private String content;
    private int status; // 0未读,1已读,2删除
    private Date createTime;
}
