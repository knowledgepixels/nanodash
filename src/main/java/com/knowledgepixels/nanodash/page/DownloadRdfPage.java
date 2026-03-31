package com.knowledgepixels.nanodash.page;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.QueryParamField;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.AbstractResourceStreamWriter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * Page that serves a bulk RDF download of all nanopublications displayed on a given page.
 * Supports TriG, TriX, JSON-LD, and N-Quads formats.
 *
 * Parameters:
 * - type: "user", "space", "resource", or "part"
 * - id: the resource identifier
 * - context: (required for type=part) the context resource ID
 * - format: "trig" (default), "trix", "jsonld", or "nq"
 *
 * NOTE: Currently limited to a maximum of 1000 nanopubs. Pagination is not yet implemented.
 */
public class DownloadRdfPage extends WebPage {

    private static final Logger logger = LoggerFactory.getLogger(DownloadRdfPage.class);

    public static final String MOUNT_PATH = "/download-rdf";

    /**
     * Maximum number of nanopubs to include in a single download.
     * TODO: Implement pagination or streaming for larger downloads.
     */
    private static final int MAX_NANOPUBS = 1000;

    private static final Map<String, RDFFormat> FORMAT_MAP = Map.of(
            "trig", RDFFormat.TRIG,
            "trix", RDFFormat.TRIX,
            "jsonld", RDFFormat.JSONLD,
            "nq", RDFFormat.NQUADS
    );

    private static final Map<String, String> EXTENSION_MAP = Map.of(
            "trig", ".trig",
            "trix", ".xml",
            "jsonld", ".jsonld",
            "nq", ".nq"
    );

    public DownloadRdfPage(final PageParameters parameters) {
        super(parameters);

        String type = parameters.get("type").toString();
        String id = parameters.get("id").toString();
        String format = parameters.get("format").toString("trig");
        boolean asText = !parameters.get("txt").isNull();

        if (type == null || id == null) {
            throw new IllegalArgumentException("Parameters 'type' and 'id' are required");
        }

        RDFFormat rdfFormat = FORMAT_MAP.get(format);
        if (rdfFormat == null) {
            throw new IllegalArgumentException("Unsupported format: " + format + ". Supported: trig, trix, jsonld, nq");
        }

        // Resolve the resource and collect nanopubs
        List<Nanopub> nanopubs = collectNanopubs(type, id, parameters);

        if (nanopubs.size() >= MAX_NANOPUBS) {
            logger.warn("Download for {} {} reached the maximum of {} nanopubs. Results may be incomplete.",
                    type, id, MAX_NANOPUBS);
        }

        logger.info("Serving RDF download: {} nanopubs in {} format for {} {}", nanopubs.size(), format, type, id);

        // Build filename from the resource label or ID
        String safeId = id.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (safeId.length() > 60) safeId = safeId.substring(safeId.length() - 60);
        String extension = EXTENSION_MAP.get(format) + (asText ? ".txt" : "");
        String filename = type + "_" + safeId + extension;

        // When txt parameter is present, serve as text/plain so it always displays in browser
        String contentType = asText ? "text/plain" : rdfFormat.getDefaultMIMEType();

        AbstractResourceStreamWriter stream = new AbstractResourceStreamWriter() {
            @Override
            public void write(OutputStream output) throws IOException {
                for (Nanopub np : nanopubs) {
                    try {
                        NanopubUtils.writeToStream(np, output, rdfFormat);
                        output.write('\n');
                    } catch (Exception ex) {
                        logger.error("Error serializing nanopub {}: {}", np.getUri(), ex.getMessage());
                    }
                }
            }

            @Override
            public String getContentType() {
                return contentType;
            }
        };

        ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(stream, filename);
        handler.setContentDisposition(ContentDisposition.INLINE);
        getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
    }

