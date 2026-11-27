package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.GuidedChoiceItem;
import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.templates.RawValue;
import org.nanopub.templates.SourceReader;
import org.nanopub.templates.Template;
import org.nanopub.templates.TemplateMatch;
import org.nanopub.templates.TemplateValues;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fills the contexts of a publishing form from an existing nanopublication, the source, as when
 * the form is opened to use it, to supersede it or to derive from it. The source is read for the
 * templates of the form by the reader of nanopub-java ({@link SourceReader}), and each value
 * becomes the value its field starts with ({@link TemplateContext#setSourceValue}), under the
 * name a parameter of a link would give it. A parameter still wins over the source.
 * <p>
 * This has to happen before the statements of a context are built
 * ({@link TemplateContext#initStatements()}): a field takes its value when it is made, and the
 * repetitions of a statement are made for the values there are of it.
 * <p>
 * A value is written as the form keeps it: a resource the source introduces stays as it is when
 * the source is superseded or overridden, so that the new version keeps it, the root definition
 * that is the source itself is given by the IRI of the source, and an IRI the form escapes is
 * shown as text and made again as the source spells it. The labels the source gives in its
 * publication info are kept for the fields that show them.
 */
public class SourceFill {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private final Nanopub source;
    private final FillMode fillMode;
    private final SourceReader reader;
    private final Set<String> introduced;
    private final Set<String> embedded;

    /**
     * Creates a fill from a source.
     *
     * @param source   the nanopublication the form is filled from
     * @param fillMode the mode of the form, or null for a nanopublication that is improved
     * @param creator  the person filling in the form, whom the creator placeholder stands for;
     *                 or null
     */
    public SourceFill(Nanopub source, FillMode fillMode, IRI creator) {
        this.source = source;
        this.fillMode = fillMode;
        this.reader = new SourceReader(source, creator);
        this.introduced = NanopubUtils.getIntroducedIriIds(source);
        this.embedded = NanopubUtils.getEmbeddedIriIds(source);
    }

    /**
     * Fills the context of the assertion template.
     *
     * @param context the context
     * @return the triples of the assertion the template has no statement for, as the form
     * shows them
     */
    public List<Statement> fillAssertion(TemplateContext context) {
        TemplateMatch match = reader.readAssertion(context.getTemplate());
        seed(context, match);
        return toShown(match.getUnusedTriples());
    }

    /**
     * Fills the context of the provenance template.
     *
     * @param context the context
     * @return the triples of the provenance the template has no statement for, as the form
     * shows them
     */
    public List<Statement> fillProvenance(TemplateContext context) {
        TemplateMatch match = reader.readProvenance(context.getTemplate());
        seed(context, match);
        return toShown(match.getUnusedTriples());
    }

    /**
     * Takes away what a publication info template of the source gives, without filling it in
     * anywhere, as for a template whose latest version is marked as transient.
     *
     * @param template the template
     */
    public void skipPubinfo(Template template) {
        reader.skipPubinfo(template);
    }

    /**
     * Fills contexts of publication info templates, in the order that gives each its share
     * ({@link SourceReader#readPubinfo(List)}).
     *
     * @param contexts the contexts
     */
    public void fillPubinfo(List<TemplateContext> contexts) {
        List<Template> templates = new ArrayList<>();
        for (TemplateContext context : contexts) templates.add(context.getTemplate());
        Map<Template, TemplateMatch> matches = reader.readPubinfo(templates);
        for (TemplateContext context : contexts) {
            TemplateMatch match = matches.get(context.getTemplate());
            if (match != null) seed(context, match);
        }
    }

    /**
     * @return the triples of the publication info that no template has taken so far, without
     * what the form writes by itself, as the form shows them
     */
    public List<Statement> getUnusedPubinfo() {
        return toShown(reader.getUnreadPubinfo());
    }

    /**
     * Keeps the labels the source gives in its publication info, those from lookups and those
     * of the things it names, for the fields that show them.
     */
    public void keepLabels() {
        reader.getLabelsFromApi().forEach(GuidedChoiceItem::setLabel);
        for (Statement st : source.getPubinfo()) {
            if (!st.getPredicate().equals(RDFS.LABEL) || st.getSubject().equals(source.getUri())) continue;
            GuidedChoiceItem.setLabel(st.getSubject().stringValue(), st.getObject().stringValue());
        }
    }

    private void seed(TemplateContext context, TemplateMatch match) {
        Template template = context.getTemplate();
        // What a transient template says is about the source alone.
        if (template.isPubinfoTemplate() && template.isTransient()) return;
        TemplateValues values = reader.getValues(match);
        for (String name : values.getNames()) {
            RawValue value = values.get(name);
            context.setSourceValue(name, value.text());
            if (value.languageTag() != null) {
                context.setSourceValue(name + TemplateContext.LANGUAGE_SUFFIX, value.languageTag());
            }
        }
        for (Map.Entry<IRI, Value> e : match.getContext().getBindings().entrySet()) {
            IRI key = e.getKey();
            if (!(e.getValue() instanceof IRI value) || key.stringValue().startsWith(NTEMPLATE.NAMESPACE)) continue;
            IRI templateIri = template.getTemplateIri(key);
            String name = TemplateValues.getName(key);
            String text = values.has(name) ? values.get(name).text() : null;
            String iri = value.stringValue();
            if (template.isRootNanopubPlaceholder(templateIri) && value.equals(source.getUri())) {
                // The form keeps the source as the root definition by its IRI.
                context.setSourceValue(name, iri);
            } else if (keepsIntroduced() && template.isIntroducedResource(templateIri) && introduced.contains(iri)
                    && !embedded.contains(iri) && !isMadeAgain(context, templateIri, text, iri)) {
                // A resource the source introduces keeps its IRI in a new version, where the
                // form would mint one for its name.
                context.setSourceValue(name, iri);
            } else if (template.isAutoEscapePlaceholder(templateIri) && text != null) {
                // The IRI is made again from the spelling the source has, not escaped anew. The
                // library gives an IRI escaped otherwise than it would escape it in full; the
                // field shows the text all the same.
                String escaped = getEscapedTail(iri, text);
                String prefix = context.getPrefix(templateIri);
                if (text.equals(iri) && prefix != null && !prefix.isEmpty() && iri.startsWith(prefix)) {
                    escaped = iri.substring(prefix.length());
                    try {
                        context.setSourceValue(name, Utils.urlDecode(escaped));
                    } catch (IllegalArgumentException ex) {
                        escaped = null;
                    }
                }
                if (escaped != null) context.setAutoEscapeSource(key, escaped);
            }
        }
    }

    // Whether the form makes the IRI again from the text of its field: an IRI in full, or a
    // name after the prefix of the placeholder, escaped as the source has it where the
    // placeholder escapes it. A name of a local resource is minted anew.
    private static boolean isMadeAgain(TemplateContext context, IRI templateIri, String text, String iri) {
        if (text == null) return false;
        if (text.equals(iri)) return true;
        if (context.getTemplate().isLocalResource(templateIri) && !context.hasDynamicPrefix(templateIri)) return false;
        String prefix = context.getPrefix(templateIri);
        if (prefix == null || prefix.isEmpty() || !iri.startsWith(prefix)) return false;
        String name = iri.substring(prefix.length());
        if (name.equals(text)) return true;
        return context.getTemplate().isAutoEscapePlaceholder(templateIri) && name.equals(getEscapedTail(iri, text));
    }

    // The end of an IRI that decodes to the text of its field.
    private static String getEscapedTail(String iri, String text) {
        for (int i = 0; i < iri.length(); i++) {
            String tail = iri.substring(i);
            try {
                if (Utils.urlDecode(tail).equals(text)) return tail;
            } catch (IllegalArgumentException ex) {
                // Not a tail that decodes; a longer one may not either, a shorter one may.
            }
        }
        return null;
    }

    private boolean keepsIntroduced() {
        return fillMode == FillMode.SUPERSEDE || fillMode == FillMode.OVERRIDE;
    }

    // The triples nothing took, written as the form shows a nanopublication being filled in:
    // the source and its assertion by the names they have in the form, and what is minted
    // within the source by its name, but for what a new version keeps.
    private List<Statement> toShown(List<Statement> triples) {
        List<Statement> shown = new ArrayList<>();
        for (Statement st : triples) {
            shown.add(vf.createStatement((Resource) toShown(st.getSubject()), (IRI) toShown(st.getPredicate()),
                    toShown(st.getObject()), (Resource) toShown(st.getContext())));
        }
        shown.sort(Comparator.comparing(st -> st.getSubject() + " " + st.getPredicate() + " " + st.getObject()));
        return shown;
    }

    private Value toShown(Value value) {
        if (value == null) return null;
        if (source.getUri().equals(value)) return LocalUri.of("nanopub");
        if (source.getAssertionUri().equals(value)) return LocalUri.of("assertion");
        if (value instanceof IRI iri) {
            String s = iri.stringValue();
            boolean kept = introduced.contains(s) && keepsIntroduced() && !embedded.contains(s);
            if (!kept && s.startsWith(source.getUri().stringValue())) return LocalUri.of(Utils.getUriPostfix(s));
        }
        return value;
    }

}
