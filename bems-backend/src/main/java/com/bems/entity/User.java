package com.bems.entity;

import lombok.Data;

/**
 * @Auther: inoue
 * @Date: 2026/4/16 - 04 - 16 - 18:41
 * @Description: com.bems.entity
 * @version: 1.0
 */
@Data
public class User {
    private Integer id;
    private String username;
    private String password;
    private String nickname;
}