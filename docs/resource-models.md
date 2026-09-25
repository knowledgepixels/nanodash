# Resources as models, not as fields (#459)

**Status:** ✅ Implemented — `ViewList`, `ButtonList`, `SpaceUserList` and
`ItemListPanel` take the resource as an `IModel` and read it when they render.
Follow-up to [#456](https://github.com/knowledgepixels/nanodash/issues/456)
(merged in [#457](https://github.com/knowledgepixels/nanodash/pull/457)).

## The problem

Wicket's default `REDIRECT_TO_BUFFER` render strategy, restored in #457, makes a
browser refresh render the **stored page instance** rather than a fresh one.
That is what preserves a partly-filled publish form across F5.

It also means that whatever the component tree holds in its fields is restored
with it. #456 stopped the *pages* from holding a `Space` or `MaintainedResource`
directly, but the components those pages handed the resource to still captured
it:

```java
// before
public ViewList(String markupId, AbstractResourceWithProfile resourceWithProfile) {
    ...
    List<ViewDisplay> viewDisplays = resourceWithProfile.getTopLevelViewDisplays();
    add(new ListView<>("groups", group(viewDisplays)) { ... });
}
```

Two things went stale there, not one:

1. **The resource object.** The captured reference does not share identity with
   the live singleton in `SpaceRepository` / `MaintainedResourceRepository`, so
   a restored page renders from a snapshot.
2. **Everything derived from it at construction time.** The view displays, their
   grouping, their section anchors, which buttons the viewer may use, who holds
   which role — all computed once, in the constructor, and then serialized.

Fixing only the first would have left the second: a model that reloads is no use
to a `ListView` built over a list that was computed once.

## The change

The components take `IModel<? extends AbstractResourceWithProfile>` and pass it
to `super(markupId, model)`, so the resource is the component's own default
model. Everything derived from it moves out of the constructor and into a
detachable model or an overridden `isVisible()`:

```java
// after
public ViewList(String markupId, IModel<? extends AbstractResourceWithProfile> resource) { ... }

private final IModel<Layout> layoutModel = new LoadableDetachableModel<Layout>() {
    @Override
    protected Layout load() {
        return layOut();
    }
};
```

`Layout` is the whole derivation — view displays, groups, anchors, and whether
the views are due a refresh — worked out afresh on every request and dropped at
the end of it.

| Component | What it now reads at render time |
| --- | --- |
| `ViewList` | which view displays there are, how they group, their section anchors, whether the queries are due a refresh, the empty notice |
| `ButtonList` | which member-only and admin-only buttons the viewer may use |
| `SpaceUserList` | the roles of the space and who holds them |
| `ItemListPanel` | the resource its buttons carry as page context and act on |

## Passing a model in

Pages that already hold a `LoadableDetachableModel` for their resource
(`SpacePage.spaceModel`, `MaintainedResourcePage.resourceModel`,
`HomePage.homeResourceModel`) pass it straight through. `UserPage` and
`ResourcePartPage` gained one over their own resolver.

Each of those models resolves through the repository that owns the resource
(`SpaceRepository.findById`, `MaintainedResourceRepository.findLastKnownById`,
`IndividualAgent.get`), so it follows the live singleton rather than a copy of
it. A plain `Model.of(resource)` is right only where the object is not a
repository singleton — in tests, for instance.

## What this does not cover

The view panels a `ViewList` builds inside `populateItem` (`HeaderViewPanel`,
`QueryFormPanel`, the `QueryResult*` builders) still take the resource as an
object. They are built at render time, so they get a current one each time the
list repopulates; what they keep only matters for their own Ajax updates.
Converting that chain is a larger change and was left alone.
