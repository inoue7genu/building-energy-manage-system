package com.bems.service;

import com.bems.entity.User;
import com.bems.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Auther: inoue
 * @Date: 2026/4/16 - 04 - 16 - 18:42
 * @Description: com.bems.service
 * @version: 1.0
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public User login(String username, String password) {
        // 1. 去数据库里找这个账号
        User user = userMapper.findByUsername(username);

        // 2. 如果账号存在，并且数据库里的密码和传进来的密码一致，说明登录成功
        // （实际开发中密码是加密的，这里我们先用明文比对跑通流程）
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }

        // 3. 登录失败，返回 null
        return null;
    }
}
