package cn.easykits.oa.modules.attendance.service;

/**
 * 考勤状态常量：打卡状态与补卡类型。
 */
public final class AttStatus {

    private AttStatus() {
    }

    /** 上班/下班状态：正常 */
    public static final int NORMAL = 0;
    /** 上班状态：迟到 */
    public static final int LATE = 1;
    /** 下班状态：早退 */
    public static final int EARLY = 1;
    /** 补卡回写状态 */
    public static final int CORRECTED = 2;

    /** 补卡类型：补上班卡 */
    public static final int CORRECTION_IN = 1;
    /** 补卡类型：补下班卡 */
    public static final int CORRECTION_OUT = 2;

    /** 补卡审批状态：待审批 / 已通过 / 已驳回 */
    public static final int AUDIT_PENDING = 0;
    public static final int AUDIT_APPROVED = 1;
    public static final int AUDIT_REJECTED = 2;

    public static Integer flexible(cn.easykits.oa.entity.AttShift shift) {
        return shift.getFlexibleMinutes() == null ? 0 : shift.getFlexibleMinutes();
    }
}
