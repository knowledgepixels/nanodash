package com.knowledgepixels.nanodash.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.knowledgepixels.nanodash.FilteredListDataProvider;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.page.NanodashPage;

public class ItemListPanel<T extends Serializable> extends Panel {

    private String description;
    private List<AbstractLink> buttons = new ArrayList<>();
    private List<AbstractLink> memberButtons = new ArrayList<>();
    private List<AbstractLink> adminButtons = new ArrayList<>();
    private AbstractResourceWithProfile resourceWithProfile;
    private boolean finalized = false;
    private boolean lazyLoading = false;
    private ReadyFunction readyFunction;

    private ItemListPanel(String markupId, String title) {
        super(markupId);
        setOutputMarkupId(true);

        add(new Label("title", title));
        WebMarkupContainer filterContainer = new WebMarkupContainer("filterContainer");
        filterContainer.add(new TextField<>("filter", Model.of("")).setVisible(false));
        filterContainer.setVisible(false);
        add(filterContainer);
    }

    private void addFilterAndItemList(String itemListId, List<T> items, ComponentProvider<T> compProvider, FilteredListDataProvider.SerializableFunction<T, String> filterTextGetter) {
        if (filterTextGetter != null) {
            IModel<String> filterModel = Model.of("");
            TextField<String> filterField = new TextField<>("filter", filterModel);
            filterField.setOutputMarkupId(true);
            filterField.add(new AjaxFormComponentUpdatingBehavior("change") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.add(ItemListPanel.this);
                }
            });
            WebMarkupContainer filterContainer = new WebMarkupContainer("filterContainer");
            filterContainer.add(filterField);
            get("filterContainer").replaceWith(filterContainer);
            add(new ItemList<T>(itemListId, items, compProvider, filterTextGetter, filterModel));
        } else {
            add(new ItemList<T>(itemListId, items, compProvider, null));
        }
    }

    public ItemListPanel(String markupId, String title, List<T> items, ComponentProvider<T> compProvider) {
        this(markupId, title, items, compProvider, null);
    }

    public ItemListPanel(String markupId, String title, List<T> items, ComponentProvider<T> compProvider, FilteredListDataProvider.SerializableFunction<T, String> filterTextGetter) {
        this(markupId, title);
        addFilterAndItemList("itemlist", items, compProvider, filterTextGetter);
    }

    public ItemListPanel(String markupId, String title, QueryRef queryRef, ApiResultListProvider<T> resultListProvider, ComponentProvider<T> compProvider) {
        this(markupId, title, queryRef, resultListProvider, compProvider, null);
    }

    public ItemListPanel(String markupId, String title, QueryRef queryRef, ApiResultListProvider<T> resultListProvider, ComponentProvider<T> compProvider, FilteredListDataProvider.SerializableFunction<T, String> filterTextGetter) {
        this(markupId, title);

        ApiResponse qResponse = ApiCache.retrieveResponseAsync(queryRef);
        if (qResponse != null) {
            addFilterAndItemList("itemlist", resultListProvider.apply(qResponse), compProvider, filterTextGetter);
        } else {
            lazyLoading = true;
            final FilteredListDataProvider.SerializableFunction<T, String> getter = filterTextGetter;
            add(new ApiResultComponent("itemlist", queryRef) {
                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    if (getter != null) {
                        IModel<String> filterModel = Model.of("");
                        ItemListPanel.this.get("filterContainer").replaceWith(createFilterContainer(filterModel, ItemListPanel.this));
                        return new ItemList<T>(markupId, resultListProvider.apply(response), compProvider, getter, filterModel);
                    }
                    return new ItemList<T>(markupId, resultListProvider.apply(response), compProvider, null);
                }
            });
        }
    }

    private WebMarkupContainer createFilterContainer(IModel<String> filterModel, ItemListPanel<?> panel) {
        WebMarkupContainer filterContainer = new WebMarkupContainer("filterContainer");
        TextField<String> filterField = new TextField<>("filter", filterModel);
        filterField.setOutputMarkupId(true);
        filterField.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(panel);
            }
        });
        filterContainer.add(filterField);
        return filterContainer;
    }

    public ItemListPanel(String markupId, String title, ReadyFunction readyFunction, ResultFunction<List<T>> resultFunction, ComponentProvider<T> compProvider) {
        this(markupId, title, readyFunction, resultFunction, compProvider, null);
    }

    public ItemListPanel(String markupId, String title, ReadyFunction readyFunction, ResultFunction<List<T>> resultFunction, ComponentProvider<T> compProvider, FilteredListDataProvider.SerializableFunction<T, String> filterTextGetter) {
        this(markupId, title);
        this.readyFunction = readyFunction;

        if (readyFunction.get()) {
            addFilterAndItemList("itemlist", resultFunction.get(), compProvider, filterTextGetter);
        } else {
            lazyLoading = true;
            final FilteredListDataProvider.SerializableFunction<T, String> getter = filterTextGetter;
            add(new MethodResultComponent<List<T>>("itemlist", readyFunction, resultFunction) {
                @Override
                public Component getResultComponent(String markupId, List<T> result) {
                    if (getter != null) {
                        IModel<String> filterModel = Model.of("");
                        ItemListPanel.this.get("filterContainer").replaceWith(createFilterContainer(filterModel, ItemListPanel.this));
                        return new ItemList<T>(markupId, resultFunction.get(), compProvider, getter, filterModel);
                    }
                    return new ItemList<T>(markupId, resultFunction.get(), compProvider, null);
                }
            });
        }
    }

    public ItemListPanel<T> setDescription(String description) {
        this.description = description;
        return this;
    }

    public ItemListPanel<T> makeInline() {
        add(new AttributeAppender("class", " inline"));
        return this;
    }

    // TODO Improve this (member/admin) button handling:
    public ItemListPanel<T> addButton(String label, Class<? extends NanodashPage> pageClass, PageParameters parameters) {
        if (parameters == null) parameters = new PageParameters();
        if (resourceWithProfile != null) parameters.set("context", resourceWithProfile.getId());
        AbstractLink button = new BookmarkablePageLink<NanodashPage>("button", pageClass, parameters);
        button.setBody(Model.of(label));
        buttons.add(button);
        return this;
    }

    public ItemListPanel<T> addMemberButton(String label, Class<? extends NanodashPage> pageClass, PageParameters parameters) {
        if (parameters == null) parameters = new PageParameters();
        if (resourceWithProfile != null) parameters.set("context", resourceWithProfile.getId());
        AbstractLink button = new BookmarkablePageLink<NanodashPage>("button", pageClass, parameters);
        button.setBody(Model.of(label));
        memberButtons.add(button);
        return this;
    }

    public ItemListPanel<T> addAdminButton(String label, Class<? extends NanodashPage> pageClass, PageParameters parameters) {
        if (parameters == null) parameters = new PageParameters();
        if (resourceWithProfile != null) parameters.set("context", resourceWithProfile.getId());
        AbstractLink button = new BookmarkablePageLink<NanodashPage>("button", pageClass, parameters);
        button.setBody(Model.of(label));
        adminButtons.add(button);
        return this;
    }

    public ItemListPanel<T> setProfiledResource(AbstractResourceWithProfile resourceWithProfile) {
        this.resourceWithProfile = resourceWithProfile;
        return this;
    }

    public ItemListPanel<T> setReadyFunction(ReadyFunction readyFunction) {
        this.readyFunction = readyFunction;
        return this;
    }

    @Override
    protected void onBeforeRender() {
        if (!finalized) {
            add(new Label("description", description).setVisible(description != null));
            if (resourceWithProfile != null && readyFunction != null && !readyFunction.get()) {
                add(new AjaxLazyLoadPanel<Component>("buttons") {

                    @Override
                    public Component getLazyLoadComponent(String markupId) {
                        return new ButtonList(markupId, resourceWithProfile, buttons, memberButtons, adminButtons);
                    }

                    @Override
                    protected boolean isContentReady() {
                        return readyFunction.get();
                    }

                    @Override
                    public Component getLoadingComponent(String id) {
                        if (lazyLoading) {
                            return new Label(id).setVisible(false);
                        } else {
                            return new Label(id, ResultComponent.getWaitComponentHtml(null)).setEscapeModelStrings(false);
                        }
                    }

                });
            } else {
                add(new ButtonList("buttons", resourceWithProfile, buttons, memberButtons, adminButtons));
            }
            finalized = true;
        }
        super.onBeforeRender();
    }


    public abstract class MethodResultComponent<R> extends ResultComponent {

        private final Supplier<Boolean> readyFunction;
        private final Supplier<R> resultFunction;

        public MethodResultComponent(String id, ReadyFunction readyFunction, ResultFunction<R> resultFunction) {
            super(id);
            setOutputMarkupId(true);
            this.readyFunction = readyFunction;
            this.resultFunction = resultFunction;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean isContentReady() {
            return readyFunction.get();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getLazyLoadComponent(String markupId) {
            R result = resultFunction.get();
            return getResultComponent(markupId, result);
        }

        public abstract Component getResultComponent(String markupId, R result);

    }

    public static interface ComponentProvider<T> extends Function<T, Component>, Serializable {
    }

    public static interface ApiResultListProvider<T> extends Function<ApiResponse, List<T>>, Serializable {
    }

    public static interface ReadyFunction extends Supplier<Boolean>, Serializable {
    }

    public static interface ResultFunction<X> extends Supplier<X>, Serializable {
    }

}
