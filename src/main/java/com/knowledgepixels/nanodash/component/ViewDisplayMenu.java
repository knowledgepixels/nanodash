package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.page.QueryPage;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.QueryRef;

/**
 * A dropdown menu panel for view displays, replacing the "^" source link.
 * Provides options to show the query, update the view display, and see its declaration.
 */
public class ViewDisplayMenu extends Panel {

    /**
     * Constructs a ViewDisplayMenu.
     *
     * @param id          the Wicket component ID
     * @param viewDisplay the view display this menu acts on (must have a non-null nanopub)
     * @param queryRef    the query reference used by this view display
     */
    public ViewDisplayMenu(String id, ViewDisplay viewDisplay, QueryRef queryRef) {
        super(id);

        add(new BookmarkablePageLink<Void>("showQuery", QueryPage.class,
                new PageParameters().set("id", queryRef.getQueryId())));

        IRI nanopubId = viewDisplay.getNanopubId();
        IRI templateId = TemplateData.get().getTemplateId(viewDisplay.getNanopub());
        String templateUri = templateId != null ? templateId.stringValue()
                : "http://purl.org/np/RACyK2NjqFgezYLiE8FQu7JI0xY1M1aNQbykeCW8oqXkA";
        String updateUrl = PublishPage.MOUNT_PATH + "?template=" + Utils.urlEncode(templateUri)
                + "&supersede=" + Utils.urlEncode(nanopubId.stringValue())
                + "&template-version=latest";
        add(new ExternalLink("update", updateUrl, "update"));

        add(new BookmarkablePageLink<Void>("viewDeclaration", ExplorePage.class,
                new PageParameters().set("id", nanopubId)));
    }

}
