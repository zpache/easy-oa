package cn.easykits.oa.modules.attendance.service;

import cn.easykits.oa.common.api.PageResult;
import cn.easykits.oa.common.api.ResultCode;
import cn.easykits.oa.common.exception.BusinessException;
import cn.easykits.oa.entity.AttCheckin;
import cn.easykits.oa.entity.AttShift;
import cn.easykits.oa.entity.SysDept;
import cn.easykits.oa.entity.SysUser;
import cn.easykits.oa.mapper.AttCheckinMapper;
import cn.easykits.oa.mapper.SysDeptMapper;
import cn.easykits.oa.mapper.SysUserMapper;
import cn.easykits.oa.modules.attendance.vo.CheckinVO;
import cn.easykits.oa.modules.attendance.vo.MonthStatVO;
import cn.easykits.oa.modules.attendance.vo.MyCheckinVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.easykits.oa.modules.attendance.service.AttStatus.*;

/**
 * 每日打卡：上下班打卡、迟到/早退判定、我的与全员考勤记录、月度统计、补卡回写。
 */
@Service
@RequiredArgsConstructor
public class AttCheckinService extends ServiceImpl<AttCheckinMapper, AttCheckin> {

    private final AttShiftService shiftService;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;

    /**
     * 打卡：当日无记录记上班卡，已有上班无下班记下班卡，两边都有则提示已完成。
     */
    public MyCheckinVO clock(Long userId) {
        AttShift shift = shiftService.resolveShiftForUser(userId);
        if (shift == null) {
            throw new BusinessException("尚未配置班次，请联系管理员先设置考勤班次");
        }
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        AttCheckin record = getByUserAndDate(userId, today);
        if (record == null) {
            record = new AttCheckin();
            record.setUserId(userId);
            record.setWorkDate(today);
            record.setShiftId(shift.getId());
            record.setClockIn(now);
            record.setClockInStatus(now.isAfter(shift.getStartTime()
                    .plusMinutes(flexible(shift))) ? LATE : NORMAL);
            save(record);
        } else if (record.getClockOut() == null) {
            record.setClockOut(now);
            record.setClockOutStatus(now.isBefore(shift.getEndTime()
                    .minusMinutes(flexible(shift))) ? EARLY : NORMAL);
            updateById(record);
        } else {
            throw new BusinessException("今日上下班均已打卡");
        }
        return todayVO(userId, record);
    }

    /**
     * 我的今日考勤状态（打卡页首屏）。
     */
    public MyCheckinVO today(Long userId) {
        AttCheckin record = getByUserAndDate(userId, LocalDate.now());
        return todayVO(userId, record);
    }

    public PageResult<CheckinVO> myPage(Long userId, long pageNum, long pageSize, String month) {
        return toVOPage(page(userId, pageNum, pageSize, month));
    }

    /**
     * 管理端全员考勤记录：可按员工、月份过滤。
     */
    public PageResult<CheckinVO> adminPage(long pageNum, long pageSize, Long userId,
                                           String month, Integer inStatus, Integer outStatus) {
        Page<AttCheckin> page = page(Page.of(pageNum, pageSize),
                Wrappers.<AttCheckin>lambdaQuery()
                        .eq(userId != null, AttCheckin::getUserId, userId)
                        .eq(inStatus != null, AttCheckin::getClockInStatus, inStatus)
                        .eq(outStatus != null, AttCheckin::getClockOutStatus, outStatus)
                        .apply(month != null && !month.isBlank(),
                                "DATE_FORMAT(work_date, '%Y-%m') = {0}", month)
                        .orderByDesc(AttCheckin::getWorkDate)
                        .orderByAsc(AttCheckin::getUserId));
        return toVOPage(page);
    }

    /**
     * 我的月度统计：日历状态 + 迟到/早退/正常/缺卡计数（缺卡按工作日估算）。
     */
    public MonthStatVO monthStat(Long userId, String yearMonth) {
        YearMonth ym = StringUtils.hasText(yearMonth) ? YearMonth.parse(yearMonth) : YearMonth.now();
        List<AttCheckin> records = list(Wrappers.<AttCheckin>lambdaQuery()
                .eq(AttCheckin::getUserId, userId)
                .ge(AttCheckin::getWorkDate, ym.atDay(1))
                .le(AttCheckin::getWorkDate, ym.atEndOfMonth()));
        Map<LocalDate, MonthStatVO.DayStat> days = records.stream()
                .collect(Collectors.toMap(AttCheckin::getWorkDate,
                        r -> new MonthStatVO.DayStat(r.getClockInStatus(), r.getClockOutStatus()),
                        (a, b) -> a));
        int late = 0;
        int early = 0;
        int normal = 0;
        for (AttCheckin r : records) {
            if (Integer.valueOf(LATE).equals(r.getClockInStatus())) {
                late++;
            }
            if (Integer.valueOf(EARLY).equals(r.getClockOutStatus())) {
                early++;
            }
            if (Integer.valueOf(NORMAL).equals(r.getClockInStatus())
                    && Integer.valueOf(NORMAL).equals(r.getClockOutStatus())) {
                normal++;
            }
        }
        // 缺卡：本月已过去的工作日中无打卡记录的天数
        int missing = 0;
        LocalDate today = LocalDate.now();
        for (int d = 1; d <= Math.min(ym.lengthOfMonth(),
                ym.equals(YearMonth.from(today)) ? today.getDayOfMonth() : ym.lengthOfMonth()); d++) {
            LocalDate date = ym.atDay(d);
            if (date.getDayOfWeek().getValue() <= 5 && !days.containsKey(date)) {
                missing++;
            }
        }
        return new MonthStatVO(yearMonth == null || yearMonth.isBlank() ? ym.toString() : yearMonth,
                days, late, early, normal, missing);
    }

