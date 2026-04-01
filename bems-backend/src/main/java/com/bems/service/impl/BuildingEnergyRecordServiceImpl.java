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
    public Page<BuildingEnergyRecord> queryRecords(Integer current, Integer size, String buildingId, String targetDate) {
        // 由于需要画出整天的趋势，这里的查询逻辑改为：
        // 如果前端传了具体日期，我们就把这天从 00:00 到 23:59 的所有数据查出来。
        // 为了兼容之前的分页查询逻辑，如果前端没传日期，我们仍然只查最新的一页(24条)。

        Page<BuildingEnergyRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<BuildingEnergyRecord> wrapper = new LambdaQueryWrapper<>();

        // 1. 按建筑 ID 筛选
        if (StringUtils.hasText(buildingId)) {
            wrapper.eq(BuildingEnergyRecord::getBuildingId, buildingId);
        }

        // 2. 按目标日期筛选（关键新增逻辑）
        if (StringUtils.hasText(targetDate)) {
            // 解析前端传来的 YYYY-MM-DD
            LocalDate date = LocalDate.parse(targetDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            // 构建当天的起始和结束时间
            LocalDateTime startOfDay = date.atTime(LocalTime.MIN);
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

            // 筛选条件：时间大于等于当天的 00:00:00，且小于等于当天的 23:59:59
            wrapper.ge(BuildingEnergyRecord::getTimestamp, startOfDay);
            wrapper.le(BuildingEnergyRecord::getTimestamp, endOfDay);

            // 如果指定了具体日期查询一整天，为了保证展示完整，强行把查询数量设为 24 条
            page.setSize(24);
            // 按照时间正序排列，方便 ECharts 画线
            wrapper.orderByAsc(BuildingEnergyRecord::getTimestamp);

        } else {
            // 如果没传日期，则保持原样：按时间降序查询最新的一页
            wrapper.orderByDesc(BuildingEnergyRecord::getTimestamp);
        }

        // 获取底层纯净数据
        Page<BuildingEnergyRecord> resultPage = this.page(page, wrapper);

        // 如果走的是默认查询（没传日期，按降序查的最新一天），需要把查出来的数据再反转回正序，否则图表时间线反着
        if (!StringUtils.hasText(targetDate) && !resultPage.getRecords().isEmpty()) {
            java.util.Collections.reverse(resultPage.getRecords());
        }

        // 3. AI 级内存动态预警诊断（保持不变）
        for (BuildingEnergyRecord record : resultPage.getRecords()) {
            String status = "normal";
            Double elec = record.getElectricity();
            Integer hour = record.getHour();
            Integer isWeekend = record.getIsWeekend();
            Double temp = record.getAirTemperature();

            if (elec != null && hour != null) {
                // 规则A：深夜非工作时间 (22:00 - 05:00) 耗电量异常偏高 (>50)
                if ((hour >= 22 || hour <= 5) && elec > 50.0) {
                    status = "night_abnormal";
                }
                // 规则B：周末空场期耗电量过高
                else if (isWeekend != null && isWeekend == 1 && elec > 100.0) {
                    status = "weekend_abnormal";
                }

                // 规则C：环境交叉验证（深度诊断）
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