# Deployment Guide

This guide covers building, pushing the Docker image, and deploying the application using Helm and ArgoCD.

## Prerequisites

- Docker installed and running
- Kubernetes cluster (local or cloud)
- kubectl configured to access your cluster
- Helm 3.x installed
- ArgoCD installed in the `argocd` namespace

## Docker Build and Push

### Build and Push to Docker Hub

The project includes a script to build and push the Docker image to Docker Hub:

```bash
./docker-build-push.sh
```

This script will:
1. Build the Docker image with tag `asadbekabdinazarov/devops-assignment:latest`
2. Login to Docker Hub using the configured credentials
3. Push the image to Docker Hub

### Manual Build and Push

If you prefer to build and push manually:

```bash
# Build the image
docker build -t asadbekabdinazarov/devops-assignment:latest .

# Login to Docker Hub
docker login -u asadbekabdinazarov

# Push the image
docker push asadbekabdinazarov/devops-assignment:latest
```

## Helm Chart Deployment

### Install Using Helm

Deploy the application using Helm:

```bash
# Install the chart
helm install devops-assignment ./helm/devops-assignment

# Or with custom values
helm install devops-assignment ./helm/devops-assignment -f ./helm/devops-assignment/values.yaml

# Upgrade existing installation
helm upgrade devops-assignment ./helm/devops-assignment

# Uninstall
helm uninstall devops-assignment
```

### Verify Deployment

```bash
# Check pods
kubectl get pods -n devops-assignment

# Check services
kubectl get svc -n devops-assignment

# Check Prometheus (manually installed)
kubectl get pods -n monitoring
kubectl get svc -n monitoring
```

### Access the Application

```bash
# Port forward to access the application
kubectl port-forward svc/devops-assignment 8080:8080 -n devops-assignment

# Access metrics endpoint
curl http://localhost:8080/actuator/prometheus

# Access health endpoint
curl http://localhost:8080/actuator/health
```

### Access Prometheus UI

```bash
# Port forward to access Prometheus (use your actual Prometheus service name)
kubectl port-forward svc/prometheus 9090:9090 -n monitoring

# Or find your Prometheus service name:
kubectl get svc -n monitoring | grep prometheus

# Open in browser
open http://localhost:9090
```

## ArgoCD Deployment

### Prerequisites

Ensure ArgoCD is installed in your cluster:

```bash
# Check if ArgoCD is installed
kubectl get pods -n argocd

# If not installed, install ArgoCD (example for local cluster)
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

### Get ArgoCD Admin Credentials

**Default username:** `admin`

**Get the password:**

For local Kubernetes cluster:
```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d && echo
```

**Access ArgoCD UI:**

```bash
# Port forward ArgoCD server
kubectl port-forward svc/argocd-server -n argocd 8080:443

# Or use NodePort/LoadBalancer if configured
# Default URL: https://localhost:8080
```

Login with:
- **Username:** `admin`
- **Password:** (from the command above)

### Deploy Using ArgoCD Application Manifest

1. **Update the Git repository URL** in the ArgoCD Application manifests:
   - Edit `argocd/application.yaml`
   - Edit `argocd/application-prometheus.yaml`
   - Replace `https://github.com/your-username/devops-assignment.git` with your actual Git repository URL

2. **Apply the ArgoCD Application:**

```bash
# Deploy the main application
kubectl apply -f argocd/application.yaml

# Note: Prometheus should be installed separately using your own Helm chart
# The application only creates ServiceMonitor for Prometheus to discover metrics
```

3. **Verify ArgoCD Application:**

```bash
# List applications
kubectl get applications -n argocd

# Get application details
kubectl get application devops-assignment -n argocd -o yaml

# Check application status via ArgoCD CLI (if installed)
argocd app get devops-assignment
```

### ArgoCD CLI (Optional)

Install ArgoCD CLI for easier management:

```bash
# macOS
brew install argocd

# Linux
curl -sSL -o /usr/local/bin/argocd https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
chmod +x /usr/local/bin/argocd
```

Login to ArgoCD:
```bash
argocd login localhost:8080
# Username: admin
# Password: (from kubectl command above)
```

## Configuration

### Update Helm Values

Edit `helm/devops-assignment/values.yaml` to customize:
- Image repository and tag
- Replica count
- Resource limits and requests
- Service type
- Namespace settings
- ServiceMonitor configuration (for Prometheus discovery)

### Environment Variables

Add environment variables in `helm/devops-assignment/values.yaml`:

```yaml
app:
  env:
    - name: SPRING_PROFILES_ACTIVE
      value: "production"
    - name: CUSTOM_VAR
      value: "custom-value"
```

## Monitoring

### Prometheus Metrics

The application exposes Prometheus metrics at:
- Endpoint: `/actuator/prometheus`
- ServiceMonitor: Automatically created if `serviceMonitor.enabled: true`

### Health Checks

- Liveness: `/actuator/health/liveness`
- Readiness: `/actuator/health/readiness`
- General: `/actuator/health`

## Troubleshooting

### Check Pod Logs

```bash
# Application logs
kubectl logs -f deployment/devops-assignment -n devops-assignment

# Prometheus logs (use your actual Prometheus deployment name)
kubectl logs -f deployment/prometheus -n monitoring
```

### Check Pod Status

```bash
# Describe pod for events
kubectl describe pod <pod-name> -n devops-assignment

# Check pod events
kubectl get events -n devops-assignment --sort-by='.lastTimestamp'
```

### ArgoCD Sync Issues

```bash
# Check application sync status
kubectl get application devops-assignment -n argocd

# Force sync via CLI
argocd app sync devops-assignment

# Check sync history
argocd app history devops-assignment
```

### Docker Build Issues

```bash
# Check Docker daemon
docker info

# Clean build cache
docker builder prune

# Build without cache
docker build --no-cache -t asadbekabdinazarov/devops-assignment:latest .
```

## Security Notes

- Docker Hub credentials are stored in `docker-build-push.sh` - consider using environment variables or secrets
- The Dockerfile runs as non-root user for security
- Update ArgoCD Application manifests with your actual Git repository URL
- Consider using sealed secrets or external secrets for sensitive data

## Next Steps

1. Update Git repository URL in ArgoCD Application manifests
2. Build and push Docker image to Docker Hub
3. Deploy using Helm or ArgoCD
4. Verify application and Prometheus are running
5. Access ArgoCD UI and verify sync status
