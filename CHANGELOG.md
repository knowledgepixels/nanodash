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
