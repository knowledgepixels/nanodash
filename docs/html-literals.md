# HTML literals

**Status:** implemented ([#378](https://github.com/knowledgepixels/nanodash/issues/378),
[#583](https://github.com/knowledgepixels/nanodash/pull/583)).

## What it is

A literal declared with the `rdf:HTML` datatype is content meant to be read as HTML, not a
string that happens to contain angle brackets. Nanodash decides by the datatype, the way it
decides dates by `xsd:date`:

- **Rendering:** such a literal is shown as HTML, sanitized and without the surrounding
  quotes, with its datatype named beside it as any other non-string literal's is.
- **Writing:** a placeholder whose template declares `nt:hasDatatype rdf:HTML` is filled
  with a rich-text editor rather than a plain text area, so the author writes formatted
  text instead of typing markup.

A template says so on the placeholder:

```turtle
sub:description a nt:LongLiteralPlaceholder ;
  rdfs:label "description of the event" ;
  nt:hasDatatype rdf:HTML .
```

## Cleaning up

The markup is sanitized with `Utils.sanitizeHtml`, which keeps a static SVG subset and
drops scripting, at two points:

- **On the way into the form**, on every value the editor submits — in
  `LiteralHtmlEditorItem`, on the input as it arrives with the request, not when the form
  is built (which happens before anything is typed). A nanopublication cannot be edited
  afterwards, so markup that would be dropped on display must never enter the assertion in
  the first place; this also catches whatever was pasted into the editor.
- **At render time**, because most HTML literals on the network were published elsewhere.

On the way in, the value also loses what writing in an editor leaves behind and nothing
reads: a space typed after the last word, which the editor has to write as `&nbsp;` for it
to survive at all, an empty last paragraph, a trailing line break. Blanks within the markup
are left alone. Apostrophes are written as themselves rather than as the escape the
sanitizer gives each one, so the published literal reads as it was written.

## Showing it

An HTML literal is shown as a box on the statement's line, lined up with the values beside
it, with "(rdf:HTML)" beside it as an `xsd:dateTime` literal is shown with its datatype. In
the publish form the datatype is not written out: what the template asks for is the editor,
and that is what the author works in, the way a date placeholder is a date picker. Content whose *text* is longer than `ReadonlyItem.LONG_LITERAL_LENGTH` is a candidate
for being cut off with a "show more" arrow, the same way a long plain literal is — the
length of the text, not of the markup carrying it.

Whether it is then really cut off is settled in the browser, by `adjustLongLiterals` in
`nanodash.js`: a character count says nothing about how many lines the text takes at the
width it is shown at, and a sentence that fits on one line was being covered by the
fade-out with the arrow sitting on top of it. Whatever fits is shown whole; the answer is
re-checked when the window is resized. This holds for long plain literals too, which are
marked the same way.

## Legacy content

Literals published before the datatype was used carry no datatype at all. For those, the
old heuristic still applies: a value starting with a block-level tag is rendered as HTML.
A literal explicitly declared `xsd:string` is always escaped, even when it looks like
markup.

## Implementation

| Piece | Where |
| --- | --- |
| Datatype check | `Utils.isHtmlLiteral`, with `Utils.looksLikeHtml` as the legacy fallback |
| Rendering | `LiteralItem`, `ReadonlyItem` |
| Editing | `LiteralHtmlEditorItem`, chosen in `ValueItem` for `rdf:HTML` placeholders |
| Editor widget | [Trix](https://trix-editor.org/), from the `org.webjars.npm:trix` webjar |
| Editor wiring | `script/html-editor.js`, loaded by the field itself, so a form without an HTML literal loads neither |

The editor edits an ordinary hidden form field, which Trix keeps in sync with what is
typed, so the value reaches the form the way any other literal does, and a locked value
(#678) is shown in the editor without being edited there. Attachments are refused: a
nanopublication carries markup, and there is nowhere to keep a file it would point at.

The formatting Trix offers — headings, bold, italic, strikethrough, links, lists, quotes
and code — is what `Utils.sanitizeHtml` keeps.

The Kendo UI editor would have been the closer fit, since `wicketstuff-kendo-ui` is
already a dependency for the date pickers, but the editor is not part of Kendo UI Core,
which is the build that jar bundles: the widget is commercial, and the field rendered as
a plain text area with `kendoEditor is not a function` in the browser console.
