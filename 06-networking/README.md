# Kapitel 6: Networking – Services, Routes & Port-Forward

## Services

Ein **Service** stellt eine stabile Netzwerkadresse für eine Gruppe von Pods bereit.

```
Client ──► Service (stabile IP/DNS) ──► Pod 1
                                    ──► Pod 2
                                    ──► Pod 3
```

### Service-Typen

| Typ | Beschreibung |
|---|---|
| `ClusterIP` | Nur intern erreichbar (Standard) |
| `NodePort` | Auf jedem Node-Port erreichbar |
| `LoadBalancer` | Externer Load Balancer (Cloud) |
| `ExternalName` | DNS-Alias auf externen Service |

### Service via Label-Selector

```yaml
apiVersion: v1
kind: Service
metadata:
  name: demo
spec:
  selector:
    app: demo         # Wählt alle Pods mit label app=demo
  ports:
    - port: 8080      # Service-Port
      targetPort: 8080 # Container-Port
```

```bash
# Services anzeigen
kubectl get services
kubectl get svc

# Service-Details
kubectl describe svc demo

# DNS innerhalb des Clusters
# Format: <service-name>.<namespace>.svc.cluster.local
curl http://demo.demo-seminar.svc.cluster.local:8080
# Kurzform (gleicher Namespace):
curl http://demo:8080
```

---

## Routes (OpenShift)

Eine **Route** macht einen Service von außerhalb des Clusters erreichbar.

```
Internet/Intranet ──► OpenShift Router ──► Route ──► Service ──► Pods
```

```bash
# Route erstellen (automatisch aus Service)
oc expose service demo

# Route anzeigen
oc get routes

# Route-URL ausgeben
oc get route demo -o jsonpath='{.spec.host}'

# Route aufrufen
curl http://$(oc get route demo -o jsonpath='{.spec.host}')
```

### TLS-Terminierung

```yaml
spec:
  tls:
    termination: edge        # TLS wird am Router beendet
    # termination: passthrough # TLS direkt zum Pod
    # termination: reencrypt  # TLS am Router + neuem TLS zum Pod
```

---

## Troubleshooting: Port-Forward

Port-Forward ermöglicht direkten Zugriff auf einen Pod/Service ohne Route:

```bash
# Direkt auf Pod zugreifen (für Debugging)
kubectl port-forward pod/demo 8080:8080
# → http://localhost:8080 leitet auf Container-Port 8080

# Auf Service zugreifen
kubectl port-forward svc/demo 8080:8080

# Im Hintergrund
kubectl port-forward svc/demo 8080:8080 &

# Stoppen
kill %1
```

### Netzwerk-Debugging


```bash
# Temporären Debug-Pod starten
kubectl run debug --image=quay.io/ghilling/ubi-tools:10-latest \
  --rm -it --restart=Never -- /bin/bash

# DNS-Test
nslookup demo.demo-seminar.svc.cluster.local

# HTTP-Test
curl http://demo:8080/
```

### Netzwerk-Policy prüfen (firewall ...)
kubectl get networkpolicies

---

## Manifeste in diesem Kapitel

- `service.yaml` – ClusterIP Service für die Demo-Anwendung
- `route.yaml` – OpenShift Route (HTTP + TLS edge)

---

## Weiterführende Links

- [Kubernetes: Services](https://kubernetes.io/docs/concepts/services-networking/service/)
- [Kubernetes: Ingress](https://kubernetes.io/docs/concepts/services-networking/ingress/)
- [OpenShift 4.22: Networking Overview](https://docs.redhat.com/en/documentation/openshift_container_platform/4.22/html/networking_overview/index)
- [OpenShift 4.22: Ingress and Load Balancing (Routes)](https://docs.redhat.com/en/documentation/openshift_container_platform/4.22/html/ingress_and_load_balancing/index)

Siehe auch [99-anhang/DOCUMENTATION.md](../99-anhang/DOCUMENTATION.md) für die vollständige Liste.
