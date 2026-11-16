<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';

import { Page, useVbenDrawer, useVbenModal } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { ElButton, ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteShift, getShiftList } from '#/api/attendance/attendance';
import { ATTENDANCE_PERMISSIONS } from '#/constants/permissions';

import { useColumns } from './data';
import BindModal from './modules/bind.vue';
import FormDrawer from './modules/form.vue';

const [FormDrawerComp, formDrawerApi] = useVbenDrawer({
  connectedComponent: FormDrawer,
  destroyOnClose: true,
});

const [BindModalComp, bindModalApi] = useVbenModal({
  connectedComponent: BindModal,
  destroyOnClose: true,
});

function onCreate() {
  formDrawerApi.setData(null).open();
}

function onEdit(row: any) {
  formDrawerApi.setData(row).open();
}

function onBind(row: any) {
  bindModalApi.setData(row).open();
}

async function onDelete(row: any) {
  await ElMessageBox.confirm(
    `确认删除班次「${row.name}」吗？绑定中的班次不可删除。`,
    '删除班次',
    { type: 'warning' },
  );
  await deleteShift(row.id);
  ElMessage.success('删除成功');
  gridApi.query();
}

function onActionClick({ code, row }: OnActionClickParams<any>) {
  if (code === 'bind') {
    onBind(row);
  } else if (code === 'delete') {
    onDelete(row).catch(() => {});
  } else if (code === 'edit') {
    onEdit(row);
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useColumns(onActionClick),
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: false },
    proxyConfig: {
      ajax: {
        query: async () => {
          const items = await getShiftList();
          return { items, total: items.length };
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
</script>

<template>
  <Page auto-content-height>
    <FormDrawerComp @success="gridApi.query()" />
    <BindModalComp />
    <Grid table-title="班次管理">
      <template #toolbar-tools>
        <ElButton
          v-access:code="ATTENDANCE_PERMISSIONS.shiftCreate"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          新增班次
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
