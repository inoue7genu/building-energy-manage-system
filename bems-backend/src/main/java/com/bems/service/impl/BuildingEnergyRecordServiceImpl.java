package com.bems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import java.util.ArrayList;
import java.util.List;

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
    // 🚀 修复点 1：方法签名这里必须加上 List<String> parameters！
    public Page<BuildingEnergyRecord> queryRecords(Integer current, Integer size, String buildingId, String startDate, String endDate, List<String> parameters) {
        Page<BuildingEnergyRecord> page = new Page<>(current, size);

        // 🚀 修复点 2：动态 Select 需要用到 QueryWrapper，不能用 LambdaQueryWrapper
        QueryWrapper<BuildingEnergyRecord> wrapper = new QueryWrapper<>();

        // 1. 按建筑精确筛选
        if (StringUtils.hasText(buildingId)) {
            wrapper.eq("building_id", buildingId);
        }

        // 必须将前端传来的 String 转换为 LocalDateTime 对象！
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (StringUtils.hasText(startDate)) {
            wrapper.ge("`timestamp`", java.time.LocalDateTime.parse(startDate + " 00:00:00", formatter));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le("`timestamp`", java.time.LocalDateTime.parse(endDate + " 23:59:59", formatter));
        }

        // 表格数据习惯“最新的一眼看到”，按时间降序排列
        wrapper.orderByDesc("`timestamp`");

        // 🚀 核心改造：精细化动态字段过滤 (Select 控制)
        // ==========================================
        if (parameters != null && !parameters.isEmpty()) {
            List<String> selectCols = new ArrayList<>();
            // 🛡️ 强制保留的“骨架字段”：用于计算夜间违规、周末异常等 status，绝对不能丢！
            selectCols.add("id");
            selectCols.add("`timestamp`");
            selectCols.add("building_id");
            selectCols.add("electricity");
            selectCols.add("hour");
            selectCols.add("is_weekend");
            selectCols.add("air_temperature");

            // 📊 根据前端请求动态附加的指标
            if (parameters.contains("chilledwater")) {
                selectCols.add("chilledwater");
            }
            if (parameters.contains("cop")) {
                selectCols.add("cop");
            }

            wrapper.select(selectCols.toArray(new String[0]));
        }
        // ==========================================

        // 执行底层物理分页查询
        Page<BuildingEnergyRecord> resultPage = this.page(page, wrapper);

        // 补充动态预警标签
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
    // 🚀 修复点 3：方法签名这里也必须加上 List<String> parameters！
    public java.util.List<java.util.Map<String, Object>> getChartData(String buildingId, String targetDate, String timeUnit, List<String> parameters) {
        java.time.LocalDate date = java.time.LocalDate.parse(targetDate, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String startTime = "";
        String endTime = "";
        String format = "";

        // 优化点：统一将前端选中的日期作为“起始日 (startTime)”
        if ("day".equals(timeUnit)) {
            startTime = date.atTime(java.time.LocalTime.MIN).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            endTime = date.atTime(java.time.LocalTime.MAX).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            format = "%H:00"; // X轴显示时间
        } else if ("week".equals(timeUnit)) {
            startTime = date.atTime(java.time.LocalTime.MIN).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            endTime = date.plusDays(6).atTime(java.time.LocalTime.MAX).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            format = "%m-%d"; // X轴显示日期
        } else if ("month".equals(timeUnit)) {
            startTime = date.atTime(java.time.LocalTime.MIN).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            endTime = date.plusDays(29).atTime(java.time.LocalTime.MAX).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            format = "%m-%d"; // X轴显示日期
        }

        // 🚀 此处完美传入 parameters
        java.util.List<java.util.Map<String, Object>> list = this.baseMapper.getAggregatedChartData(buildingId, startTime, endTime, format, parameters);

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