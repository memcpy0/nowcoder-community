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
public class DiscussPost {
    private int id; // 帖子ID
    private int userId; // 用户ID
    private String title; // 标题
    private String content; // 内容
    private int type; // 是否置顶
    private int status; // 0-正常,1-精华,2-拉黑
    private LocalDateTime createTime; // 创建时间
    private int commentCount; // 帖子分数
    private double score;

}
