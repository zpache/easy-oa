<script lang="ts" setup>
import type { SchEventApi } from '#/api/schedule/schedule';

import { computed, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElCalendar,
  ElCheckbox,
  ElDatePicker,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElRadioGroup,
  ElRadioButton,
  ElSelect,
  ElSwitch,
} from 'element-plus';

import {
  createEvent,
  deleteEvent,
  getEventRange,
  toggleEventDone,
  updateEvent,
} from '#/api/schedule/schedule';

type ScopeFilter = 'all' | 'dept' | 'personal';

interface FormState {
  allDay: boolean;
  color: string;
  endTime: null | string;
  id: null | number;
  location: string;
  remindMinutes: null | number;
  remark: string;
  repeatType: string;
  repeatUntil: null | string;
  scope: 'dept' | 'personal';
  startTime: null | string;
  title: string;
  type: 1 | 2;
}

const COLORS = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9B59B6'];

const typeFilter = ref<null | number>(null);
const scopeFilter = ref<ScopeFilter>('all');
const events = ref<SchEventApi.Event[]>([]);
const currentDate = ref(new Date());

/** 弹窗表单 */
const dialogVisible = ref(false);
const saving = ref(false);
const form = ref<FormState>(emptyForm());
const doneChecked = ref(false);

function emptyForm(): FormState {
  return {
    allDay: false,
    color: '#409EFF',
    endTime: null,
    id: null,
    location: '',
    remindMinutes: null,
    remark: '',
    repeatType: 'none',
    repeatUntil: null,
    scope: 'personal',
    startTime: null,
    title: '',
    type: 1,
  };
}

function pad(n: number) {
  return n < 10 ? `0${n}` : `${n}`;
}

function dayKey(iso: string) {
  return iso.slice(0, 10);
}

function fmtTime(iso: string) {
  return iso.slice(11, 16);
}

async function loadEvents() {
  const base = currentDate.value;
  // 取当月前后各扩一天，覆盖周末跨月的周视图场景
  const start = `${base.getFullYear()}-${pad(base.getMonth() + 1)}-01T00:00:00`;
  const endDate = new Date(base.getFullYear(), base.getMonth() + 1, 0);
  const end = `${endDate.getFullYear()}-${pad(endDate.getMonth() + 1)}-${pad(endDate.getDate())}T23:59:59`;
  events.value = await getEventRange(start, end, typeFilter.value ?? undefined);
}

watch([currentDate, typeFilter], () => loadEvents().catch(() => {}));
loadEvents();

const filteredEvents = computed(() => {
  if (scopeFilter.value === 'all') {
    return events.value;
  }
  return events.value.filter((e) => e.scope === scopeFilter.value);
});

function eventsOfDay(day: string) {
  return filteredEvents.value.filter((e) => dayKey(e.startTime) === day);
}

function eventBg(e: SchEventApi.Event) {
  if (e.done === 1) {
    return '#f0f1f5';
  }
  return e.color || '#409EFF';
}

function openCreate(day?: string) {
  const base = day ?? new Date().toISOString().slice(0, 10);
  form.value = {
    ...emptyForm(),
    startTime: `${base}T09:00:00`,
    endTime: `${base}T10:00:00`,
    repeatUntil: base,
  };
  dialogVisible.value = true;
}

function onEventClick(e: SchEventApi.Event, evt: MouseEvent) {
  evt.stopPropagation();
  if (e.virtual === 1) {
    ElMessageBox.confirm(
      '这是重复日程的一次虚拟场次，编辑将作用于整条重复日程。',
      '重复日程',
      { type: 'info' },
    )
      .then(() => openEdit(e))
      .catch(() => {});
    return;
  }
  openEdit(e);
}

function openEdit(e: SchEventApi.Event) {
  form.value = {
    allDay: e.allDay === 1,
    color: e.color || '#409EFF',
    endTime: e.endTime,
    id: e.id,
    location: e.location ?? '',
    remindMinutes: e.remindMinutes ?? null,
    remark: e.remark ?? '',
    repeatType: e.repeatType || 'none',
    repeatUntil: dayKey(e.startTime),
    scope: e.scope,
    startTime: e.startTime,
    title: e.title,
    type: e.type,
  };
  doneChecked.value = e.done === 1;
  dialogVisible.value = true;
}

