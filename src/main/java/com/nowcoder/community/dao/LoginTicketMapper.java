package com.nowcoder.community.dao;

import com.nowcoder.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Mapper
public interface LoginTicketMapper {
    // 插入凭证
    @Insert({
            "insert into login_ticket(user_id, ticket, status, expired) ",
            "values(#{userId}, #{ticket}, #{status}, #{expired})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id") // 使用生成的键,并将键值回填
    int insertLoginTicket(LoginTicket loginTicket);
    // 查询凭证
    @Select({
            "select id, user_id, ticket, status, expired ",
            "from login_ticket where ticket = #{ticket}"
    })
    LoginTicket selectByTicket(String ticket);

    // 更新状态
    @Update({
            "<script>",
            "update login_ticket set status=#{status} where ticket = #{ticket} ",
            "<if test=\"ticket!=null\"> ",
            "and 1 = 1",
            "</if>",
            "</script>"
    })
    int updateStatus(String ticket, int status);
}