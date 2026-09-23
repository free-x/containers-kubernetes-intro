# Kapitel 3: OpenShift vs. "Vanilla Kubernetes"

## Warum gibt es kein "Vanilla Kubernetes"?

Kubernetes selbst ist ein **Framework**, kein fertiges Produkt. Es fehlen viele Komponenten für den Produktionseinsatz:

| Funktion | Kubernetes (upstream) | OpenShift 4.x |
|---|---|---|
| Container Runtime | (wählbar) | CRI-O (inklusive) |
| Netzwerk-Plugin (CNI) | (wählbar)  | OVN-Kubernetes (inklusive) |
| Storage (CSI) | (wählbar)  | (wählbar)  |
| Image Registry | nicht enthalten | integrierte Registry (optional) |
| Ingress/Routes | (wählbar) | HAProxy Router (inklusive) |
| Monitoring | (wählbar) | Prometheus + Grafana (inklusive) |
| Logging | nicht enthalten | LokiStack / OpenShift Logging |
| CI/CD | nicht enthalten | OpenShift Pipelines (Tekton) |
| Service Mesh | (wählbar) | OpenShift Service Mesh (Istio) |
| Operator Framework | nicht enthalten | OperatorHub + OLM (inklusive) |
| Security (SCC) | PSA (eingeschränkt) | Security Context Constraints |
| Web-Console | Headlamp, wird gerade neu entwickelt | OpenShift Web-Console |
| Auth / LDAP | externe Lösung nötig | OAuth Server inklusive |
| Updates/Upgrades | manuell | automatisiert via CVO |

## Die wichtigsten Ergänzungen von OpenShift

### Security Context Constraints (SCC)

OpenShift hat ein erweitertes Sicherheitsmodell:

```yaml
# restricted-v2 SCC (Standard für normale Nutzer):
# - kein root (UID 0)
# - keine privilegierten Container
# - nur lesbare Root-Filesysteme
# - keine hostPath-Volumes
# - readOnlyRootFilesystem empfohlen
```

```bash
# Welche SCC hat mein Pod?
oc get pod mein-pod -o yaml | grep scc

# Welche SCCs gibt es?
oc get scc
```

### Routes (OpenShift-spezifisch)

```yaml
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: demo
spec:
  to:
    kind: Service
    name: demo
  port:
    targetPort: 8080
  tls:
    termination: edge
```

### BuildConfigs / ImageStreams

OpenShift kann Images direkt im Cluster bauen:

```bash
# Source-to-Image (S2I)
oc new-app java~https://github.com/example/myapp.git

# ImageStream: Abstraktion über Image-Tags
oc get imagestreams
oc get istag
```

## Fazit
"Vanilla Kubernetes" ist eine Basis-Infrastruktur. In der Praxis nutzt man immer eine Distribution oder einen managed Service, der alle fehlenden Komponenten integriert und supportet.

---

## Weiterführende Links

- [OpenShift 4.22: Architecture](https://docs.redhat.com/en/documentation/openshift_container_platform/4.22/html/architecture/index)
- [OpenShift 4.22: Building Applications (S2I, BuildConfigs, ImageStreams)](https://docs.redhat.com/en/documentation/openshift_container_platform/4.22/html/building_applications/index)
- [OpenShift 4.22: Security and Compliance (SCCs)](https://docs.redhat.com/en/documentation/openshift_container_platform/4.22/html/security_and_compliance/index)
- [Kubernetes: Übersicht der Konzepte](https://kubernetes.io/docs/concepts/overview/)

Siehe auch [99-anhang/DOCUMENTATION.md](../99-anhang/DOCUMENTATION.md) für die vollständige Liste.
