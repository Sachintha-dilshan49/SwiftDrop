# SwiftDrop – Kubernetes & Helm Deployment Guide

This directory contains the Helm umbrella chart for the complete **SwiftDrop** microservices platform.

---

## 1. Prerequisites

- Kubernetes cluster (e.g., [Minikube](https://minikube.sigs.k8s.io/) or Docker Desktop Kubernetes)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Helm v3](https://helm.sh/docs/intro/install/) (v3.12+)
- [ingress-nginx](https://kubernetes.github.io/ingress-nginx/) controller

---

## 2. Architecture Overview

The `swiftdrop` umbrella chart deploys:
1. **Frontend Tier:** React SPA containerized with Nginx.
2. **Edge Tier:** `api-gateway` (Spring Cloud Gateway) routing all API and SSE traffic.
3. **Backend Microservices:**
   - `auth-service` (:8081)
   - `pricing-service` (:8082)
   - `parcel-service` (:8083)
   - `dispatch-service` (:8084)
   - `merchant-service` (:8085)
   - `notification-service` (:8086)
   - `ai-assistant` (:8087)
4. **Infrastructure Tier (Standalone or Bitnami):**
   - PostgreSQL 16 (Multi-database: `auth_db`, `pricing_db`, `parcel_db`, `dispatch_db`, `merchant_db`, `notification_db`)
   - RabbitMQ 3.13 (Management UI on :15672, AMQP on :5672)
   - Redis 7 (:6379)
   - MinIO S3 Object Storage (:9000 API, :9001 Console)
   - Mailpit SMTP & Web Inspector (:1025 / :8025)
5. **Real-Time Ingress:**
   - Ingress controller with `nginx.ingress.kubernetes.io/proxy-buffering: "off"` to ensure Server-Sent Events (SSE) tracking and AI chat stream in real time.
6. **Automated Lifecycle Hooks:**
   - Post-install Helm Job creating the `delivery-proofs` MinIO bucket and setting public download permissions.

---

## 3. Quickstart: Local Deployment on Minikube

### Step 1: Start Minikube & Enable Ingress
```bash
minikube start --cpus=4 --memory=8192
minikube addons enable ingress
```

### Step 2: Configure Local Host Resolution
Add `swiftdrop.local` to your hosts file (`/etc/hosts` or `C:\Windows\System32\drivers\etc\hosts`):
```text
127.0.0.1 swiftdrop.local
```
*(On Linux/macOS with native Minikube, use `$(minikube ip) swiftdrop.local` or run `minikube tunnel`).*

### Step 3: Install the Chart
```bash
helm install swiftdrop deploy/helm/swiftdrop -f deploy/helm/swiftdrop/values-local.yaml
```

### Step 4: Verify Pod Health
```bash
kubectl get pods -w
```
All Spring Boot services feature `readinessProbe` and `livenessProbe` tied to Spring Actuator. Pods report `Ready` once database migrations complete.

---

## 4. Running System E2E Tests Against Kubernetes

You can execute the entire Playwright test suite against your running Kubernetes cluster:

```bash
cd e2e
npm ci
export E2E_GATEWAY_URL="http://swiftdrop.local/api"
export E2E_FRONTEND_URL="http://swiftdrop.local"
npx playwright test tests/api tests/flows tests/ui
```
*(On Windows PowerShell: `$env:E2E_GATEWAY_URL="http://swiftdrop.local/api"`; `$env:E2E_FRONTEND_URL="http://swiftdrop.local"`)*

---

## 5. Production & Cloud Deployments

For staging or production clusters, override default secrets and supply your domain:

```bash
helm upgrade --install swiftdrop deploy/helm/swiftdrop \
  --set ingress.host="app.swiftdrop.lk" \
  --set secrets.internalApiToken="<PRODUCTION_TOKEN>" \
  --set secrets.jwtPrivateKeyB64="<BASE64_PRIVATE_KEY>" \
  --set secrets.jwtPublicKeyB64="<BASE64_PUBLIC_KEY>" \
  --set secrets.postgresPassword="<STRONG_PASSWORD>"
```

To use official Bitnami Helm charts in place of the built-in standalone infrastructure:
```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm dependency update deploy/helm/swiftdrop
helm install swiftdrop deploy/helm/swiftdrop --set infrastructure.standalone=false
```
