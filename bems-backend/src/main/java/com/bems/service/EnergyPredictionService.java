package com.bems.service;

import com.bems.entity.BuildingEnergyRecord;
import com.bems.entity.PredictionResultDTO;
import com.bems.mapper.BuildingEnergyRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @Auther: inoue
 * @Date: 2026/4/15 - 04 - 15 - 14:21
 * @Description: com.bems.service
 * @version: 1.0
 */
@Service
public class EnergyPredictionService {

    @Resource
    private BuildingEnergyRecordMapper recordMapper;

    public List<PredictionResultDTO> predictNext24Hours(String buildingId, LocalDateTime targetDate) {
        LocalDateTime startDate = targetDate.minusDays(14);
        LambdaQueryWrapper<BuildingEnergyRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BuildingEnergyRecord::getBuildingId, buildingId)
                .between(BuildingEnergyRecord::getTimestamp, startDate, targetDate)
                .orderByAsc(BuildingEnergyRecord::getTimestamp);

        List<BuildingEnergyRecord> historyData = recordMapper.selectList(queryWrapper);
        if (historyData == null || historyData.size() < 24) {
            throw new RuntimeException("历史数据不足，无法完成 AI 预测建模");
        }

        // 1. 准备特征属性（前3个是特征，第4个是目标值）
        ArrayList<Attribute> attributesElec = new ArrayList<>();
        attributesElec.add(new Attribute("hour"));
        attributesElec.add(new Attribute("is_weekend"));
        attributesElec.add(new Attribute("air_temperature"));
        attributesElec.add(new Attribute("electricity")); // 电量目标

        ArrayList<Attribute> attributesWater = new ArrayList<>();
        attributesWater.add(new Attribute("hour"));
        attributesWater.add(new Attribute("is_weekend"));
        attributesWater.add(new Attribute("air_temperature"));
        attributesWater.add(new Attribute("chilledwater")); // 🚀 冷量目标

        // 2. 初始化双数据集
        Instances trainingSetElec = new Instances("ElecTrainSet", attributesElec, historyData.size());
        trainingSetElec.setClassIndex(3);

        Instances trainingSetWater = new Instances("WaterTrainSet", attributesWater, historyData.size());
        trainingSetWater.setClassIndex(3);

        // 3. 并行灌入历史数据
        for (BuildingEnergyRecord record : historyData) {
            double hour = record.getTimestamp().getHour();
            double isWeekend = isWeekend(record.getTimestamp()) ? 1.0 : 0.0;
            double temp = record.getAirTemperature() != null ? record.getAirTemperature() : 25.0;

            // 灌入电耗集
            Instance instElec = new DenseInstance(4);
            instElec.setValue(attributesElec.get(0), hour);
            instElec.setValue(attributesElec.get(1), isWeekend);
            instElec.setValue(attributesElec.get(2), temp);
            instElec.setValue(attributesElec.get(3), record.getElectricity() != null ? record.getElectricity() : 0.0);
            trainingSetElec.add(instElec);

            // 🚀 灌入冷量集
            Instance instWater = new DenseInstance(4);
            instWater.setValue(attributesWater.get(0), hour);
            instWater.setValue(attributesWater.get(1), isWeekend);
            instWater.setValue(attributesWater.get(2), temp);
            instWater.setValue(attributesWater.get(3), record.getChilledwater() != null ? record.getChilledwater() : 0.0);
            trainingSetWater.add(instWater);
        }

        // 4. 并行训练双模型 (双擎驱动)
        RandomForest rfElec = new RandomForest();
        RandomForest rfWater = new RandomForest();
        try {
            rfElec.buildClassifier(trainingSetElec);
            rfWater.buildClassifier(trainingSetWater);
        } catch (Exception e) {
            throw new RuntimeException("AI 模型训练失败: " + e.getMessage());
        }

        // 5. 联合推演未来 24 小时
        List<PredictionResultDTO> predictions = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        Random random = new Random();
        Double lastTemp = historyData.get(historyData.size() - 1).getAirTemperature();
        if(lastTemp == null) lastTemp = 25.0;

        for (int i = 1; i <= 24; i++) {
            LocalDateTime futureTime = targetDate.plusHours(i);
            double simulatedTemp = lastTemp + Math.sin(futureTime.getHour() * Math.PI / 12) * 5;

            try {
                // 构建未来实例
                Instance futureInstElec = new DenseInstance(4);
                futureInstElec.setDataset(trainingSetElec);
                futureInstElec.setValue(attributesElec.get(0), futureTime.getHour());
                futureInstElec.setValue(attributesElec.get(1), isWeekend(futureTime) ? 1.0 : 0.0);
                futureInstElec.setValue(attributesElec.get(2), simulatedTemp);

                Instance futureInstWater = new DenseInstance(4);
                futureInstWater.setDataset(trainingSetWater);
                futureInstWater.setValue(attributesWater.get(0), futureTime.getHour());
                futureInstWater.setValue(attributesWater.get(1), isWeekend(futureTime) ? 1.0 : 0.0);
                futureInstWater.setValue(attributesWater.get(2), simulatedTemp);

                // 分别推理
                double predictedElec = rfElec.classifyInstance(futureInstElec) * (0.95 + (0.1 * random.nextDouble()));
                double predictedWater = rfWater.classifyInstance(futureInstWater) * (0.95 + (0.1 * random.nextDouble()));

                PredictionResultDTO dto = new PredictionResultDTO();
                dto.setTimeLabel(futureTime.format(formatter));
                dto.setIsFuture(true);

                double uncertainty = 0.03 + (i * 0.005);

                // 封装电耗结果
                dto.setPredictedElec(predictedElec);
                dto.setUpperBound(predictedElec * (1 + uncertainty));
                dto.setLowerBound(predictedElec * (1 - uncertainty));

                // 🚀 封装冷负荷结果
                dto.setPredictedWater(predictedWater);
                dto.setWaterUpperBound(predictedWater * (1 + uncertainty));
                dto.setWaterLowerBound(predictedWater * (1 - uncertainty));

                predictions.add(dto);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return predictions;
    }

    private boolean isWeekend(LocalDateTime time) {
        int dayOfWeek = time.getDayOfWeek().getValue();
        return dayOfWeek == 6 || dayOfWeek == 7;
    }
}
