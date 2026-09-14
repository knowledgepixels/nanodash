# New-URI placeholders

**Status:** ✅ Implemented — [#646](https://github.com/knowledgepixels/nanodash/issues/646)

## Goal

Stop a form from silently attaching a new nanopublication to somebody else's resource.

An identifier minted under the nanopublication's own namespace picks up its artifact code at
signing time, so it is unique by construction. One built from a prefix is not: fill "Defining
an open-ended Space" with the same name twice and both nanopublications claim
`https://w3id.org/spaces/my-space`. The second does not fail — it quietly becomes further
statements about the first one's space. A nanopublication cannot be edited afterwards, so
this is worth catching before publishing rather than after.

## The tag

A template marks the placeholders whose value names a resource that **does not exist yet**,
alongside the placeholder's own type:

```turtle
sub:space a nt:ExternalUriPlaceholder, nt:NewUriPlaceholder ;
  nt:hasPrefix "https://w3id.org/spaces/" ;
  rdfs:label "Space identifier" .
```

Before publishing, Nanodash asks whether any nanopublication already introduces the resulting
IRI. If one does, the publication is refused and the offending identifier is named.

## What is and is not checked

The tag is the only thing that turns the check on, and it is the template author's
declaration of intent rather than something Nanodash infers:

- **Tagged** — checked, however the value was formed. A name placed under a prefix and an IRI
  the user typed out in full are treated the same, because whether a value names something new
  is a property of the field, not of how the text was assembled.
- **Untagged** — never checked. It publishes exactly as before even if the IRI already exists.
  This is deliberate: a field can perfectly well point at a resource that exists, and most do.

Two cases are exempt even when tagged:

- **Identifiers under the nanopublication's own namespace.** The value at check time still
  carries the `~~~ARTIFACTCODE~~~` marker rather than the IRI that ends up published, and the
  artifact code makes it unique anyway.
- **Superseding and overriding.** Keeping the source's identifier is the point of both modes
  (see [fill-modes](fill-modes.md)), so finding it in use is expected, not a collision.

A query service that cannot be reached answers "not taken". A check that cannot be made is not
evidence of a collision, and publishing should not depend on the query services being up.

## Why not infer it

An earlier version of this worked it out from the shape of the form: an IRI built from a
prefix, declared as an introduced resource, not auto-escaped, not superseding. That reads
intent out of mechanics. A prefix is a formatting device, and `nt:introduces` is attached by
templates that take the IRI of a thing that already exists too — templates such as "Defining
an open-ended Space with existing URI" exist precisely for that. Inference also missed the
opposite case, a new identifier typed out in full with no prefix involved.

Because a positive result blocks publishing outright, a false positive walls somebody out of a
nanopublication they are entitled to make, and the error message would be telling them to
change an identifier they had every right to use. Making it opt-in trades coverage for not
being wrong: a template that has not asked for the check behaves as it always did.

The cost is that existing templates get nothing until they are republished with the tag. Since
templates are themselves nanopublications, that means a new version and the governance pointer
moved with it.

## Vocabulary

`nt:NewUriPlaceholder` is `https://w3id.org/np/o/ntemplate/NewUriPlaceholder`, declared in
`Template` next to the other terms awaiting a home in nanopub-java's `NTEMPLATE`.

It says more than `nt:introduces`, which a template also attaches when the user supplies the
IRI of something that already exists. "New" here is about the identifier being minted by this
form, not about the resource being unfamiliar to the person filling it in.
