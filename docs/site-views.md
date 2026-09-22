# Contained "site" views of a space

**Status:** 🚧 In progress — [#692](https://github.com/knowledgepixels/nanodash/issues/692)

Let a Nanodash instance present itself as the website of one space: the space's page is the
home page, the space is the navigation context of everything, the site's name and logo take
the place of Nanodash's, and links to what lies outside the space are plain links rather
than links into the general Nanodash interface. Such a site should look like a domain-specific
website that happens to run on nanopublications.

The feature comes in two stages. **Stage A** (implemented) makes a whole deployment the site
of one space: one instance, one space, one domain. **Stage B** (proposed) serves several such
sites from one instance under `/site/<shorthand>` and, later, from custom domains.

## Stage A: single-space site mode

### Configuration

Site mode is switched on by naming the space, and is off — with no change in behaviour at
all — while it is not:

| Environment variable            | Preferences key     | Meaning                                                                  |
| ------------------------------- | ------------------- | ------------------------------------------------------------------------ |
| `NANODASH_SITE_SPACE`           | `siteSpace`         | The space IRI. Setting it makes the instance a site.                     |
| `NANODASH_SITE_NAME`            | `siteName`          | The site's name; the space's label when unset.                           |
| `NANODASH_SITE_LOGO`            | `siteLogo`          | Image URL or `data:` URI; the space's `schema:image` picture when unset. |
| `NANODASH_SITE_CSS`             | `siteCss`           | A stylesheet loaded after Nanodash's own on every page.                  |
| `NANODASH_SITE_EXTERNAL_LINKS`  | `siteExternalLinks` | `false` keeps links to outside resources inside this instance.           |

`SiteMode` (`com.knowledgepixels.nanodash`) is the one place that reads these; everything
else asks it.

### What changes in site mode

- **Home page.** `/` serves `SiteHomePage`, a `SpacePage` for the configured space (it
  inherits the space page's markup and everything on it: tabs, About, title menu). Wicket
  asks `WicketApplication.getHomePage()` on every request, so the class follows the
  configuration. While the space repository does not know the space yet — the first seconds
  after a start — `/` shows `SiteLoadingPage`, which sends the browser back after a moment.
  Nanodash's own `HomePage` is not served; the places that sent the user there
  (`NavigationContext.homePageClass()` / `homePageRef()`) now send them to the site's home.
- **Navigation context.** `NavigationContext.getContextId(params)` answers the site's space
  when the URL names no `context`. Everything that reads the context follows: the
  post-publish forward, the title bar's back-link, `~~SPACE~~` template prefixes, the
  refresh of the context resource after publishing. A context named in the URL still wins,
  so a sub-space's page stays that sub-space's.
- **Breadcrumbs.** The site's space tops every path, and the name at the top left already
  says so, so `TitleBar` leaves its crumb out (`withoutSiteHome`): a sub-space's page shows
  `Sub > …`, not `Site > Sub > …`. A path topped by another space keeps its top.
- **Branding.** The title bar shows the site's logo (where it has one) and name in place of
  the Nanodash logo; the logo is the page icon; page titles end in the site's name instead of
  `| nanodash`; the metadata that link previews read (`og:site_name`, the default
  description) speaks of the site. The optional stylesheet is the hook for colours and the
  like — `style.css` hardcodes its colours, and a site restyles by overriding rules, not by
  setting variables.
- **Links.** `NanodashLink` renders a link to anything outside the site as a plain link to the
  resource itself (`SiteMode.rendersExternally`). Part of the site are: the space and anything
  under its IRI or its alternative IRIs, its sub-spaces at any depth, the resources they
  maintain and what lies in those resources' namespaces, nanopublications (and IRIs minted
  inside them), and users. The rule can be switched off with `NANODASH_SITE_EXTERNAL_LINKS`.
- **What a view lists stays inside.** A part of the site is often recognisable only from its
  defining nanopublication (a class the site has a view display for, say), which is not
  affordable per link. So an IRI listed by one of the site's views (a query-result cell,
  `NanodashLink`'s view-context constructor) is linked inside the site whatever it is, as
  outside site mode, and `ExplorePage` settles it on click: a part of the site forwards to its
  part page as always, and anything else is sent on to itself with a 303. The site therefore
  never shows a page about a foreign resource. The price is that a foreign IRI in a view — a
  DOI in a "source" column — looks like an inside link and leaves the site after one hop.
  IRIs met anywhere else (a nanopublication's statements) follow the render-time rule above.
- **Outbound links look and behave differently.** In-app links are relative, so an absolute
  `http(s)` address is one that leaves the site: `style.css` marks such links with a ↗ (keyed
  on `body.site`, which `NanodashPage` sets; buttons and image links excepted), and
  `nanodash.js` opens them in a new tab (decided at click time on the document, so
  AJAX-loaded content is covered; same-origin absolute addresses stay in the tab). No
  Java-side marker is needed for either.

### Publishing from a site

Publishing is not bound to the space. The publish form uses the context for template
prefixes and for the post-publish forward only; no statement ties the published
nanopublication to the space, and `/publish` is reachable as on any instance. A site is a
*presentation* scope, not a publication scope: a nanopublication that the space's views do
not match is published all the same, and simply not listed. Two things keep that from
stranding the user:

- Nanopublications always count as part of the site, so the "successfully published" message
  in the title bar links to the new nanopublication *within* the site, whatever it is about.
- The forward to a declared space or maintained resource ([#594](https://github.com/knowledgepixels/nanodash/issues/594))
  is fenced: a nanopublication declaring a space or resource outside the site forwards to the
  site's home instead, with the same message. Users' pages are also part of every site, and a
  user's Explore tab lists their publications whatever space they concern.

Site mode fences *link rendering*, not navigation: a user can still reach any space through
explore or search, they are just not handed links there.

### Files

`SiteMode`, `NanodashPreferences` (settings), `WicketApplication` (home page class, mount of
the loading page), `page/SiteHomePage`, `page/SiteLoadingPage`, `page/SpacePage`
(`resolveSpace` fallback, title), `page/NanodashPage` (site name, icon, title suffix,
stylesheet), `NavigationContext` (context fallback, home page, forward fence),
`component/TitleBar` (logo and name), `component/NanodashLink` (link policy), plus the
pages that send the user home (`ErrorPage`, `NanopubNotFoundPage`, `PreviewPage`,
`ProfileItem`). Tests: `SiteModeTest`, `NavigationContextSiteModeTest`,
`page/SiteModePagesTest`.

### Not in Stage A

Pretty URLs beyond the home page: navigating within the site leads to the ordinary
`/space?id=…`, `/explore?id=…` URLs. Restricting *which* templates can be published from a
site (that is template governance, see
[template-identity-and-governance](template-identity-and-governance.md)). Hiding
network-wide results from views that show them.

## Stage B: several sites on one instance

Serve sites under `/site/<shorthand>`, where the shorthand stands for a space, and later
from custom domains.

- **Site registry.** Where `<shorthand>` → space comes from: the preferences file first; a
  shorthand declared by the space in a nanopublication later.
- **URL prefix.** One `IRequestMapper` decorator around Wicket's root mapper: strip
  `/site/<shorthand>` on the way in, keep the site in the request cycle, put the prefix back
  on every URL Wicket renders. Every existing bookmarkable link then stays inside the site
  without call-site changes. Shared resources (`style.css`, images, webjars) must not get the
  prefix; `Utils.absolutePageUrl` must (the RDF content-negotiation redirects and calendar
  feeds depend on it); `/mcp` is outside the Wicket filter.
- **Per-site branding and context** become per-request rather than per-instance: `SiteMode`
  answers from the request cycle instead of the preferences.
- **Custom domains.** The same mapper keyed on the `Host` header, a `check-domain` endpoint
  for on-demand TLS, and per-domain OAuth redirect URIs; see
  [custom-domains](custom-domains.md).
