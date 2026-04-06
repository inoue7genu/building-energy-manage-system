package com.bems.tools;

import com.bems.service.IBuildingEnergyRecordService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
/**
 * @Auther: inoue
 * @Date: 2026/4/3 - 04 - 03 - 21:29
 * @Description: com.bems.tools
 * @version: 1.0
 */
@Configuration
public class EnergyQueryTools {

    // 严谨的 JSON 参数要求
    public record EnergyRequest(
            @JsonProperty(required = true, value = "buildingId")
            @JsonPropertyDescription("建筑的唯一英文标识，例如：Panther_office_Karla")
            String buildingId,

            @JsonProperty(required = true, value = "date")
            @JsonPropertyDescription("查询的具体日期，必须转换为 yyyy-MM-dd 格式，例如：2016-07-01")
            String date
    ) {}

    // 返回给 AI 的数据结构
    public record EnergyResponse(String buildingId, String date, double totalElectricity, String status, String detail) {}

    /**
     * 🚀 核心 MCP 工具：真实查询数据库
     * 注意：我们在方法参数里直接注入了你们写好的 IBuildingEnergyRecordService！
     */
    @Bean
    @Description("高级权限接口：用于查询指定建筑（buildingId）在特定日期（date）的总耗电量(kWh)和诊断状态。当用户问及某栋楼的耗电、能耗情况时，必须调用此工具。")
    public Function<EnergyRequest, EnergyResponse> getBuildingEnergy(IBuildingEnergyRecordService energyService) {
        return request -> {
            System.out.println("⚡ [MCP协议触发] AI 正在调用真实 MySQL 数据库...");
            System.out.println("   -> 提取到的建筑ID: " + request.buildingId());
            System.out.println("   -> 提取到的查询日期: " + request.date());

            // 1. 调用你们现成的真实业务接口查出当天 24 小时的数据
            List<Map<String, Object>> chartData = energyService.getChartData(request.buildingId(), request.date(), "day");

            // 2. 统计这 24 小时的数据，计算总电量并捕捉异常
            double totalElec = 0.0;
            String overallStatus = "normal"; // 默认正常

            if (chartData == null || chartData.isEmpty()) {
                return new EnergyResponse(request.buildingId(), request.date(), 0.0, "no_data", "数据库中未找到该日期的能耗记录");
            }

            for (Map<String, Object> data : chartData) {
                // 累加总电量
                if (data.get("electricity") != null) {
                    totalElec += Double.parseDouble(data.get("electricity").toString());
                }
                // 如果发现某一个小时是异常，就把这一天的总标签设为异常
                String hourStatus = data.get("status").toString();
                if ("night_abnormal".equals(hourStatus) || "critical_abnormal".equals(hourStatus)) {
                    overallStatus = hourStatus;
                }
            }

            // 保留两位小数
            totalElec = Math.round(totalElec * 100.0) / 100.0;
            System.out.println("   -> 数据库计算完毕：总电量 " + totalElec + ", 状态: " + overallStatus);

            // 3. 将真实数据打包发给大模型
            return new EnergyResponse(request.buildingId(), request.date(), totalElec, overallStatus, "数据抓取成功");
        };
    }
}