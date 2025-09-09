package com.knowledgepixels.nanodash.page;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponseEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.NanopubResults;
import com.knowledgepixels.nanodash.component.TitleBar;

/**
 * SearchPage allows users to search for nanopublications by URI or free text.
 */
public class SearchPage extends NanodashPage {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SearchPage.class);

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/search";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    private TextField<String> searchField;
    private CheckBox filterUser;
    private Model<String> progress;

    private Map<String, String> pubKeyMap;
    private RadioChoice<String> pubkeySelection;

    /**
     * Constructor for SearchPage.
     *
     * @param parameters Page parameters containing the search query, filter option, and public key.
     */
    public SearchPage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, "search"));

        final String searchText = parameters.get("query").toString();
        final Boolean filterCheck = Boolean.valueOf(parameters.get("filter").toString());
        final String pubkey = parameters.get("pubkey").toString();

        Form<?> form = new Form<Void>("form") {

            private static final long serialVersionUID = 1L;

            protected void onSubmit() {
                String searchText = searchField.getModelObject().trim();
                Boolean filterCheck = filterUser.getModelObject();
                String pubkey = pubkeySelection.getModelObject();
                PageParameters params = new PageParameters();
                params.add("query", searchText);
                params.add("filter", filterCheck);
                if (pubkey != null) params.add("pubkey", pubkey);
                setResponsePage(SearchPage.class, params);
            }
        };
        add(form);

        form.add(searchField = new TextField<String>("search", Model.of(searchText)));
        WebMarkupContainer ownFilter = new WebMarkupContainer("own-filter");
        ownFilter.add(filterUser = new CheckBox("filter", Model.of(filterCheck)));
        NanodashSession session = NanodashSession.get();
        ownFilter.setVisible(!NanodashPreferences.get().isReadOnlyMode() && session.getUserIri() != null);
        ArrayList<String> pubKeyList = new ArrayList<>();
        if (session.getUserIri() != null) {
            pubKeyMap = new HashMap<>();
            String lKeyShort = Utils.getShortPubkeyhashLabel(session.getPubkeyString(), session.getUserIri());
            pubKeyList.add(lKeyShort);
            pubKeyMap.put(lKeyShort, session.getPubkeyString());
            for (String pk : User.getPubkeyhashes(session.getUserIri(), null)) {
                String keyShort = Utils.getShortPubkeyhashLabel(pk, session.getUserIri());
                if (!pubKeyMap.containsKey(keyShort)) {
                    pubKeyList.add(keyShort);
                    pubKeyMap.put(keyShort, pk);
                }
            }
        }

        pubkeySelection = new RadioChoice<String>("pubkeygroup", Model.of(pubkey), pubKeyList);
        if (!pubKeyList.isEmpty() && pubkeySelection.getModelObject() == null) {
            pubkeySelection.setDefaultModelObject(pubKeyList.get(0));
        }
        ownFilter.add(pubkeySelection);

        form.add(ownFilter);

        // TODO: Progress bar doesn't update at the moment:
        progress = new Model<>();
        final Label progressLabel = new Label("progress", progress);
        progressLabel.setOutputMarkupId(true);
        progressLabel.add(new AjaxSelfUpdatingTimerBehavior(Duration.ofMillis(1000)));
        add(progressLabel);

        if (searchText == null || searchText.isEmpty()) {
            add(new Label("nanopubs", "Enter a search term above."));
        } else {
            add(new AjaxLazyLoadPanel<NanopubResults>("nanopubs") {

                private static final long serialVersionUID = 1L;

                @Override
                public NanopubResults getLazyLoadComponent(String markupId) {
                    Map<String, String> nanopubParams = new HashMap<>();
                    List<ApiResponseEntry> nanopubResults = new ArrayList<>();
                    String s = searchText;
                    if (s != null) {
                        s = s.trim();
                        if (s.matches("https?://[^\\s]+")) {
                            logger.info("URI QUERY: {}", s);
                            nanopubParams.put("ref", s);
                            if (Boolean.TRUE.equals(filterCheck)) {
                                String pubkey = pubKeyMap.get(pubkeySelection.getModelObject());
                                logger.info("Filter for PUBKEY: {}", pubkey);
                                nanopubParams.put("pubkey", pubkey);
                            }
                            try {
                                // nanopubResults = ApiAccess.getAll("find_nanopubs_with_uri", nanopubParams).getData();
                                nanopubResults = QueryApiAccess.get("find-uri-references", nanopubParams).getData();
                            } catch (Exception ex) {
                                logger.error("Error while running the query for URI", ex);
                            }
//							nanopubResults = ApiAccess.getRecent("find_nanopubs_with_uri", nanopubParams, progress);
                        } else {
                            String freeTextQuery = getFreeTextQuery(s);
                            if (!freeTextQuery.isEmpty()) {
                                logger.info("FREE TEXT QUERY: {}", freeTextQuery);
                                nanopubParams.put("query", freeTextQuery);
                                if (filterCheck != null && Boolean.TRUE.equals(filterCheck)) {
                                    String pubkey = pubKeyMap.get(pubkeySelection.getModelObject());
                                    logger.info("Filter for PUBKEY: {}", pubkey);
                                    nanopubParams.put("pubkey", pubkey);
                                }
                                try {
                                    // nanopubResults = ApiAccess.getAll("find_nanopubs_with_text", nanopubParams).getData();
                                    nanopubResults = QueryApiAccess.get("fulltext-search-on-labels", nanopubParams).getData();
                                } catch (Exception ex) {
                                    logger.error("Error during search", ex);
                                }
//								nanopubResults = ApiAccess.getRecent("find_nanopubs_with_text", nanopubParams, progress);
                            }
                        }
                    }
                    nanopubResults.sort(new ApiResponseEntry.DataComparator());
                    List<String> nanopubIds = new ArrayList<>();
                    while (!nanopubResults.isEmpty() && nanopubIds.size() < 100) {
                        String npUri = nanopubResults.remove(0).get("np");
                        if (!nanopubIds.contains(npUri)) nanopubIds.add(npUri);
                    }
                    progress.setObject("");
                    if (nanopubIds.isEmpty()) progress.setObject("nothing found");
                    List<NanopubElement> nanopubs = new ArrayList<>();
                    for (String id : nanopubIds) nanopubs.add(NanopubElement.get(id));
                    return NanopubResults.fromList(markupId, nanopubs);
                }

                @Override
                protected void onContentLoaded(NanopubResults content, Optional<AjaxRequestTarget> target) {
                    super.onContentLoaded(content, target);
                    if (target.isPresent()) {
                        target.get().appendJavaScript("updateElements();");
                    }
                }

            });

        }
    }

    private static String getFreeTextQuery(String searchText) {
        String freeTextQuery = "";
        String previous = "AND";
        String preprocessed = "";
        boolean inQuote = true;
        for (String s : searchText.replaceAll("\\(\\s+", "(").replaceAll("\\s+\\)", ")").replaceAll("@", "").split("\"")) {
            inQuote = !inQuote;
            if (inQuote) {
                s = "\\\"" + String.join("@", s.split("[^\\p{L}0-9\\-_]+")) + "\\\"";
            }
            preprocessed += s;
        }
        preprocessed = preprocessed.trim();
        for (String s : preprocessed.split("[^\\p{L}0-9\\-_\\(\\)@\\\"\\\\]+")) {
            if (s.matches("[0-9].*")) continue;
            if (!s.matches("AND|OR|\\(+|\\)+|\\(?NOT")) {
                if (s.toLowerCase().matches("and|or|not")) {
                    // ignore lower-case and/or/not
                    continue;
                }
                if (!previous.matches("AND|OR|\\(?NOT")) {
                    freeTextQuery += " AND";
                }
            }
            freeTextQuery += " " + s.toLowerCase();
            previous = s;
        }
        freeTextQuery = freeTextQuery.replaceAll("@", " ").trim();
        return freeTextQuery;
    }

}
