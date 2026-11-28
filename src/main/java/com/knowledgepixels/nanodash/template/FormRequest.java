package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.DynamicPrefix;
import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.GuidedChoiceItem;
import com.knowledgepixels.nanodash.component.LiteralDateItem;
import com.knowledgepixels.nanodash.component.LiteralDateTimeItem;
import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import com.knowledgepixels.nanodash.domain.User;
import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;
import org.nanopub.templates.FillRequest;
import org.nanopub.templates.RawValue;
import org.nanopub.templates.Template;
import org.nanopub.templates.TemplateValues;
import org.nanopub.vocabulary.NTEMPLATE;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Turns what is filled in on a publishing form into a request for the filler of nanopub-java
 * ({@link org.nanopub.templates.TemplateFiller}).
 * <p>
 * The form keeps the value of each placeholder in a model, under the placeholder's IRI with a
 * {@code __N} suffix for a repetition. The last part of that key is the name the value has in a
 * request, the same name as in the {@code param_} parameter that fills the form from a link. A
 * model holds the text as it is entered, which is what a request takes: a local name, a name
 * after the prefix of the placeholder, or a full IRI. An empty model says that the placeholder
 * has no value, the default of the template included, as a field that was cleared does.
 * <p>
 * What follows from the nanopublication rather than from a field is left to the filler: the
 * creator, the positions in a sequence, and the root definition where the form has none of its
 * own. The labels the form looked up, and the names of agents, go into the request as labels.
 */
public final class FormRequest {

    private static final String ARTIFACT_CODE_MARKER = "~~~ARTIFACTCODE~~~/";
    private static final String ROOT_IS_SOURCE = LocalUri.of("nanopub").stringValue();

    private FormRequest() {
    }

    /**
     * Builds the request for what a publishing form has filled in.
     *
     * @param assertion       the context of the assertion template
     * @param provenance      the context of the provenance template, or null for the one the
     *                        assertion template names, or else the default one
     * @param pubinfo         the contexts of the publication info templates
     * @param targetNamespace the namespace the form makes the nanopublication in, with or
     *                        without the artifact-code marker after it
     * @param creator         the IRI of the person filling in the form
     * @param creatorName     their name, or null
     * @param creationSite    the address of this Nanodash instance, or null
     * @return the request
     */
    public static FillRequest of(TemplateContext assertion, TemplateContext provenance, List<TemplateContext> pubinfo,
                                 String targetNamespace, IRI creator, String creatorName, String creationSite) {
        FillRequest request = new FillRequest(assertion.getTemplate().getId());
        copyValues(assertion, request.getValues(), request);
        if (provenance != null) {
            request.setProvenanceTemplate(provenance.getTemplate().getId());
            copyValues(provenance, request.getProvenanceValues(), request);
        }
        for (TemplateContext context : pubinfo) {
            copyValues(context, request.addPubinfoTemplate(context.getTemplate().getId()), request);
        }
        if (targetNamespace != null) {
            String namespace = targetNamespace.endsWith(ARTIFACT_CODE_MARKER)
                    ? targetNamespace.substring(0, targetNamespace.length() - ARTIFACT_CODE_MARKER.length()) : targetNamespace;
            request.setTargetNamespace(namespace);
        }
        FillRequest.Mode mode = getMode(assertion.getFillMode());
        Nanopub source = assertion.getFillSource();
        if (mode != null && source != null) request.setSource(mode, source.getUri().stringValue());
        request.setSpace(assertion.getPrefixBase(DynamicPrefix.SPACE_TOKEN));
        request.setNamespace(assertion.getPrefixBase(DynamicPrefix.NAMESPACE_TOKEN));
        request.setCreator(creator);
        request.setCreatorName(creatorName);
        request.setCreationSite(creationSite);
        return request;
    }

    /**
     * Returns the mode of a request for a fill mode of the form. Using a nanopublication, in
     * full or in part, only takes values from it, which the request has already.
     *
     * @param fillMode the fill mode of the form, or null
     * @return the mode, or null if the nanopublication made does not relate to the source
     */
    static FillRequest.Mode getMode(FillMode fillMode) {
        if (fillMode == null) return null;
        return switch (fillMode) {
            case SUPERSEDE -> FillRequest.Mode.SUPERSEDE;
            case OVERRIDE -> FillRequest.Mode.OVERRIDE;
            case DERIVE -> FillRequest.Mode.DERIVE;
            case USE, PARTIAL -> null;
        };
    }

