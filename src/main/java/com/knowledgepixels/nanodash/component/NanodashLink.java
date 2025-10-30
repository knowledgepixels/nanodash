package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.MaintainedResource;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.connector.ios.DsConfig;
import com.knowledgepixels.nanodash.connector.ios.DsNanopubPage;
import com.knowledgepixels.nanodash.connector.pensoft.BdjConfig;
import com.knowledgepixels.nanodash.connector.pensoft.BdjNanopubPage;
import com.knowledgepixels.nanodash.connector.pensoft.RioConfig;
import com.knowledgepixels.nanodash.connector.pensoft.RioNanopubPage;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.MaintainedResourcePage;
import com.knowledgepixels.nanodash.page.ResourcePartPage;
import com.knowledgepixels.nanodash.page.SpacePage;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;
import net.trustyuri.TrustyUriUtils;
import org.apache.commons.codec.Charsets;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.vocabulary.NP;
import org.nanopub.vocabulary.NTEMPLATE;

import java.net.URLEncoder;
import java.util.*;

/**
 * A Wicket component that creates a link to a nanopublication or an IRI.
 */
public class NanodashLink extends Panel {

    public NanodashLink(String id, String uri, Nanopub np, IRI templateClass, String label) {
        this(id, uri, np, templateClass, label, null);
    }

