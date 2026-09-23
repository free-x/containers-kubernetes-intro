# Kapitel 15: Persistent Storage

Container-Dateisysteme sind flüchtig: Wird ein Container neu gestartet, sind
alle darin geschriebenen Daten weg. Für alles, was das überdauern soll
(Datenbanken, Uploads, ...), braucht es **PersistentVolumes (PV)** und
**PersistentVolumeClaims (PVC)**.

## Zusammenspiel PVC / PV / StorageClass

```
┌────────────┐        ┌───────────────┐        ┌─────────────────┐
│    Pod     │──uses──▶│      PVC      │──bound──▶│        PV        │
│ (volumeMount)│       │ (Anfrage:     │        │ (tatsächlicher   │
│            │        │  Größe, Modus)│        │  Speicher)       │
└────────────┘        └───────┬───────┘        └────────▲─────────┘
                               │ dynamisch provisioniert  │
                               ▼                          │
                       ┌───────────────┐                  │
                       │ StorageClass  │──────────────────┘
                       │ (Auto-       │
                       │  Provisioner)│
                       └───────────────┘
```

- Eine **PVC** ist eine *Anfrage* nach Speicher (Größe, Access Mode) –
  gestellt vom Anwendungsentwickler, ohne Details über die tatsächliche
  Storage-Infrastruktur zu kennen.
- Ein **PV** ist der *tatsächliche* Speicher (z. B. ein Cloud-Volume).
- Eine **StorageClass** definiert einen Auto-Provisioner, der bei Bedarf
  automatisch ein passendes PV erzeugt und mit der PVC verbindet – ohne
  StorageClass müsste ein Admin PVs manuell vorab anlegen.

```bash
oc get storageclass
# Die Default-StorageClass ist an der Annotation erkennbar:
#   storageclass.kubernetes.io/is-default-class: "true"
```

Wird in der PVC kein `storageClassName` angegeben (wie in `pvc.yaml`), wird
automatisch die Default-StorageClass des Clusters verwendet.

## Access Modes

| Modus  | Bedeutung                                              |
|--------|-----------------------------------------------------------|
| RWO    | ReadWriteOnce – von einem einzelnen Node lesend/schreibend nutzbar |
| ROX    | ReadOnlyMany – von mehreren Nodes gleichzeitig, nur lesend         |
| RWX    | ReadWriteMany – von mehreren Nodes gleichzeitig, lesend/schreibend |

Welche Modi ein Volume unterstützt, hängt vom zugrunde liegenden
Storage-Typ ab – klassischer Block-Storage (z. B. die meisten
Cloud-Disks) unterstützt in der Regel nur RWO.

## PV-Lifecycle

```
Available ──(Bind an PVC)──▶ Bound ──(PVC gelöscht)──▶ Released ──▶ (je nach Reclaim Policy)
```

Die **Reclaim Policy** des PV bestimmt, was nach dem Löschen der PVC
passiert:

- `Delete` – PV und zugrunde liegender Speicher werden automatisch gelöscht
  (Standard bei den meisten dynamisch provisionierten StorageClasses).
- `Retain` – PV bleibt im Zustand `Released` erhalten, muss manuell
  aufgeräumt oder wiederverwendet werden (schützt vor versehentlichem
  Datenverlust).

```bash
oc get pv
oc get pvc demo-data -o jsonpath='{.spec.volumeName}'
```

## Resizing

Unterstützt die StorageClass `allowVolumeExpansion: true`, lässt sich eine
PVC **vergrößern**, ohne sie neu anzulegen:

```bash
oc patch pvc demo-data -p '{"spec":{"resources":{"requests":{"storage":"2Gi"}}}}'
oc get pvc demo-data -w
```

Wichtig: Eine Verkleinerung ist nicht möglich – nur eine Vergrößerung.
Je nach Storage-Backend muss der Pod, der die PVC verwendet, für die
Dateisystem-Vergrößerung neu gestartet werden.

## Snapshots

`VolumeSnapshot`/`VolumeSnapshotClass` erlauben es, einen Zeitpunkt-Zustand
eines Volumes zu sichern und später als Basis für ein neues Volume zu
verwenden. `volumesnapshot.yaml` zeigt das Prinzip:

```yaml
spec:
  volumeSnapshotClassName: csi-snapshot-class
  source:
    persistentVolumeClaimName: demo-data
```

> Snapshots setzen voraus, dass der CSI-Treiber des Clusters
> Snapshot-Funktionalität unterstützt und eine passende
> `VolumeSnapshotClass` existiert. Das ist nicht auf jedem Cluster der Fall
> – prüfen mit `oc get volumesnapshotclass`. Ist keine vorhanden, lässt sich
> dieser Teil des Kapitels nur konzeptionell nachvollziehen.

## Beispiel-Deployment mit PVC

`deployment-with-pvc.yaml` mountet die PVC aus `pvc.yaml` unter
`/var/demo/data`. Die Update-Strategie ist bewusst auf `Recreate` gesetzt,
da die meisten RWO-Volumes nicht gleichzeitig von zwei Pods (altem und
neuem, wie bei einem Rolling-Update) gemountet werden können.

```bash
oc apply -f pvc.yaml
oc apply -f deployment-with-pvc.yaml
oc get pvc demo-data
```

## Manifeste in diesem Kapitel

- `pvc.yaml` – PersistentVolumeClaim ohne feste StorageClass (nutzt Cluster-Default)
- `deployment-with-pvc.yaml` – Deployment, das die PVC mountet
- `volumesnapshot.yaml` – Referenz-Snapshot (abhängig vom CSI-Treiber des Clusters)

---

## Weiterführende Links

- [Kubernetes: Persistent Volumes](https://kubernetes.io/docs/concepts/storage/persistent-volumes/)
- [Kubernetes: StorageClasses](https://kubernetes.io/docs/concepts/storage/storage-classes/)
- [Kubernetes: Volume-Snapshots](https://kubernetes.io/docs/concepts/storage/volume-snapshots/)
- [OpenShift 4.22: Storage](https://docs.redhat.com/en/documentation/openshift_container_platform/4.22/html/storage/index)

Siehe auch [99-anhang/DOCUMENTATION.md](../99-anhang/DOCUMENTATION.md) für die vollständige Liste.
