# Portal de Pedidos B2B — Aplicação de referência do curso CASE Java

Aplicação web Java **propositalmente vulnerável**, usada como fio condutor do curso
*Certified Application Security Engineer (CASE) — Java*. Você vai **proteger esta mesma
aplicação** ao longo das 6 aulas.

> ⚠️ **Ambiente de treinamento.** Contém vulnerabilidades reais de propósito.
> Nunca exponha na internet nem use dados reais.

## Stack

- Java 17, Spring Boot 3.2, Spring MVC + Thymeleaf, Spring Security
- Spring Data JPA + JDBC, H2 (default) ou PostgreSQL (docker)
- Maven, Docker / docker-compose
- JWT (jjwt) para a API `/api/**`

## Pré-requisitos

- JDK 17 (ou superior — o projeto compila com `--release 17`)
- Maven 3.9+ (ou use o wrapper `./mvnw`)
- Docker + Docker Compose (para o Lab com PostgreSQL e para as aulas 5 e 6)
- Uma IDE (IntelliJ / VS Code / Eclipse)
- Postman ou Insomnia (para exercitar a API e os ataques)

## Como rodar

### Opção A — local com H2 (mais rápido, sem Docker)

```bash
./mvnw spring-boot:run
```

Acesse: <http://localhost:8080>

### Opção B — docker-compose (app + PostgreSQL)

```bash
docker compose up --build
```

Acesse: <http://localhost:8080>

## Contas de teste

| Papel | E-mail | Senha | Cliente |
|-------|--------|-------|---------|
| Admin | `admin@portal.com` | `admin123` | — |
| Cliente | `joao@acme.com` | `senha123` | ACME (id 1) |
| Cliente | `maria@globex.com` | `senha123` | Globex (id 2) |

## Mapa de telas

- `/` — início
- `/produtos` e `/produtos/buscar?termo=` — catálogo e busca
- `/registrar`, `/login`, `/esqueci-senha` — autenticação
- `/pedidos`, `/pedidos/{id}` — pedidos do cliente e detalhe (com upload de comprovante)
- `/importar-catalogo` — importação de catálogo por URL
- `/admin` — painel administrativo
- `/api/auth/login`, `/api/pedidos`, `/api/pedidos/{id}` — API REST (JWT)
- `/h2-console`, `/actuator` — expostos no baseline (misconfiguration)

## Fluxo do curso (branches / tags)

Cada aula parte do estado corrigido da anterior:

```
aula-1-baseline     <- app funcional, porém vulnerável (ponto de partida)
aula-2-baseline ... aula-6-baseline
aula-N-hardened     <- solução de referência de cada aula (uso do instrutor)
```

Para começar um laboratório, faça checkout da branch/tag indicada no guia da aula.

## Aviso

As vulnerabilidades e seu mapeamento OWASP/CWE estão em
[`VULNERABILITIES.md`](VULNERABILITIES.md) (material do instrutor).
