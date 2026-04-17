package com.bems.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Auther: inoue
 * @Date: 2026/4/3 - 04 - 03 - 20:44
 * @Description: 智能诊断控制器 (完全重构为基于本地 RAG 的 MCP 架构)
 * @version: 1.0
 */
@RestController
@RequestMapping("/api/ai")
public class SmartDiagnosisController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    // 构造器注入：Spring AI 提供的流式对话客户端和我们刚才建好的本地向量库
    public SmartDiagnosisController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    /**
     * 流式对话接口 (Server-Sent Events)
     */
    @GetMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> streamChat(@RequestParam String message) {

        System.out.println("🤖 [BEMS AI] 收到诊断请求: " + message);

        // 步骤 1：本地 RAG 知识检索 (获取最相关的 7 个切片)
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.query(message).withTopK(7)
        );

        // 步骤 2：拼接上下文
        String context = similarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));

        // 👇 新增这 3 行，把 AI 看到的“小抄”直接打印在控制台！
        System.out.println("====== [RAG 检索到的知识库内容] ======");
        System.out.println(context);
        System.out.println("=====================================");

        System.out.println("📚 [BEMS RAG] 检索完成，开始构建终极提示词...");

        // ==========================================
        // 步骤 3：构建【全量 User Prompt】(放弃 System 角色)
        // 将最高指令放在最后，强制模型注意力集中在“简体中文”上
        // ==========================================
        String finalPrompt = String.format(
                "你是一个专业的建筑能效管理系统(BEMS)智能运维专家。请严格基于以下【内部知识库】的内容来回答我的提问。\n\n" +
                        "【内部知识库】开始：\n" +
                        "%s\n" +
                        "【内部知识库】结束。\n\n" +
                        "【用户提问】\n" +
                        "%s\n\n" +
                        "【回答要求】(非常重要)：\n" +
                        "1. 绝！对！禁！止！使用繁体字。你必须全程使用纯正的简体中文进行回复。\n" +
                        "2. 如果知识库中没有相关答案，直接回答“抱歉，暂未收录”，禁止自己编造。\n" +
                        "3. 保持 Markdown 格式输出。\n" +
                        "请现在开始用简体中文回答：",
                context, message
        );

        // ==========================================
        // 步骤 4：只调用 user 角色发送
        // ==========================================
        return chatClient.prompt()
                .user(finalPrompt) // 直接把所有东西当做用户的话发过去
                .stream()
                .content();
    }
}
