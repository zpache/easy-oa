import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { OfficeMeetingApi } from '#/api/office/meeting';

import { getRoomList } from '#/api/office/meeting';
import { usePermission } from '#/composables/use-permission';
import { OFFICE_PERMISSIONS } from '#/constants/permissions';

/**
 * 查询表单
 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'ApiSelect',
      componentProps: {
        allowClear: true,
        api: getRoomList,
        class: 'w-full',
        labelField: 'name',
        valueField: 'id',
      },
      fieldName: 'roomId',
      label: '会议室',
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        format: 'YYYY-MM-DD HH:mm:ss',
        type: 'datetime',
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
      },
      fieldName: 'startDate',
      label: '开始时间',
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        format: 'YYYY-MM-DD HH:mm:ss',
        type: 'datetime',
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
      },
      fieldName: 'endDate',
      label: '结束时间',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '已预约（未结束）', value: 'active' },
          { label: '已结束', value: 'ended' },
          { label: '已取消', value: 'cancelled' },
        ],
      },
      fieldName: 'state',
      label: '状态',
    },
  ];
}

/**
 * 预约展示状态：由结束时间推导，后端返回（cancelled / ended / ongoing / upcoming）。
 * 表格标签与详情弹窗共用，避免两处各写一套判断。
 */
export const BOOKING_STATE_META = {
  cancelled: { label: '已取消', tagType: 'info' },
  ended: { label: '已结束', tagType: 'info' },
  ongoing: { label: '进行中', tagType: 'primary' },
  upcoming: { label: '已预约', tagType: 'success' },
} as const;

/** 后端 state 为空（老数据/异常）时按「已预约」兜底 */
export function bookingStateMeta(state?: string) {
  const key = (state ?? 'upcoming') as keyof typeof BOOKING_STATE_META;
  return BOOKING_STATE_META[key] ?? BOOKING_STATE_META.upcoming;
}

/** 只有未结束且未取消的预约可以取消 */
export function canCancelBooking(state?: string) {
  return state === 'ongoing' || state === 'upcoming';
}

/** CellTag 的 color 取值（映射到 ElTag type）：default/error/processing/success/warning */
const BOOKING_STATE_OPTIONS = Object.entries(BOOKING_STATE_META).map(
  ([value, meta]) => ({
    color:
      meta.tagType === 'primary'
        ? 'processing'
        : meta.tagType === 'success'
          ? 'success'
          : 'default',
    label: meta.label,
    value,
  }),
);

/**
 * 预约单表格列
 */
export function useColumns(
  onActionClick: OnActionClickFn<OfficeMeetingApi.Booking>,
): VxeTableGridColumns {
  const { can } = usePermission();
  const canCancel = can(OFFICE_PERMISSIONS.bookingCancel);
  const canCreate = can(OFFICE_PERMISSIONS.bookingCreate);

  return [
    {
      field: 'roomName',
      minWidth: 130,
      title: '会议室',
    },
    {
      field: 'title',
      minWidth: 200,
      title: '会议主题',
    },
    {
      field: 'userName',
      title: '预约人',
      width: 100,
    },
    {
      field: 'participants',
      minWidth: 160,
      title: '参与人',
    },
    {
      field: 'startAt',
      title: '开始时间',
      width: 170,
    },
    {
      field: 'endAt',
      title: '结束时间',
      width: 170,
    },
    {
      field: 'createdAt',
      title: '申请时间',
      width: 170,
    },
    {
      align: 'center',
      cellRender: {
        name: 'CellTag',
        options: BOOKING_STATE_OPTIONS,
      },
      field: 'state',
      title: '状态',
      width: 100,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'title',
          nameTitle: '预约单',
          onClick: onActionClick,
        },
        name: 'CellOperation',
        options: [
          {
            code: 'cancel',
            show: (row: OfficeMeetingApi.Booking) =>
              (canCancel || canCreate) && canCancelBooking(row.state),
            text: '取消',
          },
        ],
      },
      field: 'operation',
      fixed: 'right',
      title: '操作',
      visible: canCancel || canCreate,
      width: 120,
    },
  ];
}
