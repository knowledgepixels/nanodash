# Template model from nanopub-java

**Status:** 🚧 In progress — part of
[#936](https://github.com/knowledgepixels/nanodash/issues/936): Nanodash reads templates with
the `Template` class of nanopub-java, and the library's filler makes each nanopublication a
second time, for comparison (see *The fill guard*). Filling, matching and lookups still run on
Nanodash's own code.

What a template means was implemented in Nanodash alone. nanopub-java now has it too
([nanopub-java#91](https://github.com/Nanopublication/nanopub-java/issues/91),
[#216](https://github.com/Nanopublication/nanopub-java/issues/216)), so that a program can
check and fill a template without a form. As long as both implement it, they can come to
disagree; #936 moves Nanodash onto the library step by step.

## What has moved

`Template`, `DatePrecision`, `DateValue` and `MalformedTemplateException` are the classes of
`org.nanopub.templates` (nanopub-java 1.97.0 or later). The class here was the origin of the
one in the library, which kept its method names. Other documents in this folder refer to
`Template.java` and line numbers in it; those are to its former place in
`com.knowledgepixels.nanodash.template`.

`TemplateData`, `TemplateContext`, the form components and the `lookup` package are
unchanged.

The form reads a nanopublication it is filled from (`use`, `supersede`, `derive` and the
others) with the library's `SourceReader` (nanopub-java 1.100.0 or later), in
`template/SourceFill`: the values it finds start the fields, as parameters of a link do.
The read-only view of a nanopublication is matched against its templates with the library's
`TemplateMatcher`, in `template/ViewFill`: a statement is shown once for each repetition the
matcher assigned triples to, and its fields show the values it bound. The form components no
longer unify values with RDF, except for the default value a template gives a field, which
the editable fields still put in with their `isUnifiableWith`/`unifyWith`.

## What Nanodash does itself

- **Descriptions are sanitized here.** `Template.getDescription()` and
  `getStatementDescription()` return the text as published, which can hold any HTML. Whatever
  shows a description without escaping takes it from `TemplateDescriptions`, never from the
  template directly.
- **Lookups stay here.** A template says which sources a guided-choice placeholder names
  (`getPossibleValuesApis`); asking them is `LookupApis`, called from `GuidedChoiceItem`, until
  nanopub-java has lookups
  ([nanopub-java#218](https://github.com/Nanopublication/nanopub-java/issues/218)).
- **Retrieval goes through Nanodash.** A template gets its own nanopublication, and those that
  hold the values of a placeholder (`nt:possibleValuesFrom`), from `TemplateData.RESOLVER`,
  which is `Utils.getNanopub` with its cache. It first strips an IRI within a nanopublication
  down to the nanopublication (`Utils.stripToNanopubId`), as the compliance check asks for a
  template by the IRI a nanopublication names it with, which can be its node
  (`<np>/templateVersion`). The value lists are now loaded when first asked
  for, not when the template is read.

One difference in behaviour: a label the template itself gives to an IRI now wins over a label
from a values nanopublication. Of the 635 templates compared, five show another label for some
value because of this.

## The compliance guard

`TemplateComplianceGuard` runs the library's check on every nanopublication the publishing form
makes, on publishing and on preview, and writes one line to the log for each, plus one line per
finding. It decides nothing: publishing goes ahead whatever is found, and a failure of the check
is only logged. Its purpose is to show where the form and the library disagree while both
exist. Look for `Template compliance:` in the log.

## Making the nanopublication

The publishing form makes its nanopublications with the library's `TemplateFiller`
(`PublishForm.createNanopub`), on publishing and on preview. The form gives the values; the
library binds them to the placeholders, forms the IRIs and literals, and writes the assertion,
the provenance and the publication info, with the label, the introduced and embedded resources,
the role pins, the nanopublication types, the creator's name and the site it was made at.

- **The request.** `FormRequest` turns what is filled in into the library's `FillRequest`.
  - Each field's model goes in under the name its `param_` link parameter has, with its
    language tag.
  - The fill mode and the source go in, as do the space and the namespace that dynamic
    prefixes resolve to.
  - So do the labels the form looked up, the names of agents, and the site's address.
  - A field that still holds the template's default is left out. The library takes the
    default by itself, and leaves it out with an optional statement that lacks other values,
    as the form did.
  - An auto-escape value that is left as it was filled in goes in as the full IRI the source
    spelled, which escaping the text afresh would not give back where the source wrote `%20`.
- **What stays in Nanodash.** The form refuses a nanopublication that says it is protected in
  a way registries do not recognize, and one with an identifier for a new resource that is
  already in use (`PublishForm.findTakenNewUri`, below). The field validators are still the
  form's own, so a value the library would refuse is usually caught before the form is
  submitted.
- **What the library refuses.** Where the values do not make a nanopublication of the
  templates, the library says why, and the form shows each reason as a message: a statement
  that lacks a value, a value that would be lost with an optional statement left out, a name
  the nanopublication keeps for one of its own parts, and so on.

Which IRI a field gives, for the check of new-resource identifiers while it is filled in and
for showing a name as minted here, is also formed by the library
(`TemplateContext.newBindingContext`), so that what the form checks or shows is what it
publishes.

From 5.23.2 until this step, `TemplateFillGuard` made each nanopublication a second time with
the library, next to the form's own, and logged whether the two came out the same
(`Template fill:` in the log).
