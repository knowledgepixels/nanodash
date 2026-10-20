# Being found by search engines

**Status:** ✅ Implemented ([#804](https://github.com/knowledgepixels/nanodash/issues/804),
[#168](https://github.com/knowledgepixels/nanodash/issues/168))

Nanodash pages describe themselves well — a meta description, a canonical URL, Open Graph and
Twitter card tags ([#704](https://github.com/knowledgepixels/nanodash/issues/704)), RDF by
content negotiation and embedded JSON-LD
([#710](https://github.com/knowledgepixels/nanodash/issues/710)). None of it reached a search
engine, for four reasons that had to be dealt with separately.

## What was in the way

**The site was closed.** All three public hosts served `Disallow: /` from nginx. That alone is
enough; nothing else in this document matters until it is lifted, and it is the one part that
lives outside this repository (see [Deployment](#deployment)).

**Every URL carried a session id.** The servlet container appends `;jsessionid=…` for any
client that sends no cookie, which is every crawler. A crawl therefore discovered a fresh set
of unique URLs each time, and every link on the page handed out a live session identifier.
`web.xml` now sets `<tracking-mode>COOKIE</tracking-mode>`: sessions live in a cookie or not
at all. A client that refuses cookies can no longer hold a session, so it cannot sign in or
publish — which is what `/privacy` already says a session cookie is needed for.

**The same page had many URLs.** Links carry `context`, `part`, `part-label` and `label` to
record the route the visitor took, and `NavigationContext`'s two fallback behaviours add them
to links that lack them. A nanopublication was also reachable under its bare artifact code
(what w3id.org forwards to) and under its full IRI. So one subject was many URLs, each
claiming to be canonical.

**Crawling was expensive.** Every view on a page goes through `ApiCache`, and a cold one
queues a query across the fleet. Crawlers have overloaded this fleet before (see
`kpxl-admin/incidents.md`), which is why it was closed in the first place.

## How a crawler is treated

`CrawlerRequest` answers, once per request, whether the user agent names a crawler. It is a
declaration rather than proof, which is fine because of what the answer is used for: it only
ever makes a page **cheaper** to serve, and never shows a crawler anything a visitor would not
see. A visitor mistaken for a crawler sees a page without its not-yet-loaded sections; a
crawler that hides gets the ordinary treatment.

On that answer:

| Where | Ordinarily | For a crawler |
| --- | --- | --- |
| `WicketApplication` | the page is rendered into a buffer and the client redirected to the page instance's URL (`/?3`) | rendered straight into a 200 at the clean URL: no redirect, no session bound, no page buffered into one |

The same one-pass rendering is given to a client that **arrives at a page-instance URL
holding no session**. It has to be: with sessions in a cookie alone, there is no stored
instance to find, so the page would be rendered and redirected to again — a client that
refuses cookies would bounce between `/` and `/?0` forever. Its *first* request is still
buffered and redirected, which is what sets the cookie, and what a browser needs: rendering
that one in a single pass would leave the page with no instance for its own Ajax calls to
reach, and they would fail with a 500. The rest of the table applies to crawlers only.

| Where | Ordinarily | For a crawler |
| --- | --- | --- |
| `NanodashPage.markIfBrowserReload` | `Cache-Control: no-cache` means the reader hit reload, so the page's queries run again | ignored: from a crawler the header means "not from your HTTP cache", not "re-run everything" |
| `ApiCache.retrieve*Async` | a cold or stale entry queues a refresh | whatever is cached is returned and **nothing is queued**, however old it is: a stale answer is real content served for free |
| `ApiResultComponent` / `LazyContentPanel` | a section not ready yet shows a spinner and polls | the section is left out — a crawler never comes back for the follow-up request |
| `NavigationContext`, `QueryResult.linkNavParams`, `NanodashLink` | links pick up the page's context and part | left off, so one target is one URL |

A crawled page therefore costs no queries at all. That is what makes it affordable to open
`/explore` — the nanopublication landing pages — which is what
[#168](https://github.com/knowledgepixels/nanodash/issues/168) asks for.

The one exception is a link to a **resource part**, which cannot resolve without the resource
maintaining it: there `context` is half the identity rather than a record of the route, so
those links keep it (and `ResourcePartPage`'s canonical URL keeps it too). Action links to
`/publish` keep theirs as well; they are `Disallow`ed, so a crawler never follows them.

Forwards still work as before: `RestartResponseException(Class, params)` and
`setResponsePage(Class, params)` always redirect, so a crawler asking for `/explore?id=<space>`
is still sent on to `/space?id=…`, which is exactly the consolidation wanted. One-pass
rendering is *not* switched on globally, because the form paths (`PublishForm`, `PreviewPage`,
`ChannelPage`) redirect conditionally and would leave the browser on a URL that submits again
when reloaded. Crawlers do not submit forms.

## One URL per subject

`NanodashPage.getCanonicalParameters()` builds the canonical URL (and `og:url`). By default it
keeps the page's parameters minus the navigation-only ones. Three pages need more than the
default:

- **`SiteHomePage`** — none. It extends `SpacePage` but is served at `/`, so the site's front
  page must canonicalise to the root URL.
- **`ResourcePartPage`** — `id` **and** `context`. There, `context` names the maintaining
  resource: it is half of what the page is about, not a record of the route.
- **`ExplorePage`** — `id` alone, spelled as the nanopublication's own resolved IRI, so that
  the artifact-code form and the full-IRI form collapse onto one URL.

The rule, stated once: **canonical = mount path + the identifying parameter(s), plus `tab`
where it is not the default.**

## Keeping pages out of the index

Two mechanisms, doing different jobs, and which one a page gets is a decision:

- **`Disallow` in robots.txt** stops the page being fetched at all. That is what the expensive
  paths want, and the forms and downloads that only mean something inside a session.
- **`<meta name="robots" content="noindex, follow">`**, from
  `NanodashPage.getRobotsDirective()`, keeps a page out of the index while leaving it
  crawlable. `follow`, not `nofollow`: these pages are how a crawler reaches the ones that do
  belong in the index.

They are not interchangeable. A crawler has to fetch a page to read its `noindex`, so
disallowing a page that is already indexed has the opposite of the intended effect — it stays
listed, without a description. Cheap pages that should not be indexed are therefore left
crawlable and say so themselves.

Pages override `isIndexable()` to opt out. The base also opts out any tab other than the main
one, which otherwise puts three near-copies of every space, user and resource into the index —
something robots.txt cannot express, since it cannot match a query parameter.

`DownloadRdfPage`, `DownloadDocPage` and `CalendarFeedPage` never render their markup, so a
meta tag could not reach a crawler: they send `X-Robots-Tag: noindex` instead.

## robots.txt and the sitemap

Both are generated (`RobotsPage`, `SitemapPage`) rather than shipped as files, because the
`Sitemap:` line has to be an absolute URL and this application runs on several hostnames, on
single-space sites, and on instances anyone can run. An instance with no configured
`NANODASH_WEBSITE_URL` names no sitemap at all rather than one at whatever address the request
arrived on, which behind a reverse proxy is how the proxy reached the container.

The sitemap is built from the snapshots already held in memory — `SpaceRepository`,
`MaintainedResourceRepository` and the approved users — so building it runs no query and a
crawler cannot turn it into load. It is held for an hour on top of the cache headers.

Nanopublication landing pages are deliberately **not** listed. There are tens of millions of
them, each costing a fetch from the registry to render; listing them would need hundreds of
sitemap files whose only effect would be to invite exactly the crawl this fleet cannot afford.
They are found the way they should be — by being reachable, canonical and described — from the
pages that are listed.

## Link previews

`getMetaImageUrl()` gives `og:image` and `twitter:image`; the default is
`images/social-card.png`, the Nanodash wordmark at the 1200×630 shape preview cards expect. A
space, user or maintained resource with a profile picture offers that instead, and then asks
for the small card (`hasWideMetaImage()`), because a square picture stretched into the wide
shape looks worse than the card it suits.

A profile picture may be an SVG carried in the page as a `data:` URI. That works in a browser
and nowhere else — a preview card is drawn by a service that fetches the picture over HTTP,
and most of them cannot draw SVG — so `sharableImageUrl` drops those rather than advertising
them.

## Deployment

The nginx side lives in `kpxl-admin` (`conf/nginx.conf`) and has to be deployed by hand:

1. Deploy `conf/nginx.conf` to all three hosts per `server-setup.md` § "Nginx and Let's
   Encrypt". **Look at what each host currently serves at `/robots.txt` first** — the hosts
   have drifted from the repository, and one may have been re-closed by hand during an
   incident.
2. `sudo nginx -t && sudo nginx -s reload`, then check `curl https://<host>/robots.txt`.
3. Verify all three hosts in Google Search Console (DNS TXT, so no code is needed), submit
   each `https://<host>/sitemap.xml`, and request indexing of `/`.
4. Keep the `search_engine_global` and `explore_global` rate-limit zones through the first
   crawl cycle.

Note that all three hosts serve the same content and each canonicalises to itself, so search
engines will see three sites with the same pages and split ranking signals between them.
Expect "Duplicate, Google chose a different canonical" in Search Console; it is the known cost
of the decision, not a fault. Consolidating later is a config change
(`NANODASH_WEBSITE_URL`) plus a re-crawl, not a code change.
