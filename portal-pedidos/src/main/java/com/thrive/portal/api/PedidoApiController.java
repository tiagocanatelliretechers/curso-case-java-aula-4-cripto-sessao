package com.thrive.portal.api;

import com.thrive.portal.domain.Pedido;
import com.thrive.portal.domain.Usuario;
import com.thrive.portal.service.PedidoService;
import com.thrive.portal.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoApiController {

    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;

    public PedidoApiController(PedidoService pedidoService, UsuarioService usuarioService) {
        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<Pedido> meus(Principal principal) {
        if (principal == null) {
            return List.of();
        }
        Usuario u = usuarioService.porEmail(principal.getName()).orElse(null);
        return u == null ? List.of() : pedidoService.doCliente(u.getClienteId());
    }

    /** Lab 3.2 - object-level authorization: verifica posse antes de retornar. */
    @GetMapping("/{id}")
    public ResponseEntity<?> porId(@PathVariable Long id, Principal principal) {
        Usuario u = principal == null ? null
                : usuarioService.porEmail(principal.getName()).orElse(null);
        Long clienteId = u == null ? null : u.getClienteId();
        Pedido pedido = pedidoService.porIdDoCliente(id, clienteId); // lanca 403/404
        return ResponseEntity.ok(pedido);
    }
}
