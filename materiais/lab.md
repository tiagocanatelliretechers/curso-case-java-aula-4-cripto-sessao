# Aula 4 — Guia de Laboratório (aluno)
## Criptografia + Gestão de Sessão

**Duração:** ~100 min · **Ponto de partida:** `aula-4-baseline` (IDOR/senhas já corrigidos na Aula 3).
**Ambiente:** JDK 17 + IDE + navegador (+ `jq`, `base64`/`curl` no terminal).
Subir o app: `./scripts/start.sh --lab 4 --no-docker` (ou `.\scripts\start.ps1 -Lab 4 -NoDocker`).

---

## Como usar este guia (leia antes de começar)

O objetivo **não é só concluir os passos** — é entender *por que* cada proteção existe. Se ao final de um exercício você não consegue explicar o "porquê" com suas palavras, **releia o "Conceito em 1 minuto" ou chame o instrutor** antes de avançar.

Cada exercício tem: **Por que fazemos isto** (contexto/risco) · **Conceito em 1 minuto** (fundamento) · **Passo a passo** com *o que observar* e *o que isso significa* · **Ponto de entendimento** (pergunta de compreensão) · **Se der errado** (troubleshooting) · **Conecte com a teoria** (slide).

> Dica de ritmo: leia "Por que fazemos isto" + "Conceito em 1 minuto" antes de digitar qualquer coisa.

## Mapa: conceito → laboratório

| Conceito (slides) | Vulnerabilidade no Portal | Lab | O que você vai provar |
|-------------------|---------------------------|-----|-----------------------|
| Codificação ≠ criptografia; AEAD | dados de pagamento em Base64 | 4.1 | Que "Base64" é reversível sem chave — e como cifrar de verdade. |
| JWT: assinatura precisa ser verificada | token aceito sem validar assinatura | 4.2 | Forjar um admin com `alg:none` — e como rejeitar. |
| Flags de cookie + session fixation | cookie sem flags; sessão não regenera | 4.3 | Roubo/fixação de sessão — e como blindar o cookie. |
| CSRF em fluxos por cookie | CSRF desabilitado | 4.4 | POST forjado de outro site — e como bloquear. |

## Objetivos de aprendizagem
Ao final você deve conseguir **explicar**:
1. A diferença entre **codificar** (Base64) e **cifrar** (AES-GCM), e por que a chave nunca fica no código.
2. Por que um IV/nonce deve ser único por operação e vir de `SecureRandom`.
3. Por que "ler" um JWT sem **verificar a assinatura** é o mesmo que não ter autenticação.
4. O papel de cada flag do cookie de sessão (`HttpOnly`, `Secure`, `SameSite`) e o que é session fixation.
5. Por que CSRF afeta fluxos baseados em cookie e não a API stateless com token no header.

---

## Lab 4.1 — De Base64 para AES-256-GCM (25 min)

**Por que fazemos isto:** o Portal guarda dados de pagamento "protegidos" em **Base64** — e Base64 **não é criptografia**, é só uma representação. Qualquer um decodifica sem chave nenhuma. Quando o banco vazar, os cartões vazam junto. Precisamos de cifra de verdade para dados em repouso.

**Conceito em 1 minuto:** **codificar** (Base64) transforma bytes em texto de forma reversível *e pública* — não protege nada. **Cifrar** exige uma **chave secreta**. Use **AES-256-GCM** (AEAD = *Authenticated Encryption*): além de esconder o dado (confidencialidade), detecta adulteração (integridade). Duas regras de ouro: a **chave vive fora do código** (variável de ambiente/secret manager) e o **IV/nonce** (12 bytes) é **único por operação**, gerado com `SecureRandom` — reusar IV no GCM quebra a segurança.

