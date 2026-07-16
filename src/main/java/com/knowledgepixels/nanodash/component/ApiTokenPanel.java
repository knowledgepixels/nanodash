package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiTokenService;
import com.knowledgepixels.nanodash.NanodashPreferences;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Management of the user's personal API tokens for remote MCP access (see
 * docs/remote-mcp.md): list, create, and revoke. Shown on the user's own
 * About tab when remote MCP access is enabled. A newly created token is
 * displayed once, together with ready-to-use client setup instructions, and
 * cannot be retrieved again afterwards.
 */
public class ApiTokenPanel extends Panel {

    private final String userIri;
    private String newToken;

    /**
     * @param id      the Wicket markup id
     * @param userIri the IRI of the user whose (own) About page this is
     */
    public ApiTokenPanel(String id, String userIri) {
        super(id);
        this.userIri = userIri;
        // Create/revoke work via Ajax on this panel only: a full-page render
        // could hit the auto-refresh redirect (NanodashPage.onRender), which
        // recreates the page and would lose the one-time newToken field.
        setOutputMarkupId(true);

        String mcpUrl = NanodashPreferences.get().getWebsiteUrl();
        if (!mcpUrl.endsWith("/")) mcpUrl += "/";
        mcpUrl += "mcp";
        final String endpointUrl = mcpUrl;

        add(new ListView<ApiTokenService.ApiToken>("tokens", (IModel<List<ApiTokenService.ApiToken>>) () ->
                ApiTokenService.get().getTokens(userIri)) {

            @Override
            protected void populateItem(ListItem<ApiTokenService.ApiToken> item) {
                ApiTokenService.ApiToken token = item.getModelObject();
                item.add(new Label("label", token.getLabel()));
                item.add(new Label("hashhint", token.getTokenHash().substring(0, 8)));
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                item.add(new Label("created", df.format(new Date(token.getCreated()))));
                item.add(new Label("lastused", token.getLastUsed() == 0 ? "never" : df.format(new Date(token.getLastUsed()))));
                item.add(new AjaxLink<Void>("revoke") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ApiTokenService.get().revokeToken(userIri, token.getTokenHash());
                        target.add(ApiTokenPanel.this);
                    }

                });
            }

        });

        final Model<String> labelModel = Model.of("");
        Form<Void> createForm = new Form<>("createform");
        createForm.add(new TextField<>("labelfield", labelModel));
        createForm.add(new AjaxButton("create") {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                newToken = ApiTokenService.get().createToken(userIri, labelModel.getObject());
                labelModel.setObject("");
                target.add(ApiTokenPanel.this);
            }

        });
        add(createForm);

        WebMarkupContainer newTokenBox = new WebMarkupContainer("newtoken") {

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(newToken != null);
            }

        };
        newTokenBox.add(new Label("token", (IModel<String>) () -> newToken));
        newTokenBox.add(new Label("endpoint", endpointUrl));
        newTokenBox.add(new Label("claudecommand", (IModel<String>) () ->
                "claude mcp add --transport http nanodash " + endpointUrl + " --header \"Authorization: Bearer " + newToken + "\""));
        add(newTokenBox);
    }

}
