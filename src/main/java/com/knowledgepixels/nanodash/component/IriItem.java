package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.StatementItem.RepetitionGroup;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.nanopub.vocabulary.NTEMPLATE;

import java.net.URLEncoder;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A panel that displays an IRI with a label and a link to explore it.
 */
public class IriItem extends AbstractContextComponent {

    private IRI iri;

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
        super(id, rg.getContext());
        this.iri = iriP;
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
            labelString = Utils.getShortNameFromURI(iri.stringValue());
        }
        if (!statementPartId.equals(template.getFirstOccurence(iri))) {
            labelString = labelString.replaceFirst("^[aA]n? ", "the ");
        }
        if (!labelString.isEmpty() && parentId.equals("subj") && !labelString.matches("https?://.*")) {
            // Capitalize first letter of label if at subject position:
            labelString = labelString.substring(0, 1).toUpperCase() + labelString.substring(1);
        }
        labelString = labelString.replaceAll("%I%", "" + rg.getRepeatIndex());

        String iriString = iri.stringValue();
        String description = "";
        if (iri.equals(NTEMPLATE.ASSERTION_PLACEHOLDER)) {
            if (this.context.getExistingNanopub() != null) {
                iriString = this.context.getExistingNanopub().getAssertionUri().stringValue();
            } else {
                iriString = LocalUri.PREFIX + "assertion";
            }
            description = "This is the identifier for the assertion of this nanopublication.";
        } else if (iri.equals(NTEMPLATE.NANOPUB_PLACEHOLDER)) {
            if (this.context.getExistingNanopub() != null) {
                iriString = this.context.getExistingNanopub().getUri().stringValue();
            } else {
                iriString = LocalUri.PREFIX + "nanopub";
            }
            description = "This is the identifier for this whole nanopublication.";
        } else if (template.isLocalResource(iri)) {
            if (this.context.getExistingNanopub() == null) {
                iriString = iriString.replace(Utils.getUriPrefix(iriString), LocalUri.PREFIX);
            }
        }
        if (iriString.startsWith(context.getTemplateId())) {
            iriString = iriString.replace(context.getTemplateId(), "");
            if (this.context.getExistingNanopub() != null) {
                iriString = this.context.getExistingNanopub().getUri().stringValue() + iriString;
            }
            description = "This is a local identifier minted within the nanopublication.";
        }
        if (labelString.contains(" - ")) description = labelString.replaceFirst("^.* - ", "");
        add(new Label("description", description));
        add(Utils.getUriLink("uri", iriString));

        String href = null;
        if (Utils.isLocalURI(iriString)) {
            href = "";
        } else {
            href = ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(iriString, UTF_8);
        }
        ExternalLink linkComp = new ExternalLink("link", href, labelString.replaceFirst(" - .*$", ""));
        if (iri.equals(NTEMPLATE.ASSERTION_PLACEHOLDER)) {
            linkComp.add(new AttributeAppender("class", " this-assertion "));
            iri = vf.createIRI(LocalUri.PREFIX + "assertion");
        } else if (iri.equals(NTEMPLATE.NANOPUB_PLACEHOLDER)) {
            linkComp.add(new AttributeAppender("class", " this-nanopub "));
            iri = vf.createIRI(LocalUri.PREFIX + "nanopub");
        }
        add(linkComp);
        if (template.isIntroducedResource(iri) || template.isEmbeddedResource(iri)) {
            linkComp.add(AttributeAppender.append("class", "introduced"));
        }
    }

    private static ValueFactory vf = SimpleValueFactory.getInstance();

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
        String iriS = iri.stringValue().replaceFirst("^" + context.getTemplateId() + "[#/]?", LocalUri.PREFIX);
        if (context.isReadOnly()) {
            return iriS.equals(v.stringValue().replaceFirst("^" + context.getExistingNanopub().getUri() + "[#/]?", LocalUri.PREFIX));
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
