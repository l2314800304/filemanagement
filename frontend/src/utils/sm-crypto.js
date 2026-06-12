import { sm2, sm3, sm4 } from 'sm-crypto'

/**
 * 生成随机SM4密钥（128位 = 16字节 = 32个hex字符）
 */
export function generateSm4Key() {
  const chars = '0123456789abcdef'
  let key = ''
  for (let i = 0; i < 32; i++) {
    key += chars[Math.floor(Math.random() * 16)]
  }
  return key
}

/**
 * SM2加密
 */
export function sm2Encrypt(data, publicKey) {
  // cipherMode: 1 = C1C3C2 (与后端BouncyCastle一致)
  let encrypted = sm2.doEncrypt(data, publicKey, 1)
  
  // 确保密文包含04前缀（未压缩点格式）
  // sm-crypto 库有些版本返回的密文不包含04前缀，需要手动添加
  if (!encrypted.startsWith('04')) {
    console.warn('SM2加密 - 密文缺少04前缀，自动添加')
    encrypted = '04' + encrypted
  }
  
  // 调试日志：输出加密结果信息
  console.log('SM2加密 - 原始数据:', data)
  console.log('SM2加密 - 公钥前缀:', publicKey.substring(0, 10) + '...')
  console.log('SM2加密 - 密文长度:', encrypted.length)
  console.log('SM2加密 - 密文前缀:', encrypted.substring(0, 4))
  
  return encrypted
}

/**
 * SM2解密
 */
export function sm2Decrypt(data, privateKey) {
  return sm2.doDecrypt(data, privateKey, 1)
}

/**
 * SM4加密（ECB模式）
 */
export function sm4Encrypt(data, key) {
  return sm4.encrypt(data, key)
}

/**
 * SM4解密（ECB模式）
 */
export function sm4Decrypt(data, key) {
  return sm4.decrypt(data, key)
}

/**
 * SM3哈希
 */
export function sm3Hash(data) {
  return sm3(data)
}

// ==================== 流式 SM3 哈希器 ====================
// 支持对大文件分块增量计算SM3，避免一次性加载到内存

function _rotl(x, n) {
  const s = n & 31
  return (x << s) | (x >>> (32 - s))
}

function _p0(X) { return (X ^ _rotl(X, 9)) ^ _rotl(X, 17) }
function _p1(X) { return (X ^ _rotl(X, 15)) ^ _rotl(X, 23) }

function _compress(V, block, offset) {
  const W = new Uint32Array(68)
  const M = new Uint32Array(64)
  for (let j = 0; j < 16; j++) {
    W[j] = ((block[offset + j * 4] << 24) | (block[offset + j * 4 + 1] << 16) |
            (block[offset + j * 4 + 2] << 8) | block[offset + j * 4 + 3]) >>> 0
  }
  for (let j = 16; j < 68; j++) {
    W[j] = (_p1((W[j-16] ^ W[j-9]) ^ _rotl(W[j-3], 15)) ^ _rotl(W[j-13], 7)) ^ W[j-6]
  }
  for (let j = 0; j < 64; j++) { M[j] = W[j] ^ W[j+4] }

  const T1 = 0x79cc4519, T2 = 0x7a879d8a
  let A = V[0], B = V[1], C = V[2], D = V[3]
  let E = V[4], F = V[5], G = V[6], H = V[7]

  for (let j = 0; j < 64; j++) {
    const T = j <= 15 ? T1 : T2
    const SS1 = _rotl((_rotl(A, 12) + E + _rotl(T, j)) & 0xFFFFFFFF, 7)
    const SS2 = SS1 ^ _rotl(A, 12)
    const TT1 = ((j <= 15 ? (A ^ B ^ C) : ((A & B) | (A & C) | (B & C))) + D + SS2 + M[j]) & 0xFFFFFFFF
    const TT2 = ((j <= 15 ? (E ^ F ^ G) : ((E & F) | ((~E) & G))) + H + SS1 + W[j]) & 0xFFFFFFFF
    D = C; C = _rotl(B, 9); B = A; A = TT1 >>> 0
    H = G; G = _rotl(F, 19); F = E; E = _p0(TT2) >>> 0
  }

  V[0] ^= A; V[1] ^= B; V[2] ^= C; V[3] ^= D
  V[4] ^= E; V[5] ^= F; V[6] ^= G; V[7] ^= H
}

