# Seminarunterlagen: Container und Kubernetes/OpenShift

Zweitägiges Seminar: Einführung in Container-Technologie und Kubernetes/OpenShift.

## Struktur

| Kapitel | Thema |
|---|---|
| [01-container](./01-container/) | Container-Grundlagen, Podman, Containerfile, Registries |
| [02-kubernetes-basics](./02-kubernetes-basics/) | Architektur, Control-Loop, kubectl/oc, OpenShift-Login |
| [03-openshift-vs-kubernetes](./03-openshift-vs-kubernetes/) | OpenShift vs. Vanilla Kubernetes |
| [04-pods-on-kubernetes](./04-pods-on-kubernetes/) | Namespaces, Pods, Deployments, Troubleshooting |
| [05-automation](./05-automation/) | Kubernetes-Manifeste, GitOps mit oc apply |
| [06-networking](./06-networking/) | Services, Routes, Port-Forward |
| [07-konfiguration](./07-konfiguration/) | Umgebungsvariablen, Secrets, ConfigMaps |
| [08-requests-limits](./08-requests-limits/) | Requests, Limits, LimitRange |
| [09-kustomize](./09-kustomize/) | Kustomize-Grundlagen, Base/Overlays, `oc apply -k` |
| [10-apply-strategies](./10-apply-strategies/) | Server-Side vs. Client-Side Apply, `--validate`, `--dry-run`, kuberc |
| [11-configmaps-secrets](./11-configmaps-secrets/) | ConfigMaps/Secrets vertieft, `configMapGenerator`, Sealed Secrets |
| [12-health-quotas](./12-health-quotas/) | Health-Checks (Probes), Quotas und LimitRanges vertieft |
| [13-rbac](./13-rbac/) | Kubernetes RBAC: Role, RoleBinding, ServiceAccount |
| [14-scc](./14-scc/) | OpenShift Security Context Constraints, feste User-IDs |
| [15-storage](./15-storage/) | Persistent Storage: PVCs, Provisioning, Resizing, Snapshots |
| [16-networkpolicies](./16-networkpolicies/) | Grundlagen der NetworkPolicies |

## Demo-Anwendung

Das Verzeichnis [01-container/demo-app](./01-container/demo-app/) enthält eine SpringBoot-Anwendung (Java 21),
die als roter Faden durch alle Kapitel verwendet wird.

Die Anwendung implementiert einen einfachen Webserver, der Dateien aus
`/var/demo/html` ausliefert (konfigurierbar via `DEMO_HTML_DIR` bzw. `demo.html-dir`).

### Bauen

```bash
cd demo-app
mvn package
# oder als Container:
podman build -t demo-app:1.0 .
```

### Lokal starten

```bash
# HTML-Verzeichnis vorbereiten
mkdir -p /tmp/html
echo '<html><body><h1>Hallo!</h1></body></html>' > /tmp/html/index.html

# App starten
java -Ddemo.html-dir=/tmp/html -jar target/demo-app-*.jar
# → http://localhost:8080
```

## Voraussetzungen

- OpenShift 4.21+ Cluster (kein Admin-Zugang erforderlich)
- `oc` CLI installiert
- Java 21 + Maven (für Demo-App)
- `podman` (für Container-Kapitel)

## Anmelden

```bash
oc login https://api.<cluster>.<domain>:6443
auf dem eigenen Cluster:
    oc new-project <project-name>
oder auf der Red Hat Sandbox den zugewiesenen Namespace verwenden.
```

## Alle Manifeste anwenden

```bash
oc apply -f 05-automation/namespace.yaml
oc apply -f 05-automation/deployment.yaml
oc apply -f 05-automation/service.yaml
oc apply -f 05-automation/route.yaml
```

## Weiterführende Dokumentation

Siehe [99-anhang/DOCUMENTATION.md](./99-anhang/DOCUMENTATION.md) für eine
kuratierte Liste externer Referenzen zur Kubernetes- und OpenShift-4.22-Dokumentation.