**Passo 0 — prove que Base64 não protege:**
O dado de pagamento fica na tabela **`cliente`** (coluna `dados_pagamento`). Pegue o valor no H2 (`/h2-console`, JDBC `jdbc:h2:mem:portal`, `sa`/`sa`):
```sql
SELECT razao_social, dados_pagamento FROM cliente;
```
Copie o valor e decodifique no terminal (sem chave nenhuma):
```bash
echo "VklTQSA0MTExIDExMTEgMTExMSAxMTExIHZhbCAxMi8yNyBjdnYgMTIz" | base64 -d
```
- *O que observar:* o número do cartão volta **em texto claro** (ou veja revelado em `/admin` como admin).
- *O que isso significa:* não havia proteção — só codificação. Sem chave, sem segredo.

**Passo a passo (correção):**
1. Reescreva o `CryptoService` para **AES-256-GCM**:
   - chave de 256 bits vinda de **variável de ambiente** (`PORTAL_CRYPTO_KEY`, em Base64) — nunca do código;
   - **IV/nonce de 12 bytes** por operação, com `SecureRandom`;
   - persista `IV || ciphertext || tag` concatenados em Base64 (aqui Base64 é só **transporte**, não proteção).
   ```java
   Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
   byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv);           // único por operação
   c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
   byte[] ct = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
   // guardar: base64(iv + ct)
   ```
   - *O que isso significa:* o dado em repouso vira ininteligível sem a chave; a tag GCM detecta adulteração.

2. Gere e exporte a chave do ambiente:
   ```bash
   export PORTAL_CRYPTO_KEY=$(head -c 32 /dev/urandom | base64)
   ```
   - *O que observar:* a chave nasce fora do código.

3. Regrave os dados de pagamento cifrados e valide:
   - *O que observar:* rode de novo `SELECT razao_social, dados_pagamento FROM cliente;` — o valor agora é um blob que, no `base64 -d`, **não** vira o cartão (é ciphertext AES-GCM); com a chave, o serviço recupera o original.
   - *O que isso significa:* confidencialidade em repouso + chave separada do dado.

**Ponto de entendimento:** *Por que não podemos reutilizar o mesmo IV para cifrar vários registros?*
> Resposta esperada: no GCM, reusar IV com a mesma chave vaza informação e permite forjar/derivar dados — cada operação precisa de um nonce único.

**Se der errado:**
- `AEADBadTagException` ao decifrar → IV/tag não estão sendo lidos na mesma ordem em que foram gravados.
- `InvalidKeyException` / tamanho de chave → a `PORTAL_CRYPTO_KEY` não tem 32 bytes (256 bits) após o Base64-decode.
- Recupera lixo → você decodificou Base64 como se fosse o texto (Base64 é só o envelope; a decifra é o AES).

**Conecte com a teoria:** slides "Codificação × Criptografia", "AES-GCM (AEAD)" e "Gestão de chaves".

---

## Lab 4.2 — Corrigir o JWT (25 min)

**Por que fazemos isto:** o Portal aceita o JWT **sem verificar a assinatura**. Isso é pior do que parece: qualquer pessoa monta um token dizendo `role: ADMIN` e entra como admin, sem senha. Um token não verificado é papel timbrado que ninguém confere.

**Conceito em 1 minuto:** um JWT tem 3 partes: header, payload e **assinatura**. A assinatura prova que o token foi emitido pelo servidor (com a chave secreta) e não foi alterado. Se o servidor apenas **lê** o payload (ou aceita `alg:none`), a assinatura não vale nada — o cliente controla os claims. A correção: **verificar a assinatura** com uma chave forte (≥ 256 bits, vinda do ambiente), validar `exp`, e rejeitar `alg:none`.

**Passo 0 — forje um admin (baseline):**
```bash
H=$(printf '{"alg":"none"}' | base64 | tr '+/' '-_' | tr -d '=')
P=$(printf '{"sub":"joao@acme.com","role":"ROLE_ADMIN"}' | base64 | tr '+/' '-_' | tr -d '=')
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/pedidos -H "Authorization: Bearer $H.$P."
```
- *O que observar:* o token forjado (sem assinatura) é **aceito** (200).
- *O que isso significa:* o servidor confiou em claims que o **atacante** escreveu. Isso é escalada de privilégio trivial.

