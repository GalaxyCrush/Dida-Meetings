# DAD2526 — DIDA Meetings Project (2025/2026)

## Requirements

The project requires the following dependencies:

* Java 22
* Maven 3.8.4
* Protoc 3.12

---

## Environment Setup

The repository includes a template script for setting up the environment. This script is designed for Linux/Ubuntu on x86 processors, but you may extend it for other architectures.

To run the setup script:

```
./setup_env.sh
```

All required packages are installed inside `INSTALL_DIR`. To activate the environment:

```
source INSTALL_DIR/env.sh
```

---

## Compiling

From the project root, run:

```
mvn clean install
```

### Contract Module Warning

The `contract` module requires different `pom.xml` files depending on CPU architecture:

* `arm-pom.xml` — ARM / Apple Silicon
* `intel-pom.xml` — Intel / Linux

Before the first compilation, copy the correct file and rename it:

```
cp contract/<your-architecture>-pom.xml contract/pom.xml
```

---

## Deployment Overview

The project assumes all modules run on the same machine. You must run five (5) servers for full functionality.

The system contains three main components:

* Servers
* Client App
* Console (Master)

---

## Servers

Run each server from the `server` directory:

```
mvn exec:java -Dexec.args="{port} {id} {scheduler} {max}"
```

Arguments:

* `{port}` — Base server port. Final port = `{port} + id`
* `{id}` — Server ID starting at 0
* `{scheduler}` — Scheduler (start with A)
* `{max}` — Maximum participants per meeting

---

## Client App

Run from the `app` directory:

```
mvn exec:java -Dexec.args="{id} {host} {port} {scheduler}"
```

Arguments:

* `{id}` — Client ID (starting at 1)
* `{host}` — Usually "localhost"
* `{port}` — Base server port
* `{scheduler}` — Scheduler (A to start)

Client commands:

```
help    # list commands
exit    # exit gracefully
```

---

## Console (Master)

Run from `consoleclient`:

```
mvn exec:java -Dexec.args="{host} {port} {scheduler}"
```

Console commands:

```
help
ballot <ballot_number> <server>
debug <mode> <replica_id>
exit
```

---

## Repository Structure

* contract/ → .proto definitions
* core/ → meeting & Paxos core logic
* configs/ → configuration & Vertical Paxos components
* util/ → RPC utils and shared helpers
* server/ → server implementation
* app/ → client application
* consoleclient/ → master console

---

## Project Summary

**Step 2 — Multi-Paxos**

* Concurrent Phase 2 proposals using thread pools
* Gap filling with NOOPs
* Leader runs Phase 1 only once per ballot period
* Pending / in-progress / processed request tracking

**Step 3 — Vertical Paxos II**

* Console acts as master
* Safe leader handover via state transfer
* New leader performs Phase 1 catch-up
* System only active after master sends "activate"

**Step 4 — Fast Topic Operations**

* Non-critical topic operations bypass Paxos
* Immediate execution when valid
* Pending operations attached to OPEN/ADD events
* Critical commands (OPEN/ADD/CLOSE) still ordered by Paxos

---

## Conclusion

This project implements:

* Multi-Paxos with concurrent proposals
* Vertical Paxos II with dynamic reconfiguration
* Fast topic operations for low-latency updates

It achieves efficiency, safe leadership changes, and modularity suitable for future extensions.

---

## Authors and Final Grade

* **João Pereira** — ist1112175
* **Martim Pereira** — ist1112272
* **João Tiago** — ist199986

**Final Grade:** 17.75 / 20