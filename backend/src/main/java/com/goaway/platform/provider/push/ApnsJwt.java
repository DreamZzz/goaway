package com.goaway.platform.provider.push;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * APNs token 鉴权用的 provider JWT（ES256）。
 * 用 Apple 后台下载的 .p8（EC P-256 私钥）签名，60 分钟内可复用，这里缓存 ~50 分钟刷新一次。
 * 线程安全：getToken 加锁刷新。
 */
public class ApnsJwt {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final long REFRESH_AFTER_SECONDS = 50 * 60;

    private final PrivateKey privateKey;
    private final String keyId;
    private final String teamId;

    private volatile String cachedToken;
    private volatile long issuedAt;

    public ApnsJwt(String p8Pem, String keyId, String teamId) {
        this.privateKey = parsePrivateKey(p8Pem);
        this.keyId = keyId;
        this.teamId = teamId;
    }

    public synchronized String getToken() {
        long now = Instant.now().getEpochSecond();
        if (cachedToken == null || now - issuedAt >= REFRESH_AFTER_SECONDS) {
            cachedToken = sign(now);
            issuedAt = now;
        }
        return cachedToken;
    }

    private String sign(long iat) {
        try {
            String header = encode(Map.of("alg", "ES256", "kid", keyId));
            String claims = encode(Map.of("iss", teamId, "iat", iat));
            String signingInput = header + "." + claims;

            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey);
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            byte[] jose = derToJose(signature.sign());

            return signingInput + "." + URL_ENCODER.encodeToString(jose);
        } catch (Exception e) {
            throw new IllegalStateException("APNs JWT 签名失败：" + e.getMessage(), e);
        }
    }

    private static String encode(Map<String, ?> obj) throws Exception {
        return URL_ENCODER.encodeToString(MAPPER.writeValueAsBytes(obj));
    }

    private static PrivateKey parsePrivateKey(String p8Pem) {
        try {
            String normalized = p8Pem
                    .replace("\\n", "\n")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(normalized);
            KeyFactory factory = KeyFactory.getInstance("EC");
            return factory.generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("APNs .p8 私钥解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * 把 JCE 输出的 DER 编码 ECDSA 签名转成 JOSE 要求的定长 R||S（P-256 共 64 字节）。
     */
    static byte[] derToJose(byte[] der) {
        // DER: 0x30 totalLen 0x02 rLen <r…> 0x02 sLen <s…>
        int rLen = der[3] & 0xFF;
        int rStart = 4;
        int sLenIdx = 4 + rLen + 1;     // r 字节之后是 0x02(s 标签)，再之后才是 sLen
        int sLen = der[sLenIdx] & 0xFF;
        int sStart = sLenIdx + 1;

        byte[] out = new byte[64];
        copyFixed(der, rStart, rLen, out, 0);
        copyFixed(der, sStart, sLen, out, 32);
        return out;
    }

    private static void copyFixed(byte[] src, int srcPos, int len, byte[] dst, int dstOffset) {
        // 去掉 DER 可能存在的前导 0x00 符号位；右对齐到 32 字节
        while (len > 0 && src[srcPos] == 0) {
            srcPos++;
            len--;
        }
        int start = dstOffset + (32 - len);
        System.arraycopy(src, srcPos, dst, start, len);
    }
}
