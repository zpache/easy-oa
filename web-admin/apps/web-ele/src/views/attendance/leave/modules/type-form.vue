<script lang="ts" setup>
import type { AttendanceApi } from '#/api/attendance/attendance';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { createLeaveType, updateLeaveType } from '#/api/attendance/attendance';

const emit = defineEmits<{ success: [] }>();

const editing = ref<null | AttendanceApi.LeaveType>(null);
const isEdit = computed(() => !!editing.value?.id);
const getTitle = computed(() => (isEdit.value ? '编辑假期类型' : '新增假期类型'));

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: [
    {
      component: 'Input',
      componentProps: { maxlength: 30, placeholder: '如：年假' },
      fieldName: 'name',
      label: '名称',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 100, placeholder: '说明（可选）' },
      fieldName: 'remark',
      label: '说明',
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
      defaultValue: 0,
      fieldName: 'defaultQuota',
      help: '单位：天/年。后续给员工初始化余额时的参考值',
      label: '默认额度',
      rules: 'required',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '启用', value: 1 },
          { label: '停用', value: 0 },
        ],
      },
      defaultValue: 1,
      fieldName: 'status',
      label: '状态',
    },
  ],
  showDefaultActions: false,
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const values = await formApi.getValues();
    const payload = {
      defaultQuota: values.defaultQuota ?? 0,
      name: values.name,
      remark: values.remark || undefined,
      status: values.status ?? 1,
    };
    drawerApi.lock();
    try {
      if (isEdit.value) {
        await updateLeaveType(editing.value!.id, payload);
        ElMessage.success('已保存');
      } else {
        await createLeaveType(payload);
        ElMessage.success('已新增假期类型');
      }
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = drawerApi.getData<AttendanceApi.LeaveType>();
    formApi.resetForm();
    if (data?.id) {
      editing.value = data;
      formApi.setValues({
        defaultQuota: Number(data.defaultQuota),
        name: data.name,
        remark: data.remark,
        status: data.status,
      });
    } else {
      editing.value = null;
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[460px]">
    <Form />
  </Drawer>
</template>
