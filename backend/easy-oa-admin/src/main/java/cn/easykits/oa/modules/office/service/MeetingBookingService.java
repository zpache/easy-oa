package cn.easykits.oa.modules.office.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import cn.easykits.oa.common.api.PageResult;
import cn.easykits.oa.common.api.ResultCode;
import cn.easykits.oa.common.context.UserContext;
import cn.easykits.oa.common.exception.BusinessException;
import cn.easykits.oa.entity.OfficeMeetingBooking;
import cn.easykits.oa.entity.OfficeMeetingRoom;
import cn.easykits.oa.entity.SysUser;
import cn.easykits.oa.mapper.OfficeMeetingBookingMapper;
import cn.easykits.oa.mapper.OfficeMeetingRoomMapper;
import cn.easykits.oa.mapper.SysUserMapper;
import cn.easykits.oa.modules.office.dto.MeetingBookingForm;
import cn.easykits.oa.modules.office.vo.MeetingBookingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 会议室预约：创建（冲突检测）、取消（本人或持有 office:booking:cancel 权限者）、查询。
 */
@Service
@RequiredArgsConstructor
public class MeetingBookingService extends ServiceImpl<OfficeMeetingBookingMapper, OfficeMeetingBooking> {

    private static final String CANCEL_AUTHORITY = "office:booking:cancel";

    private final OfficeMeetingRoomMapper roomMapper;
    private final SysUserMapper sysUserMapper;

    public PageResult<MeetingBookingVO> pageQuery(long pageNum, long pageSize,
                                                  Long roomId, LocalDateTime startDate,
                                                  LocalDateTime endDate, Integer status, Long userId) {
        Page<OfficeMeetingBooking> page = page(Page.of(pageNum, pageSize),
                Wrappers.<OfficeMeetingBooking>lambdaQuery()
                        .eq(roomId != null, OfficeMeetingBooking::getRoomId, roomId)
                        .ge(startDate != null, OfficeMeetingBooking::getStartAt, startDate)
                        .le(endDate != null, OfficeMeetingBooking::getStartAt, endDate)
                        .eq(status != null, OfficeMeetingBooking::getStatus, status)
                        .eq(userId != null, OfficeMeetingBooking::getUserId, userId)
                        // 列表按申请（提交）时间倒序，最新的预约排在最前；
                        // id 兜底保证同一秒内创建的记录顺序稳定。
                        // 日历视图不依赖后端顺序（前端按 startAt 自行排序并铺排）。
                        .orderByDesc(OfficeMeetingBooking::getCreatedAt)
                        .orderByDesc(OfficeMeetingBooking::getId));
        return PageResult.of(page, this::toVO);
    }

    /**
     * 创建预约：房间可预约 + 时间合法 + 与未取消的预约无时间重叠。
     */
    public OfficeMeetingBooking create(MeetingBookingForm form) {
        OfficeMeetingRoom room = roomMapper.selectById(form.roomId());
        if (room == null || !Integer.valueOf(1).equals(room.getStatus())) {
            throw new BusinessException("会议室不存在或已停用");
        }
        if (!form.endAt().isAfter(form.startAt())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        long conflict = count(Wrappers.<OfficeMeetingBooking>lambdaQuery()
                .eq(OfficeMeetingBooking::getRoomId, form.roomId())
                .eq(OfficeMeetingBooking::getStatus, 1)
                .lt(OfficeMeetingBooking::getStartAt, form.endAt())
                .gt(OfficeMeetingBooking::getEndAt, form.startAt()));
        if (conflict > 0) {
            throw new BusinessException("该时段已被预约，请换一个时间段或会议室");
        }
        OfficeMeetingBooking booking = new OfficeMeetingBooking();
        booking.setRoomId(form.roomId());
        booking.setTitle(form.title());
        booking.setUserId(requireUserId());
        booking.setParticipants(form.participants());
        booking.setStartAt(form.startAt());
        booking.setEndAt(form.endAt());
        booking.setStatus(1);
        save(booking);
        return booking;
    }

    /**
     * 取消预约：本人可取消自己的；取消他人的需要 office:booking:cancel 权限（端点已拦截，
     * 这里再校验一次，防止仅登录用户直接调接口取消别人的预约）。
     */
    public void cancel(Long id) {
        OfficeMeetingBooking booking = getById(id);
        if (booking == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "预约单不存在");
        }
        if (Objects.equals(booking.getStatus(), 0)) {
            return;
        }
        if (!Objects.equals(booking.getUserId(), requireUserId()) && !hasCancelAuthority()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能取消自己的预约");
        }
        booking.setStatus(0);
        if (!updateById(booking)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    private boolean hasCancelAuthority() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> CANCEL_AUTHORITY.equals(a.getAuthority()));
    }

    private Long requireUserId() {
        return UserContext.require().userId();
    }

    private MeetingBookingVO toVO(OfficeMeetingBooking booking) {
        OfficeMeetingRoom room = roomMapper.selectById(booking.getRoomId());
        SysUser user = booking.getUserId() == null ? null : sysUserMapper.selectById(booking.getUserId());
        return new MeetingBookingVO(booking.getId(), booking.getRoomId(),
                room == null ? null : room.getName(), booking.getTitle(),
                booking.getUserId(), user == null ? null : user.getNickname(),
                booking.getParticipants(), booking.getStartAt(), booking.getEndAt(),
                booking.getStatus(), booking.getCreatedAt());
    }
}
