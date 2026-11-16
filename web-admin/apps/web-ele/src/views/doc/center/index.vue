<script lang="ts" setup>
import type { DocApi } from '#/api/doc/doc';

import { ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  ElButton,
  ElDialog,
  ElDrawer,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElTag,
  ElTree,
  ElTreeSelect,
} from 'element-plus';

import {
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
} from '#/api/doc/doc';
import { getDeptList } from '#/api/system/dept';
import { getRoleOptions } from '#/api/system/role';
import { getUserList } from '#/api/system/user';
import { DOC_PERMISSIONS } from '#/constants/permissions';

/* ==================== 目录树 ==================== */

const folders = ref<DocApi.Folder[]>([]);
const treeProps = { children: 'children', label: 'name' };

async function loadFolders() {
  folders.value = await getFolderTree();
}
loadFolders();

const selectedFolder = ref<null | DocApi.Folder>(null);

function onNodeClick(node: DocApi.Folder) {
  selectedFolder.value = node;
  query.value.folderId = node.id;
  query.value.pageNum = 1;
  loadDocs();
}

/* ==================== 目录弹窗 ==================== */

const folderDialogVisible = ref(false);
const folderSaving = ref(false);
const folderForm = ref({ description: '', id: null as null | number, name: '', pid: 0 as number, sort: 0 });

function openFolderCreate(pid = 0) {
  folderForm.value = { description: '', id: null, name: '', pid, sort: 0 };
  folderDialogVisible.value = true;
}

function openFolderEdit(node: DocApi.Folder) {
  folderForm.value = {
    description: node.description ?? '',
    id: node.id,
    name: node.name,
    pid: node.pid,
    sort: node.sort,
  };
  folderDialogVisible.value = true;
}

async function onSaveFolder() {
  if (!folderForm.value.name.trim()) {
    ElMessage.error('请填写目录名');
    return;
  }
  folderSaving.value = true;
  try {
    const payload = {
      description: folderForm.value.description || undefined,
      name: folderForm.value.name.trim(),
      pid: folderForm.value.pid,
      sort: folderForm.value.sort,
    };
    if (folderForm.value.id) {
      await updateFolder(folderForm.value.id, payload);
    } else {
      await createFolder(payload);
    }
    ElMessage.success('保存成功');
    folderDialogVisible.value = false;
    await loadFolders();
  } finally {
    folderSaving.value = false;
  }
}

async function onDeleteFolder(node: DocApi.Folder) {
  await ElMessageBox.confirm(
    `确认删除目录「${node.name}」吗？目录下须无子目录和文档。`,
    '删除目录',
    { type: 'warning' },
  );
  await deleteFolder(node.id);
  ElMessage.success('删除成功');
  if (selectedFolder.value?.id === node.id) {
    selectedFolder.value = null;
    query.value.folderId = undefined;
  }
  await Promise.all([loadFolders(), loadDocs()]);
}

/* ==================== ACL 弹窗 ==================== */

const aclVisible = ref(false);
const aclSaving = ref(false);
const aclFolder = ref<null | DocApi.Folder>(null);
const aclDeptIds = ref<number[]>([]);
const aclRoleIds = ref<number[]>([]);
const aclUserIds = ref<number[]>([]);
const roleOptions = ref<{ label: string; value: number }[]>([]);
const userOptions = ref<{ label: string; value: number }[]>([]);
const deptTree = ref<any[]>([]);

async function openAcl(node: DocApi.Folder) {
  aclFolder.value = node;
  aclVisible.value = true;
  const [acl, roles, users, depts] = await Promise.all([
    getFolderAcl(node.id),
    getRoleOptions(),
    getUserList({ pageNum: 1, pageSize: 200 }),
    getDeptList(),
  ]);
  roleOptions.value = roles.map((r: any) => ({ label: r.name, value: r.id }));
  userOptions.value = (users.items ?? []).map((u: any) => ({
    label: u.nickname || u.username,
    value: u.id,
  }));
  deptTree.value = depts;
  aclDeptIds.value = acl.filter((i) => i.subjectType === 'dept').map((i) => i.subjectId);
  aclRoleIds.value = acl.filter((i) => i.subjectType === 'role').map((i) => i.subjectId);
  aclUserIds.value = acl.filter((i) => i.subjectType === 'user').map((i) => i.subjectId);
}

