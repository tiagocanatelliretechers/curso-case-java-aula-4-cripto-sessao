# Guia do Instrutor — Dinâmica e Roteiro de Aula
### Curso CASE Java · da Aula 3 (Parte 2) em diante

Este guia responde a três coisas:
1. **O que mudou** nos materiais (e por quê).
2. **A nova dinâmica de aula** — como conduzir sem virar "babá" de hands-on.
3. **O roteiro aula a aula** — cronometrado, pronto para demonstrar os "pré-labs" e criar a conexão teoria↔prática que os alunos pediram.

---

## 1. O que mudou (e por que)

O feedback da turma foi claro: o conteúdo é bom, mas **faltou conexão entre teoria e prática**, os conceitos passavam rápido demais e, no laboratório, virava "seguir passos sem entender". A resposta não é dar mais aula expositiva — é **mudar a dinâmica** e dar a cada aula os artefatos que sustentam essa dinâmica.

Cada aula (3 Parte 2 → 6) passou a ter **quatro artefatos que trabalham juntos**:

| Artefato | Para quem | O que faz |
|----------|-----------|-----------|
| **Deck de conteúdo** (`conteudo-…pptx`) | turma | o conceito com profundidade ("por que fazemos isto", "conceito em 1 minuto"), diagramas e exemplos |
| **`roteiro-demonstracao.md`** (novo) | **você** | o **pré-lab**: script pronto para demonstrar ao vivo — exploit no `baseline` → correção pronta (`hardened`) → provar → mostrar o código. **Sem programar na hora.** |
| **`lab.md`** (reformulado) | aluno | o hands-on **autoguiado**: cada passo com "o que observar / o que significa", "ponto de entendimento" e troubleshooting |
| **`lab-aula-N.pptx`** (reconstruído) | turma | o deck que você projeta durante o lab (mapa conceito→lab, os 4 momentos da demo, o hands-on) |

Mudanças de destaque:
- **Conexão explícita teoria↔prática:** todo lab tem um **"Mapa conceito → laboratório"** e, em cada exercício, um **"Conecte com a teoria"** apontando o slide.
- **Entender > entregar:** cada exercício termina num **"Ponto de entendimento"** (uma pergunta que o aluno deve saber responder), e a entrega inclui respondê-las.
- **Autonomia no lab:** cada passo diz **o que observar** e **o que significa**, e há **"Se der errado"** (troubleshooting) — reduz a dependência de você a cada dúvida.
- **Soluções já prontas:** as correções vivem nas tags `aula-N-hardened`. Você **demonstra** a solução via `git checkout`, sem codar ao vivo.
- **Sem Docker no aluno:** as Aulas 5 e 6 ganharam caminhos sem Docker (SonarLint, ZAP Desktop, Trivy nativo), porque as VMs não rodam Docker Desktop. Docker fica opcional/instrutor.

> A Aula 3 foi dividida em **Parte 1 (Autenticação)**, **Parte 2 (Autorização)** e **Laboratório**. A refatoração começa na **Parte 2** — é a partir dela que a nova dinâmica vale.

---

## 2. A nova dinâmica de aula — o loop de 4 tempos

A ideia central: **para cada tópico, um ciclo curto** de conceito → demonstração → prática → checagem. O aluno **vê** o conceito virar ataque e correção **antes** de pôr a mão — é isso que cria a conexão. Você **demonstra**; eles **replicam**.

```
┌─────────────┐   ┌──────────────────┐   ┌───────────────────┐   ┌──────────────┐
│ 1. CONCEITO │ → │ 2. PRÉ-LAB (você) │ → │ 3. HANDS-ON (eles)│ → │ 4. CHECAGEM  │
│  ~6–8 min   │   │  demonstração     │   │  autoguiado       │   │  ~3–5 min    │
│  (slides)   │   │  ~6–8 min         │   │  ~15–25 min       │   │  (entend.)   │
└─────────────┘   └──────────────────┘   └───────────────────┘   └──────────────┘
        repete por tópico/lab da aula  ↺
```

**1. Conceito (você apresenta ~6–8 min).** Use o deck de conteúdo, focando o **"por quê"** — não leia bullets. Termine na pergunta que o pré-lab vai responder ("será que dá para ver o pedido de outro cliente?").

**2. Pré-lab / demonstração (você demonstra ~6–8 min).** Siga o `roteiro-demonstracao.md` do tópico: mostre a **falha no `baseline`**, troque para o **`hardened`**, **prove** que bloqueou e **abra o código** que mudou. Este é o momento que amarra teoria e prática. **Os alunos assistem, ainda não codam.**

**3. Hands-on (eles fazem ~15–25 min).** Os alunos refazem no `lab.md`. Como acabaram de ver a demonstração e o guia é autoexplicativo (o que observar / o que significa / troubleshooting), **você não precisa fazer por eles** — circula e tira dúvidas pontuais.

