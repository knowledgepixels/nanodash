# Template model from nanopub-java

**Status:** 🚧 In progress — first part of
[#936](https://github.com/knowledgepixels/nanodash/issues/936): Nanodash reads templates with
the `Template` class of nanopub-java. Filling, matching and lookups still run on Nanodash's
own code.

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
  which is `Utils.getNanopub` with its cache. The value lists are now loaded when first asked
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
