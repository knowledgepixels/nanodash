# Names a nanopublication keeps for itself

**Status:** ✅ Implemented ([#29](https://github.com/knowledgepixels/nanodash/issues/29)).

## What it is

A nanopublication names its own parts under its own namespace: the four graphs
`NanopubCreator` writes — `Head`, `assertion`, `provenance`, `pubinfo` — and the `sig`
element the signature is attached to. A resource minted beside them takes the same shape,
`<nanopub>/<name>`, so a value minted as `assertion` **is** the assertion graph rather than
a resource of its own:

- everything stated about it is stated about the graph, and resolving it gives the
  assertion;
- a resource minted as `sig` lands on the signature element, beside `npx:hasSignature`,
  `npx:hasPublicKey` and `npx:signedBy`;
- and it cannot be corrected afterwards: the nanopublication is signed and immutable, so
  the only remedy is publishing a corrected version and retracting the first.

Such a value is therefore refused while the form is open, which is the only time anything
can be done about it.

## Where it is checked

| Where | What happens |
| --- | --- |
| The field, as it is filled | a validator on the field rejects a value that would be minted here under one of those names, saying which part it collides with |
| Before publishing | `PublishForm.findReservedIdentifier` refuses, whichever way the value arrived — a choice field, a `param_…` in the link that opened the form, a locked pre-filled value |
| The template itself | `Template.getStatementErrors` reports a template that mints such a name of its own accord, so the form says the template is invalid rather than letting it be filled in |

## Two exceptions

**A value already published is a fact, not a proposal.** Whether an existing value unifies
with a placeholder is decided by `IriTextfieldItem.Validator`, and this check is
deliberately kept out of it: superseding a nanopublication that carries such a name has to
fill the form with what it says. For the same reason `findReservedIdentifier` lets through
a name the superseded or overridden nanopublication already used — it is published and
cannot be taken back, and a new version keeps the shape of the old one.

**A legacy template's node really is its own assertion graph.** Templates written before
the template node was given a name of its own are shaped that way (`sub:assertion a
nt:AssertionTemplate`), and the meta-template republishes them unchanged. That is what the
exception above is for; it is also why the rule is about the name a value is *minted*
under here, rather than about every IRI a nanopublication mentions.

## The rule

`TemplateContext.isReservedIri` is the rule itself: an IRI under **this nanopublication's**
target namespace whose local name is one of `RESERVED_LOCAL_NAMES`. The same local name
below any other prefix — a space's namespace, say — collides with nothing and is left
alone, and so is a name that only looks like one (`assertions`, `Assertion`), since the
parts are named in the case a nanopublication writes them.
