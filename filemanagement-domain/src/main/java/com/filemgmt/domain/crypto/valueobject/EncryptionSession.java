package com.filemgmt.domain.crypto.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 加密会话信息值对象
 */
@Getter
@AllArgsConstructor
public class EncryptionSession {

    private final String sessionId;
    private final String sm4KeyHex;
}
