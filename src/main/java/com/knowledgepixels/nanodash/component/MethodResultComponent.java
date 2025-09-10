package com.knowledgepixels.nanodash.component;

import java.util.function.Supplier;

import org.apache.wicket.Component;

public abstract class MethodResultComponent<R> extends ResultComponent {

    private final transient Supplier<Boolean> readyFunction;
    private final transient Supplier<R> resultFunction;

    public MethodResultComponent(String id, Supplier<Boolean> readyFunction, Supplier<R> resultFunction) {
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

    // TODO Use lambda instead of abstract method?
    public abstract Component getResultComponent(String markupId, R result);

}
