package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.StatementItem.RepetitionGroup;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.nanopub.vocabulary.NTEMPLATE;

import java.net.URLDecoder;
import java.net.URLEncoder;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A panel that displays an IRI with a label and a link to explore it.
 */
public class IriItem extends Panel implements ContextComponent {

    private static final long serialVersionUID = 1L;

    private IRI iri;
    private TemplateContext context;

    /**
     * Constructor for creating an IRI item with a given IRI and repetition group.
     *
     * @param id              the component ID
     * @param parentId        the parent ID (e.g., "subj", "obj")
     * @param iriP            the IRI to display
     * @param objectPosition  whether this is in the object position of a statement
     * @param statementPartId the ID of the statement part this IRI belongs to
     * @param rg              the repetition group context
     */
    public IriItem(String id, String parentId, IRI iriP, boolean objectPosition, IRI statementPartId, RepetitionGroup rg) {
        super(id);
        this.iri = iriP;
        this.context = rg.getContext();
        final Template template = context.getTemplate();
        String labelString = null;
        if (iri.equals(NTEMPLATE.ASSERTION_PLACEHOLDER)) {
            if (context.getType() == ContextType.ASSERTION) {
                labelString = "this assertion";
            } else {
                labelString = "the assertion above";
            }
        } else if (iri.equals(NTEMPLATE.NANOPUB_PLACEHOLDER)) {
            labelString = "this nanopublication";
        }
        if (template.getLabel(iri) != null) {
            labelString = template.getLabel(iri);
        } else if (iri.equals(PROV.WAS_ATTRIBUTED_TO)) {
            // temporary solution until we have full provenance graph support
            labelString = "is attributed to";
        } else if (labelString == null) {
            labelString = getShortNameFromURI(iri.stringValue());
        }
        if (!statementPartId.equals(template.getFirstOccurence(iri))) {
            labelString = labelString.replaceFirst("^[aA]n? ", "the ");
        }
        if (labelString.length() > 0 && parentId.equals("subj") && !labelString.matches("https?://.*")) {
            // Capitalize first letter of label if at subject position:
            labelString = labelString.substring(0, 1).toUpperCase() + labelString.substring(1);
        }
        labelString = labelString.replaceAll("%I%", "" + rg.getRepeatIndex());

        String iriString = iri.stringValue();
        String description = "";
        if (iri.equals(NTEMPLATE.ASSERTION_PLACEHOLDER)) {
            if (rg.getContext().getExistingNanopub() != null) {
                iriString = rg.getContext().getExistingNanopub().getAssertionUri().stringValue();
            } else {
                iriString = "local:assertion";
            }
            description = "This is the identifier for the assertion of this nanopublication.";
        } else if (iri.equals(NTEMPLATE.NANOPUB_PLACEHOLDER)) {
            if (rg.getContext().getExistingNanopub() != null) {
                iriString = rg.getContext().getExistingNanopub().getUri().stringValue();
            } else {
                iriString = "local:nanopub";
            }
            description = "This is the identifier for this whole nanopublication.";
        } else if (template.isLocalResource(iri)) {
            if (rg.getContext().getExistingNanopub() == null) {
                iriString = iriString.replace(Utils.getUriPrefix(iriString), "local:");
            }
        }
        if (iriString.startsWith(context.getTemplateId())) {
            iriString = iriString.replace(context.getTemplateId(), "");
            if (rg.getContext().getExistingNanopub() != null) {
                iriString = rg.getContext().getExistingNanopub().getUri().stringValue() + iriString;
            }
            description = "This is a local identifier minted within the nanopublication.";
        }
        if (labelString.contains(" - ")) description = labelString.replaceFirst("^.* - ", "");
        add(new Label("description", description));
        add(Utils.getUriLink("uri", iriString));

        String href = null;
        if (iriString.startsWith("local:")) {
            href = "";
        } else {
            href = ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(iriString, UTF_8);
        }
        ExternalLink linkComp = new ExternalLink("link", href, labelString.replaceFirst(" - .*$", ""));
        if (iri.equals(NTEMPLATE.ASSERTION_PLACEHOLDER)) {
            linkComp.add(new AttributeAppender("class", " this-assertion "));
            iri = vf.createIRI("local:assertion");
        } else if (iri.equals(NTEMPLATE.NANOPUB_PLACEHOLDER)) {
            linkComp.add(new AttributeAppender("class", " this-nanopub "));
            iri = vf.createIRI("local:nanopub");
        }
        add(linkComp);
        if (template.isIntroducedResource(iri) || template.isEmbeddedResource(iri)) {
            linkComp.add(AttributeAppender.append("class", "introduced"));
        }
    }

    private static ValueFactory vf = SimpleValueFactory.getInstance();

    // TODO Merge with Utils.getShortNameFromURI

    /**
     * <p>getShortNameFromURI.</p>
     *
     * @param uri a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    public static String getShortNameFromURI(String uri) {
        if (uri.startsWith("https://doi.org/")) return uri.replace("https://doi.org/", "doi:");
        if (uri.startsWith("http://dx.doi.org/")) return uri.replace("http://dx.doi.org/", "doi:");
        uri = uri.replaceFirst("\\?.*$", "");
        uri = uri.replaceFirst("[/#]$", "");
        uri = uri.replaceFirst("^.*[/#]([^/#]*)[/#]([0-9]+)$", "$1/$2");
        if (uri.contains("#")) {
            uri = uri.replaceFirst("^.*#(.*[^0-9].*)$", "$1");
        } else {
            uri = uri.replaceFirst("^.*/([^/]*[^0-9/][^/]*)$", "$1");
        }
        uri = uri.replaceFirst("((^|[^A-Za-z0-9\\-_])RA[A-Za-z0-9\\-_]{8})[A-Za-z0-9\\-_]{35}$", "$1");
        uri = uri.replaceFirst("(^|[^A-Za-z0-9\\-_])RA[A-Za-z0-9\\-_]{43}[^A-Za-z0-9\\-_](.+)$", "$2");
        uri = URLDecoder.decode(uri, UTF_8);
        return uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromContext() {
        // Nothing to be done here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnifiableWith(Value v) {
        if (!(v instanceof IRI)) return false;
        // TODO: Check that template URIs don't have regex characters:
        String iriS = iri.stringValue().replaceFirst("^" + context.getTemplateId() + "[#/]?", "local:");
        if (context.isReadOnly()) {
            return iriS.equals(v.stringValue().replaceFirst("^" + context.getExistingNanopub().getUri() + "[#/]?", "local:"));
        } else {
            return iriS.equals(v.stringValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unifyWith(Value v) throws UnificationException {
        if (!isUnifiableWith(v)) throw new UnificationException(v.stringValue());
        // Nothing left to be done here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillFinished() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finalizeValues() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[IRI item: " + iri + "]";
    }

}
