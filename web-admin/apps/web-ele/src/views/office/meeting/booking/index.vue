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
import { cancelBooking, getBookingPage } from '#/api/office/meeting';
import { OFFICE_PERMISSIONS } from '#/constants/permissions';

import { useColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';

const [FormModal, formModalApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

function onCreate() {
  formModalApi.setData(null).open();
}

async function onCancel(row: OfficeMeetingApi.Booking) {
  await ElMessageBox.confirm(
    `确认取消「${row.roomName}」的「${row.title}」预约吗？`,
    '取消预约',
    { type: 'warning' },
  );
  await cancelBooking(row.id);
  ElMessage.success('预约已取消');
  gridApi.query();
}

function onActionClick({
  code,
  row,
}: OnActionClickParams<OfficeMeetingApi.Booking>) {
  switch (code) {
    case 'cancel': {
      onCancel(row).catch(() => {});
      break;
    }
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(onActionClick),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getBookingPage({
            endDate: formValues?.endDate,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            roomId: formValues?.roomId,
            startDate: formValues?.startDate,
            status: formValues?.status,
          });
        },
      },
    },
    rowConfig: { keyField: 'id' },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions,
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="gridApi.query()" />
    <Grid table-title="会议室预约">
      <template #toolbar-tools>
        <ElButton
          v-access:code="OFFICE_PERMISSIONS.bookingCreate"
          type="primary"
          @click="onCreate"
        >
          <Plus class="size-5" />
          预约会议室
        </ElButton>
      </template>
    </Grid>
  </Page>
</template>
