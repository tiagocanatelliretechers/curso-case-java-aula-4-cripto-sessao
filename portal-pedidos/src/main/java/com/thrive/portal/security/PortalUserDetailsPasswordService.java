package com.thrive.portal.security;

import com.thrive.portal.domain.Usuario;
import com.thrive.portal.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.stereotype.Service;

/**
 * Lab 3.3 - persiste o hash migrado (MD5 -> BCrypt) no proximo login bem-sucedido.
 * O DaoAuthenticationProvider chama updatePassword() quando o encoder indica
 * upgradeEncoding()==true e a senha confere.
 */
@Service
public class PortalUserDetailsPasswordService implements UserDetailsPasswordService {

    private final UsuarioRepository usuarioRepository;

    public PortalUserDetailsPasswordService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        Usuario u = usuarioRepository.findByEmail(user.getUsername()).orElse(null);
        if (u != null) {
            u.setSenha(newPassword);          // ja vem em BCrypt
            usuarioRepository.save(u);
        }
        return User.withUserDetails(user).password(newPassword).build();
    }
}
