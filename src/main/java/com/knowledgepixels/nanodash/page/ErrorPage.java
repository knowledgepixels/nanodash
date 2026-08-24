package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.TitleBar;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * The page shown when a request can't be answered and there is no more fitting page for
 * the outcome. Besides saying what went wrong, it says whose problem it is — that differs
 * per error, and with it what the user can do about it — and offers a way onward: back to
 * the page they came from, home, and, for the errors that are Nanodash's own, a way to
 * report them.
 *
 * @see Kind
 */
public class ErrorPage extends NanodashPage {

    /**
     * The mount path for the error page.
     */
    public static final String MOUNT_PATH = "/error";

    /**
     * Page parameter holding an optional message with details about what went wrong. It
     * is shown to the user, so it should explain the problem in plain words rather than
     * expose internals.
     */
    public static final String MESSAGE_PARAM = "message";

    /**
     * Page parameter holding the {@link Kind} of error, by lower-case name. Callers that
     * know what kind of problem they ran into set it; without it the page assumes the
     * error is Nanodash's own, since an unforeseen one usually is.
     */
    public static final String KIND_PARAM = "kind";

    /**
     * Where the errors that are Nanodash's own get reported.
     */
    private static final String ISSUE_TRACKER_URL = "https://github.com/knowledgepixels/nanodash/issues/new";

    /**
     * 422, for content that was understood but can't be worked with. Not among the
     * constants of {@link HttpServletResponse}.
     */
    private static final int SC_UNPROCESSABLE_CONTENT = 422;

    /**
     * What kind of problem an error is, which is to say who can act on it: the user, the
     * author of the thing that was asked for, or Nanodash itself. It determines what the
     * page says around the {@link #MESSAGE_PARAM details of the error}, whether it offers
     * a way to report it, and the HTTP status it is answered with, so that clients other
     * than browsers can tell these apart too.
     */
    public enum Kind {

        /**
         * Something in the address is wrong: a mistyped identifier, a missing parameter.
         * The user, or whoever wrote the link, can correct it.
         */
        REQUEST(HttpServletResponse.SC_BAD_REQUEST, "\u2753", "That request can't be answered",
                "Something in the address that led here isn't right. If you typed or edited it, correcting it is" +
                " what puts this straight; if you followed a link, it is the link that is at fault rather than" +
                " what it points at."),

        /**
         * There is no page at the address that was asked for. Reached through the
         * container's 404 handling rather than from within the application.
         */
        NOT_FOUND(HttpServletResponse.SC_NOT_FOUND, "\uD83D\uDD0D", "There is no page at this address",
                "The address that led here doesn't match any page of Nanodash. It may be mistyped, or it may be" +
                " out of date, from a time when it led somewhere."),

        /**
         * What was asked for exists but can't be used as published — an invalid query,
         * for instance. Only its author can put it right, by publishing a corrected
         * version; nanopublications can't be changed after the fact.
         */
        CONTENT(SC_UNPROCESSABLE_CONTENT, "\uD83D\uDCC4", "That can't be shown as it was published",
                "The problem is in the published nanopublication itself, which nothing on this end can change:" +
                " only its author can put it right, by publishing a corrected version of it."),

        /**
         * Nanodash malfunctioned. Ours to fix, and worth reporting, since the alternative
         * is that it dead-ends at a stack trace in the server log.
         */
        MALFUNCTION(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "\uD83D\uDCA5", "Something went wrong here",
                "This one is on Nanodash rather than on you or on what you asked for. Reporting it is what gets" +
                " it looked at; the server log alone doesn't say what you were trying to do.");

        private final int httpStatus;
        private final String emoji;
        private final String heading;
        private final String responsibility;

        Kind(int httpStatus, String emoji, String heading, String responsibility) {
            this.httpStatus = httpStatus;
            this.emoji = emoji;
            this.heading = heading;
            this.responsibility = responsibility;
        }

        /**
         * The HTTP status this kind of error is answered with.
         *
         * @return the status code
         */
        public int getHttpStatus() {
            return httpStatus;
        }

        /**
         * The pictogram shown beside the heading, as the sibling error pages have one.
         *
         * @return the emoji
         */
        public String getEmoji() {
            return emoji;
        }

        /**
         * The headline of the page, naming the situation.
         *
         * @return the heading
         */
        public String getHeading() {
            return heading;
        }

        /**
         * What the page says about who can act on this kind of error, and how.
         *
         * @return the explanation
         */
        public String getResponsibility() {
            return responsibility;
        }

        /**
         * The value this kind is passed as in the {@link #KIND_PARAM} page parameter.
         *
         * @return the parameter value
         */
        public String getParamValue() {
            return name().toLowerCase(Locale.ROOT);
        }

        /**
         * The kind denoted by the given {@link #getParamValue() parameter value}.
         *
         * @param value the parameter value, in any case
         * @return the kind, or null if the value denotes none
         */
        public static Kind byParamValue(String value) {
            if (value == null || value.isBlank()) return null;
            for (Kind kind : values()) {
                if (kind.name().equalsIgnoreCase(value.trim())) return kind;
            }
            return null;
        }

