package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.connector.ios.DsConfig;
import com.knowledgepixels.nanodash.connector.ios.DsNanopubPage;
import com.knowledgepixels.nanodash.connector.pensoft.BdjConfig;
import com.knowledgepixels.nanodash.connector.pensoft.BdjNanopubPage;
import com.knowledgepixels.nanodash.connector.pensoft.RioConfig;
import com.knowledgepixels.nanodash.connector.pensoft.RioNanopubPage;
import com.knowledgepixels.nanodash.page.ExplorePage;
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

import java.net.URLEncoder;
import java.util.*;

/**
 * A Wicket component that creates a link to a nanopublication or an IRI.
 */
public class NanodashLink extends Panel {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a link to a nanopublication or an IRI.
     *
     * @param id             the Wicket component ID
     * @param uri            the URI of the nanopublication or IRI
     * @param np             the nanopublication, or null if the link is not to a nanopublication
     * @param templateClass  the template class of the nanopublication, or null if the link is not to a nanopublication
     * @param objectPosition if true, the link is to an object position in a template, otherwise it is to a subject position
     * @param label          the label to display for the link, or null to derive it from the nanopublication or IRI
     */
    public NanodashLink(String id, String uri, Nanopub np, IRI templateClass, boolean objectPosition, String label) {
        super(id);

        final List<Template> templates = new ArrayList<>();
        final Map<IRI, String> labels = new HashMap<>();
        if (np != null) {
            for (Statement st : np.getPubinfo()) {
                if (st.getPredicate().equals(Template.HAS_LABEL_FROM_API) || st.getPredicate().equals(RDFS.LABEL)) {
                    labels.put((IRI) st.getSubject(), st.getObject().stringValue());
                }
            }
        }

        final TemplateData td = TemplateData.get();

        if (Template.ASSERTION_TEMPLATE_CLASS.equals(templateClass)) {
            IRI templateId = td.getTemplateId(np);
            if (templateId != null) {
                templates.add(td.getTemplate(templateId.stringValue()));
            }
        } else if (Template.PROVENANCE_TEMPLATE_CLASS.equals(templateClass)) {
            IRI templateId = td.getProvenanceTemplateId(np);
            if (templateId != null) {
                templates.add(td.getTemplate(templateId.stringValue()));
            }
        } else if (Template.PUBINFO_TEMPLATE_CLASS.equals(templateClass)) {
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
        } else if (uri.equals(Nanopub.HAS_ASSERTION_URI.stringValue())) {
            ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "assertion");
            link.add(new AttributeAppender("class", " nanopub-assertion "));
            add(link);
            add(new Label("description", "links a nanopublication to its assertion"));
        } else if (uri.equals(Nanopub.HAS_PROVENANCE_URI.stringValue())) {
            ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "provenance");
            link.add(new AttributeAppender("class", " nanopub-provenance "));
            add(link);
            add(new Label("description", "links a nanopublication to its provenance"));
        } else if (uri.equals(Nanopub.HAS_PUBINFO_URI.stringValue())) {
            ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "pubinfo");
            link.add(new AttributeAppender("class", " nanopub-pubinfo "));
            add(link);
            add(new Label("description", "links a nanopublication to its pubinfo"));
        } else {
            if (label == null || label.isBlank()) {
                label = IriItem.getShortNameFromURI(uri);
                if (iriObj.equals(User.getSignatureOwnerIri(np))) {
                    if (objectPosition) {
                        label = "me (" + User.getShortDisplayName(iriObj) + ")";
                    } else {
                        label = "I (" + User.getShortDisplayName(iriObj) + ")";
                    }
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
            add(createLink("link", uri, shortLabel));
            String description = "";
            if (np != null && uri.startsWith(np.getUri().stringValue())) {
                description = "This is a local identifier that was minted when the nanopublication was created.";
            }
            if (label.contains(" - ")) description = label.replaceFirst("^.* - ", "");
            add(new Label("description", description));
        }
        add(Utils.getUriLink("uri", uri));
    }

    public static Component createLink(String markupId, String uri, String label) {
        boolean isNp = TrustyUriUtils.isPotentialTrustyUri(uri);
        // TODO Improve this
        if (isNp && uri.startsWith(DsConfig.get().getTargetNamespace())) {
            return new BookmarkablePageLink<Void>(markupId, DsNanopubPage.class, new PageParameters().add("id", uri).add("mode", "final")).setBody(Model.of(label));
        } else if (isNp && uri.startsWith(BdjConfig.get().getTargetNamespace())) {
            return new BookmarkablePageLink<Void>(markupId, BdjNanopubPage.class, new PageParameters().add("id", uri).add("mode", "final")).setBody(Model.of(label));
        } else if (isNp && uri.startsWith(RioConfig.get().getTargetNamespace())) {
            return new BookmarkablePageLink<Void>(markupId, RioNanopubPage.class, new PageParameters().add("id", uri).add("mode", "final")).setBody(Model.of(label));
        } else {
            return new BookmarkablePageLink<Void>(markupId, ExplorePage.class, new PageParameters().add("id", uri).add("label", label)).setBody(Model.of(label));
        }
    }

    /**
     * Creates a link to a nanopublication or an IRI.
     *
     * @param id             the Wicket component ID
     * @param uri            the URI of the nanopublication or IRI
     * @param np             the nanopublication, or null if the link is not to a nanopublication
     * @param templateClass  the template class of the nanopublication, or null if the link is not to a nanopublication
     * @param objectPosition if true, the link is to an object position in a template, otherwise it is to a subject position
     */
    public NanodashLink(String id, String uri, Nanopub np, IRI templateClass, boolean objectPosition) {
        this(id, uri, np, templateClass, objectPosition, null);
    }

    /**
     * Creates a link to a nanopublication or an IRI.
     *
     * @param id  the Wicket component ID
     * @param uri the URI of the nanopublication or IRI
     * @param np  the nanopublication, or null if the link is not to a nanopublication
     */
    public NanodashLink(String id, String uri, Nanopub np) {
        this(id, uri, np, null, false);
    }

    /**
     * Creates a link to a nanopublication or an IRI.
     *
     * @param id  the Wicket component ID
     * @param uri the URI of the nanopublication or IRI
     */
    public NanodashLink(String id, String uri) {
        this(id, uri, null, null, false);
    }

    private static ValueFactory vf = SimpleValueFactory.getInstance();

}
