# Cloud Resource Monitoring Dashboard - common workflows.
# Run `make help` for the list of targets.

.DEFAULT_GOAL := help

SHELL := /bin/bash
BACKEND_DIR := backend
FRONTEND_DIR := frontend
MVN ?= mvn
NPM ?= npm
COMPOSE ?= docker compose

.PHONY: help install dev dev-mock backend-run backend-build backend-test \
        frontend-install frontend-run frontend-build mock-api \
        docker-build docker-up docker-down docker-logs clean distclean

help: ## Show this help
	@echo "Cloud Resource Monitoring Dashboard"
	@echo
	@echo "Usage: make <target>"
	@echo
	@grep -hE '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'
	@echo
	@echo "Requirements: JDK 21 + Maven (backend), Node 20.19+ (frontend), Docker (optional)."

## ---- Run everything -------------------------------------------------------

install: frontend-install ## Install frontend dependencies (npm ci)

dev: ## Run backend and frontend side by side (two processes, Ctrl-C stops both)
	@$(MAKE) -j2 backend-run frontend-run

dev-mock: ## Run the stub API and the frontend: no JDK, no AWS credentials
	@$(MAKE) -j2 mock-api frontend-run

## ---- Backend --------------------------------------------------------------

backend-run: ## Start the Spring Boot API on :8080 (demo mode by default)
	cd $(BACKEND_DIR) && $(MVN) -q spring-boot:run

backend-test: ## Run the backend unit + HTTP contract tests
	cd $(BACKEND_DIR) && $(MVN) test

backend-build: ## Build the executable jar in backend/target
	cd $(BACKEND_DIR) && $(MVN) -DskipTests package

## ---- Frontend -------------------------------------------------------------

frontend-install: ## npm ci in frontend/
	cd $(FRONTEND_DIR) && $(NPM) ci

frontend-run: ## Vite dev server on :5173, proxying /api to :8080
	cd $(FRONTEND_DIR) && $(NPM) run dev

frontend-build: ## Production bundle in frontend/dist
	cd $(FRONTEND_DIR) && $(NPM) run build

mock-api: ## Dev-only stub API on :8080 for UI work without the JVM
	cd $(FRONTEND_DIR) && $(NPM) run mock:api

## ---- Docker ---------------------------------------------------------------

docker-build: ## Build both images
	$(COMPOSE) build

docker-up: ## Build and start the stack, then show the dashboard URL
	$(COMPOSE) up --build -d
	@echo
	@echo "  dashboard  http://localhost:3000"
	@echo "  api        http://localhost:8080/api/health"

docker-down: ## Stop the stack
	$(COMPOSE) down

docker-logs: ## Tail logs from both containers
	$(COMPOSE) logs -f

## ---- Housekeeping ---------------------------------------------------------

clean: ## Remove build output (keeps node_modules)
	cd $(BACKEND_DIR) && $(MVN) -q clean
	rm -rf $(FRONTEND_DIR)/dist

distclean: clean ## clean, plus remove node_modules
	rm -rf $(FRONTEND_DIR)/node_modules
