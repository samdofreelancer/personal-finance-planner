# Implementation Plan: Personal Finance Planner

**Status:** Planning — Backend + Frontend + Database

## Proposed architecture

Three-tier local application:

- **Backend:** Spring Boot 3.x, Java 21, Maven
  - REST controllers: AssumptionController, ForecastController
  - Service layer: AssumptionService, ForecastService (calculation ported from forecast.mjs)
  - Entity: Assumption (JPA, single row)
  - Repository: AssumptionRepository (Spring Data JPA)
  - Database: PostgreSQL (Spring Data JPA + Hibernate)
  - CORS configured for Vue frontend
  - Seeder: inserts default assumptions on startup if table empty

- **Database:** PostgreSQL
  - Table: assumptions (one row, monetary columns BIGINT, rates DECIMAL)
  - Local installation required

- **Frontend:** Vue.js 3 (Composition API), Vite
  - Single page: assumptions form, metric cards, annual chart, annual table, monthly table
  - Services layer: api.js using fetch to call Spring Boot REST API
  - Local SVG chart (no chart library)
  - Vietnamese UI, VND formatting

## Directory structure

backend/
  pom.xml
  src/main/java/com/finance/planner/
    PlannerApplication.java
    controller/AssumptionController.java
    controller/ForecastController.java
    service/AssumptionService.java
    service/ForecastService.java
    repository/AssumptionRepository.java
    entity/Assumption.java
    dto/AssumptionDTO.java
    dto/ForecastResponse.java
    dto/ForecastMonthDTO.java
    dto/ForecastAnnualDTO.java
    config/WebConfig.java
  src/main/resources/application.properties
  src/test/java/com/finance/planner/ForecastServiceTest.java

frontend/
  index.html
  package.json
  vite.config.js
  src/main.js
  src/App.vue
  src/components/AssumptionsForm.vue
  src/components/MetricCards.vue
  src/components/AnnualChart.vue
  src/components/AnnualTable.vue
  src/components/MonthlyTable.vue
  src/services/api.js
  src/assets/style.css

## Verification

- Backend unit tests: forecast calculation matches forecast.test.mjs expected values
- Backend integration test: assumptions CRUD
- Frontend: manual browser test against http://localhost:8080
- End-to-end: edit assumptions → API PUT → API GET forecast → verify numbers