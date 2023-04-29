package com.nowcoder.community.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

@Document(indexName = "discusspost", shards = 6, replicas = 3) // 根据服务器处理能力来配置
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DiscussPost {
    @Id // 建索引时把这个数据存到ID字段
    private int id; // 帖子ID

    @Field(type = FieldType.Integer)
    private int userId; //

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart") // 用于搜索,存储时的解析器,搜索时的解析器
    private String title; // 标题

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart") // 用于搜索,存储时的解析器,搜索时的解析器
    private String content; // 内容

    @Field(type = FieldType.Integer)
    private int type; // 是否置顶

    @Field(type = FieldType.Integer)
    private int status; // 0-正常,1-精华,2-拉黑
    @Field(type = FieldType.Date)
    private Date createTime; // 创建时间

    @Field(type = FieldType.Integer)
    private int commentCount; // 评论数

    @Field(type = FieldType.Double)
    private double score; // 帖子分数

}