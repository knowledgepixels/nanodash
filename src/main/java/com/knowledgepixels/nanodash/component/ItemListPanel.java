package com.knowledgepixels.nanodash.component;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.QueryRef;

public class ItemListPanel<T extends Serializable> extends Panel {

    public ItemListPanel(String markupId, String title, List<T> items, ComponentProvider<T> compProvider) {
        super(markupId);
        setOutputMarkupId(true);

        if (markupId.endsWith("-users")) {
            add(new AttributeAppender("class", " users"));
        }

        if (title.contains("  ")) {
            add(new Label("description", title.replaceFirst("^.*  ", "")));
            title = title.replaceFirst("  .*$", "");
        } else {
            add(new Label("description").setVisible(false));
        }
        add(new Label("title", title));
        add(new Label("button").setVisible(false));

        add(new ItemList<T>("itemlist", items, compProvider));
    }

    public ItemListPanel(String markupId, String title, QueryRef queryRef, ApiResultListProvider<T> resultListProvider, ComponentProvider<T> compProvider) {
        super(markupId);
        setOutputMarkupId(true);

        if (markupId.endsWith("-users")) {
            add(new AttributeAppender("class", " users"));
        }

        if (title.contains("  ")) {
            add(new Label("description", title.replaceFirst("^.*  ", "")));
            title = title.replaceFirst("  .*$", "");
        } else {
            add(new Label("description").setVisible(false));
        }
        add(new Label("title", title));
        add(new Label("button").setVisible(false));

        ApiResponse qResponse = ApiCache.retrieveResponse(queryRef);
        if (qResponse != null) {
            add(new ItemList<T>("itemlist", resultListProvider.apply(qResponse), compProvider));
        } else {
            add(new ApiResultComponent("itemlist", queryRef) {

                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return new ItemList<T>(markupId, resultListProvider.apply(response), compProvider);
                }
            });

        }
    }

    public ItemListPanel(String markupId, String title, ReadyFunction readyFunction, ResultFunction<List<T>> resultFunction, ComponentProvider<T> compProvider) {
        super(markupId);
        setOutputMarkupId(true);

        if (markupId.endsWith("-users")) {
            add(new AttributeAppender("class", " users"));
        }

        if (title.contains("  ")) {
            add(new Label("description", title.replaceFirst("^.*  ", "")));
            title = title.replaceFirst("  .*$", "");
        } else {
            add(new Label("description").setVisible(false));
        }
        add(new Label("title", title));
        add(new Label("button").setVisible(false));

        if (readyFunction.get()) {
            add(new ItemList<T>("itemlist", resultFunction.get(), compProvider));
        } else {
            add(new MethodResultComponent<List<T>>("itemlist", readyFunction, resultFunction) {
                @Override
                public Component getResultComponent(String markupId, List<T> result) {
                    return new ItemList<T>(markupId, resultFunction.get(), compProvider);
                }
            });
        }
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
