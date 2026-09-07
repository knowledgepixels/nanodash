# Publishing protected nanopublications

**Status:** ✅ Implemented ([#671](https://github.com/knowledgepixels/nanodash/issues/671)).

## Goal

A local/private Nanopub Registry accepts, stores and serves nanopublications typed
`npx:ProtectedNanopub`; the public network refuses them. Nanodash already *recognizes*
protected nanopublications (the 🔒 flag on a nanopublication, the 🔒 restricted marker in
the title bar). This is the other half: letting users of such a deployment **create** them.

## The marker

What registries look at is exactly one triple in the publication info:

```turtle
this: a npx:ProtectedNanopub .
```

`NanopubServerUtils.isProtectedNanopub` (nanopub-java) checks for that triple, with the
nanopublication itself as the subject, and `PublishNanopub.publish` then skips every
registry that is not a local instance — refusing outright, without sending the content,
when none is left.

### Where it is sent

`Utils.publishNanopub` addresses a protected nanopublication to `Utils.getMainRegistryUrl()`
directly, instead of letting the library pick from its own list. That list comes from
bootstrap plus discovered registries (or `NANOPUB_REGISTRY_INSTANCES`) and knows nothing
about `NANODASH_MAIN_REGISTRY`, so on a deployment configured only the Nanodash way,
publishing a protected nanopublication used to fail with "None of the available registries
is a local instance" — while the local instance was sitting right there in the config.
There is only one place a protected nanopublication can go, and Nanodash already checked
that it reports itself as a local instance, so it is named directly.

Everything else keeps going to the library's list, deliberately. Registries **pull** from
their peers rather than pushing to them, so an openly published nanopublication sent only
to a private registry would never reach the public network — the opposite of what
publishing it unprotected means. The private registry picks it up again through peering.

Two near-misses do **not** count, and both look right in a form:

- `npx:hasNanopubType npx:ProtectedNanopub`, which is what the generic "Nanopublication
  type" pubinfo element produces.
- The same `rdf:type` triple with a subject other than the nanopublication.

A nanopublication carrying only a near-miss is published to the public network. The
publish form therefore refuses to build one (`PublishForm.checkProtectedMarker`).

## The template

The marker is added by an ordinary pubinfo template,
[`RAjTlfGJgWb8K7cOspGdwodPpnu859q78Rps40xDGdgZs`](https://w3id.org/np/RAjTlfGJgWb8K7cOspGdwodPpnu859q78Rps40xDGdgZs)
("Protected nanopublication"), hard-coded as `ProtectedNanopubs.TEMPLATE_ID`. It has no
placeholders: adding it adds the marker.

Going through a template rather than emitting the triple in code buys two things: the
statement has a `nt:wasCreatedFromPubinfoTemplate` link like every other one, and
`ValueFiller` claims it when a protected nanopublication is superseded, instead of
sweeping it into the hand-coded catch-all.

The template is `nt:UnlistedTemplate`, so it does not appear in the "add element…"
dropdown. Where a nanopublication may be stored is not a description of its content, and
the publication info section is behind "show more" — this decision gets its own control
instead.

The template nanopublication itself is published **publicly**: a local registry mirrors
the public network, so a public template resolves everywhere, while a protected one would
make every nanopublication built on it unresolvable outside the local instance. A copy is
in `src/test/resources/np-protected-nanopub-template.trig`, which the tests check against
the hard-coded ID.

## The control

A checkbox where the consent checkbox is, "🔒 Protected nanopublication". Toggling it adds
or removes the pubinfo context, so the RDF still comes out of the template machinery.

**The consent checkbox is about open publication, so it is shown only for a
nanopublication that will be openly published.** Where protection is possible it says so
explicitly:

> I understand that this will be openly published, that published data cannot be fully
> removed (only retracted or superseded by new versions), and that it will be publicly
> connected to my personal identifier.

Ticking the protected box therefore takes that checkbox away rather than rewording it:
nothing goes to the public network, so there is nothing to consent to. The user has one
box, and it carries a plain statement of fact instead of a second consent text:

> This nanopublication will stay on the local instance this Nanodash is connected to.

Unticked, there is no note at all — the consent checkbox already says where this goes.
Where protection is **forced** the note gives the reason instead, and the checkbox is
disabled.

The **preview page** shows the same box, ticked and disabled, whenever the nanopublication
it is about to publish carries the marker: by then it is signed into the content and
cannot be taken back there. Its consent checkbox follows the same rule and the same
wording (`PublishForm.getConsentText()`), so it is absent for a protected preview.

Two consequences of tying consent to open publication: a protected nanopublication is
published without any box being ticked (there is only the protected box, and on a
private-by-default deployment it starts ticked), and `isConsentGiven()` — which also
feeds the preview page — is true whenever protection is on.

The plain consent text (no "openly published" clause) stays in place on deployments where
protection is not possible, since there is nothing there to contrast it with.

The protected checkbox is shown only when the main registry reports itself as a local instance
(`ServiceMode.isRegistryLocal()`), because a public registry cannot store a protected
nanopublication and the option would do nothing but make publishing fail.

## Defaults

Both kinds of local instance exist — mostly-public ones with occasional protected
content, and private-by-default ones — so the default is a deployment setting:

```
NANODASH_PROTECTED_BY_DEFAULT=true
```

(or `protectedByDefault: true` in `~/.nanopub/nanodash-preferences.yml`). It only sets
where the form starts, and has no effect where protection is not offered.

## When the user has no say

`ProtectedNanopubs.getForcedReason` decides this; the checkbox is then shown checked and
disabled, with the reason spelled out.

- **The fill source is protected** (supersede, derive, override, improve, use). The new
  nanopublication repeats the old one's content, so publishing it unprotected would be
  the thing that exposes it.
- **A template the form is built on is protected**. That template is stored on the local
  instance only, so a public nanopublication made with it would carry a
  `nt:wasCreatedFromTemplate` link nobody outside can follow.

Not covered: a nanopublication governed by a protected space. The governing space is
identified by a resource IRI rather than by the nanopublication that defines it, so
answering that would take a query; in practice the templates such a space governs are
themselves stored on the local instance, which the template rule catches.

If protection is forced but the template cannot be loaded, the form refuses to publish
rather than publishing without the marker.

## Always-protected content

A kind of content that is always protected needs no code and no user decision: an
assertion template can list the pubinfo template in `nt:hasRequiredPubinfoElement`, which
adds it to the form and makes it non-removable.

## What this does not change

Publishing *without* the marker on a local instance still shares the nanopublication with
the public network, if the local registry has public peers — which is the normal setup,
since that is how it mirrors the network. "Not protected" means public, and the consent
checkbox says so.
