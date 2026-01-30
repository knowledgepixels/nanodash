package com.knowledgepixels.nanodash.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.page.ExplorePage;

/**
 * Panel that shows a "just published" message with a link to the explore page when the
 * page has a "just-published" URL parameter. Styled like link-message, with a close button.
 */
public class JustPublishedMessagePanel extends Panel {

    public JustPublishedMessagePanel(String id, PageParameters parameters) {
        super(id);
        setOutputMarkupId(true);
        String justPublishedId = parameters.get("just-published").toString("");
        setVisible(!justPublishedId.isEmpty());
        if (!justPublishedId.isEmpty()) {
            add(new AjaxLink<Void>("close") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    JustPublishedMessagePanel.this.setVisible(false);
                    target.add(JustPublishedMessagePanel.this);
                }
            }.setBody(Model.of("×")));
            Nanopub np = Utils.getAsNanopub(justPublishedId);
            String label = (np != null ? NanopubUtils.getLabel(np) : null);
            if (label == null || label.isEmpty()) label = Utils.getShortNameFromURI(justPublishedId);
            add(new BookmarkablePageLink<Void>("link", ExplorePage.class, new PageParameters().set("id", justPublishedId)).setBody(Model.of(label)));
        } else {
            add(new WebMarkupContainer("close").setVisible(false));
            add(new WebMarkupContainer("link").setVisible(false));
        }
    }
}
