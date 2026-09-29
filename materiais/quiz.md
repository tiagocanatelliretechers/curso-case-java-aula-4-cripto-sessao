# Aula 4 — Quiz de fixação (10 questões)

**1.** Para proteger dados de pagamento que precisam ser **recuperados**, você usa:
- a) Hash SHA-256
- b) Cifra simétrica (ex.: AES-256-GCM)
- c) Base64
- d) bcrypt

**2.** Base64 aplicado a um dado sensível oferece:
- a) Confidencialidade forte
- b) Nenhuma proteção — é codificação reversível sem chave
- c) Integridade
- d) Não-repúdio

**3.** Qual modo/algoritmo é recomendado hoje para cifrar dados em repouso?
- a) AES-ECB
- b) DES
- c) AES-256-GCM
- d) RC4

**4.** Para gerar um IV/nonce ou token de segurança em Java, use:
- a) `java.util.Random`
- b) `Math.random()`
- c) `java.security.SecureRandom`
- d) `System.nanoTime()`

**5.** Em AES-GCM, reutilizar o mesmo IV com a mesma chave:
- a) É recomendado para performance
- b) É catastrófico para a segurança
- c) Não faz diferença
- d) Só afeta a velocidade

**6.** O ataque `alg:none` em JWT explora:
- a) Chaves muito longas
- b) Parsers que aceitam tokens sem assinatura válida
- c) HTTPS mal configurado
- d) Cookies HttpOnly

**7.** A melhor defesa contra `alg:none`/algorithm confusion é:
- a) Aumentar a expiração do token
- b) Verificar a assinatura exigindo o algoritmo e a chave esperados
- c) Guardar o token em localStorage
- d) Usar Base64 no payload

**8.** Regenerar o ID de sessão após o login previne:
- a) SQL Injection
- b) Session fixation
- c) SSRF
- d) XSS

**9.** Quando é aceitável desabilitar a proteção CSRF?
- a) Sempre
- b) Nunca
- c) Em APIs puramente stateless que usam token em header (não em cookie)
- d) Em qualquer aplicação com login

**10.** (Laboratório) Após o Lab 4.2, o token forjado com `alg:none` e `role:ROLE_ADMIN`:
- a) Continua sendo aceito
- b) É rejeitado (401), pois a assinatura é verificada
- c) Vira um token válido de admin
- d) Faz a aplicação cair

---

## Gabarito comentado
1. **b** — Precisa recuperar → cifra simétrica.
2. **b** — Base64 é codificação, não cripto.
3. **c** — AES-256-GCM (cifra autenticada).
4. **c** — `SecureRandom` é o CSPRNG.
5. **b** — Reuso de nonce em GCM quebra a segurança.
6. **b** — Parser permissivo aceita token sem assinatura.
7. **b** — Verificar assinatura com alg/chave esperados.
8. **b** — Anti session fixation.
9. **c** — API stateless com token em header não sofre CSRF.
10. **b** — Assinatura verificada → forjado rejeitado.
