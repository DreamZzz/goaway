package com.goaway.platform.provider.push;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;

import static org.junit.jupiter.api.Assertions.*;

class ApnsJwtTest {

    /**
     * derToJose 的黄金验证：真签一次（DER）→ 转 JOSE(64B) → 再拼回 DER → 用公钥验签必须通过。
     * ECDSA 带随机性，重复多次以覆盖 r/s 出现前导零等边界。
     */
    @RepeatedTest(20)
    @DisplayName("DER→JOSE 转换后重组 DER 仍能通过 P-256 验签")
    void derToJose_roundTripsToValidSignature() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = kpg.generateKeyPair();

        byte[] msg = "header.payload".getBytes(StandardCharsets.US_ASCII);

        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(kp.getPrivate());
        signer.update(msg);
        byte[] der = signer.sign();

        byte[] jose = ApnsJwt.derToJose(der);
        assertEquals(64, jose.length, "JOSE 签名必须是定长 64 字节");

        byte[] rebuiltDer = joseToDer(jose);
        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(kp.getPublic());
        verifier.update(msg);
        assertTrue(verifier.verify(rebuiltDer), "重组 DER 应通过验签（证明 R||S 提取正确）");
    }

    // --- 测试辅助：把 JOSE 的 R||S 拼回 DER ECDSA 签名 ---

    private static byte[] joseToDer(byte[] jose) {
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(jose, 0, r, 0, 32);
        System.arraycopy(jose, 32, s, 0, 32);
        byte[] rEnc = derInt(r);
        byte[] sEnc = derInt(s);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x30);
        out.write(rEnc.length + sEnc.length);
        out.writeBytes(rEnc);
        out.writeBytes(sEnc);
        return out.toByteArray();
    }

    private static byte[] derInt(byte[] magnitude) {
        BigInteger value = new BigInteger(1, magnitude);
        byte[] bytes = value.toByteArray(); // 已是最小补码表示，必要时含前导 0x00
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x02);
        out.write(bytes.length);
        out.writeBytes(bytes);
        return out.toByteArray();
    }
}
