package com.knowledgepixels.nanodash.component;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.Charsets;
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

import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.connector.ios.DsConfig;
import com.knowledgepixels.nanodash.connector.ios.DsNanopubPage;
import com.knowledgepixels.nanodash.connector.pensoft.BdjConfig;
import com.knowledgepixels.nanodash.connector.pensoft.BdjNanopubPage;
import com.knowledgepixels.nanodash.connector.pensoft.RioConfig;
import com.knowledgepixels.nanodash.connector.pensoft.RioNanopubPage;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;

import net.trustyuri.TrustyUriUtils;

public class NanodashLink extends Panel {
	
	private static final long serialVersionUID = 1L;

	public NanodashLink(String id, String uri, Nanopub np, IRI templateClass, boolean objectPosition) {
		super(id);

		final List<Template> templates = new ArrayList<>();
		final Map<IRI,String> labels = new HashMap<>();
		if (np != null) {
			for (Statement st : np.getPubinfo()) {
				if (st.getPredicate().equals(Template.HAS_LABEL_FROM_API) || st.getPredicate().equals(RDFS.LABEL)) {
					String label = st.getObject().stringValue();
					labels.put((IRI) st.getSubject(), label);
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
			add(new ExternalLink("uri", uri, uri));
		} else if (np != null && uri.equals(np.getAssertionUri().stringValue())) {
			ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "this assertion");
			link.add(new AttributeAppender("class", " nanopub-assertion "));
			add(link);
			add(new Label("description", "the assertion of this specific nanopublication"));
			add(new ExternalLink("uri", uri, uri));
		} else if (uri.equals(Nanopub.HAS_ASSERTION_URI.stringValue())) {
			ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "assertion");
			link.add(new AttributeAppender("class", " nanopub-assertion "));
			add(link);
			add(new Label("description", "links a nanopublication to its assertion"));
			add(new ExternalLink("uri", uri, uri));
		} else if (uri.equals(Nanopub.HAS_PROVENANCE_URI.stringValue())) {
			ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "provenance");
			link.add(new AttributeAppender("class", " nanopub-provenance "));
			add(link);
			add(new Label("description", "links a nanopublication to its provenance"));
			add(new ExternalLink("uri", uri, uri));
		} else if (uri.equals(Nanopub.HAS_PUBINFO_URI.stringValue())) {
			ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "pubinfo");
			link.add(new AttributeAppender("class", " nanopub-pubinfo "));
			add(link);
			add(new Label("description", "links a nanopublication to its pubinfo"));
			add(new ExternalLink("uri", uri, uri));
		} else {
			String label = IriItem.getShortNameFromURI(uri);
//			Set<IRI> creators = null;
//			if (!templates.isEmpty()) {
//				// TODO Calling this for every link separately is very inefficient:
//				creators = SimpleCreatorPattern.getCreators(np);
//			}
//			if (creators.contains(iriObj)) {
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
			boolean isNp = TrustyUriUtils.isPotentialTrustyUri(uri);
			String shortLabel = label.replaceFirst(" - [\\s\\S]*$", "");
			// TODO Improve this
			if (isNp && uri.startsWith(DsConfig.get().getTargetNamespace())) {
				add(new BookmarkablePageLink<Void>("link", DsNanopubPage.class, new PageParameters().add("id", uri).add("mode", "final")).setBody(Model.of(shortLabel)));
			} else if (isNp && uri.startsWith(BdjConfig.get().getTargetNamespace())) {
				add(new BookmarkablePageLink<Void>("link", BdjNanopubPage.class, new PageParameters().add("id", uri).add("mode", "final")).setBody(Model.of(shortLabel)));
			} else if (isNp && uri.startsWith(RioConfig.get().getTargetNamespace())) {
				add(new BookmarkablePageLink<Void>("link", RioNanopubPage.class, new PageParameters().add("id", uri).add("mode", "final")).setBody(Model.of(shortLabel)));
			} else {
				add(new BookmarkablePageLink<Void>("link", ExplorePage.class, new PageParameters().add("id", uri)).setBody(Model.of(shortLabel)));
			}
			String description = "";
			if (np != null && uri.startsWith(np.getUri().stringValue())) {
				description = "This is a local identifier that was minted when the nanopublication was created.";
			}
			if (label.contains(" - ")) description = label.replaceFirst("^.* - ", "");
			add(new Label("description", description));
			add(new ExternalLink("uri", uri, uri));
		}
	}

	public NanodashLink(String id, String uri, Nanopub np) {
		this(id, uri, np, null, false);
	}

	public NanodashLink(String id, String uri) {
		this(id, uri, null, null, false);
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

}
