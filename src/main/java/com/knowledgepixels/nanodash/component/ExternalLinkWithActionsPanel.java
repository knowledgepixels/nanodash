package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.component.menu.BaseDisplayMenu;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;
import org.eclipse.rdf4j.model.IRI;

/**
 * External link with Actions Panel that allows to copy the link and explore it through the Nanodash ExplorePage.
 */
public class ExternalLinkWithActionsPanel extends Panel {

    private final IModel<String> urlModel;
    private IModel<String> labelModel;
    private IRI sourceNanopub;
    private BaseDisplayMenu customMenu;

    public ExternalLinkWithActionsPanel(String id, IModel<String> urlModel) {
        super(id);
        this.urlModel = urlModel;
    }

    public ExternalLinkWithActionsPanel(String id, IModel<String> urlModel, IModel<String> labelModel) {
        this(id, urlModel);
        this.labelModel = labelModel;
    }

    public ExternalLinkWithActionsPanel(String id, IModel<String> urlModel, IModel<String> labelModel, IRI sourceNanopub) {
        this(id, urlModel, labelModel);
        this.sourceNanopub = sourceNanopub;
    }

    public ExternalLinkWithActionsPanel(String id, IModel<String> urlModel, IModel<String> labelModel, BaseDisplayMenu customMenu) {
        this(id, urlModel, labelModel);
        this.customMenu = customMenu;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        ExternalLink externalLink = new ExternalLink("externalLink", urlModel, urlModel);
        add(externalLink);

        AjaxLink<Void> copyLinkButton = new AjaxLink<>("copyLinkButton") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String url = urlModel.getObject();
                String escapedUrl = url.replace("'", "\\'");
                target.appendJavaScript(
                        "navigator.clipboard.writeText('" + escapedUrl + "')" +
                        ".then(function() { alert('Link copied to clipboard!'); })" +
                        ".catch(function(err) { console.error('Copy failed:', err); });"
                );
            }
        };
        copyLinkButton.add(new Image("copyIcon", new ContextRelativeResourceReference("images/copy-icon.svg", false)));
        add(copyLinkButton);

        // The "explore" action is now reachable via the resource's Explore tab,
        // so this panel no longer shows an explore button or an explore menu.
        // A custom menu (e.g. the space admin actions) is still shown when given.
        add(new Label("exploreButton", "").setVisible(false));
        if (customMenu != null) {
            add(customMenu);
        } else {
            add(new Label("np", "").setVisible(false));
        }
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        urlModel.detach();
        if (labelModel != null) {
            labelModel.detach();
        }
    }

}
