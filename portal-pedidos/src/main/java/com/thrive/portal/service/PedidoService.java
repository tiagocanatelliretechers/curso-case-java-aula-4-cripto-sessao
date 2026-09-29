package com.thrive.portal.service;

import com.thrive.portal.domain.ItemPedido;
import com.thrive.portal.domain.Pedido;
import com.thrive.portal.repository.PedidoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoService produtoService;

    public PedidoService(PedidoRepository pedidoRepository, ProdutoService produtoService) {
        this.pedidoRepository = pedidoRepository;
        this.produtoService = produtoService;
    }

    public List<Pedido> doCliente(Long clienteId) {
        return pedidoRepository.findByClienteId(clienteId);
    }

    /**
     * Lab 3.2 - IDOR corrigido: exige o cliente autenticado e verifica a POSSE
     * do recurso antes de retorna-lo. Cliente que nao e dono recebe 403.
     */
    public Pedido porIdDoCliente(Long id, Long clienteId) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado"));
        if (clienteId == null || !pedido.getClienteId().equals(clienteId)) {
            throw new AccessDeniedException("Pedido nao pertence ao cliente autenticado");
        }
        return pedido;
    }

    public Pedido criar(Long clienteId, List<ItemPedido> itens) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(clienteId);
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal preco = produtoService.precoDe(item.getProdutoId());
            item.setPrecoUnitario(preco);
            item.setPedido(pedido);
            total = total.add(preco.multiply(BigDecimal.valueOf(item.getQuantidade())));
            pedido.getItens().add(item);
        }
        pedido.setTotal(total);
        return pedidoRepository.save(pedido);
    }

    public Pedido salvar(Pedido pedido) {
        return pedidoRepository.save(pedido);
    }
}
