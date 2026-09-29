# Aula 4 — Slides (outline)
## Secure Coding para Criptografia + Gestão de Sessão

> 240 min (2 intervalos de 10 min). Dois módulos com laboratório em cada.
> Abertura: "Na Aula 3 corrigimos senhas e acesso. Hoje: proteger dados de verdade
> (não Base64) e blindar a sessão/JWT."

---

### BLOCO 1 (50 min) — Criptografia aplicada

**Slide 1 — Objetivos**
- Escolher a primitiva certa (cifra vs. hash vs. HMAC).
- Usar algoritmos aprovados e modos seguros.
- Gerenciar chaves fora do código.
- Corrigir a "proteção" Base64 e o JWT no laboratório.

**Slide 2 — Cifra vs. Hash vs. HMAC**
- **Cifra (simétrica/assimétrica):** dado que precisa ser **recuperado** (dados de pagamento).
- **Hash:** dado que só precisa ser **verificado** (senha — via bcrypt, que é hash lento).
- **HMAC:** verificar **integridade + autenticidade** com chave secreta (ex.: assinar token).
- **Nota:** Pergunta-chave: "preciso ler de volta?" Sim→cifra; Não→hash.

**Slide 3 — Simétrica vs. Assimétrica**
- Simétrica (AES): mesma chave cifra/decifra; rápida; para dados em repouso/volume.
- Assimétrica (RSA/EC): par público/privado; troca de chave, assinatura; mais lenta.
- Híbrido: assimétrica para trocar chave, simétrica para os dados (ex.: TLS).
- **Nota:** No Portal, dados de pagamento → AES simétrico.

**Slide 4 — Algoritmos: aprovados vs. obsoletos**
- **Usar:** AES-256-**GCM** (cifra autenticada), RSA-2048+ com **OAEP**, SHA-256+.
- **Evitar:** DES/3DES/RC4, MD5/SHA-1 (para segurança), RSA PKCS#1 v1.5 puro.
- **Nunca ECB** (padrões vazam — mostrar a imagem do "pinguim ECB").
- **Nota:** GCM dá confidencialidade **e** integridade num passo.

**Slide 5 — "Criptografar" ≠ "codificar"**
- **Base64 não é criptografia** — é codificação reversível sem chave.
  ```java
  Base64.getEncoder().encodeToString(cartao); // NÃO protege nada
  ```
- Erro clássico: achar que Base64/hex/“ofuscação” protege dado sensível.
- **Nota:** Decodifique ao vivo o `dados_pagamento` do Portal para provar (Lab 4.1).

**Slide 6 — Gestão de chaves**
- Nunca hardcode chave no código/repositório.
- KMS/HSM/Vault; variáveis de ambiente como mínimo; rotação e escopo.
- Separe chave de dado (não guarde a chave ao lado do ciphertext).
- **Nota:** Ligue com `/actuator/env` da Aula 1 vazando o segredo do JWT.

**Slide 7 — SecureRandom e IV/nonce**
- Aleatoriedade de segurança: **`java.security.SecureRandom`** (CSPRNG).
- **Nunca** `java.util.Random` para chaves/IV/tokens.
- GCM: IV/nonce único por operação (12 bytes), nunca reutilizar com a mesma chave.
- **Nota:** Reuso de nonce em GCM é catastrófico — enfatize "único por mensagem".

**Slide 8 — JCA/JCE na prática (Java)**
- `Cipher.getInstance("AES/GCM/NoPadding")`, `KeyGenerator`, `GCMParameterSpec`.
- Armazenar: IV + ciphertext + tag juntos (ex.: concatenar e Base64 para persistir).
- **Nota:** Base64 **aqui** é só transporte do ciphertext — o dado já está cifrado.

**Slide 9 — TLS (dado em trânsito)**
- Exigir **TLS 1.2+** (idealmente 1.3); cipher suites fortes.
- Erros comuns em Java: `TrustManager` que confia em qualquer certificado; desabilitar verificação de host.
- Certificate pinning quando fizer sentido (mobile/cliente controlado).
- **Nota:** Mostre o anti-padrão do TrustManager permissivo e por que é perigoso.

**Slide 10 — Dados sensíveis no Portal**
- **Senha:** hash (bcrypt) — já feito na Aula 3.
- **Dados de pagamento:** cifra AES-256-GCM em repouso + TLS em trânsito.
- **Nada especial:** dados públicos do catálogo.
- **Nota:** Classifique o dado antes de decidir o controle.

