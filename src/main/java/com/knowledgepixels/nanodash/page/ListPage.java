package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.QueryRef;
import com.knowledgepixels.nanodash.User;
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
import org.wicketstuff.kendo.ui.form.datetime.AjaxDatePicker;
import org.wicketstuff.kendo.ui.form.datetime.DatePicker;

import java.util.*;
import java.util.stream.Collectors;

import static com.knowledgepixels.nanodash.page.ListPage.PARAMS.*;

/**
 * A page that shows a list of nanopublications filtered by type, public key, and time range.
 */
public class ListPage extends NanodashPage {

    private static final long serialVersionUID = 1L;
    private final String QUERY_NAME = "get-filtered-nanopub-list";
    private final String DATE_FORMAT = "d MMM yyyy";

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
    private IRI userId;
    private Date startDate = null;
    private Date endDate = null;

    enum PARAMS {
        TYPE("type"),
        USER_ID("userid"),
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

        add(new TitleBar("titlebar", this, null));
        add(new Label("pagetitle", "Nanopublication list | nanodash"));

        add(new AjaxLink<>("listEnabler") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                NanodashSession.get().setNanopubResultsViewMode(NanopubResults.ViewMode.LIST);
                logger.info("ListEnabler -- Switched to '{}' mode", NanodashSession.get().getNanopubResultsViewMode().getValue());
            }
        });

        add(new AjaxLink<>("gridEnabler") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                NanodashSession.get().setNanopubResultsViewMode(NanopubResults.ViewMode.GRID);
                logger.info("GridEnabler -- Switched to '{}' mode", NanodashSession.get().getNanopubResultsViewMode().getValue());
            }
        });

        WebMarkupContainer typeFilterContainer = new WebMarkupContainer("typeFilterContainer");
        WebMarkupContainer userFilterContainer = new WebMarkupContainer("userFilterContainer");
        WebMarkupContainer dateFilterContainer = new WebMarkupContainer("dateFilterContainer");

        List<StringValue> typeParams = parameters.getValues(TYPE.getValue());
        if (typeParams != null && !typeParams.isEmpty()) {
            typeParams.forEach(typeParam -> {
                if (!typeParam.isNull() && !typeParam.isEmpty()) {
                    types.add(Values.iri(typeParam.toString()));
                }
            });
        }

        RepeatingView filteredTypes = new RepeatingView("typeNames");
        for (IRI type : types) {
            WebMarkupContainer typeContainer = new WebMarkupContainer(filteredTypes.newChildId());
            typeContainer.add(new Label("typeName", type.getLocalName()));
            typeContainer.add(new AjaxLink<Void>("removeType") {
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

        DatePicker startDatePicker = new AjaxDatePicker("startDate", startDateModel, DATE_FORMAT) {
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

        DatePicker endDatePicker = new AjaxDatePicker("endDate", endDateModel, DATE_FORMAT) {
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

        if (!parameters.get(USER_ID.getValue()).isNull() && !parameters.get(USER_ID.getValue()).isEmpty()) {
            userId = Values.iri(parameters.get(USER_ID.getValue()).toString());
            userFilterContainer.add(new Label("userId", User.getName(userId)));
            userFilterContainer.add(new AjaxLink<Void>("removeUser") {
                @Override
                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                    parameters.remove(USER_ID.getValue());
                    userId = null;
                    setResponsePage(ListPage.class, parameters);
                }
            });
        }

        typeFilterContainer.setVisible(!types.isEmpty());
        typeFilterContainer.add(filteredTypes);
        userFilterContainer.setVisible(userId != null && !userId.stringValue().isEmpty());
        dateFilterContainer.add(clearStartDate, clearEndDate, startDatePicker, endDatePicker);

        add(typeFilterContainer, userFilterContainer, dateFilterContainer);

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
        final Map<String, String> queryParams = new HashMap<>();
        if (!types.isEmpty()) {
            queryParams.put("types", types.stream().map(IRI::stringValue).collect(Collectors.joining(" ")));
        }
        if (userId != null) {
            List<String> pubkeys = User.getPubkeyhashes(userId, null);
            if (!pubkeys.isEmpty()) {
                queryParams.put("pubkeys", String.join(" ", pubkeys));
            }
        }
        if (startDate != null) {
            queryParams.put(START_TIME.getValue(), startDate.toInstant().toString());
        }
        if (endDate != null) {
            queryParams.put(END_TIME.getValue(), endDate.toInstant().toString());
        }
        final QueryRef queryRef = new QueryRef(QUERY_NAME, queryParams);
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
                    ApiResponse response = null;
                    while (true) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                            logger.error("Interrupted while waiting for API response", ex);
                        }
                        if (!ApiCache.isRunning(queryRef)) {
                            response = ApiCache.retrieveResponse(queryRef);
                            if (response != null) break;
                        }
                    }
                    return NanopubResults.fromApiResponse(markupId, response);
                }

                @Override
                protected void onContentLoaded(NanopubResults content, Optional<AjaxRequestTarget> target) {
                    super.onContentLoaded(content, target);
                    target.ifPresent(ajaxRequestTarget -> ajaxRequestTarget.appendJavaScript("updateElements();"));
                }

            };
            results.add(AttributeAppender.append("class", NanodashSession.get().getNanopubResultsViewMode().getValue()));
            add(results);
        }
    }

}
