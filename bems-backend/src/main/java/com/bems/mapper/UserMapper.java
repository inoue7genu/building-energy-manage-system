package com.bems.mapper;

import com.bems.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @Auther: inoue
 * @Date: 2026/4/16 - 04 - 16 - 18:41
 * @Description: com.bems.mapper
 * @version: 1.0
 */
@Mapper
public interface UserMapper {
    // 🚀 核心 SQL：根据用户名去数据库查人
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User findByUsername(String username);
}