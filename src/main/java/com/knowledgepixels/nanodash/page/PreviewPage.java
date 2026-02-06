package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.MaintainedResource;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.ResourceWithProfile;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.component.NanopubItem;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.nanopub.extra.server.PublishNanopub;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewPage extends NanodashPage {

    private static final Logger logger = LoggerFactory.getLogger(PreviewPage.class);

    public static final String MOUNT_PATH = "/preview";

    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    public PreviewPage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, "preview"));

        String previewId = parameters.get("id").toString();
        if (previewId == null) {
            throw new RestartResponseException(HomePage.class);
        }

        NanodashSession.PreviewNanopub preview = NanodashSession.get().getPreviewNanopub(previewId);
        if (preview == null) {
            throw new RestartResponseException(HomePage.class);
        }

        Nanopub signedNp = preview.getNanopub();
        PageParameters pageParams = preview.getPageParams();
        Class<? extends org.apache.wicket.markup.html.WebPage> confirmPageClass = preview.getConfirmPageClass();

        add(new NanopubItem("nanopub", NanopubElement.get(signedNp)));

        FeedbackPanel feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        add(new Link<Void>("publish-button") {
            @Override
            public void onClick() {
                try {
                    String npUrl = PublishNanopub.publish(signedNp);
                    logger.info("Nanopublication published from preview: {}", npUrl);
                    NanodashSession.get().removePreviewNanopub(previewId);

                    if (!pageParams.get("refresh-upon-publish").isEmpty()) {
                        String toRefresh = pageParams.get("refresh-upon-publish").toString();
                        if (toRefresh.equals("spaces")) {
                            Space.forceRootRefresh(3 * 1000);
                        } else if (toRefresh.equals("maintainedResources")) {
                            MaintainedResource.forceRootRefresh(3 * 1000);
                        } else if (ResourceWithProfile.isResourceWithProfile(toRefresh)) {
                            ResourceWithProfile.forceRefresh(toRefresh, 3 * 1000);
                        } else {
                            QueryRef queryRef = QueryRef.parseString(toRefresh);
                            ApiCache.clearCache(queryRef, 3 * 1000);
                        }
                    }

                    String contextId = pageParams.get("context").toString("");
                    String partId = pageParams.get("part").toString("");
                    if (!contextId.isEmpty()) {
                        PageParameters redirectParams = new PageParameters().set("just-published", signedNp.getUri().stringValue());
                        if (!partId.isEmpty()) {
                            redirectParams.set("id", partId).set("context", contextId);
                            throw new RestartResponseException(ResourcePartPage.class, redirectParams);
                        }
                        redirectParams.set("id", contextId);
                        if (Space.get(contextId) != null) {
                            throw new RestartResponseException(SpacePage.class, redirectParams);
                        }
                        if (MaintainedResource.get(contextId) != null) {
                            throw new RestartResponseException(MaintainedResourcePage.class, redirectParams);
                        }
                        if (User.isUser(contextId)) {
                            throw new RestartResponseException(UserPage.class, redirectParams);
                        }
                    }

                    throw new RestartResponseException(
                            confirmPageClass,
                            new PageParameters(pageParams).set("id", signedNp.getUri().stringValue())
                    );
                } catch (RestartResponseException ex) {
                    throw ex;
                } catch (Exception ex) {
                    logger.error("Nanopublication publishing from preview failed: {}", ex);
                    String message = ex.getClass().getName();
                    if (ex.getMessage() != null) {
                        message = ex.getMessage();
                    }
                    feedbackPanel.error(message);
                }
            }
        });

        add(new Link<Void>("discard-button") {
            @Override
            public void onClick() {
                NanodashSession.get().removePreviewNanopub(previewId);
                throw new RestartResponseException(PublishPage.class, new PageParameters(pageParams));
            }
        });
    }

}
