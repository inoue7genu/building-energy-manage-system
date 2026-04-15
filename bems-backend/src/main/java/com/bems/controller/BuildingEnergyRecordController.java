package com.bems.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bems.entity.BuildingEnergyExcelDTO;
import com.bems.entity.BuildingEnergyRecord;
import com.bems.entity.PredictionResultDTO;
import com.bems.service.EnergyPredictionService;
import com.bems.service.IBuildingEnergyRecordService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


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

    @Autowired
    private EnergyPredictionService predictionService;

    @GetMapping("/page")
    public Page<BuildingEnergyRecord> getRecordsByPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "15") Integer size,
            @RequestParam(required = false) String buildingId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            // 👇 新增：接收前端传来的指标数组
            @RequestParam(required = false) List<String> parameters) {
        return energyService.queryRecords(current, size, buildingId, startDate, endDate, parameters);
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
            @RequestParam(defaultValue = "day") String timeUnit,
            // 👇 新增：接收前端传来的指标数组
            @RequestParam(required = false) List<String> parameters) {
        return energyService.getChartData(buildingId, targetDate, timeUnit, parameters);
    }

    /* ==========================================
       🚀 数据管线 1：工业级安全流式导入 (Import)
       支持：断点续传、主键认亲、重复数据智能覆盖
    ========================================== */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            EasyExcel.read(file.getInputStream(), BuildingEnergyExcelDTO.class, new PageReadListener<BuildingEnergyExcelDTO>(dataList -> {

                List<BuildingEnergyRecord> records = dataList.stream().map(dto -> {
                    BuildingEnergyRecord record = new BuildingEnergyRecord();
                    record.setTimestamp(dto.getTimestamp());
                    record.setBuildingId(dto.getBuildingId());
                    record.setElectricity(dto.getElectricity());
                    record.setChilledwater(dto.getChilledwater());
                    record.setAirTemperature(dto.getAirTemperature());
                    record.setCop(dto.getCop());

                    // 自动推算 hour 和 is_weekend 字段
                    if (dto.getTimestamp() != null) {
                        record.setHour(dto.getTimestamp().getHour());
                        int dayOfWeek = dto.getTimestamp().getDayOfWeek().getValue();
                        record.setIsWeekend((dayOfWeek == 6 || dayOfWeek == 7) ? 1 : 0);
                    }
                    return record;
                }).toList();

                // 🚀 核心升级：saveOrUpdateBatch 的前置“认亲”逻辑
                for (BuildingEnergyRecord record : records) {
                    QueryWrapper<BuildingEnergyRecord> existQuery = new QueryWrapper<>();
                    existQuery.eq("building_id", record.getBuildingId());
                    existQuery.eq("timestamp", record.getTimestamp());

                    // 去数据库查一查，在这个建筑的这个时间点，是不是已经有老数据了？
                    BuildingEnergyRecord existRecord = energyService.getOne(existQuery);

                    if (existRecord != null) {
                        // 💡 认亲成功！把数据库里真正的身份证号 (id) 贴给当前这条新数据
                        // 这样 MyBatis-Plus 底层就会触发 UPDATE（覆盖），而不是 INSERT（插入报错）
                        record.setId(existRecord.getId());
                    }
                }

                // 🚀 执行 MyBatis-Plus 批量插入或更新
                // （有 ID 的覆盖更新，没 ID 的当做新数据插入）
                energyService.saveOrUpdateBatch(records);

            })).sheet().doRead();

            return ResponseEntity.ok().body(java.util.Map.of("code", 200, "msg", "Excel 数据批量融合成功（遇重复已自动覆盖）！"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("code", 500, "msg", "文件读取失败：" + e.getMessage()));
        }
    }

    /* ==========================================
       🚀 数据管线 2：安全流式导出 (Export)
    ========================================== */
    @GetMapping("/export")
    public void exportExcel(
            HttpServletResponse response,
            @RequestParam(required = false) String buildingId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            // 🚀 新增：接收前端传来的各种切片指令
            @RequestParam(required = false) Long current,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) Long startPage,
            @RequestParam(required = false) Long endPage,
            @RequestParam(required = false) List<String> parameters
    ) throws IOException {

        // ==========================================
        // 1. 跨域与响应头设置 (保持你原有的完美配置)
        // ==========================================
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("BEMS_能耗审计报表", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // ==========================================
        // 2. 构建基础查询条件 (复用你之前的过滤逻辑)
        // ==========================================
        QueryWrapper<BuildingEnergyRecord> queryWrapper = new QueryWrapper<>();
        if (buildingId != null && !buildingId.isEmpty()) {
            queryWrapper.eq("building_id", buildingId);
        }
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            queryWrapper.ge("timestamp", startDate + " 00:00:00");
            queryWrapper.le("timestamp", endDate + " 23:59:59");
        }

        List<BuildingEnergyRecord> rawData;

        // ==========================================
        // 🚀 3. 核心截流逻辑：根据前端模式切分数据
        // ==========================================
        if (startPage != null && endPage != null && size != null) {
            // 模式 C：自定义切片提取 (利用底层 limit 精准切割)
            long offset = (startPage - 1) * size;
            long limit = (endPage - startPage + 1) * size;
            queryWrapper.last("LIMIT " + offset + ", " + limit);
            rawData = energyService.list(queryWrapper);

        } else if (current != null && size != null) {
            // 模式 A：仅提取当前视图 (单页提取)
            Page<BuildingEnergyRecord> page = new Page<>(current, size);
            rawData = energyService.page(page, queryWrapper).getRecords();

        } else {
            // 模式 B：全局提取 (前端没传分页参数，直接全量)
            rawData = energyService.queryAllForExport(buildingId, startDate, endDate);
        }

        // ==========================================
        // 4. DTO 转换与指标过滤
        // ==========================================
        List<BuildingEnergyExcelDTO> exportList = rawData.stream().map(record -> {
            BuildingEnergyExcelDTO dto = new BuildingEnergyExcelDTO();
            dto.setId(record.getId());
            dto.setTimestamp(record.getTimestamp());
            dto.setBuildingId(record.getBuildingId());

            // 🌟 锦上添花：根据前端勾选的“监测指标”决定是否填入数据
            // 如果前端没有勾选某项指标，导出的 Excel 中这一列的数据直接留空
            if (parameters == null || parameters.contains("electricity")) {
                dto.setElectricity(record.getElectricity());
            }
            if (parameters == null || parameters.contains("chilledwater")) {
                dto.setChilledwater(record.getChilledwater());
            }
            if (parameters == null || parameters.contains("airTemperature")) {
                dto.setAirTemperature(record.getAirTemperature());
            }
            if (parameters == null || parameters.contains("cop")) {
                dto.setCop(record.getCop());
            }
            return dto;
        }).toList();

        // 5. 一键写出到响应流
        EasyExcel.write(response.getOutputStream(), BuildingEnergyExcelDTO.class)
                .sheet("能耗数据切片")
                .doWrite(exportList);
    }

    @GetMapping("/predict")
    public List<PredictionResultDTO> getPrediction(
            @RequestParam("buildingId") String buildingId,
            @RequestParam("targetDate") String targetDateStr) { // 格式如 2016-01-01

        // 赛题数据是按小时存的，我们取这一天的 23:59:59 作为历史数据的截止点，推演明天的24小时
        LocalDateTime targetDate = LocalDateTime.parse(targetDateStr + " 23:59:59",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return predictionService.predictNext24Hours(buildingId, targetDate);
    }
}
