package com.thrive.portal.service;

import com.thrive.portal.domain.Comprovante;
import com.thrive.portal.repository.ComprovanteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class UploadService {

    private static final long MAX = 5L * 1024 * 1024; // 5 MB
    // Lab 2.4 - allowlist por magic number (assinatura dos primeiros bytes)
    private static final Map<String, String> ASSINATURAS = Map.of(
            "pdf", "25504446",   // %PDF
            "png", "89504e47",   // .PNG
            "jpg", "ffd8ff"      // JPEG
    );

    private final ComprovanteRepository comprovanteRepository;
    private final String uploadDir;

    public UploadService(ComprovanteRepository comprovanteRepository,
                         @Value("${portal.upload.dir}") String uploadDir) {
        this.comprovanteRepository = comprovanteRepository;
        this.uploadDir = uploadDir;
    }

    /**
     * Lab 2.4 - upload seguro:
     *  - valida tamanho (anti-DoS)
     *  - decide o tipo pelo CONTEUDO (magic number), nao pela extensao
     *  - gera o nome de arquivo no servidor (sem path traversal)
     */
    public Comprovante salvar(Long pedidoId, MultipartFile arquivo) throws IOException {
        if (arquivo.isEmpty() || arquivo.getSize() > MAX) {
            throw new IllegalArgumentException("Arquivo ausente ou acima do limite de 5MB");
        }
        byte[] head = new byte[8];
        try (InputStream in = arquivo.getInputStream()) {
            in.read(head);
        }
        String hex = HexFormat.of().formatHex(head);
        String tipo = ASSINATURAS.entrySet().stream()
                .filter(e -> hex.startsWith(e.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de arquivo nao permitido"));

        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String nomeSeguro = UUID.randomUUID() + "." + tipo;   // nome gerado pelo servidor
        Path destino = dir.resolve(nomeSeguro).normalize();
        if (!destino.startsWith(dir)) {                        // defesa extra contra traversal
            throw new IllegalArgumentException("Caminho invalido");
        }
        arquivo.transferTo(destino);

        Comprovante c = new Comprovante();
        c.setPedidoId(pedidoId);
        c.setNomeArquivo(nomeSeguro);
        c.setCaminho(destino.toString());
        c.setContentType(arquivo.getContentType());
        c.setTamanho(arquivo.getSize());
        return comprovanteRepository.save(c);
    }
}
