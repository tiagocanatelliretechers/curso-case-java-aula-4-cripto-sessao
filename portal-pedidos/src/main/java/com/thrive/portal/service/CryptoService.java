package com.thrive.portal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Lab 4.1 - Cryptographic Failures corrigido.
 *
 * Substitui o "Base64 = proteção" por AES-256-GCM real:
 *  - chave de 256 bits vinda de variavel de ambiente (PORTAL_CRYPTO_KEY, Base64);
 *  - IV de 12 bytes gerado com SecureRandom, unico por operacao;
 *  - persiste IV || ciphertext || tag em Base64 (Base64 aqui e so transporte).
 * GCM autentica: adulteracao e detectada na decifragem.
 */
@Service
public class CryptoService {

    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(@Value("${portal.crypto.key}") String base64Key) {
        byte[] k = Base64.getDecoder().decode(base64Key);
        if (k.length != 32) {
            throw new IllegalStateException("PORTAL_CRYPTO_KEY deve ter 32 bytes (AES-256) em Base64");
        }
        this.key = new SecretKeySpec(k, "AES");
    }

    public String proteger(String textoClaro) {
        if (textoClaro == null) return null;
        try {
            byte[] iv = new byte[IV_LEN];
            random.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = c.doFinal(textoClaro.getBytes(StandardCharsets.UTF_8));
            byte[] out = ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array();
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar", e);
        }
    }

    public String revelar(String protegido) {
        if (protegido == null) return null;
        try {
            byte[] all = Base64.getDecoder().decode(protegido);
            byte[] iv = Arrays.copyOfRange(all, 0, IV_LEN);
            byte[] ct = Arrays.copyOfRange(all, IV_LEN, all.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao decifrar", e);
        }
    }
}
