package cn.easykits.oa.modules.schedule.vo;

import java.time.LocalDateTime;

/**
 * 日程视图对象（日历区间查询返回）。
 * <p>
 * 重复日程在区间内展开为多条：virtual = 1 的条目是展开的虚拟场次，
 * id 仍为母本 ID，编辑时按母本整条编辑。
 *
 * @param id            母本 ID
 * @param title         标题
 * @param type          1 日程 2 待办
 * @param scope         personal / dept
 * @param ownerName     归属人姓名
 * @param mine          是否本人创建
 * @param startTime     场次开始时间（已按重复展开偏移）
 * @param endTime       场次结束时间
 * @param allDay        1 全天
 * @param repeatType    none / daily / weekly / monthly
 * @param remindMinutes 提前提醒分钟数
 * @param location      地点
 * @param color         颜色
 * @param remark        备注
 * @param done          待办完成标记
 * @param virtual       1 虚拟展开场次
 */
public record EventVO(
        Long id,
        String title,
        Integer type,
        String scope,
        String ownerName,
        boolean mine,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer allDay,
        String repeatType,
        Integer remindMinutes,
        String location,
        String color,
        String remark,
        Integer done,
        Integer virtual) {
}
