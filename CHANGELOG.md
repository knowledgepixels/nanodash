## [4.13.1](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.13.0...nanodash-4.13.1) (2026-02-09)

### Bug Fixes

* Fix broken connector configs ([4a6625f](https://github.com/knowledgepixels/nanodash/commit/4a6625fd679d0240de0ce67a858f16a0808a5f99))

### General maintenance

* setting next snapshot version [skip ci] ([e18f9e8](https://github.com/knowledgepixels/nanodash/commit/e18f9e84b2f8f70ca6eec5798c436855df8726a8))
* Update CSS version links ([32696eb](https://github.com/knowledgepixels/nanodash/commit/32696ebfdd0cf9c6b0bd0c100171559fe0c2f325))

### Style improvements

* Not showing 'contested identity' for now ([fd0d317](https://github.com/knowledgepixels/nanodash/commit/fd0d317526cf085c6ccde548432b643d604840c7))

## [4.13.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.12.0...nanodash-4.13.0) (2026-02-08)

### Features

* add consent checkbox to preview page, require only for publishing ([16572f5](https://github.com/knowledgepixels/nanodash/commit/16572f527e7ac8e37bcac4a19b7ceb4ea2a97c16))
* add Preview button to review nanopubs before publishing ([6df8cc4](https://github.com/knowledgepixels/nanodash/commit/6df8cc4aae03ce729ad61108c448f87f4a519c43))

### Bug Fixes

* hide action menu on preview page ([9bb7c7b](https://github.com/knowledgepixels/nanodash/commit/9bb7c7b26763b2a982b6f67d74746ae6ac47e260))
* **QueryResultNanopubSet:** remove unnecessary class attribute from markup ([3a34f41](https://github.com/knowledgepixels/nanodash/commit/3a34f41c52987b86e5a1efabf5ec3636b918b430))
* **QueryResultNanopubSet:** remove unnecessary class attribute from markup in HomePage and QueryResultNanopubSet ([1e80198](https://github.com/knowledgepixels/nanodash/commit/1e801983712a6cd919a93db1921bf93e53be830b))
* **style:** add padding to flex-container and adjust border for nanopub-item component ([845e011](https://github.com/knowledgepixels/nanodash/commit/845e011c52afa23106aabfacc5c8936282d985ce))
* **style:** use light style for Preview and Discard buttons ([febb69d](https://github.com/knowledgepixels/nanodash/commit/febb69d65dd284fc2d6d578a5ce39fe851fc4eba))

### General maintenance

* setting next snapshot version [skip ci] ([93b4d79](https://github.com/knowledgepixels/nanodash/commit/93b4d7966979ba2cc808573cec49db3c9e67b677))
* **SourceNanopub:** add component for linking to source nanopublication ([8f8b731](https://github.com/knowledgepixels/nanodash/commit/8f8b7315cb4be983833972361a11c97ac254068f))
* Update CSS and logo.svg links ([77f7017](https://github.com/knowledgepixels/nanodash/commit/77f7017b23306ab5e163c9bbce2ece3cfda49797))

### Style improvements

* add headings to publish/preview forms and reorder buttons ([1e4db9a](https://github.com/knowledgepixels/nanodash/commit/1e4db9a45bc67d6aecba63ff4caec64fe1bf44be))

### Refactoring

* update nanopub link components to use SourceNanopub ([28f6dc6](https://github.com/knowledgepixels/nanodash/commit/28f6dc626a4cfd190c01840611f3c9a8838206d4))
* use nanopub URI as preview page id instead of random UUID ([028214c](https://github.com/knowledgepixels/nanodash/commit/028214c754e64623ca58e28dc73caba1004c5fed))

## [4.12.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.11.0...nanodash-4.12.0) (2026-02-04)

### Features

* Add filter fields to views to User and Query pages ([27b6730](https://github.com/knowledgepixels/nanodash/commit/27b673025ed832f6d8dfe6bb6d720650eb45ea1d))
* Consistently show software agent icon in lists ([06e0232](https://github.com/knowledgepixels/nanodash/commit/06e02325ddc252d82ef28e887dd0ddf0685ad106))
* **KPXL_TERMS:** add ITEM_LIST_VIEW to supported view types ([9eed027](https://github.com/knowledgepixels/nanodash/commit/9eed027a4e46a9df4a5cba99faff12699456d7da))
* **KPXL_TERMS:** add NANOPUB_SET_VIEW constant for Nanopublication Set view type ([f47d1e0](https://github.com/knowledgepixels/nanodash/commit/f47d1e0bb534ab4868b1dbdb4f16edee5c668c0d))
* **NanodashSession:** change default NanopubResults view mode from GRID to LIST ([71f0268](https://github.com/knowledgepixels/nanodash/commit/71f026870b6194380cf155abe7bc89dca20e82ec))
* **ProfiledResource:** add default view display for latest nanopubs in user profiles ([789d83f](https://github.com/knowledgepixels/nanodash/commit/789d83fc661893d886862058c86d240b88ad7b66))
* **QueryResultNanopubSet:** add component for displaying NanopubResults list/grid as a View ([329351c](https://github.com/knowledgepixels/nanodash/commit/329351c9825c3bd25d52e333802b79e422f31c48))
* Recognize software agents ([c609f7e](https://github.com/knowledgepixels/nanodash/commit/c609f7e50670b541c3ff39312261a6373d93639e))
* Redirect directly to context page after publishing ([7f397c6](https://github.com/knowledgepixels/nanodash/commit/7f397c665375597e30495f76345d2baf4484fa62))
* Redirect to part-pages after publication ([cade9f0](https://github.com/knowledgepixels/nanodash/commit/cade9f0dbe9488061048efe7d4527641888f1394))
* Remove Spaces component on user page ([cacdb17](https://github.com/knowledgepixels/nanodash/commit/cacdb17a4ea369cfc8682de74e976849078f14eb))
* **ResourceView:** add support for NANOPUB_SET_VIEW and improve view type validation ([5642e0a](https://github.com/knowledgepixels/nanodash/commit/5642e0a0e1c6ddd31a8b83dd4c33dc5a23505cea))
* Show human users and software agents separately ([122edca](https://github.com/knowledgepixels/nanodash/commit/122edca1dcf992aabb0ff1e3dd8a6e795a61a011))
* Support part-pages for user profiles ([e0ab8d2](https://github.com/knowledgepixels/nanodash/commit/e0ab8d2ce237519443d711595124c73c673b9d49))
* **ViewList:** implement NANOPUB_SET_VIEW handling in view list ([4eb0ef5](https://github.com/knowledgepixels/nanodash/commit/4eb0ef577551deb7b8d73d77ff50dd504e54615e))

### Dependency updates

* **core-deps:** update com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer dependency to v20260102.1 ([880a6a1](https://github.com/knowledgepixels/nanodash/commit/880a6a18cce15cc7f06c2006b5c8829325666771))
* **core-deps:** Update nanopub dependency ([a2302f0](https://github.com/knowledgepixels/nanodash/commit/a2302f0b568cd5e9f1c3604f28bb7d06132a603b))
* **deps:** Update commons-exec dependency ([6485954](https://github.com/knowledgepixels/nanodash/commit/64859546807d3f9f9dd08c83a07d15548f489e06))
* **deps:** Update commons-text dependency ([5d739b7](https://github.com/knowledgepixels/nanodash/commit/5d739b70d86da084d213bb4344681a8afc94afad))
* **deps:** update dependency maven to v3.9.12 ([377933d](https://github.com/knowledgepixels/nanodash/commit/377933d0dfbd9e0db19e1bd9f9ee52f86d766ee0))
* **deps:** Update jackson-dataformat-yaml dependency ([3c5fae1](https://github.com/knowledgepixels/nanodash/commit/3c5fae1a7ac116bd5c624ec04700299aef903eb5))
* **deps:** Update junit-jupiter dependency ([fcf9afb](https://github.com/knowledgepixels/nanodash/commit/fcf9afb0a8fb9d31b72cb26e55d28e6f24285000))
* **deps:** Update mockito-core dependency ([aa28102](https://github.com/knowledgepixels/nanodash/commit/aa28102955b2d18e1adf1877499cc9009a8227eb))

### Bug Fixes

* Compile errors when run through script resolved ([66c29af](https://github.com/knowledgepixels/nanodash/commit/66c29af36dfaa14d64dff920a9befecbb2e4c958))
* **GrlcQuery:** update `get` method to use `QueryApiAccess` for query ID retrieval from QueryRef ([af612e7](https://github.com/knowledgepixels/nanodash/commit/af612e7f5d6263c568ab36173796eab9fd5fcf3c))
* **HomePage:** add missing 'view' section and update HTML structure ([a615fa0](https://github.com/knowledgepixels/nanodash/commit/a615fa0e1adb52760b4b93b057d6a88ecdd9fab7))
* **ListPage:** update params injection to accommodate new query syntax and enforce single 'type' parameter ([7d2d5b9](https://github.com/knowledgepixels/nanodash/commit/7d2d5b9eedd45d63345d1c0438f779cfc59c05cc))
* Only local default values in repeated statements get numbers ([ea2474c](https://github.com/knowledgepixels/nanodash/commit/ea2474ce2769eb26a36ef4f53a17ad5f402b78be))
* remove external container from `UserPage` views list ([c56aaeb](https://github.com/knowledgepixels/nanodash/commit/c56aaeb5416c1cbc3654027fde7001d4b8901d76))
* Removing and re-adding repeated statements works with defaults ([8e5f775](https://github.com/knowledgepixels/nanodash/commit/8e5f7757dcc0a6175ba6bcb9b633d73518f687b7))
* **style:** remove unnecessary margin from table border styling ([ac4451e](https://github.com/knowledgepixels/nanodash/commit/ac4451e403c13fccfb81e7148b70be5b20d3224d))
* Transform some remaining query names to query IDs ([4d88674](https://github.com/knowledgepixels/nanodash/commit/4d88674234cc1c8aa7538d527dacbd3c88c4c378))
* User pages AJAX loading works again (without 'wicket:container') ([8552497](https://github.com/knowledgepixels/nanodash/commit/8552497fca8dec1df8f45d91c9d502507bc65452))
* **UserPage:** group profile and channel links into a single paragraph ([e9c7ccc](https://github.com/knowledgepixels/nanodash/commit/e9c7ccc66262a60dd5a1e93f06bebf16a667bdc6))
* **ViewList:** add error message when the view type is not supported ([587155c](https://github.com/knowledgepixels/nanodash/commit/587155ca9e9d6e4cd2eafb302cfc0061f353226e))

### Tests

* **GrlcQuery:** add test for retrieving query ID from QueryRef ([8479c2c](https://github.com/knowledgepixels/nanodash/commit/8479c2c440de2d294c7fecc1b0c2e64d56a76ef7))

### General maintenance

* **HomePage:** replace "top creators" and "get started templates" sections to use `View`s and `ViewDisplay` ([835cb34](https://github.com/knowledgepixels/nanodash/commit/835cb34a76f16c76e25623ede7f19bc0396b71c2))
* move 'Show Full Channel' button from QueryResultNanopubSet to UserPage ([73d8900](https://github.com/knowledgepixels/nanodash/commit/73d89009dcf7d4287b52511b34214ee9d3068321))
* **QueryApiAccess:** update nanopub list API endpoint `get-filtered-nanopub-list` ([151a74e](https://github.com/knowledgepixels/nanodash/commit/151a74e30f10493ccfbd08572a5ff93bcf418a97))
* **QueryResultNanopubSet:** enhance UI with title and navigation links, improve component population ([feb44e9](https://github.com/knowledgepixels/nanodash/commit/feb44e90c615ae8644e590014c9c09a394e56923))
* **QueryResultNanopubSet:** enhance view selector layout and styling for improved usability ([4a5d184](https://github.com/knowledgepixels/nanodash/commit/4a5d18424a6d8f21daf779afeda6860aa6876ec1))
* setting next snapshot version [skip ci] ([c71f875](https://github.com/knowledgepixels/nanodash/commit/c71f8757db0f6debbdccd47047c241f53aee7223))
* Update CSS link version ([2c507ba](https://github.com/knowledgepixels/nanodash/commit/2c507ba1db92f4ce355f328c5d27ea01206cc912))

### Style improvements

* Add small home page footer ([280c347](https://github.com/knowledgepixels/nanodash/commit/280c34706fb6658410c25f8542608fb901c20b24))
* Align padding of user/bot lists ([749c793](https://github.com/knowledgepixels/nanodash/commit/749c7933597e5551670f2bb1800962b54d39bc02))
* Remove "beta" from logo and "by Knowledge Pixels" link ([f6fd2c5](https://github.com/knowledgepixels/nanodash/commit/f6fd2c5e2e278c1380aa8de114f678996dd8a40d))
* Simplify user page headings ([4d2263a](https://github.com/knowledgepixels/nanodash/commit/4d2263a1d54bf24f1cb1c2441011d4c9db191929))

### Refactoring

* Only use full query ID instead of query name ([17fd854](https://github.com/knowledgepixels/nanodash/commit/17fd8549f0f3687ecc126b4d713c32fa292b9da2))
* rename `ProfiledResource` to `ResourceWithProfile` and update references ([4f6de07](https://github.com/knowledgepixels/nanodash/commit/4f6de07bbb294e70fae1560217e5d93bdfb7325c))
* rename `ResourceView` to `View` and update references ([196b153](https://github.com/knowledgepixels/nanodash/commit/196b1534b65176ee420f680338d1331999367396))
* **ViewList:** improve HTML structure and update logging format for error messages ([e4a297d](https://github.com/knowledgepixels/nanodash/commit/e4a297d1978898061d1d379be931f3360ab03aa7))

## [4.11.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.10.0...nanodash-4.11.0) (2026-01-28)

### Features

* show label for profiled resources filled into form ([63ace79](https://github.com/knowledgepixels/nanodash/commit/63ace79c7fb1f6fb04a426b62e5efc727a3c5fa2))
* Support for "advanced" statements, hidden in collapsed view ([0171112](https://github.com/knowledgepixels/nanodash/commit/01711127e3ffa680cfc6af1f72ebc2d7d11c1c5b))
* **Template:** Allow for redundant group statement links ([008c4fa](https://github.com/knowledgepixels/nanodash/commit/008c4faad2309b322e3eb84f4e819061c0ce5252))

### General maintenance

* setting next snapshot version [skip ci] ([3e0cf3a](https://github.com/knowledgepixels/nanodash/commit/3e0cf3a39ed6de35b7649a5b7fdd4e1c168e3628))
* Update CSS link version ([474a8d5](https://github.com/knowledgepixels/nanodash/commit/474a8d5423899c98daa1881ab4936473507b53d0))

### Style improvements

* Adjust component width after expand/collapse ([9d5fefb](https://github.com/knowledgepixels/nanodash/commit/9d5fefb2a61c4542d4f8ef029701790b5bf8a3aa))
* Adjust labels for expand/collapse forms ([72009af](https://github.com/knowledgepixels/nanodash/commit/72009afd9b7cc309e9a62f952fb2c5f6bb92b473))
* Hide 'collapse-content' when empty ([9c4a047](https://github.com/knowledgepixels/nanodash/commit/9c4a0479762b51e43214e22b3d2db0eeffa0ea79))

## [4.10.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.9.0...nanodash-4.10.0) (2026-01-26)

### Features

* **PublishForm:** Show simple view with toggle for full view ([0195add](https://github.com/knowledgepixels/nanodash/commit/0195add2a6441e44acbcb8bf006861926c04e308))

### Bug Fixes

* Prevent pages from prematurely be discarded from the page store ([9f77daf](https://github.com/knowledgepixels/nanodash/commit/9f77dafac8e55929b93f8a79fe6a1ff095afc6e3))

### General maintenance

* Make consent box text more concise and clearer ([99fc4a3](https://github.com/knowledgepixels/nanodash/commit/99fc4a353c8f1e28b8c2369177345a0d40ce7066))
* setting next snapshot version [skip ci] ([53c81bb](https://github.com/knowledgepixels/nanodash/commit/53c81bb436898227c8e72c209b06e45a186b0607))
* Update CSS version links ([1a981df](https://github.com/knowledgepixels/nanodash/commit/1a981df7d30d315c3f47d56e23b38800aa3db32b))

### Style improvements

* Add "advanced" CSS class so we can later hide when needed ([b78ed2d](https://github.com/knowledgepixels/nanodash/commit/b78ed2d0447ca2c3930d8b3493289f52fa53a336))
* Deactivate namespace display on publish form for simplicity ([1923302](https://github.com/knowledgepixels/nanodash/commit/1923302abf850a067d71fb51f1ef37fd4144e631))
* Remove title for publish form ([a694b09](https://github.com/knowledgepixels/nanodash/commit/a694b09ccafd64fb3f891d6db215492b279ea141))

## [4.9.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.8.1...nanodash-4.9.0) (2026-01-21)

### Features

* **QueryPage:** Reactivate YasGUI link with auto-exec-blocker ([4f3d962](https://github.com/knowledgepixels/nanodash/commit/4f3d96200dd887b20dadb1c0d66cd642625a65b4))

### General maintenance

* Add bot icon (not yet used) ([0d54735](https://github.com/knowledgepixels/nanodash/commit/0d5473535c6fb5f4af465b3568c7f5f5a2df8c5a))
* setting next snapshot version [skip ci] ([4f3689b](https://github.com/knowledgepixels/nanodash/commit/4f3689be2cb3d2066e65dce17549b8e4d4493854))

## [4.8.1](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.8.0...nanodash-4.8.1) (2026-01-15)

### Bug Fixes

* **ValueFiller:** Apply transform to embedded URIs ([444d571](https://github.com/knowledgepixels/nanodash/commit/444d5713b5c3062fedd3f29e21c53202bb7d72ae))

### General maintenance

* add .vscode to .gitignore ([d393434](https://github.com/knowledgepixels/nanodash/commit/d3934349db4a793a15fc4b2e87762803b2ec2224))
* setting next snapshot version [skip ci] ([6400e96](https://github.com/knowledgepixels/nanodash/commit/6400e96306a73661314ff4ad6c1314f5921f95b7))

## [4.8.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.7.0...nanodash-4.8.0) (2026-01-14)

### Features

* **QueryResultTable:** Add result filtering textfield ([35eecfc](https://github.com/knowledgepixels/nanodash/commit/35eecfcf7da28b4397dd9a954b19f66208e684f6))

### Bug Fixes

* **AjaxZonedDateTimePicker:** Avoid NullPointerException when optional ([35f6e62](https://github.com/knowledgepixels/nanodash/commit/35f6e6214b7a1358651439faf2f36af42e844924))
* **MaintainedResource:** Fix sync problem in ensureLoaded ([0bd3618](https://github.com/knowledgepixels/nanodash/commit/0bd36189a3a90fcac17f8db7f59855b5d26a365e))

### General maintenance

* setting next snapshot version [skip ci] ([a1c0e00](https://github.com/knowledgepixels/nanodash/commit/a1c0e004f44866bc5c46bfa8b887d232f0a3a317))

## [4.7.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.6.1...nanodash-4.7.0) (2026-01-13)

### Features

* **SearchPage:** Basic search covers all literals ([ec4b1bd](https://github.com/knowledgepixels/nanodash/commit/ec4b1bdd04a9ee34c1db501e690e73034e28a1c5))

### Bug Fixes

* **QueryPage:** Enforce full-ID to ensure OpenAPI links work ([e037aaa](https://github.com/knowledgepixels/nanodash/commit/e037aaabfbac9d0a2fbf59b71514d68d5333aa71))
* **SearchPage:** Search page tampered with cached list ([acdebe6](https://github.com/knowledgepixels/nanodash/commit/acdebe64035364204544e9adfe194b00430e85a9))
* Some log messages failed to show ([1cd30f9](https://github.com/knowledgepixels/nanodash/commit/1cd30f9399ea080bbc1969be06f41d3a9b76b059))

### General maintenance

* setting next snapshot version [skip ci] ([cf694df](https://github.com/knowledgepixels/nanodash/commit/cf694dfe56fdbe1f11fa1c00fc9147935c2b8d0a))

## [4.6.1](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.6.0...nanodash-4.6.1) (2026-01-12)

### Bug Fixes

* **ViewDisplay:** Avoid breaking page for invalid view display ([d458d26](https://github.com/knowledgepixels/nanodash/commit/d458d2611d58f42b5684d10545672d6d2caf2878))

### General maintenance

* Add nohup.out to .gitignore ([7d679f2](https://github.com/knowledgepixels/nanodash/commit/7d679f2d5d962cd8ba5d407b0f8289f77a403cd7))
* setting next snapshot version [skip ci] ([75a12da](https://github.com/knowledgepixels/nanodash/commit/75a12dad21b1db0920897af066392fdf1be89d90))
* Update CSS version link ([9815864](https://github.com/knowledgepixels/nanodash/commit/9815864eae259ae8a58d3688eb0b5ad5a2cedb21))

## [4.6.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.5.2...nanodash-4.6.0) (2026-01-06)

### Features

* **LookupApis:** Update to latest ror.org API version ([9868a81](https://github.com/knowledgepixels/nanodash/commit/9868a81d20e0e1a9a00c5da56a3a426128f76f34))
* **QueryResultPlainParagraph:** Show source links of elements ([be7bda7](https://github.com/knowledgepixels/nanodash/commit/be7bda791fb30ca47a29742fcef4060443bb8c99))
* **QueryResultPlainParagraph:** Support view buttons/actions ([2337c49](https://github.com/knowledgepixels/nanodash/commit/2337c4971ada224f3da7ea8dd43844d62d235eb5))
* **QueryResultPlanParagraph:** Add proper titles and fix earlier commit ([d340def](https://github.com/knowledgepixels/nanodash/commit/d340deff2a032c013b2d2895fd62e0825a6dceb7))

### General maintenance

* setting next snapshot version [skip ci] ([52b0047](https://github.com/knowledgepixels/nanodash/commit/52b00470c5d40a93c8b429c4af513b0852f5f10f))

### Refactoring

* remove unused imports ([9a7754f](https://github.com/knowledgepixels/nanodash/commit/9a7754f5f1f580394e6300a0adf43ef793c04c51))

## [4.5.2](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.5.1...nanodash-4.5.2) (2025-12-18)

### Bug Fixes

* **ResourcePartPage:** Synchronous retrieval of definition ([deae9e0](https://github.com/knowledgepixels/nanodash/commit/deae9e05fbe2a9a8691688bf306c17d5c0d01d70))
* **Space:** Leaking Space objects fixed ([f28b782](https://github.com/knowledgepixels/nanodash/commit/f28b7829847eb74c701cbbdc5f59c71ab5c6de75))

### General maintenance

* setting next snapshot version [skip ci] ([c40515e](https://github.com/knowledgepixels/nanodash/commit/c40515eec11ce732ba1da131a1aaab06c362dea9))

## [4.5.1](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.5.0...nanodash-4.5.1) (2025-12-17)

### Bug Fixes

* Use API result cache for all requests ([e662f50](https://github.com/knowledgepixels/nanodash/commit/e662f50621f58719f0c4df8c7262d5f56b50013c))

### Build and continuous integration

* **deps:** update actions/checkout action to v5.0.1 ([003da6c](https://github.com/knowledgepixels/nanodash/commit/003da6c920038b80d137b6ac866777ea4cef613e))
* **deps:** update actions/setup-java action to v5.1.0 ([563c30d](https://github.com/knowledgepixels/nanodash/commit/563c30d501e3a20d380350791a044566eb3ed469))

### General maintenance

* setting next snapshot version [skip ci] ([929b979](https://github.com/knowledgepixels/nanodash/commit/929b979735488e66fb702230fcb8552d53e89687))

## [4.5.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.4.0...nanodash-4.5.0) (2025-12-16)

### Features

* Add simple script for stress-testing ([45a4847](https://github.com/knowledgepixels/nanodash/commit/45a4847cb1518c7b01925b9b6ea68741af440e57))
* add support for PLAIN_PARAGRAPH_VIEW in resource display logic ([a0ba42c](https://github.com/knowledgepixels/nanodash/commit/a0ba42c85ef240ef9c7f6de635413ff402636994))
* Make use of template-version=latest in more places ([c20c94c](https://github.com/knowledgepixels/nanodash/commit/c20c94c1b51e3abc3905fa0c37268460b6ebdea6))
* **QueryResultPlainParagraph:** use ListView for allowing multi paragraphs view ([3d82328](https://github.com/knowledgepixels/nanodash/commit/3d82328254b5e48c009b7999da82359dc9db17cf))
* Support for OpenAIRE Graph lookup ([39c0371](https://github.com/knowledgepixels/nanodash/commit/39c03710e870858ed22da0f5cd1e3424dbc4eba5))

### Dependency updates

* **deps:** Update nanopub dependency ([1bf4857](https://github.com/knowledgepixels/nanodash/commit/1bf48575c071187b61dbd0121c2bd29f99541211))

### Bug Fixes

* **QueryResultPlainParagraph:** update title header from h2 to h3 ([c35eff5](https://github.com/knowledgepixels/nanodash/commit/c35eff5f32d54d2568f20c39c04a39c16a3c3768))
* **Space:** Synchronize ensureLoaded wait code ([feab842](https://github.com/knowledgepixels/nanodash/commit/feab842deece800e2a65c3d0bc61231f47bba0a3))

### Tests

* **ProfiledResource:** add unit tests ([dc2c16a](https://github.com/knowledgepixels/nanodash/commit/dc2c16a78ef7558a20df90ea9064ec6ab6e9a0eb))

### General maintenance

* **KPXL_TERMS:** add IRI for PlainParagraphView ([65af494](https://github.com/knowledgepixels/nanodash/commit/65af494ca64ff9ec25945fe44bbcf0e040b84cc5))
* **QueryResultPlainParagraph:** add component for displaying query results in plain paragraph format ([960e3e5](https://github.com/knowledgepixels/nanodash/commit/960e3e506d3335031f053379bee1e6c0e2cf49a3))
* **QueryResultPlainParagraphBuilder:** add builder for QueryResultPlainParagraph component ([3d59090](https://github.com/knowledgepixels/nanodash/commit/3d59090cd45530bad45edc39d26f168c8f084c66))
* setting next snapshot version [skip ci] ([6c1916b](https://github.com/knowledgepixels/nanodash/commit/6c1916b785ef98bad2343ce4fb4f2016f88389bd))

### Refactoring

* replace Utils methods with NanopubUtils defined in the nanopub-java library ([7ce230f](https://github.com/knowledgepixels/nanodash/commit/7ce230f793750d8df2a56d98cec79fe36c68a853))
* replace VocabUtils with the ones defined in the nanopub-java library ([ed9b72e](https://github.com/knowledgepixels/nanodash/commit/ed9b72efaba5911addfd23a80f4274b6fffacb53))

## [4.4.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.3.0...nanodash-4.4.0) (2025-12-15)

### Features

* **NanodashLink:** Don't pass "^" label so it makes good nanopub links ([89837d6](https://github.com/knowledgepixels/nanodash/commit/89837d6a4836ef3659493623daeae17ee460b2c0))
* **ProfiledResource:** Consistently refresh, also for view displays ([a218bb8](https://github.com/knowledgepixels/nanodash/commit/a218bb85c2eaf11aae1199855938fbf47fa08f1f))
* **QueryResultList:** add pagination and navigation for query results ([8f5eb99](https://github.com/knowledgepixels/nanodash/commit/8f5eb99d2020109ddab073757b4ae0e12dcd92cf))

### Dependency updates

* **deps:** update sem-release dependencies to fix security issues ([0ee25fc](https://github.com/knowledgepixels/nanodash/commit/0ee25fcf5c403e9433c5e5d6d1a955d076883a64))

### Bug Fixes

* Increase wait time after failed API request ([f4d9f05](https://github.com/knowledgepixels/nanodash/commit/f4d9f052d184d85a0b33657f93476aee2685d7ef))
* **ItemListPanel:** transient marker caused problems and not needed ([82e7d95](https://github.com/knowledgepixels/nanodash/commit/82e7d952028ca6112d16520520d4b253a0698726))
* **MaintainedResource:** Keep cached results after refresh ([71380b9](https://github.com/knowledgepixels/nanodash/commit/71380b9b0a450711ac5af1b45a8af8743ce9fc3c))
* **QueryResultList:** add output markup for pagination component ([ba8cf37](https://github.com/knowledgepixels/nanodash/commit/ba8cf37c1ee022d987c7ef78ffc8ac86cd4d21b2))

### Build and continuous integration

* **release:** enhance testing workflow to support multiple Java versions ([887a0ff](https://github.com/knowledgepixels/nanodash/commit/887a0ff6afea50329e1808ddb98931cbd39819e2))
* **test:** update to support multiple Java versions and parallel coverage reporting ([1ce543d](https://github.com/knowledgepixels/nanodash/commit/1ce543df6bf6ac29be83847318e7c9bab4924906))

### General maintenance

* add contributing guidelines ([5e74c4d](https://github.com/knowledgepixels/nanodash/commit/5e74c4dcf8380f1c4083e070f8e63c31951991e4))
* **QueryResult:** add abstract base class for displaying query results ([51f8299](https://github.com/knowledgepixels/nanodash/commit/51f8299b133bab3bd82980c0152ef2566d987802))
* **QueryResultDataProvider:** add data provider for query results ([e1e0972](https://github.com/knowledgepixels/nanodash/commit/e1e09721c7aec41cc8c24f31dce6e0f04065e6f1))
* **QueryResultList:** change separator format for improved readability ([cb926fe](https://github.com/knowledgepixels/nanodash/commit/cb926feee08174e1b29f7afd3bddbcd8a3593603))
* **readme:** add badges and contributing section ([587560c](https://github.com/knowledgepixels/nanodash/commit/587560c40ce3a8c39ba1f28a81fcc09e134cbafb))
* setting next snapshot version [skip ci] ([a987b21](https://github.com/knowledgepixels/nanodash/commit/a987b214f227281d9257e80d83962ececa0e15e7))

### Refactoring

* **QueryResult:** simplify QueryResultList and QueryResultTable components ([799fceb](https://github.com/knowledgepixels/nanodash/commit/799fceb2e92892b3e3710c8e29020c2e93e9fe2e))

## [4.3.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.2.0...nanodash-4.3.0) (2025-12-05)

### Features

* **ResourceView:** Move to new model of views and their displays ([ae8e445](https://github.com/knowledgepixels/nanodash/commit/ae8e445c0d3c45cbfbb2d406eb5ecbcb58f0621d))

### Bug Fixes

* **ResourcePartPage:** update breadcrumb navigation to include resource part and full hierarchy ([7fe9042](https://github.com/knowledgepixels/nanodash/commit/7fe9042c23786eb5a52fcb3b96964647230bd883))
* **ViewDisplay:** Correctly consider gen:appliesTo values ([d90b828](https://github.com/knowledgepixels/nanodash/commit/d90b828c8fdd43cefafb905f43c27f03b98217c8))

### General maintenance

* setting next snapshot version [skip ci] ([8949f41](https://github.com/knowledgepixels/nanodash/commit/8949f41a4504ee34d61f438a230c65c452d6ff1d))
* update TODO annotations ([3ae1d94](https://github.com/knowledgepixels/nanodash/commit/3ae1d945d92d01c4d3c8faa1c2586a04df0ede16))

## [4.2.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.1.1...nanodash-4.2.0) (2025-12-04)

### Features

* **ExplorePage:** Recognize target classes of views for redirects ([6c4b6e8](https://github.com/knowledgepixels/nanodash/commit/6c4b6e8afaa7755789c7d93c074a02443396fe44))
* **MaintainedResource:** Use new "appliesTo..." fields ([896c8a2](https://github.com/knowledgepixels/nanodash/commit/896c8a2657076ddc4c437caf11e5fe59482c9217))
* **QueryResultList:** add action button ([9072fc0](https://github.com/knowledgepixels/nanodash/commit/9072fc08361d47599947959073ab50794b7e0be9))
* **ResourceView:** Automatically apply latest version of views ([353a07d](https://github.com/knowledgepixels/nanodash/commit/353a07dd00024d9219e7ecb9ebe861bd22cf8a1c))
* **ResourceView:** Support new appliesToInstancesOf/Namespace preds ([de611cb](https://github.com/knowledgepixels/nanodash/commit/de611cbb69db60308555aeb263e368e9717f96cc))
* **SpacePage:** add breadcrumb navigation ([602c89e](https://github.com/knowledgepixels/nanodash/commit/602c89eb123d6e80913a1e134c101652368e01b7))
* **Space:** Use new "appliesTo..." fields to calculate view displays ([a283441](https://github.com/knowledgepixels/nanodash/commit/a2834411ed6e5bccf9d818c529969fc094c044a5))
* **UserPage:** Add support for view displays ([d38395c](https://github.com/knowledgepixels/nanodash/commit/d38395c02ed903f5c472320123140ab6be63415a))
* **UserPage:** Removing activity panel for now, to give space to views ([0710079](https://github.com/knowledgepixels/nanodash/commit/07100796978c5578a97b7c8d2901626da2dea1d6))
* **UserPage:** Support for view displays (buttons still missing) ([e370fae](https://github.com/knowledgepixels/nanodash/commit/e370fae47c0a4c7bcaff30690078f86753e3e518))
* **ViewDisplay:** Add plain 'appliesTo' predicate support ([c676566](https://github.com/knowledgepixels/nanodash/commit/c676566052f6a03118a32b0d334f8d2612e0f3b7))
* **ViewDisplay:** Allow for deactivating view displays by other admins ([b8bb5f3](https://github.com/knowledgepixels/nanodash/commit/b8bb5f38c84ef79a8134a6503e1cffbc440ecfed))
* **ViewDisplay:** Support appliesToInstancesOf/Namespace overriding ([85d0dba](https://github.com/knowledgepixels/nanodash/commit/85d0dba89d176a4d3e01d67e3462b09450103af1))

### Dependency updates

* **core-deps:** Update dependencies ([40a082f](https://github.com/knowledgepixels/nanodash/commit/40a082f44fa220a435bca6032296f71253accbe1))

### Bug Fixes

* **ExplorePage:** Correct check whether part view applies ([d9c9dcd](https://github.com/knowledgepixels/nanodash/commit/d9c9dcdf7281cc921ce716fd212c1340b2afaf93))
* **ProfiledResource:** Fix showing of views for resource parts ([f47eb09](https://github.com/knowledgepixels/nanodash/commit/f47eb09e8d0ca39c82ffb47c2f659d595f5b7726))
* **ProfiledResource:** Working regular refreshing ([3b0544f](https://github.com/knowledgepixels/nanodash/commit/3b0544f9176ab181fffc31a284adf0455d43397c))
* **QueryResultList:** load action buttons as in tabular view ([820ad1b](https://github.com/knowledgepixels/nanodash/commit/820ad1b80479fce39df25a16b9dcb256fa24d09f))
* **QueryResultList:** populate list with API response data as last step before building to ensure that all the fields are set ([3505aba](https://github.com/knowledgepixels/nanodash/commit/3505aba252e6721d023fc2f6c996f49ba51c9d39))
* **QueryResultList:** remove escape to allow HTML rendering of items in list view ([e489516](https://github.com/knowledgepixels/nanodash/commit/e489516bc9fe6ea3ce59e3b73a36f871c7c73d64))
* update breadcrumb navigation to display the full hierarchy until the root space ([0a08fe3](https://github.com/knowledgepixels/nanodash/commit/0a08fe3be375b7510e182183f56bffb62a3d81ac))
* **Utils:** Array length exception when "?" is found at end of string ([9121001](https://github.com/knowledgepixels/nanodash/commit/9121001256ad51d6a3e70f263b94c6395dcc1dd6))
* **ViewList:** Set namespace parameter when applicable ([3a283a9](https://github.com/knowledgepixels/nanodash/commit/3a283a96b20f9e74db634b4d130b00bd1e24758e))

### Tests

* **SpaceMemberRoleRef:** add unit tests ([7c8ab5d](https://github.com/knowledgepixels/nanodash/commit/7c8ab5d606e0fd2fc96a973064dbed9aca95a644))

### General maintenance

* **QueryResultList:** enhance list item rendering ([2177390](https://github.com/knowledgepixels/nanodash/commit/21773909f7d8e8d03e7dd46635f273918207ace9))
* remove "part of/maintained by" sections in resource and space pages ([73f93db](https://github.com/knowledgepixels/nanodash/commit/73f93dbd38c161d41f348e21ec0fb4b06b5e71b1))
* setting next snapshot version [skip ci] ([d04ca30](https://github.com/knowledgepixels/nanodash/commit/d04ca3027c19dcfa7cc8377cfc6ad69388ed579d))
* Update .gitignore ([63442de](https://github.com/knowledgepixels/nanodash/commit/63442de192952e42be43ffd02d2f6bc535f3a0f3))

### Refactoring

* **ButtonList:** Generalize from Space to ProfiledResource ([0560011](https://github.com/knowledgepixels/nanodash/commit/056001115046e218a62d37a01f6cd9027ccb099d))
* **MaintainedResource:** Move ResourceData to ProfiledResource ([aee48f4](https://github.com/knowledgepixels/nanodash/commit/aee48f47583a872439d8b214cf3493f8e3f66049))
* **QueryResultTable:** remove unused space parameter and update related methods ([ee5c7c1](https://github.com/knowledgepixels/nanodash/commit/ee5c7c1ce6f9f493ed9ac2b9895ae07509be63a7))
* resolve minor issues introduced by conflicts resolution ([e3f9303](https://github.com/knowledgepixels/nanodash/commit/e3f9303659382519477847e6c9b39ed1e675d4ca))
* **Space:** Make subclass of ProfiledResource ([e943d58](https://github.com/knowledgepixels/nanodash/commit/e943d58b5fbf68e3a18947f17e134c0176a2fbc7))
* **Space:** replace Pair with SpaceMemberRoleRef for role management ([f188c2f](https://github.com/knowledgepixels/nanodash/commit/f188c2f798ce4021f047118a0319da77d3cd5cf4))
* **ViewList:** merge code for ResourceParts with the other types ([74f2d8e](https://github.com/knowledgepixels/nanodash/commit/74f2d8e711ce375effe064a96e0d0de13acb4533))

## [4.1.1](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.1.0...nanodash-4.1.1) (2025-11-26)

### Bug Fixes

* **QueryResultTable:** Exception when view is null ([80a4cfb](https://github.com/knowledgepixels/nanodash/commit/80a4cfbd465c0b6ec845a64e6105175cdfa9becd))

### General maintenance

* setting next snapshot version [skip ci] ([2ae3fcf](https://github.com/knowledgepixels/nanodash/commit/2ae3fcf2f4c480e18165ea5ece3bb95ee9e3ec06))
* Update CSS links ([115f8f9](https://github.com/knowledgepixels/nanodash/commit/115f8f9a7247ade175c397c0db959ebf5ffd446f))

## [4.1.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.0.1...nanodash-4.1.0) (2025-11-25)

### Features

* **ResourceView:** Allow for entry-level actions ([e60dff9](https://github.com/knowledgepixels/nanodash/commit/e60dff9b3804bb04baa787b870a25d4ad2402068))
* **ResourceView:** Make explicit ViewResultAction class ([15b9773](https://github.com/knowledgepixels/nanodash/commit/15b9773657d19eec096716e85cf66eb494b6f06e))

### General maintenance

* setting next snapshot version [skip ci] ([b72abfb](https://github.com/knowledgepixels/nanodash/commit/b72abfbfaf91f8ad9ff6e5d02ad5dee1dee762c0))

### Refactoring

* **ResourceView:** Consistent naming ([d400539](https://github.com/knowledgepixels/nanodash/commit/d400539925dd501ed2a960beea2c99773d490a62))

## [4.0.1](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.0.0...nanodash-4.0.1) (2025-11-18)

### Bug Fixes

* **SpaceUserList:** Fix user link ([4e10b17](https://github.com/knowledgepixels/nanodash/commit/4e10b179c8d2045d720f0b2cc27f55ebd89c285a))
* **ViewList:** add support to list view for all the Space subcomponents (e.g. maintained resources) ([61f9e36](https://github.com/knowledgepixels/nanodash/commit/61f9e364edf0baa1d8f54e2fba4542bba809f61f))

### Build and continuous integration

* **deps:** update action actions/setup-java to v5.0.0 ([2a3122e](https://github.com/knowledgepixels/nanodash/commit/2a3122ebd07fc22ef8080cca035a9d8b37dd0c71))

### General maintenance

* add KPXL_TERMS vocabulary class ([16e91ef](https://github.com/knowledgepixels/nanodash/commit/16e91efd4bd3e65ed601a731a14b7896ce24aa83))
* add VocabUtils for namespace and IRI creation ([cac6300](https://github.com/knowledgepixels/nanodash/commit/cac6300672d0b0855fd6cc08f7f80052fc19676f))
* **docker-compose:** add tag to image name and format port mapping ([b57e0fc](https://github.com/knowledgepixels/nanodash/commit/b57e0fc072607fc48cf8b40524bdfe4cb3d378f9))
* setting next snapshot version [skip ci] ([1771b33](https://github.com/knowledgepixels/nanodash/commit/1771b3335a5edc22218ef44d0bdb003092e6c168))

### Refactoring

* replace hardcoded IRIs with KPXL_TERMS constants ([be99b18](https://github.com/knowledgepixels/nanodash/commit/be99b189e18b495084a12d31a9df7c9c50cea20b))

## [4.0.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-3.64.0...nanodash-4.0.0) (2025-11-17)

### ⚠ BREAKING CHANGES

* update version to comply with semantic versioning and force breaking change release

### Features

* "refresh-upon-publish" PublishPage param to force cache refreshing ([73fbd08](https://github.com/knowledgepixels/nanodash/commit/73fbd08e7f880b5b2fcc3169bfc4d012edec0fe8))
* **SpacePage:** Show source links also for roles ([7e3fbcc](https://github.com/knowledgepixels/nanodash/commit/7e3fbcc3c04b9c1d33fa0cc2e280d5f7bffa2625))
* **SpaceUserList:** Show source nanopubs for Space memberships ([82ce154](https://github.com/knowledgepixels/nanodash/commit/82ce1543b3485ff0d91eb177f9e5afbbba4cc0a6))

### Dependency updates

* **deps:** add dependency com.google.cloud.tools:jib-maven-plugin to v3.4.6 ([f8d8834](https://github.com/knowledgepixels/nanodash/commit/f8d8834e0b8730d8b0b9d12e7fe7d314095eead3))
* **deps:** add semantic-release and related packages ([9920e8f](https://github.com/knowledgepixels/nanodash/commit/9920e8fa951b14f005d63d188b164ce19bbb307f))

### Bug Fixes

* Re-trying failed queries 2 times to reduce query failure errors ([70222f9](https://github.com/knowledgepixels/nanodash/commit/70222f9fb383442a7d53f22507fefaa3c6248758))

### Documentation

* **SpaceMemberRole:** add missing javadoc annotations ([1c835e4](https://github.com/knowledgepixels/nanodash/commit/1c835e4737530fdb121b1ee93ff492e201756ce8))

### Build and continuous integration

* **release:** update Docker Hub token env variable name ([4184997](https://github.com/knowledgepixels/nanodash/commit/41849979fdb130438597aa702edc35ce5b1364bb))
* **release:** update release workflow by using semantic-release on "release" branch ([583276d](https://github.com/knowledgepixels/nanodash/commit/583276d7df7697e39a00122e5a21e8c926731e72))

### General maintenance

* **gitignore:** add node_modules ([8f043de](https://github.com/knowledgepixels/nanodash/commit/8f043de311f3c35cdc0d7136c49d881a0c1fef2e))
* **scripts:** remove release.sh since it's now replaced by semantic-release plugin ([bb126e0](https://github.com/knowledgepixels/nanodash/commit/bb126e0fa2bc79e8d9c172e2d7cbf5fa7143599b))
* **scripts:** replace run.sh with run-dev.sh for local development setup ([c067ff5](https://github.com/knowledgepixels/nanodash/commit/c067ff5b74dfbe808b899cb10fb404d06a17dc0d))
* **sem-release:** add release branch configuration ([6fa7eee](https://github.com/knowledgepixels/nanodash/commit/6fa7eeed994202a38f8c9d61e1476b40550faab6))
* **settings:** add Docker registry auth configuration ([97b4fd4](https://github.com/knowledgepixels/nanodash/commit/97b4fd492c2fde833431de4e6ed49a95df72b11b))
* update version to comply with semantic versioning and force breaking change release ([3d7fab1](https://github.com/knowledgepixels/nanodash/commit/3d7fab101001c0220e403dae7ff88c08bfa5aa61))

### Refactoring

* **StatementItem, TemplateContext:** remove generic type parameters for StatementItem class ([7eb8180](https://github.com/knowledgepixels/nanodash/commit/7eb818065b0501f0b5e58aa107479bf472bcc04a))
