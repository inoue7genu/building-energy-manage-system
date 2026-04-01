package com.bems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bems.entity.BuildingEnergyRecord;
import com.bems.mapper.BuildingEnergyRecordMapper;
import com.bems.service.IBuildingEnergyRecordService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * @Auther: inoue
 * @Date: 2026/3/31 - 03 - 31 - 21:16
 * @Description: com.bems.service.impl
 * @version: 1.0
 * @Description: 建筑能耗业务实现类 (支持指定日期精确查询)
 */
@Service
public class BuildingEnergyRecordServiceImpl extends ServiceImpl<BuildingEnergyRecordMapper, BuildingEnergyRecord> implements IBuildingEnergyRecordService {

    @Override
    public Page<BuildingEnergyRecord> queryRecords(Integer current, Integer size, String buildingId, String startDate, String endDate) {
        Page<BuildingEnergyRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<BuildingEnergyRecord> wrapper = new LambdaQueryWrapper<>();

        // 1. 按建筑精确筛选
        if (StringUtils.hasText(buildingId)) {
            wrapper.eq(BuildingEnergyRecord::getBuildingId, buildingId);
        }

        // 2. 按时间范围筛选 (前端传入格式: YYYY-MM-DD)
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(BuildingEnergyRecord::getTimestamp, startDate + " 00:00:00");
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(BuildingEnergyRecord::getTimestamp, endDate + " 23:59:59");
        }

        // 3. 表格数据通常习惯“最新的一眼看到”，所以按时间降序排列
        wrapper.orderByDesc(BuildingEnergyRecord::getTimestamp);

        // 4. 执行底层物理分页查询
        Page<BuildingEnergyRecord> resultPage = this.page(page, wrapper);

        // 5. 补充动态预警标签（复用核心逻辑）
        for (BuildingEnergyRecord record : resultPage.getRecords()) {
            String status = "normal";
            Double elec = record.getElectricity();
            Integer hour = record.getHour();
            Integer isWeekend = record.getIsWeekend();

            if (elec != null && hour != null) {
                if ((hour >= 22 || hour <= 5) && elec > 50.0) {
                    status = "night_abnormal";
                } else if (isWeekend != null && isWeekend == 1 && elec > 100.0) {
                    status = "weekend_abnormal";
                }
                Double temp = record.getAirTemperature();
                if (!"normal".equals(status) && temp != null && temp < 20.0) {
                    status = "critical_abnormal";
                }
            }
            record.setStatus(status);
        }

        return resultPage;
    }


    // 新增日历台账实现
    @Override
    public java.util.List<java.util.Map<String, Object>> getCalendarSummary(String buildingId) {
        return this.baseMapper.getCalendarSummary(buildingId);
    }

    // 智能判断时间粒度与预警
    @Override
    public java.util.List<java.util.Map<String, Object>> getChartData(String buildingId, String targetDate, String timeUnit) {
        java.time.LocalDate date = java.time.LocalDate.parse(targetDate, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String startTime = "";
        String endTime = "";
        String format = "";

        // 🚀 优化点：统一将前端选中的日期作为“起始日 (startTime)”
        if ("day".equals(timeUnit)) {
            startTime = date.atTime(java.time.LocalTime.MIN).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            endTime = date.atTime(java.time.LocalTime.MAX).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            format = "%H:00"; // X轴显示时间
        } else if ("week".equals(timeUnit)) {
            startTime = date.atTime(java.time.LocalTime.MIN).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            // 往后推 6 天（包含当天一共 7 天）作为结束日
            endTime = date.plusDays(6).atTime(java.time.LocalTime.MAX).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            format = "%m-%d"; // X轴显示日期
        } else if ("month".equals(timeUnit)) {
            startTime = date.atTime(java.time.LocalTime.MIN).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            // 往后推 29 天（包含当天一共 30 天）作为结束日
            endTime = date.plusDays(29).atTime(java.time.LocalTime.MAX).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            format = "%m-%d"; // X轴显示日期
        }

        // 调用 Mapper 聚合
        java.util.List<java.util.Map<String, Object>> list = this.baseMapper.getAggregatedChartData(buildingId, startTime, endTime, format);

        // 内存动态预警
        for (java.util.Map<String, Object> map : list) {
            String status = "normal";
            Double elec = map.get("electricity") == null ? 0.0 : Double.parseDouble(map.get("electricity").toString());

            if ("day".equals(timeUnit)) {
                String timeLabel = map.get("timeLabel").toString();
                int hour = Integer.parseInt(timeLabel.split(":")[0]);
                if ((hour >= 22 || hour <= 5) && elec > 50.0) {
                    status = "night_abnormal";
                }
            } else {
                if (elec > 1200.0) {
                    status = "critical_abnormal";
                }
            }
            map.put("status", status);
        }
        return list;
    }
}