package com.bems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Auther: inoue
 * @Date: 2026/3/31 - 03 - 31 - 21:10
 * @Description: com.bems.entity
 * @version: 1.0
 */
@Data
@TableName("building_energy_records")
public class BuildingEnergyRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("`timestamp`") // 必须加反引号，防止 MySQL 关键字冲突
    private LocalDateTime timestamp;

    private String buildingId;

    private Double electricity;

    private Double chilledwater;

    private Double airTemperature;

    private Integer hour;

    private Integer dayOfWeek;

    private Integer isWeekend;

    private Double eui;

    private Double cop;

    // --- 以下为非数据库字段，专供大屏预警标红使用 ---
    @TableField(exist = false)
    private String status;
}
