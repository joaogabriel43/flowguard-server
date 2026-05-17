# FlowGuard Server — CLAUDE.md

**Nome:** FlowGuard Server
**Versão:** 1.0.0
**Data:** 2025-05-17

---

## Visão Geral

Plataforma self-hosted de feature flags com rollout progressivo. Alternativa ao LaunchDarkly para times que utilizam Spring Boot, com foco em simplicidade operacional, multitenancy e deploy em infraestrutura própria.

**Stack:**

- Java 21
- Spring Boot 3.2
- PostgreSQL (persistência principal)
- Redis (cache de flags + Pub/Sub para notificação entre instâncias)
- SSE — Server-Sent Events (push de atualizações para os SDKs)
- Flyway (migrações de banco de dados)
- JWT (autenticação stateless)

---

## Estrutura de Pastas Esperada

```
src/main/java/com/flowguard/
├── domain/          # Entidades, value objects, interfaces de repositório
├── application/     # Use cases, services, DTOs
├── infrastructure/  # JPA, Redis, SSE, Flyway, configs
└── presentation/    # Controllers, exception handlers, filters
```

---

## ADRs Já Fechados

### ADR-001: Dois Repositórios Separados (server + SDK)
Ciclos de vida independentes. O servidor e o SDK evoluem em ritmos diferentes, com versionamentos e pipelines de CI/CD próprios. Times consumidores podem adotar novas versões do SDK sem aguardar releases do servidor, e vice-versa.

### ADR-002: Avaliação de Flags Híbrida
O SDK avalia flags localmente, a partir de um cache em memória mantido atualizado. O servidor é responsável por fazer push das atualizações via SSE sempre que uma flag for alterada. Isso garante zero latência por request no caminho crítico da aplicação consumidora.

### ADR-003: SSE no Lugar de WebSocket
Push unidirecional (servidor → SDK) é suficiente para o caso de uso. SSE é mais simples de implementar, mais leve e possui melhor compatibilidade com proxies, load balancers e firewalls corporativos do que WebSocket.

### ADR-004: Redis como Cache + Pub/Sub
O Redis serve duas funções: cache das flags (leitura rápida sem acesso ao PostgreSQL por request) e canal de Pub/Sub para notificar todas as instâncias do servidor quando uma flag é alterada. Isso garante consistência em ambientes com múltiplas réplicas.

### ADR-005: MurmurHash3 para Rollout Consistente
O mesmo algoritmo de hashing é implementado no servidor e no SDK. Isso garante que `hash(flagKey + userId) % 100` produza o mesmo resultado em ambos os lados, assegurando que o mesmo `userId` sempre caia no mesmo bucket percentual, independentemente de qual componente realiza o cálculo.

### ADR-006: Multitenancy por tenant_id
Isolamento de dados por campo `tenant_id` presente em todas as tabelas. Não utiliza Row-Level Security (RLS) do PostgreSQL — o isolamento é garantido pela camada de aplicação, com `tenant_id` obrigatório em todas as queries. Mais simples de portar entre bancos e de auditar.

### ADR-007: Deploy no Railway
Plataforma escolhida para hospedagem. O Railway oferece PostgreSQL e Redis como serviços nativos no mesmo projeto, simplificando o setup de infraestrutura e reduzindo a latência de rede entre os componentes.

---

## Padrões do Projeto

- **Clean Architecture obrigatória:** o fluxo de dependências segue estritamente `domain → application → infrastructure → presentation`. Camadas internas nunca dependem de camadas externas.
- **DTOs na fronteira:** entidades e objetos de domínio nunca saem da camada de aplicação. Controllers recebem e retornam DTOs; a conversão ocorre na camada de aplicação.
- **Injeção de dependência via construtor:** nenhum campo é injetado com `@Autowired` diretamente. Toda dependência é declarada no construtor, facilitando testes unitários e tornando as dependências explícitas.
- **Código-fonte em inglês:** variáveis, métodos, classes, pacotes e comentários de código devem ser escritos estritamente em inglês. Documentação externa (como este arquivo), mensagens de commit e explicações arquiteturais permanecem em português.
- **Commits semânticos:** `feat:`, `fix:`, `test:`, `docs:`, `refactor:`.
- **TDD:** testes são escritos antes da implementação. Nenhuma feature é considerada pronta sem cobertura de testes correspondente.

**Proteções obrigatórias de setup (a habilitar desde o início do projeto):**

- **p6spy** — detecção de queries N+1 em tempo de desenvolvimento. Toda query inesperada deve ser investigada antes de mergear.
- **Spring Boot Actuator** — endpoints de observabilidade (`/actuator/health`, `/actuator/metrics`) habilitados desde o primeiro commit de infra.
- **jqwik** — testes baseados em propriedades para lógica crítica de negócio (rollout, hashing, avaliação de segmentos).

---

## Regras de Negócio

- Toda flag pertence a um tenant — `tenant_id` é obrigatório em todas as queries. Nunca buscar ou alterar flags sem filtrar pelo tenant do contexto autenticado.
- Rollout progressivo obedece à fórmula: `hash(flagKey + userId) % 100 < percentual`. Um usuário incluído no rollout permanece incluído enquanto o percentual não diminuir abaixo do seu bucket.
- Flag desativada globalmente (`enabled = false`) sempre retorna `false`, independentemente de qualquer regra de segmentação, percentual de rollout ou atributo do usuário.
- Audit log é obrigatório em toda ativação e desativação de flag. O log deve registrar: `tenant_id`, `flag_id`, `ação`, `ator` (usuário ou sistema), `timestamp` e o estado anterior da flag.

---

## Erros Conhecidos e Como Evitá-los

> _Seção a ser preenchida progressivamente conforme o projeto avança._

---

## Otimizações e Performance

> _Seção a ser preenchida progressivamente conforme o projeto avança._

---

## Agentes: Casos de Uso Confirmados

> _Seção a ser preenchida progressivamente conforme o projeto avança._

---

## Changelog do CLAUDE.md

> _Seção a ser preenchida progressivamente conforme o projeto avança._

| Versão | Data       | Descrição                                              | Autor                          |
|--------|------------|--------------------------------------------------------|--------------------------------|
| 1.0.0  | 2025-05-17 | Criação inicial com visão, ADRs e padrões do servidor  | @engineering-backend-architect |
