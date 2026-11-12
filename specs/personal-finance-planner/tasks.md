# Tasks: Personal Finance Planner (Backend + Frontend + DB)

**Status:** Planned

## Phase 1 — Backend: Spring Boot + PostgreSQL

- [ ] Create Maven project (backend/pom.xml) with Spring Boot 3, Java 21, spring-data-jpa, postgresql, validation, web
- [ ] Configure application.properties (PostgreSQL datasource, CORS, server port 8080)
- [ ] Create Assumption JPA entity (single row, monetary fields BIGINT)
- [ ] Create AssumptionRepository (Spring Data JPA)
- [ ] Create AssumptionDTO + ForecastResponse DTOs
- [ ] Create AssumptionController (GET/PUT /api/assumptions, POST /api/seed)
- [ ] Create ForecastService (port calculation logic from forecast.mjs to Java)
- [ ] Create ForecastController (GET /api/forecast, GET /api/export/csv)
- [ ] Create WebConfig (CORS for Vue frontend)
- [ ] Add seeder: insert default assumptions on startup if table empty
- [ ] Write unit tests for ForecastService (match forecast.test.mjs expected values)
- [ ] Build & run backend, verify http://localhost:8080/api/assumptions

## Phase 2 — Frontend: Vue.js + Vite

- [ ] Scaffold Vue 3 + Vite project in frontend/
- [ ] Create src/services/api.js (fetch wrapper to Spring Boot)
- [ ] Create App.vue layout (header, metrics, assumptions panel, chart, tables)
- [ ] Create AssumptionsForm.vue (editable inputs, validation)
- [ ] Create MetricCards.vue (net cash flow, savings, salary)
- [ ] Create AnnualChart.vue (SVG column chart)
- [ ] Create AnnualTable.vue + MonthlyTable.vue
- [ ] Wire form inputs → API PUT assumptions → API GET forecast → render
- [ ] Add reset button (calls POST /api/seed or PUT defaults)
- [ ] Add CSV export button (calls GET /api/export/csv)
- [ ] Add Vietnamese VND formatting

## Phase 3 — Verify & Deliver

- [ ] Install PostgreSQL, create database personal_finance_planner
- [ ] Update application.properties with real PostgreSQL credentials
- [ ] Run backend, verify API endpoints
- [ ] Run frontend dev server, verify UI loads from http://localhost:5173
- [ ] End-to-end test: edit salary → save → reload → values persist
- [ ] Run backend unit tests (8 test cases matching forecast.test.mjs)
- [ ] Update README with build/run instructions