package com.filemgmt.infrastructure.crypto;

import com.filemgmt.domain.crypto.service.CryptoDomainService;
import com.filemgmt.domain.crypto.valueobject.Sm2KeyPair;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.Security;
import java.nio.charset.StandardCharsets;

/**
 * 基于BouncyCastle的国密算法服务实现
 */
public class BouncyCastleCryptoService implements CryptoDomainService {

    private static final Logger log = LoggerFactory.getLogger(BouncyCastleCryptoService.class);
    private static final ECNamedCurveParameterSpec SM2_CURVE = ECNamedCurveTable.getParameterSpec("sm2p256v1");

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    public Sm2KeyPair generateSm2KeyPair() {
        try {
            java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("EC", "BC");
            kpg.initialize(new java.security.spec.ECGenParameterSpec("sm2p256v1"));
            java.security.KeyPair kp = kpg.generateKeyPair();

            org.bouncycastle.jce.interfaces.ECPublicKey pubKey =
                    (org.bouncycastle.jce.interfaces.ECPublicKey) kp.getPublic();
            org.bouncycastle.jce.interfaces.ECPrivateKey privKey =
                    (org.bouncycastle.jce.interfaces.ECPrivateKey) kp.getPrivate();

            String publicKeyHex = Hex.encodeHexString(pubKey.getQ().getEncoded(false));
            String privateKeyHex = privKey.getD().toString(16);
            // 补齐私钥到64字符
            while (privateKeyHex.length() < 64) {
                privateKeyHex = "0" + privateKeyHex;
            }

            return new Sm2KeyPair(publicKeyHex, privateKeyHex);
        } catch (Exception e) {
            throw new RuntimeException("生成SM2密钥对失败", e);
        }
    }

    @Override
    public String sm2Encrypt(String plainText, String publicKeyHex) {
        try {
            byte[] pubKeyBytes = Hex.decodeHex(publicKeyHex.toCharArray());
            org.bouncycastle.math.ec.ECPoint ecPoint = SM2_CURVE.getCurve().decodePoint(pubKeyBytes);
            ECPublicKeyParameters pubKeyParams = new ECPublicKeyParameters(
                    ecPoint,
                    new ECDomainParameters(SM2_CURVE.getCurve(), SM2_CURVE.getG(), SM2_CURVE.getN(), SM2_CURVE.getH()));

            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            engine.init(true, new ParametersWithRandom(pubKeyParams, new java.security.SecureRandom()));

            byte[] data = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = engine.processBlock(data, 0, data.length);
            return Hex.encodeHexString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("SM2加密失败", e);
        }
    }

    @Override
    public String sm2Decrypt(String cipherText, String privateKeyHex) {
        try {
            BigInteger d = new BigInteger(privateKeyHex, 16);
            ECPrivateKeyParameters privKeyParams = new ECPrivateKeyParameters(
                    d,
                    new ECDomainParameters(SM2_CURVE.getCurve(), SM2_CURVE.getG(), SM2_CURVE.getN(), SM2_CURVE.getH()));

            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            engine.init(false, privKeyParams);

            // 处理密文格式：确保密文有正确的04前缀
            String normalizedCipherText = cipherText.trim().toLowerCase();
            
            // 记录密文信息用于调试
            log.info("SM2解密 - 原始密文长度: {}, 前缀: {}", 
                    normalizedCipherText.length(), 
                    normalizedCipherText.length() > 4 ? normalizedCipherText.substring(0, 4) : "N/A");
            
            // SM2密文标准格式：C1(65字节) + C3(32字节) + C2(数据长度)
            // C1部分：04标记(1字节) + X坐标(32字节) + Y坐标(32字节) = 65字节 = 130个hex字符
            // 如果密文不以04开头，需要添加04前缀
            if (!normalizedCipherText.startsWith("04")) {
                log.warn("SM2解密 - 密文缺少04前缀，自动添加。原始前缀: {}", 
                        normalizedCipherText.substring(0, Math.min(4, normalizedCipherText.length())));
                normalizedCipherText = "04" + normalizedCipherText;
            }
            
            // 验证密文长度是否合理（至少要有C1+C3 = 97字节 = 194个hex字符）
            if (normalizedCipherText.length() < 194) {
                log.error("SM2解密 - 密文长度异常: {} 字符，期望至少 194 字符", normalizedCipherText.length());
                throw new RuntimeException("SM2密文长度异常，可能不是有效的SM2加密结果");
            }
            
            log.info("SM2解密 - 标准化后密文长度: {}, 前缀: {}", 
                    normalizedCipherText.length(), 
                    normalizedCipherText.substring(0, 4));
            
            byte[] cipherBytes = Hex.decodeHex(normalizedCipherText.toCharArray());
            byte[] decrypted = engine.processBlock(cipherBytes, 0, cipherBytes.length);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (org.bouncycastle.crypto.InvalidCipherTextException e) {
            // 针对无效密文的详细错误处理
            log.error("SM2解密失败 - 密文格式不正确或密钥不匹配.\n" +
                    "  密文长度: {},\n" +
                    "  密文前缀: {},\n" +
                    "  私钥前缀: {},\n" +
                    "  可能原因: 1.前后端密钥不匹配 2.密文被篡改 3.加密模式不一致",
                    cipherText != null ? cipherText.length() : 0,
                    cipherText != null && cipherText.length() > 4 ? cipherText.substring(0, 4) : "null",
                    privateKeyHex != null && privateKeyHex.length() > 8 ? privateKeyHex.substring(0, 8) + "..." : "null");
            throw new RuntimeException("SM2解密失败：密文格式不正确或密钥不匹配。请检查前后端密钥对是否一致", e);
        } catch (Exception e) {
            log.error("SM2解密失败", e);
            throw new RuntimeException("SM2解密失败", e);
        }
    }

    @Override
    public String sm4Encrypt(String plainText, String sm4KeyHex) {
        try {
            byte[] keyBytes = Hex.decodeHex(sm4KeyHex.toCharArray());
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "SM4");
            Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS5Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("SM4加密失败", e);
        }
    }

    @Override
    public String sm4Decrypt(String cipherText, String sm4KeyHex) {
        try {
            byte[] keyBytes = Hex.decodeHex(sm4KeyHex.toCharArray());
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "SM4");
            Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS5Padding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] cipherBytes = Hex.decodeHex(cipherText.toCharArray());
            byte[] decrypted = cipher.doFinal(cipherBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("SM4解密失败", e);
        }
    }

    @Override
    public String sm3Hash(String content) {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        return sm3Hash(data);
    }

    @Override
    public String sm3Hash(byte[] data) {
        SM3Digest digest = new SM3Digest();
        digest.update(data, 0, data.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return Hex.encodeHexString(hash);
    }

    @Override
    public String sm3Hash(InputStream inputStream) {
        try {
            SM3Digest digest = new SM3Digest();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
            byte[] hash = new byte[digest.getDigestSize()];
            digest.doFinal(hash, 0);
            return Hex.encodeHexString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SM3哈希计算失败", e);
        }
    }
}
