<script lang="ts" setup>
import { ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { bindShift, getShiftList } from '#/api/attendance/attendance';
import { getUserList } from '#/api/system/user';

const shift = ref<null | { id: number; name: string }>(null);

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
        allowClear: true,
        api: async () => {
          const shifts = await getShiftList();
          return shifts
            .filter((s: any) => s.status === 1)
            .map((s: any) => ({
              label: s.isDefault === 1 ? `${s.name}（默认）` : s.name,
              value: s.id,
            }));
        },
        class: 'w-full',
        placeholder: '留空则使用默认班次',
      },
      fieldName: 'shiftId',
      help: '不选将解除该员工的绑定，回落到默认班次',
      label: '班次',
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
      await bindShift(values.userId, values.shiftId || undefined);
      ElMessage.success('已保存绑定');
      modalApi.close();
    } finally {
      modalApi.lock(false);
    }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    shift.value = modalApi.getData<{ id: number; name: string }>();
    formApi.resetForm();
    if (shift.value) {
      formApi.setValues({ shiftId: shift.value.id });
    }
  },
});
</script>

<template>
  <Modal
    :title="shift ? `绑定员工到「${shift.name}」` : '绑定员工班次'"
    class="w-[460px]"
  >
    <Form />
  </Modal>
</template>
