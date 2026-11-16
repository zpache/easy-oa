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
          { label: '已预约', value: 1 },
          { label: '已取消', value: 0 },
        ],
      },
      fieldName: 'status',
      label: '状态',
    },
  ];
}

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
      cellRender: {
        name: 'CellTag',
      },
      field: 'status',
      formatter: ({ row }) => (row.status === 1 ? '已预约' : '已取消'),
      title: '状态',
      width: 90,
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
              (canCancel || canCreate) && row.status === 1,
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
