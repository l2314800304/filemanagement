import api from './request'
import { arrayBufferToBase64, sm4Decrypt, base64ToUint8Array } from '../utils/sm-crypto'

export function initUpload(data) {
  return api.post('/api/file/upload/init', data)
}

/**
 * 上传分片（SM4加密，由请求拦截器统一加密）
 * @param {number} uploadId - 上传ID
 * @param {number} chunkIndex - 分片序号
 * @param {string} chunkHash - 分片哈希（可选）
 * @param {Blob} chunkBlob - 分片数据
 */
export function uploadChunk(uploadId, chunkIndex, chunkHash, chunkBlob) {
  // 将分片读取为ArrayBuffer → Base64，放入JSON body
  // 请求拦截器会统一SM4加密整个body
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      try {
        const chunkBase64 = arrayBufferToBase64(reader.result)
        api.post('/api/file/upload/chunk', {
          uploadId,
          chunkIndex,
          chunkHash: chunkHash || '',
          chunkData: chunkBase64
        }).then(resolve).catch(reject)
      } catch (e) {
        reject(e)
      }
    }
    reader.onerror = () => reject(new Error('读取分片数据失败'))
    reader.readAsArrayBuffer(chunkBlob)
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

/**
 * 下载文件（SM4加密，返回解密后的Blob）
 * @param {number} id - 文件ID
 * @returns {Promise<{blob: Blob, fileName: string}>}
 */
export async function downloadFile(id) {
  const res = await api.get(`/api/file/${id}/download`)
  const { fileName, fileData, mimeType } = res.data.data

  // fileData 是SM4加密后的hex字符串（controller内部加密的）
  // 响应拦截器解密了CryptoResponseAdvice的外层加密
  // 所以 fileData 仍是controller内层的SM4加密hex
  // 用同一个SM4密钥解密
  const sm4Key = res.config._sm4Key
  const decryptedBase64 = sm4Decrypt(fileData, sm4Key)
  const fileBytes = base64ToUint8Array(decryptedBase64)
  const blob = new Blob([fileBytes], { type: mimeType || 'application/octet-stream' })

  return { blob, fileName }
}
