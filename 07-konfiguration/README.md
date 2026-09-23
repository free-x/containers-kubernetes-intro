# Kapitel 7: Konfiguration

## Umgebungsvariablen

Einfachste Methode zur Konfiguration von Containern:

```yaml
spec:
  containers:
    - name: demo
      image: quay.io/example/demo-app:1.0
      env:
        - name: DEMO_HTML_DIR
          value: /var/demo/html
        - name: SERVER_PORT
          value: "8080"
        - name: SPRING_PROFILES_ACTIVE
          value: production
```

```bash
# Umgebungsvariablen eines Pods anzeigen
kubectl exec demo -- env

# Umgebungsvariable in laufendem Deployment ändern
kubectl set env deployment/demo MY_VAR=new-value
```

---

## Secrets

**Secrets** speichern sensible Daten (Passwörter, Tokens, Zertifikate) base64-kodiert.

> ⚠️ Base64 ist **keine Verschlüsselung**! Für echte Verschlüsselung: Sealed Secrets, Vault, etc.

```bash
# Secret aus Literal-Werten erstellen
kubectl create secret generic db-credentials \
  --from-literal=username=admin \
  --from-literal=******

# Secret aus Datei
kubectl create secret generic tls-cert \
  --from-file=tls.crt=./cert.pem \
  --from-file=tls.key=./key.pem

# Secret anzeigen (base64-kodiert)
kubectl get secret db-credentials -o yaml

# Wert dekodieren
kubectl get secret db-credentials -o jsonpath='{.data.password}' | base64 -d
```

### Secret als Umgebungsvariable

```yaml
env:
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: db-credentials
        key: password
```

### Secret als Volume

```yaml
volumes:
  - name: db-creds
    secret:
      secretName: db-credentials
containers:
  - volumeMounts:
      - name: db-creds
        mountPath: /etc/secrets
        readOnly: true
```

---

## ConfigMaps

**ConfigMaps** speichern nicht-sensible Konfigurationsdaten.

```bash
# ConfigMap aus Literal-Werten
kubectl create configmap app-config \
  --from-literal=log.level=INFO \
  --from-literal=feature.flag=true

# ConfigMap aus Konfigurationsdatei
kubectl create configmap nginx-config \
  --from-file=nginx.conf

# ConfigMap anzeigen
kubectl get configmap app-config -o yaml
```

### ConfigMap als Umgebungsvariable

```yaml
env:
  - name: LOG_LEVEL
    valueFrom:
      configMapKeyRef:
        name: app-config
        key: log.level

# Alle Einträge als Env-Variablen
envFrom:
  - configMapRef:
      name: app-config
```

### ConfigMap als Konfigurationsdatei (Volume)

```yaml
volumes:
  - name: html-content
    configMap:
      name: html-files
containers:
  - volumeMounts:
      - name: html-content
        mountPath: /var/demo/html
        readOnly: true
```

---

## Konfigurationsdateien

Für unsere Demo-Anwendung (Dateien in `/var/demo/html`):

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: html-content
data:
  index.html: |
    <!DOCTYPE html>
    <html>
      <body>
        <h1>Hallo von Kubernetes!</h1>
        <p>Pod: ${HOSTNAME}</p>
      </body>
    </html>
```

```bash
# Konfiguration live ändern (Pod muss Volume-Updates unterstützen)
kubectl edit configmap html-content
# → Kubernetes aktualisiert das Volume nach ca. 1 Minute
```

---

## Manifeste in diesem Kapitel

- `configmap-html.yaml` – HTML-Inhalt als ConfigMap
- `secret-example.yaml` – Beispiel-Secret (Demo-Passwort)
- `deployment-with-config.yaml` – Deployment mit ConfigMap + Secret

---

## Weiterführende Links

- [Kubernetes: ConfigMaps](https://kubernetes.io/docs/concepts/configuration/configmap/)
- [Kubernetes: Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)
- [OpenShift 4.22: Building Applications](https://docs.redhat.com/en/documentation/openshift_container_platform/4.22/html/building_applications/index)

Siehe auch [99-anhang/DOCUMENTATION.md](../99-anhang/DOCUMENTATION.md) für die vollständige Liste.
