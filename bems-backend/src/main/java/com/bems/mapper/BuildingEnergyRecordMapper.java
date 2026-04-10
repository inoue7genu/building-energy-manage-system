package com.bems.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bems.entity.BuildingEnergyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * @Auther: inoue
 * @Date: 2026/3/31 - 03 - 31 - 21:14
 * @Description: com.bems.mapper
 * @version: 1.0
 * @Description: 建筑能耗数据 Mapper 接口
 * MyBatis-Plus 会自动帮我们实现基础的增删改查，一行 SQL 都不用写！
 */
@Mapper
public interface BuildingEnergyRecordMapper extends BaseMapper<BuildingEnergyRecord> {

    // 新增：按天分组，计算每日总电耗，用于前端生成热力日历！
    // 🚀 核心升级：使用动态 SQL，支持全局热力台账计算！
    @Select("<script>" +
            "SELECT DATE_FORMAT(`timestamp`, '%Y-%m-%d') AS recordDate, SUM(electricity) AS totalElec " +
            "FROM building_energy_records " +
            "<where>" +
            "  <if test='buildingId != null and buildingId != \"\"'>" +
            "    building_id = #{buildingId} " +
            "  </if>" +
            "</where>" +
            "GROUP BY DATE_FORMAT(`timestamp`, '%Y-%m-%d')" +
            "</script>")
    List<Map<String, Object>> getCalendarSummary(@Param("buildingId") String buildingId);

    // ✨ 新增：图表专用的动态聚合查询
    @Select("<script>" +
            "SELECT DATE_FORMAT(`timestamp`, #{format}) AS timeLabel, " +
            // 🛡️ 骨架指标：永远查出电量，供底层计算 AI 异常状态！
            "SUM(electricity) AS electricity " +
            // 📊 动态指标：前端传了 chilledwater 才去聚合冷冻水
            "<if test='parameters == null or parameters.contains(\"chilledwater\")'>, SUM(chilledwater) AS chilledwater</if> " +
            "FROM building_energy_records " +
            "WHERE building_id = #{buildingId} " +
            "AND `timestamp` >= #{startTime} AND `timestamp` <= #{endTime} " +
            "GROUP BY timeLabel " +
            "ORDER BY timeLabel ASC" +
            "</script>")
    List<Map<String, Object>> getAggregatedChartData(
            @Param("buildingId") String buildingId,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("format") String format,
            // 👇 新增传入参数
            @Param("parameters") List<String> parameters);
}