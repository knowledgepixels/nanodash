package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.page.ExplorePage;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;

/**
 * External link with Actions Panel that allows to copy the link and explore it through the Nanodash ExplorePage.
 */
public class ExternalLinkWithActionsPanel extends Panel {

    private final IModel<String> urlModel;
    private final IModel<String> labelModel;

    public ExternalLinkWithActionsPanel(String id, IModel<String> urlModel, IModel<String> labelModel) {
        super(id);
        this.urlModel = urlModel;
        this.labelModel = labelModel;
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

        AjaxLink<Void> exploreButton = new AjaxLink<>("exploreButton") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(ExplorePage.class, new PageParameters().set("id", urlModel.getObject()).set("label", labelModel.getObject()));
            }
        };
        exploreButton.add(new Label("exploreLabel", Model.of("Explore")));
        add(exploreButton);
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        urlModel.detach();
        labelModel.detach();
    }

}
