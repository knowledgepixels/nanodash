package com.knowledgepixels.nanodash.page;

import static com.knowledgepixels.nanodash.Utils.vf;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Utils;
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

	public ProjectPage(final PageParameters parameters) {
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

		for (Statement st : np.getAssertion()) {
			if (!st.getSubject().stringValue().equals(id)) continue;
			if (st.getPredicate().equals(DCTERMS.DESCRIPTION)) {
				description = st.getObject().stringValue();
			} else if (st.getPredicate().equals(RDFS.LABEL)) {
				label = st.getObject().stringValue();
			} else if (st.getPredicate().equals(HAS_OWNER) && st.getObject() instanceof IRI obj) {
				owners.add(obj);
			} else if (st.getPredicate().equals(HAS_PINNED_TEMPLATE) && st.getObject() instanceof IRI) {
				templates.add(TemplateData.get().getTemplate(st.getObject().stringValue()));
			}
		}

		add(new Label("projectname", label));
		add(new Label("pagetitle", label + " (project) | nanodash"));
		add(new Label("description", "<span class=\"internal\">" + Utils.sanitizeHtml(description) + "</span>").setEscapeModelStrings(false));
		add(TemplateResults.fromList("templates", templates));
		add(new UserList("owners", owners));
	}

	protected boolean hasAutoRefreshEnabled() {
		return true;
	}

}
