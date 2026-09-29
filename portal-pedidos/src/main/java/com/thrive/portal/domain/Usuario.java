package com.thrive.portal.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // A02 - senha armazenada como hash MD5 sem salt (ver UsuarioService)
    @Column(nullable = false)
    private String senha;

    // "ROLE_ADMIN" ou "ROLE_USER"
    @Column(nullable = false)
    private String role;

    // Cliente ao qual o usuario pertence (null para admin)
    @Column(name = "cliente_id")
    private Long clienteId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
}
