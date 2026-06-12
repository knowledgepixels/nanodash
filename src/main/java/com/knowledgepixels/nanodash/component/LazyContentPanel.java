package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.View;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;

import java.io.Serializable;

/**
 * Defers the construction of a content panel to a follow-up AJAX request, so
 * the page shell renders immediately with a loading spinner. Use this to wrap
 * panels whose constructors are expensive — e.g. the About/Explore tab bodies,
 * which resolve view nanopubs over the network (several sequential round-trips
 * in {@link com.knowledgepixels.nanodash.View#get(String)}) before they can
 * even be instantiated.
 */
public class LazyContentPanel extends AjaxLazyLoadPanel<Component> {

    /**
     * Factory building the actual content component; called in the AJAX
     * request that replaces the spinner. Captured state must be serializable
     * (it becomes part of the page tree) — prefer ids/models over domain
     * singletons.
     */
    public interface ContentFactory extends Serializable {

        /**
         * @param markupId the Wicket markup id the created component must use
         * @return the content component
         */
        Component create(String markupId);
    }

    private final ContentFactory factory;

    /**
     * Builds the content directly when all the views its constructor resolves
     * are freshly cached (so construction won't block on the network), and
     * defers to a lazy-loading spinner otherwise. Wicket's lazy panel always
     * renders the spinner first and swaps in content on a ~1s AJAX timer tick,
     * so going lazy unconditionally would make even cache-hit renders flicker.
     *
     * @param id              the Wicket markup id
     * @param factory         builds the content component
     * @param requiredViewIds the view ids the factory's component resolves via
     *                        {@link View#get(String)}; must mirror its
     *                        constructor's calls, else a cold view blocks a
     *                        supposedly-ready synchronous render
     * @return the content component itself, or a {@link LazyContentPanel} around it
     */
    public static Component of(String id, ContentFactory factory, String... requiredViewIds) {
        for (String viewId : requiredViewIds) {
            if (!View.isCached(viewId)) {
                return new LazyContentPanel(id, factory);
            }
        }
        return factory.create(id);
    }

    /**
     * @param id      the Wicket markup id
     * @param factory builds the content component once the AJAX request comes in
     */
    public LazyContentPanel(String id, ContentFactory factory) {
        super(id);
        this.factory = factory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getLazyLoadComponent(String markupId) {
        return factory.create(markupId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getLoadingComponent(String id) {
        return new Label(id, "<div class=\"row-section\"><div class=\"col-12\">" + ResultComponent.getWaitIconHtml() + "</div></div>").setEscapeModelStrings(false);
    }

}
