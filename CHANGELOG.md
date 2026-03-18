## [4.20.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.19.0...nanodash-4.20.0) (2026-03-18)

### Features

* add emoji icons to section headers across the app ([8d528fd](https://github.com/knowledgepixels/nanodash/commit/8d528fd970e642f13bba3b123789d7de88f444e9))
* add Noto Emoji monochrome font for consistent emoji rendering ([6b9190f](https://github.com/knowledgepixels/nanodash/commit/6b9190f0f12f8acbf0c986f470a5c82eb6492ea2))
* auto-wrap leading emoji in headings via JS ([118e77d](https://github.com/knowledgepixels/nanodash/commit/118e77d94919225f2d1786f1c135887382076dbb))
* style emoji spans with blue gradient and larger size ([2c3a3e2](https://github.com/knowledgepixels/nanodash/commit/2c3a3e2710c95df63cce3e4a2c0c115c5be988dd))
* support nanopub preview via base64url-encoded TriG in URL ([#403](https://github.com/knowledgepixels/nanodash/issues/403)) ([42f1dab](https://github.com/knowledgepixels/nanodash/commit/42f1dabc59694c58e18ca02b3b9f392b1e1c5d97))
* use flat theme blue for emoji instead of gradient ([8c2ed26](https://github.com/knowledgepixels/nanodash/commit/8c2ed26e4ba85118d34265e1de05b268a8561a64)), closes [#0B73DA](https://github.com/knowledgepixels/nanodash/issues/0B73DA)

### Bug Fixes

* move references/status to own row, fix stray paneltitlerow bars ([6a84408](https://github.com/knowledgepixels/nanodash/commit/6a844084dcb0b5365fec1964452836e2305ddd7b))
* rename "Resources" to "Maintained Resources" on space page ([399d749](https://github.com/knowledgepixels/nanodash/commit/399d749bf584d6615921064a424178c1dba64638))
* simplify info panel layout, add URI to references page, tweak icons ([50e7060](https://github.com/knowledgepixels/nanodash/commit/50e7060f427f1898ddac3e09279d68fe26fc838b))
* use WicketApplication in ViewPageTest for NanodashSession support ([5f918f0](https://github.com/knowledgepixels/nanodash/commit/5f918f07c60c44eb4d32ae60b7de28b4a9036a2c))

### General maintenance

* setting next snapshot version [skip ci] ([301768e](https://github.com/knowledgepixels/nanodash/commit/301768e5f74b504c52eb23472bc4b4eacc2a6202))

### Refactoring

* redesign Explore page layout and extract References page ([eeb1e15](https://github.com/knowledgepixels/nanodash/commit/eeb1e159d8bcb3b33e065a2dd5e700ca63efdadd))

## [4.19.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.18.0...nanodash-4.19.0) (2026-03-16)

### Features

* auto-merge release branch back into master after publishing ([39c6ac1](https://github.com/knowledgepixels/nanodash/commit/39c6ac11c9d8326233fa1a6ada9f63ac77897c3a))

### Bug Fixes

* add robots.txt to reduce bot-driven cache pollution ([#401](https://github.com/knowledgepixels/nanodash/issues/401)) ([2fe9541](https://github.com/knowledgepixels/nanodash/commit/2fe9541115dd7489162395f7a6cab74d1e3bc9d9))
* add viewport meta tag to QueryPage for correct mobile rendering ([732b1b9](https://github.com/knowledgepixels/nanodash/commit/732b1b9d775f7fdb977f47922afe53d53128a708))
* bound ApiCache with Guava Cache to prevent unbounded memory growth ([#401](https://github.com/knowledgepixels/nanodash/issues/401)) ([56c6fa0](https://github.com/knowledgepixels/nanodash/commit/56c6fa0ed4318f9e291ddd4af3a67af4ee96950a))
* bound remaining static caches with Guava Cache ([#401](https://github.com/knowledgepixels/nanodash/issues/401)) ([b58e926](https://github.com/knowledgepixels/nanodash/commit/b58e926b5b840d18b30092cf9d96ff78d34d2894))
* clean up session forms after publish and cap at 20 ([#401](https://github.com/knowledgepixels/nanodash/issues/401)) ([a8f86ed](https://github.com/knowledgepixels/nanodash/commit/a8f86ed7d0935515fe0bd71d13b46876b6dcda32))
* close leaked streams and HTTP entities ([#401](https://github.com/knowledgepixels/nanodash/issues/401)) ([94d9669](https://github.com/knowledgepixels/nanodash/commit/94d966973d5737aa7f08fce9c857e1ae09db5f02))
* constrain form fields to viewport width on narrow screens ([041503b](https://github.com/knowledgepixels/nanodash/commit/041503b4a2763b7fa6a708ae860ecb6dbe5489d2))
* force API cache refresh in Space data update ([#401](https://github.com/knowledgepixels/nanodash/issues/401)) ([b981083](https://github.com/knowledgepixels/nanodash/commit/b9810835b5100313178f3f9275e5bb9507d9035b))
* make images horizontally scrollable on narrow screens ([7b51058](https://github.com/knowledgepixels/nanodash/commit/7b510582a67469c70e1eb44661b45306e25a6fb3))
* reduce excess whitespace on query page in narrow screen mode ([33e4060](https://github.com/knowledgepixels/nanodash/commit/33e40605114ac0fe6bcb74924a5237d5d2a03e39))
* reduce forcedGet timeout and use exponential backoff ([#401](https://github.com/knowledgepixels/nanodash/issues/401)) ([d486a61](https://github.com/knowledgepixels/nanodash/commit/d486a61f23ad01fbcc866802eb4607fa900ea38e))
* replace unbounded thread creation with shared thread pool ([#401](https://github.com/knowledgepixels/nanodash/issues/401)) ([a068d1b](https://github.com/knowledgepixels/nanodash/commit/a068d1bbeb16b79f7fb83eb38ad8b2a3c8c771e3))
* responsive form fields and fullstop in nanopub statements ([4f8268c](https://github.com/knowledgepixels/nanodash/commit/4f8268cb0e7828920f929bdc7f151688787cdb65))
* update tests for Guava Cache and handle null query ID ([#401](https://github.com/knowledgepixels/nanodash/issues/401)) ([b1ffd3d](https://github.com/knowledgepixels/nanodash/commit/b1ffd3d2fa94cab6d11f9f43e9717e43d302fa79))
* use ONE_PASS_RENDER so browser refresh creates fresh pages ([#401](https://github.com/knowledgepixels/nanodash/issues/401)) ([e03c615](https://github.com/knowledgepixels/nanodash/commit/e03c61594ad17ffe4d6c1361cc9ab757f6c45afb))
* wrap session formMap with synchronizedMap for thread safety ([#401](https://github.com/knowledgepixels/nanodash/issues/401)) ([9f928f8](https://github.com/knowledgepixels/nanodash/commit/9f928f838d6969a7bc6aa9cb256e96c8f184822f))

### General maintenance

* setting next snapshot version [skip ci] ([71e6e5b](https://github.com/knowledgepixels/nanodash/commit/71e6e5b9378508b891ce125a5aa23dbfa8211d0b))

## [4.18.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.17.0...nanodash-4.18.0) (2026-03-14)

### Features

* add mobile navigation toggle using logo icon ([#83](https://github.com/knowledgepixels/nanodash/issues/83)) ([2f0aa1b](https://github.com/knowledgepixels/nanodash/commit/2f0aa1bf324f4e3783caba1b26c1100a19a57086))
* add responsive CSS rules behind max-width 768px breakpoint ([#83](https://github.com/knowledgepixels/nanodash/issues/83)) ([15453a6](https://github.com/knowledgepixels/nanodash/commit/15453a637477f12ae9769464de0c96b61e8845e6))
* enable responsive layout — add viewport meta tag, remove min-width ([#83](https://github.com/knowledgepixels/nanodash/issues/83)) ([98f9b4c](https://github.com/knowledgepixels/nanodash/commit/98f9b4ceecf0a1728940017cdd56f146f0fc44b5))
* make Pinned section read-only and hide when empty ([#393](https://github.com/knowledgepixels/nanodash/issues/393)) ([8df01f0](https://github.com/knowledgepixels/nanodash/commit/8df01f0704c1a6d4ebf26284b2f6036458790f52))
* navigate directly to user/space/resource pages for their URIs ([#396](https://github.com/knowledgepixels/nanodash/issues/396)) ([ea9cf9d](https://github.com/knowledgepixels/nanodash/commit/ea9cf9dc2edb3f7745cc1534c2c8e344941e4edf))
* show start and end time for single-day Spaces ([#390](https://github.com/knowledgepixels/nanodash/issues/390)) ([5b75041](https://github.com/knowledgepixels/nanodash/commit/5b7504117f6677942ee610246b5c94b18b9ed817))

### Bug Fixes

* align paneltitlerow buttons with flex layout ([cc422f5](https://github.com/knowledgepixels/nanodash/commit/cc422f5254ad31bb2dab13391f848ac73c58fd65))
* ensure periodic refresh triggers recalculation of Space roles and members ([#391](https://github.com/knowledgepixels/nanodash/issues/391)) ([584ede6](https://github.com/knowledgepixels/nanodash/commit/584ede6a619acfba6afb8b5ff1a036179099e0b0))
* remove noisy unconditional log in Space.triggerDataUpdate() ([e53308c](https://github.com/knowledgepixels/nanodash/commit/e53308c26e646d49058f97e9e5ac98017d8f9d1a))
* responsive polish — dropdowns, tooltips, tables, form elements ([#83](https://github.com/knowledgepixels/nanodash/issues/83)) ([4d8635b](https://github.com/knowledgepixels/nanodash/commit/4d8635b7eb04c37c036720ccaada351ee9627e67))
* responsive table vertical scroll and panel title dropdown alignment ([0bc284a](https://github.com/knowledgepixels/nanodash/commit/0bc284a52b61bdf35a263f6fbeb17b9a621e958c))
* show view action buttons to all users, restrict to owner on user pages ([3b2c8e5](https://github.com/knowledgepixels/nanodash/commit/3b2c8e57cc7fffa16adbd13875196dba5a6e5888))

### Documentation

* Added important note for docker-compose.yml ([6c58f7f](https://github.com/knowledgepixels/nanodash/commit/6c58f7f7cf5bf2bd6c9232aee8ed367c35ab2e2c))

### General maintenance

* setting next snapshot version [skip ci] ([685794c](https://github.com/knowledgepixels/nanodash/commit/685794c3736145f081f3b97616fd9d42e9fbc22c))

## [4.17.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.16.0...nanodash-4.17.0) (2026-03-13)

### Features

* replace title bar nav with logo dropdown menu ([#293](https://github.com/knowledgepixels/nanodash/issues/293)) ([caafcef](https://github.com/knowledgepixels/nanodash/commit/caafcefe6c9ce809375e62bf789d07946cc4b83f))
* show user profile image icon in title bar ([#293](https://github.com/knowledgepixels/nanodash/issues/293)) ([323377b](https://github.com/knowledgepixels/nanodash/commit/323377bdc8db3ca8bd9410f09f59b3b331a3555b))
* use icon logo in title bar and full logo on home page ([#293](https://github.com/knowledgepixels/nanodash/issues/293)) ([fd779e1](https://github.com/knowledgepixels/nanodash/commit/fd779e14f7f9435e061d9aa29386673d95dde0ba))
* **UserPage:** add dropdown menu with Explore and Show Full Channel ([#387](https://github.com/knowledgepixels/nanodash/issues/387)) ([ebef061](https://github.com/knowledgepixels/nanodash/commit/ebef0612f0b7bf1ebca88ba956306f5667282b1f))
* **UserPage:** move "+ view display" button to top section ([#387](https://github.com/knowledgepixels/nanodash/issues/387)) ([cb51957](https://github.com/knowledgepixels/nanodash/commit/cb51957933e4682a8481baa6fd77e8b5534d8f88))
* **ViewDisplayMenu:** add deactivate option and post-publish redirect ([#387](https://github.com/knowledgepixels/nanodash/issues/387)) ([716b23d](https://github.com/knowledgepixels/nanodash/commit/716b23d6b1e7d60dd623b8e680af510ac776386d))

### Dependency updates

* **core-deps:** update tools.jackson.dataformat:jackson-dataformat-yaml dependency to v3.1.0 ([4f0382e](https://github.com/knowledgepixels/nanodash/commit/4f0382ea3ae7537d93344aebaf26f132aae9f221))
* **deps:** bump release dependencies ([522ce36](https://github.com/knowledgepixels/nanodash/commit/522ce36ffa5d6517977d3ce707dba82f2cf4843e))

### Bug Fixes

* **ApiCache:** honor forced flag and refresh view list on deactivate ([#387](https://github.com/knowledgepixels/nanodash/issues/387)) ([bebea58](https://github.com/knowledgepixels/nanodash/commit/bebea58212ddee916468e069237957a866eab09e))
* **ProfileLicenseItem:** update default license template ([e1f5d50](https://github.com/knowledgepixels/nanodash/commit/e1f5d50f62817cbaf91b8af790945fba85b03e0e))
* restrict view action buttons to admins and own profile ([#387](https://github.com/knowledgepixels/nanodash/issues/387)) ([9707c37](https://github.com/knowledgepixels/nanodash/commit/9707c371488ea78cd315ae1febf65faeb88a96e8))
* **style:** fix URI and Explore button layout on user page ([#385](https://github.com/knowledgepixels/nanodash/issues/385)) ([0940234](https://github.com/knowledgepixels/nanodash/commit/09402345829f09780a15266f4019e7afd745097e))
* **style:** prevent extra whitespace on non-square profile images ([64c4b65](https://github.com/knowledgepixels/nanodash/commit/64c4b65524153d6fc9ee3ea6e70721a768e97db9))
* **ViewDisplay:** fix deactivation filtering for all views ([#387](https://github.com/knowledgepixels/nanodash/issues/387)) ([2596828](https://github.com/knowledgepixels/nanodash/commit/259682845b2ebc5cd4d773cb0349597c6d9cb857))

### General maintenance

* **chore-deps:** update org.wicketstuff:* dependency to v10.8.0 ([5897703](https://github.com/knowledgepixels/nanodash/commit/5897703887ca29ec091f35fd8923091890282088))
* setting next snapshot version [skip ci] ([ad1adb7](https://github.com/knowledgepixels/nanodash/commit/ad1adb7ecae2aa8ad3e6947fff48b52bc777293b))

### Style improvements

* append "..." to all publish-page button and menu labels ([#387](https://github.com/knowledgepixels/nanodash/issues/387)) ([28ee2c5](https://github.com/knowledgepixels/nanodash/commit/28ee2c5d280f789b312de9c1ef77fc2d3f05bd23))

### Refactoring

* move "+ view display" button to top section on all pages ([#387](https://github.com/knowledgepixels/nanodash/issues/387)) ([c30fa10](https://github.com/knowledgepixels/nanodash/commit/c30fa10b60bcdb8b82bef30102edccab12791a35))
* **NanodashPreferences:** update to use YAMLMapper and handle JacksonException ([66d885d](https://github.com/knowledgepixels/nanodash/commit/66d885d999faa7a06b158dbd23804d73344f08fd))
* **StatusLine:** standardize status line identifiers and improve HTML structure ([7a3ddda](https://github.com/knowledgepixels/nanodash/commit/7a3ddda1251d0e997b0890f9c058e13b86a66787))

## [4.16.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.15.2...nanodash-4.16.0) (2026-03-11)

### Features

* **AgentChoiceItem:** implement dynamic user icon rendering based on user selection ([c90d867](https://github.com/knowledgepixels/nanodash/commit/c90d8679eb1663835c103a4a74c71ac21256caa7))
* **ItemListElement:** add profile pic rendering when available ([f3b3294](https://github.com/knowledgepixels/nanodash/commit/f3b32944b86a27a21e343698d621219cf6b8a3a3))
* **Profile:** add profile image and license components to user profile page ([9befdaf](https://github.com/knowledgepixels/nanodash/commit/9befdaf3fd3f4130147735843eb12697b9e574ae))
* **QueryApiAccess:** add endpoint constant for retrieving user default license ([2ce6cdb](https://github.com/knowledgepixels/nanodash/commit/2ce6cdbc34a89c4d74951059964bef5318bfb601))
* **QueryPage:** support CONSTRUCT queries ([#380](https://github.com/knowledgepixels/nanodash/issues/380)) ([eade858](https://github.com/knowledgepixels/nanodash/commit/eade858ece492b1d6fb4ccd47585f6367928aced))
* **Template:** add user default license processing ([dec8967](https://github.com/knowledgepixels/nanodash/commit/dec896758aa3db0891686163b262446b7e97dfa1))
* **UserData:** add method to retrieve default license for a user ([2f6a42f](https://github.com/knowledgepixels/nanodash/commit/2f6a42f9ad1135fdd0a4d3d2c70c54bee44a2216))
* **UserPage:** add retrieval and url rendering for user that have a profile picture defined ([6eb1fae](https://github.com/knowledgepixels/nanodash/commit/6eb1fae278bf2254f53b1641379b73daefd9cc64))

### Bug Fixes

* **AgentChoiceItem:** update user icon handling ([33fb6ed](https://github.com/knowledgepixels/nanodash/commit/33fb6edea6dbf2816189f429285a020072b60124))
* Forgotten word in documentation of backup keys. ([a91b2c1](https://github.com/knowledgepixels/nanodash/commit/a91b2c16194d67b85681cd1fd28182ff16f9c00b))
* Issue with passing the encryption password to openssl ([8c12173](https://github.com/knowledgepixels/nanodash/commit/8c12173dce41ca7c0a4da931b59ff45d4b0c0d53))
* **ItemListElement:** userIcon was never added to the page with a null userIri ([986af9a](https://github.com/knowledgepixels/nanodash/commit/986af9afa49721990c973ace331af4d9f5330282))
* **NanodashPage:** remove pagetitle to allow it to be dynamic ([84698cc](https://github.com/knowledgepixels/nanodash/commit/84698cca6f8039e74795667315b46387f4abbb03))
* **PublishForm:** update license template URL and adjust related references ([46b9f47](https://github.com/knowledgepixels/nanodash/commit/46b9f47a3f261627cf9580d218696b3a081b04f5))
* Show view action buttons on list views for resources without a space ([6d60802](https://github.com/knowledgepixels/nanodash/commit/6d608029f9a5fd9b3c1648051a5b6bbe69e0a1b5)), closes [#376](https://github.com/knowledgepixels/nanodash/issues/376)
* **style:** add light gray background color to user-icon element ([2844266](https://github.com/knowledgepixels/nanodash/commit/284426655a0eee7ceb517c1c39bfd6b78397e160))
* **style:** add light gray background color to user-profile-pic element ([d40fde1](https://github.com/knowledgepixels/nanodash/commit/d40fde147c786a70a8f4e9fbf9fb44109dd9ee18))
* **style:** add mask to profile pictures as done on knowledgepixels.com ([fb271a7](https://github.com/knowledgepixels/nanodash/commit/fb271a7b67827c5fa6f899204c3c2693788b183d))
* **style:** show profile pics as rounded images in the user page ([609f87a](https://github.com/knowledgepixels/nanodash/commit/609f87a9252182ad6e8572fcbcd849c603a07db7))

### Documentation

* Added info for building the backup-keys docker image ([2f0a2a6](https://github.com/knowledgepixels/nanodash/commit/2f0a2a6bcdbc6e2209e300e0defbe98cc7990518))
* minor fix in documentation of the backup-keys feature ([45247de](https://github.com/knowledgepixels/nanodash/commit/45247de7b80e54385f8ed1a00016b3046cf1b9b8))

### Build and continuous integration

* **deps:** update actions/checkout action to v6.0.2 ([42d916f](https://github.com/knowledgepixels/nanodash/commit/42d916ffa7e92a538c81380f69376fb6210cb84b))

### General maintenance

* **KPXL_TERMS:** add `HAS_DEFAULT_LICENSE` constant to KPXL_TERMS for default license handling ([0178834](https://github.com/knowledgepixels/nanodash/commit/0178834811b9a581e623532efe898ed11a00ca28))
* **NanodashPage:** add stylesheet link in the page header rendering method with current version for cache busting ([9681021](https://github.com/knowledgepixels/nanodash/commit/96810211fb067a0a68bd7a21f9639ad0d8722468))
* **NanodashPage:** create base HTML NanodashPage ([65b7d1d](https://github.com/knowledgepixels/nanodash/commit/65b7d1d081d1e8012f1826d6f084a38f2f62b62f))
* **pages:** add style.css link for local static preview ([02d79f6](https://github.com/knowledgepixels/nanodash/commit/02d79f6504f2bc580a90c82553b8d2cb27e24274))
* remove `update-css-version.sh` script - not needed anymore ([8059209](https://github.com/knowledgepixels/nanodash/commit/8059209d10f251b1d5820764b2b12332b1f0e899))
* setting next snapshot version [skip ci] ([fc78737](https://github.com/knowledgepixels/nanodash/commit/fc78737c16d6cf86ebc02061e18e5efe79c176a4))
* **UserPage, UserListPage:** update code and style for user icon rendering ([7938c37](https://github.com/knowledgepixels/nanodash/commit/7938c370856d3aaf3bbb51eccea48ddc10e66ee0))

### Refactoring

* consolidate appliesTo method across resource classes for improved consistency ([a8c52ca](https://github.com/knowledgepixels/nanodash/commit/a8c52ca6083dfcb09967a64ce7c215f3ea5815d0))
* **PublishForm, TemplateFormPreview:** replace string constants with public static final fields for template URLs ([1ac7fc5](https://github.com/knowledgepixels/nanodash/commit/1ac7fc55ba9f1fa9a59caa0cd8e8222b5f6b083f))
* **PublishForm:** improve error logging for missing publication info template parameters ([6358df4](https://github.com/knowledgepixels/nanodash/commit/6358df41478029d784a08c5dbdeaef25aab27c02))
* **PublishForm:** standardize variable naming for publication info templates ([8fc430f](https://github.com/knowledgepixels/nanodash/commit/8fc430fc3a971c6036efc07f29ee30aa8ee9fec6))
* rename old references to `profiledResource` with new term `resourceWithProfile` for consistency ([e0cab78](https://github.com/knowledgepixels/nanodash/commit/e0cab788d15bb8ab0b6ebfb78e6be2b231d3dd67))
* simplify context resource checks by utilizing AbstractResourceWithProfile ([6fa5481](https://github.com/knowledgepixels/nanodash/commit/6fa5481c11f80aa05d34058992bb343057067a3d))
* **SpacePage:** update constructor ([0a1e075](https://github.com/knowledgepixels/nanodash/commit/0a1e075b832224082de0d6f2adf393f8931f2d21))
* update HTML pages by using wicket extend ([242d0a4](https://github.com/knowledgepixels/nanodash/commit/242d0a4c778de9369d058ad71c80a512612138c1))
* update links in HTML files to use relative paths ([4babb00](https://github.com/knowledgepixels/nanodash/commit/4babb00c4fcc116c0e9438bfd606ad6d6478bfbd))
* update method name casing for pubkey hash retrieval across multiple files ([fa9c2e7](https://github.com/knowledgepixels/nanodash/commit/fa9c2e7daa378c99b9ca34b6527c91bed2a142a6))
* **UserPage:** update HTML page by using wicket extend ([5fdfd1e](https://github.com/knowledgepixels/nanodash/commit/5fdfd1eeb90868bf62f988651825051f6b95c555))

## [4.15.2](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.15.1...nanodash-4.15.2) (2026-03-05)

### Bug Fixes

* Fix race conditions in repository refresh and Space field visibility ([5ba420c](https://github.com/knowledgepixels/nanodash/commit/5ba420c2a60264da0c3b210d2fb01cddda2f85b5)), closes [#374](https://github.com/knowledgepixels/nanodash/issues/374)

### General maintenance

* setting next snapshot version [skip ci] ([996c6b1](https://github.com/knowledgepixels/nanodash/commit/996c6b18412788348577fbb24fc15e7120775a24))

## [4.15.1](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.15.0...nanodash-4.15.1) (2026-03-05)

### Bug Fixes

* Fix earlier issue (this is to trigger release) ([384ba90](https://github.com/knowledgepixels/nanodash/commit/384ba907a1eee000da71913b2dd8dc6fd1853ea8))
* Previous commit was actually fixing a bug, so tagging it here ([cc0d86f](https://github.com/knowledgepixels/nanodash/commit/cc0d86fe4487395be153a542bc63424f1d3451d6))

### General maintenance

* Make inner record serializable, resolving error messages ([8010e5d](https://github.com/knowledgepixels/nanodash/commit/8010e5d8a8d9290cbe44b557d7036ca2cbecd312))
* setting next snapshot version [skip ci] ([a462c09](https://github.com/knowledgepixels/nanodash/commit/a462c096eb08d4bf42c9d64c661d6369e8b85510))

## [4.15.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.14.0...nanodash-4.15.0) (2026-03-04)

### Features

* add `AddViewDisplayButton` component for linking to PublishPage with pre-filled parameters ([60955a0](https://github.com/knowledgepixels/nanodash/commit/60955a0700d0cd99337953c479b1a42fa4c546e8))
* add Umami analytics support (closes [#367](https://github.com/knowledgepixels/nanodash/issues/367)) ([9cdf05e](https://github.com/knowledgepixels/nanodash/commit/9cdf05e9bf431410b403f4085619c5ce800e961a))
* add Umami env vars to docker-compose.override.yml.template ([5fabe1e](https://github.com/knowledgepixels/nanodash/commit/5fabe1e03bf725735dbd1fac0e02f97287c74f7b))
* **BaseDisplayMenu:** add base menu component to be used as core for other components ([05e4dcf](https://github.com/knowledgepixels/nanodash/commit/05e4dcfed24b096c630b55e227835297f9ba364e))
* **ExternalLinkWithActionsPanel:** add panel for showing external links and allowing users to navigate/copy/explore them ([e836ded](https://github.com/knowledgepixels/nanodash/commit/e836deda0780d05b2220568c8ef36154ce2428c8))
* **ExternalLinkWithActionsPanel:** update component so to choose between shown elements (copy icon, explore button, menu) ([62604b6](https://github.com/knowledgepixels/nanodash/commit/62604b6f918ab77e599a5ba6ecd51366fbea6a6f))
* **ExternalLinkWithActionsPanel:** use now the ExploreDisplayMenu to show the explore button and the source nanopub link ([3cd71a6](https://github.com/knowledgepixels/nanodash/commit/3cd71a603ba214bd1c29aafb0c54853af244747b))
* log Umami analytics config status at startup ([83abcc8](https://github.com/knowledgepixels/nanodash/commit/83abcc8a73c6c25a81ad838d0b002aa321e22c18))
* pre-fill query params in ViewDisplayMenu 'show query' link ([7685790](https://github.com/knowledgepixels/nanodash/commit/768579056b43d0f2f26d10ceb85ed1ebdd7d23d8))
* **QueryPage:** auto-fill Yasgui placeholders with entered parameter values ([#370](https://github.com/knowledgepixels/nanodash/issues/370)) ([dc37f46](https://github.com/knowledgepixels/nanodash/commit/dc37f463a3816af80a5460ee02ec8a33759dc69a))
* **QueryParamField:** add `clearValue` method ([3eb3fec](https://github.com/knowledgepixels/nanodash/commit/3eb3fec59547f5b56b7d4f058f6e3a7b409aa531))
* refine ViewDisplayMenu — rename to adjust, supersede/derive logic, admin visibility ([0b7362a](https://github.com/knowledgepixels/nanodash/commit/0b7362ae6f7e55d7d084d72738b464b88e57ed75))
* replace "^" source link on view displays with dropdown menu ([#364](https://github.com/knowledgepixels/nanodash/issues/364)) ([4eb8211](https://github.com/knowledgepixels/nanodash/commit/4eb821179550a7413071aad0ac074f5e79109a65))

### Dependency updates

* **core-deps:** org.apache.commons:commons-exec dependency ([baff5ae](https://github.com/knowledgepixels/nanodash/commit/baff5ae2978f453981fc29d659228e0fa8fb686d))

### Bug Fixes

* **AbstractResourceWithProfile, MaintainedResourceRepository, Space:** enhance concurrency issues with volatile fields and ConcurrentHashMap ([009aa96](https://github.com/knowledgepixels/nanodash/commit/009aa9608709ce921cb9e02997f2540d68ce6f1d))
* add 30s timeout to thread.join() in Space.ensureInitialized() ([882917a](https://github.com/knowledgepixels/nanodash/commit/882917a930fcaee499f0ca6311117e945485cfcc))
* add 30s timeout to thread.join() in Space.ensureInitialized() ([dc08c53](https://github.com/knowledgepixels/nanodash/commit/dc08c53b7756b814bb402bd7ad8696dd11395b94))
* **css:** align ViewDisplayMenu button with view display buttons ([344684b](https://github.com/knowledgepixels/nanodash/commit/344684b5b7e9020fe72197ee868a7ca87e419d70))
* **css:** anchor view-selector to top of paneltitlerow ([4d14ff2](https://github.com/knowledgepixels/nanodash/commit/4d14ff2ac2f2b2993ee0f4aef16a99a2f29be70e))
* **css:** correct ViewDisplayMenu alignment in paneltitlerow and view-selector ([abd1d02](https://github.com/knowledgepixels/nanodash/commit/abd1d026f985ab99312705683b342a8cc8d02493))
* **css:** fix row-section stripe background on narrow/mobile screens ([552332b](https://github.com/knowledgepixels/nanodash/commit/552332bff8c9c0ed9269c7ab04572023ae8090af))
* **css:** fix row-section stripe background on narrow/mobile screens ([aed8072](https://github.com/knowledgepixels/nanodash/commit/aed8072180b908c8f38e3a21a2a5a23047ebed08))
* **css:** isolate paneltitlerow margin-top rule from NanopubSet view-selector ([823812c](https://github.com/knowledgepixels/nanodash/commit/823812c4755ab940988ba1e2d7050fffa2c671c3))
* **css:** prevent view-selector span rule from unhiding actionmenu-content ([94b5acc](https://github.com/knowledgepixels/nanodash/commit/94b5acc43b04adaf39249152895076d50d53556a))
* **ExternalLinkWithActionsPanel:** add SourceNanopub component as part of the component (hidden by default) ([03b9e6c](https://github.com/knowledgepixels/nanodash/commit/03b9e6c730bfb2389954a5e9d544320fb976ebe3))
* **ExternalLinkWithActionsPanel:** pass label as parameter to the ExplorePage ([149478c](https://github.com/knowledgepixels/nanodash/commit/149478cc96f203eb9078061320e0bb9836b21aa8))
* **logging:** update error logging to include exception messages ([bf0994e](https://github.com/knowledgepixels/nanodash/commit/bf0994e3c37e0a3afcd287c8f1b194359d144e4d))
* **QueryPage:** clear value of `QueryParamField` before populating to avoid multiple values in textarea after refresh ([a19d3ce](https://github.com/knowledgepixels/nanodash/commit/a19d3ced6d7013067ea31e9c3b16950c75f4729a))
* replace infinite retry loop in forcedGet() with a 60s deadline ([dae84b8](https://github.com/knowledgepixels/nanodash/commit/dae84b8e1dd779eca8efadb47a7cc168d2dd9136))
* replace infinite retry loop in forcedGet() with a 60s deadline ([a656995](https://github.com/knowledgepixels/nanodash/commit/a656995ab786897c01b0df6e7b06006caed11b67))
* set pageResource in async ApiResultComponent paths of list/paragraph builders ([6951bc4](https://github.com/knowledgepixels/nanodash/commit/6951bc4cffda75491f46fd55a6c39328d574f313))
* **SpaceRepository:** replace lock on String with lock on Object for thread safety ([2b58c86](https://github.com/knowledgepixels/nanodash/commit/2b58c8657c756ec159460af3743a8cf6b0ebd4b6))
* **style.css:** increase dimensions of action menu button and adjust margin-top for panel title row ([4d793be](https://github.com/knowledgepixels/nanodash/commit/4d793bec9b05eb84617f0e4b699538dcf2dd5200))
* use JavaScriptHeaderItem instead of StringHeaderItem for Umami ([e6dd3a3](https://github.com/knowledgepixels/nanodash/commit/e6dd3a365ddf7d376e2e10718094db3b2c101abb))
* **user-page:** conditionally show latest nanopubs for unconfigured users ([6215c67](https://github.com/knowledgepixels/nanodash/commit/6215c67abf9e67a76b246f53abcf554bb0068a32))
* **ViewDisplayMenu:** improve action menu labels ([a74c657](https://github.com/knowledgepixels/nanodash/commit/a74c6574239f7d35dc831a0fc7c4b2893b095aa6))

### Documentation

* document Umami analytics configuration in README ([1189a1c](https://github.com/knowledgepixels/nanodash/commit/1189a1c2ee11480983d9d04a232e1850c6e40884))
* update annotations for clarity and consistency ([6b9a8bb](https://github.com/knowledgepixels/nanodash/commit/6b9a8bbdc2bf1a31c161d2f6faacf1d4c57e3b3f))

### Build and continuous integration

* disable Coveralls steps until the service outage is solved (no ETA) ([bcc20f5](https://github.com/knowledgepixels/nanodash/commit/bcc20f5152774035852ea9b48d7c779596db78e1))
* re-enable Coveralls steps in maven test workflow ([1ee98f6](https://github.com/knowledgepixels/nanodash/commit/1ee98f62079b4b01744afe33599706a16602384d))
* re-enable Coveralls steps in maven test workflow ([a8c4b4a](https://github.com/knowledgepixels/nanodash/commit/a8c4b4a77df87ae16df7b3119a0bc548b3c18c9b))

### General maintenance

* add `.env` to ignore list ([4aa47d7](https://github.com/knowledgepixels/nanodash/commit/4aa47d7210384c59db84641dc1965aae0b6a80c2))
* add `SpaceFactory` for creating Space instances from API responses ([23739b4](https://github.com/knowledgepixels/nanodash/commit/23739b483cca7b48882db12ffbf1296f9577f334))
* add SpaceRepository class for managing spaces in memory ([6b507ee](https://github.com/knowledgepixels/nanodash/commit/6b507ee8150e905257781bc736208e2835f5a8db))
* cleaning code ([ad4e589](https://github.com/knowledgepixels/nanodash/commit/ad4e58967c716504af515faa9e00f7fa15bad7c9))
* **domain:** add domain model package ([adbf5d7](https://github.com/knowledgepixels/nanodash/commit/adbf5d76c4b4deb013f45615525ad259e0584c17))
* **domain:** add ResourceWithProfile interface ([772e7f4](https://github.com/knowledgepixels/nanodash/commit/772e7f49a1b133d91200ff2563f26770132879a4))
* **domain:** define ResourceWithProfile interface with essential methods ([4c917e9](https://github.com/knowledgepixels/nanodash/commit/4c917e940cb57ca47c5ecaf29c349afde1429060))
* **ExploreDisplayMenu:** add component for explore menu ([349de4a](https://github.com/knowledgepixels/nanodash/commit/349de4ac8c36e48387d2c737fae6a381b6335070))
* introduce `MaintainedResourceFactory` and `MaintainedResourceRepository` for managing maintained resources ([82ac53e](https://github.com/knowledgepixels/nanodash/commit/82ac53e7fc45100b0ea33194c2ef9010a628b712))
* **MaintainedResourceFactory:** enhance resource management with getOrCreate and removeStale methods ([34e8261](https://github.com/knowledgepixels/nanodash/commit/34e8261b8795f23c8b77a2188d719c28f5670681))
* **NanopubAction:** improve logging messages ([8a28016](https://github.com/knowledgepixels/nanodash/commit/8a280160c91ff1c45811d0e06f0b6dd5773f7bf7))
* **release:** 4.14.0 [skip ci] ([06a40ee](https://github.com/knowledgepixels/nanodash/commit/06a40ee34e2ca42579ef79b67872ad0849851d75))
* setting next snapshot version [skip ci] ([07b67c7](https://github.com/knowledgepixels/nanodash/commit/07b67c76deca451ac7ac369e5530d7685234e98a))
* setting next snapshot version [skip ci] ([5842f89](https://github.com/knowledgepixels/nanodash/commit/5842f8914da662d7583ce900bf6f14f91d953859))
* Update CSS link version ([6067596](https://github.com/knowledgepixels/nanodash/commit/60675966f75a40600794d9490e1644dd2d0f1ac4))

### Refactoring

* **AbstractResourceWithProfile:** enhance instance management and logging ([15aeb67](https://github.com/knowledgepixels/nanodash/commit/15aeb676a45053ad1fd3078538eac182831cb086))
* **domain:** rename old ResourceWithProfile class to AbstractResourceWithProfile and update references ([6c25212](https://github.com/knowledgepixels/nanodash/commit/6c25212d56e272b2205f953ef6f28d6d5e938d1f))
* **domain:** update AbstractResourceWithProfile and related classes ([6652db4](https://github.com/knowledgepixels/nanodash/commit/6652db408dcdfb550019819ccdab5ca386e314a6))
* **IndividualAgent:** relocate to domain package ([81861aa](https://github.com/knowledgepixels/nanodash/commit/81861aa0e093bf5926d3b2c22fded24ce4cedca5))
* **MaintainedResourceRepository, SpaceRepository:** streamline resource refresh logic and remove stale entries ([ffeaf24](https://github.com/knowledgepixels/nanodash/commit/ffeaf246d0479090e97706b986623a6a0b87f69d))
* **MaintainedResourceRepository:** convert static fields and methods to instance variables and methods ([44937c8](https://github.com/knowledgepixels/nanodash/commit/44937c8ff96064594f4674edc5a74df6c51f63ec))
* **menu:** add a `...component.menu` package and move all the menu components in there ([e36586c](https://github.com/knowledgepixels/nanodash/commit/e36586c9fec208b0cfc360502338dc72c1b27409))
* relocate `Project`, `User`, and `UserData` to domain package ([10d2b59](https://github.com/knowledgepixels/nanodash/commit/10d2b597d365abdaa28013a36d54270c1447d497))
* replace add view button implementation with `AddViewDisplayButton` ([d4f274b](https://github.com/knowledgepixels/nanodash/commit/d4f274b5b324ce4b84d7b79b4e9216eadac9fa8d))
* replace direct Space access with SpaceRepository methods in ExplorePage, NanodashLink, PreviewPage, ProjectPage, PublishForm, and ResourcePartPage ([7a2762b](https://github.com/knowledgepixels/nanodash/commit/7a2762b4382503a79adb5bb20ab7782e2b727886))
* replace links with `ExternalLinkWithActionsPanel` component ([f48e3db](https://github.com/knowledgepixels/nanodash/commit/f48e3db6edc6220967e765d7e2b69d5abc009ddd))
* replace MaintainedResource access and creation with MaintainedResourceRepository and MaintainedResourceFactory ([7ed7d0e](https://github.com/knowledgepixels/nanodash/commit/7ed7d0edcf9b139be327344f023dcbfd04e53208))
* replace User references with IndividualAgent for consistency ([afa2a08](https://github.com/knowledgepixels/nanodash/commit/afa2a0826eaf9c0a2c8cca7d9232995c9b78f975))
* **Space:** replace direct Space access with SpaceRepository methods ([8cce67d](https://github.com/knowledgepixels/nanodash/commit/8cce67d9db16a28a06fae71ff9a7153f86f0fa0d))
* update AbstractResourceWithProfile to use a final instances map and utilize SpaceFactory for Space creation ([508821e](https://github.com/knowledgepixels/nanodash/commit/508821ef7250d9f7ab1dd4e8ab3e7ad66aac83f0))
* **UserPage:** clean up imports ([3d3b286](https://github.com/knowledgepixels/nanodash/commit/3d3b286fd413d46acec8ebaebdd2b59d33af97e6))
* **Utils:** replace EnvironmentUtils with System.getenv for value retrieval ([124f4c9](https://github.com/knowledgepixels/nanodash/commit/124f4c97a0ce838bf3879ac7cc54a940921fd164))
* **ViewDisplayMenu:** extend new component BaseDisplayMenu ([a3c9436](https://github.com/knowledgepixels/nanodash/commit/a3c9436f495ed56ec6213ea0d5a5b30dac38a213))

## [4.14.0](https://github.com/knowledgepixels/nanodash/compare/nanodash-4.13.1...nanodash-4.14.0) (2026-02-27)

### Features

* Add template form preview on PreviewPage for assertion templates ([86f347c](https://github.com/knowledgepixels/nanodash/commit/86f347c1b430baddda1f2142224adcb6380f1a63))
* **QueryResultNanopubSet:** add functionality to control title visibility ([dd9e02c](https://github.com/knowledgepixels/nanodash/commit/dd9e02cac0df9f5caf7a26a19a6f33d6e803bf9d))
* Use different custom "display view" templates per page type ([cffb1c7](https://github.com/knowledgepixels/nanodash/commit/cffb1c7b8dbab235bb957f10a2013b7d26fca56b))
* **WicketApplication:** implement nanopub published event listener registration and notification ([e9796f5](https://github.com/knowledgepixels/nanodash/commit/e9796f5e4165d797963b2b30d958ed89a640bdda))
* **WicketApplication:** update nanopub published event handling to use listener pattern ([fee3105](https://github.com/knowledgepixels/nanodash/commit/fee31054f147ef6573f655d6114b2b45d594474a))

### Bug Fixes

* list/grid view selectors working when async loaded ([ef33651](https://github.com/knowledgepixels/nanodash/commit/ef336515accfdb30dc30f0c14baa5486cdcdd866))
* **UserPage:** adjust margin for 'Show Full Channel' button ([e803110](https://github.com/knowledgepixels/nanodash/commit/e8031109166b1ee60128ab8738abb6e7059c2021))

### Documentation

* Update README; add new demo video link ([37b1f75](https://github.com/knowledgepixels/nanodash/commit/37b1f7535f03554b6a0d987c500b263f0d43eaa5))
* Work on mock-up ([aab1126](https://github.com/knowledgepixels/nanodash/commit/aab1126a82c21d3896976486349f8345855f292e))

### Tests

* **ApiCache:** add `retrieveResponseSync` unit tests for fresh cached response ([c31553a](https://github.com/knowledgepixels/nanodash/commit/c31553aed97e10520af39cc293d0c9bbce60ed54))
* **ApiCache:** add unit test for `retrieveResponseSync` with forced flag ([a5af699](https://github.com/knowledgepixels/nanodash/commit/a5af699b26351759f7415825fe353c11c19d035e))
* **ApiCache:** add unit tests for `clearCache` method with various scenarios ([0855cd2](https://github.com/knowledgepixels/nanodash/commit/0855cd2aa9f439a441d2521835d7598d9c7059e9))
* **ApiCache:** add unit tests for `isRunning` method with various scenarios ([f371d09](https://github.com/knowledgepixels/nanodash/commit/f371d099489977252a0dab8fb2700710f9a644e8))
* **ApiCache:** add unit tests for `retrieveResponseSync` method and minor changes ([4f448b0](https://github.com/knowledgepixels/nanodash/commit/4f448b0c26e32306ef63faef9c8c31943d4018f0))
* **ApiCache:** refactor tests to use MockitoExtension and simplify setup ([7ffc7a8](https://github.com/knowledgepixels/nanodash/commit/7ffc7a8e9e08be9044b172caec78847bdfaab70a))
* **deps:** add org.mockito:mockito-junit-jupiter dependency to v5.21.0 ([bf1645f](https://github.com/knowledgepixels/nanodash/commit/bf1645f0590f7c0add615240347d584c120c15f4))

### General maintenance

* **ApiCache:** add validation on `waitMillis` parameter for clearing cache (negative values not allowed) ([7149100](https://github.com/knowledgepixels/nanodash/commit/7149100b08b5d1d85751fe82974e388cc1bc426c))
* enhance logging for values retrieval from environment variables ([92f120f](https://github.com/knowledgepixels/nanodash/commit/92f120fcac0acdf4cdaa4324b8e1c7af0891ab4e))
* **events:** add NanopubPublishedListener interface ([494cafc](https://github.com/knowledgepixels/nanodash/commit/494cafc0b07c7a6ba57b7ca20edddfa6e76ede4f))
* **events:** add NanopubPublishedPublisher interface ([82be6a5](https://github.com/knowledgepixels/nanodash/commit/82be6a561eee67333d00b11de0bbf997ec564798))
* **ListPage:** add default "np_" parameters ([832a7d1](https://github.com/knowledgepixels/nanodash/commit/832a7d11403cabb4c481d39aec7000cbd1bb39c5))
* **ListPage:** replace nanopubs list with QueryResultNanopubSet ([83cb1b3](https://github.com/knowledgepixels/nanodash/commit/83cb1b3025cc22c15060af91ae8038664a478d0b))
* **QueryApiAccess:** update GET_FILTERED_NANOPUB_LIST with new query version ([9c47b00](https://github.com/knowledgepixels/nanodash/commit/9c47b00b323192af85f837f8953e2b51ba955b14))
* **QueryResultNanopubSet:** enhance view selector functionality and styling when the title is not shown ([fd3457d](https://github.com/knowledgepixels/nanodash/commit/fd3457dc6e3939604ca404456bba9f9ae935bd6b))
* **QueryResultNanopubSet:** remove unnecessary HTML elements ([0950ddd](https://github.com/knowledgepixels/nanodash/commit/0950ddd0a954bca3bba9f7685149b04b8b5492f3))
* setting next snapshot version [skip ci] ([d4521af](https://github.com/knowledgepixels/nanodash/commit/d4521afd4c5b18adf09dd5b6a312cebd790db5f0))

### Refactoring

* **ApiCache:** improve logging messages and simplify cache handling ([0b8c135](https://github.com/knowledgepixels/nanodash/commit/0b8c135752659358edc387a0d9a22bdb8bc974c7))
* **HomePage, ListPage:** update QueryRef initialization by using queryId defined in View ([e8e9d2a](https://github.com/knowledgepixels/nanodash/commit/e8e9d2ac1b49a4d0e2ed62339cc7bdfe4568fc38))
* **PublishForm:** clean up imports and improve error logging ([82044ed](https://github.com/knowledgepixels/nanodash/commit/82044edb2d7fe7af530af9eb220650f6a8e4d3a7))
* **WicketApplication:** implement NanopubPublishedPublisher interface and enhance event notification ([fc7d39a](https://github.com/knowledgepixels/nanodash/commit/fc7d39a9efe23e4e2a00e5995aa8b79fc58f7c54))

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
