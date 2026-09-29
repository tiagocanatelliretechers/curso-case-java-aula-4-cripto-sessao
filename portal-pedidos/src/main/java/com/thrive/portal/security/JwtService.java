package com.thrive.portal.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

/**
 * Lab 4.2 - JWT com verificacao de assinatura.
 *
 * Substitui a leitura sem verificacao por parseClaimsJws, que exige assinatura
 * valida (HS256) com a chave esperada. Isso rejeita tokens forjados e o ataque
 * alg:none. A chave (>= 256 bits) vem de configuracao/ambiente, nao do codigo.
 */
@Service
public class JwtService {

    private final Key key;
    private final long expirationMs;

    public JwtService(@Value("${portal.jwt.secret}") String base64Secret,
                      @Value("${portal.jwt.expiration-ms:900000}") long expirationMs) {
        byte[] bytes = Base64.getDecoder().decode(base64Secret);
        this.key = Keys.hmacShaKeyFor(bytes);   // exige >= 32 bytes
        this.expirationMs = expirationMs;        // 15 min por padrao
    }

    public String gerarToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Lanca JwtException se assinatura/algoritmo/expiracao forem invalidos. */
    public Jws<Claims> validar(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }
}
