package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.StatementItem.RepetitionGroup;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.nanopub.vocabulary.NTEMPLATE;


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
            String iriString = iri.stringValue();
            Space space = SpaceRepository.get().findById(iriString);
            if (space != null) {
                labelString = space.getLabel();
            } else if (SpaceRepository.get().findByAltId(iriString) != null) {
                labelString = SpaceRepository.get().findByAltId(iriString).getLabel();
            } else if (MaintainedResourceRepository.get().findById(iriString) != null) {
                labelString = MaintainedResourceRepository.get().findById(iriString).getLabel();
            } else {
                labelString = Utils.getShortNameFromURI(iriString);
            }
        }
        if (!statementPartId.equals(template.getFirstOccurrence(iri))) {
            labelString = labelString.replaceFirst("^[aA]n? ", "the ");
        }
        if (!labelString.isEmpty() && parentId.equals("subj") && !labelString.matches("https?://.*")) {
            // Capitalize first letter of label if at subject position:
            labelString = labelString.substring(0, 1).toUpperCase() + labelString.substring(1);
        }
        boolean hadIndexPlaceholder = labelString.contains("%I%");
        labelString = labelString.replaceAll("%I%", "" + rg.getRepeatIndex());
        // Auto-number repeated local resources so each expansion of a repeatable group is
        // distinguishable (e.g. "the context-specific alias 1", "… 2"). Skip when the label
        // already injects the index via %I%. The index/count are read lazily at render time
        // (see the link model below) because they change as repetitions are added/removed.
        final boolean autoNumber = template.isLocalResource(iri) && !hadIndexPlaceholder;

        String iriString = iri.stringValue();
        String description = "";
        if (iri.equals(NTEMPLATE.ASSERTION_PLACEHOLDER)) {
            if (this.context.getExistingNanopub() != null) {
                iriString = this.context.getExistingNanopub().getAssertionUri().stringValue();
            } else {
                iriString = LocalUri.of("assertion").stringValue();
            }
            description = "This is the identifier for the assertion of this nanopublication.";
        } else if (iri.equals(NTEMPLATE.NANOPUB_PLACEHOLDER)) {
            if (this.context.getExistingNanopub() != null) {
                iriString = this.context.getExistingNanopub().getUri().stringValue();
            } else {
                iriString = LocalUri.of("nanopub").stringValue();
            }
            description = "This is the identifier for this whole nanopublication.";
        } else if (template.isLocalResource(iri)) {
            if (this.context.getExistingNanopub() == null) {
                iriString = iriString.replace(Utils.getUriPrefix(iriString), LocalUri.PREFIX);
            }
        }
        if (iriString.startsWith(context.getTemplateNanopubUri())) {
            iriString = iriString.replace(context.getTemplateNanopubUri(), "");
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
            href = NanodashLink.getPageUrl(iriString);
        }
        // Truncate over-long labels for display; the full label stays available on
        // the entity's own page (shown as its title).
        final String linkLabelBase = Utils.truncateLinkLabel(labelString.replaceFirst(" - .*$", ""));
        IModel<String> linkLabelModel;
        if (autoNumber) {
            // Append the 1-based repetition index, but only once there is more than one
            // repetition. Read lazily so the number stays correct as groups are added/removed.
            linkLabelModel = (IModel<String>) () -> {
                if (rg.getRepetitionCount() > 1) {
                    return linkLabelBase + " " + (rg.getRepeatIndex() + 1);
                }
                return linkLabelBase;
            };
        } else {
            linkLabelModel = Model.of(linkLabelBase);
        }
        ExternalLink linkComp = new ExternalLink("link", Model.of(href), linkLabelModel);
        linkComp.add(NavigationContext.hrefContextFallback());
        if (iri.equals(NTEMPLATE.ASSERTION_PLACEHOLDER)) {
            linkComp.add(new AttributeAppender("class", " this-assertion "));
            iri = LocalUri.of("assertion");
        } else if (iri.equals(NTEMPLATE.NANOPUB_PLACEHOLDER)) {
            linkComp.add(new AttributeAppender("class", " this-nanopub "));
            iri = LocalUri.of("nanopub");
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
        String iriS = iri.stringValue().replaceFirst("^" + context.getTemplateNanopubUri() + "[#/]?", LocalUri.PREFIX);
        String vs = v.stringValue();
        if (context.isReadOnly()) {
            vs = vs.replaceFirst("^" + context.getExistingNanopub().getUri() + "[#/]?", LocalUri.PREFIX);
        }
        if (iriS.contains(ARTIFACT_CODE_MARKER)) {
            // A fresh artifact code is minted for this resource at publish time, so the
            // "~~~ARTIFACTCODE~~~" marker acts as a wildcard: a source resource that already
            // carries a concrete artifact code (supersede/derive/override) must still unify.
            return vs.matches(artifactCodeUnifyRegex(iriS));
        }
        return iriS.equals(vs);
    }

    // Marker left in the IRI once StatementItem.transform has expanded a template's
    // "~~ARTIFACTCODE~~" placeholder (see StatementItem#transform).
    private static final String ARTIFACT_CODE_MARKER = "~~~ARTIFACTCODE~~~";

    // Turns an IRI holding the artifact-code marker into a regex that matches the same IRI
    // with any (trusty) artifact code in that position; all other characters stay literal.
    private static String artifactCodeUnifyRegex(String iriWithMarker) {
        String[] parts = iriWithMarker.split(java.util.regex.Pattern.quote(ARTIFACT_CODE_MARKER), -1);
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) regex.append("[A-Za-z0-9_-]+");
            regex.append(java.util.regex.Pattern.quote(parts[i]));
        }
        return regex.toString();
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
