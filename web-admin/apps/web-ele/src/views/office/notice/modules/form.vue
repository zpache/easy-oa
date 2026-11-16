<script lang="ts" setup>
import type { OfficeNoticeApi } from '#/api/office/notice';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { ElMessage, ElMessageBox } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import {
  createNotice,
  getNoticeDetail,
  publishNotice,
  updateNotice,
} from '#/api/office/notice';
import { getDeptList } from '#/api/system/dept';

const emit = defineEmits<{ success: [] }>();

/** 编辑时后端列表不带正文，详情需单独拉取 */
const editing = ref<null | OfficeNoticeApi.Notice>(null);
const isEdit = computed(() => !!editing.value?.id);

const getTitle = computed(() => (isEdit.value ? '编辑公告' : '新增公告'));

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: [
    {
      component: 'Input',
      componentProps: { maxlength: 100, placeholder: '请输入公告标题' },
      fieldName: 'title',
      label: '标题',
      rules: 'required',
    },
    {
      component: 'Select',
      componentProps: {
        class: 'w-full',
        options: [
          { label: '通知', value: 'notice' },
          { label: '制度', value: 'policy' },
          { label: '活动', value: 'activity' },
        ],
      },
      defaultValue: 'notice',
      fieldName: 'category',
      label: '分类',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: '全员', value: 'all' },
          { label: '指定部门', value: 'dept' },
        ],
      },
      defaultValue: 'all',
      fieldName: 'scopeType',
      label: '投放范围',
    },
    {
      component: 'ApiTreeSelect',
      componentProps: {
        allowClear: true,
        api: getDeptList,
        childrenField: 'children',
        class: 'w-full',
        labelField: 'name',
        multiple: true,
        valueField: 'id',
      },
      dependencies: {
        show: (values) => values?.scopeType === 'dept',
        triggerFields: ['scopeType'],
      },
      fieldName: 'deptIds',
      help: '选择的部门（含子部门员工）可见该公告',
      label: '可见部门',
    },
    {
      component: 'Textarea',
      componentProps: { autosize: { maxRows: 20, minRows: 10 }, placeholder: '支持 HTML 富文本内容' },
      fieldName: 'content',
      label: '正文',
      rules: 'required',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: '普通', value: 0 },
          { label: '置顶', value: 1 },
        ],
      },
      defaultValue: 0,
      fieldName: 'top',
      label: '是否置顶',
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        format: 'YYYY-MM-DD HH:mm:ss',
        placeholder: '留空表示长期有效',
        type: 'datetime',
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
      },
      fieldName: 'expireAt',
      label: '有效期至',
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
    if (values.scopeType === 'dept' && !(values.deptIds ?? []).length) {
      ElMessage.error('指定部门范围时至少选择一个部门');
      return;
    }
    drawerApi.lock();
    const payload = {
      category: values.category ?? 'notice',
      content: values.content,
      deptIds: values.scopeType === 'dept' ? (values.deptIds ?? []) : [],
      expireAt: values.expireAt || undefined,
      scopeType: values.scopeType,
      title: values.title,
      top: values.top ?? 0,
    };
    try {
      let created: null | { id: number } = null;
      if (isEdit.value) {
        await updateNotice(editing.value!.id, payload);
      } else {
        created = await createNotice(payload);
      }
      // 新建默认进草稿箱：主动引导发布，避免「建完员工看不到」的断链
      if (created?.id) {
        drawerApi.lock(false);
        try {
          await ElMessageBox.confirm(
            '公告已保存为草稿。草稿不会出现在员工端的公告中心，是否立即发布？',
            '发布确认',
            { confirmButtonText: '立即发布', cancelButtonText: '暂不发布', type: 'info' },
          );
          await publishNotice(created.id);
          ElMessage.success('已发布，员工端公告中心可见');
        } catch {
          ElMessage.info('已保存为草稿，可在列表中点击「发布」上线');
        }
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
    const data = drawerApi.getData<OfficeNoticeApi.Notice>();
    formApi.resetForm();
    if (data?.id) {
      // 列表行不带正文，拉一次详情
      const detail = await getNoticeDetail(data.id);
      editing.value = detail;
      formApi.setValues({
        category: detail.category,
        content: detail.content,
        deptIds: detail.deptIds ?? [],
        expireAt: detail.expireAt,
        scopeType: detail.scopeType,
        title: detail.title,
        top: detail.top,
      });
    } else {
      editing.value = null;
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle" class="w-[640px]">
    <Form />
  </Drawer>
</template>