**Passo a passo (correção):**
1. Em `JwtService`, troque a leitura sem verificação por parsing **com verificação de assinatura**:
   ```java
   Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token); // lança se assinatura/exp inválidos
   ```
2. Use uma **chave forte** (≥ 256 bits) vinda de variável de ambiente; **remova** o segredo do `application.yml`.
3. Reduza a expiração (ex.: 15 min) e valide `exp`.
4. Reinicie e **repita o Passo 0**.
   - *O que observar:* o token `alg:none`/forjado agora dá **401**.
   - *O que isso significa:* só tokens assinados com a chave do servidor passam.
5. Confirme que um token **legítimo** (via `/api/auth/login`) continua funcionando.

**Ponto de entendimento:** *Por que aceitar `alg:none` é catastrófico?*
> Resposta esperada: `alg:none` diz "não há assinatura"; se o servidor honra isso, qualquer payload é aceito como verdadeiro — inclusive `role: ADMIN`.

**Se der errado:**
- Token legítimo passou a dar 401 → a chave de verificação difere da usada para assinar (confira a mesma `PORTAL_JWT_SECRET`).
- Ainda aceita forjado → você continua usando `parseClaimsJwt` (sem "s") ou lendo o payload manualmente; use `parseClaimsJws`.

**Conecte com a teoria:** slides "Anatomia do JWT", "alg:none e o token não verificado" e "Validação de assinatura/exp".

---

## Lab 4.3 — Cookie seguro + regeneração de sessão (25 min)

**Por que fazemos isto:** o cookie de sessão é a "chave" que identifica o usuário logado. Sem flags de proteção, ele pode ser roubado por JavaScript (XSS) ou fixado por um atacante antes do login. Blindar o cookie e regenerar a sessão no login fecha esses vetores.

**Conceito em 1 minuto:** três flags no cookie de sessão: **HttpOnly** (JavaScript não lê o cookie → some o roubo por XSS), **Secure** (só trafega em HTTPS → não vaza em rede), **SameSite** (limita envio cross-site → reduz CSRF). Além disso, **session fixation**: se o ID de sessão não muda no login, um atacante que plantou um ID antes consegue "herdar" a sessão autenticada — por isso **regeneramos o ID no login** (`changeSessionId`, o default seguro).

**Passo a passo:**
1. No `application.yml`, ligue os flags:
   ```yaml
   server.servlet.session.cookie.http-only: true
   server.servlet.session.cookie.secure: true      # em dev sem HTTPS pode ficar false; documente
   server.servlet.session.cookie.same-site: lax
   ```
2. No `SecurityConfig`, remova `sessionFixation().none()` (deixe o default `changeSessionId()`).
3. Capture o `JSESSIONID` **antes** do login (DevTools → Application → Cookies) e faça login.
   - *O que observar:* o `JSESSIONID` **muda** após o login.
   - *O que isso significa:* a sessão foi regenerada — anti fixation.
4. Inspecione o `Set-Cookie` (DevTools → Network).
   - *O que observar:* aparecem `HttpOnly` e `SameSite`.

**Ponto de entendimento:** *Por que `HttpOnly` reduz o impacto de um XSS?*
> Resposta esperada: com `HttpOnly`, o `document.cookie` não enxerga o cookie de sessão, então um script injetado não consegue exfiltrá-lo.

**Se der errado:**
- Login "não gruda"/volta pro login com `secure: true` em `http://localhost` → o navegador descarta cookie `Secure` sem HTTPS; use `secure: false` **só em dev** (com `same-site: lax`).
- O ID não muda → ainda há `sessionFixation().none()` no `SecurityConfig`.

**Conecte com a teoria:** slides "Flags do cookie de sessão" e "Session fixation".

---

## Lab 4.4 — Reativar CSRF (25 min)

**Por que fazemos isto:** o Portal desabilitou o CSRF globalmente. Em fluxos que autenticam por **cookie**, isso permite que outro site force o navegador da vítima a enviar uma ação (o navegador anexa o cookie sozinho). Reativar o CSRF exige um token que só a sua aplicação conhece.

