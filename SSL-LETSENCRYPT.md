# SSL Certificates with Let's Encrypt

## How Let's Encrypt Works in This Project

VARS Quickstart uses Let's Encrypt's **HTTP-01 challenge** to verify domain ownership. When you run `./varsq letsencrypt`, it:

1. Starts nginx (which serves port 80)
2. Runs certbot in a Docker container
3. Let's Encrypt reaches out over the internet to `http://your-domain/.well-known/acme-challenge/` to verify ownership
4. If successful, issues a 90-day trusted certificate
5. Copies the certificate to the paths nginx expects

This works great when your server is publicly reachable. On an intranet, step 3 fails — Let's Encrypt cannot reach an internal server.

---

## Lab Setup Instructions (Server with Public DNS)

If your server has a registered domain name and port 80 is accessible from the internet:

**Prerequisites:**

- A fully-qualified domain name (FQDN) like `vars.yourinstitution.org` — not an IP address
- DNS A record pointing that name to your server's public IP
- Port 80 open in any firewall between the internet and your server (only needed during certificate issuance/renewal)
- Port 443 open for ongoing HTTPS traffic
- Docker installed and running

**Steps:**

```bash
# 1. Configure your environment for your named host
./varsq configure etc/env/your-target.env

# 2. Obtain the Let's Encrypt certificate
./varsq letsencrypt --email admin@yourinstitution.org

# 3. Start all services
./varsq start

# 4. Verify everything is working
./varsq status
```

**Certificate renewal** — Let's Encrypt certificates expire after 90 days. Run this on a cron schedule (weekly is safe; certbot only contacts Let's Encrypt when within 30 days of expiry):

```bash
./varsq renewcert
./varsq docker restart nginx
```

Example weekly cron entry:

```
0 3 * * 0  cd /path/to/vars-quickstart && ./varsq renewcert && ./varsq docker restart nginx
```

---

## Intranet Setup: What the IT Department Needs to Do

When the VARS server is on an intranet, Let's Encrypt cannot reach it to complete the HTTP-01 challenge. The solution is the **DNS-01 challenge**, which proves domain ownership by adding a DNS TXT record instead of serving a file over HTTP. The server never needs to be publicly reachable.

### What DNS-01 Requires

| Requirement | Notes |
|---|---|
| A registered domain name | Must be a real domain (e.g., `yourinstitution.org`), not just an internal hostname like `varsserver` |
| Control over public DNS for that domain | IT must be able to add TXT records to the public DNS zone |
| A subdomain for the VARS server | e.g., `vars.yourinstitution.org` |
| Internal DNS (split-horizon) | So intranet clients resolve `vars.yourinstitution.org` to the **internal** IP |

