package com.nowcoder.community.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.UUID;

public class CommunityUtil {
    // 生成随机字符串
    public static String generateUUID() {
        return UUID.randomUUID().toString().replaceAll("-", ""); // 由字母和-构成,替换掉-
    }

    // md5加密,对密码加密
    // hello + 3sdf3 -> asdfklb123sdf
    public static String md5(String key) {
        if (StringUtils.isBlank(key)) { // null,空串,空格都返回true
            return null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }
}