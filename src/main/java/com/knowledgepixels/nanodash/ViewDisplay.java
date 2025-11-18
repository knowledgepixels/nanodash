package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class representing the display of a resource view associated with a Space.
 */
public class ViewDisplay implements Serializable, Comparable<ViewDisplay> {

    private static final Logger logger = LoggerFactory.getLogger(ViewDisplay.class);

    private String id;
    private Nanopub nanopub;
    private ResourceView view;
    private String title;
    private Integer pageSize;
    private Integer displayWidth;
    private String structuralPosition;
    private Set<IRI> types = new HashSet<>();
    private IRI resource;

    private static Map<String, ViewDisplay> viewDisplays = new HashMap<>();

    /**
     * Get a ResourceView by its ID.
     *
     * @param id the ID of the ResourceView
     * @return the ResourceView object
     */
    public static ViewDisplay get(String id) {
        if (!viewDisplays.containsKey(id)) {
            try {
                Nanopub np = Utils.getAsNanopub(id.replaceFirst("^(.*[^A-Za-z0-9-_])?(RA[A-Za-z0-9-_]{43})[^A-Za-z0-9-_].*$", "$2"));
                viewDisplays.put(id, new ViewDisplay(id, np));
            } catch (Exception ex) {
                logger.error("Couldn't load nanopub for resource: " + id, ex);
            }
        }
        return viewDisplays.get(id);
    }

    /**
     * Constructor for ViewDisplay.
     *
     * @param entry an ApiResponseEntry containing the view and nanopub ID.
     */
    private ViewDisplay(String id, Nanopub nanopub) {
        this.id = id;
        this.nanopub = nanopub;

        boolean viewDisplayTypeFound = false;
        for (Statement st : nanopub.getAssertion()) {
            if (st.getSubject().stringValue().equals(id)) {
                if (st.getPredicate().equals(RDF.TYPE)) {
                    if (st.getObject().equals(KPXL_TERMS.VIEW_DISPLAY)) {
                        viewDisplayTypeFound = true;
                    } else if (st.getObject() instanceof IRI objIri) {
                        types.add(objIri);
                    }
                } else if (st.getPredicate().equals(DCTERMS.TITLE)) {
                    title = st.getObject().stringValue();
                } else if (st.getPredicate().equals(KPXL_TERMS.IS_DISPLAY_OF_VIEW) && st.getObject() instanceof IRI objIri) {
                    if (view != null) {
                        throw new IllegalArgumentException("View already set: " + objIri);
                    }
                    view = ResourceView.get(objIri.stringValue());
                } else if (st.getPredicate().equals(KPXL_TERMS.IS_DISPLAY_FOR) && st.getObject() instanceof IRI objIri) {
                    if (resource != null) {
                        throw new IllegalArgumentException("Resource already set: " + objIri);
                    }
                    resource = objIri;
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_PAGE_SIZE) && st.getObject() instanceof Literal objL) {
                    try {
                        pageSize = Integer.parseInt(objL.stringValue());
                    } catch (NumberFormatException ex) {
                        logger.error("Invalid page size value: " + objL.stringValue(), ex);
                    }
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_DISPLAY_WIDTH) && st.getObject() instanceof IRI objIri) {
                    displayWidth = ResourceView.columnWidths.get(objIri);
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_DISPLAY_WIDTH) && st.getObject() instanceof Literal objL) {
                    structuralPosition = objL.stringValue();
                }
            }
        }
        if (!viewDisplayTypeFound) throw new IllegalArgumentException("Not a proper view display nanopub: " + id);
        if (view == null) throw new IllegalArgumentException("View not found: " + id);
    }

    public String getId() {
        return id;
    }

    /**
     * Creates a plain minimal view display without attached view object.
     *
     * @param pageSize the page size of the view display
     */
    public ViewDisplay(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Gets the ResourceView associated with this ViewDisplay.
     *
     * @return the ResourceView
     */
    public ResourceView getView() {
        return view;
    }

    /**
     * Gets the nanopub ID associated with this ViewDisplay.
     *
     * @return the nanopub ID
     */
    public String getNanopubId() {
        if (nanopub == null) return null;
        return nanopub.getUri().stringValue();
    }

    public Integer getPageSize() {
        if (pageSize != null) return pageSize;
        if (view == null) return 10;
        if (view.getPageSize() != null) return view.getPageSize();
        return 10;
    }

    public Integer getDisplayWidth() {
        if (displayWidth != null) return displayWidth;
        if (view == null) return 12;
        if (view.getDisplayWidth() != null) return view.getDisplayWidth();
        return 12;
    }

    public String getStructuralPosition() {
        if (structuralPosition != null) return structuralPosition;
        if (view == null) return "5.5.default";
        if (view.getStructuralPosition() != null) return view.getStructuralPosition();
        return "5.5.default";
    }

    public String getTitle() {
        if (title != null) return title;
        if (view != null) return view.getTitle();
        return null;
    }

    @Override
    public int compareTo(ViewDisplay other) {
        return this.getStructuralPosition().compareTo(other.getStructuralPosition());
    }

    

}
