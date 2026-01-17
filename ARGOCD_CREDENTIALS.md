# ArgoCD Credentials

## Quick Access

### Default Username
```
admin
```

### Get Password (Local Kubernetes)

Run this command to retrieve the ArgoCD admin password:

```bash
kubectl -n argo-cd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d && echo
```

### Access ArgoCD UI

1. Port forward the ArgoCD server:
```bash
kubectl port-forward svc/argocd-server -n argo-cd 8080:443
```

2. Open in browser:
```
https://localhost:8080
```

3. Login with:
   - **Username:** `admin`
   - **Password:** (from the command above)

### Change Password (Optional)

After first login, you can change the password using ArgoCD CLI:

```bash
# Install ArgoCD CLI (if not installed)
# macOS: brew install argocd
# Linux: See https://argo-cd.readthedocs.io/en/stable/cli_installation/

# Login
argocd login localhost:8080

# Change password
argocd account update-password
```

## Notes

- The initial admin password is stored in the `argocd-initial-admin-secret` secret
- After changing the password, the initial secret is no longer used
- For production, consider setting up SSO or additional authentication methods