async function onSave() {
  const f = form.value;
  if (!f.title.trim()) {
    ElMessage.error('请填写标题');
    return;
  }
  if (!f.startTime || !f.endTime) {
    ElMessage.error('请选择起止时间');
    return;
  }
  if (f.startTime > f.endTime) {
    ElMessage.error('结束时间必须晚于开始时间');
    return;
  }
  if (f.repeatType !== 'none' && !f.repeatUntil) {
    ElMessage.error('设置重复时必须选择重复截止日期');
    return;
  }
  saving.value = true;
  const payload: SchEventApi.EventPayload = {
    allDay: f.allDay ? 1 : 0,
    color: f.color,
    endTime: f.allDay
      ? `${dayKey(f.endTime)}T23:59:59`
      : f.endTime,
    location: f.location || undefined,
    remindMinutes: f.remindMinutes,
    remark: f.remark || undefined,
    repeatType: f.repeatType,
    repeatUntil:
      f.repeatType === 'none' || !f.repeatUntil ? undefined : f.repeatUntil,
    scope: f.scope,
    startTime: f.allDay
      ? `${dayKey(f.startTime)}T00:00:00`
      : f.startTime,
    title: f.title.trim(),
    type: f.type,
  };
  try {
    if (f.id) {
      await updateEvent(f.id, payload);
    } else {
      await createEvent(payload);
    }
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    await loadEvents();
  } finally {
    saving.value = false;
  }
}

async function onDelete() {
  if (!form.value.id) {
    return;
  }
  await ElMessageBox.confirm('确认删除该日程吗？', '删除日程', { type: 'warning' });
  await deleteEvent(form.value.id);
  ElMessage.success('删除成功');
  dialogVisible.value = false;
  await loadEvents();
}
</script>

