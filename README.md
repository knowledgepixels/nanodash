[![Maven Test & Coverage upload](https://github.com/knowledgepixels/nanodash/actions/workflows/maven-test.yml/badge.svg)](https://github.com/knowledgepixels/nanodash/actions/workflows/maven-test.yml)
[![Coverage Status](https://coveralls.io/repos/github/knowledgepixels/nanodash/badge.svg?branch=master)](https://coveralls.io/github/knowledgepixels/nanodash?branch=master)
[![semantic-release: angular](https://img.shields.io/badge/semantic--release-angular-e10079?logo=semantic-release)](https://github.com/semantic-release/semantic-release)

![logo](nanodash.png)

Nanodash was previously called Nanobench. Nanodash is a client to browse and publish nanopublications.

### Online Instances

You can use Nanodash by login in via ORCID in one of the online instances:

- https://nanodash.petapico.org
- https://nanodash.knowledgepixels.com
- https://nanodash.net

### Local Installation

To use Nanodash locally, see the [installation instructions with Docker](INSTALL-with-Docker.md).


### Analytics

Nanodash supports privacy-friendly, cookie-free analytics via [Umami](https://umami.is/).
Analytics are disabled by default and can be enabled by pointing Nanodash at an existing Umami instance.

**Docker / environment variables** (recommended for server deployments) — add to your `docker-compose.override.yml`:

```yaml
services:
  nanodash:
    environment:
      - NANODASH_UMAMI_SCRIPT_URL=https://your.umami.instance/script.js
      - NANODASH_UMAMI_WEBSITE_ID=your-umami-website-id
```

**YAML preferences file** (convenient for local development) — add to `~/.nanopub/nanodash-preferences.yml`:

```yaml
umamiScriptUrl: https://your.umami.instance/script.js
umamiWebsiteId: your-umami-website-id
```

When configured, Nanodash logs `Umami analytics configured: <url>` at startup to confirm the setting was picked up.

### Screenshot

This screenshot of Nanodash is showing its publishing feature with auto-complete-powered forms generated from semantic
templates:

![screenshot of Nanodash showing the publishing feature](screenshot.png)

### Tutorials

Check out this [short Nanodash demo video](https://youtu.be/exJ_8p584cE).

[This hands-on demo](https://knowledgepixels.com/nanopub-demo/) (slightly outdated) gives you a quick hands-on introduction into nanopublications via
the Nanodash interface, including a video: [Hands-on demo video](https://youtu.be/_wmXHgC706I)

### Contributing

Contributions are welcome! Please see the [CONTRIBUTING](CONTRIBUTING.md) file for details.

### License

Copyright (C) 2022-2026 Knowledge Pixels

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see https://www.gnu.org/licenses/.
