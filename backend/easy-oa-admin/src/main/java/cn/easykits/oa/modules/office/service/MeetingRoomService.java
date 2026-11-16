package cn.easykits.oa.modules.office.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import cn.easykits.oa.common.api.ResultCode;
import cn.easykits.oa.common.exception.BusinessException;
import cn.easykits.oa.entity.OfficeMeetingBooking;
import cn.easykits.oa.entity.OfficeMeetingRoom;
import cn.easykits.oa.mapper.OfficeMeetingBookingMapper;
import cn.easykits.oa.mapper.OfficeMeetingRoomMapper;
import cn.easykits.oa.modules.office.dto.MeetingRoomForm;
import cn.easykits.oa.modules.office.vo.MeetingRoomVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 会议室档案管理。删除前校验是否存在未取消的预约。
 */
@Service
@RequiredArgsConstructor
public class MeetingRoomService extends ServiceImpl<OfficeMeetingRoomMapper, OfficeMeetingRoom> {

    private final OfficeMeetingBookingMapper bookingMapper;

    public List<MeetingRoomVO> listAll() {
        return list(Wrappers.<OfficeMeetingRoom>lambdaQuery()
                .orderByAsc(OfficeMeetingRoom::getId)).stream()
                .map(this::toVO).toList();
    }

    public OfficeMeetingRoom create(MeetingRoomForm form) {
        if (nameExists(form.name(), null)) {
            throw new BusinessException("会议室名称已存在");
        }
        OfficeMeetingRoom room = new OfficeMeetingRoom();
        applyForm(room, form);
        save(room);
        return room;
    }

    public OfficeMeetingRoom update(Long id, MeetingRoomForm form) {
        OfficeMeetingRoom room = getById(id);
        if (room == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "会议室不存在");
        }
        if (nameExists(form.name(), id)) {
            throw new BusinessException("会议室名称已存在");
        }
        applyForm(room, form);
        if (!updateById(room)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return room;
    }

    public void delete(Long id) {
        OfficeMeetingRoom room = getById(id);
        if (room == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "会议室不存在");
        }
        long activeBookings = bookingMapper.selectCount(Wrappers.<OfficeMeetingBooking>lambdaQuery()
                .eq(OfficeMeetingBooking::getRoomId, id)
                .eq(OfficeMeetingBooking::getStatus, 1));
        if (activeBookings > 0) {
            throw new BusinessException("该会议室存在未取消的预约，无法删除");
        }
        removeById(id);
    }

    private void applyForm(OfficeMeetingRoom room, MeetingRoomForm form) {
        room.setName(form.name());
        room.setLocation(form.location());
        room.setCapacity(form.capacity());
        room.setEquipment(form.equipment());
        room.setStatus(form.status() == null ? 1 : form.status());
        room.setRemark(form.remark());
    }

    private boolean nameExists(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        return count(Wrappers.<OfficeMeetingRoom>lambdaQuery()
                .eq(OfficeMeetingRoom::getName, name)
                .ne(excludeId != null, OfficeMeetingRoom::getId, excludeId)) > 0;
    }

    private MeetingRoomVO toVO(OfficeMeetingRoom room) {
        return new MeetingRoomVO(room.getId(), room.getName(), room.getLocation(),
                room.getCapacity(), room.getEquipment(), room.getStatus(), room.getRemark());
    }
}
