package cn.easykits.oa.modules.attendance.service;

import cn.easykits.oa.common.api.ResultCode;
import cn.easykits.oa.common.exception.BusinessException;
import cn.easykits.oa.entity.AttShift;
import cn.easykits.oa.entity.AttUserShift;
import cn.easykits.oa.mapper.AttShiftMapper;
import cn.easykits.oa.mapper.AttUserShiftMapper;
import cn.easykits.oa.modules.attendance.dto.BindForm;
import cn.easykits.oa.modules.attendance.dto.ShiftForm;
import cn.easykits.oa.modules.attendance.vo.ShiftVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 考勤班次管理：班次档案 + 员工绑定 + 默认班次解析。
 */
@Service
@RequiredArgsConstructor
public class AttShiftService extends ServiceImpl<AttShiftMapper, AttShift> {

    private final AttUserShiftMapper userShiftMapper;

    public List<ShiftVO> listAll() {
        List<AttShift> shifts = list(Wrappers.<AttShift>lambdaQuery()
                .orderByDesc(AttShift::getIsDefault)
                .orderByAsc(AttShift::getId));
        Map<Long, Long> boundCounts = userShiftMapper.selectList(Wrappers.emptyWrapper()).stream()
                .collect(Collectors.groupingBy(AttUserShift::getShiftId, Collectors.counting()));
        return shifts.stream()
                .map(s -> ShiftVO.of(s, boundCounts.getOrDefault(s.getId(), 0L).intValue()))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public AttShift create(ShiftForm form) {
        if (nameExists(form.name(), null)) {
            throw new BusinessException("班次名称已存在");
        }
        AttShift shift = new AttShift();
        applyForm(shift, form);
        shift.setStatus(form.status() == null ? 1 : form.status());
        save(shift);
        if (Integer.valueOf(1).equals(shift.getIsDefault())) {
            clearOtherDefault(shift.getId());
        }
        return shift;
    }

    @Transactional(rollbackFor = Exception.class)
    public AttShift update(Long id, ShiftForm form) {
        AttShift shift = getById(id);
        if (shift == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "班次不存在");
        }
        if (nameExists(form.name(), id)) {
            throw new BusinessException("班次名称已存在");
        }
        applyForm(shift, form);
        if (!updateById(shift)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        if (Integer.valueOf(1).equals(shift.getIsDefault())) {
            clearOtherDefault(shift.getId());
        }
        return shift;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AttShift shift = getById(id);
        if (shift == null) {
            return;
        }
        Long bound = userShiftMapper.selectCount(Wrappers.<AttUserShift>lambdaQuery()
                .eq(AttUserShift::getShiftId, id));
        if (bound != null && bound > 0) {
            throw new BusinessException("该班次已绑定 " + bound + " 名员工，请先解除绑定");
        }
        removeById(id);
    }

    /**
     * 绑定员工班次；shiftId 为空表示解除绑定，回落到默认班次。
     */
    @Transactional(rollbackFor = Exception.class)
    public void bind(BindForm form) {
        if (form.shiftId() == null) {
            userShiftMapper.delete(Wrappers.<AttUserShift>lambdaQuery()
                    .eq(AttUserShift::getUserId, form.userId()));
            return;
        }
        AttShift shift = getById(form.shiftId());
        if (shift == null || !Integer.valueOf(1).equals(shift.getStatus())) {
            throw new BusinessException("班次不存在或已停用");
        }
        AttUserShift existing = userShiftMapper.selectOne(Wrappers.<AttUserShift>lambdaQuery()
                .eq(AttUserShift::getUserId, form.userId()));
        if (existing == null) {
            AttUserShift binding = new AttUserShift();
            binding.setUserId(form.userId());
            binding.setShiftId(form.shiftId());
            userShiftMapper.insert(binding);
        } else if (!Objects.equals(existing.getShiftId(), form.shiftId())) {
            existing.setShiftId(form.shiftId());
            userShiftMapper.updateById(existing);
        }
    }

    /**
     * 解析员工当日生效班次：优先个人绑定，其次默认班次；都没有返回 null（由调用方决定是否报错）。
     */
    public AttShift resolveShiftForUser(Long userId) {
        AttUserShift binding = userShiftMapper.selectOne(Wrappers.<AttUserShift>lambdaQuery()
                .eq(AttUserShift::getUserId, userId));
        if (binding != null) {
            AttShift bound = getById(binding.getShiftId());
            if (bound != null && Integer.valueOf(1).equals(bound.getStatus())) {
                return bound;
            }
        }
        return getOne(Wrappers.<AttShift>lambdaQuery()
                .eq(AttShift::getIsDefault, 1)
                .eq(AttShift::getStatus, 1)
                .last("limit 1"));
    }

    public Map<Long, AttShift> shiftMapByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return listByIds(ids).stream()
                .collect(Collectors.toMap(AttShift::getId, Function.identity()));
    }

    private void applyForm(AttShift shift, ShiftForm form) {
        shift.setName(form.name().trim());
        shift.setStartTime(LocalTime.parse(form.startTime()));
        shift.setEndTime(LocalTime.parse(form.endTime()));
        if (!shift.getEndTime().isAfter(shift.getStartTime())) {
            throw new BusinessException("下班时间必须晚于上班时间");
        }
        shift.setFlexibleMinutes(form.flexibleMinutes() == null ? 0 : form.flexibleMinutes());
        shift.setIsDefault(form.isDefault() == null ? 0 : form.isDefault());
        shift.setStatus(form.status() == null ? 1 : form.status());
    }

    private void clearOtherDefault(Long keepId) {
        List<AttShift> others = list(Wrappers.<AttShift>lambdaQuery()
                .eq(AttShift::getIsDefault, 1)
                .ne(AttShift::getId, keepId));
        for (AttShift other : others) {
            other.setIsDefault(0);
            updateById(other);
        }
    }

    private boolean nameExists(String name, Long excludeId) {
        return count(Wrappers.<AttShift>lambdaQuery()
                .eq(AttShift::getName, StringUtils.trimWhitespace(name))
                .ne(excludeId != null, AttShift::getId, excludeId)) > 0;
    }
}
