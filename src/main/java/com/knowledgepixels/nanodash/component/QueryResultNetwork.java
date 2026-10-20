package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NetworkGraph;
import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.WicketApplication;
import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.util.List;

/**
 * Component for displaying query results as an interactive network (gen:NetworkView,
 * issue #792). The query returns one row per edge — {@code source} and {@code target}
 * node IRIs, with the usual {@code _label} companions, optional {@code _type} columns
 * grouping the nodes by colour, and optional {@code edge_type}/{@code edge_label} naming
 * the relation, and optional {@code _desc} columns setting the hover text — and
 * {@link NetworkGraph} turns those rows into the node and edge sets that the browser lays
 * out, draws and makes hoverable.
 *
 * <p>The graph travels to the page as JSON in a script block rather than as markup: a
 * diagram on a canvas is not something the HTML sanitizer could carry (it drops
 * {@code data-*} attributes and scripts alike), and JSON keeps every label as data, so
 * nothing query-derived is ever parsed as markup.
 *
 * <p>Per-row entry actions are not offered here. They need a row of their own to hang a
 * menu on, which a canvas does not have; the view's result-level actions work as for any
 * other view type.
 */
public class QueryResultNetwork extends QueryResult {

    // The layout engine is loaded with the component, not with every page, exactly as the
    // rich-text editor's resources are: a page without a network view pays for none of it.
    // The standalone UMD build is self-contained and exposes the "vis" global.
    private static final WebjarsJavaScriptResourceReference VIS_NETWORK_JS =
            new WebjarsJavaScriptResourceReference("vis-network/current/standalone/umd/vis-network.min.js");
    private static final WebjarsCssResourceReference VIS_NETWORK_CSS =
            new WebjarsCssResourceReference("vis-network/current/styles/vis-network.min.css");
    private static final JavaScriptResourceReference NETWORK_JS =
            new JavaScriptResourceReference(WicketApplication.class, "script/network.js");

    private final LoadableDetachableModel<NetworkGraph.Graph> graphModel;

    /**
     * Constructor for QueryResultNetwork.
     *
     * @param markupId    the markup ID
     * @param queryRef    the query reference
     * @param response    the API response
     * @param viewDisplay the view display
     */
    QueryResultNetwork(String markupId, QueryRef queryRef, ApiResponse response, ViewDisplay viewDisplay) {
        super(markupId, queryRef, response, viewDisplay);

        String label = grlcQuery.getLabel();
        if (viewDisplay.getTitle() != null) {
            label = viewDisplay.getTitle();
        }
        add(new Label("label", label).setVisible(label != null && !label.isEmpty()));
        setOutputMarkupId(true);

        // Built when the page renders, not here: the page a node leads to depends on the
        // navigation context, which is only known once this component is on a page.
        graphModel = new LoadableDetachableModel<>() {
            @Override
            protected NetworkGraph.Graph load() {
                return NetworkGraph.build(QueryResultNetwork.this.response,
                        QueryResultNetwork.this::nodeUrl, NanodashLink::getKnownLabel);
            }
        };

        populateComponent();
    }

    @Override
    protected void populateComponent() {
        // Nothing found means no canvas either, rather than an empty box the height of one.
        WebMarkupContainer container = new WebMarkupContainer("network-container") {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(!graphModel.getObject().isEmpty());
            }
        };
        container.setOutputMarkupId(true);
        container.add(new AttributeAppender("class", " network-view-content"));
        // The graph as data, read by script/network.js from inside the container. Not
        // escaped as markup, because Gson has already escaped it as JSON — which is what
        // keeps a label carrying "</script>" from closing this block.
        container.add(new Label("graph-data", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return graphModel.getObject().json();
            }
        }).setEscapeModelStrings(false));
        add(container);

        add(new Label("no-records", "(nothing found)") {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(graphModel.getObject().isEmpty());
            }
        });

        // The colours only mean something if the reader is told what they stand for.
        WebMarkupContainer legendRow = new WebMarkupContainer("legend-row") {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(!graphModel.getObject().legend().isEmpty());
            }
        };
        legendRow.add(new ListView<NetworkGraph.LegendEntry>("legend", new LoadableDetachableModel<List<NetworkGraph.LegendEntry>>() {
            @Override
            protected List<NetworkGraph.LegendEntry> load() {
                return graphModel.getObject().legend();
            }
        }) {
            @Override
            protected void populateItem(ListItem<NetworkGraph.LegendEntry> item) {
                NetworkGraph.LegendEntry entry = item.getModelObject();
                WebMarkupContainer legendEntry = new WebMarkupContainer("entry");
                // Read by script/network.js, which puts the type's IRI in the status line
                // while the pointer is on its entry, as it does for a node or an edge.
                legendEntry.add(AttributeModifier.replace("data-iri", entry.type()));
                WebMarkupContainer swatch = new WebMarkupContainer("swatch");
                swatch.add(new AttributeAppender("style", "background-color: " + entry.color()));
                legendEntry.add(swatch);
                legendEntry.add(new Label("type", entry.label()));
                item.add(legendEntry);
            }
        });
        add(legendRow);

        add(new Label("truncation-note", "(only the first " + NetworkGraph.MAX_EDGES + " connections are shown)") {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(graphModel.getObject().truncated());
            }
        });
    }

    /**
     * The page a node or edge leads to on click: the IRI's own page under this view's
     * navigation context, so the destination's back-link leads back here.
     */
    private String nodeUrl(String iri) {
        String url = NanodashLink.getPageUrl(iri, renderContextId());
        // Only an app-internal link takes the navigation parameters; an external resource
        // (an ipfs:/did: resolver, or anything outside a site) is linked as itself.
        return url.startsWith("/") ? url + linkNavParams(url) : url;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(VIS_NETWORK_CSS));
        response.render(JavaScriptHeaderItem.forReference(VIS_NETWORK_JS));
        response.render(JavaScriptHeaderItem.forReference(NETWORK_JS));
    }

    @Override
    protected void onDetach() {
        graphModel.detach();
        super.onDetach();
    }

}