**Conceito em 1 minuto:** CSRF explora que o navegador **envia o cookie automaticamente** em qualquer requisição para o seu domínio — inclusive as disparadas por um site malicioso. O **token CSRF** (um valor aleatório por sessão, exigido em requisições de escrita) prova que a requisição veio da **sua** página, e não de um formulário externo. A **API stateless** que autentica por token no header **não** é afetada (o navegador não anexa esse header sozinho).

**Passo a passo:**
1. No `SecurityConfig`, **remova** o `.csrf(disable)` dos fluxos web (mantendo a API stateless separada).
2. Garanta que os formulários Thymeleaf enviem o token (o Spring injeta em forms com `th:action`).
3. Teste o ataque: dispare um POST **sem** token para um endpoint de escrita (`/pedidos`), simulando um site externo. O corpo correto usa `produtoId`/`quantidade`. **Atenção:** no hardened, até o login exige o token CSRF — então pegue o token da página de login primeiro:
   ```bash
   # 1) cookie + token CSRF da página de login
   TOKEN=$(curl -s -c j.txt http://localhost:8080/login | grep -oE 'name="_csrf"[^>]*value="[^"]*"' | grep -oE 'value="[^"]*"' | sed 's/value="//;s/"//')
   # 2) loga com o token
   curl -s -b j.txt -c j.txt --data-urlencode "username=joao@acme.com" --data-urlencode "password=senha123" --data-urlencode "_csrf=$TOKEN" http://localhost:8080/login -o /dev/null
   # 3) ataque: POST sem token CSRF
   curl -s -o /dev/null -w '%{http_code}\n' -b j.txt -X POST http://localhost:8080/pedidos \
     --data-urlencode "produtoId=1" --data-urlencode "quantidade=1"
   ```
   - *O que observar:* **403** (bloqueado) no hardened; **302** no baseline (passou).
   - *O que isso significa:* sem o token, o servidor não confia na origem da requisição.
   > Mais simples: use a collection `materiais/postman/Aula4-CSRF...json` (extrai o token sozinha).
4. Confirme o fluxo normal: pegue um token fresco de `/pedidos` (já logado) e inclua `_csrf` no POST → **302** (funciona).

**Ponto de entendimento:** *Por que a API com JWT no header não precisa de proteção CSRF?*
> Resposta esperada: o navegador não anexa automaticamente um header `Authorization`; o atacante não consegue forjá-lo a partir de outro site, então o vetor CSRF não se aplica.

**Se der errado:**
- Formulário legítimo passou a dar 403 → o token não está sendo enviado (form sem `th:action` ou campo hidden ausente).
- API quebrou → você reativou CSRF para a API stateless também; mantenha a API separada (token em header).

**Conecte com a teoria:** slides "CSRF em fluxos por cookie" e "Token CSRF x API stateless".

---

## Autoavaliação (responda sem olhar o gabarito)
1. Explique a diferença entre Base64 e AES-GCM, citando "chave" e "IV".
2. O que um atacante faz com um servidor que não verifica a assinatura do JWT?
3. Para que serve cada flag: `HttpOnly`, `Secure`, `SameSite`?
4. Por que CSRF afeta cookie e não a API com token no header?

> Travou em alguma? Volte ao "Conceito em 1 minuto" do lab antes de seguir para a Aula 5.

## Entrega
- `CryptoService` (AES-GCM) e `JwtService` (assinatura verificada).
- Config de cookie + regeneração de sessão; CSRF reativado na web.
- Evidências: Base64 decodificado (4.1) e token `alg:none` **rejeitado** (4.2).
- Uma frase por lab respondendo ao respectivo "Ponto de entendimento".

## Onde pedir ajuda / erros comuns gerais
- **App não sobe / login volta pro login:** rode com `-NoDocker`; em dev use `secure: false` + `same-site: lax`.
- **Comparar com a solução:** `git checkout aula-4-hardened`.
- **Travou num conceito?** Não avance "só para entregar" — chame o instrutor.
