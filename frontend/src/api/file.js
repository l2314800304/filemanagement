import api from './request'

export function initUpload(data) {
  return api.post('/api/file/upload/init', data)
}

export function uploadChunk(uploadId, chunkIndex, chunkHash, file) {
  const formData = new FormData()
  formData.append('uploadId', uploadId)
  formData.append('chunkIndex', chunkIndex)
  formData.append('chunkHash', chunkHash || '')
  formData.append('file', file)
  return api.post('/api/file/upload/chunk', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function mergeChunks(data) {
  return api.post('/api/file/upload/merge', data)
}

export function cancelUpload(uploadId) {
  return api.post('/api/file/upload/cancel', { uploadId })
}

export function getFileList(page = 1, size = 20) {
  return api.get('/api/file/list', { params: { page, size } })
}

export function getFileDetail(id) {
  return api.get(`/api/file/${id}`)
}

export function getDownloadUrl(id) {
  return `http://localhost:8080/api/file/${id}/download`
}
