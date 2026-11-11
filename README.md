# assessment-service

Serviço do Correcta responsável pelas atividades (listas, aulas e provas) de
cada turma: questões, casos de teste, parâmetros, sessões dos estudantes e
resultado final. A correção das respostas fica fora deste serviço: o
submission-service e o evaluation-orchestrator cuidam dela.

Stack: Kotlin 1.9, Spring Boot 3.4, Java 17, PostgreSQL (Flyway), RabbitMQ e
OpenFeign.

## Estrutura

| Pacote | Conteúdo |
| --- | --- |
| `controllers` | Interfaces com as rotas e anotações de segurança; `controllers/impl` só delega aos serviços. |
| `service` | Regras de negócio, controle de acesso por turma e varredura de sessões. |
| `entities` / `repositories` | Entidades JPA e repositórios Spring Data. |
| `models` | Corpos de requisição (com validação) e de resposta. |
| `messaging` | Outbox transacional, relay para o RabbitMQ e mensagens de sessão. |
| `security` | Autenticação por cabeçalhos do gateway e o aspecto que aplica as anotações. |
| `clients` | Cliente Feign do cohort-service. |
| `handlers` / `exceptions` | Exceções de negócio e sua conversão em respostas HTTP. |
| `utils` | Paginação e geração de slugs. |

## Entidades

| Entidade | Tabela | Descrição |
| --- | --- | --- |
| `Assessment` | `assessments` | Atividade de uma turma. Estado `DRAFT`, `PUBLISHED` ou `ARCHIVED`; slug único na turma. |
| `AssessmentParameters` | `assessment_parameters` | Regras da atividade: entrega após o prazo, tempo máximo, erros de compilação no editor e visibilidade da nota dos casos de teste. |
| `AssessmentParameterChangeLog` | `assessment_parameters_change_log` | Histórico de cada parâmetro alterado (valor antigo e novo). |
| `AssessmentQuestion` | `assessment_questions` | Questão ordenada por `position`, com slug único na atividade. Código inicial e harness por linguagem em `assessment_question_code_drafts`. |
| `AssessmentQuestionTestcase` | `assessment_question_testcases` | Entrada, saída esperada, limites de tempo e memória e tipo (`PUBLIC`, `PRIVATE`, `EVALUATION`). |
| `AssessmentSession` | `assessment_sessions` | Tentativa de um estudante (no máximo uma por atividade): `IN_PROGRESS`, `SUBMITTED` ou `EXPIRED`. |
| `AssessmentSessionEvent` | `assessment_session_events` | Auditoria da sessão: início, retomada, entrega, expiração, ações do docente. |
| `AssessmentResult` | `assessment_results` | Nota final e comentário geral do docente para um estudante. Hoje só é lida (nota no detalhe da atividade); não há endpoint de escrita. |
| `OutboxMessage` | `outbox_messages` | Mensagem aguardando publicação no RabbitMQ. |

IDs de perfil e de turma são referências externas: o serviço não consulta os
bancos do identity-service nem do cohort-service.

Hoje só a linguagem `C` é aceita nas questões.

## Endpoints

Base: `/api/v1`. Todos os endpoints públicos exigem `@IsLoggedUser`. "Docente"
significa membro `FACULTY` da turma da atividade; ações de gestão também exigem
turma `ACTIVE`.

### Atividades (`/assessments`)

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/assessments?cohortId=` | Lista as atividades da turma (obrigatória). Estudantes só veem publicadas. Filtros: `title`, `state`; paginação `page`, `size`, `sortBy` (`createdAt`, `title`, `dueDate`), `sortDirection`. |
| `GET` | `/assessments/{assessmentId}` | Detalha a atividade, com totais de questões, nota e sessão do estudante. |
| `GET` | `/assessments/by-slug/{cohortId}/{slug}` | Busca a atividade pelo slug na turma. |
| `POST` | `/assessments` | Cria um rascunho (docente). |
| `POST` | `/assessments/{assessmentId}/edit` | Altera título, slug, descrição e prazo (docente). |
| `PATCH` | `/assessments/{assessmentId}/parameters` | Altera parâmetros e registra o histórico (docente). |
| `POST` | `/assessments/{assessmentId}/publish` | Publica; exige ao menos uma questão (docente). |
| `POST` | `/assessments/{assessmentId}/archive` | Arquiva e expira as sessões em andamento (docente). |

### Questões (`/assessments/{assessmentId}/questions`)

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/` | Lista as questões. Estudantes só veem após iniciar a sessão; o harness só aparece para docentes. |
| `GET` | `/{questionId}` | Detalha uma questão. |
| `POST` | `/` | Cria questão (docente). Sem `position`, vai para o fim; sem `slug`, gera a partir do título. |
| `POST` | `/{questionId}/edit` | Substitui os campos (docente). A nota máxima não muda depois que algum estudante inicia. |
| `DELETE` | `/{questionId}` | Exclui (docente), só antes de qualquer estudante iniciar. Atividade publicada mantém ao menos uma questão. |

