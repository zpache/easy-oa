<script lang="ts" setup>
import type { OfficeAssetApi } from '#/api/office/asset';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { createAsset, updateAsset } from '#/api/office/asset';

const emit = defineEmits<{ success: [] }>();

const editing = ref<null | OfficeAssetApi.Asset>(null);
const isEdit = computed(() => !!editing.value?.id);
const getTitle = computed(() => (isEdit.value ? '编辑资产' : '新增资产'));

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: [
    {
      component: 'Input',
      componentProps: {
        maxlength: 40,
        placeholder: '留空自动生成（ZC-日期-序号）',
      },
      fieldName: 'assetNo',
      help: '建议留空由系统生成；存量或特殊编号可手动填写（需全局唯一）',
      label: '资产编号',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 100, placeholder: '如：MacBook Pro 14' },
      fieldName: 'name',
      label: '名称',
      rules: 'required',
    },
    {
      component: 'Select',
      componentProps: {
        class: 'w-full',
        options: [
          { label: '电脑设备', value: 'computer' },
          { label: '外设/办公设备', value: 'device' },
          { label: '家具', value: 'furniture' },
          { label: '电器', value: 'appliance' },
          { label: '其他', value: 'other' },
        ],
        placeholder: '请选择分类',
      },
      defaultValue: 'device',
      fieldName: 'category',
      label: '分类',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 100, placeholder: '如：Apple M3 Pro' },
      fieldName: 'brandModel',
      label: '品牌型号',
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        placeholder: '请选择购置日期',
        type: 'date',
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'purchaseDate',
      label: '购置日期',
    },
    {
      component: 'InputNumber',
      componentProps: {
        class: 'w-full',
        max: 9_999_999,
        min: 0,
        placeholder: '购置价格',
        precision: 2,
      },
      fieldName: 'price',
      label: '价格（元）',
    },
    {
      component: 'Input',
      componentProps: { maxlength: 100, placeholder: '如：3F-工位区' },
      fieldName: 'location',
      label: '存放位置',
    },
    {
      component: 'Textarea',
      componentProps: {
        autosize: { maxRows: 6, minRows: 3 },
        maxlength: 500,
        placeholder: '备注（可选）',
      },
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
    const payload = {
      assetNo: values.assetNo?.trim() || undefined,
      brandModel: values.brandModel || undefined,
      category: values.category,
      location: values.location || undefined,
      name: values.name,
      price: values.price == null ? undefined : Number(values.price),
      purchaseDate: values.purchaseDate || undefined,
      remark: values.remark || undefined,
    };
    drawerApi.lock();
    try {
      if (isEdit.value) {
        await updateAsset(editing.value!.id, payload);
        ElMessage.success('已保存');
      } else {
        const created = await createAsset(payload);
        ElMessage.success(`已新增，资产编号 ${created?.assetNo ?? ''}，状态为「在库」`);
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
    const data = drawerApi.getData<OfficeAssetApi.Asset>();
    formApi.resetForm();
    if (data?.id) {
      editing.value = data;
      formApi.setValues({
        assetNo: data.assetNo,
        brandModel: data.brandModel,
        category: data.category,
        location: data.location,
        name: data.name,
        price: data.price == null ? undefined : Number(data.price),
        purchaseDate: data.purchaseDate,
        remark: data.remark,
      });
    } else {
      editing.value = null;
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[560px]">
    <Form />
  </Drawer>
</template>