    /**
     * Collects all nanopubs for the given page type and resource.
     * This includes view display nanopubs and the nanopubs returned by each view's query.
     */
    private List<Nanopub> collectNanopubs(String type, String id, PageParameters parameters) {
        // Use LinkedHashSet to preserve order and deduplicate by URI
        Map<String, Nanopub> collected = new LinkedHashMap<>();

        AbstractResourceWithProfile resource;
        String partId = null;
        Set<IRI> partClasses = null;
        String nanopubRef = null;

        switch (type) {
            case "user" -> {
                resource = IndividualAgent.get(id);
                if (resource == null) {
                    throw new IllegalArgumentException("User not found: " + id);
                }
                waitForData(resource);
            }
            case "space" -> {
                resource = SpaceRepository.get().findById(id);
                if (resource == null) {
                    throw new IllegalArgumentException("Space not found: " + id);
                }
                waitForData(resource);
            }
            case "resource" -> {
                resource = MaintainedResourceRepository.get().findById(id);
                if (resource == null) {
                    throw new IllegalArgumentException("Resource not found: " + id);
                }
                waitForData(resource);
            }
            case "part" -> {
                String contextId = parameters.get("context").toString();
                if (contextId == null) {
                    throw new IllegalArgumentException("Parameter 'context' is required for type=part");
                }
                resource = resolveContextResource(contextId);
                waitForData(resource);
                partId = id;
                partClasses = resolvePartClasses(id, contextId, resource);
                nanopubRef = resolvePartNanopubRef(id, contextId, resource);
            }
            default -> throw new IllegalArgumentException("Unknown type: " + type + ". Supported: user, space, resource, part");
        }

        // Get view displays
        List<ViewDisplay> viewDisplays;
        if (partId != null) {
            viewDisplays = resource.getPartLevelViewDisplays(partId, partClasses);
        } else {
            viewDisplays = resource.getTopLevelViewDisplays();
        }

        String targetId = partId != null ? partId : resource.getId();
        String targetNpId = nanopubRef != null ? nanopubRef : resource.getNanopubId();

        for (ViewDisplay vd : viewDisplays) {
            // Add the view display nanopub itself
            if (vd.getNanopub() != null) {
                addNanopub(collected, vd.getNanopub());
            }

            if (collected.size() >= MAX_NANOPUBS) break;

            // Build query parameters (mirrors ViewList logic)
            QueryRef queryRef = buildQueryRef(vd, resource, targetId, targetNpId);
            if (queryRef == null) continue;

            // Execute the query synchronously and collect result nanopubs
            try {
                ApiResponse response = ApiCache.retrieveResponseSync(queryRef, false);
                if (response == null) continue;

                for (ApiResponseEntry entry : response.getData()) {
                    if (collected.size() >= MAX_NANOPUBS) break;

                    // Single-valued "np" column
                    String npUri = entry.get("np");
                    if (npUri != null) {
                        fetchAndAdd(collected, npUri);
                    }

                    // Multi-valued "np_multi_iri" column (space-separated URIs)
                    String npMulti = entry.get("np_multi_iri");
                    if (npMulti != null) {
                        for (String uri : npMulti.split("\\s+")) {
                            if (collected.size() >= MAX_NANOPUBS) break;
                            if (!uri.isBlank()) {
                                fetchAndAdd(collected, uri);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                logger.error("Error executing query for view display {}: {}", vd.getId(), ex.getMessage());
            }
        }

        return new ArrayList<>(collected.values());
    }

    /**
     * Builds a QueryRef for a view display, mirroring the parameter logic from ViewList.
     */
    private QueryRef buildQueryRef(ViewDisplay vd, AbstractResourceWithProfile resource, String targetId, String targetNpId) {
        View view = vd.getView();
        if (view == null || view.getQuery() == null) return null;

        Multimap<String, String> queryRefParams = ArrayListMultimap.create();
        for (String p : view.getQuery().getPlaceholdersList()) {
            String paramName = QueryParamField.getParamName(p);
            if (paramName.equals(view.getQueryField())) {
                queryRefParams.put(view.getQueryField(), targetId);
                if (QueryParamField.isMultiPlaceholder(p) && resource instanceof Space space) {
                    for (String altId : space.getAltIDs()) {
                        queryRefParams.put(view.getQueryField(), altId);
                    }
                }
            } else if (paramName.equals(view.getQueryField() + "Namespace") && resource.getNamespace() != null) {
                queryRefParams.put(view.getQueryField() + "Namespace", resource.getNamespace());
            } else if (paramName.equals(view.getQueryField() + "Np")) {
                if (!QueryParamField.isOptional(p) && targetNpId == null) {
                    queryRefParams.put(view.getQueryField() + "Np", "x:");
                } else {
                    queryRefParams.put(view.getQueryField() + "Np", targetNpId);
                }
            } else if (paramName.equals("user_pubkey") && QueryParamField.isMultiPlaceholder(p) && resource instanceof Space space) {
                for (IRI userId : space.getUsers()) {
                    for (String memberHash : User.getUserData().getPubkeyHashes(userId, true)) {
                        queryRefParams.put("user_pubkey", memberHash);
                    }
                }
            } else if (paramName.equals("admin_pubkey") && QueryParamField.isMultiPlaceholder(p) && resource instanceof Space space) {
                for (IRI adminId : space.getAdmins()) {
                    for (String adminHash : User.getUserData().getPubkeyHashes(adminId, true)) {
                        queryRefParams.put("admin_pubkey", adminHash);
                    }
                }
            } else if (!QueryParamField.isOptional(p)) {
                logger.error("Query has non-optional parameter that cannot be filled: {} {}", view.getQuery().getQueryId(), p);
                return null;
            }
        }
        return new QueryRef(view.getQuery().getQueryId(), queryRefParams);
    }

    private void addNanopub(Map<String, Nanopub> collected, Nanopub np) {
        String uri = np.getUri().stringValue();
        if (!collected.containsKey(uri)) {
            collected.put(uri, np);
        }
    }

    private void fetchAndAdd(Map<String, Nanopub> collected, String npUri) {
        Nanopub np = Utils.getAsNanopub(npUri);
        if (np != null) {
            addNanopub(collected, np);
        }
    }

    /**
     * Waits for the resource's async data loading to complete (up to 30 seconds).
     */
    private void waitForData(AbstractResourceWithProfile resource) {
        resource.triggerDataUpdate();
        int waited = 0;
        while (!resource.isDataInitialized() && waited < 30_000) {
            try {
                Thread.sleep(200);
                waited += 200;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (!resource.isDataInitialized()) {
            logger.warn("Data not initialized for resource {} after {}ms", resource.getId(), waited);
        }
    }

    /**
     * Resolves the context resource for a part page (same logic as ResourcePartPage).
     */
    private AbstractResourceWithProfile resolveContextResource(String contextId) {
        AbstractResourceWithProfile resource = MaintainedResourceRepository.get().findById(contextId);
        if (resource != null) return resource;

        if (SpaceRepository.get().findById(contextId) != null) {
            return SpaceRepository.get().findById(contextId);
        }
        if (IndividualAgent.isUser(contextId)) {
            return IndividualAgent.get(contextId);
        }
        throw new IllegalArgumentException("Not a resource, space, or user: " + contextId);
    }

    /**
     * Resolves the classes of a part (mirrors ResourcePartPage logic).
     */
    private Set<IRI> resolvePartClasses(String partId, String contextId, AbstractResourceWithProfile resource) {
        Set<IRI> classes = new HashSet<>();
        String nanopubId = resolvePartNanopubId(partId, contextId, resource);
        if (nanopubId != null) {
            Nanopub nanopub = Utils.getAsNanopub(nanopubId);
            if (nanopub != null) {
                for (Statement st : nanopub.getAssertion()) {
                    if (st.getSubject().stringValue().equals(partId) && st.getPredicate().equals(RDF.TYPE) && st.getObject() instanceof IRI objIri) {
                        classes.add(objIri);
                    }
                }
            }
        }
        return classes;
    }

    /**
     * Resolves the nanopub ref for a part (used as query param), returning "x:" if not found.
     */
    private String resolvePartNanopubRef(String partId, String contextId, AbstractResourceWithProfile resource) {
        String npId = resolvePartNanopubId(partId, contextId, resource);
        return npId != null ? npId : "x:";
    }

    /**
     * Looks up the nanopub ID for a part's term definition (mirrors ResourcePartPage logic).
     */
    private String resolvePartNanopubId(String partId, String contextId, AbstractResourceWithProfile resource) {
        QueryRef getDefQuery = new QueryRef(QueryApiAccess.GET_TERM_DEFINITIONS, "term", partId);
        if (resource.getSpace() != null) {
            for (IRI userIri : resource.getSpace().getUsers()) {
                for (String pubkey : User.getUserData().getPubkeyHashes(userIri, true)) {
                    getDefQuery.getParams().put("pubkey", pubkey);
                }
            }
        } else {
            for (String pubkey : User.getUserData().getPubkeyHashes(Utils.vf.createIRI(contextId), true)) {
                getDefQuery.getParams().put("pubkey", pubkey);
            }
        }
        ApiResponse resp = ApiCache.retrieveResponseSync(getDefQuery, false);
        if (resp != null && !resp.getData().isEmpty()) {
            return resp.getData().iterator().next().get("np");
        }
        return null;
    }

}
