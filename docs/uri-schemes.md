# Non-http(s) URI schemes

**Status:** ✅ Implemented ([#655](https://github.com/knowledgepixels/nanodash/issues/655))

Nanodash accepts and displays URIs in the schemes a nanopublication is allowed to reference, not
just `http(s)`. This is about **linking to** such resources from a nanopub. It is not about
publishing nanopublications over IPFS, and not about using DIDs as agent identifiers — Nanodash's
agent model is still ORCID-shaped, and that is a separate design question.

| Scheme | Example | What it identifies |
| --- | --- | --- |
| `ipfs:` | `ipfs://bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi` | Content-addressed data |
| `ipns:` | `ipns://example.org` | A mutable name pointing at content |
| `did:` | `did:plc:z72i7hdynmk6r22z27h6tvur` | A decentralized identifier |
| `at:` | `at://did:plc:z72i…/app.bsky.feed.post/3k2akmn5c7l2v` | An individual ATProto record |

The list is not Nanodash's own: it comes from `org.nanopub.UriSchemes.ALLOWED_SCHEMES` in
nanopub-java ([#138](https://github.com/Nanopublication/nanopub-java/issues/138)), so that Nanodash
and the nanopublication verifier agree on what counts as a URI. Adding a scheme there is enough to
make it work here.

## What this affects

- **Form input.** Placeholders that take a URI accept these schemes. A value in an unsupported
  scheme is rejected with *"IRI scheme not allowed here; use one of: …"*.
- **Rendering.** A value in one of these schemes is recognized as a URI and rendered as a link,
  rather than falling through to plain text. `Utils.isUriValue(String)` is the single
  discriminator for this; it replaced about thirty ad-hoc `matches("https?://.+")` tests.
- **HTML from views.** The sanitizer keeps `href`/`src` attributes in these schemes instead of
  stripping them.
- **Labels.** `did:` has no slashes and a CID is 59 characters, so neither shortens usefully by
  splitting on `/` and `#`. These get scheme-aware labels instead: `did:plc:z72i…tvur`,
  `ipfs:bafy…bzdi`. An AT-URI is labelled by its record key.

## Following a link

Nanodash has no page of its own to show for an `ipfs:`, `did:` or `at:` resource, so wherever such
a URI is rendered as a link — in nanopublication and form display (`IriItem`, `ReadonlyItem`) and
in query-result tables and lists (`NanodashLink`) — it points at an external web resolver rather
than at the Explore page, which has nothing to look up for one of these. The map is configurable,
with `$uri` expanding to the whole URI and `$rest` to the part after the scheme:

```yaml
# ~/.nanopub/nanodash-preferences.yml
uriResolvers: "ipfs=http://127.0.0.1:8080/ipfs/$rest,did=https://dev.uniresolver.io/1.0/identifiers/$uri"
```

or as an environment variable:

```
NANODASH_URI_RESOLVERS=ipfs=http://127.0.0.1:8080/ipfs/$rest,ipns=http://127.0.0.1:8080/ipns/$rest
```

The value **replaces** the defaults rather than adding to them, so setting it to an empty string
turns outbound resolution off and these URIs become non-links. The defaults are public third-party
services (`ipfs.io`, `dev.uniresolver.io`, `pdsls.dev`); an instance that runs its own IPFS node
should point the gateway at it.

## A note on persistence

`ipfs:` and `did:` are both offered here, but they sit at opposite ends of the same trade-off and
should not be treated as interchangeable "persistent identifiers":

- A **CID** is immutable — it is a hash of the content, so the reference cannot silently drift.
  But it is only retrievable while somebody pins the data; nothing guarantees availability.
- A **`did:plc:`** is reliably resolvable, but mutable: its keys can be rotated, and it depends on
  a semi-central directory operated by Bluesky. What it resolves to today is not necessarily what
  it resolved to last year.

Which of the two failure modes is acceptable depends on what the reference is for.
