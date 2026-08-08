# DNS Sinkhole (Spring Boot, runs on a Termux'd Android phone)

## What's here

- `DnsUdpServer` — opens a raw UDP socket, parses incoming DNS queries with
  dnsjava, checks each domain against the blocklist, and either replies
  NXDOMAIN (blocked) or forwards to a real upstream DNS server (allowed).
- `BlocklistService` — loads a big static hosts-style list into memory
  (fast lookups), and checks the DB for user-added BLOCK/ALLOW overrides.
- `DomainRule` / `QueryLog` — JPA entities backed by an H2 file database, so
  your manual rules and query history survive restarts.
- `AdminController` — REST endpoints to view stats/logs and add block/allow
  rules (`/api/stats`, `/api/logs`, `/api/rules`, POST `/api/rules/block`,
  POST `/api/rules/allow`).

## Get the full blocklist

The repo ships with only 4 sample domains so it runs out of the box. Swap in
a real list before relying on this:

```bash
wget -O blocklist/hosts.txt https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts
```

## Build

You can build directly on the phone (slower, but no transfer step):

```bash
cd dns-sinkhole
mvn clean package -DskipTests
```

Or build on your PC (faster) and push the jar over:

```bash
mvn clean package -DskipTests
scp -P 8022 target/dns-sinkhole.jar <user>@<phone-ip>:~/dns-sinkhole/
```

## Run

```bash
java -jar target/dns-sinkhole.jar
```

You should see logs like:
```
Loaded 4 domains into the static blocklist
DNS sinkhole listening on UDP port 5300
```

Test the admin API from your PC:
```
http://<phone-ip>:8080/api/stats
```

## The port 53 problem (read this before pointing real devices at it)

Standard DNS clients (routers, phones, laptops) always query port 53 — there's
no "use port 5300 instead" option in normal OS network settings. But Termux
without root **can't bind to port 53** (it's privileged, <1024).

Two ways around it:

1. **Redirect 53 → 5300 at the OS level**, e.g. with `iptables` if you end up
   rooting the device, or a userspace redirector. This is the "correct" fix
   but adds setup complexity on unrooted Termux.
2. **Simplest for now: test from a device where you control the DNS port.**
   Some tools (like `dig` or `nslookup` on your PC) let you query a specific
   port directly, which is perfect for verifying the sinkhole logic works
   before you solve the network-wide redirect:

   ```bash
   nslookup doubleclick.net <phone-ip> -port=5300      # some nslookup builds
   dig @<phone-ip> -p 5300 doubleclick.net              # more reliable
   ```

   If `doubleclick.net` comes back NXDOMAIN and a normal domain (e.g.
   `example.com`) resolves fine, your sinkhole logic is working correctly —
   the only remaining problem is the port 53 redirect, which is a separate,
   solvable step once the core logic is proven.

## Next steps once this runs

- Get the port 53 redirect working so real devices can use it network-wide.
- Point your Funbox's DNS server field (if it has one) at the phone's static IP.
- Add a simple Thymeleaf dashboard on top of `/api/stats` and `/api/logs`.
