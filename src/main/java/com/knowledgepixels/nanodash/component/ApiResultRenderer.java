package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.nanopub.extra.services.ApiResponse;

import java.io.Serializable;

/**
 * Builds the component that shows a query's results. Passed to
 * {@link ApiResultComponent#create(String, org.nanopub.extra.services.QueryRef, ApiResponse, ApiResultRenderer)}
 * so the same rendering code serves all three states a view display can be in: results
 * that are current, outdated results shown while a refresh runs (see
 * {@link RefreshingResultPanel}), and nothing cached yet.
 * <p>
 * Implementations become part of the page tree and must be serializable — the builders
 * implement them as lambdas over their own (serializable) fields.
 */
public interface ApiResultRenderer extends Serializable {

    /**
     * @param markupId the Wicket markup id the created component must use
     * @param response the query results to render
     * @return the component showing those results
     */
    Component render(String markupId, ApiResponse response);

}
