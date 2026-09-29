package com.thrive.portal.service;

import com.thrive.portal.domain.Produto;
import com.thrive.portal.repository.ProdutoRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProdutoService {

    private final JdbcTemplate jdbcTemplate;
    private final ProdutoRepository produtoRepository;

    public ProdutoService(JdbcTemplate jdbcTemplate, ProdutoRepository produtoRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.produtoRepository = produtoRepository;
    }

    public List<Produto> listarTodos() {
        return produtoRepository.findAll();
    }

    /**
     * Lab 2.2 - SQL Injection corrigido com query PARAMETRIZADA.
     * O termo (incluindo o curinga %) e enviado como parametro, nunca concatenado
     * ao texto da query, de modo que aspas/UNION no input viram dados literais.
     */
    public List<Produto> buscar(String termo) {
        String sql = "SELECT id, nome, descricao, preco, estoque FROM produto "
                + "WHERE nome LIKE ? OR descricao LIKE ?";
        String like = "%" + termo + "%";
        return jdbcTemplate.query(sql, ps -> {
            ps.setString(1, like);
            ps.setString(2, like);
        }, (rs, rowNum) -> {
            Produto p = new Produto();
            p.setId(rs.getLong("id"));
            p.setNome(rs.getString("nome"));
            p.setDescricao(rs.getString("descricao"));
            p.setPreco(rs.getBigDecimal("preco"));
            p.setEstoque(rs.getObject("estoque") == null ? null : rs.getInt("estoque"));
            return p;
        });
    }

    public Produto porId(Long id) {
        return produtoRepository.findById(id).orElse(null);
    }

    public BigDecimal precoDe(Long id) {
        Produto p = porId(id);
        return p == null ? BigDecimal.ZERO : p.getPreco();
    }
}
