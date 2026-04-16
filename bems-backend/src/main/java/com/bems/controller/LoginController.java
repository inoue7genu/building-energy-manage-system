package com.bems.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @Auther: inoue
 * @Date: 2026/4/16 - 04 - 16 - 15:42
 * @Description: com.bems.controller
 * @version: 1.0
 */
@RestController
@RequestMapping("/api")
public class LoginController {

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> user) {
        String username = user.get("username");
        String password = user.get("password");
        Map<String, Object> res = new HashMap<>();

        // 简单逻辑：如果是 admin/123456 则发放 Token
        if ("admin".equals(username) && "123456".equals(password)) {
            res.put("code", 200);
            res.put("token", "BEMS-AUTH-TOKEN-" + UUID.randomUUID()); // 模拟一个Token
            res.put("msg", "登录成功");
            return res;
        }

        res.put("code", 401);
        res.put("msg", "账号或密码错误");
        return res;
    }
}
