package com.knowledgepixels.nanodash.page;

import com.googlecode.wicket.jquery.ui.form.datepicker.AjaxDatePicker;
import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.QueryRef;
import com.knowledgepixels.nanodash.component.NanopubResults;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.nanopub.extra.services.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.time.Instant.parse;

/**
 * A page that shows a list of nanopublications filtered by type, public key, and time range.
 */
public class ListPage extends NanodashPage {

    private static final long serialVersionUID = 1L;

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/list";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    private boolean added = false;
    private static final Logger logger = LoggerFactory.getLogger(ListPage.class);
    private final List<IRI> types = new ArrayList<>();
    private final List<String> pubKeys = new ArrayList<>();
    private String startTime = "";
    private String endTime = "";

    /**
     * Constructor for ListPage.
     *
     * @param parameters Page parameters containing filters like types, pubKeys, startTime, and endTime.
     */
    public ListPage(final PageParameters parameters) {
        super(parameters);
        logger.info("Rendering ListPage with '{}' mode.", NanodashSession.get().getNanopubResultsViewMode().getValue());

        WebMarkupContainer dateFilterContainer = new WebMarkupContainer("dateFilterContainer");


        add(new AjaxLink<>("listEnabler") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                NanodashSession.get().setNanopubResultsViewMode(NanopubResults.ViewMode.LIST);
                logger.info("ListEnabler -- Switched to '{}' mode", NanodashSession.get().getNanopubResultsViewMode().getValue());
            }
        });

        add(new AjaxLink<>("gridEnabler") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                NanodashSession.get().setNanopubResultsViewMode(NanopubResults.ViewMode.GRID);
                logger.info("GridEnabler -- Switched to '{}' mode", NanodashSession.get().getNanopubResultsViewMode().getValue());
            }
        });

        if (!parameters.get("types").isNull() && !parameters.get("types").isEmpty()) {
            Arrays.stream(parameters.get("types").toString().split(" ")).toList().forEach(type -> types.add(Values.iri(type)));
        }

        WebMarkupContainer typeFilterContainer = new WebMarkupContainer("typeFilterContainer");
        RepeatingView filteredTypes = new RepeatingView("typeNames");
        for (IRI type : types) {
            WebMarkupContainer typeContainer = new WebMarkupContainer(filteredTypes.newChildId());
            typeContainer.add(new Label("typeName", type.getLocalName()));
            typeContainer.add(new AjaxLink<Void>("removeType") {
                @Override
                protected void onInitialize() {
                    super.onInitialize();
                    add(new Label("crossIcon", "×"));
                }

                @Override
                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                    List<IRI> updatedTypes = types.stream()
                            .filter(t -> !t.equals(type))
                            .toList();
                    if (!updatedTypes.isEmpty()) {
                        parameters.set("types", updatedTypes.stream()
                                .map(IRI::stringValue)
                                .collect(Collectors.joining(" ")));
                    } else {
                        parameters.remove("types");
                    }
                    setResponsePage(ListPage.class, parameters);
                }
            });

            filteredTypes.add(typeContainer);
            filteredTypes.setVisible(!types.isEmpty());
        }
        add(typeFilterContainer);
        typeFilterContainer.setVisible(!types.isEmpty());
        typeFilterContainer.add(filteredTypes);

        if (!parameters.get("pubkeys").isNull() && !parameters.get("pubkeys").isEmpty()) {
            pubKeys.addAll(Arrays.stream(parameters.get("pubkeys").toString().split(" ")).toList());
        }

        Model<Date> startDateModel = Model.of((Date) null);
        Model<Date> endDateModel = Model.of((Date) null);

        if (!parameters.get("starttime").isNull() && !parameters.get("starttime").isEmpty()) {
            startTime = parameters.get("starttime").toString();
            startDateModel = Model.of(Date.from(parse(startTime)));
        }

        if (!parameters.get("endtime").isNull() && !parameters.get("endtime").isEmpty()) {
            endTime = parameters.get("endtime").toString();
            endDateModel = Model.of(Date.from(parse(endTime)));
        }

        AjaxDatePicker startDatePicker = new AjaxDatePicker("startDate", startDateModel) {
            @Override
            public void onValueChanged(IPartialPageRequestHandler handler) {
                super.onValueChanged(handler);
                Date selectedDate = getModelObject();
                if (selectedDate == null) {
                    parameters.remove("starttime");
                } else {
                    parameters.set("starttime", selectedDate.toInstant().toString());
                }
                setResponsePage(ListPage.class, parameters);
                logger.info("Selected start date: {}", selectedDate);
            }
        };

        AjaxDatePicker endDatePicker = new AjaxDatePicker("endDate", endDateModel) {
            @Override
            public void onValueChanged(IPartialPageRequestHandler handler) {
                super.onValueChanged(handler);
                Date selectedDate = getModelObject();
                if (selectedDate == null) {
                    parameters.remove("endtime");
                } else {
                    parameters.set("endtime", selectedDate.toInstant().toString());
                }
                setResponsePage(ListPage.class, parameters);
                logger.info("Selected end date: {}", selectedDate);
            }
        };

        dateFilterContainer.add(startDatePicker);
        dateFilterContainer.add(endDatePicker);
        add(dateFilterContainer);

        add(new TitleBar("titlebar", this, null));
        add(new Label("pagetitle", "Nanopublication list | nanodash"));

        refresh();
    }

    /**
     * <p>hasAutoRefreshEnabled.</p>
     *
     * @return a boolean
     */
    protected boolean hasAutoRefreshEnabled() {
        return true;
    }

    private synchronized void refresh() {
        if (added) {
            remove("nanopubs");
        }
        added = true;
        final Map<String, String> params = new HashMap<>();
        if (!types.isEmpty()) {
            params.put("types", types.stream().map(IRI::stringValue).collect(Collectors.joining(" ")));
        }
        if (!pubKeys.isEmpty()) {
            params.put("pubkeys", String.join(" ", pubKeys));
        }
        if (!startTime.isBlank()) {
            params.put("starttime", startTime);
        }
        if (!endTime.isBlank()) {
            params.put("endtime", endTime);
        }
        final QueryRef queryRef = new QueryRef("get-filtered-nanopub-list", params);
        ApiResponse cachedResponse = ApiCache.retrieveResponse(queryRef);
        if (cachedResponse != null) {
            NanopubResults cachedResults = NanopubResults.fromApiResponse("nanopubs", cachedResponse);
            cachedResults.add(AttributeAppender.append("class", NanodashSession.get().getNanopubResultsViewMode().getValue()));
            add(cachedResults);
        } else {
            AjaxLazyLoadPanel<NanopubResults> results = new AjaxLazyLoadPanel<NanopubResults>("nanopubs") {

                private static final long serialVersionUID = 1L;

                @Override
                public NanopubResults getLazyLoadComponent(String markupId) {
                    ApiResponse r = null;
                    while (true) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                            logger.error("Interrupted while waiting for API response", ex);
                        }
                        if (!ApiCache.isRunning(queryRef)) {
                            r = ApiCache.retrieveResponse(queryRef);
                            if (r != null) break;
                        }
                    }
                    return NanopubResults.fromApiResponse(markupId, r);
                }

                @Override
                protected void onContentLoaded(NanopubResults content, Optional<AjaxRequestTarget> target) {
                    super.onContentLoaded(content, target);
                    if (target.isPresent()) {
                        target.get().appendJavaScript("updateElements();");
                    }
                }

            };
            results.add(AttributeAppender.append("class", NanodashSession.get().getNanopubResultsViewMode().getValue()));
            add(results);
        }
    }

}
