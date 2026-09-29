package com.thrive.portal.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "comprovante")
public class Comprovante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id")
    private Long pedidoId;

    private String nomeArquivo;

    private String caminho;

    private String contentType;

    private Long tamanho;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }
    public String getNomeArquivo() { return nomeArquivo; }
    public void setNomeArquivo(String nomeArquivo) { this.nomeArquivo = nomeArquivo; }
    public String getCaminho() { return caminho; }
    public void setCaminho(String caminho) { this.caminho = caminho; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getTamanho() { return tamanho; }
    public void setTamanho(Long tamanho) { this.tamanho = tamanho; }
}
