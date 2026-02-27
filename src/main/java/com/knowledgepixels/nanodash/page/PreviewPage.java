package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.component.NanopubItem;
import com.knowledgepixels.nanodash.component.TemplateFormPreview;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.nanopub.Nanopub;
import org.nanopub.extra.server.PublishNanopub;
import org.nanopub.vocabulary.NTEMPLATE;
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

        add(new NanopubItem("nanopub", NanopubElement.get(signedNp)).noActions());

        FeedbackPanel feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        Form<Void> form = new Form<Void>("form") {
            @Override
            protected void onSubmit() {
                try {
                    String npUrl = PublishNanopub.publish(signedNp);
                    logger.info("Nanopublication published from preview: {}", npUrl);
                    NanodashSession.get().removePreviewNanopub(previewId);

                    if (!pageParams.get("refresh-upon-publish").isEmpty()) {
                        String toRefresh = pageParams.get("refresh-upon-publish").toString();
                        WicketApplication.get().notifyNanopubPublished(signedNp, toRefresh, 3 * 1000);
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
        };
        add(form);

        CheckBox consentCheck = new CheckBox("consentcheck", new Model<>(preview.isConsentChecked()));
        consentCheck.setRequired(true);
        consentCheck.add(new IValidator<Boolean>() {

            @Override
            public void validate(IValidatable<Boolean> validatable) {
                if (!Boolean.TRUE.equals(validatable.getValue())) {
                    validatable.error(new ValidationError("You need to check the checkbox that you understand the consequences."));
                }
            }

        });
        form.add(consentCheck);

        Button discardButton = new Button("discard-button") {
            @Override
            public void onSubmit() {
                NanodashSession.get().removePreviewNanopub(previewId);
                throw new RestartResponseException(PublishPage.class, new PageParameters(pageParams));
            }
        };
        discardButton.setDefaultFormProcessing(false);
        form.add(discardButton);

        if (Utils.isNanopubOfClass(signedNp, NTEMPLATE.ASSERTION_TEMPLATE)) {
            WebMarkupContainer section = new WebMarkupContainer("template-form-preview-section");
            try {
                section.add(new TemplateFormPreview("template-form-preview", signedNp));
            } catch (Exception ex) {
                logger.error("Failed to generate template form preview: {}", ex.getMessage());
                String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName();
                section.add(new Label("template-form-preview", "<p class=\"negative\">Error generating template form preview: " + message + "</p>").setEscapeModelStrings(false));
            }
            add(section);
        } else {
            add(new WebMarkupContainer("template-form-preview-section").setVisible(false));
        }
    }

}
