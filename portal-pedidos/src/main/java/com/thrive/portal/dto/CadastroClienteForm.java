package com.thrive.portal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lab 2.3 - DTO com Bean Validation (allowlist de formato/tamanho) + @CNPJ. */
public class CadastroClienteForm {

    @NotBlank
    @Size(max = 150)
    private String razaoSocial;

    @NotBlank
    @CNPJ
    private String cnpj;

    @NotBlank
    @Email
    @Size(max = 180)
    private String email;

    // Lab 3.4 - politica minima de senha
    @NotBlank
    @Size(min = 10, max = 100)
    private String senha;

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}
