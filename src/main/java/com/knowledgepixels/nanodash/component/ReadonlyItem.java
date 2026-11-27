package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.StatementItem.RepetitionGroup;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import com.knowledgepixels.nanodash.template.ContextType;
import org.nanopub.templates.Template;
import org.apache.commons.codec.Charsets;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.vocabulary.NTEMPLATE;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * ReadonlyItem is a component that displays a read-only item in the form.
 */
public class ReadonlyItem extends AbstractContextComponent {

    private static final int LONG_LITERAL_LENGTH = 100;
    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private IModel<String> model;
    private String prefix;
    private ExternalLink linkComp;
    private Label extraComp, languageComp, datatypeComp;
    private IModel<String> extraModel, languageModel, datatypeModel;
    private IRI iri;
    private Label showMoreLabelLiteral, showMoreLabelHTML;
    private final Template template;

    /**
     * Constructor for ReadonlyItem.
     *
     * @param id              the component id
     * @param parentId        the parent id (e.g., "subj", "obj")
     * @param iriP            the IRI of the item
     * @param statementPartId the statement part ID
     * @param rg              the repetition group
     */
    public ReadonlyItem(String id, String parentId, final IRI iriP, IRI statementPartId, final RepetitionGroup rg) {
        super(id, rg.getContext());
        this.iri = iriP;
        template = context.getTemplate();
        model = (IModel<String>) context.getComponentModels().get(iri);
        boolean modelIsNew = false;
        if (model == null) {
            model = Model.of("");
            context.getComponentModels().put(iri, model);
            modelIsNew = true;
        }
        String postfix = Utils.getUriPostfix(iri);
        if (modelIsNew && context.hasValue(postfix)) {
            model.setObject(context.getValue(postfix));
        }
        if (model.getObject().isEmpty() && template.isRootNanopubPlaceholder(iri) && context.getReferenceNanopub() == null) {
            model.setObject(LocalUri.of("nanopub").stringValue());
        }

        final Map<String, String> foafNameMap;
        if (context.getExistingNanopub() == null) {
            foafNameMap = new HashMap<>();
        } else {
            foafNameMap = Utils.getFoafNameMap(context.getExistingNanopub());
        }

        prefix = context.getPrefix(iri);
        if (prefix == null) prefix = "";
        add(new Label("prefix", new Model<String>() {

            @Override
            public String getObject() {
                String prefixLabel = template.getPrefixLabel(iri);
                String v = getFullValue();
                if (prefixLabel == null || IndividualAgent.isUser(v) || foafNameMap.containsKey(v)) {
                    return "";
                } else if (Utils.isUriValue(prefixLabel)) {
                    // Hide prefix label that leaks through as a raw URL.
                    return "";
                } else {
                    if (!prefixLabel.isEmpty() && parentId.equals("subj")) {
                        // Capitalize first letter of label if at subject position:
                        prefixLabel = prefixLabel.substring(0, 1).toUpperCase() + prefixLabel.substring(1);
                    }
                    return prefixLabel;
                }
            }

        }));

        linkComp = new ExternalLink("link", new Model<String>() {

            @Override
            public String getObject() {
                String obj = getFullValue();
                if (obj == null) return "";
                if (obj.equals(LocalUri.of("nanopub").stringValue())) {
                    if (context.getExistingNanopub() != null) {
                        obj = context.getExistingNanopub().getUri().stringValue();
                        return ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(obj, Charsets.UTF_8);
                    } else {
                        return "";
                    }
                } else if (obj.equals(LocalUri.of("assertion").stringValue())) {
                    if (context.getExistingNanopub() != null) {
                        obj = context.getExistingNanopub().getAssertionUri().stringValue();
                        return ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(obj, Charsets.UTF_8);
                    } else {
                        return "";
                    }
                } else if (Utils.isUriValue(obj)) {
                    return NanodashLink.getPageUrl(obj);
                } else {
                    return "";
                }
            }

        }, new Model<String>() {

            @Override
            public String getObject() {
                String obj = getFullValue();
                if (rootResolvesToThisNanopub()) {
                    // No prior value migrated (e.g. supersede with a newer template that added this slot),
                    // or we are deriving. Either way the filler makes the new nanopub its own
                    // root definition, so surface that to the user in the form.
                    return "this nanopublication";
                }
                if (obj != null && obj.equals(LocalUri.of("nanopub").stringValue())) {
                    return "this nanopublication";
                }
                if (obj != null && obj.equals(LocalUri.of("assertion").stringValue())) {
                    return context.getType() == ContextType.ASSERTION ? "this assertion" : "the assertion above";
                }
                if (obj != null && Utils.isUriValue(obj)) {
                    IRI objIri = vf.createIRI(obj);
                    if (iri.equals(NTEMPLATE.CREATOR_PLACEHOLDER)) {
                        // TODO We might want to introduce a "(you)" flag here at some point
                        return User.getShortDisplayName(objIri);
                    } else if (isAssertionValue(objIri)) {
                        if (context.getType() == ContextType.ASSERTION) {
                            return "this assertion";
                        } else {
                            return "the assertion above";
                        }
                    } else if (isNanopubValue(objIri)) {
                        return "this nanopublication";
                    } else if (IndividualAgent.isUser(obj)) {
                        return User.getShortDisplayName(objIri);
                    } else if (foafNameMap.containsKey(obj)) {
                        return foafNameMap.get(obj);
                    }
                    Space space = SpaceRepository.get().findById(obj);
                    if (space != null) return Utils.truncateLinkLabel(space.getLabel());
                    space = SpaceRepository.get().findByAltId(obj);
                    if (space != null) return Utils.truncateLinkLabel(space.getLabel());
                    MaintainedResource mr = MaintainedResourceRepository.get().findById(obj);
                    if (mr != null) return Utils.truncateLinkLabel(mr.getLabel());
                    return Utils.truncateLinkLabel(getLabelString(objIri));
                }
                return obj;
            }

        });
        if (template.isIntroducedResource(iri) || template.isEmbeddedResource(iri)) {
            linkComp.add(AttributeAppender.append("class", "introduced"));
        }
        linkComp.add(NavigationContext.hrefContextFallback());
        add(linkComp);
        add(new Label("description", new Model<String>() {

            @Override
            public String getObject() {
                String obj = getFullValue();
                if (rootResolvesToThisNanopub()) {
                    return "This is the identifier for this whole nanopublication.";
                }
                if (obj != null && Utils.isUriValue(obj)) {
                    IRI objIri = vf.createIRI(obj);
                    if (isAssertionValue(objIri)) {
                        return "This is the identifier for the assertion of this nanopublication.";
                    } else if (isNanopubValue(objIri)) {
                        return "This is the identifier for this whole nanopublication.";
                    } else if (context.isReadOnly() && obj.startsWith(context.getExistingNanopub().getUri().stringValue())) {
                        return "This is a local identifier minted within the nanopublication.";
                    }
                    String labelString = getLabelString(objIri);
                    String description = "";
                    if (labelString.contains(" - ")) description = labelString.replaceFirst("^.* - ", "");
                    return description;
                } else if (obj != null && obj.startsWith("\"")) {
                    return "(this is a literal)";
                }
                return "";
            }

        }));
        Model<String> uriModel = new Model<String>() {

            @Override
            public String getObject() {
                String obj = getFullValue();
                if (rootResolvesToThisNanopub()) {
                    return LocalUri.of("nanopub").stringValue();
                }
                if (obj != null && obj.startsWith("\"")) return "";
                if (isAssertionValue(obj)) {
                    return getAssertionValue();
                } else if (isNanopubValue(obj)) {
                    return getNanopubValue();
                }
                return obj;
            }

        };
        add(Utils.getUriLink("uri", uriModel));
        extraModel = Model.of("");
        extraComp = new Label("extra", extraModel);
        extraComp.setVisible(false);
        add(extraComp);
        languageModel = Model.of("");
        languageComp = new Label("language", languageModel);
        languageComp.setVisible(false);
        add(languageComp);
        datatypeModel = Model.of("");
        datatypeComp = new Label("datatype", datatypeModel);
        datatypeComp.setVisible(false);
        add(datatypeComp);

        showMoreLabelLiteral = new Label("show-more-literal", "");
        add(showMoreLabelLiteral);
        showMoreLabelLiteral.setVisible(false);

        showMoreLabelHTML = new Label("show-more-html", "");
        add(showMoreLabelHTML);
        showMoreLabelHTML.setVisible(false);

        Value shown = context.getShownValue(iri);
        if (shown != null) show(shown);
    }

