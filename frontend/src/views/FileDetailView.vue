<template>
  <div>
    <el-card v-loading="loading">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3 style="margin: 0">文件详情</h3>
          <el-button @click="$router.push('/files')">返回列表</el-button>
        </div>
      </template>
      <el-descriptions v-if="file" :column="2" border>
        <el-descriptions-item label="文件ID">{{ file.id }}</el-descriptions-item>
        <el-descriptions-item label="文件名">{{ file.fileName }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ formatSize(file.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="MIME类型">{{ file.mimeType || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="文件哈希(SM3)" :span="2">
          <el-text style="word-break: break-all">{{ file.fileHash }}</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="上传时间">{{ file.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ file.updatedAt }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="file" style="margin-top: 20px; text-align: center">
        <el-button type="success" @click="handleDownload">
          <el-icon><Download /></el-icon> 下载文件
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getFileDetail, downloadFile } from '../api/file'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'

const route = useRoute()
const authStore = useAuthStore()
const file = ref(null)
const loading = ref(false)

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0, size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(1) + ' ' + units[i]
}

async function loadFile() {
  loading.value = true
  try {
    const res = await getFileDetail(route.params.id)
    file.value = res.data.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function handleDownload() {
  try {
    ElMessage.info('正在下载并解密文件...')
    const { blob, fileName } = await downloadFile(file.value.id)
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = fileName
    link.click()
    URL.revokeObjectURL(link.href)
    ElMessage.success('下载成功')
  } catch (e) {
    console.error(e)
    ElMessage.error('下载失败: ' + (e.message || '未知错误'))
  }
}

onMounted(loadFile)
</script>
