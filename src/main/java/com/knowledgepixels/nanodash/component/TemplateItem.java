package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.Template;
import net.trustyuri.TrustyUriUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.NanopubSignatureElement;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.services.ApiResponseEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A single template item in a list, showing the template name, user, and timestamp.
 */
public class TemplateItem extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(TemplateItem.class);

    /**
     * A single template item in a list, showing the template name, user, and timestamp.
     *
     * @param id    the wicket id of this component
     * @param entry the API response entry to display
     */
    public TemplateItem(String id, ApiResponseEntry entry) {
        this(id, entry, null);
    }

    public TemplateItem(String id, ApiResponseEntry entry, PageParameters additionalParams) {
        super(id);

        PageParameters params = new PageParameters();
        params.add("template", entry.get("np"));
        params.add("template-version", "latest");
        if (additionalParams != null) params.mergeWith(additionalParams);
        BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("link", PublishPage.class, params);
        String label = entry.get("label");
        if (label == null || label.isBlank()) label = TrustyUriUtils.getArtifactCode(entry.get("np")).substring(0, 10);
        l.add(new Label("name", label));
        add(l);
        IRI userIri = null;
        try {
            userIri = Utils.vf.createIRI(entry.get("creator"));
        } catch (IllegalArgumentException | NullPointerException ex) {
            logger.error("Error creating IRI from creator string: {}", entry.get("creator"), ex);
        }
        String userString = User.getShortDisplayNameForPubkeyhash(userIri, entry.get("pubkeyhash"));
        add(new Label("user", userString));
        add(new Label("timestamp", entry.get("date").substring(0, 10)));
    }


    public TemplateItem(String id, Template template) {
        this(id, template, null);
    }

    /**
     * A single template item in a list, showing the template name, user, and timestamp.
     *
     * @param id               the wicket id of this component
     * @param template         the template to display
     * @param additionalParams additional parameters to add to the link
     */
    public TemplateItem(String id, Template template, PageParameters additionalParams) {
        super(id);

        PageParameters params = new PageParameters();
        params.add("template", template.getId());
        params.add("template-version", "latest");
        if (additionalParams != null) params.mergeWith(additionalParams);
        BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("link", PublishPage.class, params);
        l.add(new Label("name", template.getLabel()));
        add(l);
        String userString = "somebody";
        try {
            NanopubSignatureElement se = SignatureUtils.getSignatureElement(template.getNanopub());
            if (se != null) {
                IRI signer = (se.getSigners().isEmpty() ? null : se.getSigners().iterator().next());
                String pubkeyHash = Utils.createSha256HexHash(se.getPublicKeyString());
                userString = User.getShortDisplayNameForPubkeyhash(signer, pubkeyHash);
            }
        } catch (Exception ex) {
            logger.error("Error getting signature element for template {}", template.getId(), ex);
        }
        add(new Label("user", userString));
        String timeString = "unknown date";
        Calendar c = SimpleTimestampPattern.getCreationTime(template.getNanopub());
        if (c != null) {
            timeString = (new SimpleDateFormat("yyyy-MM-dd")).format(c.getTime());
        }
        add(new Label("timestamp", timeString));
    }

}
