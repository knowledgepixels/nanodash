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
import org.apache.wicket.util.string.StringValue;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.nanopub.extra.services.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.knowledgepixels.nanodash.page.ListPage.PARAMS.*;

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
    private Date startDate = null;
    private Date endDate = null;

    enum PARAMS {
        TYPE("type"),
        PUBKEY("pubkey"),
        START_TIME("starttime"),
        END_TIME("endtime");

        private final String value;

        PARAMS(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

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

        List<StringValue> typeParams = parameters.getValues(TYPE.getValue());
        if (typeParams != null && !typeParams.isEmpty()) {
            typeParams.forEach(typeParam -> {
                ;
                if (!typeParam.isNull() && !typeParam.isEmpty()) {
                    types.add(Values.iri(typeParam.toString()));
                }
            });
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
                        parameters.set(TYPE.getValue(), updatedTypes.stream()
                                .map(IRI::stringValue)
                                .collect(Collectors.joining(" ")));
                    } else {
                        parameters.remove(TYPE.getValue());
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


        List<StringValue> pubKeysParams = parameters.getValues(PUBKEY.getValue());
        if (pubKeysParams != null && !pubKeysParams.isEmpty()) {
            pubKeysParams.forEach(pubKeyParam -> {
                if (!pubKeyParam.isNull() && !pubKeyParam.isEmpty()) {
                    pubKeys.add(pubKeyParam.toString());
                }
            });
        }

        Model<Date> startDateModel = Model.of((Date) null);
        Model<Date> endDateModel = Model.of((Date) null);

        if (!parameters.get(START_TIME.getValue()).isNull() && !parameters.get(START_TIME.getValue()).isEmpty()) {
            startDate = Date.from(parameters.get(START_TIME.getValue()).toInstant());
            startDateModel = Model.of(startDate);
        }

        if (!parameters.get(END_TIME.getValue()).isNull() && !parameters.get(END_TIME.getValue()).isEmpty()) {
            endDate = Date.from(parameters.get(END_TIME.getValue()).toInstant());
            endDateModel = Model.of(endDate);
        }

        AjaxDatePicker startDatePicker = new AjaxDatePicker("startDate", startDateModel) {
            @Override
            public void onValueChanged(IPartialPageRequestHandler handler) {
                super.onValueChanged(handler);
                Date selectedDate = getModelObject();
                if (selectedDate.after(new Date())) {
                    handler.appendJavaScript("alert('Start date cannot be in the future.');");
                    selectedDate = null;
                } else if (endDate != null && selectedDate.after(endDate)) {
                    handler.appendJavaScript("alert('Start date cannot be after end date.');");
                    selectedDate = null;
                }
                if (selectedDate != null) {
                    parameters.set(START_TIME.getValue(), selectedDate.toInstant().toString());
                    logger.info("Selected start date: {}", selectedDate.toInstant().toString());
                    setResponsePage(ListPage.class, parameters);
                } else {
                    this.setModelObject(null);
                    handler.add(this);
                }
            }
        };

        WebMarkupContainer clearStartDate = new AjaxLink<Void>("clearStartDate") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                startDatePicker.setModelObject(null);
                parameters.remove(START_TIME.getValue());
                setResponsePage(ListPage.class, parameters);
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(startDatePicker.getModelObject() != null);
            }
        };
        clearStartDate.add(new Label("crossIcon", "×"));
        dateFilterContainer.add(clearStartDate);

        AjaxDatePicker endDatePicker = new AjaxDatePicker("endDate", endDateModel) {
            @Override
            public void onValueChanged(IPartialPageRequestHandler handler) {
                super.onValueChanged(handler);
                Date selectedDate = getModelObject();
                if (startDate != null && selectedDate.before(startDate)) {
                    handler.appendJavaScript("alert('End date cannot be before start date.');");
                    selectedDate = null;
                }
                if (selectedDate != null) {
                    parameters.set(END_TIME.getValue(), selectedDate.toInstant().toString());
                    logger.info("Selected end date: {}", selectedDate);
                    setResponsePage(ListPage.class, parameters);
                } else {
                    this.setModelObject(null);
                    handler.add(this);
                }
            }
        };

        WebMarkupContainer clearEndDate = new AjaxLink<Void>("clearEndDate") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                endDatePicker.setModelObject(null);
                parameters.remove(END_TIME.getValue());
                setResponsePage(ListPage.class, parameters);
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(endDatePicker.getModelObject() != null);
            }
        };
        clearEndDate.add(new Label("crossIcon", "×"));
        dateFilterContainer.add(clearEndDate);

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
            params.put(TYPE.getValue(), types.stream().map(IRI::stringValue).collect(Collectors.joining(" ")));
        }
        if (!pubKeys.isEmpty()) {
            params.put(PUBKEY.getValue(), String.join(" ", pubKeys));
        }
        if (startDate != null) {
            params.put(START_TIME.getValue(), startDate.toInstant().toString());
        }
        if (endDate != null) {
            params.put(END_TIME.getValue(), endDate.toInstant().toString());
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
