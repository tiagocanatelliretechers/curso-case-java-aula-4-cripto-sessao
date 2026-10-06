# Aula 4 — Roteiro de Demonstração (INSTRUTOR)

Guia **pronto para demonstrar ao vivo**, sem programar na hora. Padrão:
**mostrar a falha no `baseline` → trocar para o código pronto (`hardened`) → provar que bloqueou → mostrar o trecho que mudou.**

As soluções já estão na tag `aula-4-hardened` (`CryptoService` AES-GCM, `JwtService` com assinatura, cookie/CSRF no `SecurityConfig`, `SeedCryptoRunner`).

## Preparação
```bash
git checkout aula-4-baseline
./scripts/start.sh --lab 4 --no-docker      # Windows: .\scripts\start.ps1 -Lab 4 -NoDocker
```
> Alternar baseline ↔ hardened: `Ctrl+C`, `git checkout aula-4-hardened` (ou baseline), subir de novo (~15s).
> **PowerShell:** use `curl.exe`; para gerar chave: `[Convert]::ToBase64String((1..32|%{Get-Random -Max 256}))`.

---

## DEMO 1 — "Base64 não é criptografia"  · ~6 min

**O que falar:** "O Portal diz que 'protege' o cartão. Vamos ver o que é essa proteção."

### 1a) A falsa proteção (baseline)
O dado de pagamento fica na tabela **`cliente`** (coluna `dados_pagamento`). No H2 (`/h2-console`, URL JDBC `jdbc:h2:mem:portal`, usuário/senha `sa`/`sa`):
```sql
SELECT razao_social, dados_pagamento FROM cliente;
```
Copie o valor (ex.: da ACME) e decodifique no terminal — **sem chave nenhuma**:
```bash
echo "VklTQSA0MTExIDExMTEgMTExMSAxMTExIHZhbCAxMi8yNyBjdnYgMTIz" | base64 -d
```
- **Esperado:** `VISA 4111 1111 1111 1111 val 12/27 cvv 123` — cartão em texto claro.
- **O que dizer:** "Sem chave, sem segredo. Base64 é só um envelope transparente."

> **Alternativa visual (sem H2):** logue como `admin@portal.com`/`admin123` e abra **`/admin`** — a tabela "Dados de pagamento (revelado)" mostra o cartão em claro. (Isto prova a exposição; o diferencial do antes/depois é o **valor armazenado**, que você vê no `SELECT`.)

### 1b) Cifra de verdade (hardened)
```bash
git checkout aula-4-hardened
export PORTAL_CRYPTO_KEY=$(head -c 32 /dev/urandom | base64)
./scripts/start.sh --lab 4 --no-docker
```
No H2, rode o **mesmo** `SELECT razao_social, dados_pagamento FROM cliente;`:
- **Esperado:** agora o `dados_pagamento` é um **blob diferente** (o `SeedCryptoRunner` cifra o seed com AES-GCM na subida). Ao copiar e `base64 -d`, **não** vira o cartão — sai binário/lixo (é ciphertext, não codificação).
- **O que dizer:** "Agora sem a chave (`PORTAL_CRYPTO_KEY`) ninguém recupera. A chave vive fora do código."
- *(No `/admin` o admin continua vendo o cartão revelado — porque ele tem a chave. A diferença do antes/depois está no **valor armazenado**, não na tela do admin.)*

### 1c) Mostre o código
Abra **`CryptoService.java`** (hardened) e destaque:
```java
Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv);   // nonce único por operação
c.init(ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));      // chave de 256 bits (env)
```
- **Conceito em voz alta:** AEAD (confidencialidade + integridade), IV único, chave no ambiente.

---

## DEMO 2 — JWT: forjar um admin (o mais impactante)  · ~7 min

**O que falar:** "Vou virar admin sem senha — só montando um token no terminal."

### 2a) O token forjado é aceito (baseline)
```bash
H=$(printf '{"alg":"none"}' | base64 | tr '+/' '-_' | tr -d '=')
P=$(printf '{"sub":"joao@acme.com","role":"ROLE_ADMIN"}' | base64 | tr '+/' '-_' | tr -d '=')
curl -s -o /dev/null -w 'forjado -> %{http_code}\n' http://localhost:8080/api/pedidos -H "Authorization: Bearer $H.$P."
```
- **Esperado:** `forjado -> 200` (aceito!).
- **O que dizer:** "O servidor só *leu* o payload. Eu escrevi `role: ADMIN`. Ninguém conferiu a assinatura."

### 2b) A verificação (hardened)
```bash
git checkout aula-4-hardened && ./scripts/start.sh --lab 4 --no-docker
# repita o token forjado:
curl -s -o /dev/null -w 'forjado -> %{http_code}\n' http://localhost:8080/api/pedidos -H "Authorization: Bearer $H.$P."   # 401
# token legítimo continua funcionando:
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"joao@acme.com","senha":"senha123"}' | jq -r .token)
curl -s -o /dev/null -w 'legítimo -> %{http_code}\n' http://localhost:8080/api/pedidos -H "Authorization: Bearer $TOKEN"     # 200
```
- **Esperado:** `forjado -> 401`, `legítimo -> 200`.

