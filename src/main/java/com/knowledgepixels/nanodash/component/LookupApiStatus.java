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

/**
 * Says under a field whether the APIs it is about can be reached (issue #763): in green if they
 * all can, in red, naming the ones that cannot, with a button to try them again, if any cannot.
 * A guided choice field is about the APIs it looks its values up in; the field in which a
 * template gives such an API, as the object of {@code nt:possibleValuesFromApi}, is about the API
 * entered, and is checked again whenever it changes, including when the form fills it in. The APIs are contacted in a follow-up Ajax
 * request, so the form does not wait for them, unless it is known from the last minutes whether
 * they can be reached.
 */
public class LookupApiStatus extends Panel {

    static final String CHECK_ID = "check";

    private final List<String> fixedApis;

    private List<String> checkedApis;

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
        if (!(entered instanceof String api) || api.isBlank()) return List.of();
        return List.of(api.strip());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onConfigure() {
        super.onConfigure();
        if (!getApis().equals(checkedApis)) replace(newCheck());
        setVisible(!checkedApis.isEmpty());
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
                return new Label(markupId, "checking the lookup " + apiNoun(apis.size()) + "...")
                        .add(AttributeModifier.replace("class", "api-status-checking"));
            }

        };
    }

    private void retry(AjaxRequestTarget target, List<String> apis) {
        LookupApis.forgetReachability(apis);
        replace(newCheck());
        target.add(this);
    }

    /**
     * Returns the message for the outcome of a check.
     *
     * @param reachability whether each API is reachable, by URL
     * @return the message
     */
    static String getMessage(Map<String, Boolean> reachability) {
        List<String> unreachable = getUnreachable(reachability);
        if (unreachable.isEmpty()) return "lookup " + apiNoun(reachability.size()) + " reachable";
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
     * The outcome of a check, with a button to check again if an API could not be reached.
     */
    private class Outcome extends Fragment {

        Outcome(String id, List<String> apis) {
            super(id, "outcome", LookupApiStatus.this);
            Map<String, Boolean> reachability = LookupApis.checkReachability(apis);
            boolean allReachable = getUnreachable(reachability).isEmpty();
            add(new Label("message", getMessage(reachability))
                    .add(AttributeModifier.replace("class", allReachable ? "api-status-ok" : "api-status-failed"))
                    .add(AttributeModifier.replace("title", String.join("\n", getUnreachable(reachability)))));
            add(new AjaxLink<Void>("retry") {

                @Override
                public void onClick(AjaxRequestTarget target) {
                    retry(target, apis);
                }

            }.setVisible(!allReachable));
        }

    }

}
