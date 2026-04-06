# SAIKOO — Documentação Técnica

**Projeto:** PB_TP5 — Teste de Performance 5
**Disciplina:** Engenharia Disciplinada de Software
**Aluno:** André Luis Becker
**Tecnologia:** Java 21 · Spring Boot 3.3 · Maven · GitHub Actions

---

## Sumário

- [1. Visão Geral do Projeto](#1-visão-geral-do-projeto)
  - [1.1 Evolução do TP4 para o TP5](#11-evolução-do-tp4-para-o-tp5)
  - [1.2 Refatorações aplicadas no TP5](#12-refatorações-aplicadas-no-tp5)
- [2. Arquitetura](#2-arquitetura)
- [3. Suíte de Testes](#3-suíte-de-testes)
- [4. Pipeline CI/CD com GitHub Actions](#4-pipeline-cicd-com-github-actions)
- [5. Boas Práticas Aplicadas na Pipeline](#5-boas-práticas-aplicadas-na-pipeline)
- [6. Deploy Multi-Ambiente](#6-deploy-multi-ambiente)
- [7. Monitoramento e Depuração dos Workflows](#7-monitoramento-e-depuração-dos-workflows)
- [8. Justificativas Técnicas](#8-justificativas-técnicas)
- [9. Como Executar](#9-como-executar)
- [10. Evidências de Execução](#10-evidências-de-execução)
- [11. Referências](#11-referências)

---

## 1. Visão Geral do Projeto

O SAIKOO é um sistema de gestão financeira pessoal construído com Java 21 e Spring Boot 3.3. Cobre o ciclo completo de um portfólio de investimentos — Ações, FIIs, Criptomoedas, Renda Fixa, Precatórios e Ativos Reais — com CRUD completo, dashboard com gráficos e interface dark theme.

O TP5 é a **entrega final** do projeto, incorporando:

- Refatorações orientadas à imutabilidade, polimorfismo e eliminação de código morto
- Pipeline CI/CD expandida com análise de segurança (SAST/DAST), deploy multi-ambiente e aprovação manual para produção
- Testes pós-deploy com Selenium validando a integridade do sistema em staging
- Monitoramento com logs personalizados, resumos Markdown nos jobs e badges de status

### Funcionalidades

- Dashboard com patrimônio total, gráficos de evolução, alocação por tipo (donut), comparativo Top 5 (Cripto e Ações) e ranking Top 10
- CRUD completo de ativos com validação via Bean Validation e notificações Toast
- Cálculo automático de preço médio (`valorInvestido / quantidade`) encapsulado no modelo
- Tratamento centralizado de exceções via `@ControllerAdvice` sem exposição de stacktraces
- Log rotativo por sessão em `logs/saikoo-<data-hora>.log` via `logback-spring.xml`

---

### 1.1 Evolução do TP4 para o TP5

| Dimensão | TP4 | TP5 |
| -------- | ---- | ---- |
| Cobertura mínima | 85% | 90% |
| Workflows | 2 (`ci.yml`, `cd.yml`) | 3 (+ `post-deploy.yml`) |
| Ambientes de deploy | — | dev, staging, prod |
| Aprovação manual | — | obrigatória para prod |
| Análise de segurança | — | SAST com CodeQL |
| Testes pós-deploy | — | `AtivoSeleniumPosDeployTest` em staging |
| Resumo de resultados | logs do runner | Markdown em `$GITHUB_STEP_SUMMARY` |
| Refatorações | Estruturais (TP4) | Imutabilidade + polimorfismo (TP5) |

---

### 1.2 Refatorações aplicadas no TP5

O TP5 aplica refatorações orientadas à imutabilidade, polimorfismo e redução de complexidade, guiadas pela suíte de testes existente.

| Ponto refatorado | Antes (TP4) | Depois (TP5) | Princípio aplicado |
| ---------------- | ----------- | ------------ | ------------------ |
| Objetos de valor imutáveis | `DashboardMetrics` como `record` imutável introduzido no TP4 | Extensão do padrão: demais objetos de transferência de dados passam a ser `record` ou ter campos `final` onde aplicável | Imutabilidade · CQS |
| Condicionais de tipo | Verificações `instanceof` ou switch por tipo de ativo para comportamentos específicos | Polimorfismo: comportamentos específicos por tipo delegados a subclasses ou interfaces, eliminando condicionais de tipo no service | OCP · Polimorfismo |
| Condicionais aninhadas | Blocos `if-else` encadeados no service para validações | Cláusulas de guarda (early return) — fluxo principal sem indentação excessiva | Clean Code (Martin) |
| Código morto | Métodos e campos não referenciados remanescentes do TP3 | Remoção completa — nenhum campo ou método sem uso no classpath | YAGNI · Clean Code |
| Separação consulta/modificador | Métodos que retornavam valor e modificavam estado simultaneamente | Separação explícita seguindo CQS: consultas não têm efeitos colaterais; modificadores não retornam valor | CQS (Meyer) |

Cada refatoração foi validada pela suíte de testes, garantindo que nenhum comportamento externo foi alterado.

---

## 2. Arquitetura

Layered Architecture com separação estrita de responsabilidades:

```
controller  →  service  →  repository  →  model
     ↓                                      ↑
exception (GerenciadorExcecoesGlobal via @ControllerAdvice)
```

```
src/main/java/com/infnet/financas/
├── controller/     # Roteamento HTTP — zero lógica de negócio
├── service/        # Regras de negócio + DashboardMetrics record (Java 21)
├── repository/     # Spring Data JPA
├── model/          # Entidade JPA + enums + getPrecoMedio()
└── exception/      # Exceções de domínio + handler global
```

O frontend adota arquitetura de componentes com Thymeleaf Fragments. `layout.html` tem ~45 linhas; `dashboard.html` tem ~40 linhas. Todo CSS e JavaScript está em `static/`, sem nada inline nos templates.

---

## 3. Suíte de Testes

A suíte cobre a pirâmide completa de testes com 68+ casos e cobertura JaCoCo ≥ 90%.

| Classe de Teste | Tipo | Casos |
| --------------- | ---- | ----- |
| `AtivoFinanceiroServiceTest` | Unitário (Mockito) | 12 |
| `AtivoControllerTest` | Unitário (MockMvc) | 17 |
| `AtivoFinanceiroRepositoryTest` | Integração (@DataJpaTest) | 7 |
| `AtivoFinanceiroTest` | Unitário (modelo) | 7 |
| `GerenciadorExcecoesGlobalTest` | Unitário | 4 |
| `HomeControllerTest` | Unitário (MockMvc) | 1 |
| `CenariosAdversosTest` | Cenários adversos | 8 |
| `AtivoFinanceiroPropriedadeTest` | Property-based (Jqwik) | 4 × 1.000 |
| `AtivoSeleniumTest` | E2E (Selenium) | 6 |
| `AtivoSeleniumPosDeployTest` | E2E pós-deploy (Selenium) | 4 |
| `SaikooApplicationTest` | Contexto Spring | 2 |

O gate JaCoCo está configurado no `pom.xml` com mínimo de 90% de linhas e falha o build em `mvn verify` se não atingido. Exclusões: `SaikooApplication`, `TipoAtivo`, `CategoriaAtivo`, `AtivoFinanceiroBuilder`.

---

## 4. Pipeline CI/CD com GitHub Actions

O TP5 opera com três workflows em `.github/workflows/` que cobrem validação, entrega e verificação pós-deploy.

### O que é CI/CD

**Integração Contínua (CI)** valida toda alteração de código automaticamente antes de chegar à branch principal, detectando regressões o mais cedo possível.

**Entrega Contínua (CD)** empacota e promove o artefato pelos ambientes (dev → staging → prod) de forma automatizada, com gate de aprovação manual para produção.

**Pós-deploy** executa testes de fumaça com Selenium contra o ambiente recém-promovido, confirmando que o sistema está operacional antes de qualquer anúncio de release.

---

### 4.1. Workflow CI — `ci.yml`

**Arquivo:** `.github/workflows/ci.yml`

**Quando dispara:**
- Em todo `push` para qualquer branch
- Em todo `pull_request` direcionado à branch `main`

**Diagrama de execução:**

```
push / pull_request
        │
        ▼
┌──────────────────────────────────────────────────────┐
│  Job 1: testes-unitarios-integracao                  │
│  Timeout: 15 minutos                                 │
│                                                      │
│  1. Checkout do repositório                          │
│  2. Setup Java 21 (Eclipse Temurin) + cache Maven    │
│  3. mvn -B clean verify                              │
│     -Dtest="!AtivoSeleniumTest"                      │
│     ├── compila o projeto                            │
│     ├── executa 50+ testes                           │
│     └── jacoco:check — falha se < 90%                │
│  4. Análise SAST com CodeQL                          │
│  5. Upload: surefire-reports (14 dias)               │
│  6. Upload: jacoco-coverage-report (14 dias)         │
│  7. Resumo Markdown ($GITHUB_STEP_SUMMARY)           │
└──────────────────────┬───────────────────────────────┘
                       │ needs — só avança se job 1 passou
                       ▼
┌──────────────────────────────────────────────────────┐
│  Job 2: testes-e2e                                   │
│  Timeout: 10 minutos                                 │
│                                                      │
│  1. Checkout do repositório                          │
│  2. Setup Java 21 + cache Maven                      │
│  3. mvn -B test -Dtest=AtivoSeleniumTest             │
│     └── Chrome headless (config. no teste)           │
│  4. Upload: selenium-screenshots (14 dias)           │
│     (if: always — inclui falhas)                     │
└──────────────────────────────────────────────────────┘
```

**Por que dois jobs separados?**
Separar testes unitários/integração dos E2E permite feedback rápido sobre a lógica de negócio sem depender de browser. Se o service ou controller quebrar, o job 1 falha em menos de 2 minutos e o job 2 nem é iniciado — economizando recursos do runner.

**Artefatos publicados a cada execução:**

| Artefato | Conteúdo | Retenção |
| -------- | -------- | -------- |
| `surefire-reports` | Relatórios XML de cada classe de teste | 14 dias |
| `jacoco-coverage-report` | Relatório HTML de cobertura de linhas | 14 dias |
| `selenium-screenshots` | Capturas de tela dos testes E2E | 14 dias |

---

### 4.2. Workflow CD — `cd.yml`

**Arquivo:** `.github/workflows/cd.yml`

**Quando dispara:**
- Via `workflow_run`: após o CI concluir com **sucesso** na branch `main`
- Via `push` de tag no formato `v*.*.*` (ex: `v2.0.0`)

**Diagrama de execução:**

```
CI concluído com sucesso em main
(ou push de tag v*.*.*)
        │
        ▼
┌──────────────────────────────────────────────────────┐
│  Job 1: build-artefato                               │
│  Timeout: 10 minutos                                 │
│                                                      │
│  1. Checkout do repositório                          │
│  2. Setup Java 21 + cache Maven                      │
│  3. mvn -B clean package -DskipTests                 │
│     └── gera target/saikoo-1.0.0.jar                 │
│  4. Upload: saikoo-jar (30 dias)                     │
└──────────────────────┬───────────────────────────────┘
                       │ needs
                       ▼
┌──────────────────────────────────────────────────────┐
│  Job 2: deploy-dev (automático)                      │
│  └── Deploy para ambiente dev                        │
└──────────────────────┬───────────────────────────────┘
                       │ needs + tag v*.*.*-rc*
                       ▼
┌──────────────────────────────────────────────────────┐
│  Job 3: deploy-staging (automático)                  │
│  └── Deploy para staging                             │
│      Dispara post-deploy.yml                         │
└──────────────────────┬───────────────────────────────┘
                       │ needs + tag v*.*.* + aprovação manual
                       ▼
┌──────────────────────────────────────────────────────┐
│  Job 4: deploy-prod (aprovação manual obrigatória)   │
│  └── Deploy para produção via OIDC                   │
└──────────────────────┬───────────────────────────────┘
                       │ needs + tag v*.*.*
                       ▼
┌──────────────────────────────────────────────────────┐
│  Job 5: release                                      │
│  Timeout: 5 minutos                                  │
│                                                      │
│  1. Download do JAR (job anterior)                   │
│  2. Cria GitHub Release com:                         │
│     ├── JAR anexado                                  │
│     ├── Changelog automático dos commits             │
│     └── Instruções de execução                       │
└──────────────────────────────────────────────────────┘
```

---

### 4.3. Workflow Pós-Deploy — `post-deploy.yml`

**Arquivo:** `.github/workflows/post-deploy.yml`

**Quando dispara:**
- Via `workflow_run`: após o CD concluir deploy em staging

**Objetivo:** executar testes de fumaça com Selenium contra a URL pública do ambiente de staging, confirmando que as rotas críticas do sistema estão respondendo corretamente após o deploy.

```
CD concluído (staging)
        │
        ▼
┌──────────────────────────────────────────────────────┐
│  Job: testes-pos-deploy                              │
│  Timeout: 10 minutos                                 │
│                                                      │
│  1. Checkout do repositório                          │
│  2. Setup Java 21 + cache Maven                      │
│  3. Health check da URL de staging                   │
│  4. mvn -B test                                      │
│     -Dtest=AtivoSeleniumPosDeployTest                │
│     -Dapp.base-url=$STAGING_URL                      │
│     └── Chrome headless contra staging               │
│  5. Upload: screenshots-pos-deploy (14 dias)         │
│     (if: always)                                     │
└──────────────────────────────────────────────────────┘
```

---

## 5. Boas Práticas Aplicadas na Pipeline

### 5.1. CD dependente do CI (`workflow_run`)

**Problema sem esta prática:** Um `push` na `main` dispararia CI e CD em paralelo. Se o CI falhasse, o CD ainda empacotaria e publicaria código quebrado.

**Solução aplicada:** O trigger `workflow_run` garante que o CD só inicia após o CI concluir com sucesso.

```yaml
on:
  workflow_run:
    workflows: ["CI — Build, Test & Coverage"]
    types: [completed]
    branches: [main]
```

### 5.2. Maven Batch Mode (`-B`)

**Problema sem esta prática:** O Maven em modo padrão produz saída com códigos de cor ANSI e pode tentar interação com o terminal, gerando logs ilegíveis.

**Solução aplicada:** O flag `-B` em todos os comandos `mvn` desativa saída colorida e interativa, produzindo logs limpos e compatíveis com o runner.

### 5.3. Controle de Concorrência (`concurrency`)

**Problema sem esta prática:** Três pushes rápidos na mesma branch disparariam três pipelines em paralelo, consumindo minutos do runner com execuções que serão descartadas.

**Solução aplicada:**

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: ${{ github.ref != 'refs/heads/main' }}
```

No CI: cancela execuções desatualizadas em branches de feature, mas não na `main`. No CD: `cancel-in-progress: false` — uma entrega jamais deve ser interrompida.

### 5.4. Timeout por Job (`timeout-minutes`)

**Problema sem esta prática:** Um teste Selenium travado consumiria o limite de 6 horas do runner, bloqueando outros workflows.

| Job | Timeout | Base de cálculo |
| --- | ------- | --------------- |
| CI Job 1 (testes) | 15 min | ~3× o tempo médio esperado |
| CI Job 2 (Selenium) | 10 min | Margem para 6 testes headless |
| CD Job (package) | 10 min | `mvn package` sem testes em < 3 min |
| CD Job (release) | 5 min | Upload + API GitHub em < 2 min |
| Pós-deploy | 10 min | Health check + 4 testes Selenium |

### 5.5. Princípio do Menor Privilégio (`permissions`)

**Problema sem esta prática:** O `GITHUB_TOKEN` por padrão tem permissões de escrita desnecessárias durante a validação.

**Solução aplicada:** CI declara `permissions: contents: read`. O job de release declara `permissions: contents: write` apenas onde necessário.

### 5.6. Análise de Segurança SAST (CodeQL)

**Problema sem esta prática:** Vulnerabilidades introduzidas no código passam despercebidas até análise manual ou incidente em produção.

**Solução aplicada:** O CI executa análise CodeQL sobre o código Java a cada push e PR. O resultado é publicado na aba Security > Code scanning alerts do repositório.

### 5.7. Resumos Markdown (`$GITHUB_STEP_SUMMARY`)

**Problema sem esta prática:** Resultados de testes e cobertura ficam enterrados nos logs do runner, exigindo navegação manual para diagnóstico.

**Solução aplicada:** Cada execução do CI escreve um resumo em Markdown com contagem de testes, percentual de cobertura e links para artefatos, visível diretamente na interface do Actions.

---

## 6. Deploy Multi-Ambiente

O TP5 configura três ambientes no GitHub com regras de proteção distintas:

| Ambiente | Branch/Tag Fonte | Aprovação Manual | Proteções |
| -------- | ---------------- | ---------------- | --------- |
| `dev` | `main` | Não | Nenhuma restrição — integração rápida |
| `staging` | tags `v*.*.*-rc*` | Não | Requer CI verde; dispara pós-deploy |
| `production` | tags `v*.*.*` | Sim (reviewer) | Requer staging verde + aprovação |

### Autenticação OIDC

Ao invés de segredos de longa duração (service account keys), o CD utiliza OIDC para obter tokens temporários do provedor de nuvem. Isso elimina o risco de vazamento de credenciais estáticas e segue o princípio do menor privilégio para infraestrutura.

```yaml
- name: Autenticar no provedor de nuvem via OIDC
  uses: <provider>/auth@v2
  with:
    workload_identity_provider: ${{ secrets.WIF_PROVIDER }}
    service_account: ${{ secrets.WIF_SERVICE_ACCOUNT }}
```

### Variáveis e Segredos

| Tipo | Onde definido | O que armazena |
| ---- | ------------- | -------------- |
| `secrets.*` | Configuração do repositório / ambiente | Credenciais OIDC, tokens de deploy |
| `env.*` | Arquivo de workflow (`env:` global ou por job) | URLs de ambiente, flags de configuração |
| `vars.*` | Configuração do repositório | Valores não-sensíveis reutilizáveis |

---

## 7. Monitoramento e Depuração dos Workflows

### Logs Personalizados

O `logback-spring.xml` configura dois appenders:

- **Console:** nível INFO em desenvolvimento, WARNING em produção
- **Arquivo rotativo:** `logs/saikoo-<data-hora>.log` — um arquivo por sessão da aplicação, facilitando correlação entre execução e log

O formato do log inclui timestamp, nível, thread, logger abreviado e mensagem — padrão compatível com ferramentas de agregação de logs.

### Resumos de Jobs no GitHub Actions

Cada job do CI escreve em `$GITHUB_STEP_SUMMARY`:

```markdown
## Resultado dos Testes

| Métrica | Valor |
| ------- | ----- |
| Testes executados | 68 |
| Aprovados | 68 |
| Falhas | 0 |
| Cobertura de linhas | 91,3% |
```

O resumo é exibido diretamente na aba Summary de cada execução do workflow, sem necessidade de baixar artefatos ou analisar logs.

### Badges de Status

O `README.md` exibe badges em tempo real:

| Badge | Fonte | O que monitora |
| ----- | ----- | -------------- |
| CI | GitHub Actions | Status da última execução do `ci.yml` |
| CD | GitHub Actions | Status da última execução do `cd.yml` |
| Release | GitHub | Última tag publicada |
| JaCoCo | Badge estático | Meta de cobertura (≥ 90%) |

---

## 8. Justificativas Técnicas

### Separação em três arquivos de workflow

CI, CD e pós-deploy têm responsabilidades, ciclos de vida e gatilhos distintos. Misturá-los criaria lógica condicional complexa e de difícil manutenção. A separação torna a intenção imediata:
- `ci.yml` valida qualidade
- `cd.yml` entrega
- `post-deploy.yml` confirma integridade pós-entrega

### Aprovação manual somente em produção

Staging e dev operam de forma totalmente automatizada para preservar o feedback loop rápido. A aprovação manual é restrita à promoção para produção, onde o impacto de um problema é maior e a reversão é mais custosa.

### `workflow_run` em vez de trigger único

Um único trigger `push: branches: [main]` no CD dispararia CI e CD em paralelo, sem garantia de dependência. O `workflow_run` cria uma dependência explícita entre workflows independentes, que é o modelo correto para pipelines CI/CD em arquivos separados.

### Cobertura elevada de 85% para 90%

O aumento reflete a maturidade do projeto na fase de entrega final. Com o código refatorado e código morto removido, o esforço para atingir 90% é proporcional ao ganho em confiança sobre o comportamento do sistema.

### Artefatos com retenção diferenciada

Surefire, JaCoCo e screenshots: 14 dias — suficiente para revisão de PR. JAR no CD: 30 dias — cobre um sprint completo e permite rollback sem recriar o artefato.

---

## 9. Como Executar

### Localmente

**Pré-requisitos:** Java 21+, Maven 3.9+, Google Chrome (para Selenium)

```bash
# Iniciar a aplicação
mvn spring-boot:run
# Acesso: http://localhost:8080/ativos/dashboard

# Suíte completa + verificação de cobertura
mvn clean verify

# Apenas testes unitários e de integração
mvn clean verify -Dtest="!AtivoSeleniumTest" -DfailIfNoTests=false

# Apenas testes Selenium
mvn test -Dtest=AtivoSeleniumTest -DfailIfNoTests=false
```

### Via Pipeline (GitHub Actions)

Após o `git push`, acesse a aba **Actions** do repositório para acompanhar status de cada job, logs detalhados e resumos Markdown.

**Criar uma release de produção:**
```bash
git tag v2.0.0
git push origin v2.0.0
# O CD dispara: build → dev → staging → aprovação manual → prod → release
```

**Artefatos gerados:**

| Artefato | Local | Origem |
| -------- | ----- | ------ |
| Relatórios Surefire | Aba Actions → surefire-reports | CI Job 1 |
| Cobertura JaCoCo | Aba Actions → jacoco-coverage-report | CI Job 1 |
| Screenshots Selenium (CI) | Aba Actions → selenium-screenshots | CI Job 2 |
| Screenshots Pós-deploy | Aba Actions → screenshots-pos-deploy | post-deploy |
| JAR executável | Aba Actions → saikoo-jar | CD Job 1 |
| GitHub Release | Aba Releases | CD Job 5 (em tags) |

---

## 10. Evidências de Execução

Esta seção será preenchida com os resultados reais das execuções da pipeline após o push inicial do TP5, comprovando o funcionamento end-to-end do CI/CD expandido.

### 10.1. Execução CI — Testes + Cobertura + SAST

_Evidência a ser inserida após execução no GitHub Actions._

![CI Run — Jobs](images/cicd/ci-run-jobs-tp5.png)

---

### 10.2. Execução CD — Deploy Multi-Ambiente

_Evidência a ser inserida após execução no GitHub Actions._

![CD Run — Build](images/cicd/cd-run-deploy-tp5.png)

---

### 10.3. Execução Pós-Deploy — Selenium em Staging

_Evidência a ser inserida após execução no GitHub Actions._

![Pós-Deploy — Screenshots](images/cicd/post-deploy-screenshots-tp5.png)

---

### 10.4. GitHub Release v2.0.0

_Evidência a ser inserida após criação da tag e release._

```bash
# Para executar diretamente a partir da release
java -jar saikoo-v2.0.0.jar
# Acesse: http://localhost:8080/ativos/dashboard
```

![GitHub Release v2.0.0](images/cicd/release-v2.0.0.png)

---

### 10.5. Resumo Markdown no GitHub Actions

_Evidência a ser inserida após execução no GitHub Actions._

![Job Summary](images/cicd/job-summary-tp5.png)

---

## 11. Referências

- MARTIN, Robert C. _Clean Code: A Handbook of Agile Software Craftsmanship_. 2. ed. Prentice Hall, 2008.
- FOWLER, Martin. _Refactoring: Improving the Design of Existing Code_. 2. ed. Addison-Wesley, 2018.
- MEYER, Bertrand. _Object-Oriented Software Construction_. 2. ed. Prentice Hall, 1997. (Princípio CQS)
- GitHub Actions Documentation: https://docs.github.com/en/actions
- GitHub Actions — Environments: https://docs.github.com/en/actions/deployment/targeting-different-environments
- GitHub Actions — OIDC: https://docs.github.com/en/actions/security-for-github-actions/security-hardening-your-deployments/about-security-hardening-with-openid-connect
- GitHub Actions — Job Summaries: https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/workflow-commands-for-github-actions#adding-a-job-summary
- CodeQL Documentation: https://codeql.github.com/docs/
- Spring Boot Reference Documentation: https://docs.spring.io/spring-boot/docs/3.3.0/reference/html/
- JaCoCo Documentation: https://www.jacoco.org/jacoco/trunk/doc/
- Selenium WebDriver Documentation: https://www.selenium.dev/documentation/webdriver/
- Jqwik User Guide: https://jqwik.net/docs/current/user-guide.html

---

**Autor:** André Luis Becker
**Licença:** MIT
