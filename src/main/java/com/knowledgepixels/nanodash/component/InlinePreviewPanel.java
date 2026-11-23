package com.knowledgepixels.nanodash.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.danekja.java.util.function.serializable.SerializableConsumer;
import org.nanopub.Nanopub;

import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.ProtectedNanopubs;
import com.knowledgepixels.nanodash.page.PreviewPage;

/**
 * The preview of an inline publish form (see {@link PublishForm.InlinePreview}), shown in its
 * place: what the preview page shows, inside the page the form is on. Publishing from here
 * forwards like publishing from the form; going back shows the form again as it was.
 */
public class InlinePreviewPanel extends Panel {

    /**
     * Creates the preview.
     *
     * @param id           the Wicket component ID
     * @param signedNp     the nanopublication as it would be published
     * @param pageParams   the parameters the form was built from
     * @param consentGiven whether the consent checkbox of the form was ticked
     * @param onBack       shows the form again
     */
    public InlinePreviewPanel(String id, Nanopub signedNp, PageParameters pageParams, boolean consentGiven,
            SerializableConsumer<AjaxRequestTarget> onBack) {
        super(id);
        setOutputMarkupId(true);

        add(new NanopubItem("nanopub", NanopubElement.get(signedNp)).noActions());

        FeedbackPanel feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        Form<Void> form = new Form<>("form");
        add(form);

        // As on the preview page: the protected marker is part of the signed nanopublication,
        // so it is shown and cannot be changed here, and there is no consent to give for it (#671).
        boolean isProtected = ProtectedNanopubs.isProtected(signedNp);
        WebMarkupContainer protectedSection = new WebMarkupContainer("protected-section");
        // Generated ids rather than fixed ones (as the preview page has), for the labels to point at.
        protectedSection.add(new CheckBox("protectedcheck", new Model<>(true)).setOutputMarkupId(true).setEnabled(false));
        protectedSection.add(new Label("protected-note", ProtectedNanopubs.STAYS_LOCAL_NOTE));
        protectedSection.setVisible(isProtected);
        form.add(protectedSection);

        WebMarkupContainer consentSection = new WebMarkupContainer("consent-section");
        CheckBox consentCheck = new CheckBox("consentcheck", new Model<>(consentGiven));
        consentCheck.setOutputMarkupId(true);
        consentCheck.setRequired(!isProtected);
        consentCheck.add((IValidator<Boolean>) validatable -> {
            if (!Boolean.TRUE.equals(validatable.getValue())) {
                validatable.error(new ValidationError("You need to check the checkbox that you understand the consequences."));
            }
        });
        consentSection.add(consentCheck);
        consentSection.add(new Label("consenttext", PublishForm.getConsentText()));
        consentSection.setVisible(!isProtected);
        form.add(consentSection);

        form.add(new AjaxButton("publish-button") {

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(new AjaxCallListener().onBefore("showPublishing(this);"));
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                // Leaves for the page publishing forwards to; returns only if it failed.
                PreviewPage.publishPreviewed(signedNp, pageParams, null, feedbackPanel, null);
                showAgain(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                showAgain(target);
            }

        });

        AjaxButton backButton = new AjaxButton("back-button") {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                onBack.accept(target);
            }

        };
        backButton.setDefaultFormProcessing(false);
        form.add(backButton);
    }

    private void showAgain(AjaxRequestTarget target) {
        target.add(this);
        target.appendJavaScript("publishingStopped();");
    }

}
