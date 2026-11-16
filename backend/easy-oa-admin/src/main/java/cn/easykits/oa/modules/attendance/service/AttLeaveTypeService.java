package cn.easykits.oa.modules.attendance.service;

import cn.easykits.oa.common.api.ResultCode;
import cn.easykits.oa.common.exception.BusinessException;
import cn.easykits.oa.entity.AttLeaveBalance;
import cn.easykits.oa.entity.AttLeaveType;
import cn.easykits.oa.mapper.AttLeaveBalanceMapper;
import cn.easykits.oa.mapper.AttLeaveTypeMapper;
import cn.easykits.oa.modules.attendance.dto.LeaveTypeForm;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 假期类型管理。
 */
@Service
@RequiredArgsConstructor
public class AttLeaveTypeService extends ServiceImpl<AttLeaveTypeMapper, AttLeaveType> {

    private final AttLeaveBalanceMapper balanceMapper;

    public List<AttLeaveType> listAll() {
        return list(Wrappers.<AttLeaveType>lambdaQuery()
                .orderByAsc(AttLeaveType::getId));
    }

    /** 启用中的类型（员工端下拉用） */
    public List<AttLeaveType> listEnabled() {
        return list(Wrappers.<AttLeaveType>lambdaQuery()
                .eq(AttLeaveType::getStatus, 1)
                .orderByAsc(AttLeaveType::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public AttLeaveType create(LeaveTypeForm form) {
        if (nameExists(form.name(), null)) {
            throw new BusinessException("假期类型名称已存在");
        }
        AttLeaveType type = new AttLeaveType();
        applyForm(type, form);
        type.setStatus(form.status() == null ? 1 : form.status());
        save(type);
        return type;
    }

    @Transactional(rollbackFor = Exception.class)
    public AttLeaveType update(Long id, LeaveTypeForm form) {
        AttLeaveType type = getById(id);
        if (type == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "假期类型不存在");
        }
        if (nameExists(form.name(), id)) {
            throw new BusinessException("假期类型名称已存在");
        }
        applyForm(type, form);
        if (!updateById(type)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return type;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long used = balanceMapper.selectCount(Wrappers.<AttLeaveBalance>lambdaQuery()
                .eq(AttLeaveBalance::getTypeId, id));
        if (used != null && used > 0) {
            throw new BusinessException("已有员工持有该类型假期余额，请先处理余额后删除");
        }
        removeById(id);
    }

    private void applyForm(AttLeaveType type, LeaveTypeForm form) {
        type.setName(form.name().trim());
        type.setRemark(StringUtils.hasText(form.remark()) ? form.remark().trim() : null);
        type.setDefaultQuota(form.defaultQuota());
        type.setStatus(form.status() == null ? 1 : form.status());
    }

    private boolean nameExists(String name, Long excludeId) {
        return count(Wrappers.<AttLeaveType>lambdaQuery()
                .eq(AttLeaveType::getName, StringUtils.trimWhitespace(name))
                .ne(excludeId != null, AttLeaveType::getId, excludeId)) > 0;
    }
}
