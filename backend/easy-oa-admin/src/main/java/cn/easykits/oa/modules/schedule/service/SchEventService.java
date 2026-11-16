package cn.easykits.oa.modules.schedule.service;

import cn.easykits.oa.common.api.ResultCode;
import cn.easykits.oa.common.context.UserContext;
import cn.easykits.oa.common.exception.BusinessException;
import cn.easykits.oa.entity.SchEvent;
import cn.easykits.oa.entity.SysUser;
import cn.easykits.oa.mapper.SchEventMapper;
import cn.easykits.oa.mapper.SysUserMapper;
import cn.easykits.oa.modules.schedule.dto.EventForm;
import cn.easykits.oa.modules.schedule.vo.EventVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 个人日程 / 待办 + 部门共享日程。
 * <p>
 * 查询为区间模式：私人日程按归属人、部门共享按部门命中，重复日程在区间内展开为虚拟场次
 * （快进算法跳过区间前场次，单条母本最多输出 200 场）；编辑按母本整条编辑。
 */
@Service
@RequiredArgsConstructor
public class SchEventService extends ServiceImpl<SchEventMapper, SchEvent> {

    public static final Integer TYPE_EVENT = 1;
    public static final Integer TYPE_TODO = 2;

    private static final String SCOPE_PERSONAL = "personal";
    private static final String SCOPE_DEPT = "dept";

    /** 单条重复母本在区间内的最大展开场数 */
    private static final int MAX_OCCURRENCES = 200;

    private final SysUserMapper sysUserMapper;

    /* ==================== 查询 ==================== */

    /**
     * 区间查询：我的私人日程 + 我所在部门的共享日程。
     *
     * @param startISO 开始时间（ISO 格式，含）
     * @param endISO   结束时间（ISO 格式，含）
     * @param type     1 日程 2 待办，空 = 全部
     */
    public List<EventVO> listRange(String startISO, String endISO, Integer type) {
        Long userId = requireUserId();
        LocalDateTime start = LocalDateTime.parse(startISO);
        LocalDateTime end = LocalDateTime.parse(endISO);
        SysUser me = sysUserMapper.selectById(userId);

        List<SchEvent> events = list(Wrappers.<SchEvent>lambdaQuery()
                .and(w -> w
                        .eq(SchEvent::getUserId, userId)
                        .or(w2 -> w2
                                .eq(SchEvent::getScope, SCOPE_DEPT)
                                .eq(me != null && me.getDeptId() != null, SchEvent::getDeptId, me.getDeptId())))
                .eq(type != null, SchEvent::getType, type)
                .le(SchEvent::getStartTime, end)
                .ge(SchEvent::getEndTime, start)
                .orderByAsc(SchEvent::getStartTime));

        List<EventVO> result = new ArrayList<>();
        for (SchEvent event : events) {
            if (!StringUtils.hasText(event.getRepeatType())
                    || "none".equals(event.getRepeatType())) {
                result.add(toVO(event, event.getStartTime(), event.getEndTime(), 0));
                continue;
            }
            expand(event, start, end, result);
        }
        return result;
    }

    /**
     * 今日日程/待办（工作台卡片）。
     */
    public List<EventVO> listToday() {
        LocalDate today = LocalDate.now();
        return listRange(today.atStartOfDay().toString(),
                today.plusDays(1).atStartOfDay().toString(), null);
    }

    /* ==================== 写操作（仅创建人） ==================== */

