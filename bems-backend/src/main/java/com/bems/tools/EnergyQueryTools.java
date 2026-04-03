package com.bems.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;
/**
 * @Auther: inoue
 * @Date: 2026/4/3 - 04 - 03 - 21:29
 * @Description: com.bems.tools
 * @version: 1.0
 */
@Configuration
public class EnergyQueryTools {

    // 1. 定义大模型传给我们的参数结构 (大模型会自动从用户的自然语言中提取这些字段)
    public record EnergyRequest(
            @JsonProperty(required = true, value = "buildingId")
            @JsonPropertyDescription("建筑的唯一英文标识，例如：Panther_office_Karla")
            String buildingId,

            @JsonProperty(required = true, value = "date")
            @JsonPropertyDescription("查询的具体日期，必须转换为 yyyy-MM-dd 格式，例如：2016-07-01")
            String date
    ) {}

    // 2. 定义我们查完数据库后，返回给大模型的数据结构
    public record EnergyResponse(String buildingId, String date, double totalElectricity, String status) {}

    /**
     * 🚀 核心 MCP 工具：查询能耗
     * @Description 的内容极其重要！大模型就是靠阅读这段中文来决定要不要调用这个工具的。
     */
    @Bean
    @Description("高级权限接口：用于查询指定建筑（buildingId）在特定日期（date）的总耗电量(kWh)和诊断状态。当用户问及某栋楼的耗电、能耗情况时，必须调用此工具。")
    public Function<EnergyRequest, EnergyResponse> getBuildingEnergy() {
        return request -> {
            // 当大模型按规矩出牌时，这里就会被完美触发！
            System.out.println("⚡ [MCP协议触发] AI 正在调用后端数据库工具...");
            System.out.println("   -> 提取到的建筑ID: " + request.buildingId());
            System.out.println("   -> 提取到的查询日期: " + request.date());

            // 战术模拟数据 (等跑通后，这里换成你的 MyBatis-Plus 查询)
            double mockElec = 1850.5;
            String mockStatus = "night_abnormal";

            return new EnergyResponse(request.buildingId(), request.date(), mockElec, mockStatus);
        };
    }
}
