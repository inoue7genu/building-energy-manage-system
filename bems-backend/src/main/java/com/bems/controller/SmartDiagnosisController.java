package com.bems.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * @Auther: inoue
 * @Date: 2026/4/3 - 04 - 03 - 20:44
 * @Description: com.bems.controller
 * @version: 1.0
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin // 允许前端跨域请求，以后 Vue 调接口才不会报错
public class SmartDiagnosisController {

    private final ChatClient chatClient;

    // 构造函数：Spring 自动把配置好的 AI 客户端注入进来
    public SmartDiagnosisController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 极客终端流式对话接口 (SSE 技术)
     * 返回值 Flux<String> 就像一根水管，源源不断地把 AI 生成的字推给前端
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam String message) {

        System.out.println("🤖 接收到指挥官指令: " + message);

        return chatClient.prompt()
                .user(message) // 把前端用户的提问塞给 AI
                .stream()      // 🚀 核心：开启流式打字机模式
                .content();    // 提取文字内容返回
    }
}
