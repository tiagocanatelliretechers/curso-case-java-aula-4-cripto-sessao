package com.thrive.portal.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A02 - Cryptographic Failures.
 *
 * Este servico DEVERIA proteger dados sensiveis (ex.: dados de pagamento do
 * cliente). No baseline ele apenas aplica Base64 - que e CODIFICACAO, nao
 * criptografia. Qualquer pessoa com acesso ao dado consegue reverter.
 *
 * Sera corrigido no Lab 4.1 (AES-256-GCM com chave fora do codigo).
 */
@Service
public class CryptoService {

    public String proteger(String textoClaro) {
        if (textoClaro == null) {
            return null;
        }
        // VULNERAVEL: Base64 nao e criptografia.
        return Base64.getEncoder().encodeToString(textoClaro.getBytes(StandardCharsets.UTF_8));
    }

    public String revelar(String protegido) {
        if (protegido == null) {
            return null;
        }
        return new String(Base64.getDecoder().decode(protegido), StandardCharsets.UTF_8);
    }
}
