package com.knowledgepixels.nanodash.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.page.NanodashPage;

public class ItemListPanel<T extends Serializable> extends Panel {

    private String description;
    private List<AbstractLink> buttons = new ArrayList<>();
    private List<AbstractLink> memberButtons = new ArrayList<>();
    private List<AbstractLink> adminButtons = new ArrayList<>();
    private Space space;
    private boolean finalized = false;
    private boolean lazyLoading = false;
    private ReadyFunction readyFunction;

    private ItemListPanel(String markupId, String title) {
        super(markupId);
        setOutputMarkupId(true);

        add(new Label("title", title));
    }
    
    public ItemListPanel(String markupId, String title, List<T> items, ComponentProvider<T> compProvider) {
        this(markupId, title);

        add(new ItemList<T>("itemlist", items, compProvider));
    }

    public ItemListPanel(String markupId, String title, QueryRef queryRef, ApiResultListProvider<T> resultListProvider, ComponentProvider<T> compProvider) {
        this(markupId, title);

        ApiResponse qResponse = ApiCache.retrieveResponse(queryRef);
        if (qResponse != null) {
            add(new ItemList<T>("itemlist", resultListProvider.apply(qResponse), compProvider));
        } else {
            lazyLoading = true;
            add(new ApiResultComponent("itemlist", queryRef) {
                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return new ItemList<T>(markupId, resultListProvider.apply(response), compProvider);
                }
            });
        }
    }

    public ItemListPanel(String markupId, String title, ReadyFunction readyFunction, ResultFunction<List<T>> resultFunction, ComponentProvider<T> compProvider) {
        this(markupId, title);
        this.readyFunction = readyFunction;

        if (readyFunction.get()) {
            add(new ItemList<T>("itemlist", resultFunction.get(), compProvider));
        } else {
            lazyLoading = true;
            add(new MethodResultComponent<List<T>>("itemlist", readyFunction, resultFunction) {
                @Override
                public Component getResultComponent(String markupId, List<T> result) {
                    return new ItemList<T>(markupId, resultFunction.get(), compProvider);
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
        if (space != null) parameters.set("context", space.getId());
        AbstractLink button = new BookmarkablePageLink<NanodashPage>("button", pageClass, parameters);
        button.setBody(Model.of(label));
        buttons.add(button);
        return this;
    }

    public ItemListPanel<T> addMemberButton(String label, Class<? extends NanodashPage> pageClass, PageParameters parameters) {
        if (parameters == null) parameters = new PageParameters();
        if (space != null) parameters.set("context", space.getId());
        AbstractLink button = new BookmarkablePageLink<NanodashPage>("button", pageClass, parameters);
        button.setBody(Model.of(label));
        memberButtons.add(button);
        return this;
    }

    public ItemListPanel<T> addAdminButton(String label, Class<? extends NanodashPage> pageClass, PageParameters parameters) {
        if (parameters == null) parameters = new PageParameters();
        if (space != null) parameters.set("context", space.getId());
        AbstractLink button = new BookmarkablePageLink<NanodashPage>("button", pageClass, parameters);
        button.setBody(Model.of(label));
        adminButtons.add(button);
        return this;
    }

    public ItemListPanel<T> setSpace(Space space) {
        this.space = space;
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
            if (space != null && readyFunction != null && !readyFunction.get()) {
                add(new AjaxLazyLoadPanel<Component>("buttons") {

                    @Override
                    public Component getLazyLoadComponent(String markupId) {
                        return new ButtonList(markupId, space, buttons, memberButtons, adminButtons);
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
                add(new ButtonList("buttons", space, buttons, memberButtons, adminButtons));
            }
            finalized = true;
        }
        super.onBeforeRender();
    }


    public abstract class MethodResultComponent<R> extends ResultComponent {

        private final transient Supplier<Boolean> readyFunction;
        private final transient Supplier<R> resultFunction;

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
