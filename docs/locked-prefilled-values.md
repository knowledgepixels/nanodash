# Locking pre-filled values and statements

**Status:** ✅ Implemented ([#678](https://github.com/knowledgepixels/nanodash/issues/678)).

## Goal

A link into the publish form can pre-fill fields with URL parameters
(`?template=…&param_public-key=…`). Some of those values are not the user's to change:
the "Create Introduction" link fills in the public key of the local key pair, and an
introduction that declares a different key is simply wrong. The `locked` parameter lets
the link say so: the field is shown with its value, but cannot be edited.

Locking is per field and, for repeatable statements, **per repetition**. That is what
lets a link pre-fill and fix the keys a user already has while leaving them free to add
more. A second parameter, `locked-statements`, fixes the *set* of repetitions instead:
no adding, no removing. The two are independent — a locked value in an unlocked
statement can still be dropped by removing its repetition, and a locked statement's
values can still be edited.

## The `locked` parameter

`locked` names the URL parameters it locks, with the same prefixes that set them:

```
/publish?template=…
  &param_public-key=<key>
  &locked=param_public-key
```

- `param_x` — assertion template, `prparam_x` — provenance template, `piparamN_x` — the
  Nth publication-info template. A name **without** a prefix refers to the assertion
  template, so `locked=public-key` and `locked=param_public-key` are the same thing.
- Several names can be given as a comma-separated list, as repeated `locked` parameters,
  or both.
- A lock on a parameter that carries no value is ignored (and logged). Locking an empty
  field would leave it empty and uneditable — for a required field, unpublishable.

### Repetitions

Repetition suffixes are part of the name, following the convention the pre-fill
parameters already use: `x` is the first repetition, `x__1` the second, and so on (the
relative `x__.1` form works too and is translated along with the value). So

```
&param_public-key=<key A>&param_public-key__1=<key B>
&locked=param_public-key,param_public-key__1
```

pre-fills two key groups, fixes both, and leaves any group the user adds with "+" fully
editable.

Only *narrow-scope* placeholders — those used in a single top-level statement — get
repetition suffixes. A placeholder used in several statements is wide-scope: it has one
shared model and no suffix, so locking it locks it everywhere. In the "Introducing a
user" template that is exactly the intent: `user` is wide-scope (it appears in the name
statement and in the key group) and locks as a whole, while `public-key` is narrow-scope
to the repeatable key group and locks per key.

## Locking statements: `locked-statements`

`locked-statements` names the statements whose repetitions are fixed. The `+` and `-`
buttons of such a statement are not rendered, so the form publishes exactly the
repetitions the link pre-filled:

```
&param_public-key=<key A>&param_public-key__1=<key B>
&locked=param_public-key,param_public-key__1
&locked-statements=public-key
```

It takes the same template prefixes as `locked` (`param_` / `prparam_` / `piparamN_`,
bare = assertion template). A statement is named either by its node in the template
(`st2`) or by a placeholder that occurs in that statement and no other (`public-key`) —
the latter is the name a link author is more likely to have at hand. A placeholder used
in several statements is wide-scope and names none of them. The name is resolved when the
lock is queried, not when it is parsed, because the statements are only built afterwards.

Unlike a locked value, a locked statement genuinely holds: Wicket does not invoke the
listener of a component that is not visible, so a hidden `+` cannot be triggered by
editing the page either.

**Optional statements** need no separate parameter to be kept: an optional statement is
dropped by leaving one of its fields empty (`StatementItem.addTriplesTo`), so a locked
value already forces it to stay. Forcing an optional statement to stay *absent* is a
different feature — hiding the field rather than locking it — and is deliberately not
part of this.

## What locking does *not* do

- **It is not enforcement.** The lock lives in the URL, so anyone can edit the URL before
  loading the form. It prevents accidental edits, not deliberate ones. A wrong key in an
  introduction is caught where it matters anyway: the signature won't match.
- **A value lock does not lock the statement.** The "-" button still removes a repetition
  group, locked value and all — you cannot change the value, but you can drop the whole
  statement. `locked-statements` is what stops that, and it is deliberately a separate
  decision.

## Implementation

- `TemplateContext` keeps a `lockedParams` set beside its parameter map, with
  `setLocked`/`isLocked`/`moveLock`/`clearLock`. `isLocked(IRI)` takes the postfix of the
  placeholder IRI as handed to the form component, which already carries the repetition
  suffix — that is where per-repetition granularity comes from for free.
- `PublishForm.applyLocks` parses both parameters into those contexts, right after the
  loop that reads the `param_`/`prparam_`/`piparamN_` values; `forEachLockedName` holds
  the prefix handling both share.
- `TemplateContext.setStatementLocked`/`isStatementLocked` keep the statement locks, whose
  names are matched against the statement node and against the placeholders narrow-scoped
  to it. `StatementItem.updateViewElements` hides the repetition buttons of a locked
  statement.
- `AbstractContextComponent.lockIfNeeded` attaches a behavior that marks the field
  uneditable in the browser, adds an explanatory `title` and a `locked-value` class, and
  decides at **render** time rather than at construction time, because the lock state can
  change while the form is open (see below). Every editable placeholder item calls it
  where it registers its form component.

  The component stays **enabled** as far as Wicket is concerned, and its value keeps being
  submitted with the form. Disabling it in Wicket looks right and is wrong: the browser
  sends nothing for a disabled control, and the form then reads the field as an emptied
  one — the locked value disappears from the field and its required-value check fails.
  (This is what the live check caught; unit tests that only rendered the form did not.)
  So text inputs and text areas are marked `readonly`, which browsers do submit, and
  choice fields — which render as `select`, and HTML has no readonly for those — are
  marked `disabled` with their value mirrored in a hidden field of the same name. Either
  way the form receives the locked value, unchanged, and publishes it as it would have
  anyway.
- `StatementItem.RepetitionGroup.remove()` shifts locks with the values. Removing a
  repetition group does not delete a slot: it shifts the values of the following groups
  up through fixed placeholder slots and drops the last one. The lock belongs to the
  pre-filled value rather than to the slot, so it has to travel with it — otherwise
  removing a locked repetition would leave the value that slides into its place
  uneditable. Only IRIs that actually get a repetition suffix are shifted, under the same
  condition `transform()` applies; the statement's constants are in the same set and one
  of them can share a placeholder's postfix (`rdfs:comment` and a `comment` placeholder,
  say), which would shift the same lock twice and undo it.

Tests: `LockedFieldTest` (per-repetition locking, lock shifting on removal, rendering),
`PublishFormLockTest` (parameter parsing).
