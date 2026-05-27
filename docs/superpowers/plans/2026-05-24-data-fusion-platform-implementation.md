# Data Fusion Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Spring Boot 3 local demo application described by `docs/superpowers/specs/2026-05-24-data-fusion-platform-design.md`.

**Architecture:** A Maven Java 17 monolith uses Spring MVC, Thymeleaf, Spring JDBC, and SQLite. Startup initialization creates local `data` directories, schema, demo users, notices, data sources, clean rule defaults, and sample CSV/XLSX/XLS files. Services own import parsing, row cleaning, statistics, quality checks, export, ledger, notices, users, and operation logs; controllers expose the five required main pages plus auth/account/report endpoints.

**Tech Stack:** Java 17, Maven, Spring Boot 3.x, Thymeleaf, Spring JDBC, SQLite, Apache POI, Commons CSV, Jackson, local static CSS/JS.

---

## File Structure

- Create `pom.xml` for Spring Boot, SQLite, Apache POI, Commons CSV, and tests.
- Create `src/main/java/com/dataprocess/platform/DataFusionPlatformApplication.java` as the boot class.
- Create `src/main/java/com/dataprocess/platform/config/DataSourceConfig.java` and `WebConfig.java` for local SQLite and session auth.
- Create `src/main/java/com/dataprocess/platform/init/DatabaseInitializer.java` for directories, schema, seed data, and sample files.
- Create model records under `src/main/java/com/dataprocess/platform/model`.
- Create DTO records under `src/main/java/com/dataprocess/platform/dto`.
- Create `src/main/java/com/dataprocess/platform/repository/PlatformRepository.java` for SQLite access.
- Create services named in the design document, including `DuoyuanRongheChuli`.
- Create controllers named in the design document for auth, aggregation, fusion, statistics, quality, ledger, account, notices, and reports.
- Create Thymeleaf pages under `src/main/resources/templates`.
- Create local CSS/JS under `src/main/resources/static`.
- Create tests under `src/test/java/com/dataprocess/platform/service`.

## Tasks

### Task 1: Project and Red Tests

**Files:**
- Create: `pom.xml`
- Create: `src/test/java/com/dataprocess/platform/service/DataPreviewServiceTest.java`
- Create: `src/test/java/com/dataprocess/platform/service/DataCleanServiceTest.java`
- Create: `src/test/java/com/dataprocess/platform/service/QualityCheckServiceTest.java`

- [ ] Write tests for CSV preview, row cleaning, and quality evaluation.
- [ ] Run `mvn test` and verify the tests fail because the production classes do not exist yet.

### Task 2: Core Model, DTO, Repository, and Initialization

**Files:**
- Create: `src/main/java/com/dataprocess/platform/model/*.java`
- Create: `src/main/java/com/dataprocess/platform/dto/*.java`
- Create: `src/main/java/com/dataprocess/platform/repository/PlatformRepository.java`
- Create: `src/main/java/com/dataprocess/platform/init/DatabaseInitializer.java`

- [ ] Implement data records matching the SQLite schema.
- [ ] Implement schema creation for `sys_user`, `data_source`, `import_batch`, `import_record`, `fusion_record`, `cleaned_record`, `clean_rule_config`, `check_result`, `stat_report`, `sys_log`, and `notice`.
- [ ] Seed `admin / 123456`, demo data sources, notices, clean rule defaults, and sample CSV/XLSX/XLS files.

### Task 3: Core Services

**Files:**
- Create: `src/main/java/com/dataprocess/platform/service/*.java`
- Create: `src/main/java/com/dataprocess/platform/util/*.java`

- [ ] Implement import parsing for CSV, XLSX, and XLS with preview tokens.
- [ ] Implement confirm-import persistence into `import_batch` and `import_record`.
- [ ] Implement real clean rule application and persistence into `fusion_record` and `cleaned_record`.
- [ ] Implement statistics, quality check, export, ledger, notice, user, auth, and log services.
- [ ] Implement `DuoyuanRongheChuli` with the required legacy method names delegating to services.
- [ ] Run `mvn test` and verify the three core service tests pass.

### Task 4: Web Controllers and Views

**Files:**
- Create: `src/main/java/com/dataprocess/platform/controller/*.java`
- Create: `src/main/resources/templates/*.html`
- Create: `src/main/resources/static/css/app.css`
- Create: `src/main/resources/static/js/app.js`
- Create: `src/main/resources/application.properties`

- [ ] Implement `/login`, `/register`, `/forgot`, `/logout`, `/aggregation`, `/fusion`, `/statistics`, `/quality`, `/ledger`, `/account`, `/notices`, and report download/export routes.
- [ ] Implement Thymeleaf pages with orange navigation, gray background, white content panels, modal dialogs, Chinese labels, upload preview, clean rule config, chart canvases, quality report generation, account reset, notices, and logout confirmation.

### Task 5: Verification

**Files:**
- Modify as required by verification feedback.

- [ ] Run `mvn test`.
- [ ] Run `mvn package`.
- [ ] Start the app with `mvn spring-boot:run`.
- [ ] Verify `http://localhost:8080/login` responds and the application initializes `data/data_fusion.db`, `data/uploads`, `data/exports`, and `data/samples`.
