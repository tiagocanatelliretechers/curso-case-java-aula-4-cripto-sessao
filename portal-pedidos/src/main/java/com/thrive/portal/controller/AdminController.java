package com.thrive.portal.controller;

import com.thrive.portal.repository.ClienteRepository;
import com.thrive.portal.repository.PedidoRepository;
import com.thrive.portal.repository.UsuarioRepository;
import com.thrive.portal.service.CryptoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * A01 - Broken Access Control (escalada vertical).
 *
 * A rota /admin/** esta mapeada como apenas "authenticated" no SecurityConfig,
 * entao QUALQUER usuario logado (inclusive um cliente comum) acessa o painel.
 * Alem disso, exibe dados sensiveis "protegidos" apenas com Base64 (A02).
 * Corrigido no Lab 3.5 (@PreAuthorize / hasRole ADMIN).
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CryptoService cryptoService;

    public AdminController(ClienteRepository clienteRepository, PedidoRepository pedidoRepository,
                           UsuarioRepository usuarioRepository, CryptoService cryptoService) {
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.cryptoService = cryptoService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("clientes", clienteRepository.findAll());
        model.addAttribute("pedidos", pedidoRepository.findAll());
        model.addAttribute("usuarios", usuarioRepository.findAll());

        // Revela os dados de pagamento "protegidos" - demonstra A02 no Lab.
        Map<Long, String> pagamentos = new HashMap<>();
        clienteRepository.findAll().forEach(c ->
                pagamentos.put(c.getId(), cryptoService.revelar(c.getDadosPagamento())));
        model.addAttribute("pagamentos", pagamentos);
        return "admin/dashboard";
    }
}
