package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.knowledgepixels.nanodash.page.UserPage;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * The link from a resource part page to the part's own page: a person's profile, a
 * sub-space's space page, a maintained resource's resource page. A part page is the
 * resource seen inside a context, so without this there is no way on from it to the
 * resource itself — a person reached by clicking their node in a space's diagram lands on
 * a page about their work in that space with nothing leading to their profile.
 * <p>
 * The link is an ordinary internal link carrying the part page's navigation context and
 * part, not a redirect, which is what makes the trip round: the page it opens is a context
 * page reached under a <em>different</em> context, so it shows a back-link to the part
 * rather than ending the trail (issue #697, see {@code TitleBar.createBackContextRef}).
 */
public class OwnPageLink {

    private OwnPageLink() {
    }

    /**
     * The link to the part's own page, or an invisible placeholder where the part has no
     * page of its own.
     * <p>
     * A space or a maintained resource is <em>resolved</em> rather than merely recognised:
     * an id that merely looks like one has no page to offer. A person is recognised instead,
     * by the same test that routes a link to them here in the first place
     * ({@link IndividualAgent#isAgentId}). The two have to
     * agree: a well-formed ORCID nobody has introduced still reaches this page — the space
     * knows their role and their talks even where Nanodash knows nothing else about them —
     * and leaving the link out would strand the reader on it with no way on (issue #822).
     *
     * @param markupId  the Wicket component id
     * @param partId    the part resource id
     * @param partLabel the part's label, carried so the back-link can name it
     * @param context   the resource the part belongs to
     * @return the link, or an invisible {@link Label}
     */
    public static Component of(String markupId, String partId, String partLabel, AbstractResourceWithProfile context) {
        AbstractResourceWithProfile own = NavigationContext.resolve(partId);
        Class<? extends NanodashPage> ownPageClass = NavigationContext.getPageClass(own);
        boolean agent = own instanceof IndividualAgent
                || (ownPageClass == null && IndividualAgent.isAgentId(partId));
        if (agent) ownPageClass = UserPage.class;
        if (ownPageClass == null || context == null) {
            return new Label(markupId).setVisible(false);
        }
        BookmarkablePageLink<Void> link = new BookmarkablePageLink<>(markupId, ownPageClass,
                ownPageParameters(partId, partLabel, context.getId()));
        link.setBody(Model.of(agent ? "\uD83D\uDC64 open user page" : icon(own) + " " + label(own)));
        return link;
    }

    /**
     * The parameters of the link to a part's own page: the part as that page's resource,
     * plus the context and the part it is reached under, which is what gives that page its
     * back-link.
     *
     * @param partId    the part resource id
     * @param partLabel the part's label, or null if none is known
     * @param contextId the context resource id the part is shown under
     * @return the link parameters
     */
    static PageParameters ownPageParameters(String partId, String partLabel, String contextId) {
        // Set through withSelfPageNav rather than withPart, which refuses a link whose
        // target id is the part itself. That rule is right for links within a context,
        // where such a target is the part page and its back-link would point at itself.
        return NavigationContext.withSelfPageNav(new PageParameters().set("id", partId),
                contextId, partId, partLabel);
    }

    /** The icon for the page of the given resource. */
    private static String icon(AbstractResourceWithProfile resource) {
        if (resource instanceof IndividualAgent) return "👤";
        if (resource instanceof Space) return "🏠";
        return "📦";
    }

    /** The wording for the page of the given resource. */
    private static String label(AbstractResourceWithProfile resource) {
        if (resource instanceof IndividualAgent) return "open user page";
        if (resource instanceof Space) return "open space page";
        return "open resource page";
    }

}
