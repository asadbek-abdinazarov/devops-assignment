# Prometheus ServiceMonitor Discovery - Troubleshooting Guide

## Tekshirish Qadamlari

### 1. ServiceMonitor Label'larini Tekshirish

```bash
# ServiceMonitor'ni ko'rish
kubectl get servicemonitor -n devops-assignment

# ServiceMonitor label'larini batafsil ko'rish
kubectl get servicemonitor -n devops-assignment -o yaml | grep -A 10 labels

# To'liq ServiceMonitor konfiguratsiyasini ko'rish
kubectl get servicemonitor -n devops-assignment -o yaml
```

**Kutilayotgan natija:** ServiceMonitor'da `release: prometheus` label'i bo'lishi kerak.

### 2. Prometheus CRD Konfiguratsiyasini Tekshirish

```bash
# Prometheus CRD'ni topish
kubectl get prometheus -n monitoring

# Prometheus konfiguratsiyasini ko'rish
kubectl get prometheus -n monitoring -o yaml

# ServiceMonitor selector'ni tekshirish
kubectl get prometheus -n monitoring -o jsonpath='{.items[0].spec.serviceMonitorSelector}'
```

**Kutilayotgan natija:** Prometheus'ning `serviceMonitorSelector` ServiceMonitor label'lariga mos kelishi kerak.

### 3. Prometheus Target'larini Tekshirish

```bash
# Prometheus UI'ga kirish (port-forward)
kubectl port-forward svc/prometheus-kube-prometheus-prometheus 9090:9090 -n monitoring

# Keyin browser'da oching: http://localhost:9090
# Status → Targets bo'limiga o'ting
```

**Kutilayotgan natija:**
- Target'lar "UP" holatida bo'lishi kerak
- `http://<service-name>:8080/actuator/prometheus` endpoint ko'rinishi kerak
- Agar "DOWN" bo'lsa, xatolik sababini ko'ring

### 4. Service va Pod Label'larini Tekshirish

ServiceMonitor Service'ni topishi uchun label'lar mos kelishi kerak:

```bash
# Service label'larini ko'rish
kubectl get svc -n devops-assignment --show-labels

# Pod label'larini ko'rish
kubectl get pods -n devops-assignment --show-labels

# ServiceMonitor selector'ni ko'rish
kubectl get servicemonitor -n devops-assignment -o jsonpath='{.items[0].spec.selector}'
```

**Muammo:** Agar label'lar mos kelmasa, ServiceMonitor Service'ni topa olmaydi.

### 5. Metrikalar Endpoint'ini Tekshirish

```bash
# Pod ichida metrikalar endpoint'ini tekshirish
POD_NAME=$(kubectl get pods -n devops-assignment -o jsonpath='{.items[0].metadata.name}')
kubectl exec -it $POD_NAME -n devops-assignment -- curl http://localhost:8080/actuator/prometheus | head -20

# Service orqali tekshirish (port-forward)
kubectl port-forward svc/devops-assignment-devops 8080:8080 -n devops-assignment
curl http://localhost:8080/actuator/prometheus | head -20
```

**Kutilayotgan natija:** Prometheus formatida metrikalar ko'rinishi kerak.

## Keng Tarqalgan Muammolar va Yechimlar

### Muammo 1: ServiceMonitor Prometheus tomonidan topilmayapti

**Sabab:** Label'lar mos kelmayapti

**Yechim:** 
1. `values.yaml` da `serviceMonitor.additionalLabels` ga to'g'ri label qo'shing:
   ```yaml
   serviceMonitor:
     additionalLabels:
       release: prometheus
   ```
2. ArgoCD'ni sync qiling yoki Helm upgrade qiling
3. ServiceMonitor label'larini tekshiring

### Muammo 2: Prometheus boshqa namespace'dan ServiceMonitor qidirayapti

**Sabab:** Prometheus CRD'da `serviceMonitorNamespaceSelector` sozlangan

**Yechim:** Prometheus CRD'ga namespace selector qo'shing:
```yaml
spec:
  serviceMonitorNamespaceSelector:
    matchLabels:
      name: devops-assignment
  # yoki barcha namespace'larni qabul qilish uchun:
  serviceMonitorNamespaceSelector: {}
```

### Muammo 3: Target DOWN holatida

**Sabablar:**
- Network connectivity muammosi
- Service port nomi noto'g'ri
- Endpoint path noto'g'ri

**Yechim:**
1. Service port nomi `http` bo'lishi kerak (ServiceMonitor'da `port: http`)
2. Endpoint path `/actuator/prometheus` bo'lishi kerak
3. Pod va Service label'larini tekshiring

### Muammo 4: Metrikalar ko'rinmayapti

**Sabablar:**
- Dastur metrikalarni expose qilmayapti
- Prometheus scrape qilmayapti
- Grafana to'g'ri data source'ga ulanmagan

**Yechim:**
1. Dastur metrikalar endpoint'ini tekshiring
2. Prometheus target'larini tekshiring
3. Grafana data source'ni tekshiring

## Tezkor Tekshirish Skripti

```bash
#!/bin/bash

NAMESPACE="devops-assignment"
PROMETHEUS_NAMESPACE="monitoring"

echo "=== 1. ServiceMonitor holati ==="
kubectl get servicemonitor -n $NAMESPACE

echo -e "\n=== 2. ServiceMonitor label'lar ==="
kubectl get servicemonitor -n $NAMESPACE -o jsonpath='{.items[0].metadata.labels}' | jq

echo -e "\n=== 3. ServiceMonitor selector ==="
kubectl get servicemonitor -n $NAMESPACE -o jsonpath='{.items[0].spec.selector}'
echo ""

echo -e "\n=== 4. Service label'lar ==="
kubectl get svc -n $NAMESPACE --show-labels

echo -e "\n=== 5. Prometheus CRD ==="
kubectl get prometheus -n $PROMETHEUS_NAMESPACE

echo -e "\n=== 6. Prometheus ServiceMonitor selector ==="
kubectl get prometheus -n $PROMETHEUS_NAMESPACE -o jsonpath='{.items[0].spec.serviceMonitorSelector}' 2>/dev/null || echo "Topilmadi"
echo ""

echo -e "\n=== 7. Metrikalar endpoint tekshiruvi ==="
POD_NAME=$(kubectl get pods -n $NAMESPACE -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
if [ ! -z "$POD_NAME" ]; then
    kubectl exec -it $POD_NAME -n $NAMESPACE -- curl -s http://localhost:8080/actuator/prometheus | head -5
else
    echo "Pod topilmadi"
fi
```

## Keyingi Qadamlar

1. `values.yaml` ni yangilang va ArgoCD'ni sync qiling
2. ServiceMonitor label'larini tekshiring
3. Prometheus target'larini tekshiring
4. Agar hali ham muammo bo'lsa, Prometheus CRD konfiguratsiyasini tekshiring
