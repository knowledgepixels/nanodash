package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.LookupApis;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Says under a field that an API it is about cannot be reached (issues #763 and #815): in red,
 * naming the APIs that cannot, with a button to try them again. Only that is reported: while the
 * APIs are being checked, and once they have all answered, the line says nothing, so that it does
 * not clutter every field that works.
 * A guided choice field is about the APIs it looks its values up in; the field in which a
 * template gives such an API, as the object of {@code nt:possibleValuesFromApi}, is about the API
 * entered, and is checked again whenever it changes, including when the form fills it in. The APIs are contacted in a follow-up Ajax
 * request, so the form does not wait for them, unless it is known from the last minutes whether
 * they can be reached.
 */
public class LookupApiStatus extends Panel {

    static final String CHECK_ID = "check";

    /**
     * The class of a line that is checking APIs without saying so, which hides it.
     */
    static final String PENDING_CLASS = "api-status-pending";

    private final List<String> fixedApis;

    private List<String> checkedApis;

    private boolean showProgress;

    /**
     * Creates the status line of a field that looks its values up in the given APIs.
     *
     * @param id   the Wicket component ID
     * @param apis the API URLs the field looks its values up in; with none, nothing is shown
     */
    public LookupApiStatus(String id, List<String> apis) {
        this(id, new ArrayList<>(apis), null);
    }

    private LookupApiStatus(String id, List<String> fixedApis, IModel<String> apiModel) {
        super(id, apiModel);
        this.fixedApis = fixedApis;
        setOutputMarkupPlaceholderTag(true);
        IModel<String> pendingClass = () -> isCheckingSilently() ? PENDING_CLASS : null;
        add(AttributeModifier.append("class", pendingClass));
        add(newCheck());
    }

    /**
     * Creates the status line of a field in which an API URL is entered. It shares the model of
     * the field, so that the field, which updates the components with its model when it changes,
     * has the line check the new URL.
     *
     * @param id       the Wicket component ID
     * @param apiModel the model of the field; while it is empty, nothing is shown
     * @return the status line
     */
    public static LookupApiStatus forField(String id, IModel<String> apiModel) {
        return new LookupApiStatus(id, null, apiModel);
    }

    /**
     * Returns the APIs this line is about.
     *
     * @return the API URLs, empty if there are none
     */
    List<String> getApis() {
        if (fixedApis != null) return fixedApis;
        Object entered = getDefaultModelObject();
        if (!(entered instanceof String value)) return List.of();
        String api = unquote(value.strip()).strip();
        if (api.isEmpty()) return List.of();
        return List.of(api);
    }

    /**
     * Removes the quotation marks around a value. A field shown read-only, as when a template is
     * explored, holds a literal in quotation marks, as it is shown, which are not part of the URL.
     *
     * @param value the value of the field
     * @return the value without the quotation marks around it, if it has them
     */
    static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onConfigure() {
        super.onConfigure();
        if (!getApis().equals(checkedApis)) replace(newCheck());
        setVisible(hasAnythingToSay());
    }

    /**
     * Tells whether this line has anything to say. Only an API that cannot be reached is
     * reported, so the line is there while a check is still pending, in case it fails, and
     * otherwise only once it has failed.
     *
     * @return true if the line is to be rendered
     */
    private boolean hasAnythingToSay() {
        if (checkedApis.isEmpty()) return false;
        if (!LookupApis.isReachabilityKnown(checkedApis)) return true;
        return !getUnreachable(LookupApis.checkReachability(checkedApis)).isEmpty();
    }

    /**
     * Tells whether this line is checking APIs without showing anything, which it does unless the
     * check was asked for with the retry button. It is then hidden by {@link #PENDING_CLASS}
     * rather than left out, so that the Ajax request that does the check is still made.
     *
     * @return true if a silent check is pending
     */
    private boolean isCheckingSilently() {
        return !showProgress && !checkedApis.isEmpty() && !LookupApis.isReachabilityKnown(checkedApis);
    }

    private Component newCheck() {
        List<String> apis = getApis();
        checkedApis = apis;
        if (apis.isEmpty()) return new WebMarkupContainer(CHECK_ID);
        if (LookupApis.isReachabilityKnown(apis)) return new Outcome(CHECK_ID, apis);
        return new AjaxLazyLoadPanel<Component>(CHECK_ID) {

            @Override
            public Component getLazyLoadComponent(String markupId) {
                return new Outcome(markupId, apis);
            }

            @Override
            public Component getLoadingComponent(String markupId) {
                if (!showProgress) return new WebMarkupContainer(markupId);
                return new Label(markupId, "checking the lookup " + apiNoun(apis.size()) + "...")
                        .add(AttributeModifier.replace("class", "api-status-checking"));
            }

            @Override
            protected void onContentLoaded(Component content, Optional<AjaxRequestTarget> target) {
                showProgress = false;
                // The whole line is repainted, not just the outcome, because the line is hidden
                // unless the outcome is that an API cannot be reached.
                target.ifPresent(t -> t.add(LookupApiStatus.this));
            }

        };
    }

    private void retry(AjaxRequestTarget target, List<String> apis) {
        LookupApis.forgetReachability(apis);
        // A check the user asked for says that it is running, so that the button is seen to do
        // something even when the APIs stay unreachable.
        showProgress = true;
        replace(newCheck());
        target.add(this);
    }

    /**
     * Returns the message naming the APIs that cannot be reached.
     *
     * @param unreachable the URLs of the APIs that cannot be reached, at least one of them
     * @return the message
     */
    static String getMessage(List<String> unreachable) {
        List<String> hosts = unreachable.stream().map(LookupApiStatus::getHost).distinct().toList();
        return "lookup " + apiNoun(unreachable.size()) + " not reachable: " + String.join(", ", hosts);
    }

    private static List<String> getUnreachable(Map<String, Boolean> reachability) {
        return reachability.entrySet().stream().filter(e -> !e.getValue()).map(Map.Entry::getKey).toList();
    }

    private static String apiNoun(int count) {
        return count == 1 ? "API" : "APIs";
    }

    /**
     * Returns the host of an API URL, which names the API briefly.
     *
     * @param api the API URL
     * @return the host, or the URL itself if it has none
     */
    static String getHost(String api) {
        try {
            String host = new URI(api.replace(" ", "%20")).getHost();
            return host == null ? api : host;
        } catch (URISyntaxException ex) {
            return api;
        }
    }

    /**
     * The outcome of a check, shown only if an API could not be reached, with a button to check
     * again.
     */
    private class Outcome extends Fragment {

        Outcome(String id, List<String> apis) {
            super(id, "outcome", LookupApiStatus.this);
            List<String> unreachable = getUnreachable(LookupApis.checkReachability(apis));
            boolean failed = !unreachable.isEmpty();
            add(new Label("message", failed ? getMessage(unreachable) : "")
                    .add(AttributeModifier.replace("class", "api-status-failed"))
                    .add(AttributeModifier.replace("title", String.join("\n", unreachable)))
                    .setVisible(failed));
            add(new AjaxLink<Void>("retry") {

                @Override
                public void onClick(AjaxRequestTarget target) {
                    retry(target, apis);
                }

            }.setVisible(failed));
        }

    }

}
