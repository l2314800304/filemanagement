<template>
  <div>
    <el-card>
      <template #header><h3 style="margin: 0">上传文件</h3></template>
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
        :on-exceed="() => ElMessage.warning('只能上传一个文件')"
      >
        <el-icon style="font-size: 48px; color: #409eff"><Upload /></el-icon>
        <div style="margin-top: 8px">拖拽文件到此处或 <em>点击选择</em></div>
      </el-upload>

      <div v-if="selectedFile" style="margin-top: 20px">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="文件名">{{ selectedFile.name }}</el-descriptions-item>
          <el-descriptions-item label="大小">{{ formatSize(selectedFile.size) }}</el-descriptions-item>
          <el-descriptions-item label="分片数">{{ totalChunks }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag">{{ statusText }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-progress :percentage="progress" :status="progressStatus" style="margin-top: 16px" />

        <div style="margin-top: 16px; text-align: center">
          <el-button type="primary" @click="handleUpload" :loading="uploading" :disabled="uploaded">开始上传</el-button>
          <el-button @click="$router.push('/files')">返回文件列表</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { initUpload, uploadChunk, mergeChunks } from '../api/file'
import { sm3Hash } from '../utils/sm-crypto'
import { ElMessage } from 'element-plus'

const router = useRouter()
const selectedFile = ref(null)
const uploading = ref(false)
const uploaded = ref(false)
const progress = ref(0)
const progressStatus = ref('')
const totalChunks = ref(0)
const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB

const statusText = ref('等待上传')
const statusTag = ref('info')

function handleFileChange(file) {
  selectedFile.value = file.raw
  totalChunks.value = Math.ceil(file.raw.size / CHUNK_SIZE)
  progress.value = 0
  progressStatus.value = ''
  uploaded.value = false
  statusText.value = '等待上传'
  statusTag.value = 'info'
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0, size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(1) + ' ' + units[i]
}

async function computeFileHash(file) {
  // 对大文件分块计算SM3哈希
  const blobSlice = File.prototype.slice
  const chunkSize = 2 * 1024 * 1024
  const chunks = Math.ceil(file.size / chunkSize)
  let currentChunk = 0
  // 简化：对整个文件内容做SM3（对于极大文件可能需要WebWorker）
  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const hash = sm3Hash(new Uint8Array(e.target.result))
      resolve(hash)
    }
    reader.readAsArrayBuffer(file)
  })
}

async function handleUpload() {
  if (!selectedFile.value) return
  uploading.value = true
  statusText.value = '计算文件哈希...'
  statusTag.value = 'warning'

  try {
    const file = selectedFile.value
    const fileHash = await computeFileHash(file)
    const chunks = Math.ceil(file.size / CHUNK_SIZE)

    // 1. 初始化上传
    statusText.value = '初始化上传...'
    const initRes = await initUpload({
      fileName: file.name,
      fileSize: file.size,
      fileHash: fileHash,
      totalChunks: chunks,
      chunkSize: CHUNK_SIZE
    })

    const { skipUpload, uploadId, uploadedChunks } = initRes.data.data
    if (skipUpload) {
      progress.value = 100
      progressStatus.value = 'success'
      statusText.value = '秒传成功！'
      statusTag.value = 'success'
      uploaded.value = true
      ElMessage.success('秒传成功！文件已存在')
      uploading.value = false
      return
    }

    const uploadedSet = new Set(uploadedChunks || [])

    // 2. 逐个上传分片（支持并发限制）
    statusText.value = '上传分片中...'
    statusTag.value = 'warning'
    let uploadedCount = uploadedSet.size
    const concurrency = 3
    const tasks = []

    for (let i = 0; i < chunks; i++) {
      if (uploadedSet.has(i)) continue
      tasks.push(i)
    }

    // 并发上传
    for (let i = 0; i < tasks.length; i += concurrency) {
      const batch = tasks.slice(i, i + concurrency)
      await Promise.all(batch.map(async (chunkIndex) => {
        const start = chunkIndex * CHUNK_SIZE
        const end = Math.min(start + CHUNK_SIZE, file.size)
        const blob = file.slice(start, end)
        await uploadChunk(uploadId, chunkIndex, '', blob)
        uploadedCount++
        progress.value = Math.round((uploadedCount / chunks) * 90)
      }))
    }

    // 3. 合并分片
    statusText.value = '合并文件中...'
    progress.value = 95
    await mergeChunks({ uploadId: uploadId, fileHash: fileHash })

    progress.value = 100
    progressStatus.value = 'success'
    statusText.value = '上传完成！'
    statusTag.value = 'success'
    uploaded.value = true
    ElMessage.success('文件上传成功！')
  } catch (e) {
    progressStatus.value = 'exception'
    statusText.value = '上传失败'
    statusTag.value = 'danger'
    console.error(e)
  } finally {
    uploading.value = false
  }
}
</script>
