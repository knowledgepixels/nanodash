# Contributing to Nanodash

A big welcome and thank you for considering contributing to **Nanodash**! It's people like you that
make it a reality for users in our community.

Reading and following these guidelines will help us make the contribution process easy and effective for everyone
involved. It also communicates that you agree to respect the time of the developers managing and developing these open
source projects. In return, we will reciprocate that respect by addressing your issue, assessing changes, and helping
you finalize your pull requests.

## Quicklinks

* [Getting Started](#getting-started)
    * [Issues](#issues)
    * [Pull Requests](#pull-requests)
    * [Commit Formatting Guidelines](#commit-formatting-guidelines)
* [Release Process](#release-process)
    * [Versions Management](#versions-management)
    * [Changelogs](#changelogs)
    * [Publishing](#publishing)

## Getting Started

Contributions are made to this repo via Issues and Pull Requests (PRs). A few general guidelines that cover both:

- Search for existing Issues and PRs before creating your own.
- We work hard to make sure issues are handled in a timely manner but, depending on the impact, it could take a while
  to investigate the root cause. A friendly ping in the comment thread to the submitter or a contributor can help draw
  attention if your issue is blocking.

### Issues

Issues should be used to report problems with the library, request a new feature, or to discuss potential changes before
a PR is created. When you create a new Issue, please be sure to provide as much relevant information as possible. This
includes:

- A descriptive title and detailed description of the issue.
- Specifying its Type (Bug, Feature, Task).
- Steps to reproduce the issue (if applicable).
- Expected and actual results.
- Any relevant logs, error messages, or screenshots.
- Environment details (library version, OS, browser, etc.).
- Code samples or snippets (if applicable).
- Any other information that might help us understand and address the issue.

If you find an Issue that addresses the problem you're having, please add your own reproduction information to the
existing issue rather than creating a new one.

### Pull Requests

PRs to this library are always welcome and can be a quick way to get your fix or improvement slated for the next
release. In general, PRs should:

- Only fix/add the functionality in question **OR** address wide-spread whitespace/style issues, not both.
- Add unit or integration tests for fixed or changed functionality (if a test suite already exists).
- Address a single concern in the least number of changed lines as possible.

For changes that address core functionality or would require breaking changes (e.g. a major release), it's best to open
an Issue to discuss your proposal first. This is not required but can save time creating and reviewing changes.

In general, we follow the ["fork-and-pull" Git workflow](https://github.com/susam/gitpr)

1. Fork the repository to your own Github account
2. Clone the project to your machine
3. Create a branch locally with a succinct but descriptive name
4. Commit changes to the branch following the [commit formatting guidelines](#commit-formatting-guidelines) specific to
   this repo
5. Push changes to your fork
6. Open a PR in this repository

### Commit Formatting Guidelines

This repository follows the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification for
commit
messages. This helps us automate the release process and generate changelogs easily. Here are the basic rules:

- Use the following structure for commit messages:

  ```
  <type>[optional scope]: <description>
  
  [optional body]
  
  [optional footer(s)]
  ```
- **Type**: The type of change being made. For an exhaustive list of supported types, please refer to
  the [following list](https://github.com/DanySK/semantic-release-preconfigured-conventional-commits?tab=readme-ov-file#configuration).
  Common types include:
    - `feat`: A new feature
    - `fix`: A bug fix
    - `chore`: Changes to the build process or auxiliary tools and libraries
- **Scope**: A scope may be provided to a commit's type, to provide additional contextual information and is
  contained within parentheses, e.g., `feat(parser): add ability to parse arrays`.
- **Description**: A brief summary of the changes made in the commit.

Using this format is **mandatory** in order to make automated releases work correctly. If your commit does not follow
these
guidelines, please specify it in the PR description so maintainers can help adjust it during the review process.

## Release Process

Releases are managed automatically using [semantic-release](https://semantic-release.gitbook.io/semantic-release/). All
the PRs and new changes are pushed to the `master` branch before being released. When a new release is to be made, the
changes are merged into the `release` branch, and a new release will be created (if applicable) based on the commit
messages since the last release.

### Versions Management

This project uses [Semantic Versioning](https://semver.org/) to manage versions. The versions are automatically managed
by `semantic-release` based on the commit messages when releasing. **Do not manually** change the version in any files.

### Changelogs

Changelogs are automatically generated using the commit messages since the last release. They are included in the
release notes on GitHub and in the `CHANGELOG.md` file in the repository.

### Publishing

Releases are automatically published to GitHub Releases and a Docker image is published to Docker Hub when a release is
made.
