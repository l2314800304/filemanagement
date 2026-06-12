import axios from 'axios'
import { generateSm4Key, sm2Encrypt, sm4Encrypt, sm4Decrypt } from '../utils/sm-crypto'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 120000
})

// 缓存SM2公钥
let sm2PublicKey = null

/**
 * 获取SM2公钥
 */
async function fetchPublicKey() {
  if (sm2PublicKey) return sm2PublicKey
  const res = await axios.get('http://localhost:8080/api/crypto/public-key')
  if (res.data && res.data.code === 200) {
    sm2PublicKey = res.data.data.publicKey
    return sm2PublicKey
  }
  throw new Error('获取SM2公钥失败')
}

/**
 * 请求拦截器 - SM4加密请求体
 */
api.interceptors.request.use(async (config) => {
  // 添加认证Token
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers['X-Auth-Token'] = authStore.token
  }

  // 不需要加密的路径
  const skipPaths = ['/api/crypto/public-key', '/api/file/upload/chunk', '/api/file/list', '/api/file']
  const isDownload = config.url && config.url.includes('/download')
  const isChunkUpload = config.url && config.url.includes('/upload/chunk')
  const isSkipPath = skipPaths.some(p => config.url === p) || isDownload || isChunkUpload

  if (isSkipPath) return config

  // 获取SM2公钥
  try {
    const publicKey = await fetchPublicKey()

    // 生成SM4密钥
    const sm4Key = generateSm4Key()

    // SM2加密SM4密钥
    const encryptedSm4Key = sm2Encrypt(sm4Key, publicKey)
    config.headers['X-Encrypted-SM4-Key'] = encryptedSm4Key

    // SM4加密请求体
    if (config.data && typeof config.data === 'object' && !(config.data instanceof FormData)) {
      const jsonStr = JSON.stringify(config.data)
      const encrypted = sm4Encrypt(jsonStr, sm4Key)
      config.data = encrypted
      config.headers['Content-Type'] = 'text/plain'
    }

    // 保存SM4密钥用于解密响应
    config._sm4Key = sm4Key
  } catch (e) {
    console.error('加密失败:', e)
  }

  return config
})

/**
 * 响应拦截器 - SM4解密响应体
 */
api.interceptors.response.use(
  (response) => {
    const isDownload = response.config.url && response.config.url.includes('/download')
    const isChunkUpload = response.config.url && response.config.url.includes('/upload/chunk')
    const isListOrDetail = response.config.url && (
      response.config.url.includes('/api/file/list') ||
      (response.config.url.match(/\/api\/file\/\d+$/) && !response.config.url.includes('/download'))
    )
    const isPublicKey = response.config.url && response.config.url.includes('/public-key')

    if (isDownload || isChunkUpload || isPublicKey) {
      return response
    }

    // SM4解密响应
    const sm4Key = response.config._sm4Key
    if (sm4Key && typeof response.data === 'string') {
      try {
        const decrypted = sm4Decrypt(response.data, sm4Key)
        response.data = JSON.parse(decrypted)
      } catch (e) {
        console.error('响应解密失败:', e)
      }
    }

    // 统一错误处理
    if (response.data && response.data.code !== 200) {
      ElMessage.error(response.data.message || '请求失败')
      return Promise.reject(new Error(response.data.message))
    }

    return response
  },
  (error) => {
    if (error.response && error.response.data) {
      const msg = error.response.data.message || '请求失败'
      ElMessage.error(msg)
    } else {
      ElMessage.error('网络错误')
    }
    return Promise.reject(error)
  }
)

export default api
