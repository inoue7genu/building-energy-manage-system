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
    private Double predictedElec;   // AI预测电耗
    private Double upperBound;      // 置信区间上限
    private Double lowerBound;      // 置信区间下限
    private Boolean isFuture;       // 前端用来判断是否画虚线的标识
}
