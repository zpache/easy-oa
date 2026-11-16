import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace AttendanceApi {
  export interface Shift {
    [key: string]: any;
    boundCount?: number;
    endTime: string;
    flexibleMinutes: number;
    id: number;
    isDefault: 0 | 1;
    name: string;
    startTime: string;
    status: 0 | 1;
  }

  export interface MyCheckin {
    clockedIn: boolean;
    clockInStatus?: number;
    clockIn?: string;
    clockedOut: boolean;
    clockOutStatus?: number;
    clockOut?: string;
    flexibleMinutes?: number;
    shiftEnd?: string;
    shiftId?: number;
    shiftName?: string;
    shiftStart?: string;
    workDate: string;
  }

  export interface Checkin {
    [key: string]: any;
    clockIn?: string;
    clockInStatus?: number;
    clockOut?: string;
    clockOutStatus?: number;
    deptName?: string;
    id: number;
    shiftName?: string;
    userId: number;
    userName?: string;
    workDate: string;
  }

  export interface MonthStat {
    days: Record<string, { clockInStatus: null | number; clockOutStatus: null | number }>;
    earlyCount: number;
    lateCount: number;
    missingCount: number;
    normalCount: number;
    yearMonth: string;
  }

  export interface Correction {
    [key: string]: any;
    approveAt?: string;
    approverName?: string;
    approveRemark?: string;
    createdAt?: string;
    deptName?: string;
    id: number;
    reason: string;
    status: 0 | 1 | 2;
    type: 1 | 2;
    userId: number;
    userName?: string;
    workDate: string;
  }

  export interface LeaveType {
    [key: string]: any;
    defaultQuota: number;
    id: number;
    name: string;
    remark?: string;
    status: 0 | 1;
  }

  export interface LeaveBalance {
    deptName?: string;
    id?: number;
    remainQuota: number;
    totalQuota: number;
    typeId: number;
    typeName?: string;
    usedQuota: number;
    userId: number;
    userName?: string;
  }
}

const paged = (result: Recordable<any>) => ({
  items: result.records,
  total: result.total,
});

/* ==================== 班次 ==================== */

async function getShiftList() {
  return requestClient.get<AttendanceApi.Shift[]>('/attendance/shift');
}

async function createShift(data: Recordable<any>) {
  return requestClient.post<{ id: number }>('/attendance/shift', data);
}

async function updateShift(id: number, data: Recordable<any>) {
  return requestClient.put(`/attendance/shift/${id}`, data);
}

async function deleteShift(id: number) {
  return requestClient.delete(`/attendance/shift/${id}`);
}

async function bindShift(userId: number, shiftId?: number) {
  return requestClient.post('/attendance/shift/bind', { shiftId, userId });
}

/* ==================== 打卡 ==================== */

async function clock() {
  return requestClient.post<AttendanceApi.MyCheckin>('/attendance/clock');
}

async function getToday() {
  return requestClient.get<AttendanceApi.MyCheckin>('/attendance/today');
}

async function getMonthStat(yearMonth?: string) {
  return requestClient.get<AttendanceApi.MonthStat>('/attendance/month', {
    params: { yearMonth },
  });
}

async function getMyRecords(params: Recordable<any>) {
  const result = await requestClient.get<Recordable<any>>(
    '/attendance/my-records',
    { params },
  );
  return paged(result);
}

/** 全员考勤记录（管理端） */
async function getRecords(params: Recordable<any>) {
  const result = await requestClient.get<Recordable<any>>(
    '/attendance/records',
    { params },
  );
  return paged(result);
}

/* ==================== 补卡 ==================== */

async function applyCorrection(data: {
  reason: string;
  type: number;
  workDate: string;
}) {
  return requestClient.post<{ id: number }>('/attendance/correction', data);
}

async function getMyCorrections(params: Recordable<any>) {
  const result = await requestClient.get<Recordable<any>>(
    '/attendance/correction/my',
    { params },
  );
  return paged(result);
}

async function getCorrectionPage(params: Recordable<any>) {
  const result = await requestClient.get<Recordable<any>>(
    '/attendance/correction',
    { params },
  );
  return paged(result);
}

async function auditCorrection(
  id: number,
  data: { remark?: string; status: 1 | 2 },
) {
  return requestClient.post(`/attendance/correction/${id}/audit`, data);
}

/* ==================== 假期 ==================== */

async function getLeaveTypes() {
  return requestClient.get<AttendanceApi.LeaveType[]>('/attendance/leave/type');
}

async function createLeaveType(data: Recordable<any>) {
  return requestClient.post<{ id: number }>('/attendance/leave/type', data);
}

async function updateLeaveType(id: number, data: Recordable<any>) {
  return requestClient.put(`/attendance/leave/type/${id}`, data);
}

async function deleteLeaveType(id: number) {
  return requestClient.delete(`/attendance/leave/type/${id}`);
}

async function getMyBalances() {
  return requestClient.get<AttendanceApi.LeaveBalance[]>(
    '/attendance/leave/balance/my',
  );
}

async function getBalances(params: Recordable<any>) {
  const result = await requestClient.get<Recordable<any>>(
    '/attendance/leave/balance',
    { params },
  );
  return paged(result);
}

async function adjustBalance(data: {
  totalQuota: number;
  typeId: number;
  userId: number;
}) {
  return requestClient.post<AttendanceApi.LeaveBalance>(
    '/attendance/leave/balance/adjust',
    data,
  );
}

export {
  adjustBalance,
  applyCorrection,
  auditCorrection,
  bindShift,
  clock,
  createLeaveType,
  createShift,
  deleteLeaveType,
  deleteShift,
  getBalances,
  getCorrectionPage,
  getLeaveTypes,
  getMonthStat,
  getMyBalances,
  getMyCorrections,
  getMyRecords,
  getRecords,
  getShiftList,
  getToday,
  updateLeaveType,
  updateShift,
};
