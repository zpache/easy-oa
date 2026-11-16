<script lang="ts" setup>
import type { OfficeMeetingApi } from '#/api/office/meeting';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { createBooking, getRoomList } from '#/api/office/meeting';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | OfficeMeetingApi.Booking>(null);
const isEdit = computed(() => !!formData.value?.id);

const getTitle = computed(() => (isEdit.value ? '编辑预约' : '预约会议室'));

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: [
    {
      component: 'ApiSelect',
      componentProps: {
        api: getRoomList,
        class: 'w-full',
        labelField: 'name',
        valueField: 'id',
      },
      fieldName: 'roomId',
      help: '仅展示可预约状态的会议室',
      label: '会议室',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 100, placeholder: '如：Q4 产品规划评审' },
      fieldName: 'title',
      label: '会议主题',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 255, placeholder: '如：产品组全员 + 客户 2 人' },
      fieldName: 'participants',
      label: '参与人',
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        format: 'YYYY-MM-DD HH:mm:ss',
        type: 'datetime',
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
      },
      fieldName: 'startAt',
      label: '开始时间',
      rules: 'required',
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        format: 'YYYY-MM-DD HH:mm:ss',
        type: 'datetime',
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
      },
      fieldName: 'endAt',
      label: '结束时间',
      rules: 'required',
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
    if (values.endAt <= values.startAt) {
      ElMessage.error('结束时间必须晚于开始时间');
      return;
    }
    drawerApi.lock();
    const payload = {
      endAt: values.endAt,
      participants: values.participants,
      roomId: values.roomId,
      startAt: values.startAt,
      title: values.title,
    };
    try {
      await createBooking(payload);
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    formApi.resetForm();
    formData.value = null;
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[520px]">
    <Form />
  </Drawer>
</template>
