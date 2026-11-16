<script lang="ts" setup>
import type {
  OnActionClickParams,
  VxeTableGridOptions,
} from '#/adapter/vxe-table';

import { Page } from '@vben/common-ui';

import { ElMessage, ElMessageBox } from 'element-plus';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { auditCorrection, getCorrectionPage } from '#/api/attendance/attendance';

import { useColumns, useGridFormSchema } from './data';

async function onAudit(row: any, status: 1 | 2) {
  const action = status === 1 ? '通过' : '驳回';
  let remark = '';
  if (status === 2) {
    const { value } = await ElMessageBox.prompt(
      `驳回 ${row.userName} ${row.workDate} 的补卡申请，请填写驳回原因：`,
      '驳回补卡',
      { inputPlaceholder: '驳回原因（可选）' },
    );
    remark = value ?? '';
  } else {
    await ElMessageBox.confirm(
      `确认通过 ${row.userName} ${row.workDate} 的补卡申请吗？通过后自动回写打卡记录。`,
      '通过补卡',
      { type: 'warning' },
    );
  }
  await auditCorrection(row.id, { remark: remark || undefined, status });
  ElMessage.success(`已${action}`);
  gridApi.query();
}

function onActionClick({ code, row }: OnActionClickParams<any>) {
  if (code === 'approve') {
    onAudit(row, 1).catch(() => {});
  } else if (code === 'reject') {
    onAudit(row, 2).catch(() => {});
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
          return await getCorrectionPage({
            month: formValues?.month,
            pageNum: page.currentPage,
            pageSize: page.pageSize,
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
    <Grid table-title="补卡审批" />
  </Page>
</template>
