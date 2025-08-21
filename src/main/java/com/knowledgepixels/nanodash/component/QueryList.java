package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * A component that displays a list of queries.
 * Each query is linked to its corresponding QueryPage.
 */
public class QueryList extends Panel {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for QueryList.
     *
     * @param id   the component ID
     * @param resp the API response containing query data
     */
    public QueryList(String id, ApiResponse resp) {
        super(id);
        List<GrlcQuery> queries = new ArrayList<>();
        for (ApiResponseEntry e : resp.getData()) {
            try {
                queries.add(GrlcQuery.get(e.get("np")));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        add(new DataView<GrlcQuery>("querylist", new ListDataProvider<GrlcQuery>(queries)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<GrlcQuery> item) {
                GrlcQuery q = item.getModelObject();
                PageParameters params = new PageParameters();
                params.add("id", q.getQueryId());
                BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("querylink", QueryPage.class, params);
                l.add(new Label("linktext", q.getLabel()));
                item.add(l);

                String userString = "somebody";
                try {
                    NanopubSignatureElement se = SignatureUtils.getSignatureElement(q.getNanopub());
                    if (se != null) {
                        IRI signer = (se.getSigners().isEmpty() ? null : se.getSigners().iterator().next());
                        String pubkeyhash = Utils.createSha256HexHash(se.getPublicKeyString());
                        userString = User.getShortDisplayNameForPubkeyhash(signer, pubkeyhash);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                item.add(new Label("user", userString));
                String timeString = "unknown date";
                Calendar c = SimpleTimestampPattern.getCreationTime(q.getNanopub());
                if (c != null) {
                    timeString = (new SimpleDateFormat("yyyy-MM-dd")).format(c.getTime());
                }
                item.add(new Label("timestamp", timeString));
            }

        });
    }

}
