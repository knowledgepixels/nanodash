package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.NanopubLookup;
import com.knowledgepixels.nanodash.component.TitleBar;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * The page shown when a nanopublication that was asked for couldn't be retrieved, even
 * though the identifier itself is well-formed. This is a normal outcome rather than a
 * malfunction — the nanopublication may never have been published, may have been
 * published to a network this instance doesn't reach, or may not have propagated to the
 * registries yet — so it gets its own page instead of the generic error page. A
 * malformed identifier is a different matter and goes to {@link ErrorPage} with the
 * details of what is wrong with it.
 *
 * @see NanopubLookup
 */
public class NanopubNotFoundPage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/nanopub-not-found";

    /**
     * Page parameter holding the identifier that couldn't be resolved.
     */
    public static final String ID_PARAM = "id";

    /**
     * Page parameter set when the lookup ran out of time instead of coming back empty.
     */
    public static final String TIMEOUT_PARAM = "timeout";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    private final boolean timedOut;

    /**
     * Shows the outcome of a failed nanopublication lookup on the page it belongs on, by
     * aborting the current request: this page when the identifier is well-formed but
     * doesn't resolve, the generic error page (with details) when it is malformed.
     *
     * @param lookup the failed lookup
     * @throws RestartResponseException always, to render the other page instead
     * @throws IllegalArgumentException if the lookup did find a nanopublication
     */
    public static void forwardFor(NanopubLookup lookup) {
        switch (lookup.getStatus()) {
            case FOUND -> throw new IllegalArgumentException("Nanopublication was found, nothing to forward to");
            case INVALID_ID -> throw new RestartResponseException(ErrorPage.class,
                    new PageParameters().set(ErrorPage.MESSAGE_PARAM, lookup.getErrorMessage()));
            default -> {
                PageParameters params = new PageParameters().set(ID_PARAM, lookup.getId());
                if (lookup.getStatus() == NanopubLookup.Status.TIMEOUT) {
                    params.set(TIMEOUT_PARAM, "true");
                }
                throw new RestartResponseException(NanopubNotFoundPage.class, params);
            }
        }
    }

    /**
     * Constructor for NanopubNotFoundPage.
     *
     * @param parameters Page parameters containing the identifier that couldn't be
     *                   resolved, and optionally whether the lookup timed out.
     */
    public NanopubNotFoundPage(final PageParameters parameters) {
        super(parameters);
        add(new TitleBar("titlebar", this));

        String id = parameters.get(ID_PARAM).toString("");
        timedOut = parameters.get(TIMEOUT_PARAM).toBoolean(false);

        add(new Label("explanation", timedOut
                ? "Looking it up took too long, so it couldn't be shown here. It may well be out there: the network" +
                  " is slow to answer right now, or the nanopublication sits on a registry that is taking its time." +
                  " Trying again in a moment is likely to help."
                : "It may never have been published or it may have been published in a registry not connected to this instance and not propagated to others yet."));
        add(new Label("nanopub-id", id).setVisible(!id.isEmpty()));

        // Retrying is worth offering: a lookup that timed out here keeps running in the
        // background, so the nanopublication may well be in the cache by now.
        add(new BookmarkablePageLink<Void>("retry-link", ExplorePage.class, new PageParameters().set("id", id))
                .add(NavigationContext.pageContextFallback())
                .setVisible(!id.isEmpty()));
        add(new BookmarkablePageLink<Void>("home-link", HomePage.class));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Answers with the HTTP status that matches the reason the nanopublication couldn't
     * be shown, so that clients other than browsers can tell the two apart too.
     */
    @Override
    protected void configureResponse(WebResponse response) {
        super.configureResponse(response);
        response.setStatus(timedOut ? HttpServletResponse.SC_GATEWAY_TIMEOUT : HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isVersioned() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isErrorPage() {
        return true;
    }

}
