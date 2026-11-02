# RootAdBlocker (Spring Boot, running on a rooted Android device via Termux)

## What this is

A network-wide DNS-based ad/tracker blocker built from scratch in Java +
Spring Boot, running on a rooted Android device. Devices on the network point
their DNS settings at the host device's static IP, the app intercepts DNS
lookups, checks each domain against a blocklist, and either kills the request
(ad/tracker) or forwards it upstream (everything else).

Any spare rooted Android phone or tablet works as the host — this was
originally built and tested on an old low-spec Samsung j2 pro to prove that even
years-old, underpowered hardware is enough to run this reliably.

## What's here

- `DnsUdpServer`: opens a raw UDP socket, parses incoming DNS queries with
  dnsjava, checks each domain against the blocklist, and either replies
  NXDOMAIN (blocked) or forwards to a real upstream DNS server (allowed). If
  the upstream lookup fails it replies SERVFAIL so clients don't hang.
- `BlocklistService`: loads a big static hosts-style list into memory and
  keeps the user-added BLOCK/ALLOW rules cached in memory too, so a DNS lookup
  never touches the database.
- `DomainRuleService`: validates and saves BLOCK/ALLOW rules, and keeps the
  in-memory cache in sync.
- `QueryLogWriter`: buffers query log entries and writes them to the database
  in batches every 2 seconds. Entries older than the retention period are
  deleted daily at 04:00.
- `DomainRule` / `QueryLog`: JPA entities backed by an H2 file database, so
  manual rules and query history survive restarts.
- `AdminController`: REST API.

### API

| Method | Path | What it does |
|---|---|---|
| GET | `/api/stats` | Blocklist size, total and blocked query counts |
| GET | `/api/logs` | Last 100 queries |
| GET | `/api/rules` | All BLOCK/ALLOW rules |
| POST | `/api/rules/block` | Body `{"domain": "ads.example.com"}`. Adds or updates a BLOCK rule |
| POST | `/api/rules/allow` | Body `{"domain": "ads.example.com"}`. Adds or updates an ALLOW rule |
| DELETE | `/api/rules/{id}` | Deletes a rule (404 if it doesn't exist) |
| GET | `/api/blocklist/check?domain=...` | Whether a domain is currently blocked |
| POST | `/api/blocklist/reload` | Re-reads the `blocklist/` directory without a restart |

Errors come back as JSON problem details with a 400 or 404 status.

### Configuration

All settings live in `src/main/resources/application.properties` and can be
overridden on the command line, e.g. `java -jar target/dns-sinkhole.jar --sinkhole.listen-port=53`.

| Property | Default | Meaning |
|---|---|---|
| `sinkhole.listen-port` | `5300` | UDP port for DNS. Use `53` on a rooted device |
| `sinkhole.upstream-dns` | `1.1.1.1` | Upstream resolver for allowed domains |
| `sinkhole.upstream-timeout` | `3s` | Upstream lookup timeout |
| `sinkhole.worker-threads` | `4` | Threads handling DNS queries |
| `sinkhole.queue-capacity` | `1000` | Pending queries before new ones are dropped |
| `sinkhole.blocklist.dir` | `blocklist` | Directory (or single file) of hosts-style lists |
| `sinkhole.query-log.retention-days` | `7` | Days of query history to keep. `0` keeps everything |
| `sinkhole.query-log.buffer-capacity` | `10000` | Log entries held in memory between writes |

## Why rooting is necessary

Standard DNS clients (routers, phones, laptops) always query **port 53** —
there's no OS-level setting anywhere to tell a device "use a different port."
Port 53 is privileged (<1024), and Termux without root cannot bind to it.

Two options existed: run on a high port (5300) and try to redirect 53→5300 at
the network level, or root the device and bind port 53 directly. If the device isn't a
daily driver, rooting will be the simpler, more direct fix — no
router cooperation or userspace redirect tooling required. Once rooted, this
project runs the DNS listener straight on port 53
(`sinkhole.listen-port=53` in `application.properties`), so devices need
nothing beyond a normal DNS server IP setting.

*(A Raspberry Pi is the more conventional choice here since it's Linux with
root by default and skips this whole detour — using an old Android phone
instead was the point of this project, 
however can be run anywhere and isn't tied to any specific device.)*

## Setup

### 1. Root the device and install Termux

Root access is required for step 4 below. Rooting steps vary by exact devices
model/bootloader — not covered here.

Install Termux from **F-Droid**, could be from the Play Store (however Play Store wasn't used to download the app, so try this at your own risk):
https://f-droid.org/en/packages/com.termux/

### 2. Initial Termux setup

```bash
pkg update && pkg upgrade
termux-setup-storage
pkg install openssh openjdk-17 maven git wget
```

### 3. Set up SSH so you can work from your PC instead of the device screen

Inside Termux:
```bash
sshd
passwd        # set a password when prompted
whoami        # note your username, e.g. u0_a158
ifconfig      # note the devices's IP under wlan0
```

From your PC (same Wi-Fi):
```bash
ssh -p 8022 <username>@<device-ip>
```
e.g. `ssh -p 8022 u0_a158@192.168.1.10`, then enter the password you set.
You should land on the Termux welcome banner:
Welcome to Termux!

Docs: https://termux.dev/docs
Donate: https://termux.dev/donate
Community: https://termux.dev/community


Note: `sshd` needs to be re-run each time Termux is fully closed and reopened
— it doesn't persist across restarts by default.

### 4. Give the device a fixed local IP

So the IP you SSH/connect to never changes, set a **DHCP/static lease** on
your router for the device's Wi-Fi MAC address (Settings → About device →
Status → Wi-Fi MAC address).

### 5. Get the project onto the device

Clone it directly on the device (simplest — no manual file transfer needed):
```bash
git clone https://github.com/Daniel3886/RootDNS.git
cd RootDNS
```

Download one or more real blocklists into the `blocklist/` directory (the repo
ships with a handful of sample domains only, not a full list). Every regular
file in that directory is loaded at startup:
```bash
wget -O blocklist/hosts.txt https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts
```

### 6. Build

Directly on the device (slower, no transfer step):
```bash
mvn clean package -DskipTests
```

Or build on your PC (faster) and copy just the jar over instead:
```bash
# on your PC, inside the project folder:
mvn clean package -DskipTests
scp -P 8022 target/dns-sinkhole.jar <username>@<device-ip>:~/RootDNS/target/
```

### 7. Run

```bash
java -jar target/dns-sinkhole.jar
```

Expected output:

Loaded <N> domains into the static blocklist
DNS sinkhole listening on UDP port 53


Keep it alive in the background: run `termux-wake-lock` first, disable
battery optimization for Termux in Android settings, and keep the phone
plugged in.

### 8. Point a device at it

On the client device (phone/laptop), set its DNS server manually to the selected devices
static IP. It'll now resolve normally for everything except domains on the
blocklist, which will fail to load.

Check it's working from your PC:

```bash
http://<phone-ip>:8080/api/stats
```
