# FlowGuard Server — CLAUDE.md

**Nome:** FlowGuard Server
**Versão:** 1.1.0
**Data:** 2026-05-18

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
├── domain/                  # Entidades, value objects, interfaces de repositório
├── application/
│   ├── dto/                 # Commands, requests e responses
│   ├── service/             # Use cases
│   └── validation/          # Custom Bean Validation constraints (ex: ValidAttributeMap)
├── infrastructure/          # JPA, Redis, SSE, Flyway, security configs
└── presentation/            # Controllers, exception handlers, filters
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

### ADR-008: audit_logs.flag_id com ON DELETE SET NULL
O FK de `audit_logs.flag_id` para `feature_flags` usa `ON DELETE SET NULL` (não CASCADE). Isso garante que o histórico de auditoria sobreviva à deleção de uma flag. Quando uma flag é deletada, seu `flag_id` nos registros de audit fica NULL — mas o `tenant_id`, `action`, `performed_by` e `previous_state` são preservados.

---

## Padrões do Projeto

- **Clean Architecture obrigatória:** o fluxo de dependências segue estritamente `domain → application → infrastructure → presentation`. Camadas internas nunca dependem de camadas externas.
- **DTOs na fronteira:** entidades e objetos de domínio nunca saem da camada de aplicação. Controllers recebem e retornam DTOs; a conversão ocorre na camada de aplicação.
- **Injeção de dependência via construtor:** nenhum campo é injetado com `@Autowired` diretamente. Toda dependência é declarada no construtor, facilitando testes unitários e tornando as dependências explícitas.
- **Código-fonte em inglês:** variáveis, métodos, classes, pacotes e comentários de código devem ser escritos estritamente em inglês. Documentação externa (como este arquivo), mensagens de commit e explicações arquiteturais permanecem em português.
- **Commits semânticos:** `feat:`, `fix:`, `test:`, `docs:`, `refactor:`.
- **TDD:** testes são escritos antes da implementação. Nenhuma feature é considerada pronta sem cobertura de testes correspondente.
- **Secrets apenas via variáveis de ambiente:** nenhuma credencial com fallback hardcoded — nem em `application.yml`, nem em `application-prod.yml`. Se a variável não estiver definida, o startup falha explicitamente. Variáveis obrigatórias: `JWT_SECRET`, `DATABASE_USER`, `DATABASE_PASSWORD`.
- **Audit log em toda mutação de flag:** criar, atualizar, deletar, ativar/desativar, adicionar/remover regra — todas essas operações devem persistir um `AuditLog` com `performed_by` (via `SecurityContextHolder`), `previous_state` e `new_state`.
- **Custom constraints em `application/validation/`:** validações complexas que não cabem em anotações padrão (ex: tamanho de chaves/valores de Map) ficam neste pacote como `@Constraint` + `ConstraintValidator`.

**Proteções obrigatórias de setup (a habilitar desde o início do projeto):**

- **p6spy** — detecção de queries N+1 em tempo de desenvolvimento. Toda query inesperada deve ser investigada antes de mergear.
- **Spring Boot Actuator** — endpoints `/actuator/health` e `/actuator/info` são públicos; todos os outros exigem `ROLE_ADMIN`. `show-details: when-authorized` em todos os perfis.
- **jqwik** — testes baseados em propriedades para lógica crítica de negócio (rollout, hashing, avaliação de segmentos).

---

## Regras de Negócio

- Toda flag pertence a um tenant — `tenant_id` é obrigatório em todas as queries. Nunca buscar ou alterar flags sem filtrar pelo tenant do contexto autenticado.
- Rollout progressivo obedece à fórmula: `hash(flagKey + userId) % 100 < percentual`. Um usuário incluído no rollout permanece incluído enquanto o percentual não diminuir abaixo do seu bucket.
- Flag desativada globalmente (`enabled = false`) sempre retorna `false`, independentemente de qualquer regra de segmentação, percentual de rollout ou atributo do usuário.
- Audit log é obrigatório em **toda** mutação de flag: criação, atualização, deleção, ativação, desativação, adição e remoção de regra. O log registra: `tenant_id`, `flag_id` (nullable pós-deleção), `action`, `performed_by`, `performed_at`, `previous_state` e `new_state`.

---

## Erros Conhecidos e Como Evitá-los

### [2026-05-18] Credentials hardcoded com fallback em application.yml
**O que aconteceu:** `JWT_SECRET`, `DATABASE_USER` e `DATABASE_PASSWORD` tinham valores hardcoded como fallback (`${JWT_SECRET:valor_secreto}`). Com o repositório público, qualquer pessoa podia forjar tokens JWT ou acessar o banco.
**Por que:** Conveniência de desenvolvimento — o dev não precisa setar variáveis locais.
**Como prevenir:** Nunca usar fallback para credenciais. Usar fallback apenas para configurações não-sensíveis (hosts, portas, nomes de banco). Para dev local, setar as variáveis via `docker-compose.yml` ou arquivo `.env` ignorado pelo git.

