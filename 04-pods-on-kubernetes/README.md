# Kapitel 4: Container/Pods auf Kubernetes

## Namespace / OpenShift Project

Ein **Namespace** ist eine logische Isolationsschicht in Kubernetes.
OpenShift nennt diese **Projects** (= Namespace + zusätzliche Metadaten).

Genauer: Ein **Project** ist ein Hilfsobjekt, um leichter mit **Namespaces** umgehen zu können.

```bash
# Namespace/Project anzeigen
oc get projects #(geht immer)
kubectl get namespaces #(funktioniert nur mit cluster-admin-Berechtigung)

# Neues Project erstellen
oc new-project demo-seminar

# In Namespace/Project wechseln
oc project demo-seminar

# (fast) Alle Ressourcen im aktuellen Namespace
kubectl get all
```

---

## Warum Pods?

Ein **Pod** ist die kleinste deploybare Einheit in Kubernetes:
- Enthält einen oder mehrere Container
- Teilen sich Netzwerk (localhost) und Storage (optional)
- Haben eine gemeinsame IP-Adresse
- Laufen immer auf demselben Node

```bash
# Pod direkt starten (für Tests/Demos)
oc run demo --image=quay.io/ghilling/spring-demo --port=8080

# Pod anzeigen
oc get pods
oc get pod demo -o wide

# Pod-Details
oc describe pod demo

# Logs
oc logs demo
oc logs -f demo   # folgen

# In Pod einloggen
oc exec -it demo -- /bin/bash
oc exec -it demo -- sh
```

---

## Troubleshooting

```bash
# Pod-Status prüfen
oc get pods
# STATUS: Running | Pending | CrashLoopBackOff | ImagePullBackOff | Error

# Detaillierte Infos (Events sind wichtig!)
oc describe pod <podname>

# Logs des Containers
oc logs <podname>
oc logs <podname> --previous   # vorheriger Container (nach Crash)

# Events im Namespace
oc get events --sort-by='.lastTimestamp'

# Resource-Nutzung
oc adm top pods

# Netzwerk-Test aus dem Pod
oc exec -it <podname> -- curl http://other-service:8080/health
```

**Häufige Fehler:**

| Status | Ursache |
|---|---|
| `ImagePullBackOff` | Image nicht gefunden oder keine Pull-Berechtigung |
| `CrashLoopBackOff` | Container startet, crasht sofort (Logs prüfen!) |
| `Pending` | Kein Node verfügbar (Ressourcen?) oder fehlende PVC |
| `OOMKilled` | Container überschreitet Memory-Limit |
| `CreateContainerConfigError` | Secret oder ConfigMap fehlt |

---

## Grenzen von "Standalone Pods"

Ein direkter Pod (ohne Deployment) hat folgende Nachteile:

| Problem | Standalone Pod | Deployment |
|---|---|---|
| Node stirbt | Pod verschwindet ❌ | Neuer Pod wird gestartet ✅ |
| Container crasht | Nur limitierter Neustart ❌ | Automatischer Neustart ✅ |
| Skalierung | Manuell ❌ | `replicas: N` ✅ |
| Rolling Update | Nicht möglich ❌ | Automatisch ✅ |
| Rollback | Nicht möglich ❌ | `kubectl rollout undo` ✅ |

> **Fazit**: Standalone Pods nur für Debugging und Tests. Für Produktionsanwendungen immer ein **Deployment** verwenden!

---

## Deployment und ReplicaSet

```
Deployment
  └── ReplicaSet (v1)          ← alte Version
  └── ReplicaSet (v2, aktiv)   ← neue Version
        └── Pod
        └── Pod
        └── Pod
```

```bash
# Deployment erstellen
oc create deployment demo --image quay.io/ghilling/spring-demo --port 8080
# oder:
oc apply -f deployment.yaml

# Um die Manifest-Datei selber zu erzeugen:
oc create deployment demo --image quay.io/ghilling/spring-demo --port 8080 --dry-run=client -o yaml > demo-deployment.yaml

# Deployment anzeigen
oc get deployments
oc describe deployment demo

# Skalieren
oc scale deployment demo --replicas=3

# Rolling Update
oc set image deployment/demo demo=quay.io/ghilling/spring-demo:2.0

# Rollout-Status verfolgen
oc rollout status deployment/demo

# Rollback
oc rollout undo deployment/demo

# Rollout-Historie
oc rollout history deployment/demo
```

---

## Kubernetes-Manifeste für dieses Kapitel

Die YAML-Manifeste befinden sich in diesem Verzeichnis:

- `pod.yaml` – Standalone Pod (für Demo)
- `deployment.yaml` – Deployment mit ReplicaSet

### Hinweis: SecurityContext in OpenShift

OpenShift setzt über die **Security Context Constraints (SCC)** automatisch restriktive Sicherheitsrichtlinien für Pods und Container durch — ohne dass diese explizit im Manifest angegeben werden müssen. Die `restricted-v2` SCC (Standard in OpenShift) erzwingt beispielsweise automatisch:

- `allowPrivilegeEscalation: false`
- `runAsNonRoot: true`
- `seccompProfile.type: RuntimeDefault`
- `capabilities.drop: [ALL]`

Das bedeutet: In der Praxis müssen diese `securityContext`-Felder **nicht** explizit in Deployment-Manifesten aufgeführt werden — OpenShift kümmert sich selbstständig darum. In der `pod.yaml` dieses Kapitels sind sie zur Veranschaulichung dennoch eingetragen, um zu zeigen, was OpenShift im Hintergrund einstellt.

---

## Weiterführende Links

- [Kubernetes: Namespaces](https://kubernetes.io/docs/concepts/overview/working-with-objects/namespaces/)
- [Kubernetes: Pods](https://kubernetes.io/docs/concepts/workloads/pods/)
- [Kubernetes: Deployments](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)
- [OpenShift 4.22: Building Applications](https://docs.redhat.com/en/documentation/openshift_container_platform/4.22/html/building_applications/index)

Siehe auch [99-anhang/DOCUMENTATION.md](../99-anhang/DOCUMENTATION.md) für die vollständige Liste.
