package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.page.QueryPage;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A panel representing a single query item in a list.
 * Displays the query label as a link, the user who created it, and the creation timestamp.
 */
public class QueryItem extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(QueryItem.class);

    /**
     * Constructor.
     *
     * @param id    the Wicket id for this panel
     * @param query the GrlcQuery object containing query details
     */
    public QueryItem(String id, GrlcQuery query, PageParameters additionalParams, boolean extended) {
        super(id);

        PageParameters params = new PageParameters();
        params.add("id", query.getQueryId());
        if (additionalParams != null) params.mergeWith(additionalParams);
        BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("querylink", QueryPage.class, params);
        l.add(new Label("linktext", query.getLabel()));
        add(l);

        WebMarkupContainer statusPart = new WebMarkupContainer("status");
        if (extended) {
            String userString = "somebody";
            try {
                NanopubSignatureElement se = SignatureUtils.getSignatureElement(query.getNanopub());
                if (se != null) {
                    IRI signer = (se.getSigners().isEmpty() ? null : se.getSigners().iterator().next());
                    String pubkeyhash = Utils.createSha256HexHash(se.getPublicKeyString());
                    userString = User.getShortDisplayNameForPubkeyhash(signer, pubkeyhash);
                }
            } catch (Exception ex) {
                logger.error("Error retrieving signature info: {}", ex.getMessage());
            }
            statusPart.add(new Label("user", userString));
            String timeString = "unknown date";
            Calendar c = SimpleTimestampPattern.getCreationTime(query.getNanopub());
            if (c != null) {
                timeString = (new SimpleDateFormat("yyyy-MM-dd")).format(c.getTime());
            }
            statusPart.add(new Label("timestamp", timeString));
        } else {
            statusPart.setVisible(false);
        }
        add(statusPart);
    }

}
