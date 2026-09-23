# Kapitel 2: Kubernetes Basics

## Architektur

```
                    ┌─────────────────────────────────┐
                    │         Control Plane           │
                    │  ┌────────────────────────────┐ │
                    │  │        API Server          │ │
                    │  └──────────────┬─────────────┘ │
                    │  ┌─────────────┴─────────────-┐ │
                    │  │ etcd │Scheduler│Controlle  │ │
                    │  └────────────────────────────┘ │
                    └────────────────┬────────────────┘
                                     │
              ┌──────────────────────┼──────────────────────┐
              │                      │                      │
   ┌──────────┴───────┐   ┌──────────┴───────┐   ┌──────────┴───────┐
   │   Worker Node 1  │   │   Worker Node 2  │   │   Worker Node 3  │
   │  ┌─────────────┐ │   │  ┌─────────────┐ │   │  ┌─────────────┐ │
   │  │   kubelet   │ │   │  │   kubelet   │ │   │  │   kubelet   │ │
   │  │  kube-proxy │ │   │  │  kube-proxy │ │   │  │  kube-proxy │ │
   │  │  Container  │ │   │  │  Container  │ │   │  │  Container  │ │
   │  │  Runtime    │ │   │  │  Runtime    │ │   │  │  Runtime    │ │
   │  └─────────────┘ │   │  └─────────────┘ │   │  └─────────────┘ │
   └──────────────────┘   └──────────────────┘   └──────────────────┘
```

### Komponenten

**Control Plane:**
- **API Server**: Einziger Einstiegspunkt; REST-API; Authentifizierung & Autorisierung
- **etcd**: Verteilter Key-Value-Store; gesamter Cluster-Zustand
- **Scheduler**: Entscheidet, auf welchem Node ein Pod läuft
- **Controller Manager**: Führt Controller aus (ReplicaSet, Deployment, …)

**Worker Nodes:**
- **kubelet**: Agent auf jedem Node; kommuniziert mit API-Server; startet Container
- **kube-proxy**: Netzwerk-Regeln (Services → Pods)
- **Container Runtime**: z.B. CRI-O, containerd

---

## Der Control-Loop (Reconciliation)

Kubernetes arbeitet nach dem **Desired State** Prinzip:

```
  Desired State            Actual State
  (in etcd)                (im Cluster)
      │                         │
      └───────► Controller ◄────┘
                    │
                    ▼
             reconcile()
           "Wie erreiche ich
            Actual = Desired?"
```

**Beispiel:**
- Ich sage: "Ich möchte 3 Replicas"
- Kubernetes sieht: Es laufen nur 2
- Controller erstellt einen weiteren Pod → Actual = Desired

### Baukastensystem

Kubernetes besteht aus unabhängigen, kombinierbaren Ressourcentypen (beispielhaft):

```
Deployment
  └── ReplicaSet
        └── Pod

Service ──► Pod (via Label-Selector)
Route  ──► Service (OpenShift)
ConfigMap ──► Pod (als Env-Variable oder Volume)
Secret    ──► Pod (als Env-Variable oder Volume)
```

---

## Tools: kubectl und oc

### kubectl (Standard Kubernetes CLI)
```bash
# Cluster-Verbindung prüfen (Berechtigung!)
kubectl cluster-info

# Namespaces anzeigen (Berechtigung!)
kubectl get namespaces

# Pods anzeigen
kubectl get pods

# Pods im Namespace anzeigen
kubectl get pods -n mein-namespace

# Ausführliche Infos
kubectl describe pod mein-pod

# Logs
kubectl logs mein-pod

# YAML ausgeben
kubectl get pod mein-pod -o yaml

# Ressource erstellen/aktualisieren
kubectl apply -f manifest.yaml

# Ressource löschen
kubectl delete pod mein-pod
kubectl delete -f manifest.yaml
```

### oc (OpenShift CLI – Erweiterung von kubectl)
```bash
# Alles, was kubectl kann + OpenShift-Erweiterungen:

# Anmelden
oc login https://api.cluster.example.com:6443

# Aktuelles Projekt/Namespace anzeigen
oc project

# Projekt wechseln
oc project mein-projekt

# Neues Projekt erstellen
oc new-project mein-projekt

# Status anzeigen
oc status

# Route erstellen (OpenShift-spezifisch)
oc expose service mein-service
```

---

## Anmelden in OpenShift

Wichtig: Die Anmeldung über die Web-Console funktioniert immer. Für das ```oc login```-Command muss entweder username/password verfügbar sein, oder man verwendet die Option ```--web```, um über ein Browser-Fenster ein login-Token zu erhalten. Dies ist immer dann notwendig, wenn SSO-Verfahren mit 2-FA genutzt werden.

### Web-Console

1. Browser öffnen: `https://console.apps.cluster.example.com`
2. Mit Benutzername & Passwort anmelden
3. Oben rechts → "Copy login command"
4. Token kopieren → in Terminal einfügen

### Commandline

```bash
# Mit Benutzername/Passwort
oc login https://api.cluster.example.com:6443 -u developer -p password

# Mit Token (aus Web-Console kopiert)
oc login https://api.cluster.example.com:6443 --token=sha256~xxxxxxxxxxxx

# Verbindung prüfen
oc whoami
oc cluster-info

# kubeconfig anzeigen
cat ~/.kube/config
```

### kubeconfig

Die Konfigurationsdatei `~/.kube/config` enthält:
- Cluster-Adressen
- Benutzer-Tokens / Zertifikate
- Aktiven Context (welcher Cluster/User/Namespace)

```bash
# Alle Contexts anzeigen
kubectl config get-contexts

# Context wechseln
kubectl config use-context mein-context
```

---

## Weiterführende Links

- [Kubernetes: Komponenten des Clusters](https://kubernetes.io/docs/concepts/overview/components/)
- [Kubernetes: kubectl-Referenz](https://kubernetes.io/docs/reference/kubectl/)
- [OpenShift 4.22: CLI Tools](https://docs.redhat.com/en/documentation/openshift_container_platform/4.22/html/cli_tools/index)
- [OpenShift 4.22: API Overview](https://docs.redhat.com/en/documentation/openshift_container_platform/4.22/html/api_overview/index)

Siehe auch [99-anhang/DOCUMENTATION.md](../99-anhang/DOCUMENTATION.md) für die vollständige Liste.
