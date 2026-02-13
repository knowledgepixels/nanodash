package com.knowledgepixels.nanodash.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;

import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.TemplateData;

/**
 * A preview panel that shows what the template form will look like when used,
 * without the consent checkbox and publish/preview buttons.
 */
public class TemplateFormPreview extends Panel {

    private static final String creatorPubinfoTemplateId = "https://w3id.org/np/RAukAcWHRDlkqxk7H2XNSegc1WnHI569INvNr-xdptDGI";
    private static final String licensePubinfoTemplateId = "https://w3id.org/np/RA0J4vUn_dekg-U1kK3AOEt02p9mT2WO03uGxLDec1jLw";
    private static final String defaultProvTemplateId = "https://w3id.org/np/RA7lSq6MuK_TIC6JMSHvLtee3lpLoZDOqLJCLXevnrPoU";

    /**
     * Creates a form preview for a template nanopub that may not yet be published.
     *
     * @param id the Wicket component ID
     * @param templateNanopub the nanopub that defines the assertion template to preview
     */
    public TemplateFormPreview(String id, Nanopub templateNanopub) {
        super(id);

        TemplateData td = TemplateData.get();

        // Register the template from the nanopub so it can be looked up by TemplateContext
        Template template = td.registerTemplate(templateNanopub);
        String templateId = templateNanopub.getUri().stringValue();

        String targetNamespace = template.getTargetNamespace();
        if (targetNamespace == null) {
            targetNamespace = Template.DEFAULT_TARGET_NAMESPACE;
        }
        targetNamespace = targetNamespace + "~~~ARTIFACTCODE~~~/";

        // Assertion context
        TemplateContext assertionContext = new TemplateContext(ContextType.ASSERTION, templateId, "statement", targetNamespace);
        assertionContext.initStatements();
        assertionContext.finalizeStatements();

        // Provenance context
        String prTemplateId;
        if (template.getDefaultProvenance() != null) {
            prTemplateId = template.getDefaultProvenance().stringValue();
        } else {
            prTemplateId = defaultProvTemplateId;
        }
        TemplateContext provenanceContext = new TemplateContext(ContextType.PROVENANCE, prTemplateId, "pr-statement", targetNamespace);
        provenanceContext.initStatements();
        provenanceContext.finalizeStatements();

        // Pubinfo contexts
        List<TemplateContext> pubInfoContexts = new ArrayList<>();
        pubInfoContexts.add(new TemplateContext(ContextType.PUBINFO, creatorPubinfoTemplateId, "pi-statement", targetNamespace));
        pubInfoContexts.add(new TemplateContext(ContextType.PUBINFO, licensePubinfoTemplateId, "pi-statement", targetNamespace));
        for (IRI r : template.getRequiredPubinfoElements()) {
            String rId = r.stringValue();
            boolean alreadyAdded = false;
            for (TemplateContext c : pubInfoContexts) {
                if (c.getTemplate().getId().equals(rId)) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                pubInfoContexts.add(new TemplateContext(ContextType.PUBINFO, rId, "pi-statement", targetNamespace));
            }
        }
        for (TemplateContext c : pubInfoContexts) {
            c.initStatements();
            c.finalizeStatements();
        }

        // Build the form (needed for Wicket form components to render, but no submit action)
        Form<?> form = new Form<Void>("form");

        // Assertion section
        form.add(new BookmarkablePageLink<Void>("templatelink", ExplorePage.class, new PageParameters().set("id", templateId)));
        form.add(new Label("templatename", template.getLabel()));
        form.add(new Label("templatedesc", template.getDescription()).setEscapeModelStrings(false));

        form.add(new ListView<StatementItem>("statements", assertionContext.getStatementItems()) {
            protected void populateItem(ListItem<StatementItem> item) {
                item.add(item.getModelObject());
            }
        });

        // Provenance section
        form.add(new Label("prtemplatename", provenanceContext.getTemplate().getLabel()));
        form.add(new BookmarkablePageLink<Void>("prtemplatelink", ExplorePage.class, new PageParameters().set("id", provenanceContext.getTemplate().getId())));

        form.add(new ListView<StatementItem>("pr-statements", provenanceContext.getStatementItems()) {
            protected void populateItem(ListItem<StatementItem> item) {
                item.add(item.getModelObject());
            }
        });

        // Pubinfo section
        form.add(new ListView<TemplateContext>("pis", pubInfoContexts) {
            protected void populateItem(ListItem<TemplateContext> item) {
                TemplateContext pic = item.getModelObject();
                item.add(new Label("pitemplatename", pic.getTemplate().getLabel()));
                item.add(new BookmarkablePageLink<Void>("pitemplatelink", ExplorePage.class, new PageParameters().set("id", pic.getTemplate().getId())));
                item.add(new ListView<StatementItem>("pi-statements", pic.getStatementItems()) {
                    protected void populateItem(ListItem<StatementItem> item) {
                        item.add(item.getModelObject());
                    }
                });
            }
        });

        add(form);
    }

}
