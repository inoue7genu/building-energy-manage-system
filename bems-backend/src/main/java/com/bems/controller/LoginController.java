package com.bems.controller;

import com.bems.entity.User;
import com.bems.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @Auther: inoue
 * @Date: 2026/4/16
 * @Description: 真实数据库联调版本 LoginController
 */
@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private UserService userService; // 🚀 注入业务层

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> userMap) {
        String username = userMap.get("username");
        String password = userMap.get("password");
        Map<String, Object> res = new HashMap<>();

        // 🚀 调用 Service 去对比数据库！
        User loginUser = userService.login(username, password);

        if (loginUser != null) {
            // 查无此人或密码错误会返回null，进到这里说明完全匹配！
            res.put("code", 200);
            res.put("token", "BEMS-AUTH-TOKEN-" + UUID.randomUUID());
            res.put("msg", "登录成功");
            res.put("nickname", loginUser.getNickname()); // 把昵称也传给前端
            return res;
        }

        // 走到这里说明数据库里没有这个账号，或者密码对不上
        res.put("code", 401);
        res.put("msg", "账号或密码错误");
        return res;
    }
}