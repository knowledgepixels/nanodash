package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Tells visitors what happens to their personal data on this instance.
 * <p>
 * Nanodash is run by whoever deploys it, so this page describes what the software does —
 * the ORCID login, the signing key kept server-side, and above all that publishing is
 * permanent and public — and names the operator only where the deployment has configured
 * one with {@code NANODASH_PRIVACY_OPERATOR}. An instance that would rather point at a
 * policy of its own can set {@code NANODASH_PRIVACY_URL} instead, in which case the footer
 * links there and this page is never reached.
 */
public class PrivacyPage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/privacy";

    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for PrivacyPage.
     *
     * @param parameters the page parameters
     */
    public PrivacyPage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this));
        add(new Label("pagetitle", "Privacy" + titleSuffix()));

        NanodashPreferences prefs = NanodashPreferences.get();

        String operator = prefs.getPrivacyOperator();
        add(new Label("operator", operator).setVisible(operator != null));
        add(new WebMarkupContainer("no-operator").setVisible(operator == null));

        // Only an instance that actually runs analytics should say that it does.
        String umamiUrl = prefs.getUmamiScriptUrl();
        String umamiId = prefs.getUmamiWebsiteId();
        boolean analytics = umamiUrl != null && !umamiUrl.isBlank() && umamiId != null && !umamiId.isBlank();
        add(new WebMarkupContainer("analytics").setVisible(analytics));
    }

}
