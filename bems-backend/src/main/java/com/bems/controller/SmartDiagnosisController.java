package com.bems.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * @Auther: inoue
 * @Date: 2026/4/3 - 04 - 03 - 20:44
 * @Description: com.bems.controller
 * @version: 1.0
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class SmartDiagnosisController {

    private final ChatClient chatClient;

    public SmartDiagnosisController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> streamChat(@RequestParam String message) {

        System.out.println("🤖 接收到指挥官指令: " + message);

        try {
            // 🚀 第 1 步：使用同步调用 (.call) 稳如泰山地执行大模型和 MCP 工具查库
            // 这会完美绕过阿里云流式返回空字符串的 Bug
            String fullResponse = chatClient.prompt()
                    .user(message)
                    .functions("getBuildingEnergy", "consultExpertManual")// 挂载查询工具
                    .call()
                    .content();

            // 🚀 第 2 步：极客魔法！将完整的回答切成一个个字，手动组装成流式（SSE）水管发给前端
            String[] words = fullResponse.split("");
            return Flux.fromArray(words)
                    .delayElements(Duration.ofMillis(30)); // 完美模拟：每个字延迟 30 毫秒吐出

        } catch (Exception e) {
            e.printStackTrace();
            return Flux.just("系统警告：MCP 链路接驳异常，请检查控制台日志。");
        }
    }
}
