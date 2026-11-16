import type { Recordable } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace DocApi {
  export interface Folder {
    children: Folder[];
    description?: null | string;
    docCount: number;
    id: number;
    name: string;
    pid: number;
    sort: number;
  }

  export interface FolderPayload {
    description?: string;
    name: string;
    pid?: number;
    sort?: number;
  }

  export type AclItem = {
    /** dept 部门 / role 角色 / user 指定人 */
    subjectType: 'dept' | 'role' | 'user';
    subjectId: number;
  };

  export interface Doc {
    createdAt?: string;
    creatorName?: null | string;
    folderId: number;
    folderName?: null | string;
    id: number;
    status: 0 | 1;
    title: string;
    updatedAt?: string;
  }

  export interface DocDetail extends Doc {
    content: string;
  }

  export interface DocPayload {
    content: string;
    folderId: number;
    title: string;
  }

  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }
}

/* ==================== 目录 ==================== */

async function getFolderTree() {
  return requestClient.get<DocApi.Folder[]>('/doc/folders');
}

async function createFolder(data: DocApi.FolderPayload) {
  return requestClient.post<{ id: number }>('/doc/folders', data);
}

async function updateFolder(id: number, data: DocApi.FolderPayload) {
  return requestClient.put(`/doc/folders/${id}`, data);
}

async function deleteFolder(id: number) {
  return requestClient.delete(`/doc/folders/${id}`);
}

async function getFolderAcl(id: number) {
  return requestClient.get<DocApi.AclItem[]>(`/doc/folders/${id}/acl`);
}

async function setFolderAcl(id: number, items: DocApi.AclItem[]) {
  return requestClient.put(`/doc/folders/${id}/acl`, { items });
}

/* ==================== 文档 ==================== */

async function getDocPage(params: Recordable<any>) {
  const result = await requestClient.get<DocApi.PageResult<any>>('/doc/list', {
    params,
  });
  return { items: result.records, total: result.total };
}

async function getDocDetail(id: number) {
  return requestClient.get<DocApi.DocDetail>(`/doc/${id}`);
}

async function createDoc(data: DocApi.DocPayload, publish = false) {
  return requestClient.post<{ id: number }>('/doc', data, {
    params: { publish },
  });
}

async function updateDoc(id: number, data: DocApi.DocPayload) {
  return requestClient.put(`/doc/${id}`, data);
}

async function publishDoc(id: number) {
  return requestClient.put(`/doc/${id}/publish`);
}

async function deleteDoc(id: number) {
  return requestClient.delete(`/doc/${id}`);
}

export {
  createDoc,
  createFolder,
  deleteDoc,
  deleteFolder,
  getDocDetail,
  getDocPage,
  getFolderAcl,
  getFolderTree,
  publishDoc,
  setFolderAcl,
  updateDoc,
  updateFolder,
};
