package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.page.NanodashPage;
import com.knowledgepixels.nanodash.page.PublishPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.INamedParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * A button that links to the PublishPage with pre-filled parameters for adding a new view display.
 */
public class AddViewDisplayButton extends BookmarkablePageLink<NanodashPage> {

    public AddViewDisplayButton(String id, String template, String templateVersion, String context, String paramResource, PageParameters additionalPageParameters) {
        super(id, PublishPage.class, new PageParameters()
                .set("template", template)
                .set("template-version", templateVersion)
                .set("context", context)
                .set("param_resource", paramResource));

        for (INamedParameters.NamedPair param : additionalPageParameters.getAllNamed()) {
            getPageParameters().set(param.getKey(), param.getValue());
        }

        setBody(Model.of("+ view display..."));
    }

}