    /**
     * Shows the value the nanopublication of a read-only view has in this position: an IRI
     * without the prefix of the placeholder, and a literal in quotes, with its language or
     * datatype, as HTML where it is HTML, and folded where it is long.
     *
     * @param v the value
     */
    private void show(Value v) {
        String vs = v.stringValue();
        if (v instanceof IRI) {
            if (!prefix.isEmpty() && vs.startsWith(prefix)) {
                vs = vs.substring(prefix.length());
            }
            if (template.isAutoEscapePlaceholder(iri)) {
                // Before decoding, so the exact IRI can be given back unchanged:
                // see TemplateContext.autoEscape.
                context.setAutoEscapeSource(iri, vs);
                vs = Utils.urlDecode(vs);
            }
            model.setObject(vs);
        } else if (v instanceof Literal vL) {
            if (vs.length() >= LONG_LITERAL_LENGTH) {
                linkComp.add(AttributeAppender.append("class", "long-literal collapsed"));
                showMoreLabelLiteral.setVisible(true);
            }
            boolean renderAsHtml = renderAsHtml(vL);
            boolean showDatatype = !vL.getDatatype().equals(XSD.STRING);
            model.setObject("\"" + vs + "\"");
            if (vL.getLanguage().isPresent()) {
                languageModel.setObject("(" + Literals.normalizeLanguageTag(vL.getLanguage().get()) + ")");
                languageComp.setVisible(true);
            } else if (showDatatype) {
                datatypeModel.setObject("(" + Utils.getDatatypeLabel(vL.getDatatype()) + ")");
                datatypeComp.setVisible(true);
            }
            if (renderAsHtml) {
                linkComp.setVisible(false);
                extraModel.setObject(Utils.sanitizeHtml(vs));
                extraComp.setEscapeModelStrings(false);
                extraComp.setVisible(true);
                showMoreLabelLiteral.setVisible(false);
                // Only content that is actually too long to show is cut off and given the
                // "show more" arrow, and its length is the length of the text rather than of
                // the markup carrying it. Whether it then really doesn't fit is settled in the
                // browser, which is the only place that knows how many lines it takes
                // (adjustLongLiterals in nanodash.js).
                if (Utils.htmlToPlainText(vs).length() >= LONG_LITERAL_LENGTH) {
                    extraComp.add(AttributeAppender.append("class", "long-literal collapsed"));
                    showMoreLabelHTML.setVisible(true);
                }
            }
        }
        String obj = getFullValue();
        if (isAssertionValue(obj)) {
            linkComp.add(new AttributeAppender("class", "this-assertion"));
        } else if (isNanopubValue(obj)) {
            linkComp.add(new AttributeAppender("class", "this-nanopub"));
        } else if (context.getExistingNanopub() != null) {
            Nanopub np = context.getExistingNanopub();
            if (NanopubUtils.getIntroducedIriIds(np).contains(obj) || NanopubUtils.getEmbeddedIriIds(np).contains(obj)) {
                linkComp.add(AttributeAppender.append("class", "introduced"));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finalizeValues() {
    }

    private String getLabelString(IRI iri) {
        if (template.getLabel(iri) != null) {
            return template.getLabel(iri).replaceFirst(" - .*$", "");
        } else if (context.getLabel(iri) != null) {
            return context.getLabel(iri).replaceFirst(" - .*$", "");
        } else {
            return Utils.getShortNameFromURI(iri.stringValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromContext() {
        // Nothing to be done here.
    }

    private String getFullValue() {
        String s = model.getObject();
        if (s == null) return null;
        if (template.isAutoEscapePlaceholder(iri)) {
            s = context.autoEscape(iri, s);
        }
        if (!prefix.isEmpty()) {
            s = prefix + s;
        }
        return s;
    }

    // True when the root-nanopub placeholder will resolve to the new nanopub itself:
    // either no prior root value was migrated, or we are deriving (which resets the root
    // to the new nanopub, as the filler of nanopub-java does / issue #527).
    private boolean rootResolvesToThisNanopub() {
        if (!template.isRootNanopubPlaceholder(iri)) return false;
        String obj = getFullValue();
        return obj == null || obj.isEmpty() || context.getFillMode() == PublishForm.FillMode.DERIVE;
    }

    private boolean isNanopubValue(Object obj) {
        if (obj == null) return false;
        if (obj.toString().equals(LocalUri.of("nanopub").stringValue())) return true;
        // A fillSource match means a *prior* nanopub (supersede/derive), not "this" one — don't label it as such.
        if (context.getExistingNanopub() == null) return false;
        return obj.toString().equals(context.getExistingNanopub().getUri().stringValue());
    }

    private String getNanopubValue() {
        Nanopub ref = context.getReferenceNanopub();
        if (ref != null) {
            return ref.getUri().stringValue();
        } else {
            return LocalUri.of("nanopub").stringValue();
        }
    }

    private boolean isAssertionValue(Object obj) {
        if (obj == null) return false;
        if (obj.toString().equals(LocalUri.of("assertion").stringValue())) return true;
        // A fillSource match means a *prior* assertion (supersede/derive), not "this" one.
        if (context.getExistingNanopub() == null) return false;
        return obj.toString().equals(context.getExistingNanopub().getAssertionUri().stringValue());
    }

    private String getAssertionValue() {
        Nanopub ref = context.getReferenceNanopub();
        if (ref != null) {
            return ref.getAssertionUri().stringValue();
        } else {
            return LocalUri.of("assertion").stringValue();
        }
    }

    /**
     * Decides whether a literal's content is to be rendered as HTML rather than escaped.
     *
     * <p>The datatype decides: {@code rdf:HTML} on the literal itself, or declared by the
     * template for this placeholder, means HTML. Only when the template declares no
     * datatype at all do we fall back to the pattern heuristic, which covers nanopubs
     * published before HTML content was tagged, and those made without a template
     * (see issue #378).
     *
     * @param literal the literal to be displayed
     * @return true if the content is to be rendered as HTML
     */
    private boolean renderAsHtml(Literal literal) {
        if (Utils.isHtmlLiteral(literal)) return true;
        // A language-tagged literal is an rdf:langString, so a datatype the template
        // declares alongside the tag does not apply to it.
        IRI declaredDatatype = literal.getLanguage().isPresent() ? null : template.getDatatype(iri);
        if (declaredDatatype != null) return RDF.HTML.equals(declaredDatatype);
        return Utils.looksLikeHtml(literal.stringValue());
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String toString() {
        return "[read-only IRI item: " + iri + "]";
    }

}
