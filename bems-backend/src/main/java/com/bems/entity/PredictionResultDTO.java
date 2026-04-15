package com.bems.entity;

import lombok.Data;

/**
 * @Auther: inoue
 * @Date: 2026/4/15 - 04 - 15 - 14:21
 * @Description: com.bems.entity
 * @version: 1.0
 */
@Data
public class PredictionResultDTO {
    private String timeLabel;       // 预测时间 (格式: MM-dd HH:mm)
    private Boolean isFuture;       // 前端用来判断是否画虚线的标识

    // --- 电耗预测数据 ---
    private Double predictedElec;   // AI预测电耗
    private Double upperBound;      // 电耗置信区间上限
    private Double lowerBound;      // 电耗置信区间下限

    // --- 🚀 新增：冷负荷预测数据 ---
    private Double predictedWater;  // AI预测冷负荷
    private Double waterUpperBound; // 冷负荷置信区间上限
    private Double waterLowerBound; // 冷负荷置信区间下限
}
