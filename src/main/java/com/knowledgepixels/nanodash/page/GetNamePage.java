package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * This page retrieves the display name of a user based on their ID.
 * It is used to provide a simple way to get usernames for display purposes.
 */
public class GetNamePage extends NanodashPage {

    private static final long serialVersionUID = 1L;

    /**
     * The mount path for the GetNamePage.
     */
    public static final String MOUNT_PATH = "/get-name";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for the GetNamePage.
     *
     * @param parameters Page parameters containing the user ID.
     */
    public GetNamePage(final PageParameters parameters) {
        super(parameters);
        if (parameters.contains("id")) {
            String name = User.getShortDisplayName(Utils.vf.createIRI(parameters.get("id").toString()));
            add(new Label("name", name));
        } else {
            throw new IllegalArgumentException("argument 'id' not found");
        }
        // TODO return Content-Type text/plain instead of text/html
    }

}
