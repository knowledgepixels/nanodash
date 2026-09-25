/* Wiring for the rich-text editor on rdf:HTML literals (issues #378, #672). Trix
   keeps its own hidden input in sync with what is typed, so the form submits the
   markup without help; what it doesn't do is anything Wicket or a nanopublication
   needs on top of that. */

/* Trix writes into the hidden input directly, without an event on it, so Wicket's
   OnChangeAjaxBehavior would never learn of the change and components bound to the
   same model would keep the old value. Leaving the editor is when a text field
   reports a change, so that is when this one reports one too. */
function reportHtmlEditorChangeToWicket(event) {
  var input = event.target.inputElement;
  if (input) input.dispatchEvent(new Event("change", {bubbles: true}));
}

/* An attachment would be stored nowhere and served from nowhere: a nanopublication
   carries the markup, not the file it refers to. */
function rejectHtmlEditorAttachment(event) {
  event.preventDefault();
}

document.addEventListener("trix-blur", reportHtmlEditorChangeToWicket);
document.addEventListener("trix-file-accept", rejectHtmlEditorAttachment);
