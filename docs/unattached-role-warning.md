# Warning about roles a space has not attached

**Status:** ✅ Implemented ([#648](https://github.com/knowledgepixels/nanodash/issues/648)).

## What it is

A view that lists the holders of one role — the Observers view, say — asks for that
exact role in its query:

```sparql
?ri a gen:RoleInstantiation ; npa:forSpace ?_space_multi_iri ; npa:forAgent ?user_iri ;
    gen:hasRole <https://w3id.org/np/RAqAgIgZ…/observer-role> .
```

A space only holds such role instantiations once the role is attached to it
(`gen:hasRole`). Display the view on a space that has not attached the role and it lists
nothing at all, however many grants of that role have been published — which reads as
"nobody is an observer" rather than as "this space has no observer role".

The About tab therefore says so, above everything else: one line per view and the role it
is waiting for, linked so it can be looked at.

## How the role is found

`View.getPinnedRoles()` reads the view's query rather than the view's metadata, since the
role is named in the SPARQL and nowhere else: the query is parsed and every `gen:hasRole`
pattern whose object is an IRI rather than a variable contributes one role. A view that
leaves the role open (one listing every role-holder of a space) is pinned to none and is
never warned about, and so is a query that cannot be parsed, which has other problems.

`AboutSpacePanel.unattachedRoles` pairs each of the space's top-level view displays with
the roles it is pinned to that the space has not attached. The admin role needs no
attaching and is in `Space.getRoles()` from the start, so a view pinned to it is not
reported.

## Implementation

| Piece | Where |
| --- | --- |
| Roles a view is pinned to | `View.getPinnedRoles()`, `View.rolesPinnedBy(String)` |
| Comparison with the space | `AboutSpacePanel.unattachedRoles(Space, String)` |
| The warning itself | `AboutSpacePanel.roleWarning(...)`, `AboutSpacePanel.html`, `.message.warning` |
