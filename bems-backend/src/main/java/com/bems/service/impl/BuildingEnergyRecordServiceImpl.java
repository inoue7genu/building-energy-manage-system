package com.bems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bems.entity.BuildingEnergyRecord;
import com.bems.mapper.BuildingEnergyRecordMapper;
import com.bems.service.IBuildingEnergyRecordService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: inoue
 * @Date: 2026/3/31 - 03 - 31 - 21:16
 * @Description: 建筑能耗业务实现类 (已修复时间过滤解析失效问题)
 */
@Service
public class BuildingEnergyRecordServiceImpl extends ServiceImpl<BuildingEnergyRecordMapper, BuildingEnergyRecord> implements IBuildingEnergyRecordService {

    @Override
    public Page<BuildingEnergyRecord> queryRecords(Integer current, Integer size, String buildingId, String startDate, String endDate, List<String> parameters) {
        Page<BuildingEnergyRecord> page = new Page<>(current, size);
        QueryWrapper<BuildingEnergyRecord> wrapper = new QueryWrapper<>();

        // 1. 按建筑精确筛选
        if (StringUtils.hasText(buildingId)) {
            wrapper.eq("building_id", buildingId);
        }

        // 🚀 核心修复：直接给 MySQL 传字符串进行日期对比，抛弃容易引发 JDBC 映射失效的 LocalDateTime！
        if (StringUtils.hasText(startDate)) {
            wrapper.ge("`timestamp`", startDate + " 00:00:00");
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le("`timestamp`", endDate + " 23:59:59");
        }

        // 表格数据习惯“最新的一眼看到”，按时间降序排列
        wrapper.orderByAsc("`timestamp`");

        // 🚀 核心改造：精细化动态字段过滤 (Select 控制)
        if (parameters != null && !parameters.isEmpty()) {
            List<String> selectCols = new ArrayList<>();
            // 🛡️ 强制保留的“骨架字段”
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

    @Override
    public java.util.List<java.util.Map<String, Object>> getCalendarSummary(String buildingId) {
        return this.baseMapper.getCalendarSummary(buildingId);
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getChartData(String buildingId, String targetDate, String timeUnit, List<String> parameters) {
        java.time.LocalDate date = java.time.LocalDate.parse(targetDate, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String startTime = "";
        String endTime = "";
        String format = "";

        if ("day".equals(timeUnit)) {
            startTime = date.atTime(java.time.LocalTime.MIN).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            endTime = date.atTime(java.time.LocalTime.MAX).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            format = "%H:00";
        } else if ("week".equals(timeUnit)) {
            startTime = date.atTime(java.time.LocalTime.MIN).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            endTime = date.plusDays(6).atTime(java.time.LocalTime.MAX).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            format = "%m-%d";
        } else if ("month".equals(timeUnit)) {
            startTime = date.atTime(java.time.LocalTime.MIN).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            endTime = date.plusDays(29).atTime(java.time.LocalTime.MAX).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            format = "%m-%d";
        }

        java.util.List<java.util.Map<String, Object>> list = this.baseMapper.getAggregatedChartData(buildingId, startTime, endTime, format, parameters);

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

    @Override
    public List<BuildingEnergyRecord> queryAllForExport(String buildingId, String startDate, String endDate) {
        QueryWrapper<BuildingEnergyRecord> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(buildingId)) {
            wrapper.eq("building_id", buildingId);
        }

        if (StringUtils.hasText(startDate)) {
            wrapper.ge("`timestamp`", startDate + " 00:00:00");
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le("`timestamp`", endDate + " 23:59:59");
        }

        // 🚀 修复点 2：导出报表也同步改为时间升序排列
        wrapper.orderByAsc("`timestamp`");

        return this.list(wrapper);
    }
}