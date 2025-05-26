package com.knowledgepixels.nanodash.page;

import static com.knowledgepixels.nanodash.Utils.vf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.FailedApiCallException;

import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.QueryResultTable;
import com.knowledgepixels.nanodash.component.TemplateResults;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.component.UserList;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;

public class ProjectPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/project";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public static final IRI HAS_OWNER = vf.createIRI("https://w3id.org/kpxl/gen/terms/hasOwner");
	public static final IRI HAS_PINNED_TEMPLATE = vf.createIRI("https://w3id.org/kpxl/gen/terms/hasPinnedTemplate");
	public static final IRI HAS_PINNED_QUERY = vf.createIRI("https://w3id.org/kpxl/gen/terms/hasPinnedQuery");

	public ProjectPage(final PageParameters parameters) throws FailedApiCallException {
		super(parameters);

		String id = parameters.get("id").toString();
		ApiResponse resp = QueryApiAccess.get("get-introducing-np", "thing", id);
		String npId = resp.getData().get(0).get("np");
		Nanopub np = Utils.getAsNanopub(npId);

		add(new TitleBar("titlebar", this, null));
		String label = id.replaceFirst("^.*/", "");

		String description = null;
		List<IRI> owners = new ArrayList<>();
		List<Template> templates = new ArrayList<>();
		Set<String> templateTags = new HashSet<>();
		Map<String,List<Template>> templatesPerTag = new HashMap<>();
		List<IRI> queryIds = new ArrayList<>();
		IRI defaultProvenance = null;

		for (Statement st : np.getAssertion()) {
			if (st.getSubject().stringValue().equals(id)) {
				if (st.getPredicate().equals(DCTERMS.DESCRIPTION)) {
					description = st.getObject().stringValue();
				} else if (st.getPredicate().equals(RDFS.LABEL)) {
					label = st.getObject().stringValue();
				} else if (st.getPredicate().equals(HAS_OWNER) && st.getObject() instanceof IRI obj) {
					owners.add(obj);
				} else if (st.getPredicate().equals(HAS_PINNED_TEMPLATE) && st.getObject() instanceof IRI obj) {
					templates.add(TemplateData.get().getTemplate(obj.stringValue()));
				} else if (st.getPredicate().equals(HAS_PINNED_QUERY) && st.getObject() instanceof IRI obj) {
					queryIds.add(obj);
				} else if (st.getPredicate().equals(Template.HAS_DEFAULT_PROVENANCE_PREDICATE) && st.getObject() instanceof IRI obj) {
					defaultProvenance = obj;
				}
			} else if (st.getPredicate().equals(Template.HAS_TAG) && st.getObject() instanceof Literal l) {
				templateTags.add(l.stringValue());
				List<Template> list = templatesPerTag.get(l.stringValue());
				if (list == null) {
					list = new ArrayList<>();
					templatesPerTag.put(l.stringValue(), list);
				}
				list.add(TemplateData.get().getTemplate(st.getSubject().stringValue()));
			}
		}

		add(new Label("pagetitle", label + " (project) | nanodash"));
		add(new Label("projectname", label));
		add(new ExternalLink("id", id, id));
		add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().add("id", np.getUri())));
		add(new Label("description", "<span class=\"internal\">" + Utils.sanitizeHtml(description) + "</span>").setEscapeModelStrings(false));

		final PageParameters params = new PageParameters();
		if (defaultProvenance != null) {
			params.add("prtemplate", defaultProvenance.stringValue());
		}
		List<Pair<String,List<Template>>> templateLists = new ArrayList<>();
		List<String> templateTagList = new ArrayList<>(templateTags);
		Collections.sort(templateTagList);
		for (String tag : templateTagList) {
			for (Template t : templatesPerTag.get(tag)) {
				if (templates.contains(t)) templates.remove(t);
			}
			templateLists.add(Pair.of(tag, templatesPerTag.get(tag)));
		}
		if (!templates.isEmpty()) {
			String l = templateLists.isEmpty() ? "Templates" : "Other Templates";
			templateLists.add(Pair.of(l,templates));
		}
		add(new DataView<Pair<String,List<Template>>>("template-lists", new ListDataProvider<Pair<String,List<Template>>>(templateLists)) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Pair<String,List<Template>>> item) {
				item.add(new Label("label", item.getModelObject().getLeft()));
				item.add(TemplateResults.fromList("templates", item.getModelObject().getRight(), params));
			}

		});
		add(TemplateResults.fromList("templates", templates, params));
		add(new UserList("owners", owners));

		add(new DataView<IRI>("queries", new ListDataProvider<IRI>(queryIds)) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<IRI> item) {
				String queryId = QueryApiAccess.getQueryId(item.getModelObject());
				item.add(QueryResultTable.createComponent("query", queryId, false));
			}

		});
	}

	protected boolean hasAutoRefreshEnabled() {
		return true;
	}

}
