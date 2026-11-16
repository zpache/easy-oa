<script lang="ts" setup>
import type { OfficeMeetingApi } from '#/api/office/meeting';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createRoom, updateRoom } from '#/api/office/meeting';

const emit = defineEmits<{ success: [] }>();

const formData = ref<null | OfficeMeetingApi.MeetingRoom>(null);
const isEdit = computed(() => !!formData.value?.id);

const getTitle = computed(() => (isEdit.value ? '编辑会议室' : '新增会议室'));

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: [
    {
      component: 'Input',
      componentProps: { maxlength: 50, placeholder: '如：大会议室 A' },
      fieldName: 'name',
      label: '名称',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 100, placeholder: '如：3F-东侧' },
      fieldName: 'location',
      label: '位置',
    },
    {
      component: 'InputNumber',
      componentProps: { class: 'w-full', max: 999, min: 1 },
      fieldName: 'capacity',
      label: '容纳人数',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 255, placeholder: '如：投屏,白板,视频会议' },
      fieldName: 'equipment',
      label: '设备说明',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: '可预约', value: 1 },
          { label: '停用', value: 0 },
        ],
      },
      defaultValue: 1,
      fieldName: 'status',
      label: '状态',
    },
    {
      component: 'Textarea',
      componentProps: { maxlength: 255, rows: 3 },
      fieldName: 'remark',
      label: '备注',
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
    drawerApi.lock();
    const payload = {
      capacity: values.capacity,
      equipment: values.equipment,
      location: values.location,
      name: values.name,
      remark: values.remark,
      status: values.status ?? 1,
    };
    try {
      await (isEdit.value
        ? updateRoom(formData.value!.id, payload)
        : createRoom(payload));
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
    const data = drawerApi.getData<OfficeMeetingApi.MeetingRoom>();
    formApi.resetForm();
    formData.value = data && data.id ? data : null;
    if (data?.id) {
      formApi.setValues({
        capacity: data.capacity,
        equipment: data.equipment,
        location: data.location,
        name: data.name,
        remark: data.remark,
        status: data.status ?? 1,
      });
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[520px]">
    <Form />
  </Drawer>
</template>
