# Aula 4 — Criptografia e Gestão de Sessão

=== Criptografia Aplicada

## O que vamos aprender hoje
> Na Aula 3 protegemos identidade e acesso. Hoje protegemos os dados de verdade — e blindamos a sessão e o token.
- Cifra x hash x HMAC, algoritmos aprovados e gestão de chaves.
- Sessão segura, cookies, JWT e CSRF.
- Lab: substituir "Base64" por AES-GCM, verificar assinatura de JWT e proteger a sessão.
Regra de ouro: a pergunta certa é "preciso ler este dado de volta?" — ela decide qual primitiva usar.

## Cifra x Hash x HMAC
> Três ferramentas diferentes para três problemas diferentes — escolher errado é a falha.
Teoria:
- Cifra (simétrica/assimétrica): protege dado que precisa ser recuperado (ex.: dados de pagamento).
- Hash: verifica sem guardar o original (ex.: senha, via bcrypt).
- HMAC: verifica integridade + autenticidade com uma chave secreta (ex.: assinar um token).
Analogia: cifrar é trancar no cofre; hashear é triturar; o HMAC é o lacre que prova que ninguém abriu.

## Simétrica x Assimétrica
> Duas famílias de cifra, com papéis complementares.
Teoria: simétrica (AES) usa a mesma chave para cifrar e decifrar — rápida, ideal para volume e dados em repouso. Assimétrica (RSA/EC) usa par público/privado — mais lenta, ótima para troca de chave e assinatura.
Exemplo tangível: o TLS combina as duas — assimétrica para trocar uma chave, simétrica para cifrar os dados da sessão.
No Portal: para dados de pagamento em repouso, usamos AES (simétrica).

## Algoritmos: aprovados x obsoletos
> Usar o algoritmo certo, no modo certo, é metade da batalha.
Teoria:
- Use: AES-256-GCM (cifra autenticada), RSA-2048+ com OAEP, SHA-256+.
- Evite: DES/3DES/RC4, MD5/SHA-1 para segurança, RSA PKCS#1 v1.5 puro.
- Nunca ECB: blocos iguais viram cifra igual, e o padrão vaza (o clássico "pinguim ECB").
Regra de ouro: prefira GCM — ele entrega confidencialidade E integridade em um passo.

## "Criptografar" não é "codificar"
> O erro mais comum de criptografia é não usar criptografia nenhuma.
Teoria: Base64 é codificação — reversível sem chave. Não oferece confidencialidade alguma.
```java
// "Proteção" falsa: qualquer um decodifica
String protegido = Base64.getEncoder().encodeToString(cartao.getBytes());
```
Analogia: Base64 é escrever de trás para frente — parece embaralhado, mas qualquer um lê no espelho.
No Portal: os dados de pagamento estão em Base64; no Lab 4.1 decodificamos ao vivo para provar que não há proteção.

## Gestão de chaves
> A cifra só é tão forte quanto o sigilo da chave — e a chave não mora no código.
Teoria: nunca faça hardcode da chave; use KMS/HSM/Vault ou, no mínimo, variáveis de ambiente; rotacione e dê escopo mínimo; separe a chave do dado (não guarde a chave ao lado do ciphertext).
Erro comum: commitar a chave no repositório "só para testar" — e ela vive para sempre no histórico do git.
No Portal: a chave de cifra vem de variável de ambiente (`PORTAL_CRYPTO_KEY`), não do código.

## SecureRandom, IV e nonce
> Aleatoriedade de segurança não é a mesma aleatoriedade de um jogo.
Teoria: use `java.security.SecureRandom` (CSPRNG) para chaves, IVs e tokens — nunca `java.util.Random`. No GCM, o IV/nonce (12 bytes) deve ser único por operação.
Atenção: reutilizar o mesmo IV com a mesma chave no GCM é catastrófico — quebra a confidencialidade.
Analogia: o IV é como um tempero único por prato; repetir o mesmo tempero com a mesma receita entrega o segredo.

## JCA/JCE na prática — AES-256-GCM
> Cifrando de forma correta em Java, com a API padrão da plataforma.
```java
byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv);
Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
byte[] ct = c.doFinal(claro.getBytes(UTF_8));   // guarde iv || ct (a tag já vem junto)
```
Na prática: persista `IV || ciphertext || tag` (por exemplo, em Base64 — aqui Base64 é só transporte do dado já cifrado).
No Portal: é a correção do Lab 4.1; na decifragem, o GCM ainda verifica a integridade (adulteração é detectada).

## TLS — o dado em trânsito
> Cifrar em repouso não basta; o dado também precisa de proteção no caminho.
Teoria: exija TLS 1.2+ (idealmente 1.3) com cipher suites fortes. Em clientes controlados (mobile), considere certificate pinning.
Erro comum (Java): um `TrustManager` permissivo que confia em qualquer certificado — desliga toda a proteção do TLS.
Regra de ouro: nunca desabilite a verificação de certificado "para funcionar" — é abrir a porta para man-in-the-middle.