### [2026-05-18] CORS wildcard + allowCredentials — bypass de proteção CSRF
**O que aconteceu:** `setAllowedOriginPatterns(["*"])` com `allowCredentials(true)` permite que qualquer site faça requisições autenticadas cross-origin.
**Por que:** Configuração padrão copiada sem analisar a interação entre os dois parâmetros.
**Como prevenir:** Nunca combinar wildcard com `allowCredentials(true)`. A regra: se origins contém `*`, desativar credentials. Checar sempre `SecurityConfig.corsConfigurationSource()` ao adicionar novos ambientes.

### [2026-05-18] ex.printStackTrace() bypassa o logger em produção
**O que aconteceu:** `ex.printStackTrace()` escreve em `System.err` diretamente, sem passar pelo SLF4J/Logback. Em ambientes com logging estruturado (JSON), o erro some ou aparece desformatado fora do agregador.
**Por que:** Hábito de debug — funciona localmente mas quebra em prod.
**Como prevenir:** Sempre usar `logger.error("mensagem", ex)`. Nunca usar `printStackTrace()` em código de produção. Revisão de código deve rejeitar qualquer ocorrência.

### [2026-05-18] Audit log de DELETE deletado por cascade
**O que aconteceu:** O FK `audit_logs.flag_id → feature_flags(id)` era `ON DELETE CASCADE`. Ao deletar uma flag, todos os seus audit logs eram deletados junto — incluindo o próprio registro de deleção que estava sendo criado.
**Por que:** CASCADE é o default mais comum em FKs, mas é destrutivo para tabelas de auditoria.
**Como prevenir:** Tabelas de auditoria/log **sempre** devem usar `ON DELETE SET NULL` ou `ON DELETE NO ACTION` — nunca CASCADE. Ao criar qualquer FK para uma tabela de audit, questionar explicitamente o comportamento de deleção.

### [2026-05-18] flag_rules sem tenant_id — isolamento indireto
**O que aconteceu:** `flag_rules` não tinha `tenant_id` próprio. O isolamento dependia de um join com `feature_flags`. Uma query direta em `flag_rules` sem o join poderia retornar dados de outros tenants.
**Por que:** A relação `flag → rules` parecia suficiente para garantir isolamento.
**Como prevenir:** Toda tabela que contenha dados de tenant deve ter `tenant_id` próprio, mesmo que seja redundante com um FK indireto. Isso protege contra queries acidentais e facilita auditorias.

---

## Otimizações e Performance

### [2026-05-18] Índices em tenant_id — obrigatórios em todas as tabelas multitenancy
Todas as tabelas com `tenant_id` precisam de índice nessa coluna. Sem índice, toda query com `WHERE tenant_id = ?` faz seq scan — invisível em desenvolvimento com poucos dados, catastrófico em produção.

Índices criados em V6:
- `idx_users_tenant_id`
- `idx_feature_flags_tenant_id`
- `idx_audit_logs_tenant_id`
- `idx_audit_logs_flag_id`
- `idx_flag_rules_tenant_id`

**Regra:** ao criar qualquer nova tabela com `tenant_id`, adicionar o índice na própria migration de criação.

### [2026-05-18] SSE heartbeat para sobreviver a proxies
Conexões SSE sem heartbeat são fechadas silenciosamente por proxies (Railway, Nginx, load balancers) após 60–120 s de inatividade. O cliente perde atualizações sem receber erro.

**Solução:** `@Scheduled(fixedRate = 30_000)` em `SseController` chama `SseEmitterRegistry.broadcastHeartbeat()` a cada 30 s. O evento `heartbeat` com data vazia mantém o TCP ativo.

---

## Agentes: Casos de Uso Confirmados

### Revisão de segurança pré-portfólio
**Agentes usados:** `@engineering-security-engineer`, `@engineering-backend-architect`, `@engineering-code-reviewer`
**Resultado:** Identificados 3 CRÍTICOs, 4 ALTOs, 7 MÉDIOs, 3 BAIXOs. Todos corrigidos em 3 commits semânticos (`fix(security)`, `fix(database)`, `fix(audit)`).
**Quando usar:** Antes de tornar qualquer repositório público ou de publicar em portfólio.

---

## Changelog do CLAUDE.md

| Versão | Data       | Descrição                                                                                 | Autor                          |
|--------|------------|-------------------------------------------------------------------------------------------|--------------------------------|
| 1.0.0  | 2025-05-17 | Criação inicial com visão, ADRs e padrões do servidor                                     | @engineering-backend-architect |
| 1.1.0  | 2026-05-18 | Revisão de segurança completa: erros conhecidos, otimizações, ADR-008, padrões atualizados | @engineering-security-engineer |
