# Kapitel 12: Health-Checks, Quotas und LimitRanges

Kubernetes kann nur dann sinnvoll auf Probleme reagieren, wenn es weiß, ob
eine Anwendung überhaupt funktioniert. Dafür gibt es **Probes**. Gleichzeitig
schützen **ResourceQuotas** und **LimitRanges** (siehe bereits Kapitel 8) den
Cluster davor, dass einzelne Anwendungen zu viele Ressourcen beanspruchen. In
diesem Kapitel werden beide Themen kombiniert: Die Demo-App aus Kapitel 1
bekommt echte Health-Endpunkte, an denen sich das Verhalten der
unterschiedlichen Probe-Typen beobachten lässt.

## App-Erweiterung

Die Demo-App (`01-container/demo-app`) wurde um einen `HealthController`
erweitert (ab Version `1.1.0`, Image-Tag `demo-app:1.1`):

| Endpoint                  | Methode | Wirkung                                                              |
|----------------------------|---------|-----------------------------------------------------------------------|
| `/health`                  | GET     | `200 {"status":"UP"}` oder `503 {"status":"DOWN"}`                    |
| `/admin/health/toggle`     | POST    | Kippt den internen Health-Zustand (UP ↔ DOWN)                         |
| `/admin/restart`           | POST    | Beendet den Prozess nach kurzer Verzögerung (simulierter Absturz)     |

Der Health-Zustand wird nur im Arbeitsspeicher gehalten (`AtomicBoolean`) –
nach einem Neustart des Containers ist er wieder `UP`.

## Probe-Typen

Kubernetes kennt drei Arten von Probes, die alle dieselben Prüfmechanismen
(`httpGet`, `exec`, `tcpSocket`) verwenden können, aber unterschiedliche
Konsequenzen haben:

| Probe            | Frage                                      | Bei Fehlschlag                                              |
|-------------------|---------------------------------------------|--------------------------------------------------------------|
| `startupProbe`    | Ist die Anwendung überhaupt hochgefahren?   | Andere Probes warten, bis diese einmal erfolgreich war       |
| `readinessProbe`  | Kann der Pod gerade Traffic annehmen?       | Pod wird aus den Service-Endpoints entfernt (kein Neustart)   |
| `livenessProbe`   | Läuft die Anwendung noch korrekt?           | Kubelet startet den Container neu                             |

Prüfmechanismen:

- `httpGet`: HTTP-Request auf Pfad/Port, Status `2xx`/`3xx` gilt als Erfolg.
- `exec`: Ein Befehl wird im Container ausgeführt, Exit-Code `0` gilt als Erfolg.
- `tcpSocket`: Es wird nur geprüft, ob sich eine TCP-Verbindung öffnen lässt.

## Beispiel-Deployment

`deployment-with-probes.yaml` definiert `readinessProbe` und `livenessProbe`
auf `GET /health`, mit unterschiedlichen Schwellwerten:

```yaml
readinessProbe:
  httpGet:
    path: /health
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
  failureThreshold: 1
livenessProbe:
  httpGet:
    path: /health
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 10
  failureThreshold: 3
```

Die Readiness-Probe reagiert schnell (bereits nach einem Fehlschlag), die
Liveness-Probe toleriert mehr Fehlschläge, bevor sie eingreift – ein
klassisches Muster, um kurze Hänger nicht sofort mit einem Neustart zu
"bestrafen".

## Demo-Ablauf

```bash
oc apply -f deployment-with-probes.yaml
oc apply -f service.yaml

# Pod-Name ermitteln
oc get pods -l app=demo

# Health-Status umschalten
oc exec <pod> -- curl -s -X POST http://localhost:8080/admin/health/toggle
```

Beobachtbares Verhalten:

1. **Sofort:** Die Readiness-Probe schlägt beim nächsten Check fehl
   (`failureThreshold: 1`). Der Pod verschwindet aus den Endpoints des
   Service, erhält aber **keinen** Neustart:

   ```bash
   oc get endpoints demo
   oc describe pod <pod>   # Events: "Readiness probe failed"
   ```

2. **Nach einigen Sekunden:** Bleibt der Zustand `DOWN`, schlägt irgendwann
   auch die Liveness-Probe dreimal in Folge fehl. Erst dann startet das
   Kubelet den Container neu:

   ```bash
   oc get pods -l app=demo -w
   oc describe pod <pod>   # Events: "Liveness probe failed", "Killing container"
   ```

Zum Vergleich: `/admin/restart` beendet den Prozess sofort, ohne auf
irgendeine Probe-Schwelle zu warten:

```bash
oc exec <pod> -- curl -s -X POST http://localhost:8080/admin/restart
oc get pods -l app=demo -w
```

Der Unterschied macht deutlich, wofür Probes eigentlich da sind: Sie
erkennen Probleme, die die Anwendung selbst nicht meldet (z. B. ein
hängender Thread), während `/admin/restart` einen von außen sichtbaren,
sofortigen Absturz simuliert.

## Vertiefung: Quotas und LimitRanges (Rückblick Kapitel 8)

Kapitel 8 hat `LimitRange` (Standard-Requests/Limits pro Container) und
`ResourceQuota` (Obergrenze pro Namespace/Project) eingeführt. Hier folgen
ein paar Befehle, um deren Auswirkungen konkret zu beobachten:

```bash
# Aktuelle Auslastung der Quota im eigenen Project ansehen
oc describe quota

# Wie viele Ressourcen sind einem Pod tatsächlich zugewiesen?
oc describe pod <pod> | grep -A5 Limits

# Events nach Ressourcen-Problemen durchsuchen
oc get events --field-selector reason=OOMKilling
oc describe pod <pod>   # Abschnitt "Last State": "OOMKilled"
```

Wird das Memory-Limit eines Containers überschritten, beendet der Kernel den
Prozess mit `OOMKilled` – das Kubelet startet ihn danach neu (analog zu einer
fehlgeschlagenen Liveness-Probe). Wird nur das CPU-Limit erreicht, wird der
Container **gedrosselt** (Throttling), aber nicht beendet – das lässt sich in
`oc describe quota` als anhaltend hohe `cpu`-Auslastung erkennen, ohne dass
Pods neu starten.

`resourcequota.yaml` in diesem Kapitel zeigt beispielhaft eine
ResourceQuota, die die Summe aller Requests/Limits sowie die Anzahl Pods in
einem Project begrenzt (Cluster-Admin-Rechte nötig, um sie in einem fremden
Project anzulegen – im eigenen Project i. d. R. bereits von der
Cluster-Administration vorgegeben).

## Manifeste in diesem Kapitel

- `deployment-with-probes.yaml` – Deployment mit `readinessProbe` und `livenessProbe`
- `service.yaml` – Service für die Demo-App
- `resourcequota.yaml` – Beispiel-ResourceQuota (Referenz, siehe Kapitel 8)

---

## Weiterführende Links

- [Kubernetes: Liveness-, Readiness- und Startup-Probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)
- [Kubernetes: ResourceQuotas](https://kubernetes.io/docs/concepts/policy/resource-quotas/)
- [Kubernetes: LimitRange](https://kubernetes.io/docs/concepts/policy/limit-range/)
- [OpenShift 4.22: Nodes](https://docs.redhat.com/en/documentation/openshift_container_platform/4.22/html/nodes/index)

Siehe auch [99-anhang/DOCUMENTATION.md](../99-anhang/DOCUMENTATION.md) für die vollständige Liste.