/**
 * 流式SM3哈希计算器
 * 支持对大文件分块增量计算哈希，避免将整个文件加载到内存
 * @example
 * const hasher = new StreamingSm3()
 * hasher.update(chunk1)  // Uint8Array
 * hasher.update(chunk2)  // Uint8Array
 * const hash = hasher.digest()  // 返回hex字符串
 */
export class StreamingSm3 {
  constructor() {
    this._v = new Uint32Array([0x7380166f, 0x4914b2b9, 0x172442d7, 0xda8a0600, 0xa96f30bc, 0x163138aa, 0xe38dee4d, 0xb0fb0e4e])
    this._buf = new Uint8Array(64)
    this._bufLen = 0
    this._totalLen = 0
  }

  update(data) {
    let offset = 0
    this._totalLen += data.length

    // 填充缓冲区
    if (this._bufLen > 0) {
      const need = 64 - this._bufLen
      const copy = Math.min(need, data.length)
      this._buf.set(data.subarray(0, copy), this._bufLen)
      this._bufLen += copy
      offset = copy
      if (this._bufLen === 64) {
        _compress(this._v, this._buf, 0)
        this._bufLen = 0
      }
    }

    // 直接处理完整的64字节块
    while (offset + 64 <= data.length) {
      _compress(this._v, data, offset)
      offset += 64
    }

    // 缓存剩余字节
    if (offset < data.length) {
      const remaining = data.length - offset
      this._buf.set(data.subarray(offset, offset + remaining), 0)
      this._bufLen = remaining
    }
  }

  digest() {
    const totalBits = this._totalLen * 8

    // 添加0x80填充
    this._buf[this._bufLen] = 0x80
    this._bufLen++

    // 如果剩余空间不足8字节（存放长度），需要额外一个块
    if (this._bufLen > 56) {
      this._buf.fill(0, this._bufLen, 64)
      _compress(this._v, this._buf, 0)
      this._bufLen = 0
      this._buf.fill(0, 0, 56)
    } else {
      this._buf.fill(0, this._bufLen, 56)
    }

    // 写入64位大端长度（bits）
    const hi = Math.floor(totalBits / 0x100000000)
    const lo = totalBits >>> 0
    this._buf[56] = (hi >>> 24) & 0xff
    this._buf[57] = (hi >>> 16) & 0xff
    this._buf[58] = (hi >>> 8) & 0xff
    this._buf[59] = hi & 0xff
    this._buf[60] = (lo >>> 24) & 0xff
    this._buf[61] = (lo >>> 16) & 0xff
    this._buf[62] = (lo >>> 8) & 0xff
    this._buf[63] = lo & 0xff

    _compress(this._v, this._buf, 0)

    // 转换为hex字符串
    let hex = ''
    for (let i = 0; i < 8; i++) {
      hex += (this._v[i] >>> 0).toString(16).padStart(8, '0')
    }
    return hex
  }
}

/**
 * ArrayBuffer转Base64
 */
export function arrayBufferToBase64(buffer) {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i])
  }
  return btoa(binary)
}

/**
 * Base64转Uint8Array
 */
export function base64ToUint8Array(base64) {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return bytes
}

/**
 * Hex字符串转Uint8Array
 */
export function hexToUint8Array(hex) {
  const bytes = new Uint8Array(hex.length / 2)
  for (let i = 0; i < hex.length; i += 2) {
    bytes[i / 2] = parseInt(hex.substring(i, i + 2), 16)
  }
  return bytes
}
