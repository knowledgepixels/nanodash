package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.Utils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.List;

/**
 * A panel that displays a list of nanopubs.
 */
public class NanopubResults extends Panel {

    private static final long serialVersionUID = -5109507637942030910L;

    /**
     * Creates a NanopubResults panel from a list of NanopubElements.
     *
     * @param id          the component id
     * @param nanopubList the list of NanopubElements to display
     * @return a new NanopubResults panel
     */
    public static NanopubResults fromList(String id, List<NanopubElement> nanopubList) {
        NanopubResults r = new NanopubResults(id);
        r.add(new DataView<NanopubElement>("nanopubs", new ListDataProvider<NanopubElement>(nanopubList)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<NanopubElement> item) {
                item.add(new NanopubItem("nanopub", item.getModelObject()).setMinimal());
            }

        });
        return r;
    }

    /**
     * Creates a NanopubResults panel from an ApiResponse.
     *
     * @param id          the component id
     * @param apiResponse the ApiResponse containing nanopub data
     * @return a new NanopubResults panel
     */
    public static NanopubResults fromApiResponse(String id, ApiResponse apiResponse) {
        return fromApiResponse(id, apiResponse, -1);
    }

    /**
     * Creates a NanopubResults panel from an ApiResponse with a limit on the number of nanopubs.
     *
     * @param id          the component id
     * @param apiResponse the ApiResponse containing nanopub data
     * @param limit       the maximum number of nanopubs to display, or -1 for no limit
     * @return a new NanopubResults panel
     */
    public static NanopubResults fromApiResponse(String id, ApiResponse apiResponse, int limit) {
        List<ApiResponseEntry> list = apiResponse.getData();
        if (limit >= 0 && list.size() > limit) {
            list = Utils.subList(list, 0, limit);
        }
        NanopubResults r = new NanopubResults(id);
        r.add(new DataView<ApiResponseEntry>("nanopubs", new ListDataProvider<ApiResponseEntry>(list)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<ApiResponseEntry> item) {
                item.add(new NanopubItem("nanopub", NanopubElement.get(item.getModelObject().get("np"))).setMinimal());
            }

        });
        return r;
    }

    private NanopubResults(String id) {
        super(id);
    }

}
