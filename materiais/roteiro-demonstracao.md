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
No H2 (`/h2-console`, `jdbc:h2:mem:portal`, `sa/sa`):
```sql
SELECT dados_pagamento FROM pedido;   -- (ou onde o pagamento é guardado)
```
Copie o valor e decodifique no terminal:
```bash
echo "VklTQSA0MTExIDExMTEgMTExMSAxMTExIHZhbCAxMi8yNyBjdnYgMTIz" | base64 -d
```
- **Esperado:** `VISA 4111 1111 1111 1111 val 12/27 cvv 123` — cartão em texto claro.
- **O que dizer:** "Sem chave, sem segredo. Base64 é só um envelope transparente."

### 1b) Cifra de verdade (hardened)
```bash
git checkout aula-4-hardened
export PORTAL_CRYPTO_KEY=$(head -c 32 /dev/urandom | base64)
./scripts/start.sh --lab 4 --no-docker
```
No H2, rode o mesmo `SELECT`:
- **Esperado:** um blob Base64 que, ao `base64 -d`, **não** vira o cartão (é ciphertext AES-GCM).
- **O que dizer:** "Agora sem a chave (`PORTAL_CRYPTO_KEY`) ninguém recupera. A chave vive fora do código."

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

### 4a) Prepare uma sessão (cookie) autenticada
```bash
# guarde o cookie de uma sessão logada (ajuste conforme o fluxo de login web)
curl -s -c cookies.txt -d "username=joao@acme.com&password=senha123" http://localhost:8080/login -o /dev/null
```

### 4b) POST forjado, sem token CSRF
```bash
curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8080/pedidos -b cookies.txt -d "item=X&quantidade=1"
```
- **Baseline (CSRF off):** aceita (`200/302`) — a ação forjada passou.
- **Hardened (CSRF on):** **403** — sem token, bloqueado.

### 4c) Mostre o código
Abra **`SecurityConfig.java`** (hardened) — o `.csrf(...)` deixou de estar `disable` para a web; a API stateless segue com token no header.
- **O que dizer:** "O token CSRF prova que a requisição veio da nossa página. A API com JWT no header não é afetada."

---

## Encerramento (30s)
Amarre o mapa: **Base64→AES-GCM**, **JWT lido→JWT verificado**, **cookie nu→cookie blindado + regen**, **CSRF off→CSRF on**. Depois os alunos repetem no `lab.md`.

## Erros comuns na hora da demo
- **`jq` ausente:** copie o token do JSON manualmente.
- **Cookie some com `secure:true` em http:** use `secure:false` em dev.
- **Token legítimo dá 401 no hardened:** a `PORTAL_JWT_SECRET` de assinatura e verificação precisa ser a mesma.
- **H2 recria a base a cada restart:** esperado (em memória) — regrave o dado cifrado se necessário (SeedCryptoRunner cuida disso no hardened).
