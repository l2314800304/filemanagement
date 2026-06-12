<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3 style="margin: 0">我的文件</h3>
          <el-button type="primary" @click="$router.push('/upload')">上传文件</el-button>
        </div>
      </template>
      <el-table :data="files" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="180" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/file/${row.id}`)">详情</el-button>
            <el-button size="small" type="success" @click="handleDownload(row)">下载</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 16px; text-align: right"
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadFiles"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFileList, getDownloadUrl } from '../api/file'
import { useAuthStore } from '../stores/auth'

const files = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const authStore = useAuthStore()

async function loadFiles() {
  loading.value = true
  try {
    const res = await getFileList(currentPage.value, pageSize.value)
    files.value = res.data.data.files || []
    total.value = res.data.data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(1) + ' ' + units[i]
}

function handleDownload(row) {
  const url = getDownloadUrl(row.id)
  const a = document.createElement('a')
  a.href = url
  a.download = row.fileName
  // 添加认证token
  const xhr = new XMLHttpRequest()
  xhr.open('GET', url, true)
  xhr.setRequestHeader('X-Auth-Token', authStore.token)
  xhr.responseType = 'blob'
  xhr.onload = () => {
    if (xhr.status === 200 || xhr.status === 206) {
      const blob = xhr.response
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = row.fileName
      link.click()
      URL.revokeObjectURL(link.href)
    }
  }
  xhr.send()
}

onMounted(loadFiles)
</script>
