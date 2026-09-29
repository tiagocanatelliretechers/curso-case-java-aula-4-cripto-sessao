package com.thrive.portal.service;

import com.thrive.portal.domain.Usuario;
import com.thrive.portal.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Lab 3.3 - verificacao via PasswordEncoder (BCrypt; migra MD5 legado). */
    public boolean senhaConfere(String senhaDigitada, String hashArmazenado) {
        return passwordEncoder.matches(senhaDigitada, hashArmazenado);
    }

    public Usuario registrar(String email, String senha, String role, Long clienteId) {
        Usuario u = new Usuario();
        u.setEmail(email);
        // Lab 3.3 - grava hash BCrypt (com salt e work factor)
        u.setSenha(passwordEncoder.encode(senha));
        u.setRole(role);
        u.setClienteId(clienteId);
        return usuarioRepository.save(u);
    }

    public Long clienteIdDoEmail(String email) {
        return usuarioRepository.findByEmail(email).map(Usuario::getClienteId).orElse(null);
    }

    public Optional<Usuario> porEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    /**
     * Lab 3.4 - fluxo "esqueci minha senha" SEM user enumeration:
     * a resposta e sempre a mesma, exista ou nao o e-mail. O envio real do
     * token (uso unico, expiracao curta) ocorreria de forma assincrona.
     */
    public String recuperarSenha(String email) {
        usuarioRepository.findByEmail(email).ifPresent(u -> {
            // gerar token de uso unico + expiracao e enviar por e-mail (fora do escopo do lab)
        });
        return "Se o e-mail estiver cadastrado, enviaremos as instrucoes de redefinicao.";
    }
}
