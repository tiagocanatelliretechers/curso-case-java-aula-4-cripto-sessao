package com.thrive.portal.config;

import com.thrive.portal.repository.ClienteRepository;
import com.thrive.portal.service.CryptoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Lab 4.1 - cifra (AES-GCM) os dados de pagamento do seed no startup, para que
 * fiquem cifrados em repouso. Em producao, o dado ja chegaria cifrado.
 */
@Component
public class SeedCryptoRunner implements CommandLineRunner {

    private final ClienteRepository clienteRepository;
    private final CryptoService cryptoService;

    public SeedCryptoRunner(ClienteRepository clienteRepository, CryptoService cryptoService) {
        this.clienteRepository = clienteRepository;
        this.cryptoService = cryptoService;
    }

    @Override
    public void run(String... args) {
        clienteRepository.findAll().forEach(c -> {
            String dado = c.getDadosPagamento();
            if (dado != null && !dado.isBlank() && ehTextoClaro(dado)) {
                c.setDadosPagamento(cryptoService.proteger(dado));
                clienteRepository.save(c);
            }
        });
    }

    // Heuristica simples: o seed entra em texto claro (contem espacos/letras de cartao).
    private boolean ehTextoClaro(String v) {
        return v.contains(" ");
    }
}
