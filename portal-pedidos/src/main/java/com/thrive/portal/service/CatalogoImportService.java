package com.thrive.portal.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * A10 - Server-Side Request Forgery (SSRF).
 *
 * Funcionalidade "importar catalogo do fornecedor a partir de uma URL".
 * No baseline, a aplicacao busca QUALQUER URL informada pelo usuario, sem
 * validar host/esquema. Um atacante pode apontar para recursos internos:
 *   http://169.254.169.254/latest/meta-data/   (metadados de cloud)
 *   http://localhost:8080/actuator/env          (segredos internos)
 *   file:///etc/passwd                          (leitura de arquivo local)
 *
 * Sera discutido/corrigido como parte de Secure Design (allowlist de destino).
 */
@Service
public class CatalogoImportService {

    public String importarDe(String urlInformada) throws Exception {
        // VULNERAVEL: sem validacao de esquema, host ou faixa de IP.
        URL url = new URL(urlInformada);
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