---

### BLOCO 2 (40 min) — Laboratório de Criptografia

**Slide 11 — Laboratório (parte cripto)**
- Lab 4.1: provar que Base64 não protege → implementar AES-256-GCM com chave em variável de ambiente e `SecureRandom` no IV.
- Lab 4.2: corrigir geração/validação do JWT (chave forte fora do código, expiração curta) e demonstrar `alg:none` antes.
- **Nota:** Distribua `lab.md`. Peça que rodem o ataque `alg:none` do baseline primeiro.

---

### INTERVALO (10 min)

---

### BLOCO 3 (40 min) — Session Management

**Slide 12 — Ciclo de vida seguro da sessão**
- ID com entropia suficiente (framework cuida, se bem configurado).
- **Regenerar** o ID após login (anti session fixation).
- Invalidar no logout e por **timeout** (inatividade e absoluto).
- **Nota:** No baseline, `sessionFixation().none()` desliga a proteção — Lab 4.3.

**Slide 13 — Cookies de sessão seguros**
- **HttpOnly** (JS não lê), **Secure** (só HTTPS), **SameSite** (Lax/Strict).
- Domínio/path corretos; nome sem revelar tecnologia quando possível.
- **Nota:** No baseline os flags estão desligados no `application.yml`.

**Slide 14 — Stateful vs. Stateless**
- **JSESSIONID** (stateful): estado no servidor; fácil invalidar; escala com sticky/replicação.
- **JWT** (stateless): sem estado no servidor; difícil revogar antes de expirar.
- Trade-offs para o Portal (web usa sessão; API usa JWT).
- **Nota:** "Stateless não é 'melhor' — é diferente; revogação é o calcanhar do JWT."

**Slide 15 — JWT com segurança**
- **Validar a assinatura** sempre; fixar o algoritmo esperado (rejeitar `alg:none` e confusão HS/RS).
- Expiração curta + refresh token; validar `iss`/`aud`/`exp`.
- Onde guardar no cliente: cookie **HttpOnly** > localStorage (exposto a XSS).
- **Nota:** No baseline o JWT não é verificado (aceita forjado/`alg:none`) — Lab 4.2.

**Slide 16 — Ataque alg:none e algorithm confusion**
- `alg:none`: token sem assinatura aceito por parsers permissivos.
- Confusão HS/RS: assinar com a chave pública (tratada como segredo HMAC).
- Defesa: exigir algoritmo específico e verificar com a chave correta.
- **Nota:** Demonstração prática no Lab 4.2.

**Slide 17 — CSRF**
- Ataque: navegador envia cookies automaticamente em requisição forjada.
- Por que ainda importa mesmo com SameSite: navegadores antigos, subdomínios, exceções.
- Proteção nativa do Spring Security (token sincronizador) para fluxos com cookie.
- Quando desabilitar: API **puramente stateless** com token em **header** (não em cookie).
- **Nota:** No baseline, CSRF está desabilitado globalmente — Lab 4.4 reativa para os fluxos com cookie.

**Slide 18 — Timeout e concurrent session control**
- Timeout de inatividade + tempo máximo absoluto.
- Limitar sessões simultâneas por usuário; encerrar sessões antigas.
- **Nota:** Spring Security `maximumSessions(1)` como exemplo.

---

### BLOCO 4 (60 min) — Laboratório de Sessão

**Slide 19 — Laboratório (parte sessão)**
- Lab 4.3: flags de cookie (HttpOnly/Secure/SameSite) + regeneração de ID no login.
- Lab 4.4: reativar CSRF para os fluxos com cookie + teste de requisição forjada bloqueada.
- **Nota:** Se o tempo apertar, priorize 4.3 (impacto direto) e demonstre 4.4.

---

### INTERVALO (10 min)

---

### BLOCO 5 (20 min) — Debrief + Quiz

**Slide 20 — Debrief**
- Mostrar o ciphertext AES (e que sem a chave não se recupera o dado).
- Rodar o token `alg:none` contra a versão corrigida (deve ser rejeitado).
- Inspecionar o `Set-Cookie` com os flags corretos.
- **Nota:** Contraponha com o baseline lado a lado.

**Slide 21 — Quiz + ponte para a Aula 5**
- Quiz (`quiz.md`).
- Aula 5: tratamento de erros e testes (SAST/DAST) — vamos escanear a app.
- **Nota:** Aula 5 parte de `aula-5-baseline` (cripto/sessão corrigidos).