    @Transactional(rollbackFor = Exception.class)
    public SchEvent create(EventForm form) {
        validate(form);
        Long userId = requireUserId();
        SchEvent event = new SchEvent();
        applyForm(event, form);
        event.setUserId(userId);
        if (SCOPE_DEPT.equals(event.getScope())) {
            SysUser me = sysUserMapper.selectById(userId);
            event.setDeptId(me == null ? null : me.getDeptId());
            if (event.getDeptId() == null) {
                throw new BusinessException("你尚未归属部门，无法创建部门共享日程");
            }
        }
        save(event);
        return event;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, EventForm form) {
        SchEvent event = requireOwned(id);
        validate(form);
        applyForm(event, form);
        if (SCOPE_DEPT.equals(event.getScope()) && event.getDeptId() == null) {
            SysUser me = sysUserMapper.selectById(event.getUserId());
            event.setDeptId(me == null ? null : me.getDeptId());
        }
        if (!updateById(event)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireOwned(id);
        removeById(id);
    }

    /**
     * 待办完成/取消完成（仅创建人；日程类型不支持）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleDone(Long id) {
        SchEvent event = requireOwned(id);
        if (!TYPE_TODO.equals(event.getType())) {
            throw new BusinessException("仅待办支持完成标记");
        }
        boolean done = !Integer.valueOf(1).equals(event.getDone());
        event.setDone(done ? 1 : 0);
        event.setDoneAt(done ? LocalDateTime.now() : null);
        if (!updateById(event)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    /* ==================== 私有方法 ==================== */

    /**
     * 重复日程展开：快进到查询区间前最近一次场次，再逐场输出，
     * 落在区间内的场次标记 virtual = 1，单条母本最多输出 MAX_OCCURRENCES 场。
     */
    private void expand(SchEvent event, LocalDateTime rangeStart,
                        LocalDateTime rangeEnd, List<EventVO> out) {
        String repeatType = event.getRepeatType();
        long steps = fastForwardIndex(event.getStartTime(), rangeStart, repeatType);
        LocalDate until = event.getRepeatUntil();
        LocalDateTime occStart = shift(event.getStartTime(), steps, repeatType);
        LocalDateTime occEnd = shift(event.getEndTime(), steps, repeatType);
        for (int i = 0; i < MAX_OCCURRENCES; i++) {
            if (until != null && occStart.toLocalDate().isAfter(until)) {
                break;
            }
            if (occStart.isAfter(rangeEnd)) {
                break;
            }
            if (!occEnd.isBefore(rangeStart)) {
                out.add(toVO(event, occStart, occEnd, 1));
            }
            occStart = shift(occStart, 1, repeatType);
            occEnd = shift(occEnd, 1, repeatType);
        }
    }

    /**
     * 计算母本第一次场次到区间开始之间的步数（向下取整，宁多不少）。
     */
    private long fastForwardIndex(LocalDateTime baseStart, LocalDateTime rangeStart,
                                  String repeatType) {
        if (!rangeStart.isAfter(baseStart)) {
            return 0;
        }
        return switch (repeatType) {
            case "daily" -> Math.max(0, ChronoUnit.DAYS.between(
                    baseStart.toLocalDate(), rangeStart.toLocalDate()) - 1);
            case "weekly" -> ChronoUnit.WEEKS.between(
                    baseStart.toLocalDate(), rangeStart.toLocalDate());
            case "monthly" -> ChronoUnit.MONTHS.between(
                    YearMonth.from(baseStart.toLocalDate()), YearMonth.from(rangeStart.toLocalDate()));
            default -> 0;
        };
    }

    private LocalDateTime shift(LocalDateTime time, long steps, String repeatType) {
        return switch (repeatType) {
            case "daily" -> time.plusDays(steps);
            case "weekly" -> time.plusWeeks(steps);
            case "monthly" -> time.plusMonths(steps);
            default -> time;
        };
    }

    private SchEvent requireOwned(Long id) {
        SchEvent event = getById(id);
        if (event == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "日程不存在");
        }
        if (!event.getUserId().equals(requireUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作自己创建的日程");
        }
        return event;
    }

    private void validate(EventForm form) {
        if (form.startTime().isAfter(form.endTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        String repeatType = StringUtils.hasText(form.repeatType())
                ? form.repeatType() : "none";
        if (!List.of("none", "daily", "weekly", "monthly").contains(repeatType)) {
            throw new BusinessException("重复规则不合法，仅支持 none / daily / weekly / monthly");
        }
        if (!"none".equals(repeatType)) {
            if (form.repeatUntil() == null) {
                throw new BusinessException("设置重复时必须选择重复截止日期");
            }
            if (form.repeatUntil().isBefore(form.startTime().toLocalDate())) {
                throw new BusinessException("重复截止日期不能早于开始日期");
            }
        }
    }

    private void applyForm(SchEvent event, EventForm form) {
        event.setTitle(form.title().trim());
        event.setType(form.type());
        event.setScope(StringUtils.hasText(form.scope()) ? form.scope() : SCOPE_PERSONAL);
        event.setStartTime(form.startTime());
        event.setEndTime(form.endTime());
        event.setAllDay(Integer.valueOf(1).equals(form.allDay()) ? 1 : 0);
        event.setRepeatType(StringUtils.hasText(form.repeatType()) ? form.repeatType() : "none");
        event.setRepeatUntil("none".equals(event.getRepeatType()) ? null : form.repeatUntil());
        event.setRemindMinutes(form.remindMinutes());
        event.setLocation(form.location());
        event.setColor(form.color());
        event.setRemark(form.remark());
    }

    private EventVO toVO(SchEvent event, LocalDateTime start, LocalDateTime end, Integer virtual) {
        SysUser owner = event.getUserId() == null ? null
                : sysUserMapper.selectById(event.getUserId());
        return new EventVO(event.getId(), event.getTitle(), event.getType(), event.getScope(),
                owner == null ? null : owner.getNickname(),
                event.getUserId() != null && event.getUserId().equals(requireUserId()),
                start, end, event.getAllDay(), event.getRepeatType(),
                event.getRemindMinutes(), event.getLocation(), event.getColor(),
                event.getRemark(), event.getDone(), virtual);
    }

    private Long requireUserId() {
        return UserContext.require().userId();
    }
}
