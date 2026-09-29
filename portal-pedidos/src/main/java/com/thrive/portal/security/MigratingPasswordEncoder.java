package com.thrive.portal.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Lab 3.3 - PasswordEncoder de migracao.
 *
 * - encode(): sempre BCrypt (senhas novas ja nascem seguras).
 * - matches(): aceita hash BCrypt ("$2...") e, para hashes legados MD5,
 *   compara por MD5 (permite login dos usuarios antigos do seed).
 * - upgradeEncoding(): retorna true para hashes legados MD5, sinalizando ao
 *   Spring Security (via UserDetailsPasswordService) para regravar em BCrypt
 *   no proximo login bem-sucedido (rehash on login).
 */
public class MigratingPasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null) return false;
        if (encodedPassword.startsWith("$2")) {
            return bcrypt.matches(rawPassword, encodedPassword);
        }
        return md5(rawPassword.toString()).equalsIgnoreCase(encodedPassword);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return encodedPassword != null && !encodedPassword.startsWith("$2");
    }

    private String md5(String senha) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return String.format("%032x", new BigInteger(1, md.digest(senha.getBytes())));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
