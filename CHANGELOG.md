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