### Casos de teste (`.../questions/{questionId}/testcases`)

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/` | Lista. Estudantes não veem `EVALUATION`; em `PRIVATE`, entrada e saída ficam ocultas. |
| `POST` | `/` | Cria (docente). |
| `POST` | `/{testcaseId}/edit` | Substitui os campos (docente). |
| `POST` | `/bulk-edit` | Altera limites e tipo de vários casos (docente). |
| `DELETE` | `/{testcaseId}` | Exclui (docente). |
| `POST` | `/bulk-delete` | Exclui vários casos (docente). |
| `POST` | `/import` | Importa de dois arquivos multipart (`inputs`, `outputs`), um caso por linha (docente). |
| `POST` | `/reorder` | Reordena todos os casos a partir de 1 (docente). |

Depois que um estudante inicia a atividade, só casos `PUBLIC` podem ser excluídos.

### Sessões

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/assessments/{id}/sessions/me` | Sessão do estudante autenticado. |
| `POST` | `/assessments/{id}/sessions/me/start` | Inicia (`201`) ou retoma (`200`) a sessão. Docentes não iniciam. |
| `POST` | `/assessments/{id}/sessions/me/heartbeat` | Soma o tempo com a página aberta (intervalos acima de 60 s não contam). |
| `GET` | `/assessments/{id}/sessions` | Lista as sessões da atividade (docente). |
| `GET` | `/assessments/{id}/sessions/summary` | Contagem por estado (docente). |
| `GET` | `/assessments/{id}/sessions/{sessionId}` | Detalha uma sessão (docente). |
| `GET` | `/assessments/{id}/sessions/{sessionId}/events` | Eventos da sessão (docente). |
| `POST` | `/assessments/{id}/sessions/{sessionId}/extend` | Acrescenta minutos a uma sessão em andamento com limite de tempo (docente). |
| `POST` | `/assessments/{id}/sessions/{sessionId}/reopen` | Reabre uma sessão finalizada de atividade publicada (docente). |
| `POST` | `/assessments/{id}/sessions/{sessionId}/close` | Encerra uma sessão em andamento como entregue (docente). |
| `GET` | `/assessment-sessions/me` | Sessões do estudante em todas as turmas. |

A sessão expira no menor valor entre o tempo máximo e, sem entrega após o
prazo, a data de entrega. O estudante não encerra a sessão pela API: a entrega
chega pelo workspace-service (ver "Mensageria").

