package com.thrive.portal.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final String secret;
    private final long expirationMs;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtService(@Value("${portal.jwt.secret}") String secret,
                      @Value("${portal.jwt.expiration-ms}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    private Key key() {
        // A02 - chave de baixa entropia, hardcoded no repositorio e "esticada"
        // com zeros para atingir o tamanho minimo. Sera corrigida no Lab 4.2.
        byte[] bytes = Arrays.copyOf(secret.getBytes(StandardCharsets.UTF_8), 32);
        return Keys.hmacShaKeyFor(bytes);
    }

    public String gerarToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key())
                .compact();
    }

    /**
     * A02/A07 - VALIDACAO QUEBRADA.
     *
     * Em vez de verificar a assinatura, este metodo apenas decodifica o payload
     * (Base64) e confia nas claims. Isso aceita:
     *  - tokens forjados com qualquer assinatura
     *  - tokens com header {"alg":"none"} e sem assinatura
     *  - escalada de privilegio trocando "role":"ROLE_USER" por "ROLE_ADMIN"
     *
     * Sera corrigido no Lab 4.2 (parseClaimsJws com chave forte).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> lerClaimsSemVerificar(String token) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length < 2) {
                return null;
            }
            byte[] payload = Base64.getUrlDecoder().decode(partes[1]);
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            return null;
        }
    }
}
