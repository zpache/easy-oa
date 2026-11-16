<script lang="ts" setup>
import type { OfficeAssetApi } from '#/api/office/asset';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import { assignAsset, transferAsset } from '#/api/office/asset';
import { getUserList } from '#/api/system/user';

const emit = defineEmits<{ success: [] }>();

const asset = ref<null | OfficeAssetApi.Asset>(null);
const actionType = ref<'assign' | 'transfer'>('assign');

const getTitle = computed(() =>
  actionType.value === 'assign'
    ? `领用资产：${asset.value?.name ?? ''}`
    : `转移资产：${asset.value?.name ?? ''}`,
);

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: [
    {
      component: 'ApiSelect',
      componentProps: {
        allowClear: true,
        api: async () => {
          const result = await getUserList({ pageNum: 1, pageSize: 500, status: 1 });
          return (result.items ?? []).map((u: any) => ({
            label: `${u.nickname ?? u.username}（${u.deptName ?? '未分配部门'}）`,
            value: u.id,
          }));
        },
        class: 'w-full',
        filterable: true,
        placeholder: '请选择使用人',
        showSearch: true,
      },
      fieldName: 'userId',
      help: '启用状态下的员工',
      label: '使用人',
      rules: 'required',
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

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const values = await formApi.getValues();
    modalApi.lock();
    try {
      await (actionType.value === 'assign'
        ? assignAsset(asset.value!.id, Number(values.userId), values.remark || undefined)
        : transferAsset(asset.value!.id, Number(values.userId), values.remark || undefined));
      ElMessage.success(actionType.value === 'assign' ? '已领用' : '已转移');
      emit('success');
      modalApi.close();
    } finally {
      modalApi.lock(false);
    }
  },
  onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const data = modalApi.getData<{ asset: OfficeAssetApi.Asset; type: 'assign' | 'transfer' }>();
    asset.value = data.asset;
    actionType.value = data.type;
    formApi.resetForm();
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-[480px]">
    <Form />
  </Modal>
</template>
