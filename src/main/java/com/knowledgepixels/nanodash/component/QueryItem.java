package com.knowledgepixels.nanodash.component;

import java.text.SimpleDateFormat;
import java.util.Calendar;

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

import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.page.QueryPage;

public class QueryItem extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(QueryItem.class);

    public QueryItem(String id, GrlcQuery query) {
        super(id);

        PageParameters params = new PageParameters();
        params.add("id", query.getQueryId());
        BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("querylink", QueryPage.class, params);
        l.add(new Label("linktext", query.getLabel()));
        add(l);

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
        add(new Label("user", userString));
        String timeString = "unknown date";
        Calendar c = SimpleTimestampPattern.getCreationTime(query.getNanopub());
        if (c != null) {
            timeString = (new SimpleDateFormat("yyyy-MM-dd")).format(c.getTime());
        }
        add(new Label("timestamp", timeString));
    }

}
