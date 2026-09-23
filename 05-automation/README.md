# Kapitel 5: Automation – Kubernetes Manifeste & GitOps

## Kubernetes Manifeste

Kubernetes-Ressourcen werden als **deklarative YAML-Manifeste** beschrieben.

### Struktur eines Manifests

```yaml
apiVersion: apps/v1        # API-Gruppe und Version
kind: Deployment           # Ressourcentyp
metadata:
  name: demo               # Name der Ressource
  namespace: demo-seminar  # Namespace (optional, nutzt Default wenn weggelassen)
  labels:
    app: demo
spec:                      # Gewünschter Zustand (desired state)
  replicas: 2
  ...
```

---

## GitOps mit oc apply / kubectl apply

**GitOps**: YAML-Manifeste in Git verwalten → automatisch im Cluster anwenden.

```bash
# Einzelne Datei anwenden
kubectl apply -f deployment.yaml

# Komplettes Verzeichnis anwenden
kubectl apply -f ./manifests/

# Rekursiv
kubectl apply -R -f ./

# Dry-Run (Was würde passieren?)
kubectl apply -f deployment.yaml --dry-run=client

# Server-seitiger Dry-Run
kubectl apply -f deployment.yaml --dry-run=server

# Diff anzeigen (was ändert sich?)
kubectl diff -f deployment.yaml
```

### Ablauf GitOps

```
Developer       Git-Repository       Kubernetes (Soll-Zustand) (Ist-Zustand)
    │                 │                  │                     |
    │──── push ──────►│                  │                     |
    │                 │                  │                     |
    │                 │──── oc apply ───►│                     |
    │                 │                  │                     |
    │                 │                  |── control-loop ───► |

```
---

## Arbeiten mit Manifesten

### Ressourcen anzeigen

```bash
# Manifest eines laufenden Objekts ausgeben
kubectl get deployment demo -o yaml

# Alles im Namespace
kubectl get all -n demo-seminar

# Alle API-Ressourcentypen
kubectl api-resources

```
### Labels und Selektoren

```bash
# Ressourcen mit Label filtern
kubectl get pods -l app=demo

# Label hinzufügen
kubectl label pod demo version=v2

# Label entfernen
kubectl label pod demo version-
```

### Kustomize (eingebaut in kubectl)

```bash
# kustomization.yaml in aktuellem Verzeichnis anwenden
kubectl apply -k .

# Kustomize-Output anzeigen
kubectl kustomize .
```

---

## Manifeste in diesem Kapitel

- `namespace.yaml` – Namespace/Project-Definition
- `deployment.yaml` – Deployment der Demo-Anwendung

---

## Weiterführende Links

- [Kubernetes: Deklaratives Management von Objekten](https://kubernetes.io/docs/tasks/manage-kubernetes-objects/declarative-config/)
- [Kubernetes: Kustomization](https://kubernetes.io/docs/tasks/manage-kubernetes-objects/kustomization/)
- [OpenShift 4.22: GitOps](https://docs.redhat.com/en/documentation/openshift_container_platform/4.22/html/gitops/index)
- [OpenShift 4.22: CI/CD Overview](https://docs.redhat.com/en/documentation/openshift_container_platform/4.22/html/cicd_overview/index)

Siehe auch [99-anhang/DOCUMENTATION.md](../99-anhang/DOCUMENTATION.md) für die vollständige Liste.
