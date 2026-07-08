package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.apache.wicket.Component;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.QueryRef;

/**
 * Maps a view's display type to the matching query-result component. The single
 * place where the display-type IRIs are dispatched to their builders, shared by
 * {@link ViewList} (views on resource pages) and
 * {@link com.knowledgepixels.nanodash.page.ViewResultsPage} (the standalone
 * results page a query-form view leads to).
 */
public class QueryResultComponentFactory {

    private QueryResultComponentFactory() {
    }

    /**
     * Builds the result component for the given view display's display type.
     *
     * @param markupId            the markup ID for the component
     * @param queryRef            the query reference to show the results of
     * @param viewDisplay         the view display (determines display type, title, width, page size)
     * @param resourceWithProfile the resource whose page the component is on, or null on a
     *                            standalone page (view actions needing a resource context are
     *                            then omitted by the builders)
     * @param id                  the resource or part id the view is shown for, or null
     * @param contextId           the context id, or null
     * @param refRoot             the pinned ref's root nanopub, or null
     * @return the component, or null if the view has no or an unrecognized display type
     */
    public static Component build(String markupId, QueryRef queryRef, ViewDisplay viewDisplay,
            AbstractResourceWithProfile resourceWithProfile, String id, String contextId, String refRoot) {
        View view = viewDisplay.getView();
        IRI viewType = (view == null ? null : view.getViewType());
        if (viewType == null) return null;
        if (viewType.equals(KPXL_TERMS.LIST_VIEW)) {
            return QueryResultListBuilder.create(markupId, queryRef, viewDisplay)
                    .resourceWithProfile(resourceWithProfile)
                    .pageResource(resourceWithProfile)
                    .id(id)
                    .contextId(contextId)
                    .refRoot(refRoot)
                    .build();
        } else if (viewType.equals(KPXL_TERMS.TABULAR_VIEW)) {
            return QueryResultTableBuilder.create(markupId, queryRef, viewDisplay)
                    .resourceWithProfile(resourceWithProfile)
                    .contextId(contextId)
                    .id(id)
                    .refRoot(refRoot)
                    .build();
        } else if (viewType.equals(KPXL_TERMS.PLAIN_PARAGRAPH_VIEW)) {
            return QueryResultPlainParagraphBuilder.create(markupId, queryRef, viewDisplay)
                    .pageResource(resourceWithProfile)
                    .contextId(contextId)
                    .id(id)
                    .refRoot(refRoot)
                    .build();
        } else if (viewType.equals(KPXL_TERMS.NANOPUB_SET_VIEW)) {
            return QueryResultNanopubSetBuilder.create(markupId, queryRef, viewDisplay)
                    .resourceWithProfile(resourceWithProfile)
                    .pageResource(resourceWithProfile)
                    .id(id)
                    .contextId(contextId)
                    .refRoot(refRoot)
                    .build();
        } else if (viewType.equals(KPXL_TERMS.ITEM_LIST_VIEW)) {
            return QueryResultItemListBuilder.create(markupId, queryRef, viewDisplay)
                    .resourceWithProfile(resourceWithProfile)
                    .pageResource(resourceWithProfile)
                    .id(id)
                    .contextId(contextId)
                    .refRoot(refRoot)
                    .build();
        }
        return null;
    }

}
