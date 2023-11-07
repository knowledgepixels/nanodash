package com.knowledgepixels.nanodash;

import java.net.URISyntaxException;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.Validatable;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.knowledgepixels.nanodash.StatementItem.RepetitionGroup;

import net.trustyuri.TrustyUriUtils;

public class ReadonlyItem extends Panel implements ContextComponent {

	// TODO: Make ContextComponent an abstract class with superclass Panel, and move the common code of the form items there.

	private static final long serialVersionUID = 1L;

	private IModel<String> model;
	private PublishFormContext context;
	private String prefix;
	private Label labelComp;
	private IRI iri;
	private final Template template;

	public ReadonlyItem(String id, String parentId, final IRI iriP, boolean objectPosition, IRI statementPartId, RepetitionGroup rg) {
		super(id);
		context = rg.getContext();
		this.iri = iriP;
		template = context.getTemplate();
		model = context.getComponentModels().get(iri);
		if (model == null) {
			model = Model.of("");
			context.getComponentModels().put(iri, model);
		}
		String postfix = Utils.getUriPostfix(iri);
		if (context.hasParam(postfix)) {
			model.setObject(context.getParam(postfix));
		}
		prefix = template.getPrefix(iri);
		if (prefix == null) prefix = "";
		if (template.isLocalResource(iri)) {
			prefix = Utils.getUriPrefix(iri);
		}
		String prefixLabel = template.getPrefixLabel(iri);
		Label prefixLabelComp;
		if (prefixLabel == null) {
			prefixLabelComp = new Label("prefix", "");
			prefixLabelComp.setVisible(false);
		} else {
			if (prefixLabel.length() > 0 && parentId.equals("subj") && !prefixLabel.matches("https?://.*")) {
				// Capitalize first letter of label if at subject position:
				prefixLabel = prefixLabel.substring(0, 1).toUpperCase() + prefixLabel.substring(1);
			}
			prefixLabelComp = new Label("prefix", prefixLabel);
		}
		add(prefixLabelComp);
		String prefixTooltip = prefix;
		if (!prefix.isEmpty()) {
			prefixTooltip += "...";
			if (template.isLocalResource(iri)) {
				prefixTooltip = "local:...";
			}
		}
		add(new Label("prefixtooltiptext", prefixTooltip));

		labelComp = new Label("label", new Model<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				String obj = model.getObject();
				if (obj != null && obj.matches("(https?|file)://.+")) {
					IRI objIri = vf.createIRI(obj);
					return getLabelString(objIri);
				}
				return obj;
			}
			
		});
		if (iri.equals(Template.ASSERTION_PLACEHOLDER)) {
			labelComp.add(new AttributeAppender("class", " nanopub-assertion "));
			labelComp.add(new AttributeAppender("style", "padding: 4px; border-radius: 4px;"));
		} else if (iri.equals(Template.NANOPUB_PLACEHOLDER)) {
			labelComp.add(new AttributeAppender("style", "background: #ffffff; background-image: url(\"npback-left.png\"); border-width: 1px; border-color: #666; border-style: solid; padding: 4px 4px 4px 20px; border-radius: 4px;"));
		}
		add(labelComp);
		add(new Label("description", new Model<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				String obj = model.getObject();
				if (obj != null && obj.matches("(https?|file)://.+")) {
					IRI objIri = vf.createIRI(obj);
					String description = "";
					// TODO We'd need the nanopub ID here, not the template ID:
//					if (obj.startsWith(context.getTemplateId())) {
//						obj = obj.replace(context.getTemplateId(), "");
//						description = "This is a local identifier that was minted when the nanopublication was created.";
//					}
					String labelString = getLabelString(objIri);
					if (labelString.contains(" - ")) description = labelString.replaceFirst("^.* - ", "");
					return description;
				}
				return "";
			}
			
		}));
		add(new ExternalLink("uri", model, new Model<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				String obj = model.getObject();
				if (obj != null && obj.startsWith("\"")) return "";
				return obj;
			}
			
		}));
	}

	private String getLabelString(IRI iri) {
		if (template.getLabel(iri) != null) {
			return template.getLabel(iri);
		} else {
			return IriItem.getShortNameFromURI(iri.stringValue());
		}
	}

	@Override
	public void removeFromContext() {
		// Nothing to be done here.
	}

	@Override
	public boolean isUnifiableWith(Value v) {
		if (v == null) return true;
		if (v instanceof IRI) {
			String vs = v.stringValue();
			if (vs.startsWith(prefix)) vs = vs.substring(prefix.length());
			if (vs.startsWith("local:")) vs = vs.replaceFirst("^local:", "");
			if (template.isAutoEscapePlaceholder(iri)) {
				vs = Utils.urlDecode(vs);
			}
			Validatable<String> validatable = new Validatable<>(vs);
			if (template.isLocalResource(iri) && !Utils.isUriPostfix(vs)) {
				vs = Utils.getUriPostfix(vs);
			}
			new Validator(iri, template, prefix).validate(validatable);
			if (!validatable.isValid()) {
				return false;
			}
			if (model.getObject().isEmpty()) {
				return true;
			}
			return vs.equals(model.getObject());
		} else if (v instanceof Literal) {
			if (template.getRegex(iri) != null && !v.stringValue().matches(template.getRegex(iri))) {
				return false;
			}
			if (labelComp.getDefaultModelObject() == null || labelComp.getDefaultModelObject().toString().isEmpty()) {
				return true;
			}
			return labelComp.getDefaultModelObject().equals("\"" + v.stringValue() + "\"");
		}
		return false;
	}

	@Override
	public void unifyWith(Value v) throws UnificationException {
		if (v == null) return;
		String vs = v.stringValue();
		if (!isUnifiableWith(v)) throw new UnificationException(vs);
		if (v instanceof IRI) {
			if (!prefix.isEmpty() && vs.startsWith(prefix)) {
				vs = vs.substring(prefix.length());
			} else if (vs.startsWith("local:")) {
				vs = vs.replaceFirst("^local:", "");
			} else if (template.isLocalResource(iri) && !Utils.isUriPostfix(vs)) {
				vs = Utils.getUriPostfix(vs);
			}
			if (template.isAutoEscapePlaceholder(iri)) {
				vs = Utils.urlDecode(vs);
			}
			model.setObject(vs);
		} else if (v instanceof Literal) {
			model.setObject("\"" + vs + "\"");
		}
	}


	protected static class Validator extends InvalidityHighlighting implements IValidator<String> {

		private static final long serialVersionUID = 1L;

		private IRI iri;
		private Template template;
		private String prefix;

		public Validator(IRI iri, Template template, String prefix) {
			this.iri = iri;
			this.template = template;
			this.prefix = prefix;
		}

		@Override
		public void validate(IValidatable<String> s) {
			String sv = s.getValue();
			String p = prefix;
			if (template.isAutoEscapePlaceholder(iri)) {
				sv = Utils.urlEncode(sv);
			}
			if (sv.matches("(https?|file)://.+")) {
				p = "";
			} else if (sv.contains(":")) {
				s.error(new ValidationError("Colon character is not allowed in postfix"));
			}
			String iriString = p + sv;
			if (iriString.matches("[^:# ]+")) {
				p = "local:";
				iriString = p + sv;
			}
			try {
				ParsedIRI piri = new ParsedIRI(iriString);
				if (!piri.isAbsolute()) {
					s.error(new ValidationError("IRI not well-formed"));
				}
				if (p.isEmpty() && !sv.startsWith("local:") && !sv.matches("(https?|file)://.+")) {
					s.error(new ValidationError("Only http(s):// and file:// IRIs are allowed here"));
				}
			} catch (URISyntaxException ex) {
				s.error(new ValidationError("IRI not well-formed"));
			}
			String regex = template.getRegex(iri);
			if (regex != null) {
				if (!sv.matches(regex)) {
					s.error(new ValidationError("Value '" + sv + "' doesn't match the pattern '" + regex + "'"));
				}
			}
			if (template.isExternalUriPlaceholder(iri)) {
				if (!iriString.matches("(https?|file)://.+")) {
					s.error(new ValidationError("Not an external IRI"));
				}
			}
			if (template.isTrustyUriPlaceholder(iri)) {
				if (!TrustyUriUtils.isPotentialTrustyUri(iriString)) {
					s.error(new ValidationError("Not a trusty URI"));
				}
			}
		}

	}

	public String toString() {
		return "[read-only IRI item: " + iri + "]";
	}

	static final ValueFactory vf = SimpleValueFactory.getInstance();

}
