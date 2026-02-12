package com.knowledgepixels.nanodash.page;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.QueryResultNanopubSetBuilder;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.kendo.ui.form.datetime.AjaxDatePicker;
import org.wicketstuff.kendo.ui.form.datetime.DatePicker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.knowledgepixels.nanodash.page.ListPage.PAGE_PARAMS.*;

/**
 * A page that shows a list of nanopublications filtered by type, public key, and time range.
 */
public class ListPage extends NanodashPage {

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

    enum PAGE_PARAMS {
        TYPE("type"),
        USER_ID("userid"),
        START_TIME("starttime"),
        END_TIME("endtime");

        private final String value;

        PAGE_PARAMS(String value) {
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

        WebMarkupContainer typeFilterContainer = new WebMarkupContainer("typeFilterContainer");
        WebMarkupContainer userFilterContainer = new WebMarkupContainer("userFilterContainer");
        WebMarkupContainer dateFilterContainer = new WebMarkupContainer("dateFilterContainer");

        List<StringValue> typeParams = parameters.getValues(TYPE.getValue());
        if (typeParams.size() > 1) {
            throw new IllegalArgumentException("Multiple 'type' parameters are not supported. Please provide a single 'type' parameter with space-separated values.");
        }
        if (!typeParams.isEmpty()) {
            StringValue typeParam = parameters.getValues(TYPE.getValue()).getFirst();
            types.add(Values.iri(typeParam.toString()));
            /*typeParams.forEach(typeParam -> {
                if (!typeParam.isNull() && !typeParam.isEmpty()) {
                    types.add(Values.iri(typeParam.toString()));
                }
            });*/
        }

        RepeatingView filteredTypes = new RepeatingView("typeNames");
        for (IRI type : types) {
            WebMarkupContainer typeContainer = new WebMarkupContainer(filteredTypes.newChildId());
            typeContainer.add(new Label("typeName", type.getLocalName()));
            typeContainer.add(new AjaxLink<Void>("removeType") {
                @Override
                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                    /*List<IRI> updatedTypes = types.stream()
                            .filter(t -> !t.equals(type))
                            .toList();
                    if (!updatedTypes.isEmpty()) {
                        parameters.set(TYPE.getValue(), updatedTypes.stream()
                                .map(IRI::stringValue)
                                .collect(Collectors.joining(" ")));
                    } else {*/
                    parameters.remove(TYPE.getValue());
                    //}
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
        final Multimap<String, String> queryParams = ArrayListMultimap.create();
        if (!types.isEmpty()) {
            types.forEach(type -> queryParams.put("types", type.stringValue()));
        }
        if (userId != null) {
            List<String> pubkeys = User.getPubkeyhashes(userId, null);
            if (!pubkeys.isEmpty()) {
                pubkeys.forEach(pubKey -> queryParams.put("np_pubkeys", pubKey));
            }
        }
        if (startDate != null) {
            queryParams.put("np_starttime", startDate.toInstant().toString());
        }
        if (endDate != null) {
            queryParams.put("np_endtime", endDate.toInstant().toString());
        }
        View filteredNanopubsView = View.get("https://w3id.org/np/RAAxsnXxYLev1_STgHnb2Y-oNRE3DRERXXDoJbELHSnzA/filtered-nanopubs-view");
        final QueryRef queryRef = new QueryRef(filteredNanopubsView.getQuery().getQueryId(), queryParams);
        add(new DataView<>("filteredNanopubs", new ListDataProvider<>(List.of(new ViewDisplay(filteredNanopubsView)))) {

            @Override
            protected void populateItem(Item<ViewDisplay> item) {
                item.add(QueryResultNanopubSetBuilder.create("view", queryRef, item.getModelObject())
                        .noTitle()
                        .build()
                );
            }
        });
    }

}