**4. Checagem (~3–5 min).** Faça o **"Ponto de entendimento"** do exercício (pergunta rápida à turma). Se muita gente travar, retome o "conceito em 1 minuto" antes de seguir. Mede **compreensão**, não conclusão.

### Como conduzir sem virar "babá"
- **A demonstração carrega o peso.** Quando você mostra o ataque e a correção, a maioria das dúvidas some antes do hands-on.
- **Aponte para o `lab.md`, não repita o passo.** Se perguntarem "o que faço agora?", responda "qual passo do lab.md e o que ele diz em *o que observar*?". Ensina a usar o guia.
- **Use o troubleshooting.** Antes de resolver, pergunte "o que a seção *Se der errado* sugere?".
- **Peer help.** Quem terminou explica para quem travou (consolidação dupla).
- **Timebox visível.** Anuncie o tempo de cada hands-on; ao acabar, mostre a solução (`hardened`) e siga — não espere 100% terminarem.
- **"Entender, não entregar."** Repita isso. A entrega inclui responder aos pontos de entendimento; quem só copiou não consegue.
- **Opcional (turmas grandes):** peça que rodem o **pré-lab de casa** (o `roteiro-demonstracao.md` é público no repo) — chegam com o contexto pronto e a aula rende mais.

---

## 3. Roteiro aula a aula

Legenda: **C** = Conceito (slides) · **D** = Demonstração/pré-lab (você, via `roteiro-demonstracao.md`) · **H** = Hands-on (aluno, via `lab.md`) · **✔** = Checagem (ponto de entendimento).
Prepare sempre antes: `git checkout aula-N-baseline` e `./scripts/start.sh --lab N --no-docker`.

---

### Aula 3 · Parte 2 — Autorização (após a Parte 1 e o lab de autenticação)
**Materiais:** `aula-3/conteudo-aula-3-parte2-kafka-style.pptx` · `aula-3/roteiro-demonstracao.md` · `aula-3/lab.md` · `aula-3/lab-aula-3.pptx`
**Abertura (10 min):** recap da Parte 1 + o "mapa conceito→lab" (slide do deck do lab). Enuncie a regra de ouro: *autenticado ≠ autorizado*.

| Bloco | Tempo | O quê |
|------|-------|-------|
| AuthZ, modelos (RBAC/ABAC/ReBAC) | C 10 | deck de conteúdo, foco no "por quê" |
| **IDOR** | D 8 · H 25 · ✔ 5 | D: `roteiro` Demo 1 (trocar id → 200 no baseline → 403 no hardened → `PedidoService`). H: Lab 3.1 + 3.2. ✔: "se eu esconder o link, o ataque ainda funciona?" |
| **MD5 → BCrypt** | C 5 · D 6 · H 20 · ✔ 4 | D: Demo 2 (hash no H2 → migração no login). H: Lab 3.3. ✔: "por que dois hashes iguais viram diferentes?" |
| **Rate limiting** | D 4 · H 15 · ✔ 3 | D: Demo 3 (6 tentativas → 429). H: Lab 3.4. ✔: "por que não bloquear a conta de vez?" |
| **RBAC /admin** | D 4 · H 10 · ✔ 3 | D: Demo 4 (joao entra no /admin → 403 no hardened). H: Lab 3.5 (se sobrar tempo). ✔: `authenticated()` × `hasRole('ADMIN')` |
| Fechamento | 8 | recap + autoavaliação do `lab.md` |

---

### Aula 4 — Criptografia & Sessão
**Materiais:** `aula-4/conteudo-aula-4.*` · `aula-4/roteiro-demonstracao.md` · `aula-4/lab.md` · `aula-4/lab-aula-4.pptx`
**Abertura (8 min):** "hoje protegemos os dados de verdade e blindamos a sessão" + mapa conceito→lab.

| Bloco | Tempo | O quê |
|------|-------|-------|
| Codificação × criptografia (AEAD) | C 8 | deck; a diferença Base64 vs cifra |
| **Base64 → AES-256-GCM** | D 6 · H 25 · ✔ 4 | D: Demo 1 (`base64 -d` mostra o cartão → cifra no hardened). H: Lab 4.1. ✔: "por que não reusar o IV?" |
| JWT: anatomia e assinatura | C 6 | deck; o que a assinatura garante |
| **JWT forjado (`alg:none`)** | D 7 · H 25 · ✔ 4 | D: Demo 2 (token forjado 200 → 401 no hardened → `JwtService`). H: Lab 4.2. ✔: "por que `alg:none` é catastrófico?" |
| **Cookie seguro + fixation** | C 4 · D 5 · H 20 · ✔ 3 | D: Demo 3 (JSESSIONID muda no login; flags no Set-Cookie). H: Lab 4.3. ✔: "para que serve `HttpOnly`?" |
| **CSRF** | C 4 · D 5 · H 20 · ✔ 3 | D: Demo 4 (POST sem token → 403 no hardened). H: Lab 4.4. ✔: "por que a API com token no header não precisa de CSRF?" |
| Fechamento | 8 | recap + autoavaliação |

