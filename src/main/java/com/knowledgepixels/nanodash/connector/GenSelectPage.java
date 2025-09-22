package com.knowledgepixels.nanodash.connector;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.Arrays;

/**
 * Page for selecting the type of nanopublication to create.
 */
public class GenSelectPage extends ConnectorPage {

    // TODO This page isn't linked yet, and only for testing so far.


    /**
     * Mount path for this page.
     */
    public static final String MOUNT_PATH = "/connector/select";

    private Form<?> form;
    private RadioGroup<String> radioGroup;

    /**
     * Constructor for the GenSelectPage.
     *
     * @param params Page parameters containing the connector ID.
     */
    public GenSelectPage(PageParameters params) {
        super(params);
        add(new Label("pagetitle", getConfig().getJournalName() + ": Create Nanopublication | nanodash"));
        PageParameters journalParam = new PageParameters().add("journal", getConnectorId());
        add(new TitleBar("titlebar", this, "connectors",
                new NanodashPageRef(GenOverviewPage.class, journalParam, getConfig().getJournalName()),
                new NanodashPageRef("Create Nanopublication")
        ));
        add(new Image("logo", new PackageResourceReference(getConfig().getClass(), getConfig().getLogoFileName())));


        form = new Form<Void>("form") {

            protected void onSubmit() {
                ConnectorOption option = ConnectorOption.valueOf(radioGroup.getModelObject());
                PageParameters params = new PageParameters();
                params.add("journal", getConnectorId());
                params.add("type", option.name().toLowerCase());
                params.add("template", option.getTemplateId());
                params.add("prtemplate", option.getPrTemplateId());
                params.add("pitemplate1", "https://w3id.org/np/RA16U9Wo30ObhrK1NzH7EsmVRiRtvEuEA_Dfc-u8WkUCA");  // Author list
                throw new RestartResponseException(GenPublishPage.class, params);
            }

        };
        form.setOutputMarkupId(true);

        radioGroup = new RadioGroup<>("radio-group", new Model<String>(getConfig().getOptions().get(0).getOptions()[0].name()));
        radioGroup.setOutputMarkupId(true);
        radioGroup.add(new ListView<ConnectorOptionGroup>("option-group", getConfig().getOptions()) {

            @Override
            protected void populateItem(ListItem<ConnectorOptionGroup> item) {
                ConnectorOptionGroup g = item.getModelObject();
                item.add(new Label("option-group-title", g.getTitle()));
                item.add(new ListView<>("option-container", Arrays.asList(g.getOptions())) {

                    @Override
                    protected void populateItem(ListItem<ConnectorOption> item) {
                        ConnectorOption o = item.getModelObject();
                        WebMarkupContainer c = new WebMarkupContainer("option");
                        Radio<String> radio = new Radio<String>("option-input", new Model<String>(o.name()), radioGroup);
                        radio.setOutputMarkupId(true);
                        radioGroup.add(radio);
                        c.add(radio);
                        c.add(new Label("option-title", o.getName()));
                        c.add(new Label("option-explanation", o.getExplanation()).setEscapeModelStrings(false).setVisible(o.getExplanation() != null));
                        item.add(c);
                    }

                });
            }

        });
        form.add(radioGroup);
        add(form);
        add(new ExternalLink("type-support-link", "mailto:contact-project+knowledgepixels-support-desk@incoming.gitlab.com?subject=[" + getConfig().getJournalAbbrev() + "%20type]%20my%20problem/question&body=type%20your%20problem/question%20here"));
        add(new ExternalLink("support-link", "mailto:contact-project+knowledgepixels-support-desk@incoming.gitlab.com?subject=[" + getConfig().getJournalAbbrev() + "%20general]%20my%20problem/question&body=type%20your%20problem/question%20here"));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the mount path for this page.
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

}