    /**
     * Creates a link to a nanopublication or an IRI.
     *
     * @param id            the Wicket component ID
     * @param uri           the URI of the nanopublication or IRI
     * @param np            the nanopublication, or null if the link is not to a nanopublication
     * @param templateClass the template class of the nanopublication, or null if the link is not to a nanopublication
     * @param label         the label to display for the link, or null to derive it from the nanopublication or IRI
     */
    public NanodashLink(String id, String uri, Nanopub np, IRI templateClass, String label, String contextId) {
        super(id);

        final List<Template> templates = new ArrayList<>();
        final Map<IRI, String> labels = new HashMap<>();
        if (np != null) {
            for (Statement st : np.getPubinfo()) {
                if (st.getPredicate().equals(NTEMPLATE.HAS_LABEL_FROM_API) || st.getPredicate().equals(RDFS.LABEL)) {
                    labels.put((IRI) st.getSubject(), st.getObject().stringValue());
                }
            }
        }

        final TemplateData td = TemplateData.get();

        if (NTEMPLATE.ASSERTION_TEMPLATE.equals(templateClass)) {
            IRI templateId = td.getTemplateId(np);
            if (templateId != null) {
                templates.add(td.getTemplate(templateId.stringValue()));
            }
        } else if (NTEMPLATE.PROVENANCE_TEMPLATE.equals(templateClass)) {
            IRI templateId = td.getProvenanceTemplateId(np);
            if (templateId != null) {
                templates.add(td.getTemplate(templateId.stringValue()));
            }
        } else if (NTEMPLATE.PUBINFO_TEMPLATE.equals(templateClass)) {
            Set<IRI> templateIds = td.getPubinfoTemplateIds(np);
            for (IRI templateId : templateIds) {
                templates.add(td.getTemplate(templateId.stringValue()));
            }
        }

        final IRI iriObj = vf.createIRI(uri);
        if (np != null && uri.equals(np.getUri().stringValue())) {
            ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "this");
            add(link);
            add(new Label("description", "this specific nanopublication"));
        } else if (np != null && uri.equals(np.getAssertionUri().stringValue())) {
            ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "this assertion");
            link.add(new AttributeAppender("class", " nanopub-assertion "));
            add(link);
            add(new Label("description", "the assertion of this specific nanopublication"));
        } else if (uri.equals(NP.HAS_ASSERTION.stringValue())) {
            ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "assertion");
            link.add(new AttributeAppender("class", " nanopub-assertion "));
            add(link);
            add(new Label("description", "links a nanopublication to its assertion"));
        } else if (uri.equals(NP.HAS_PROVENANCE.stringValue())) {
            ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "provenance");
            link.add(new AttributeAppender("class", " nanopub-provenance "));
            add(link);
            add(new Label("description", "links a nanopublication to its provenance"));
        } else if (uri.equals(NP.HAS_PUBINFO.stringValue())) {
            ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "pubinfo");
            link.add(new AttributeAppender("class", " nanopub-pubinfo "));
            add(link);
            add(new Label("description", "links a nanopublication to its pubinfo"));
        } else {
            if (label == null || label.isBlank()) {
                label = Utils.getShortNameFromURI(uri);
                if (iriObj.equals(User.getSignatureOwnerIri(np))) {
                    // TODO We might want to introduce a "(you)" flag here at some point
                    label = User.getShortDisplayName(iriObj);
                } else if (User.getName(iriObj) != null) {
                    label = User.getShortDisplayName(iriObj);
                } else {
                    for (Template template : templates) {
                        // TODO For pubinfo templates, we don't consider which triple came from which template (which is non-trivial):
                        String l = template.getLabel(iriObj);
                        if (l != null) {
                            label = l;
                            break;
                        }
                        l = labels.get(iriObj);
                        if (l != null) {
                            label = l;
                            break;
                        }
                    }
                }
            }
            String shortLabel = label.replaceFirst(" - [\\s\\S]*$", "");
            add(createLink("link", uri, shortLabel, contextId));
            String description = "";
            if (np != null && uri.startsWith(np.getUri().stringValue())) {
                description = "This is a local identifier that was minted when the nanopublication was created.";
            }
            if (label.contains(" - ")) description = label.replaceFirst("^.* - ", "");
            add(new Label("description", description));
        }
        add(Utils.getUriLink("uri", uri));
    }

    /**
     * <p>createLink.</p>
     *
     * @param markupId a {@link java.lang.String} object
     * @param uri      a {@link java.lang.String} object
     * @param label    a {@link java.lang.String} object
     * @return a {@link org.apache.wicket.Component} object
     */
    public static Component createLink(String markupId, String uri, String label, String contextId) {
        boolean isNp = TrustyUriUtils.isPotentialTrustyUri(uri);
        PageParameters params = new PageParameters().set("id", uri);
        if (contextId != null) params.set("context", contextId);
        // TODO Improve this
        if (isNp && uri.startsWith(DsConfig.get().getTargetNamespace())) {
            return new BookmarkablePageLink<Void>(markupId, DsNanopubPage.class, params.set("mode", "final")).setBody(Model.of(label));
        } else if (isNp && uri.startsWith(BdjConfig.get().getTargetNamespace())) {
            return new BookmarkablePageLink<Void>(markupId, BdjNanopubPage.class, params.set("mode", "final")).setBody(Model.of(label));
        } else if (isNp && uri.startsWith(RioConfig.get().getTargetNamespace())) {
            return new BookmarkablePageLink<Void>(markupId, RioNanopubPage.class, params.set("mode", "final")).setBody(Model.of(label));
        } else if (Space.get(uri) != null) {
            label = Space.get(uri).getLabel();
            return new BookmarkablePageLink<Void>(markupId, SpacePage.class, params).setBody(Model.of(label));
        } else if (MaintainedResource.get(uri) != null) {
            label = MaintainedResource.get(uri).getLabel();
            return new BookmarkablePageLink<Void>(markupId, MaintainedResourcePage.class, params).setBody(Model.of(label));
        } else if (isPartOfResource(uri, contextId)) {
            return new BookmarkablePageLink<Void>(markupId, ResourcePartPage.class, params.set("label", label)).setBody(Model.of(label));
        } else {
            return new BookmarkablePageLink<Void>(markupId, ExplorePage.class, params.set("label", label).set("forward-to-part", "true")).setBody(Model.of(label));
        }
    }

    private static boolean isPartOfResource(String uri, String contextId) {
        if (contextId == null) return false;
        String uriNamespace = MaintainedResource.getNamespace(uri);
        MaintainedResource resource = MaintainedResource.getByNamespace(uriNamespace);
        if (resource == null) return false;
        return resource.getId().equals(contextId);
    }

    /**
     * Creates a link to a nanopublication or an IRI.
     *
     * @param id            the Wicket component ID
     * @param uri           the URI of the nanopublication or IRI
     * @param np            the nanopublication, or null if the link is not to a nanopublication
     * @param templateClass the template class of the nanopublication, or null if the link is not to a nanopublication
     */
    public NanodashLink(String id, String uri, Nanopub np, IRI templateClass) {
        this(id, uri, np, templateClass, null);
    }

    /**
     * Creates a link to a nanopublication or an IRI.
     *
     * @param id  the Wicket component ID
     * @param uri the URI of the nanopublication or IRI
     * @param np  the nanopublication, or null if the link is not to a nanopublication
     */
    public NanodashLink(String id, String uri, Nanopub np) {
        this(id, uri, np, null);
    }

    /**
     * Creates a link to a nanopublication or an IRI.
     *
     * @param id  the Wicket component ID
     * @param uri the URI of the nanopublication or IRI
     */
    public NanodashLink(String id, String uri) {
        this(id, uri, null, null);
    }

    private static ValueFactory vf = SimpleValueFactory.getInstance();

}
