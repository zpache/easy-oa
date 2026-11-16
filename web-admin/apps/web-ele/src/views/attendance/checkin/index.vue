<script lang="ts" setup>
import type { AttendanceApi } from '#/api/attendance/attendance';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  ElButton,
  ElCard,
  ElCol,
  ElDatePicker,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElPagination,
  ElRadio,
  ElRadioGroup,
  ElRow,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import {
  applyCorrection,
  clock,
  getMonthStat,
  getMyRecords,
  getToday,
} from '#/api/attendance/attendance';

const IN_STATUS_TEXT: Record<number, string> = {
  0: '正常',
  1: '迟到',
  2: '补卡',
};
const OUT_STATUS_TEXT: Record<number, string> = {
  0: '正常',
  1: '早退',
  2: '补卡',
};
const STATUS_TAG: Record<number, string> = {
  0: 'success',
  1: 'danger',
  2: 'warning',
};

const today = ref<AttendanceApi.MyCheckin | null>(null);
const stat = ref<AttendanceApi.MonthStat | null>(null);
const statMonth = ref('');
const records = ref<AttendanceApi.Checkin[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = 10;
const recordMonth = ref('');
const clocking = ref(false);

const clockButtonText = computed(() => {
  if (!today.value) {
    return '上班打卡';
  }
  if (!today.value.clockedIn) {
    return '上班打卡';
  }
  if (!today.value.clockedOut) {
    return '下班打卡';
  }
  return '今日已打满';
});

const clockDisabled = computed(
  () => !!today.value && today.value.clockedIn && today.value.clockedOut,
);

/* 补卡申请弹窗 */
const correctionVisible = ref(false);
const correctionForm = ref({
  reason: '',
  type: 1,
  workDate: '',
});

async function loadToday() {
  today.value = await getToday();
}

async function loadStat() {
  stat.value = await getMonthStat(statMonth.value || undefined);
}

async function loadRecords() {
  const result = await getMyRecords({
    month: recordMonth.value || undefined,
    pageNum: pageNum.value,
    pageSize,
  });
  records.value = result.items;
  total.value = result.total;
}

function onMonthChange() {
  pageNum.value = 1;
  loadRecords();
  loadStat();
}

function onStatMonthChange() {
  loadStat();
}

async function onClock() {
  clocking.value = true;
  try {
    const result = await clock();
    ElMessage.success(
      result.clockedOut ? `下班打卡成功 ${result.clockOut ?? ''}` : `上班打卡成功 ${result.clockIn ?? ''}`,
    );
    await loadToday();
    await loadStat();
  } finally {
    clocking.value = false;
  }
}

function openCorrection() {
  correctionForm.value = {
    reason: '',
    type: 1,
    workDate: new Date().toISOString().slice(0, 10),
  };
  correctionVisible.value = true;
}

async function submitCorrection() {
  if (!correctionForm.value.workDate) {
    ElMessage.warning('请选择补卡日期');
    return;
  }
  if (!correctionForm.value.reason.trim()) {
    ElMessage.warning('请填写申请原因');
    return;
  }
  await applyCorrection(correctionForm.value);
  ElMessage.success('补卡申请已提交，等待审批');
  correctionVisible.value = false;
  loadRecords();
}

onMounted(async () => {
  await Promise.all([loadToday(), loadStat(), loadRecords()]);
});
</script>

<template>
  <Page>
    <ElRow :gutter="16">
      <ElCol :span="10">
        <ElCard shadow="never">
          <template #header>
            <span class="font-medium">今日打卡</span>
          </template>
          <div class="flex flex-col items-center gap-4 py-2">
            <div class="text-lg">
              {{ today?.shiftName ?? '未配置班次' }}
              <span v-if="today?.shiftStart" class="text-muted-foreground ml-2 text-sm">
                {{ today.shiftStart?.slice(0, 5) }} - {{ today.shiftEnd?.slice(0, 5) }}
                <template v-if="today.flexibleMinutes">
                  （弹性 {{ today.flexibleMinutes }} 分钟）
                </template>
              </span>
            </div>
            <div class="flex items-center gap-3">
              <span>
                上班：
                <ElTag
                  v-if="today?.clockedIn"
                  :type="(STATUS_TAG[today.clockInStatus ?? 0] as any)"
                  size="small"
                >
                  {{ today.clockIn?.slice(0, 5) }}
                  {{ IN_STATUS_TEXT[today.clockInStatus ?? 0] }}
                </ElTag>
                <span v-else class="text-muted-foreground">未打卡</span>
              </span>
              <span>
                下班：
                <ElTag
                  v-if="today?.clockedOut"
                  :type="(STATUS_TAG[today.clockOutStatus ?? 0] as any)"
                  size="small"
                >
                  {{ today.clockOut?.slice(0, 5) }}
                  {{ OUT_STATUS_TEXT[today.clockOutStatus ?? 0] }}
                </ElTag>
                <span v-else class="text-muted-foreground">未打卡</span>
              </span>
            </div>
            <ElButton
              :disabled="clockDisabled"
              :loading="clocking"
              round
              size="large"
              type="primary"
              @click="onClock"
            >
              {{ clockButtonText }}
            </ElButton>
            <ElButton link type="primary" @click="openCorrection">
              漏打卡？申请补卡
            </ElButton>
          </div>
        </ElCard>
      </ElCol>
      <ElCol :span="14">
        <ElCard shadow="never">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="font-medium">本月统计</span>
              <ElDatePicker
                v-model="statMonth"
                class="w-32"
                clearable
                placeholder="选择月份"
                type="month"
                value-format="YYYY-MM"
                @change="onStatMonthChange"
              />
            </div>
          </template>
          <ElRow :gutter="8">
            <ElCol :span="6">
              <div class="rounded-lg bg-green-50 p-4 text-center dark:bg-green-950">
                <div class="text-2xl font-semibold">{{ stat?.normalCount ?? 0 }}</div>
                <div class="text-muted-foreground text-xs">正常</div>
              </div>
            </ElCol>
            <ElCol :span="6">
              <div class="rounded-lg bg-red-50 p-4 text-center dark:bg-red-950">
                <div class="text-2xl font-semibold">{{ stat?.lateCount ?? 0 }}</div>
                <div class="text-muted-foreground text-xs">迟到</div>
              </div>
            </ElCol>
            <ElCol :span="6">
              <div class="rounded-lg bg-orange-50 p-4 text-center dark:bg-orange-950">
                <div class="text-2xl font-semibold">{{ stat?.earlyCount ?? 0 }}</div>
                <div class="text-muted-foreground text-xs">早退</div>
              </div>
            </ElCol>
            <ElCol :span="6">
              <div class="rounded-lg bg-gray-100 p-4 text-center dark:bg-gray-800">
                <div class="text-2xl font-semibold">{{ stat?.missingCount ?? 0 }}</div>
                <div class="text-muted-foreground text-xs">缺卡（工作日）</div>
              </div>
            </ElCol>
          </ElRow>
        </ElCard>
      </ElCol>
    </ElRow>

    <ElCard class="mt-4" shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">我的打卡记录</span>
          <ElDatePicker
            v-model="recordMonth"
            class="w-32"
            clearable
            placeholder="选择月份"
            type="month"
            value-format="YYYY-MM"
            @change="onMonthChange"
          />
        </div>
      </template>
      <ElTable :data="records" row-key="id">
        <ElTableColumn label="日期" prop="workDate" width="120" />
        <ElTableColumn label="班次" prop="shiftName" width="100" />
        <ElTableColumn label="上班" width="160">
          <template #default="{ row }">
            <ElTag
              v-if="row.clockIn"
              :type="(STATUS_TAG[row.clockInStatus ?? 0] as any)"
              size="small"
            >
              {{ row.clockIn?.slice(0, 5) }} {{ IN_STATUS_TEXT[row.clockInStatus ?? 0] }}
            </ElTag>
            <span v-else class="text-muted-foreground">缺卡</span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="下班" width="160">
          <template #default="{ row }">
            <ElTag
              v-if="row.clockOut"
              :type="(STATUS_TAG[row.clockOutStatus ?? 0] as any)"
              size="small"
            >
              {{ row.clockOut?.slice(0, 5) }}
              {{ OUT_STATUS_TEXT[row.clockOutStatus ?? 0] }}
            </ElTag>
            <span v-else class="text-muted-foreground">缺卡</span>
          </template>
        </ElTableColumn>
      </ElTable>
      <div class="mt-3 flex justify-end">
        <ElPagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          background
          layout="total, prev, pager, next"
          @current-change="loadRecords"
        />
      </div>
    </ElCard>

    <ElDialog v-model="correctionVisible" title="补卡申请" width="480px">
      <ElForm label-width="90px">
        <ElFormItem label="补卡日期">
          <ElDatePicker
            v-model="correctionForm.workDate"
            class="!w-full"
            placeholder="选择日期"
            type="date"
            value-format="YYYY-MM-DD"
          />
        </ElFormItem>
        <ElFormItem label="补哪次卡">
          <ElRadioGroup v-model="correctionForm.type">
            <ElRadio :value="1">上班卡</ElRadio>
            <ElRadio :value="2">下班卡</ElRadio>
          </ElRadioGroup>
        </ElFormItem>
        <ElFormItem label="申请原因">
          <ElInput
            v-model="correctionForm.reason"
            :maxlength="200"
            :rows="3"
            placeholder="如：外出拜访客户忘记打卡"
            show-word-limit
            type="textarea"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="correctionVisible = false">取消</ElButton>
        <ElButton type="primary" @click="submitCorrection">提交申请</ElButton>
      </template>
    </ElDialog>
  </Page>
</template>
