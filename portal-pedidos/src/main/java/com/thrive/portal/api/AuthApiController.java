package com.thrive.portal.api;

import com.thrive.portal.domain.Usuario;
import com.thrive.portal.dto.LoginApiRequest;
import com.thrive.portal.security.JwtService;
import com.thrive.portal.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public AuthApiController(UsuarioService usuarioService, JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginApiRequest req) {
        Usuario u = usuarioService.porEmail(req.getEmail()).orElse(null);
        // A07 - sem rate limiting; mensagens permitem inferir contas validas
        if (u == null || !usuarioService.senhaConfere(req.getSenha(), u.getSenha())) {
            return ResponseEntity.status(401).body(Map.of("erro", "credenciais invalidas"));
        }
        String token = jwtService.gerarToken(u.getEmail(), u.getRole());
        return ResponseEntity.ok(Map.of("token", token, "role", u.getRole()));
    }
}