---

### Aula 5 — Error Handling + SAST & DAST  *(sem Docker no aluno)*
**Materiais:** `aula-5/conteudo-aula-5.*` · `aula-5/roteiro-demonstracao.md` · `aula-5/lab.md` · `aula-5/lab-aula-5.pptx`
**Abertura (8 min):** "ferramenta sem interpretação é ruído" + mapa conceito→lab. Avise: **sem Docker** — SonarLint e ZAP Desktop.

| Bloco | Tempo | O quê |
|------|-------|-------|
| Vazamento por erro / log injection | C 6 | deck |
| **Error handling** | D 7 · H 25 · ✔ 4 | D: Demo 1 (`/pedidos/abc` stack trace → genérico no hardened). H: Lab 5.1. ✔: "o que um stack trace entrega ao atacante?" |
| SAST — como funciona (source→sink) | C 8 | deck; VP × FP |
| **SAST (SonarLint)** | D 8 · H 30 · ✔ 5 | D: Demo 2 (rodar na IDE + **triar** 1 finding VP/FP). H: Lab 5.2 (triar ≥5, corrigir 1 alto). ✔: "por que um finding pode ser falso positivo?" |
| DAST — caixa-preta | C 6 | deck; SAST × DAST |
| **DAST (ZAP Desktop)** | D 8 · H 30 · ✔ 5 | D: Demo 3 (scan + **validar** 1 finding com `curl -I`). H: Lab 5.3. ✔: "por que validar manualmente?" |
| Fechamento | 8 | recap; SAST × DAST se complementam |

> Bônus (se **você** tiver Docker): mostrar SonarQube em container + Quality Gate. Não é necessário.

---

### Aula 6 — Deploy Seguro Ponta a Ponta  *(sem Docker no aluno)*
**Materiais:** `aula-6/conteudo-aula-6.*` · `aula-6/roteiro-demonstracao.md` · `aula-6/lab.md` · `aula-6/lab-aula-6.pptx`
**Abertura (8 min):** "código seguro não basta — a operação, as dependências, a imagem e o pipeline também" + mapa.

| Bloco | Tempo | O quê |
|------|-------|-------|
| Superfície mínima em produção | C 5 | deck |
| **Actuator / H2** | D 6 · H 20 · ✔ 4 | D: Demo 1 (`/actuator/env` 200 → 401/404). H: Lab 6.1. ✔: "por que `/actuator/env` é perigoso?" |
| SCA e supply chain (CVE/CVSS) | C 7 | deck; Text4Shell |
| **SCA (Dependency-Check)** | D 7 · H 25 · ✔ 4 | D: Demo 2 (relatório aponta `commons-text` 1.9 → atualizar). H: Lab 6.2. ✔: "por que dependência vulnerável é tão grave quanto bug próprio?" |
| Imagem de container segura | C 5 · D 6 · H 25 · ✔ 4 | D: Demo 3 (`trivy config/fs` + Dockerfile hardened). H: Lab 6.3. ✔: "por que rodar não-root reduz o risco?" |
| Shift-left & gates no CI | C 5 · D 5 · H 20 · ✔ 3 | D: Demo 4 (reintroduzir CVE → gate falha). H: Lab 6.4. ✔: "por que shift-left compensa?" |
| Fechamento do curso | 12 | ciclo ponta a ponta; encaminhar **CTF** (`aula-6/anexos/ctf.md`) e **Simulado** (`simulado-final.md`) |

---

## 4. Checklist de preparação (antes de cada aula)
- [ ] `git checkout aula-N-baseline` e `./scripts/start.sh --lab N --no-docker` sobem sem erro.
- [ ] Testei cada **Demo** do `roteiro-demonstracao.md` da aula (comandos + saída esperada).
- [ ] Deixei dois terminais prontos (comandos / reiniciar app) e o navegador em `localhost:8080`.
- [ ] Aula 5/6: SonarLint instalado na IDE; ZAP Desktop baixado; Trivy nativo instalado; **NVD API key** configurada (Aula 6).
- [ ] Projetor com o **deck de conteúdo** e o **`lab-aula-N.pptx`** abertos.
- [ ] Combinei os timeboxes de hands-on (e o momento de mostrar o `hardened` e seguir).

## 5. Sinais de que a dinâmica está funcionando
- Os alunos **respondem** aos pontos de entendimento sem consultar o passo a passo.
- As dúvidas migram de "o que eu faço?" para "por que isso acontece?".
- No hands-on, eles usam o `lab.md` (e o "Se der errado") **antes** de te chamar.
- Na checagem, conseguem **reexplicar** o ataque e a correção com as próprias palavras.
