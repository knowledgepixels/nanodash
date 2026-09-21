package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.template.TemplateContext;
import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.eclipse.rdf4j.model.IRI;

import java.util.regex.Pattern;

/**
 * A rich-text editor for literals declared with the {@code rdf:HTML} datatype (issue #378).
 * Such a literal is rendered as HTML wherever it is shown, so it is also written as HTML
 * here, rather than leaving the author to type markup into a plain text area.
 * <p>
 * The editor is Trix, which edits a normal hidden form field: it keeps the field in sync
 * with what is typed, so the value reaches the form the way any other literal does. The
 * formatting it offers — headings, bold, italic, strikethrough, links, lists, quotes and
 * code — is what {@link Utils#sanitizeHtml(String)} keeps.
 * <p>
 * The markup is sanitized when the value is finalized, with the same policy that renders it
 * later, so a nanopublication cannot carry markup that would be dropped on display — and
 * pasted content brings no scripting into the assertion. A nanopublication cannot be edited
 * after publication, which is why this happens before signing rather than only at render
 * time.
 */
public class LiteralHtmlEditorItem extends LiteralTextfieldItem {

    private static final WebjarsCssResourceReference TRIX_CSS =
            new WebjarsCssResourceReference("trix/current/dist/trix.css");
    private static final WebjarsJavaScriptResourceReference TRIX_JS =
            new WebjarsJavaScriptResourceReference("trix/current/dist/trix.umd.min.js");
    private static final JavaScriptResourceReference HTML_EDITOR_JS =
            new JavaScriptResourceReference(WicketApplication.class, "script/html-editor.js");

    private static final String BLANK = "(?:\\s|&nbsp;|&#0*160;|&#[xX]0*[aA]0;|\u00A0|<br\\s*/?>)+";
    private static final String UP_TO_THE_END = "(?=(?:</[A-Za-z][^>]*>\\s*)*\\z)";

    private static final Pattern BLANK_AT_THE_END = Pattern.compile(BLANK + UP_TO_THE_END);
    private static final Pattern EMPTY_ELEMENT_AT_THE_END =
            Pattern.compile("<([A-Za-z][A-Za-z0-9]*)(?:\\s[^<>]*)?>\\s*</\\1>" + UP_TO_THE_END);
    private static final Pattern BLANK_AT_THE_START =
            Pattern.compile("\\A((?:<(?!br)[A-Za-z][^>]*>\\s*)*)" + BLANK);
    private static final Pattern ESCAPED_APOSTROPHE = Pattern.compile("&#0*39;|&#[xX]0*27;|&apos;");

    private HiddenField<String> valueField;

    /**
     * Constructor for an HTML literal editor item.
     *
     * @param id       the component id
     * @param iri      the IRI of the literal placeholder
     * @param optional whether the field is optional
     * @param context  the template context
     */
    public LiteralHtmlEditorItem(String id, final IRI iri, boolean optional, TemplateContext context) {
        super(id, iri, optional, context);
        add(createEditorElement(iri, context.getTemplate().getLabel(iri)));
    }

    /**
     * Creates the editor element, pointed at the form field it edits.
     *
     * @param iri   the IRI of the literal placeholder
     * @param label the placeholder's label, shown while the editor is empty, or null
     * @return the editor element
     */
    private WebMarkupContainer createEditorElement(IRI iri, String label) {
        WebMarkupContainer editorElement = new WebMarkupContainer("editor");
        editorElement.add(AttributeModifier.replace("input", (IModel<String>) valueField::getMarkupId));
        if (label != null) {
            editorElement.add(AttributeModifier.replace("placeholder", label));
        }
        markAsLockedIfNeeded(editorElement, iri);
        return editorElement;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractTextComponent<String> initTextComponent(IModel<String> model) {
        valueField = new HiddenField<>("editorinput", model) {

            @Override
            public String getInput() {
                return cleanUp(super.getInput());
            }

        };
        valueField.setOutputMarkupId(true);
        return valueField;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractTextComponent<String> getTextComponent() {
        return valueField;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Adds the editor's own resources, so that a form without an HTML literal in it pays
     * for none of them.
     */
    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(TRIX_CSS));
        response.render(JavaScriptHeaderItem.forReference(TRIX_JS));
        response.render(JavaScriptHeaderItem.forReference(HTML_EDITOR_JS));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Cleans up a value the form was opened with, the way what is typed is cleaned up on its way
     * in.
     */
    @Override
    public void finalizeValues() {
        IModel<String> model = getTextComponent().getModel();
        model.setObject(cleanUp(model.getObject()));
        super.finalizeValues();
    }

    /**
     * Sanitizes the markup and takes the blank edges off it. This runs on the way into the form,
     * so that what is published, shown and validated is the cleaned-up markup: a nanopublication
     * cannot be edited afterwards, and whatever is in it at signing time stays in it.
     *
     * @param html the markup as it arrives, or null
     * @return the cleaned-up markup, or null if there was none
     */
    private static String cleanUp(String html) {
        if (html == null || html.isBlank()) return html;
        return keepApostrophes(trimBlankEdges(Utils.sanitizeHtml(html)));
    }

    /**
     * Writes an apostrophe as itself rather than as the escape the sanitizer gives every one of
     * them. Nothing needs the escape: the sanitizer quotes attribute values with double quotes,
     * and in text an apostrophe is an ordinary character. What is published reads as it was
     * written.
     *
     * @param sanitizedHtml the markup as the sanitizer wrote it
     * @return the markup with its apostrophes spelled out
     */
    private static String keepApostrophes(String sanitizedHtml) {
        return ESCAPED_APOSTROPHE.matcher(sanitizedHtml).replaceAll("'");
    }

    /**
     * Removes what writing in the editor leaves at the ends of the markup and nothing reads: a
     * space typed after the last word, which the editor has to write as a non-breaking space for
     * it to survive at all, an empty last paragraph, a line break at the end.
     *
     * @param html the sanitized markup
     * @return the markup without blank edges, which is empty if nothing else was in it
     */
    private static String trimBlankEdges(String html) {
        String trimmed = html;
        String previous = null;
        while (!trimmed.equals(previous)) {
            previous = trimmed;
            trimmed = BLANK_AT_THE_END.matcher(trimmed).replaceAll("");
            trimmed = EMPTY_ELEMENT_AT_THE_END.matcher(trimmed).replaceAll("");
            trimmed = BLANK_AT_THE_START.matcher(trimmed).replaceAll("$1");
        }
        return trimmed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[HTML literal editor item]";
    }

}
