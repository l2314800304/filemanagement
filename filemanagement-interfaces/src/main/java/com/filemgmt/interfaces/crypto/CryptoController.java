package com.filemgmt.interfaces.crypto;

import com.filemgmt.domain.crypto.service.CryptoDomainService;
import com.filemgmt.domain.crypto.service.Sm2KeyPort;
import com.filemgmt.interfaces.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    private final Sm2KeyPort sm2KeyPort;
    private final CryptoDomainService cryptoService;

    public CryptoController(Sm2KeyPort sm2KeyPort, CryptoDomainService cryptoService) {
        this.sm2KeyPort = sm2KeyPort;
        this.cryptoService = cryptoService;
    }

    /**
     * 获取SM2公钥（不加密）
     */
    @GetMapping("/public-key")
    public Result<Map<String, String>> getPublicKey() {
        Map<String, String> data = Map.of("publicKey", sm2KeyPort.getPublicKeyHex());
        return Result.success(data);
    }

    /**
     * 诊断端点：测试SM2加解密是否正常（仅用于开发调试）
     */
    @GetMapping("/diagnose")
    public Result<Map<String, Object>> diagnoseSm2(@RequestParam(required = false, defaultValue = "false") boolean enabled) {
        if (!enabled) {
            return Result.success(Map.of("message", "诊断功能未启用，请添加 ?enabled=true 参数"));
        }

        try {
            String publicKey = sm2KeyPort.getPublicKeyHex();
            String privateKey = sm2KeyPort.getPrivateKeyHex();

            // 测试加密解密
            String testPlain = "Hello SM2 Test";
            String encrypted = cryptoService.sm2Encrypt(testPlain, publicKey);
            String decrypted = cryptoService.sm2Decrypt(encrypted, privateKey);

            boolean match = testPlain.equals(decrypted);

            return Result.success(Map.of(
                    "publicKeyPrefix", publicKey.substring(0, Math.min(20, publicKey.length())),
                    "privateKeyPrefix", privateKey.substring(0, Math.min(20, privateKey.length())),
                    "testPlainText", testPlain,
                    "encryptedLength", encrypted.length(),
                    "encryptedPrefix", encrypted.substring(0, Math.min(4, encrypted.length())),
                    "decryptedText", decrypted,
                    "match", match
            ));
        } catch (Exception e) {
            return Result.success(Map.of(
                    "error", e.getMessage(),
                    "stackTrace", e.toString()
            ));
        }
    }
}