async function onSaveAcl() {
  if (!aclFolder.value) {
    return;
  }
  const items: DocApi.AclItem[] = [
    ...aclDeptIds.value.map((subjectId) => ({ subjectId, subjectType: 'dept' as const })),
    ...aclRoleIds.value.map((subjectId) => ({ subjectId, subjectType: 'role' as const })),
    ...aclUserIds.value.map((subjectId) => ({ subjectId, subjectType: 'user' as const })),
  ];
  aclSaving.value = true;
  try {
    await setFolderAcl(aclFolder.value.id, items);
    ElMessage.success(
      items.length ? '权限已保存，未命中的用户将不可见该目录' : '已清空权限，目录对全员可见',
    );
    aclVisible.value = false;
  } finally {
    aclSaving.value = false;
  }
}

/* ==================== 文档列表 ==================== */

const query = ref({ folderId: undefined as undefined | number, keyword: '', pageNum: 1, pageSize: 10 });
const docs = ref<DocApi.Doc[]>([]);
const total = ref(0);
const loading = ref(false);

async function loadDocs() {
  loading.value = true;
  try {
    const result = await getDocPage(query.value);
    docs.value = result.items;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}
loadDocs();

function onSearch() {
  query.value.pageNum = 1;
  loadDocs();
}

function onPageChange(page: number) {
  query.value.pageNum = page;
  loadDocs();
}

/* ==================== 文档编辑抽屉 ==================== */

const docDrawerVisible = ref(false);
const docSaving = ref(false);
const docForm = ref({ content: '', folderId: undefined as undefined | number, id: null as null | number, title: '' });

function openDocCreate() {
  docForm.value = {
    content: '',
    folderId: selectedFolder.value?.id,
    id: null,
    title: '',
  };
  docDrawerVisible.value = true;
}

async function openDocEdit(row: DocApi.Doc) {
  const detail = await getDocDetail(row.id);
  docForm.value = {
    content: detail.content,
    folderId: detail.folderId,
    id: detail.id,
    title: detail.title,
  };
  docDrawerVisible.value = true;
}

async function onSaveDoc(publish = false) {
  if (!docForm.value.title.trim()) {
    ElMessage.error('请填写标题');
    return;
  }
  if (!docForm.value.folderId) {
    ElMessage.error('请选择所属目录');
    return;
  }
  if (!docForm.value.content.trim()) {
    ElMessage.error('请填写正文');
    return;
  }
  docSaving.value = true;
  try {
    const payload = {
      content: docForm.value.content,
      folderId: docForm.value.folderId,
      title: docForm.value.title.trim(),
    };
    if (docForm.value.id) {
      await updateDoc(docForm.value.id, payload);
      if (publish) {
        await publishDoc(docForm.value.id);
      }
    } else {
      await createDoc(payload, publish);
    }
    ElMessage.success(publish ? '已发布' : '已保存为草稿');
    docDrawerVisible.value = false;
    await Promise.all([loadDocs(), loadFolders()]);
  } finally {
    docSaving.value = false;
  }
}

/* ==================== 文档查看 / 发布 / 删除 ==================== */

const viewVisible = ref(false);
const viewDoc = ref<null | DocApi.DocDetail>(null);

async function onViewDoc(row: DocApi.Doc) {
  viewDoc.value = await getDocDetail(row.id);
  viewVisible.value = true;
}

async function onPublishDoc(row: DocApi.Doc) {
  await ElMessageBox.confirm(
    `确认发布「${row.title}」吗？发布后可见范围内的用户均可阅读。`,
    '发布文档',
    { type: 'info' },
  );
  await publishDoc(row.id);
  ElMessage.success('发布成功');
  await loadDocs();
}

async function onDeleteDoc(row: DocApi.Doc) {
  await ElMessageBox.confirm(`确认删除「${row.title}」吗？`, '删除文档', {
    type: 'warning',
  });
  await deleteDoc(row.id);
  ElMessage.success('删除成功');
  await Promise.all([loadDocs(), loadFolders()]);
}

function statusTag(status: 0 | 1) {
  return status === 1 ? { label: '已发布', type: 'success' as const } : { label: '草稿', type: 'info' as const };
}
</script>

<template>
  <Page>
    <div class="flex h-full gap-3">
      <!-- 左：目录树 -->
      <div class="w-72 shrink-0 rounded-lg border border-gray-200 p-3">
        <div class="mb-2 flex items-center justify-between">
          <span class="font-medium">知识库目录</span>
          <ElButton
            v-access:code="DOC_PERMISSIONS.folderCreate"
            size="small"
            type="primary"
            @click="openFolderCreate(0)"
          >
            <Plus class="size-4" />
            根目录
          </ElButton>
        </div>
        <ElTree
          :data="folders"
          :expand-on-click-node="false"
          node-key="id"
          :props="treeProps"
          @node-click="onNodeClick"
        >
          <template #default="{ data }">
            <div class="group flex w-full items-center justify-between">
              <span class="truncate">
                {{ data.name }}
                <span class="text-xs text-gray-400">({{ data.docCount }})</span>
              </span>
              <span class="hidden gap-1 group-hover:flex">
                <ElButton
                  v-access:code="DOC_PERMISSIONS.folderCreate"
                  link
                  size="small"
                  type="primary"
                  @click.stop="openFolderCreate(data.id)"
                >
                  +
                </ElButton>
                <ElButton
                  v-access:code="DOC_PERMISSIONS.folderUpdate"
                  link
                  size="small"
                  @click.stop="openFolderEdit(data)"
                >
                  改
                </ElButton>
                <ElButton
                  v-access:code="DOC_PERMISSIONS.folderAcl"
                  link
                  size="small"
                  type="warning"
                  @click.stop="openAcl(data)"
                >
                  权
                </ElButton>
                <ElButton
                  v-access:code="DOC_PERMISSIONS.folderDelete"
                  link
                  size="small"
                  type="danger"
                  @click.stop="onDeleteFolder(data)"
                >
                  删
                </ElButton>
              </span>
            </div>
          </template>
        </ElTree>
      </div>

      <!-- 右：文档列表 -->
      <div class="flex min-w-0 flex-1 flex-col rounded-lg border border-gray-200 p-3">
        <div class="mb-3 flex items-center gap-2">
          <ElInput
            v-model="query.keyword"
            class="max-w-64"
            clearable
            placeholder="搜索标题 / 正文关键字"
            @change="onSearch"
            @clear="onSearch"
          />
          <ElButton @click="onSearch">搜索</ElButton>
          <div class="flex-1"></div>
          <ElButton
            v-access:code="DOC_PERMISSIONS.docCreate"
            type="primary"
            @click="openDocCreate"
          >
            <Plus class="size-5" />
            新建文档
          </ElButton>
        </div>

        <div class="flex-1 overflow-auto">
          <table class="w-full text-sm">
            <thead class="text-left text-gray-500">
              <tr class="border-b">
                <th class="px-2 py-2">标题</th>
                <th class="px-2 py-2">目录</th>
                <th class="px-2 py-2">状态</th>
                <th class="px-2 py-2">创建人</th>
                <th class="px-2 py-2">更新时间</th>
                <th class="px-2 py-2 text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in docs"
                :key="row.id"
                class="border-b border-gray-100 hover:bg-gray-50"
              >
                <td class="px-2 py-2">
                  <ElButton link type="primary" @click="onViewDoc(row)">
                    {{ row.title }}
                  </ElButton>
                </td>
                <td class="px-2 py-2">{{ row.folderName }}</td>
                <td class="px-2 py-2">
                  <ElTag :type="statusTag(row.status).type" size="small">
                    {{ statusTag(row.status).label }}
                  </ElTag>
                </td>
                <td class="px-2 py-2">{{ row.creatorName }}</td>
                <td class="px-2 py-2 text-gray-500">
                  {{ row.updatedAt?.replace('T', ' ').slice(0, 16) }}
                </td>
                <td class="px-2 py-2 text-right">
                  <ElButton
                    v-access:code="DOC_PERMISSIONS.docUpdate"
                    link
                    size="small"
                    @click="openDocEdit(row)"
                  >
                    编辑
                  </ElButton>
                  <ElButton
                    v-if="row.status === 0"
                    v-access:code="DOC_PERMISSIONS.docPublish"
                    link
                    size="small"
                    type="success"
                    @click="onPublishDoc(row)"
                  >
                    发布
                  </ElButton>
                  <ElButton
                    v-access:code="DOC_PERMISSIONS.docDelete"
                    link
                    size="small"
                    type="danger"
                    @click="onDeleteDoc(row)"
                  >
                    删除
                  </ElButton>
                </td>
              </tr>
              <tr v-if="docs.length === 0">
                <td class="px-2 py-8 text-center text-gray-400" colspan="6">
                  暂无文档
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mt-3 flex items-center justify-end gap-2">
          <ElButton
            :disabled="query.pageNum <= 1"
            size="small"
            @click="onPageChange(query.pageNum - 1)"
          >
            上一页
          </ElButton>
          <span class="text-sm text-gray-500">
            {{ query.pageNum }} / {{ Math.max(1, Math.ceil(total / query.pageSize)) }} 页，共
            {{ total }} 篇
          </span>
          <ElButton
            :disabled="query.pageNum >= Math.ceil(total / query.pageSize)"
            size="small"
            @click="onPageChange(query.pageNum + 1)"
          >
            下一页
          </ElButton>
        </div>
      </div>
    </div>

    <!-- 目录弹窗 -->
    <ElDialog
      v-model="folderDialogVisible"
      :title="folderForm.id ? '编辑目录' : '新增目录'"
      width="460px"
    >
      <ElForm label-width="80px">
        <ElFormItem label="父目录">
          <ElTreeSelect
            v-model="folderForm.pid"
            :data="folders"
            :props="treeProps"
            check-strictly
            class="!w-full"
            node-key="id"
            placeholder="根目录"
          />
        </ElFormItem>
        <ElFormItem label="目录名" required>
          <ElInput v-model="folderForm.name" maxlength="50" placeholder="请输入目录名" />
        </ElFormItem>
        <ElFormItem label="排序">
          <ElInputNumber v-model="folderForm.sort" :min="0" />
        </ElFormItem>
        <ElFormItem label="说明">
          <ElInput v-model="folderForm.description" maxlength="200" placeholder="选填" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="folderDialogVisible = false">取消</ElButton>
        <ElButton :loading="folderSaving" type="primary" @click="onSaveFolder">
          保存
        </ElButton>
      </template>
    </ElDialog>

    <!-- ACL 弹窗 -->
    <ElDialog
      v-model="aclVisible"
      :title="`阅读权限 - ${aclFolder?.name ?? ''}`"
      width="560px"
    >
      <ElForm label-width="80px">
        <ElFormItem label="可见部门">
          <ElTreeSelect
            v-model="aclDeptIds"
            :data="deptTree"
            :props="treeProps"
            check-strictly
            class="!w-full"
            clearable
            multiple
            node-key="id"
            placeholder="不限制则留空"
          />
        </ElFormItem>
        <ElFormItem label="可见角色">
          <ElSelect
            v-model="aclRoleIds"
            class="!w-full"
            clearable
            multiple
            placeholder="不限制则留空"
          >
            <ElOption
              v-for="r in roleOptions"
              :key="r.value"
              :label="r.label"
              :value="r.value"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="指定人员">
          <ElSelect
            v-model="aclUserIds"
            class="!w-full"
            clearable
            filterable
            multiple
            placeholder="不限制则留空"
          >
            <ElOption
              v-for="u in userOptions"
              :key="u.value"
              :label="u.label"
              :value="u.value"
            />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <p class="px-4 text-xs text-gray-400">
        三类主体任一命中即可见；全部留空 = 全员可见。子目录继承父目录限制。
      </p>
      <template #footer>
        <ElButton @click="aclVisible = false">取消</ElButton>
        <ElButton :loading="aclSaving" type="primary" @click="onSaveAcl">
          保存
        </ElButton>
      </template>
    </ElDialog>

    <!-- 文档编辑抽屉 -->
    <ElDrawer
      v-model="docDrawerVisible"
      :title="docForm.id ? '编辑文档' : '新建文档'"
      size="720px"
    >
      <ElForm label-width="80px">
        <ElFormItem label="所属目录" required>
          <ElTreeSelect
            v-model="docForm.folderId"
            :data="folders"
            :props="treeProps"
            check-strictly
            class="!w-full"
            node-key="id"
            placeholder="请选择目录"
          />
        </ElFormItem>
        <ElFormItem label="标题" required>
          <ElInput v-model="docForm.title" maxlength="100" placeholder="请输入文档标题" />
        </ElFormItem>
        <ElFormItem label="正文" required>
          <ElInput
            v-model="docForm.content"
            :autosize="{ maxRows: 28, minRows: 16 }"
            placeholder="支持 HTML 富文本内容"
            type="textarea"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="docDrawerVisible = false">取消</ElButton>
        <ElButton :loading="docSaving" @click="onSaveDoc(false)">存草稿</ElButton>
        <ElButton :loading="docSaving" type="primary" @click="onSaveDoc(true)">
          保存并发布
        </ElButton>
      </template>
    </ElDrawer>

    <!-- 文档查看 -->
    <ElDialog v-model="viewVisible" :title="viewDoc?.title ?? ''" width="760px">
      <div
        class="max-h-[60vh] overflow-auto leading-relaxed"
        v-html="viewDoc?.content"
      ></div>
      <template #footer>
        <ElButton @click="viewVisible = false">关闭</ElButton>
      </template>
    </ElDialog>
  </Page>
</template>
