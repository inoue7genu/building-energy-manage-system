package com.bems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bems.entity.BuildingEnergyRecord;
import com.bems.service.IBuildingEnergyRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Auther: inoue
 * @Date: 2026/3/31 - 03 - 31 - 21:16
 * @Description: com.bems.controller
 * @version: 1.0
 * @Description: 前后端分离 API 接口
 */
@RestController
@RequestMapping("/api/energy")
@CrossOrigin(origins = "*")
public class BuildingEnergyRecordController {

    @Autowired
    private IBuildingEnergyRecordService energyService;

    @GetMapping("/page")
    public Page<BuildingEnergyRecord> getRecordsByPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "24") Integer size,
            @RequestParam(required = false) String buildingId,
            // 新增接收前端传来的具体日期参数 (格式: YYYY-MM-DD)
            @RequestParam(required = false) String targetDate) {

        return energyService.queryRecords(current, size, buildingId, targetDate);
    }

    // 新增：前端日历热力图专用接口
    @GetMapping("/calendar")
    public java.util.List<java.util.Map<String, Object>> getCalendarSummary(@RequestParam String buildingId) {
        return energyService.getCalendarSummary(buildingId);
    }

    /**
     * 新增：大屏图表专用数据源 (按 日/周/月 动态聚合)
     */
    @GetMapping("/chart")
    public java.util.List<java.util.Map<String, Object>> getChartData(
            @RequestParam String buildingId,
            @RequestParam String targetDate,
            @RequestParam(defaultValue = "day") String timeUnit) {

        return energyService.getChartData(buildingId, targetDate, timeUnit);
    }
}
