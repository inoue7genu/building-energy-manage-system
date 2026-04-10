package com.bems.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: inoue
 * @Date: 2026/4/9 - 04 - 09 - 20:07
 * @Description: BEMS 本地 RAG 混合知识库配置 (全动态扫描版)
 * @version: 1.0
 */
@Configuration
public class RagConfig {

    // 🌟 核心魔法：使用 classpath* 通配符，自动递归扫描 knowledge_base 下所有子文件夹里的 .txt 文件！
    @Value("classpath*:knowledge_base/**/*.txt")
    private Resource[] knowledgeResources;

    // 定义本地向量缓存文件的存放路径 (存放在项目根目录的 data 文件夹下)
    private static final String VECTOR_STORE_PATH = "data/vector_store.json";

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        System.out.println("====== 🕵️ 核心诊断：当前注入的向量引擎是：" + embeddingModel.getClass().getSimpleName() + " ======");

        SimpleVectorStore vectorStore = new SimpleVectorStore(embeddingModel);
        File vectorStoreFile = new File(VECTOR_STORE_PATH);

        // ==========================================
        // 1. 缓存闪电加载逻辑
        // ==========================================
        if (vectorStoreFile.exists()) {
            System.out.println("⚡ [BEMS 性能优化]: 发现本地向量缓存文件，正在从本地闪电加载...");
            vectorStore.load(vectorStoreFile);
            System.out.println("✅ [BEMS 系统日志]: 自动化知识库加载完成！");
            return vectorStore;
        }

        // ==========================================
        // 2. 首次初始化与动态文件遍历
        // ==========================================
        System.out.println("⏳ [BEMS 系统日志]: 未发现本地缓存，系统正在扫描知识库文件夹并初始化...");
        List<Document> allDocuments = new ArrayList<>();

        if (knowledgeResources != null && knowledgeResources.length > 0) {
            System.out.println("📁 [BEMS 系统日志]: 共扫描到 " + knowledgeResources.length + " 个核心知识库文件，开始读取...");
            for (Resource resource : knowledgeResources) {
                try {
                    // 动态读取每个文件
                    allDocuments.addAll(new TextReader(resource).get());
                    System.out.println("  -> 成功装载: " + resource.getFilename());
                } catch (Exception e) {
                    System.err.println("❌ [BEMS 警告]: 读取文件失败 " + resource.getFilename() + "，原因：" + e.getMessage());
                }
            }
        } else {
            System.err.println("⚠️ [BEMS 警告]: knowledge_base 目录下未扫描到任何 txt 文件！");
        }

        // ==========================================
        // 3. 文本切片与向量化 (Embedding)
        // ==========================================
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> splitDocuments = splitter.apply(allDocuments);

        System.out.println("🧠 [BEMS 系统日志]: 正在调用大模型进行高维向量化计算，这可能需要几十秒，请耐心等待...");
        vectorStore.add(splitDocuments);

        // ==========================================
        // 4. 持久化缓存至本地
        // ==========================================
        File parentDir = vectorStoreFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs(); // 确保 data 目录存在
        }
        vectorStore.save(vectorStoreFile);

        System.out.println("🚀 [BEMS 系统日志]: 知识库初次向量化完毕，共切分 " + splitDocuments.size() + " 个知识区块。已永久缓存至本地：" + VECTOR_STORE_PATH);

        return vectorStore;
    }
}
