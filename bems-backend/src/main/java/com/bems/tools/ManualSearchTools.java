package com.bems.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.function.Function;

/**
 * @Auther: inoue
 * @Date: 2026/4/6 - 04 - 06 - 19:53
 * @Description: com.bems.tools
 * @version: 1.0
 */
@Configuration
public class ManualSearchTools {

    // 自动读取你在 application.yml 里配置的阿里云 Key
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    // ✅ 你的真实百炼 RAG 应用 ID
    private final String APP_ID = "7699519e200a4fc7b161f42ef2a3258d";

    public record ManualRequest(
            @JsonProperty(required = true, value = "question")
            @JsonPropertyDescription("用户提出的关于设备故障排查、运维规范、产品参数的具体问题")
            String question
    ) {}

    public record ManualResponse(String answer) {}

    @Bean
    @Description("知识检索专家：当用户询问【设备发生故障如何排查】、【运维规范标准】、【设备产品说明与参数】时，必须调用此工具获取官方解答。")
    public Function<ManualRequest, ManualResponse> consultExpertManual() {
        return request -> {
            System.out.println("📚 [MCP协议触发] AI 正在翻阅云端官方运维手册...");
            System.out.println("   -> 检索关键词: " + request.question());

            try {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);

                // 🚀 核心修复 1：使用百炼专属的 Application API 数据格式
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode body = mapper.createObjectNode();
                ObjectNode input = mapper.createObjectNode();

                // 百炼的 App 接口要求参数名为 "prompt"
                input.put("prompt", request.question());
                body.set("input", input);

                String requestBody = mapper.writeValueAsString(body);
                HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

                // 🚀 核心修复 2：切入百炼专属的 RAG 应用调用通道（把 APP_ID 拼到了 URL 里）
                String url = "https://dashscope.aliyuncs.com/api/v1/apps/" + APP_ID + "/completion";
                String response = restTemplate.postForObject(url, entity, String.class);

                // 🚀 核心修复 3：根据新通道的格式剥离出最终答案
                JsonNode root = mapper.readTree(response);
                String answer = root.path("output").path("text").asText();

                System.out.println("   -> 知识库查阅完毕！已提取到官方方案。");
                return new ManualResponse(answer);
            } catch (Exception e) {
                System.out.println("   -> 知识库检索失败：" + e.getMessage());
                return new ManualResponse("抱歉，知识库检索通道暂不可用，请人工核查手册。");
            }
        };
    }
}