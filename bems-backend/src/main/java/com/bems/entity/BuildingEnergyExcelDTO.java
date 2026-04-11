package com.bems.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * @Auther: inoue
 * @Date: 2026/4/10 - 04 - 10 - 20:00
 * @Description: com.bems.entity
 * @version: 1.0
 */
@Data
public class BuildingEnergyExcelDTO {

    @ExcelProperty("流水号")
    @ColumnWidth(10)
    private Long id;

    @ExcelProperty("记录时间")
    @ColumnWidth(25)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    @ExcelProperty("建筑节点标识")
    @ColumnWidth(30)
    private String buildingId;

    @ExcelProperty("耗电量(kWh)")
    @ColumnWidth(15)
    private Double electricity;

    @ExcelProperty("冷负荷(kWh)")
    @ColumnWidth(15)
    private Double chilledwater;

    @ExcelProperty("室外温度(℃)")
    @ColumnWidth(15)
    private Double airTemperature;

    @ExcelProperty("系统COP")
    @ColumnWidth(15)
    private Double cop;
}