### 2c) Mostre o código
Abra **`JwtService.java`** (hardened):
```java
Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token); // verifica assinatura e exp
```
- **O que dizer:** "`parseClaimsJws` (com 's') verifica a assinatura. A chave saiu do application.yml para o ambiente."

---

## DEMO 3 — Cookie de sessão + fixation  · ~5 min

**O que falar:** "O cookie é a chave da sessão. Vamos ver se ele está protegido e se a sessão regenera no login."

### 3a) Baseline (DevTools → Application → Cookies)
- Antes do login, anote o `JSESSIONID`. Faça login.
- **Esperado (baseline):** o `JSESSIONID` **não muda** (fixation) e o `Set-Cookie` (aba Network) **não** tem `HttpOnly`.

### 3b) Hardened
- Repita: o `JSESSIONID` **muda** após o login; o `Set-Cookie` mostra `HttpOnly` e `SameSite`.
- Mostre o **`application.yml`** (flags `http-only/secure/same-site`) e o **`SecurityConfig.java`** (sem `sessionFixation().none()`).
- **O que dizer:** "HttpOnly some com roubo por XSS; regenerar o ID mata a fixation."

> Nota: em `http://localhost` deixe `secure: false` em dev, senão o navegador descarta o cookie.

---

## DEMO 4 — CSRF  · ~5 min

**O que falar:** "Um site externo consegue disparar uma ação no Portal usando o cookie da vítima? Vamos testar."

> **Importante:** o corpo correto do POST é `produtoId` e `quantidade` (não `item`). E, no **hardened**, até o **login** exige o token CSRF — por isso o fluxo abaixo pega o token da página de login antes. **A forma mais simples de rodar isto é pela collection Postman `materiais/postman/Aula4-CSRF...json`** (ela extrai o token sozinha). Abaixo, a versão curl.

### 4a) Baseline (CSRF off) — ataque direto funciona
```bash
curl -s -c cookies.txt --data-urlencode "username=joao@acme.com" --data-urlencode "password=senha123" http://localhost:8080/login -o /dev/null
curl -s -o /dev/null -w 'forjado -> %{http_code}\n' -X POST http://localhost:8080/pedidos -b cookies.txt \
  --data-urlencode "produtoId=1" --data-urlencode "quantidade=1"
```
- **Esperado (baseline):** `forjado -> 302` — a ação forjada **passou** (CSRF desligado).

### 4b) Hardened (CSRF on) — precisa do token até para logar
```bash
# 1) pega cookie + token CSRF da página de login
TOKEN=$(curl -s -c j.txt http://localhost:8080/login | grep -oE 'name="_csrf"[^>]*value="[^"]*"' | grep -oE 'value="[^"]*"' | sed 's/value="//;s/"//')
# 2) loga COM o token
curl -s -b j.txt -c j.txt --data-urlencode "username=joao@acme.com" --data-urlencode "password=senha123" --data-urlencode "_csrf=$TOKEN" http://localhost:8080/login -o /dev/null
# 3) POST forjado SEM token -> deve ser bloqueado
curl -s -o /dev/null -w 'forjado(sem csrf) -> %{http_code}\n' -b j.txt -X POST http://localhost:8080/pedidos \
  --data-urlencode "produtoId=1" --data-urlencode "quantidade=1"
```
- **Esperado (hardened):** `forjado(sem csrf) -> 403` — bloqueado. (Com o token, o fluxo legítimo retorna 302.)

### 4c) Mostre o código
Abra **`SecurityConfig.java`** (hardened) — o `.csrf(...)` deixou de ser `disable`: fica ligado para a web e só ignora a API (`ignoringRequestMatchers("/api/**")`).
- **O que dizer:** "O token CSRF prova que a requisição veio da nossa página. A API com JWT no header não é afetada (o navegador não anexa esse header sozinho)."

---

## Encerramento (30s)
Amarre o mapa: **Base64→AES-GCM**, **JWT lido→JWT verificado**, **cookie nu→cookie blindado + regen**, **CSRF off→CSRF on**. Depois os alunos repetem no `lab.md`.

## Erros comuns na hora da demo
- **`jq` ausente:** copie o token do JSON manualmente.
- **Cookie some com `secure:true` em http:** use `secure:false` em dev.
- **Token legítimo dá 401 no hardened:** a `PORTAL_JWT_SECRET` de assinatura e verificação precisa ser a mesma.
- **H2 recria a base a cada restart:** esperado (em memória) — regrave o dado cifrado se necessário (SeedCryptoRunner cuida disso no hardened).
