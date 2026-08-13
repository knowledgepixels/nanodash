package com.knowledgepixels.nanodash.page;

import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Forwards the URLs of the retired list pages (users, spaces, queries, search)
 * to the home page, which shows their content as view sections now. Each
 * retired path is mounted on its own subclass: mounting one class on several
 * paths would make Wicket first redirect to the class's canonical mount,
 * turning every forward into a double redirect.
 */
public abstract class HomeForwarder extends NanodashPage {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return HomePage.MOUNT_PATH;
    }

    /**
     * Constructor that redirects to the home page.
     *
     * @param parameters The page parameters (ignored).
     */
    public HomeForwarder(final PageParameters parameters) {
        super(parameters);
        throw new RedirectToUrlException(HomePage.MOUNT_PATH);
    }

    /**
     * Forwards the retired user-list page URL to the home page.
     */
    public static class UserList extends HomeForwarder {

        /**
         * The mount path of the retired user-list page.
         */
        public static final String MOUNT_PATH = "/userlist";

        /**
         * Constructor that redirects to the home page.
         *
         * @param parameters The page parameters (ignored).
         */
        public UserList(final PageParameters parameters) {
            super(parameters);
        }
    }

    /**
     * Forwards the retired space-list page URL to the home page.
     */
    public static class SpaceList extends HomeForwarder {

        /**
         * The mount path of the retired space-list page.
         */
        public static final String MOUNT_PATH = "/spaces";

        /**
         * Constructor that redirects to the home page.
         *
         * @param parameters The page parameters (ignored).
         */
        public SpaceList(final PageParameters parameters) {
            super(parameters);
        }
    }

    /**
     * Forwards the retired query-list page URL to the home page.
     */
    public static class QueryList extends HomeForwarder {

        /**
         * The mount path of the retired query-list page.
         */
        public static final String MOUNT_PATH = "/queries";

        /**
         * Constructor that redirects to the home page.
         *
         * @param parameters The page parameters (ignored).
         */
        public QueryList(final PageParameters parameters) {
            super(parameters);
        }
    }

    /**
     * Forwards the retired search page URL to the home page.
     */
    public static class Search extends HomeForwarder {

        /**
         * The mount path of the retired search page.
         */
        public static final String MOUNT_PATH = "/search";

        /**
         * Constructor that redirects to the home page.
         *
         * @param parameters The page parameters (ignored).
         */
        public Search(final PageParameters parameters) {
            super(parameters);
        }
    }

}
