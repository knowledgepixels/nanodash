# Custom Domain Support for Nanodash

Allow users to connect their own domain (e.g. tkuhn.org) so that it serves
their Nanodash profile page while keeping the custom domain visible in the
browser URL bar.

## Overview

A user who owns `tkuhn.org` should be able to point it at the Nanodash server
so that visitors to `https://tkuhn.org` see the user's profile page (the same
content as `/user?id=<their-iri>`) without any redirect.

## Architecture

Four layers are involved:

```
User's DNS (CNAME) --> Reverse proxy (TLS) --> Tomcat --> Wicket request mapper
```

### 1. Domain-to-user mapping

We need a way to store which custom domain maps to which user IRI. Options:

- **Nanopublication**: Users publish a nanopub asserting "my domain is
  tkuhn.org". Nanodash queries for this at runtime (with caching). Fits the
  existing data model naturally.
- **DNS TXT record**: The user adds a TXT record like
  `nanodash-user=https://orcid.org/0000-...` to their domain, and Nanodash does
  a DNS lookup on first request (then caches). This is the pattern GitHub Pages
  and Mastodon use for domain verification, and it avoids needing a central
  registry.
- **Configuration file**: A simple YAML/JSON file mapping domains to user IRIs.
  Simpler but outside the nanopub ecosystem.

These are not mutually exclusive -- DNS TXT could serve as verification even if
the mapping is stored as a nanopub.

### 2. Wicket request routing (application level)

In `WicketApplication.init()`, add a custom `IRequestMapper` or
`RequestCycleListener` that:

1. Inspects the `Host` header of incoming requests.
2. Looks up the host in the domain-to-user mapping (with a cache).
3. If a match is found, internally routes to the UserPage with the mapped user
   IRI, without a redirect (so the browser URL stays on the custom domain).
4. If no match, proceeds with normal Wicket routing.

This is estimated at roughly 50-100 lines of Java code. The key classes
involved:

- `WicketApplication.java` -- register the mapper/listener
- `UserPage.java` -- target page (already works with an `id` parameter)
- A new small class for the domain lookup + cache logic

### 3. TLS and reverse proxy (infrastructure level)

Each custom domain needs a valid HTTPS certificate. The recommended approach:

**Caddy** with on-demand TLS (simplest option):

```
{
    on_demand_tls {
        ask http://localhost:8080/api/check-domain
    }
}

:443 {
    tls {
        on_demand
    }
    reverse_proxy localhost:8080
}
```

Caddy automatically provisions Let's Encrypt certificates for any domain that
passes the `ask` check. Nanodash exposes a small verification endpoint
(`/api/check-domain?domain=...`) that returns HTTP 200 only for registered
domains. This prevents abuse of certificate issuance.

**Alternative**: nginx + certbot with a hook script, but this requires
significantly more operational complexity.

The reverse proxy must preserve the `Host` header when forwarding to Tomcat so
the Wicket request mapper can read it.

### 4. DNS setup (user side)

Users connecting a custom domain need to:

1. Add a **CNAME** record pointing their domain to the Nanodash server
   (e.g. `tkuhn.org CNAME nanodash.knowledgepixels.com`).
2. Optionally add a **TXT** record for verification
   (e.g. `nanodash-user=https://orcid.org/0000-0002-1267-0234`).

For apex domains (no subdomain), some DNS providers don't support CNAME. In
that case users can use an A record pointing to the server's IP, or use a
provider that supports CNAME flattening (Cloudflare, etc.).

## Effort estimate

| Layer                   | Effort       | Notes                                        |
|-------------------------|--------------|----------------------------------------------|
| Domain-user mapping     | Small        | Nanopub assertion and/or DNS TXT lookup       |
| Wicket request mapper   | Small        | ~50-100 lines in WicketApplication            |
| Verification endpoint   | Small        | Prevents certificate issuance abuse           |
| TLS / reverse proxy     | Medium       | Caddy with on-demand TLS is simplest path     |
| User-facing UI          | Small-medium | "Connect your domain" settings panel          |

## Open questions

- Should custom domains show only the profile, or also allow navigating to
  other Nanodash pages (spaces, search, etc.) under the custom domain?
- Should the mapping be stored as a nanopub, in DNS, or both?
- Is Caddy acceptable for the deployment, or does the existing infrastructure
  mandate nginx/Traefik?
- Do we need to support subdomains (e.g. `nanodash.tkuhn.org`) in addition to
  apex domains?