### Internos (sem autenticação)

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/internal/assessments/{id}/questions/{questionId}?language=C&types=` | Harness da linguagem e casos de teste (todos ou dos tipos pedidos). |
| `GET` | `/internal/assessments/{id}/sessions/{studentProfileId}` | Diz ao submission-service se a sessão ainda aceita respostas (em andamento, ou até 5 s após expirar por tempo). |

As rotas `/internal` não devem ser expostas pelo gateway.

## Autenticação e autorização

O serviço não valida tokens. O gateway (`local/proxy` em desenvolvimento,
forward auth no cluster) chama `GET /account/me` do identity-service e repassa
o resultado em cabeçalhos:

| Cabeçalho | Origem em `/account/me` |
| --- | --- |
| `X-User-Id` | `profile.profileId` |
| `X-User-Person-Id` | `person.personId` |
| `X-User-Profile-Type` | `profile.type` |
| `X-User-Login` | `account.login` |
| `X-User-Email` | `account.email` |

`HeaderAuthenticationFilter` transforma os cabeçalhos em `ProfilePrincipal`.
`SecurityAspect` aplica as anotações das interfaces dos controllers:

- `@IsLoggedUser`: exige `X-User-Id`, senão `401`.
- `@IsProfileType("STAFF")`: exige o tipo de perfil, senão `403`.

As permissões por turma ficam em `AssessmentAccessService`, que consulta o
cohort-service.

**Atenção:** os cabeçalhos são aceitos como chegam. O serviço só pode ser
acessível pelo gateway, e o gateway precisa descartar cabeçalhos `X-User-*`
enviados pelo cliente.

## Integração com o cohort-service

`CohortClient` (OpenFeign) chama
`GET /api/v1/internal/cohorts/{cohortId}/members/{profileId}` direto no
cluster, sem gateway. `404` significa "não é membro" (`403` para o chamador).
Qualquer outra falha responde `503`: o acesso falha fechado.

## Mensageria

Exchange `correcta.assessment-sessions`, com dead-letter em
`correcta.assessment-sessions.dlx` e fila `<fila>.dlq` para cada fila.

| Routing key | Direção | Significado |
| --- | --- | --- |
| `session.ended` | assessment-service para workspace-service | A sessão terminou sem entrega do estudante (prazo, arquivamento ou docente): entregar os rascunhos. |
| `session.delivered` | workspace-service para assessment-service | O estudante entregou: marcar a sessão como `SUBMITTED`. |

As mensagens enviadas passam pela outbox: `Outbox` grava a linha na mesma
transação da mudança, e `OutboxRelay` publica com confirmação do broker, ao
menos uma vez, com backoff exponencial de até 5 minutos. Os consumidores
precisam ser idempotentes.

`AssessmentSessionSweeper` expira periodicamente as sessões cujo tempo acabou,
para que `session.ended` saia mesmo sem requisições.

## Configuração

| Variável | Padrão |
| --- | --- |
| `ENV_DATASOURCE_POSTGRES_URL` | obrigatória |
| `ENV_DATASOURCE_POSTGRES_USERNAME` | obrigatória |
| `ENV_DATASOURCE_POSTGRES_PASSWORD` | obrigatória |
| `ENV_JPA_DDL_AUTO` | `validate` |
| `ENV_SPRINGDOC_ENABLED` | `false` |
| `ENV_COHORT_SERVICE_URL` | obrigatória |
| `ENV_COHORT_SERVICE_CONNECT_TIMEOUT_MS` | `2000` |
| `ENV_COHORT_SERVICE_READ_TIMEOUT_MS` | `5000` |
| `ENV_RABBITMQ_URL` | obrigatória |
| `ENV_RABBITMQ_LISTENER_MAX_ATTEMPTS` | `5` |
| `ENV_OUTBOX_RELAY_INTERVAL_MS` | `1000` |

O perfil `local` (`SPRING_PROFILES_ACTIVE=local`) traz padrões para PostgreSQL,
RabbitMQ e cohort-service locais, liga o Swagger em `/swagger-ui.html` e mostra
o SQL. O intervalo do sweeper é `sessions.sweep-interval-ms` (padrão 60000).

## Migrações

O Flyway é dono do schema: os scripts de `src/main/resources/db/migration`
rodam na inicialização e o Hibernate só valida as entidades
(`ddl-auto=validate`). Mudar colunas exige um novo `V<n>__<descricao>.sql`;
nunca edite um script que já rodou.

| Script | Mudança |
| --- | --- |
| `V1` | Schema inicial, com `IF NOT EXISTS`. |
| `V2` | Remove colunas deixadas pelo `ddl-auto=update`. |
| `V3` | Casos de teste das questões. |
| `V4` | Restringe os códigos iniciais a C. |
| `V5` | Slugs de atividades e questões. |
| `V6` | Controle de sessões. |
| `V7` | Parâmetro de erros de compilação. |
| `V8` | Remove o monitoramento (sessão única e abandono). |
| `V9` | Tabela da outbox. |
| `V10` | Harness por linguagem. |

Bancos criados antes do Flyway recebem baseline na versão 0
(`spring.flyway.baseline-on-migrate`).

## Execução

Na raiz do workspace: `./local/run assessment` (porta 8086). Pelo proxy:
`http://127.0.0.1:8080/api/assessment/api/v1/...`.

Imagem Docker: `Dockerfile` (build com Gradle, execução em JRE 17). O workflow
`release` publica a imagem a cada push na `main` e atualiza a tag de produção
no repositório de infraestrutura.

## Testes e cobertura

```sh
./gradlew test
```

Os testes não precisam de serviços externos: usam H2 com o schema gerado pelas
entidades (Flyway desligado), cohort-service e relógio simulados, e o listener
do RabbitMQ parado.

- Testes de integração (`*Tests`): sobem o contexto Spring e exercitam a API
  com MockMvc.
- Testes unitários (`*Test`): serviços, controllers, segurança, mensageria,
  modelos e utilitários com Mockito.

O JaCoCo gera o relatório ao fim de `test`, em
`build/reports/jacoco/test/html/index.html`. Cobertura atual: 100% de linhas,
branches, instruções e métodos.

## Formatação

Spotless com ktlint (`./gradlew spotlessApply`). O hook de pre-commit em
`.githooks` é instalado pela build e formata os arquivos Kotlin no commit.
