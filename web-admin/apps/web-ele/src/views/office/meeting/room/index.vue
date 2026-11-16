<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';
import type { OfficeMeetingApi } from '#/api/office/meeting';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteRoom, getRoomList } from '#/api/office/meeting';
import { OFFICE_PERMISSIONS } from '#/constants/permissions';
import { usePermission } from '#/composables/use-permission';

import Form from './modules/form.vue';

const { can } = usePermission();
const canUpdate = can(OFFICE_PERMISSIONS.roomUpdate);
const canDelete = can(OFFICE_PERMISSIONS.roomDelete);

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

function onCreate() {
  formDrawerApi.setData(null).open();
}

function onEdit(row: OfficeMeetingApi.MeetingRoom) {
  formDrawerApi.setData(row).open();
}

async function onDelete(row: OfficeMeetingApi.MeetingRoom) {
  await ElMessageBox.confirm(
    `确认删除会议室「${row.name}」吗？存在未取消预约时无法删除。`,
    '删除会议室',
    { type: 'warning' },
  );
  await deleteRoom(row.id);
  ElMessage.success('删除成功');
  refreshGrid();
}

function onActionClick({
  code,
  row,
}: OnActionClickParams<OfficeMeetingApi.MeetingRoom>) {
  switch (code) {
    case 'delete': {
      onDelete(row).catch(() => {});
      break;
    }
    case 'edit': {
      onEdit(row);
      break;
    }
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: [
      { field: 'name', minWidth: 160, title: '名称' },
      { field: 'location', minWidth: 140, title: '位置' },
      { field: 'capacity', title: '容量（人）', width: 100 },
      { field: 'equipment', minWidth: 180, title: '设备' },
      {
        cellRender: {
          name: 'CellTag',
        },
        field: 'status',
        formatter: ({ row }) => (row.status === 1 ? '可预约' : '已停用'),
        title: '状态',
        width: 90,
      },
      { field: 'remark', minWidth: 160, title: '备注' },
      {
        align: 'center',
        cellRender: {
          attrs: {
            nameField: 'name',
            nameTitle: '会议室',
            onClick: onActionClick,
          },
          name: 'CellOperation',
          options: [
            { code: 'edit', show: canUpdate },
            { code: 'delete', show: canDelete },
          ],
        },
        field: 'operation',
        fixed: 'right',
        title: '操作',
        visible: canUpdate || canDelete,
        width: 160,
      },
    ],
    height: 'auto',
    keepSource: true,
    pagerConfig: {
      enabled: false,
    },
    proxyConfig: {
      ajax: {
        query: async () => {
          const list = await getRoomList();
          return { items: list, total: list.length };
        },
      },
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      zoom: true,
    },
  } as VxeTableGridOptions,
});

function refreshGrid() {
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawer @success="refreshGrid" />
    <Grid table-title="会议室管理">
      <template #toolbar-tools>
        <ElButton
          v-access:code="OFFICE_PERMISSIONS.roomCreate"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增会议室
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
