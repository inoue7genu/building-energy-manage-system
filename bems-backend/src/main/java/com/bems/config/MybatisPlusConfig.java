package com.bems.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Auther: inoue
 * @Date: 2026/4/1 - 04 - 01 - 14:48
 * @Description: com.bems.config
 * @version: 1.0
 * @Description: MyBatis-Plus 核心配置类
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册分页插件（极其重要！没有它，分页查询会变成全量查询，导致前端卡死）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 指定数据库类型为 MySQL，开启物理分页拦截
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