        /**
         * The kind that fits an HTTP status the servlet container has already settled on,
         * for the errors that reach this page through the container's error handling
         * rather than from within the application.
         *
         * @param status the status code the container answered with
         * @return the matching kind
         */
        public static Kind byHttpStatus(int status) {
            if (status == HttpServletResponse.SC_NOT_FOUND) return NOT_FOUND;
            if (status >= 400 && status < 500) return REQUEST;
            return MALFUNCTION;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    private final Kind kind;

    /**
     * Default constructor for ErrorPage.
     * Initializes the page with the provided parameters.
     *
     * @param parameters Page parameters to initialize the page.
     */
    public ErrorPage(final PageParameters parameters) {
        super(parameters);
        add(new TitleBar("titlebar", this));

        HttpServletRequest containerRequest = getContainerRequest();
        kind = resolveKind(parameters, containerRequest);
        String message = parameters.get(MESSAGE_PARAM).toString("");
        String address = getFailingAddress(containerRequest);
        String backUrl = getBackUrl(containerRequest);

        add(new Label("pagetitle", kind.getHeading() + " | nanodash"));
        add(new Label("heading", kind.getEmoji() + " " + kind.getHeading()));
        add(new Label("message", message).setVisible(!message.isBlank()));
        add(new Label("responsibility", kind.getResponsibility()));
        // The address is worth spelling out where it is the whole of the problem; elsewhere
        // the message says what was wrong with what was asked for.
        add(new Label("address", address).setVisible(kind == Kind.NOT_FOUND && address != null));

        add(new ExternalLink("back-link", backUrl).setVisible(backUrl != null));
        add(new BookmarkablePageLink<Void>("home-link", HomePage.class));
        add(new ExternalLink("report-link", getReportUrl(message, address, backUrl))
                .setVisible(kind == Kind.MALFUNCTION));
    }

    /**
     * The kind of error to answer with: what the caller said it is, or else what the
     * container's error handling implies, or else — for an error nobody foresaw — one of
     * Nanodash's own.
     */
    private static Kind resolveKind(PageParameters parameters, HttpServletRequest request) {
        Kind named = Kind.byParamValue(parameters.get(KIND_PARAM).toString(""));
        if (named != null) return named;
        Integer containerStatus = getContainerErrorStatus(request);
        if (containerStatus != null) return Kind.byHttpStatus(containerStatus);
        return Kind.MALFUNCTION;
    }

    private HttpServletRequest getContainerRequest() {
        Request request = getRequest();
        if (request != null && request.getContainerRequest() instanceof HttpServletRequest containerRequest) {
            return containerRequest;
        }
        return null;
    }

    /**
     * The status the servlet container settled on before dispatching to this page, for the
     * errors (a 404 on an address that maps to no page, an exception that got as far as
     * the container) that arrive through {@code web.xml}'s error-page mapping. Null when
     * this page was reached from within the application instead.
     */
    private static Integer getContainerErrorStatus(HttpServletRequest request) {
        if (request == null) return null;
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status instanceof Integer code) return code;
        if (status == null) return null;
        try {
            return Integer.valueOf(status.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * The address the failed request was for: the one the container was dispatching for
     * where it dispatched, and the current one otherwise (a page that forwards here does
     * so within the request, so the address stays the one that was asked for).
     */
    private static String getFailingAddress(HttpServletRequest request) {
        if (request == null) return null;
        Object errorUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (errorUri != null && !errorUri.toString().isBlank()) return errorUri.toString();
        StringBuffer url = request.getRequestURL();
        if (url == null) return null;
        String query = request.getQueryString();
        return query == null || query.isBlank() ? url.toString() : url + "?" + query;
    }

    /**
     * Where the user was before they landed here, so that an error isn't a dead end.
     * Only this instance's own pages qualify: an off-site referrer is not where the user
     * was working, and the error page itself is no way back either.
     */
    private static String getBackUrl(HttpServletRequest request) {
        if (request == null) return null;
        String referrer = request.getHeader("Referer");
        if (referrer == null || referrer.isBlank()) return null;
        URI uri;
        try {
            uri = new URI(referrer);
        } catch (URISyntaxException ex) {
            return null;
        }
        if (uri.getScheme() == null || uri.getHost() == null) return null;
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) return null;
        if (!uri.getHost().equalsIgnoreCase(request.getServerName())) return null;
        String path = uri.getPath() == null ? "" : uri.getPath();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.equals(MOUNT_PATH) || path.startsWith(MOUNT_PATH + "/")) return null;
        return referrer;
    }

    /**
     * A pre-filled issue on the tracker, so that reporting a malfunction takes no more
     * than following a link and pressing a button, and arrives with what is needed to
     * place it: what the user saw, where, and which version said it.
     */
    private static String getReportUrl(String message, String address, String cameFrom) {
        String title = message.isBlank()
                ? "Something went wrong in Nanodash"
                : "Error: " + Utils.truncateLabel(message);
        StringBuilder body = new StringBuilder();
        body.append("What Nanodash said:\n\n> ")
                .append(message.isBlank() ? "(no details were given)" : message)
                .append("\n\n");
        body.append("What I was doing:\n\n(please describe)\n\n");
        if (address != null) body.append("Address: ").append(address).append("\n");
        if (cameFrom != null) body.append("Came from: ").append(cameFrom).append("\n");
        body.append("Nanodash version: ").append(WicketApplication.getThisVersion()).append("\n");
        return ISSUE_TRACKER_URL + "?title=" + Utils.urlEncode(title) + "&body=" + Utils.urlEncode(body.toString());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Answers with the HTTP status that matches the {@link Kind} of error, so that
     * clients other than browsers can tell these apart too.
     */
    @Override
    protected void configureResponse(WebResponse response) {
        super.configureResponse(response);
        response.setStatus(kind.getHttpStatus());
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
