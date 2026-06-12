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