    /**
     * 补卡回写：把指定日期的上班/下班卡补为班次时间（状态=补卡）；无记录则补建一条。
     */
    public void applyCorrection(Long userId, LocalDate workDate, Integer type, AttShift fallbackShift) {
        AttCheckin record = getByUserAndDate(userId, workDate);
        AttShift shift = record != null && record.getShiftId() != null
                ? shiftService.getById(record.getShiftId()) : fallbackShift;
        if (shift == null) {
            throw new BusinessException("该日期无生效班次，无法补卡");
        }
        if (record == null) {
            record = new AttCheckin();
            record.setUserId(userId);
            record.setWorkDate(workDate);
            record.setShiftId(shift.getId());
        }
        if (type != null && type == CORRECTION_IN) {
            record.setClockIn(shift.getStartTime());
            record.setClockInStatus(CORRECTED);
        } else {
            record.setClockOut(shift.getEndTime());
            record.setClockOutStatus(CORRECTED);
        }
        saveOrUpdate(record);
    }

    // ------------------------------ 内部 ------------------------------

    private AttCheckin getByUserAndDate(Long userId, LocalDate date) {
        return getOne(Wrappers.<AttCheckin>lambdaQuery()
                .eq(AttCheckin::getUserId, userId)
                .eq(AttCheckin::getWorkDate, date)
                .last("limit 1"));
    }

    private MyCheckinVO todayVO(Long userId, AttCheckin record) {
        AttShift shift = record != null && record.getShiftId() != null
                ? shiftService.getById(record.getShiftId())
                : shiftService.resolveShiftForUser(userId);
        if (shift == null) {
            return new MyCheckinVO(LocalDate.now(), null, null, null, null, null,
                    null, null, null, null, false, false);
        }
        return new MyCheckinVO(record != null ? record.getWorkDate() : LocalDate.now(),
                shift.getId(), shift.getName(), shift.getStartTime(), shift.getEndTime(),
                flexible(shift),
                record != null ? record.getClockIn() : null,
                record != null ? record.getClockInStatus() : null,
                record != null ? record.getClockOut() : null,
                record != null ? record.getClockOutStatus() : null,
                record != null && record.getClockIn() != null,
                record != null && record.getClockOut() != null);
    }

    private Page<AttCheckin> page(Long userId, long pageNum, long pageSize, String month) {
        return page(Page.of(pageNum, pageSize), Wrappers.<AttCheckin>lambdaQuery()
                .eq(AttCheckin::getUserId, userId)
                .apply(month != null && !month.isBlank(),
                        "DATE_FORMAT(work_date, '%Y-%m') = {0}", month)
                .orderByDesc(AttCheckin::getWorkDate));
    }

    private PageResult<CheckinVO> toVOPage(Page<AttCheckin> page) {
        List<Long> userIds = page.getRecords().stream()
                .map(AttCheckin::getUserId).distinct().toList();
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of()
                : sysUserMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, String> deptNames = deptNameMap(users.values().stream()
                .map(SysUser::getDeptId).filter(Objects::nonNull).distinct().toList());
        Map<Long, AttShift> shifts = shiftService.shiftMapByIds(page.getRecords().stream()
                .map(AttCheckin::getShiftId).filter(Objects::nonNull).distinct().toList());
        return PageResult.of(page, r -> {
            SysUser user = users.get(r.getUserId());
            return new CheckinVO(r.getId(), r.getUserId(),
                    user != null ? user.getNickname() : null,
                    user != null ? deptNames.get(user.getDeptId()) : null,
                    r.getWorkDate(), r.getShiftId(),
                    r.getShiftId() != null && shifts.containsKey(r.getShiftId())
                            ? shifts.get(r.getShiftId()).getName() : null,
                    r.getClockIn(), r.getClockInStatus(),
                    r.getClockOut(), r.getClockOutStatus());
        });
    }

    private Map<Long, String> deptNameMap(List<Long> deptIds) {
        if (deptIds.isEmpty()) {
            return Map.of();
        }
        return sysDeptMapper.selectByIds(deptIds).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName));
    }
}