    private static void copyValues(TemplateContext context, TemplateValues values, FillRequest request) {
        Template template = context.getTemplate();
        for (Map.Entry<IRI, IModel<?>> e : context.getComponentModels().entrySet()) {
            IRI key = e.getKey();
            String keyString = key.stringValue();
            // The creator and the other values the nanopublication itself gives, and the
            // models that only go with a value: its language, its prefix, its source spelling.
            if (keyString.startsWith(NTEMPLATE.NAMESPACE) || keyString.startsWith(LocalUri.PREFIX)) continue;
            if (keyString.endsWith(TemplateContext.LANGUAGE_SUFFIX) || keyString.endsWith(TemplateContext.AUTO_ESCAPE_SOURCE_SUFFIX)) continue;
            IRI templateIri = template.getTemplateIri(key);
            if (!template.isPlaceholder(templateIri) || template.isSequenceElementPlaceholder(templateIri)) continue;
            String text = toText(e.getValue() == null ? null : e.getValue().getObject());
            if (template.isRootNanopubPlaceholder(templateIri) && (text.isEmpty() || text.equals(ROOT_IS_SOURCE))) continue;
            // The form puts the default of the template into a field. Left as it is, it is not
            // a value the person gave: the filler takes the default by itself, and leaves it out
            // with its statement where that statement lacks other values, as the form does.
            if (isDefault(context, key, text)) continue;
            String languageTag = null;
            IModel<?> languageModel = context.getComponentModels().get(TemplateContext.getLanguageModelKey(key));
            if (languageModel != null && template.isLanguageTagSelectable(templateIri)) {
                languageTag = toText(languageModel.getObject());
            }
            if (!text.isEmpty() && template.isUriPlaceholder(templateIri)) addLabels(context, key, templateIri, text, request);
            if (!text.isEmpty() && template.isAutoEscapePlaceholder(templateIri)) text = keepSourceSpelling(context, key, text);
            values.set(TemplateValues.getName(key), new RawValue(text, languageTag));
        }
    }

    // An auto-escape value that is left as it was filled in keeps the spelling it was filled
    // from, which escaping the text afresh does not give back where the source wrote "%20"
    // (TemplateContext#autoEscape). The filler takes an IRI after the prefix of the
    // placeholder as it is.
    private static String keepSourceSpelling(TemplateContext context, IRI key, String text) {
        String escaped = context.autoEscape(key, text);
        if (escaped.equals(Utils.urlEncode(text))) return text;
        String prefix = context.getPrefix(key);
        if (prefix == null || prefix.isEmpty()) return text;
        return prefix + escaped;
    }

    // Whether a field holds the default value of its placeholder, in one of the ways the form
    // writes it: a literal by its text, an IRI within the template by its local name, and any
    // other IRI in full or after the prefix of the placeholder.
    static boolean isDefault(TemplateContext context, IRI key, String text) {
        if (text.isEmpty()) return false;
        Template template = context.getTemplate();
        Value value = template.getDefault(key);
        if (value == null) return false;
        if (!(value instanceof IRI)) return text.equals(value.stringValue());
        String iri = value.stringValue();
        if (text.equals(iri)) return true;
        String templateUri = template.getNanopub().getUri().stringValue();
        if (iri.startsWith(templateUri) && !iri.equals(templateUri) && text.equals(Utils.getUriPostfix(iri))) return true;
        String prefix = context.getPrefix(key);
        return prefix != null && !prefix.isEmpty() && iri.startsWith(prefix) && text.equals(iri.substring(prefix.length()));
    }

    // The labels the form knows for the IRI a field holds: those looked up for a value chosen
    // from a list, which the form keeps whatever kind of field the value is in, and the names
    // of people.
    private static void addLabels(TemplateContext context, IRI key, IRI templateIri, String text, FillRequest request) {
        String iri = text;
        if (!Utils.isUriValue(iri)) {
            String prefix = context.getPrefix(key);
            if (prefix == null || prefix.isEmpty()) return;
            iri = prefix + text;
        }
        String label = GuidedChoiceItem.getLabel(iri);
        if (label == null) label = GuidedChoiceItem.getLabel(text);
        if (label != null) request.setLabelFromApi(iri, label);
        if (context.getTemplate().isAgentPlaceholder(templateIri) && Utils.isUriValue(iri)) {
            String name = User.getName(SimpleValueFactory.getInstance().createIRI(iri));
            if (name != null) request.setLabel(iri, name);
        }
    }

    // The text a model holds, as the form writes it into a nanopublication.
    private static String toText(Object object) {
        if (object == null) return "";
        if (object instanceof ZonedDateTime dateTime) return LiteralDateTimeItem.format.format(dateTime);
        if (object instanceof Date date) {
            synchronized (LiteralDateItem.format) {
                return LiteralDateItem.format.format(date);
            }
        }
        return object.toString();
    }

}