## Classificando dados sensíveis no Portal
> Nem todo dado precisa do mesmo tratamento — classifique antes de decidir o controle.
- Senha: hash (bcrypt) — já feito na Aula 3.
- Dados de pagamento: cifra AES-GCM em repouso + TLS em trânsito.
- Catálogo público: nada especial.
Juntos: que outros campos do Portal você classificaria como sensíveis, e como os protegeria?

=== Gestão de Sessão

## Ciclo de vida seguro da sessão
> Como o servidor lembra de você precisa ser à prova de sequestro.
Teoria: gere o ID de sessão com entropia suficiente; regenere o ID após o login (anti session fixation); invalide no logout e por timeout (inatividade e absoluto).
Exemplo tangível: se o ID de sessão não muda após o login, um atacante que plantou um ID conhecido assume a sua sessão autenticada.
No Portal: hoje a proteção contra fixation está desligada — o Lab 4.3 regenera o ID no login.

## Cookies de sessão seguros
> Três flags simples que evitam boa parte dos roubos de sessão.
Teoria:
- HttpOnly: o JavaScript não lê o cookie (mitiga roubo via XSS).
- Secure: o cookie só trafega em HTTPS.
- SameSite (Lax/Strict): limita o envio em requisições cross-site (mitiga CSRF).
No Portal: no baseline o cookie está sem essas flags — o Lab 4.3 as habilita.
Regra de ouro: cookie de sessão sem HttpOnly e Secure é um cookie à venda.

## Stateful x Stateless (JWT) — trade-offs
> Não existe "melhor"; existe o adequado ao contexto.
Teoria: sessão stateful (JSESSIONID) guarda estado no servidor — fácil de revogar. Stateless (JWT) não guarda estado — fácil de escalar, mas difícil de revogar antes de expirar.
No Portal: a web usa sessão; a API usa JWT — e as duas terão correções hoje.
Analogia: a sessão é a pulseira da festa que o segurança pode cancelar; o JWT é o ingresso impresso que vale até a data — cancelar antes é difícil.

## JWT com segurança
> Um JWT só vale se a assinatura for verificada — sempre.
Teoria: valide a assinatura fixando o algoritmo esperado; verifique `exp`, `iss`, `aud`; use expiração curta + refresh token; guarde o token em cookie HttpOnly de preferência (localStorage é exposto a XSS).
```java
Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token); // lança se inválido
```
No Portal: hoje o token é lido sem verificar a assinatura — o Lab 4.2 corrige.

## alg:none e algorithm confusion
> Duas armadilhas clássicas de JWT que transformam "verificação" em teatro.
Teoria: `alg:none` é um token sem assinatura que parsers permissivos aceitam. Na confusão HS/RS, o atacante assina com a chave pública tratando-a como segredo HMAC.
Como explorar: forjar `{"alg":"none"}` com `role: ADMIN` e enviar sem assinatura — se aceito, é escalada instantânea.
Como se defende: exija um algoritmo específico e verifique com a chave correta; rejeite tokens sem assinatura.

## CSRF — por que ainda importa
> Mesmo com SameSite, o CSRF continua relevante para fluxos baseados em cookie.
Teoria: no CSRF, o navegador da vítima envia automaticamente os cookies numa requisição forjada por um site malicioso. O Spring Security protege com um token sincronizador.
Quando desabilitar: apenas em APIs puramente stateless que usam token em header (não em cookie) — aí o CSRF não se aplica.
No Portal: o CSRF está desabilitado globalmente sem justificativa; o Lab 4.4 reativa para os fluxos web e isenta a API.

## Timeout e sessões concorrentes
> Sessões eternas e ilimitadas ampliam a janela e a superfície de ataque.
Teoria: aplique timeout de inatividade e um tempo máximo absoluto; considere limitar sessões simultâneas por usuário, encerrando as antigas.
Exemplo tangível: `maximumSessions(1)` no Spring Security impede que a mesma conta fique aberta em vários lugares sem controle.
Regra de ouro: toda sessão deve expirar — por inatividade e por tempo total.

=== Laboratório e Fechamento

## Laboratório da Aula 4 (visão geral)
> Explorar a falha (quando aplicável) e então corrigir, com evidência do antes e depois.
- Lab 4.1 — De Base64 para AES-256-GCM (chave em variável de ambiente, IV com SecureRandom).
- Lab 4.2 — Corrigir o JWT (verificar assinatura) e demonstrar o ataque `alg:none` antes.
- Lab 4.3 — Cookie seguro (HttpOnly/Secure/SameSite) + regeneração de sessão.
- Lab 4.4 — Reativar o CSRF para a web + teste de requisição forjada bloqueada.
No Portal: o guia detalhado está no deck de laboratório (lab-aula-4) e no lab.md.

## Fechamento e ponte para a Aula 5
> Hoje protegemos o dado (cifra real, não codificação) e blindamos a sessão e o token.
- Você substituiu o Base64 por AES-GCM, passou a verificar o JWT e endureceu a sessão.
- Na Aula 5: tratar erros sem vazar informação e usar ferramentas (SAST/DAST) para caçar falhas.
Tarefa: revise AES-GCM e verificação de JWT. A Aula 5 parte de `aula-5-baseline`.
