<script lang="ts" setup>
import { useVbenModal } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { adjustBalance, getLeaveTypes } from '#/api/attendance/attendance';
import { getUserList } from '#/api/system/user';

const emit = defineEmits<{ success: [] }>();

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: [
    {
      component: 'ApiSelect',
      componentProps: {
        api: async () => {
          const result = await getUserList({
            pageNum: 1,
            pageSize: 500,
            status: 1,
          });
          return (result.items ?? []).map((u: any) => ({
            label: `${u.nickname ?? u.username}（${u.deptName ?? '未分配部门'}）`,
            value: u.id,
          }));
        },
        class: 'w-full',
        filterable: true,
        placeholder: '请选择员工',
        showSearch: true,
      },
      fieldName: 'userId',
      label: '员工',
      rules: 'required',
    },
    {
      component: 'ApiSelect',
      componentProps: {
        api: async () => {
          const types = await getLeaveTypes();
          return types
            .filter((t: any) => t.status === 1)
            .map((t: any) => ({ label: t.name, value: t.id }));
        },
        class: 'w-full',
        placeholder: '请选择假期类型',
      },
      fieldName: 'typeId',
      label: '假期类型',
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: {
        class: 'w-full',
        max: 999,
        min: 0,
        placeholder: '0',
        precision: 1,
      },
      fieldName: 'totalQuota',
      help: '直接设定总额度（天）；不能小于已用额度',
      label: '总额度（天）',
      rules: 'required',
    },
  ],
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const values = await formApi.getValues();
    modalApi.lock();
    try {
      await adjustBalance({
        totalQuota: Number(values.totalQuota),
        typeId: values.typeId,
        userId: values.userId,
      });
      ElMessage.success('额度已保存');
      emit('success');
      modalApi.close();
    } finally {
      modalApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      formApi.resetForm();
    }
  },
});
</script>

<template>
  <Modal title="设定员工假期额度" class="w-[460px]">
    <Form />
  </Modal>
</template>
