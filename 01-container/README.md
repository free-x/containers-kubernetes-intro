# Kapitel 1: Container

## Warum Container? Vergleich zu VMs

| Merkmal | Virtuelle Maschine | Container |
|---|---|---|
| Isolation | Vollständige OS-Isolation | Prozess-Isolation (Kernel shared) |
| Startzeit | Minuten | Sekunden / Millisekunden (nur die Anwendung) |
| Overhead | Hoch (komplettes Gast-OS) | Gering (nur Prozess + Libs) |
| Image-Größe | GB | MB |
| Portabilität | Eingeschränkt | Sehr hoch (OCI-Standard) |

### Schlüsselkonzepte
- **Namespaces** (Linux): Isolieren PID, Network, Mount, UTS, IPC, User
- **cgroups** (Linux): Begrenzen CPU, Speicher, I/O
- **Union Filesystems**: Layer-basierte Images (overlayfs)

```
┌─────────────────────────────────────────────┐
│               Container Engine              │
│  ┌───────────┐  ┌───────────┐  ┌──────────┐ │
│  │ Container │  │ Container │  │Container │ │
│  │ App + Libs│  │ App + Libs│  │App + Libs│ │
│  └───────────┘  └───────────┘  └──────────┘ │
│              Linux Kernel                   │
└─────────────────────────────────────────────┘
```

---

## Podman Basics

Podman ist eine daemonloser Container-Runtime (kein root nötig). Und stellt dabei dieselben Kommandos bereit wie docker:

```bash
# Image herunterladen
podman pull registry.access.redhat.com/ubi10/ubi-minimal:latest

# Container starten
podman run --rm registry.access.redhat.com/ubi10/ubi-minimal:latest cat /etc/os-release

# Container interaktiv
podman run --rm -it registry.access.redhat.com/ubi10/ubi-minimal:latest /bin/bash

# Laufende Container anzeigen
podman ps

# Alle Container (inkl. gestoppter)
podman ps -a

# Logs anzeigen
podman logs <container-id>

# Container stoppen
podman stop <container-id>

# Container entfernen
podman rm <container-id>

# Images anzeigen
podman images

# Image entfernen
podman rmi <image>

# Port-Mapping: Host-Port 8080 → Container-Port 8080
podman run -d -p 8080:8080 --name demo quay.io/example/demo-app:latest

# Container im Hintergrund starten
podman run -d --name demo quay.io/example/demo-app:latest

# In laufenden Container einloggen
podman exec -it demo /bin/bash
```

---

## Ephemeral – Was bedeutet das bei Containern?

Container sind **vergänglich** (ephemeral):
- Alles, was in einen Container geschrieben wird, geht beim Löschen des Containers verloren (was nicht verwunderlich ist.).
- Der Container-Prozess schreibt in das Container-eigene Dateisystem (oberste Layer).
- Beim `podman stop && podman rm` gehen alle Daten verloren.

```bash
# Beispiel: Daten gehen verloren
podman run --rm ubi10-minimal sh -c "echo hallo > /tmp/test.txt && cat /tmp/test.txt"
# → /tmp/test.txt existiert, während der Container existiert. Stoppen und erneutes Starten behält die Datei bei!

# Lösung: Volumes mounten für persistente Daten
podman run --rm -v /host/daten:/container/daten:Z ubi10-minimal ls /container/daten
# Das 'z' "repariert" die selinux Labels. 
```


### Konsequenzen für Anwendungen:
- Konfiguration **nicht** in Container einbauen → Umgebungsvariablen/Konfigurationsdateien.
- Logs auf **stdout/stderr** schreiben (nicht in Dateien): Diese werden von der Runtime "eingesammelt".
- Zustand (State) **extern** speichern: Host-Verzeichnisse (podman) oder PersistentVolumeClaims (Kubernetes)

---

## Container konfigurieren

### 1. Umgebungsvariablen (empfohlen für Geheimnisse & Konfiguration)
```bash
podman run -e MY_VAR=value -e DB_URL=jdbc:postgresql://host/db myimage
```

### 2. Dateien via Volume mounten
```bash
podman run -v /host/config.yml:/app/config.yml:ro,Z myimage
```

### 3. Kommandozeilenargumente
```bash
podman run myimage --server.port=9090
```

---

## Image Build – Containerfile

Ein **Containerfile** (= Dockerfile) beschreibt den Build-Prozess eines Images.

### Beispiel: SpringBoot-Anwendung

```dockerfile
# Schritt 1: Build-Stage (Maven)
FROM registry.access.redhat.com/ubi10/openjdk-21:latest AS builder

USER root
WORKDIR /build

COPY demo-app /build/

RUN mvn -q package -DskipTests
# Schritt 2: Runtime-Stage (ubi10-minimal + JRE aus Builder)
FROM registry.access.redhat.com/ubi10/ubi-minimal:latest
COPY --from=builder /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

# Nicht-Root-User (Security)
USER 1001

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Image bauen
podman build -t demo-app:1.0 .

# Image starten
podman run -p 8080:8080 demo-app:1.0
```

---

## Image Registries und Image Tags

### Registry
Eine Registry ist ein zentrales Repository für Container-Images.

| Registry | Beschreibung |
|---|---|
| `registry.access.redhat.com` | Red Hat offizielle Images |
| `quay.io` | Red Hat Quay (öffentlich + privat) |
| `docker.io` | Docker Hub |
| `ghcr.io` | GitHub Container Registry |

### Image-Namen und Tags

```
registry.example.com/namespace/image-name:tag
│──────────────────│ │───────│ │────────│ │──│
       Registry      Namespace   Image-Name  Tag
```

### Tags sind **nicht immutable**!

```bash
# Gleicher Tag, unterschiedlicher Inhalt möglich:
podman pull myimage:latest  # heute = Version 1.5
# nächste Woche:
podman pull myimage:latest  # latest = jetzt Version 2.0 !
```

**Besser:**:
- **Digest**: `myimage@sha256:abc123...` (unveränderbar)
- **Versionierte Tags**: `myimage:1.5.2` (Konvention, aber keine Garantie!)

```bash
# Image taggen
podman tag demo-app:1.0 quay.io/myorg/demo-app:1.0

# Image pushen
podman push quay.io/myorg/demo-app:1.0
```

---

## Weiterführende Links

- [Kubernetes: Container-Images](https://kubernetes.io/docs/concepts/containers/images/)
- [OpenShift 4.22: Images](https://docs.redhat.com/en/documentation/openshift_container_platform/4.22/html/images/index)

Siehe auch [99-anhang/DOCUMENTATION.md](../99-anhang/DOCUMENTATION.md) für die vollständige Liste.
