package com.knowledgepixels.nanodash.component;

import java.util.function.Function;

import org.apache.wicket.Component;

public abstract class MethodResultComponent<T,R> extends ResultComponent {

    private final T obj;
    private final transient Function<T,Boolean> readyFunction;
    private final transient Function<T,R> resultFunction;

    public MethodResultComponent(String id, T obj, Function<T,Boolean> readyFunction, Function<T,R> resultFunction) {
        super(id);
        this.obj = obj;
        this.readyFunction = readyFunction;
        this.resultFunction = resultFunction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isContentReady() {
        return readyFunction.apply(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getLazyLoadComponent(String markupId) {
        R result = resultFunction.apply(obj);
        return getResultComponent(markupId, result);
    }

    // TODO Use lambda instead of abstract method?
    public abstract Component getResultComponent(String markupId, R result);

}
