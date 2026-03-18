package com.knowledgepixels.nanodash.page;

import java.io.ByteArrayInputStream;
import java.util.Base64;

import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.NanopubItem;
import com.knowledgepixels.nanodash.component.TemplateFormPreview;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.vocabulary.NTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ViewPage is the page that displays a single nanopublication.
 */
public class ViewPage extends NanodashPage {

    private static final Logger logger = LoggerFactory.getLogger(ViewPage.class);

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/view";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for the ViewPage.
     *
     * @param parameters The page parameters containing the nanopub ID and display options.
     */
    public ViewPage(final PageParameters parameters) {
        super(parameters);
        add(new TitleBar("titlebar", this, "preview"));
        addNanopubItem(this, parameters);
    }

    static void addNanopubItem(NanodashPage page, final PageParameters parameters) {
        Nanopub np;
        String nanopubTrig = parameters.get("_nanopub_trig").toOptionalString();
        boolean isPreview = nanopubTrig != null;
        if (isPreview) {
            try {
                byte[] trig = Base64.getUrlDecoder().decode(nanopubTrig);
                np = new NanopubImpl(new ByteArrayInputStream(trig), RDFFormat.TRIG);
            } catch (MalformedNanopubException | java.io.IOException | IllegalArgumentException ex) {
                throw new RuntimeException("Failed to parse nanopub from '_nanopub_trig' parameter", ex);
            }
        } else {
            String ref = parameters.get("id").toString();
            np = Utils.getAsNanopub(ref);
        }
        boolean showHeader = "on".equals(parameters.get("show-header").toOptionalString());
        boolean showFooter = "on".equals(parameters.get("show-footer").toOptionalString());
        boolean showProv = !"off".equals(parameters.get("show-prov").toOptionalString());
        boolean showPubinfo = !"off".equals(parameters.get("show-pubinfo").toOptionalString());
        String templateId = parameters.get("template").toString(null);
        page.add(new Label("heading", isPreview ? "Preview Nanopublication" : "Nanopublication"));
        page.add(new NanopubItem("nanopub", NanopubElement.get(np), templateId).setProvenanceHidden(!showProv).setPubinfoHidden(!showPubinfo).setHeaderHidden(!showHeader).setFooterHidden(!showFooter));

        if (Utils.isNanopubOfClass(np, NTEMPLATE.ASSERTION_TEMPLATE)) {
            WebMarkupContainer section = new WebMarkupContainer("template-form-preview-section");
            try {
                section.add(new TemplateFormPreview("template-form-preview", np));
            } catch (Exception ex) {
                logger.error("Failed to generate template form preview: {}", ex.getMessage());
                String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName();
                section.add(new Label("template-form-preview", "<p class=\"negative\">Error generating template form preview: " + message + "</p>").setEscapeModelStrings(false));
            }
            page.add(section);
        } else {
            page.add(new WebMarkupContainer("template-form-preview-section").setVisible(false));
        }
    }

}
