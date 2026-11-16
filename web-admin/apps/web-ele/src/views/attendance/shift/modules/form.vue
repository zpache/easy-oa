<script lang="ts" setup>
import type { AttendanceApi } from '#/api/attendance/attendance';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { createShift, updateShift } from '#/api/attendance/attendance';

const emit = defineEmits<{ success: [] }>();

const editing = ref<null | AttendanceApi.Shift>(null);
const isEdit = computed(() => !!editing.value?.id);
const getTitle = computed(() => (isEdit.value ? '编辑班次' : '新增班次'));

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: [
    {
      component: 'Input',
      componentProps: { maxlength: 30, placeholder: '如：标准班' },
      fieldName: 'name',
      label: '班次名称',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '09:00' },
      fieldName: 'startTime',
      help: '格式 HH:mm',
      label: '上班时间',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '18:00' },
      fieldName: 'endTime',
      help: '格式 HH:mm',
      label: '下班时间',
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: {
        class: 'w-full',
        max: 240,
        min: 0,
        placeholder: '0',
      },
      defaultValue: 0,
      fieldName: 'flexibleMinutes',
      help: '上班晚于「上班时间 + 弹性」记迟到，下班早于「下班时间 - 弹性」记早退',
      label: '弹性分钟数',
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
    {
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '是', value: 1 },
          { label: '否', value: 0 },
        ],
      },
      defaultValue: 0,
      fieldName: 'isDefault',
      help: '未单独绑定班次的员工将使用默认班次打卡',
      label: '设为默认班次',
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
      endTime: values.endTime,
      flexibleMinutes: values.flexibleMinutes ?? 0,
      isDefault: values.isDefault ?? 0,
      name: values.name,
      startTime: values.startTime,
      status: values.status ?? 1,
    };
    drawerApi.lock();
    try {
      if (isEdit.value) {
        await updateShift(editing.value!.id, payload);
        ElMessage.success('已保存');
      } else {
        await createShift(payload);
        ElMessage.success('已新增班次');
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
    const data = drawerApi.getData<AttendanceApi.Shift>();
    formApi.resetForm();
    if (data?.id) {
      editing.value = data;
      formApi.setValues({
        endTime: data.endTime?.slice(0, 5),
        flexibleMinutes: data.flexibleMinutes,
        isDefault: data.isDefault,
        name: data.name,
        startTime: data.startTime?.slice(0, 5),
        status: data.status,
      });
    } else {
      editing.value = null;
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[480px]">
    <Form />
  </Drawer>
</template>