> **Why split-horizon DNS?** The public DNS record points to your public IP (or is just used for the certificate challenge and doesn't route traffic at all). Internal clients need to resolve the same name to the server's internal IP address. Split-horizon DNS serves different answers to internal vs. external resolvers.

### Step-by-Step IT Instructions

**Step 1: Register or delegate a subdomain**

The VARS server needs a real domain name. Work with whoever manages your institution's DNS (e.g., `yourinstitution.org`) to create:

```
vars.yourinstitution.org  →  (internal IP, e.g., 10.0.1.50)
```

Add this A record to your **internal DNS server** so lab computers can resolve it. You do _not_ need to publish the internal IP in public DNS.

**Step 2: Obtain the certificate using DNS-01 challenge (manual method)**

On the VARS server, run certbot manually once to obtain the initial certificate. This does not require certbot to be installed — use Docker:

```bash
# Set your domain and email
DOMAIN="vars.yourinstitution.org"
EMAIL="admin@yourinstitution.org"
LETSENCRYPT_DIR="./temp/ssl/letsencrypt"

mkdir -p "$LETSENCRYPT_DIR"

docker run --rm -it \
  -v "${LETSENCRYPT_DIR}:/etc/letsencrypt" \
  certbot/certbot certonly \
  --manual \
  --preferred-challenges dns \
  --email "$EMAIL" \
  --agree-tos \
  --no-eff-email \
  -d "$DOMAIN"
```

Certbot will pause and display something like:

```
Please deploy a DNS TXT record under the name:
_acme-challenge.vars.yourinstitution.org

with the following value:
aBcDeFgHiJkLmNoPqRsTuVwXyZ_some_token_here

Press Enter to continue...
```

**Step 3: IT adds the DNS TXT record**

Before pressing Enter, IT must log in to the **public DNS provider** for `yourinstitution.org` and add:

```
Record type:  TXT
Name:         _acme-challenge.vars.yourinstitution.org
Value:        aBcDeFgHiJkLmNoPqRsTuVwXyZ_some_token_here
TTL:          60 (low TTL so it propagates quickly)
```

Wait for DNS propagation (typically 1–5 minutes with a low TTL). You can verify it has propagated with:

```bash
dig TXT _acme-challenge.vars.yourinstitution.org @8.8.8.8
```

Once the TXT record appears, press Enter in the certbot session. Let's Encrypt will query public DNS for the TXT record — it never contacts your server.

**Step 4: Install the certificate into VARS**

After certbot succeeds, the certificate is at `./temp/ssl/letsencrypt/live/vars.yourinstitution.org/`. Copy it to where VARS expects it (matching your `SSL_CERT_FILE` and `SSL_KEY_FILE` environment variables):

```bash
# Adjust paths to match your SSL_CERT_FILE and SSL_KEY_FILE values
cp ./temp/ssl/letsencrypt/live/vars.yourinstitution.org/fullchain.pem ./temp/ssl/server.crt
cp ./temp/ssl/letsencrypt/live/vars.yourinstitution.org/privkey.pem   ./temp/ssl/server.key
```

Then start VARS normally:

```bash
./varsq start
```

**Step 5: Renewal (every ~60 days)**

Certificates expire after 90 days. Repeat Steps 2–4 before expiry. Because the manual DNS method is interactive, IT needs to be available to add the TXT record each time.

To check the current expiry date:

```bash
docker run --rm \
  -v "$(pwd)/temp/ssl/letsencrypt:/etc/letsencrypt" \
  certbot/certbot certificates
```

---

### Automated DNS-01 Renewal (Recommended for IT)

Manual renewal every 60 days is burdensome. Most DNS providers have APIs that certbot can use to add TXT records automatically. If your institution uses one of the supported providers, IT can set up fully automated renewal:

| DNS Provider | Certbot Plugin |
|---|---|
| Cloudflare | `certbot-dns-cloudflare` |
| AWS Route 53 | `certbot-dns-route53` |
| Azure DNS | `certbot-dns-azure` |
| Google Cloud DNS | `certbot-dns-google` |
| Infoblox, Palo Alto, etc. | Community plugins available |

Example with Cloudflare — IT creates a credentials file `cloudflare.ini` with a scoped API token:

```ini
# cloudflare.ini
dns_cloudflare_api_token = YOUR_CLOUDFLARE_API_TOKEN
```

```bash
chmod 600 cloudflare.ini

docker run --rm \
  -v "$(pwd)/temp/ssl/letsencrypt:/etc/letsencrypt" \
  -v "$(pwd)/cloudflare.ini:/cloudflare.ini:ro" \
  certbot/certbot:latest certonly \
  --dns-cloudflare \
  --dns-cloudflare-credentials /cloudflare.ini \
  --email admin@yourinstitution.org \
  --agree-tos \
  --no-eff-email \
  -d vars.yourinstitution.org
```

Renewal then becomes fully automated and can be added to cron with no IT interaction.

---

## Alternative: Self-Signed Certificate (No IT Involvement)

If IT cannot provide a domain name or DNS control, use the built-in self-signed certificate option instead:

```bash
./varsq mkcert
./varsq start
```

This generates a local CA and certificate trusted only within your lab. Users will need to install the exported CA certificate on each client machine:

```bash
./varsq exportca
```

That command prints platform-specific instructions for Windows, macOS, and Linux. The trade-off is that each client computer requires a one-time setup step.

---

## Decision Guide for IT

```
Does the server have a publicly registered domain name?
├── No  → Use ./varsq mkcert (self-signed) + distribute CA to clients via ./varsq exportca
└── Yes → Can port 80 reach the server from the internet during cert issuance?
          ├── Yes → Use ./varsq letsencrypt (simplest option)
          └── No  → Use DNS-01 challenge (manual or automated via DNS provider API)
                    Does your DNS provider have a certbot plugin?
                    ├── Yes → Set up automated DNS-01 renewal
                    └── No  → Use manual DNS-01 (IT adds TXT record every ~60 days)
```
