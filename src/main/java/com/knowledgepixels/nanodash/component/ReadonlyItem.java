package com.knowledgepixels.nanodash.component;

import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.Charsets;
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
import org.nanopub.Nanopub;
import org.nanopub.SimpleCreatorPattern;

import com.knowledgepixels.nanodash.RestrictedChoice;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.StatementItem.RepetitionGroup;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.UserPage;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;

import net.trustyuri.TrustyUriUtils;

public class ReadonlyItem extends Panel implements ContextComponent {

	// TODO: Make ContextComponent an abstract class with superclass Panel, and move the common code of the form items there.

	private static final long serialVersionUID = 1L;

	private IModel<String> model;
	private TemplateContext context;
	private String prefix;
	private ExternalLink linkComp;
	private Label extraComp;
	private IModel<String> extraModel;
	private IRI iri;
	private RestrictedChoice restrictedChoice;
	private final Template template;

	public ReadonlyItem(String id, String parentId, final IRI iriP, boolean objectPosition, IRI statementPartId, final RepetitionGroup rg) {
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

		final Map<String,String> foafNameMap;
		if (context.getExistingNanopub() == null) {
			foafNameMap = new HashMap<>();
		} else {
			foafNameMap = Utils.getFoafNameMap(context.getExistingNanopub());
		}

		prefix = template.getPrefix(iri);
		if (prefix == null) prefix = "";
		if (template.isRestrictedChoicePlaceholder(iri)) {
			restrictedChoice = new RestrictedChoice(iri, context);
		}
		add(new Label("prefix", new Model<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				String prefixLabel = template.getPrefixLabel(iri);
				String v = getFullValue();
				if (prefixLabel == null || User.isUser(v) || foafNameMap.containsKey(v)) {
					return "";
				} else {
					if (prefixLabel.length() > 0 && parentId.equals("subj") && !prefixLabel.matches("https?://.*")) {
						// Capitalize first letter of label if at subject position:
						prefixLabel = prefixLabel.substring(0, 1).toUpperCase() + prefixLabel.substring(1);
					}
					return prefixLabel;
				}
			}
			
		}));

		linkComp = new ExternalLink("link", new Model<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				String obj = getFullValue();
				if (obj == null) return "";
				if (obj.equals("local:nanopub")) {
					if (context.getExistingNanopub() != null) {
						obj = context.getExistingNanopub().getUri().stringValue();
						return ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(obj, Charsets.UTF_8);
					} else {
						return "";
					}
				} else if (obj.equals("local:assertion")) {
					if (context.getExistingNanopub() != null) {
						obj = context.getExistingNanopub().getAssertionUri().stringValue();
						return ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(obj, Charsets.UTF_8);
					} else {
						return "";
					}
				} else if (User.isUser(obj)) {
					return UserPage.MOUNT_PATH + "?id=" + URLEncoder.encode(obj, Charsets.UTF_8);
				} else if (obj.matches("https?://.+")) {
					return ExplorePage.MOUNT_PATH + "?id=" + URLEncoder.encode(obj, Charsets.UTF_8);
				} else {
					return "";
				}
			}
			
		}, new Model<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				String obj = getFullValue();
				if (obj != null && obj.matches("https?://.+")) {
					IRI objIri = vf.createIRI(obj);
					if (iri.equals(Template.CREATOR_PLACEHOLDER)) {
						if (objectPosition) {
							return "me (" + User.getShortDisplayName(objIri) + ")";
						} else {
							return "I (" + User.getShortDisplayName(objIri) + ")";
						}
					} else if (isAssertionValue(objIri)) {
						if (context.getType() == ContextType.ASSERTION) {
							return "this assertion";
						} else {
							return "the assertion above";
						}
					} else if (isNanopubValue(objIri)) {
						return "this nanopublication";
					} else if (User.isUser(obj)) {
						return User.getShortDisplayName(objIri);
					} else if (foafNameMap.containsKey(obj)) {
						return foafNameMap.get(obj);
					}
					return getLabelString(objIri);
				}
				return obj;
			}
			
		});
		if (template.isIntroducedResource(iri) || template.isEmbeddedResource(iri)) {
			linkComp.add(AttributeAppender.append("class", "introduced"));
		}
		add(linkComp);
		add(new Label("description", new Model<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				String obj = getFullValue();
				if (obj != null && obj.matches("https?://.+")) {
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

			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				String obj = getFullValue();
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
	}

	@Override
	public void fillFinished() {
		String obj = getFullValue();
		if (obj != null) {
			if (isAssertionValue(obj)) {
				linkComp.add(new AttributeAppender("class", "this-assertion"));
			} else if (isNanopubValue(obj)) {
				linkComp.add(new AttributeAppender("class", "this-nanopub"));
			} else if (context.getExistingNanopub() != null) {
				Nanopub np = context.getExistingNanopub();
				if (Utils.getIntroducedIriIds(np).contains(obj) || Utils.getEmbeddedIriIds(np).contains(obj)) {
					linkComp.add(AttributeAppender.append("class", "introduced"));
				}
			}
		}
	}

	@Override
	public void finalizeValues() {
	}

	private String getLabelString(IRI iri) {
		if (template.getLabel(iri) != null) {
			return template.getLabel(iri).replaceFirst(" - .*$", "");
		} else if (context.getLabel(iri) != null) {
			return context.getLabel(iri).replaceFirst(" - .*$", "");
		} else {
			return IriItem.getShortNameFromURI(iri.stringValue());
		}
	}

	@Override
	public void removeFromContext() {
		// Nothing to be done here.
	}

	private String getFullValue() {
		String s = model.getObject();
		if (s == null) return null;
		if (template.isAutoEscapePlaceholder(iri)) {
			s = Utils.urlEncode(s);
		}
		if (!prefix.isEmpty()) {
			s = prefix + s;
		}
		return s;
	}

	private boolean isNanopubValue(Object obj) {
		if (obj == null) return false;
		if (obj.toString().equals("local:nanopub")) return true;
		if (context.getExistingNanopub() == null) return false;
		return obj.toString().equals(context.getExistingNanopub().getUri().stringValue());
	}

	private String getNanopubValue() {
		if (context.getExistingNanopub() != null) {
			return context.getExistingNanopub().getUri().stringValue();
		} else {
			return "local:nanopub";
		}
	}

	private boolean isAssertionValue(Object obj) {
		if (obj == null) return false;
		if (obj.toString().equals("local:assertion")) return true;
		if (context.getExistingNanopub() == null) return false;
		return obj.toString().equals(context.getExistingNanopub().getAssertionUri().stringValue());
	}

	private String getAssertionValue() {
		if (context.getExistingNanopub() != null) {
			return context.getExistingNanopub().getAssertionUri().stringValue();
		} else {
			return "local:assertion";
		}
	}

	@Override
	public boolean isUnifiableWith(Value v) {
		if (v == null) return true;
		if (v instanceof IRI) {
			String vs = v.stringValue();
			if (vs.equals("local:nanopub")) {
				vs = getNanopubValue();
			} else if (vs.equals("local:assertion")) {
				vs = getAssertionValue();
			}
			if (vs.startsWith(prefix)) vs = vs.substring(prefix.length());
//			if (vs.startsWith("local:")) vs = vs.replaceFirst("^local:", "");
			if (template.isAutoEscapePlaceholder(iri)) {
				vs = Utils.urlDecode(vs);
			}
			Validatable<String> validatable = new Validatable<>(vs);
//			if (template.isLocalResource(iri) && !Utils.isUriPostfix(vs)) {
//				vs = Utils.getUriPostfix(vs);
//			}
			new Validator().validate(validatable);
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
			if (linkComp.getDefaultModelObject() == null || linkComp.getDefaultModelObject().toString().isEmpty()) {
				return true;
			}
			return linkComp.getDefaultModelObject().equals("\"" + v.stringValue() + "\"");
		}
		return false;
	}

	@Override
	public void unifyWith(Value v) throws UnificationException {
		if (v == null) return;
		String vs = v.stringValue();
		if (!isUnifiableWith(v)) throw new UnificationException(vs);
		if (v instanceof IRI) {
			if (vs.equals("local:nanopub")) {
				vs = getNanopubValue();
			} else if (vs.equals("local:assertion")) {
				vs = getAssertionValue();
			}
			if (!prefix.isEmpty() && vs.startsWith(prefix)) {
				vs = vs.substring(prefix.length());
			// With read-only items, we don't need preliminary local identifiers:
//			} else if (vs.startsWith("local:")) {
//				vs = vs.replaceFirst("^local:", "");
//			} else if (template.isLocalResource(iri) && !Utils.isUriPostfix(vs)) {
//				vs = Utils.getUriPostfix(vs);
			}
			if (template.isAutoEscapePlaceholder(iri)) {
				vs = Utils.urlDecode(vs);
			}
			model.setObject(vs);
		} else if (v instanceof Literal) {
			model.setObject("\"" + vs + "\"");
			// TODO Didn't manage to encode this into a working regex:
			if (vs.startsWith("<p>") || vs.startsWith("<p ") || vs.startsWith("<div>") || vs.startsWith("<div ") || vs.startsWith("<span>") || vs.startsWith("<span ") || vs.startsWith("<img ")) {
				linkComp.setVisible(false);
				extraModel.setObject("<span class=\"internal\">" + Utils.sanitizeHtml(vs) + "</span>");
				extraComp.setEscapeModelStrings(false);
				extraComp.setVisible(true);
			}
		}
	}


	protected class Validator extends InvalidityHighlighting implements IValidator<String> {

		private static final long serialVersionUID = 1L;

		public Validator() {
		}

		@Override
		public void validate(IValidatable<String> s) {
			String sv = s.getValue();
			String p = prefix;
			if (template.isAutoEscapePlaceholder(iri)) {
				sv = Utils.urlEncode(sv);
			}
			if (sv.matches("https?://.+")) {
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
				if (p.isEmpty() && !sv.startsWith("local:") && !sv.matches("https?://.+")) {
					s.error(new ValidationError("Only http(s):// IRIs are allowed here"));
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
			if (template.isRestrictedChoicePlaceholder(iri)) {
				if (!restrictedChoice.getPossibleValues().contains(iriString) && !restrictedChoice.hasPossibleRefValues()) {
					// not checking the possible ref values can overgenerate, but normally works
					s.error(new ValidationError("Invalid choice"));
				}
			}
			if (template.isExternalUriPlaceholder(iri)) {
				if (!iriString.matches("https?://.+")) {
					s.error(new ValidationError("Not an external IRI"));
				}
			}
			if (template.isTrustyUriPlaceholder(iri)) {
				if (!TrustyUriUtils.isPotentialTrustyUri(iriString)) {
					s.error(new ValidationError("Not a trusty URI"));
				}
			}
			if (iri.equals(Template.CREATOR_PLACEHOLDER) && context.getExistingNanopub() != null) {
				boolean found = false;
				for (IRI creator : SimpleCreatorPattern.getCreators(context.getExistingNanopub())) {
					if (creator.stringValue().equals(iriString)) { found = true; break; }
				}
				if (!found) {
					s.error(new ValidationError("Not a creator of nanopub"));
				}
			}
		}

	}

	public String toString() {
		return "[read-only IRI item: " + iri + "]";
	}

	static final ValueFactory vf = SimpleValueFactory.getInstance();

}
