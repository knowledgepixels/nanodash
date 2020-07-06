package org.petapico.nanobench;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import net.trustyuri.TrustyUriUtils;

public class GuidedChoiceItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	private String prefix;

	public GuidedChoiceItem(String id, String parentId, final IRI iri, boolean optional, final PublishForm form) {
		super(id);
		IModel<String> model = form.formComponentModels.get(iri);
		if (model == null) {
			String value = "";
			String postfix = iri.stringValue().replaceFirst("^.*[/#](.*)$", "$1");
			if (form.params.containsKey(postfix)) {
				value = form.params.get(postfix);
			}
			model = Model.of(value);
			form.formComponentModels.put(iri, model);
		}
		final List<String> possibleValues = new ArrayList<>();
		for (Value v : form.template.getPossibleValues(iri)) {
			possibleValues.add(v.toString());
		}

		prefix = form.template.getPrefix(iri);
		if (prefix == null) prefix = "";
		String prefixLabel = form.template.getPrefixLabel(iri);
		Label prefixLabelComp;
		if (prefixLabel == null) {
			prefixLabelComp = new Label("prefix", "");
			prefixLabelComp.setVisible(false);
		} else {
			if (prefixLabel.length() > 0 && parentId.equals("subj")) {
				// Capitalize first letter of label if at subject position:
				prefixLabel = prefixLabel.substring(0, 1).toUpperCase() + prefixLabel.substring(1);
			}
			prefixLabelComp = new Label("prefix", prefixLabel);
		}
		add(prefixLabelComp);
		String prefixTooltip = prefix;
		if (!prefix.isEmpty()) {
			prefixTooltip += "...";
		}
		add(new Label("prefixtooltiptext", prefixTooltip));

		ChoiceProvider<String> choiceProvider = new ChoiceProvider<String>() {

			private static final long serialVersionUID = 1L;

			private Map<String,String> labelMap = new HashMap<>();

			@Override
			public String getDisplayValue(String id) {
				if (id == null || id.isEmpty()) return "";
				String label = null;
				if (id.matches("(https?|file)://.+") && form.template.getLabel(vf.createIRI(id)) != null) {
					label = form.template.getLabel(vf.createIRI(id));
				} else if (labelMap.containsKey(id)) {
					label = labelMap.get(id);
				}
				if (label != null) {
					return label + " (" + id + ")";
				} else {
					return id;
				}
			}

			@Override
			public String getIdValue(String object) {
				return object;
			}

			// Getting strange errors with Tomcat if this method is not overridden:
			@Override
			public void detach() {
			}

			@Override
			public void query(String term, int page, Response<String> response) {
				if (term == null) {
					response.addAll(possibleValues);
					return;
				}
				term = term.toLowerCase();
				for (String s : possibleValues) {
					if (s.toLowerCase().contains(term) || getDisplayValue(s).toLowerCase().contains(term)) response.add(s);
				}
				// Nanopub API:
				try {
					Map<String,String> params = new HashMap<>();
					params.put("searchterm", " " + term);
					params.put("type", "http://www.w3.org/2002/07/owl#Class");
					List<Map<String,String>> result = ApiAccess.getAll("find_signed_things", params);
					int count = 0;
					for (Map<String,String> r : result) {
						if (r.get("superseded").equals("1") || r.get("retracted").equals("1")) continue;
						String uri = r.get("thing");
						response.add(uri);
						String desc = r.get("description");
						if (desc.length() > 80) desc = desc.substring(0, 77) + "...";
						if (!desc.isEmpty()) desc = " - " + desc;
						String userString = "";
						User user = User.getUserForPubkey(r.get("pubkey"));
						if (user != null) userString = " - by " + user.getShortDisplayName();
						labelMap.put(uri, r.get("label") + desc + userString);
						count++;
						if (count > 5) return;
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				// Wikidata API:
				try {
					String apiString = "https://www.wikidata.org/w/api.php?action=wbsearchentities&language=en&format=json&limit=5&search=";
					URL url = new URL(apiString + URLEncoder.encode(term, "UTF-8"));
					String jsonString = IOUtils.toString(url, Charset.forName("UTF-8"));
					JSONObject json = new JSONObject(jsonString);
					JSONArray l = (JSONArray) json.get("search");
					for (int i = 0; i < l.length(); i++) {
						JSONObject o = (JSONObject) l.get(i);
						String uri = o.getString("concepturi");
						response.add(uri);
						String desc = "";
						if (o.has("description")) desc = o.getString("description");
						if (desc.length() > 80) desc = desc.substring(0, 77) + "...";
						labelMap.put(uri, o.getString("label") + " - " + desc);
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				// BioPortal API:
				try {
					String apiString = "http://data.bioontology.org/search?pagesize=5&apikey=fd451bec-eacd-4519-b972-90fb6c7007cb&q=";
					HttpGet get = new HttpGet(apiString + URLEncoder.encode(term, "UTF-8"));
					InputStream in = HttpClientBuilder.create().build().execute(get).getEntity().getContent();
					String jsonString = IOUtils.toString(in, StandardCharsets.UTF_8);
					JSONObject json = new JSONObject(jsonString);
					JSONArray l = (JSONArray) json.get("collection");
					for (int i = 0; i < l.length(); i++) {
						JSONObject o = (JSONObject) l.get(i);
						String uri = o.getString("@id");
						response.add(uri);
						String desc = "";
						if (o.has("defintion")) desc = o.getString("defintion");
						if (desc.length() > 80) desc = desc.substring(0, 77) + "...";
						labelMap.put(uri, o.getString("prefLabel") + " - " + desc);
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}

			@Override
			public Collection<String> toChoices(Collection<String> ids) {
				return ids;
			}

		};
		final Select2Choice<String> textfield = new Select2Choice<String>("textfield", model, choiceProvider);
		textfield.getSettings().setCloseOnSelect(true);
		textfield.getSettings().setTags(true);
		textfield.getSettings().setPlaceholder("");
		textfield.getSettings().setAllowClear(true);

		if (!optional) textfield.setRequired(true);
		textfield.add(new AttributeAppender("style", "width:750px;"));
		textfield.add(new IValidator<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public void validate(IValidatable<String> s) {
				String p = prefix;
				if (s.getValue().matches("(https?|file)://.+")) p = "";
				try {
					ParsedIRI piri = new ParsedIRI(p + s.getValue());
					if (!piri.isAbsolute()) {
						s.error(new ValidationError("IRI not well-formed"));
					}
					if (p.isEmpty() && !(s.getValue().matches("(https?|file)://.+"))) {
						s.error(new ValidationError("Only http(s):// and file:// IRIs are allowed here"));
					}
				} catch (URISyntaxException ex) {
					s.error(new ValidationError("IRI not well-formed"));
				}
				String regex = form.template.getRegex(iri);
				if (regex != null) {
					if (!s.getValue().matches(regex)) {
						s.error(new ValidationError("Value '" + s.getValue() + "' doesn't match the pattern '" + regex + "'"));
					}
				}
				if (form.template.isTrustyUriPlaceholder(iri)) {
					if (!TrustyUriUtils.isPotentialTrustyUri(p + s.getValue())) {
						s.error(new ValidationError("Not a trusty URI"));
					}
				}
			}

		});
		form.formComponents.add(textfield);
		if (form.template.getLabel(iri) != null) {
			textfield.getSettings().setPlaceholder(form.template.getLabel(iri));
		}
		textfield.add(new OnChangeAjaxBehavior() {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				for (FormComponent<String> fc : form.formComponents) {
					if (fc == textfield) continue;
					if (fc.getModel() == textfield.getModel()) {
						fc.modelChanged();
						target.add(fc);
					}
				}
			}

		});
		add(textfield);
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

}
