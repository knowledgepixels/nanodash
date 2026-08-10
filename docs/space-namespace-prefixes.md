# Space-/namespace-dependent prefixes in templates

**Status:** ✅ Implemented — [#571](https://github.com/knowledgepixels/nanodash/issues/571)

## Goal

Let a template mint resources under **whichever space or maintained resource the form was
opened from**, instead of a namespace the template author has to hard-code. Without this, a
template that creates, say, a dataset record under a space needs one copy per space.

A `nt:hasPrefix` value may now contain a placeholder standing for the currently applicable
space or maintained-resource namespace:

```
sub:resource nt:hasPrefix "~~SPACE~~/"     .  # https://w3id.org/space/foo/bar/
sub:resource nt:hasPrefix "~~SPACE~~/r/"   .  # https://w3id.org/space/foo/bar/r/
sub:resource nt:hasPrefix "~~NAMESPACE~~"  .  # the current maintained resource's namespace
```

- **`~~SPACE~~`** stands for the IRI of the applicable space, **without** a trailing
  separator — the template writes the separator it wants (`"~~SPACE~~/"`,
  `"~~SPACE~~/r/"`, …).
- **`~~NAMESPACE~~`** stands for the namespace of the current maintained resource
  **including** its trailing separator: its declared namespace, or its own IRI plus `/`
  when it declares none.

Only one placeholder per prefix is meaningful; `~~SPACE~~` wins if both appear.

## Where the value comes from

The base is taken from the **navigation context** — the `context` URL parameter that
Nanodash already carries across pages (`NavigationContext`), naming the space, maintained
resource, or user a page was reached under:

| Context resource | `~~SPACE~~` | `~~NAMESPACE~~` |
| --- | --- | --- |
| a space | the space's IRI | *unresolved* |
| a maintained resource | the IRI of the space maintaining it | the resource's namespace |
| a user, or no context | *unresolved* | *unresolved* |

When the context determines no base, the form renders a **picker** in front of the
placeholder's text field, offering the known spaces (for `~~SPACE~~`) or maintained
resources (for `~~NAMESPACE~~`) by label. The context always wins when it does determine
one: the picker only appears where it is actually needed, so the common case (publishing
from a space page) stays a single-field form.

## Implementation

- **`DynamicPrefix`** (`com.knowledgepixels.nanodash`) owns the placeholder constants,
  detects which one a raw prefix uses, resolves it against a context id, and lists the
  selectable bases (from `SpaceRepository.findAll()` /
  `MaintainedResourceRepository.findAll()`). `Template` is untouched: it keeps storing the
  `nt:hasPrefix` literal verbatim, and resolution is a fill-time concern.
- **`TemplateContext.getPrefix(IRI)`** is the resolved counterpart of
  `Template.getPrefix(IRI)` and is what every consumer now calls (`IriTextfieldItem`,
  `GuidedChoiceItem`, `ReadonlyItem`, the nanopub-label pattern in `PublishForm`, and
  `processValue`). It returns null both when the template declares no prefix and when it
  declares an unresolved dynamic one; `hasUnresolvedPrefix(IRI)` tells the two apart.
  The navigation context is handed to the context via `setNavigationContextId`, which
  `PublishForm` fills from its page parameters.
- **The picked base** lives in a component model keyed by the **token**, not by the
  placeholder (`TemplateContext.getPrefixModelKey(token)` → `local:prefix-base/SPACE`,
  `local:prefix-base/NAMESPACE`). Every field whose prefix depends on the same thing —
  including every repetition, and including fields with different suffixes such as
  `~~SPACE~~/` and `~~SPACE~~/r/` — therefore shares **one** model instance. That is what
  makes picking a space in one dropdown apply to all of them: the pickers share a model, so
  the same shared-model AJAX refresh the form already uses for placeholders that appear in
  several statements (`c.getDefaultModel() == choice.getModel()`) reaches every other
  picker, and each field keeps its own suffix on top of the shared base. `~~SPACE~~` and
  `~~NAMESPACE~~` keep separate models — they are different things picked from different
  lists. The base is prefillable via the URL parameter `param_<postfix>__prefix` on any of
  the sharing placeholders (first one wins).
- **A field under a dynamic prefix ignores its own `param_<postfix>`.** The field holds
  only the name below the prefix, and the namespace comes from the context or the picker;
  a parameter for it — in practice a full IRI — would land in the field and bypass the
  prefix altogether. Dropping it makes a bare `~~SPACE~~` behave exactly like
  `~~SPACE~~/r/`, whatever the prefix's trailing path is. Static prefixes are unaffected:
  `param_<postfix>` prefills them as before.
- **`IriTextfieldItem`** renders the picker (a select2 dropdown, `prefixchoice`) when the
  prefix is dynamic and the navigation context resolves nothing. The dropdown is required
  exactly when the paired text field holds something that isn't already a full URI, so an
  untouched optional field and a fully-typed-out URI both stay valid. The prefix used for
  display, validation, and unification is re-resolved on every access (`PrefixModel`), so
  picking a base updates the field without rebuilding the form.
- **Publishing** with an unresolved prefix yields **no value** for the placeholder rather
  than an IRI in the wrong namespace (`TemplateContext.processValue`); form validation
  blocks the case anyway, this is the belt-and-braces half.

## Deliberately out of scope

Both are the "maybe for later" items of [#571](https://github.com/knowledgepixels/nanodash/issues/571):

- **Always showing the picker**, with the context merely pre-selecting it. The context is
  authoritative today; a picker that can silently contradict the page you are on is a
  bigger UX decision than this issue needed.
- **Restricting the options to the user's own spaces.** The picker lists every known
  space / maintained resource; publishing into one the user has no rights on is caught
  downstream, not in the form.

Also not covered: a dynamic prefix on a **restricted-choice** or **guided-choice**
placeholder renders no picker (only `IriTextfieldItem` has one). Such a placeholder still
resolves its prefix from the navigation context, and publishes nothing when it cannot —
the natural placeholder type for "mint a resource under this space" is a plain URI
placeholder.

## Backwards compatibility

- **Old templates, new code:** no deployed template uses the placeholders, so every prefix
  is detected as static and handled exactly as before.
- **New templates, old code:** an old Nanodash treats `"~~SPACE~~/"` as a literal prefix
  and would publish IRIs starting with `~~SPACE~~/`. There is no graceful degradation
  here, so don't put the placeholders in templates shared with instances that predate this
  feature.
