package com.thrive.portal.controller;

import com.thrive.portal.service.CatalogoImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ImportController {

    private static final Logger log = LoggerFactory.getLogger(ImportController.class);

    private final CatalogoImportService catalogoImportService;

    public ImportController(CatalogoImportService catalogoImportService) {
        this.catalogoImportService = catalogoImportService;
    }

    @GetMapping("/importar-catalogo")
    public String form() {
        return "importar-catalogo";
    }

    /**
     * A10 - SSRF (CatalogoImportService) + A09 - Log Injection.
     * A URL informada pelo usuario e registrada no log sem sanitizacao
     * (permite injetar linhas falsas de log com \n).
     */
    @PostMapping("/importar-catalogo")
    public String importar(@RequestParam String url, Model model) {
        log.info("Importacao de catalogo solicitada para URL: " + url);
        try {
            String conteudo = catalogoImportService.importarDe(url);
            model.addAttribute("conteudo", conteudo);
        } catch (Exception e) {
            model.addAttribute("erro", e.getMessage());
        }
        return "importar-catalogo";
    }
}
