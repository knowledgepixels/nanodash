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

`TemplateData`, `TemplateContext`, `ValueFiller`, the form components and the `lookup`
package are unchanged.

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

## The fill guard

The next step of #936 has the form make its nanopublications with the library's
`TemplateFiller`. Before it does, `TemplateFillGuard` makes each nanopublication a second time
that way, on publishing and on preview. The form's own nanopublication is the one that is signed
and published. The guard logs whether the library's comes out the same, and if not, which
triples only one of them has, or why the library would not fill the template.

- **The request.** `FormRequest` turns what is filled in into the library's `FillRequest`.
  - Each field's model goes in under the name its `param_` link parameter has, with its
    language tag.
  - The fill mode and the source go in, as do the space and the namespace that dynamic
    prefixes resolve to.
  - So do the labels the form looked up, the names of agents, and the site's address.
  - A field that still holds the template's default is left out. The library takes the
    default by itself, and leaves it out with an optional statement that lacks other values,
    as the form does.
- **The comparison.** It is made triple by triple in each graph, with each nanopublication's
  URI and what is minted under it written relative to it. The head, the creation time and the
  signature are left out.

Look for `Template fill:` in the log.
