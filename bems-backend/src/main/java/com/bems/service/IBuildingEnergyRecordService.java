package com.bems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bems.entity.BuildingEnergyRecord;

import java.util.List;

/**
 * @Auther: inoue
 * @Date: 2026/3/31 - 03 - 31 - 21:15
 * @Description: com.bems.service
 * @version: 1.0
 * @Description: 建筑能耗业务接口
 */
public interface IBuildingEnergyRecordService extends IService<BuildingEnergyRecord> {

    // 核心大屏数据源：分页与条件查询
    Page<BuildingEnergyRecord> queryRecords(Integer current, Integer size, String buildingId, String startDate, String endDate, List<String> parameters);

    // 新增日历台账接口
    java.util.List<java.util.Map<String, Object>> getCalendarSummary(String buildingId);

    // 获取动态聚合的图表数据
    java.util.List<java.util.Map<String, Object>> getChartData(String buildingId, String targetDate, String timeUnit, List<String> parameters);

    // 新增：用于导出的无分页全量查询
    List<BuildingEnergyRecord> queryAllForExport(String buildingId, String startDate, String endDate);

}