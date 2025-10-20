package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.component.DifferentKeyErrorItem;
import com.knowledgepixels.nanodash.component.PublishForm;
import com.knowledgepixels.nanodash.component.TemplateList;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Random;

/**
 * The PublishPage class is responsible for rendering the application's publish page.
 * It allows users to publish content based on templates and manage forms.
 */
public class PublishPage extends NanodashPage {

    /**
     * The mount path for the PublishPage.
     */
    public static final String MOUNT_PATH = "/publish";

    private boolean autoRefresh = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructs a new PublishPage with the given parameters.
     *
     * @param parameters The parameters for the page, which may include template and form object identifiers.
     */
    public PublishPage(final PageParameters parameters) {
        super(parameters);

        final NanodashSession session = NanodashSession.get();
        add(new TitleBar("titlebar", this, "publish"));
        if (parameters.get("template").toString() != null) {
            session.redirectToLoginIfNeeded(MOUNT_PATH, parameters);
            if (!parameters.get("sigkey").isNull() && !parameters.get("sigkey").toString().equals(session.getPubkeyString())) {
                add(new DifferentKeyErrorItem("form", parameters));
            } else {
                if (!parameters.contains("formobj")) {
                    throw new RestartResponseException(getClass(), parameters.add("formobj", Math.abs(new Random().nextLong()) + ""));
                }
                String formObjId = parameters.get("formobj").toString();
                if (!session.hasForm(formObjId)) {
                    PublishForm publishForm = new PublishForm("form", parameters, getClass(), ExplorePage.class);
                    session.setForm(formObjId, publishForm);
                }
                add(session.getForm(formObjId));
            }
            add(new Label("templatelist").setVisible(false));
        } else {
            autoRefresh = true;
            add(new Label("form").setVisible(false));
            add(new TemplateList("templatelist"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        // TODO: There is probably a better place to define this function:
        response.render(JavaScriptHeaderItem.forScript(
                "function disableTooltips() { $('.select2-selection__rendered').prop('title', ''); }\n" +
                //"$(document).ready(function() { $('.select2-static').select2(); });",  // for static select2 textfields
                "",
                "custom-functions"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasAutoRefreshEnabled() {
        return autoRefresh;
    }

}
