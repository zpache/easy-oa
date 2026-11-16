import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace OfficeNoticeApi {
  export interface Notice {
    [key: string]: any;
    category: string;
    content: string;
    createdAt?: string;
    deptIds: number[];
    expireAt?: string;
    id: number;
    publishAt?: string;
    publisher?: string;
    readCount?: number;
    scopeType: 'all' | 'dept';
    status: 0 | 1 | 2;
    title: string;
    top: 0 | 1;
  }

  export interface NoticePayload {
    category?: string;
    content: string;
    deptIds?: number[];
    expireAt?: string;
    scopeType: 'all' | 'dept';
    title: string;
    top?: number;
  }

  export interface NoticeRead {
    deptName?: string;
    nickname: string;
    readAt: string;
    userId: number;
  }

  export interface ReadStat {
    readCount: number;
    totalUsers: number;
  }

  export interface PortalNotice {
    category: string;
    expireAt?: string;
    id: number;
    publishAt?: string;
    read: boolean;
    title: string;
    top: 0 | 1;
  }

  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }
}

/* ==================== 管理端 ==================== */

async function getNoticePage(params: Recordable<any>) {
  const result = await requestClient.get<OfficeNoticeApi.PageResult<any>>(
    '/office/notice/list',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function getNoticeDetail(id: number) {
  return requestClient.get<OfficeNoticeApi.Notice>(`/office/notice/${id}`);
}

async function createNotice(data: OfficeNoticeApi.NoticePayload, publish = false) {
  return requestClient.post('/office/notice', data, { params: { publish } });
}

async function updateNotice(id: number, data: OfficeNoticeApi.NoticePayload) {
  return requestClient.put(`/office/notice/${id}`, data);
}

async function publishNotice(id: number) {
  return requestClient.put(`/office/notice/${id}/publish`);
}

async function withdrawNotice(id: number) {
  return requestClient.put(`/office/notice/${id}/withdraw`);
}

async function toggleNoticeTop(id: number) {
  return requestClient.put(`/office/notice/${id}/top`);
}

async function deleteNotice(id: number) {
  return requestClient.delete(`/office/notice/${id}`);
}

async function getNoticeReads(id: number) {
  return requestClient.get<OfficeNoticeApi.NoticeRead[]>(
    `/office/notice/${id}/reads`,
  );
}

async function getNoticeReadStat(id: number) {
  return requestClient.get<OfficeNoticeApi.ReadStat>(
    `/office/notice/${id}/read-stat`,
  );
}

/* ==================== 员工端（公告中心） ==================== */

async function getPortalNoticePage(params: Recordable<any>) {
  const result = await requestClient.get<OfficeNoticeApi.PageResult<any>>(
    '/office/notice/portal/list',
    { params },
  );
  return { items: result.records, total: result.total };
}

async function getPortalNoticeDetail(id: number) {
  return requestClient.get<Recordable<any>>(`/office/notice/portal/${id}`);
}

/** 我的未读公告数（顶栏铃铛 / 工作台提醒） */
async function getPortalUnreadCount() {
  return requestClient.get<number>('/office/notice/portal/unread-count');
}

export {
  createNotice,
  deleteNotice,
  getNoticeDetail,
  getNoticePage,
  getNoticeReadStat,
  getNoticeReads,
  getPortalNoticeDetail,
  getPortalNoticePage,
  getPortalUnreadCount,
  publishNotice,
  toggleNoticeTop,
  updateNotice,
  withdrawNotice,
};
