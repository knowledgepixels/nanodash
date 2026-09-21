# HTML literals

**Status:** implemented ([#378](https://github.com/knowledgepixels/nanodash/issues/378),
[#583](https://github.com/knowledgepixels/nanodash/pull/583)).

## What it is

A literal declared with the `rdf:HTML` datatype is content meant to be read as HTML, not a
string that happens to contain angle brackets. Nanodash decides by the datatype, the way it
decides dates by `xsd:date`:

- **Rendering:** such a literal is shown as HTML, sanitized, and without the surrounding
  quotes or the datatype marker — the rendered content already shows what it is.
- **Writing:** a placeholder whose template declares `nt:hasDatatype rdf:HTML` is filled
  with a rich-text editor rather than a plain text area, so the author writes formatted
  text instead of typing markup.

A template says so on the placeholder:

```turtle
sub:description a nt:LongLiteralPlaceholder ;
  rdfs:label "description of the event" ;
  nt:hasDatatype rdf:HTML .
```

## Sanitizing

The markup is sanitized twice, with the same policy (`Utils.sanitizeHtml`, which keeps a
static SVG subset and drops scripting):

- **Before publishing**, when the editor's value is finalized. A nanopublication cannot be
  edited afterwards, so markup that would be dropped on display should never enter the
  assertion in the first place — this also catches whatever was pasted into the editor.
- **At render time**, because most HTML literals on the network were published elsewhere.

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
