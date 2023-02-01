package org.petapico.nanobench;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.Charsets;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.Nanopub;
import org.nanopub.SimpleCreatorPattern;

public class NanobenchLink extends Panel {
	
	private static final long serialVersionUID = 1L;

	public NanobenchLink(String id, String uri, Nanopub np, IRI templateClass, boolean objectPosition) {
		super(id);

		List<Template> templates = new ArrayList<>();
		Map<IRI,String> labels = new HashMap<>();
		if (np != null) {
			for (Statement st : np.getPubinfo()) {
				if (st.getPredicate().equals(Template.HAS_LABEL_FROM_API)) labels.put((IRI) st.getSubject(), st.getObject().stringValue());
			}
		}
		if (Template.ASSERTION_TEMPLATE_CLASS.equals(templateClass)) {
			IRI templateId = Template.getTemplateId(np);
			if (templateId != null) {
				templates.add(Template.getTemplate(templateId.stringValue()));
			}
		} else if (Template.PROVENANCE_TEMPLATE_CLASS.equals(templateClass)) {
			IRI templateId = Template.getProvenanceTemplateId(np);
			if (templateId != null) {
				templates.add(Template.getTemplate(templateId.stringValue()));
			}
		} else if (Template.PUBINFO_TEMPLATE_CLASS.equals(templateClass)) {
			Set<IRI> templateIds = Template.getPubinfoTemplateIds(np);
			for (IRI templateId : templateIds) {
				templates.add(Template.getTemplate(templateId.stringValue()));
			}
		}

		final IRI iriObj = vf.createIRI(uri);
		if (np != null && uri.equals(np.getUri().stringValue())) {
			ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "this");
			link.add(new AttributeAppender("style", "background: #666; color: #fff; padding: 0 5px; border-radius: 7px;"));
			add(link);
			add(new Label("iri", uri));
		} else if (np != null && uri.equals(np.getAssertionUri().stringValue())) {
			ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "this assertion");
			link.add(new AttributeAppender("class", " nanopub-assertion "));
			link.add(new AttributeAppender("style", "padding: 0 5px; border-radius: 7px; border-width: 1px; border-color: #666; border-style: solid;"));
			add(link);
			add(new Label("iri", uri));
		} else if (uri.equals(Nanopub.HAS_ASSERTION_URI.stringValue())) {
			ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "assertion");
			link.add(new AttributeAppender("class", " nanopub-assertion "));
			link.add(new AttributeAppender("style", "padding: 0 5px; border-radius: 7px; border-width: 1px; border-color: #666; border-style: solid;"));
			add(link);
			add(new Label("iri", uri));
		} else if (uri.equals(Nanopub.HAS_PROVENANCE_URI.stringValue())) {
			ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "provenance");
			link.add(new AttributeAppender("class", " nanopub-provenance "));
			link.add(new AttributeAppender("style", "padding: 0 5px; border-radius: 7px; border-width: 1px; border-color: #666; border-style: solid;"));
			add(link);
			add(new Label("iri", uri));
		} else if (uri.equals(Nanopub.HAS_PUBINFO_URI.stringValue())) {
			ExternalLink link = new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), "pubinfo");
			link.add(new AttributeAppender("class", " nanopub-pubinfo "));
			link.add(new AttributeAppender("style", "padding: 0 5px; border-radius: 7px; border-width: 1px; border-color: #666; border-style: solid;"));
			add(link);
			add(new Label("iri", uri));
		} else {
			String label = IriItem.getShortNameFromURI(uri);
			Set<IRI> creators = null;
			if (!templates.isEmpty()) {
				// TODO Calling this for every link separately is very inefficient:
				creators = SimpleCreatorPattern.getCreators(np);
			}
			for (Template template : templates) {
				// TODO For pubinfo templates, we don't consider which triple came from which template (which is non-trivial):
				if (creators.contains(iriObj)) {
					if (objectPosition) {
						label = "me";
					} else {
						label = "I";
					}
					break;
				} else {
					IRI i = vf.createIRI(uri);
					String l = template.getLabel(i);
					if (l != null) {
						label = l;
						break;
					}
					l = labels.get(i);
					if (l != null) {
						label = l;
						break;
					}
				}
			}
			add(new ExternalLink("link", ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(uri, Charsets.UTF_8), label));
			add(new Label("iri", uri));
		}
	}

	public NanobenchLink(String id, String uri, Nanopub np) {
		this(id, uri, np, null, false);
	}

	public NanobenchLink(String id, String uri) {
		this(id, uri, null, null, false);
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

}
