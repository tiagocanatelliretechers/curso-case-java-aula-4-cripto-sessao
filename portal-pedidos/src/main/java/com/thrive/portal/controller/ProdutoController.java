package com.thrive.portal.controller;

import com.thrive.portal.service.ProdutoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping("/produtos")
    public String listar(Model model) {
        model.addAttribute("produtos", produtoService.listarTodos());
        model.addAttribute("termo", "");
        return "produtos/lista";
    }

    /**
     * A03 - a busca repassa o termo diretamente para uma query concatenada.
     * Endpoint publico (permitAll) para facilitar a demonstracao no laboratorio.
     */
    @GetMapping("/produtos/buscar")
    public String buscar(@RequestParam(defaultValue = "") String termo, Model model) {
        model.addAttribute("produtos", produtoService.buscar(termo));
        model.addAttribute("termo", termo);
        return "produtos/lista";
    }
}