<template>
  <Page>
    <div class="flex items-center justify-between px-2 pb-3">
      <div class="flex items-center gap-3">
        <ElRadioGroup v-model="scopeFilter" size="small">
          <ElRadioButton value="all">全部</ElRadioButton>
          <ElRadioButton value="personal">我的</ElRadioButton>
          <ElRadioButton value="dept">部门共享</ElRadioButton>
        </ElRadioGroup>
        <ElSelect
          v-model="typeFilter"
          class="w-28"
          clearable
          placeholder="全部类型"
          size="small"
        >
          <ElOption :value="1" label="日程" />
          <ElOption :value="2" label="待办" />
        </ElSelect>
      </div>
      <ElButton type="primary" @click="openCreate()">新建日程</ElButton>
    </div>

    <ElCalendar v-model="currentDate">
      <template #date-cell="{ data }">
        <div class="min-h-[64px]" @dblclick="openCreate(data.day)">
          <div class="flex items-center justify-between">
            <span
              :class="{
                'text-red-500': data.type === 'current-month' && data.isSelected,
              }"
            >
              {{ Number(data.day.slice(8)) }}
            </span>
            <ElButton
              class="!p-0 !h-4 !w-4 text-xs"
              link
              type="primary"
              @click.stop="openCreate(data.day)"
            >
              +
            </ElButton>
          </div>
          <div class="mt-1 flex flex-col gap-0.5">
            <div
              v-for="e in eventsOfDay(data.day).slice(0, 3)"
              :key="`${e.id}-${e.startTime}`"
              class="truncate rounded px-1 py-0.5 text-xs text-white cursor-pointer"
              :style="{ backgroundColor: eventBg(e) }"
              @click="onEventClick(e, $event)"
            >
              {{ e.type === 2 && e.done === 1 ? '✓ ' : '' }}
              {{ e.allDay === 1 ? '全天' : fmtTime(e.startTime) }}
              {{ e.title }}
            </div>
            <span
              v-if="eventsOfDay(data.day).length > 3"
              class="text-xs text-gray-400"
            >
              还有 {{ eventsOfDay(data.day).length - 3 }} 项
            </span>
          </div>
        </div>
      </template>
    </ElCalendar>

    <ElDialog
      v-model="dialogVisible"
      :title="form.id ? '编辑日程' : '新建日程'"
      width="560px"
    >
      <ElForm label-width="90px">
        <ElFormItem label="标题" required>
          <ElInput v-model="form.title" maxlength="100" placeholder="请输入标题" />
        </ElFormItem>
        <ElFormItem label="类型" required>
          <ElRadioGroup v-model="form.type">
            <ElRadioButton :value="1">日程</ElRadioButton>
            <ElRadioButton :value="2">待办</ElRadioButton>
          </ElRadioGroup>
          <ElCheckbox
            v-if="form.type === 2 && form.id"
            v-model="doneChecked"
            class="ml-3"
            @change="() => toggleEventDone(form.id!).then(() => loadEvents())"
          >
            已完成
          </ElCheckbox>
        </ElFormItem>
        <ElFormItem label="范围">
          <ElRadioGroup v-model="form.scope">
            <ElRadioButton value="personal">仅自己</ElRadioButton>
            <ElRadioButton value="dept">部门共享</ElRadioButton>
          </ElRadioGroup>
        </ElFormItem>
        <ElFormItem label="全天">
          <ElSwitch v-model="form.allDay" />
        </ElFormItem>
        <ElFormItem label="开始" required>
          <ElDatePicker
            v-model="form.startTime"
            :disabled="form.allDay"
            class="!w-full"
            format="YYYY-MM-DD HH:mm"
            placeholder="选择开始时间"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
          <ElDatePicker
            v-if="form.allDay"
            v-model="form.startTime"
            class="!w-full"
            format="YYYY-MM-DD"
            placeholder="选择日期"
            type="date"
            value-format="YYYY-MM-DDT00:00:00"
          />
        </ElFormItem>
        <ElFormItem label="结束" required>
          <ElDatePicker
            v-model="form.endTime"
            :disabled="form.allDay"
            class="!w-full"
            format="YYYY-MM-DD HH:mm"
            placeholder="选择结束时间"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
          <ElDatePicker
            v-if="form.allDay"
            v-model="form.endTime"
            class="!w-full"
            format="YYYY-MM-DD"
            placeholder="选择日期"
            type="date"
            value-format="YYYY-MM-DDT23:59:59"
          />
        </ElFormItem>
        <ElFormItem label="重复">
          <ElSelect v-model="form.repeatType" class="!w-32">
            <ElOption value="none" label="不重复" />
            <ElOption value="daily" label="每天" />
            <ElOption value="weekly" label="每周" />
            <ElOption value="monthly" label="每月" />
          </ElSelect>
          <ElDatePicker
            v-if="form.repeatType !== 'none'"
            v-model="form.repeatUntil"
            class="!w-44 ml-2"
            format="YYYY-MM-DD"
            placeholder="重复截止日期"
            type="date"
            value-format="YYYY-MM-DD"
          />
        </ElFormItem>
        <ElFormItem label="提醒">
          <ElInputNumber
            v-model="form.remindMinutes"
            :min="0"
            :step="5"
            controls-position="right"
            placeholder="分钟"
          />
          <span class="ml-2 text-xs text-gray-400">分钟前提醒（预留字段）</span>
        </ElFormItem>
        <ElFormItem label="地点">
          <ElInput v-model="form.location" maxlength="100" placeholder="选填" />
        </ElFormItem>
        <ElFormItem label="颜色">
          <div class="flex gap-2">
            <span
              v-for="c in COLORS"
              :key="c"
              class="inline-block size-5 rounded-full cursor-pointer border-2"
              :class="form.color === c ? 'border-black' : 'border-transparent'"
              :style="{ backgroundColor: c }"
              @click="form.color = c"
            ></span>
          </div>
        </ElFormItem>
        <ElFormItem label="备注">
          <ElInput
            v-model="form.remark"
            maxlength="200"
            placeholder="选填"
            type="textarea"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton v-if="form.id" type="danger" plain @click="onDelete">
          删除
        </ElButton>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton :loading="saving" type="primary" @click="onSave">
          保存
        </ElButton>
      </template>
    </ElDialog>
  </Page>
</template>
