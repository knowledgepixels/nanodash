package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.lookup.ThingOrigins;
import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.GovernedVersions;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Utils;
import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.vocabulary.NTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Singleton class that manages templates data.
 */
public class TemplateData implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(TemplateData.class);

    private static TemplateData instance;

    /**
     * Refreshes the templates data by creating a new instance of TemplateData.
     */
    public static synchronized void refreshTemplates() {
        logger.info("Refreshing templates...");
        instance = new TemplateData(instance);
    }

    /**
     * Ensures that the TemplateData instance is loaded, and asks again for any template
     * listing the query service didn't answer last time.
     */
    public static synchronized void ensureLoaded() {
        if (instance == null) {
            refreshTemplates();
        } else {
            instance.loadMissingListings();
        }
    }

    /**
     * Forgets the loaded instance, so that the next {@link #get()} loads the listings from
     * scratch. For tests, which must not see listings another test has loaded.
     */
    static synchronized void forget() {
        instance = null;
    }

    /**
     * Gets the singleton instance of TemplateData.
     *
     * @return the TemplateData instance
     */
    public static TemplateData get() {
        ensureLoaded();
        return instance;
    }

    private volatile List<ApiResponseEntry> assertionTemplates, provenanceTemplates, pubInfoTemplates;
    private final ConcurrentMap<String, Template> templateMap = new ConcurrentHashMap<>();
    private final Set<String> missingListings = ConcurrentHashMap.newKeySet();

    /**
     * Constructor to initialize the TemplateData instance.
     */
    public TemplateData() {
        this(null);
    }

    /**
     * Loads the template listings, keeping those of the given earlier instance for any listing
     * the query service doesn't answer (issue #916).
     *
     * @param previous the instance this one replaces, or null if there is none
     */
    private TemplateData(TemplateData previous) {
        assertionTemplates = loadListing(QueryApiAccess.GET_ASSERTION_TEMPLATES,
                previous == null ? null : previous.assertionTemplates);
        provenanceTemplates = loadListing(QueryApiAccess.GET_PROVENANCE_TEMPLATES,
                previous == null ? null : previous.provenanceTemplates);
        pubInfoTemplates = loadListing(QueryApiAccess.GET_PUBINFO_TEMPLATES,
                previous == null ? null : previous.pubInfoTemplates);
    }

    private synchronized void loadMissingListings() {
        if (missingListings.isEmpty()) return;
        assertionTemplates = reloadIfMissing(QueryApiAccess.GET_ASSERTION_TEMPLATES, assertionTemplates);
        provenanceTemplates = reloadIfMissing(QueryApiAccess.GET_PROVENANCE_TEMPLATES, provenanceTemplates);
        pubInfoTemplates = reloadIfMissing(QueryApiAccess.GET_PUBINFO_TEMPLATES, pubInfoTemplates);
    }

    private List<ApiResponseEntry> reloadIfMissing(String queryId, List<ApiResponseEntry> current) {
        if (!missingListings.contains(queryId)) return current;
        return loadListing(queryId, current);
    }

    /**
     * Loads one template listing, or, if the query service doesn't answer, keeps the given
     * fallback and remembers to ask again.
     *
     * @param queryId  the query listing the templates
     * @param fallback the listing to keep if there is no answer, or null for an empty one
     * @return the loaded listing, or the fallback
     */
    private List<ApiResponseEntry> loadListing(String queryId, List<ApiResponseEntry> fallback) {
        ApiResponse response = ApiCache.retrieveResponseSync(new QueryRef(queryId), true);
        if (response == null) {
            boolean newlyMissing = missingListings.add(queryId);
            if (newlyMissing) {
                logger.warn("No answer from the query service for template listing {}; {}", queryId,
                        fallback == null || fallback.isEmpty() ? "listing no templates for now" : "keeping the previous listing");
            }
            return fallback == null ? new ArrayList<>() : fallback;
        }
        missingListings.remove(queryId);
        return toListing(response);
    }

    private static List<ApiResponseEntry> toListing(ApiResponse templateEntries) {
        List<ApiResponseEntry> templates = new ArrayList<>();
        String previousId = null;
        logger.info("Loading templates...");
        for (ApiResponseEntry entry : templateEntries.getData()) {
            if ("true".equals(entry.get("unlisted"))) continue;
            // Who published each template and when, so the dropdowns can say so without
            // reading the nanopublications again (the listings report both per row).
            ThingOrigins.record(entry.get("np"), entry.get("pubkey"), entry.get("date"));
            if (!entry.get("np").equals(previousId)) {
                templates.add(entry);
            }
            previousId = entry.get("np");
        }
        templates.sort(templateComparator);
        return templates;
    }

    /**
     * Returns the list of assertion templates.
     *
     * @return a list of assertion templates
     */
    public List<ApiResponseEntry> getAssertionTemplates() {
        return assertionTemplates;
    }

    /**
     * Returns the list of provenance templates.
     *
     * @return a list of provenance templates
     */
    public List<ApiResponseEntry> getProvenanceTemplates() {
        return provenanceTemplates;
    }

    /**
     * Returns the list of publication information templates.
     *
     * @return a list of publication information templates
     */
    public List<ApiResponseEntry> getPubInfoTemplates() {
        return pubInfoTemplates;
    }

    /**
     * Returns a Template object for the given template ID. The ID may be either
     * form: the nanopublication URI, or (for templates with embedded identity) the
     * embedded template-node IRI.
     *
     * @param id the ID of the template
     * @return the Template object if found, or null if not found or invalid
     */
    public Template getTemplate(String id) {
        Template template = templateMap.get(id);
        if (template != null) return template;
        String npId = Utils.stripToNanopubId(id);
        template = templateMap.get(npId);
        if (template != null) {
            templateMap.put(id, template);
            return template;
        }
        if (TrustyUriUtils.isPotentialTrustyUri(npId)) {
            try {
                Template t = new Template(npId);
                templateMap.put(id, t);
                templateMap.put(npId, t);
                templateMap.put(t.getId(), t);
                return t;
            } catch (MalformedTemplateException ex) {
                // Not an error on our side: a nanopublication that claims a template type
                // without defining a template (an FDO/SHACL profile, a maintained-resource
                // declaration of a template's version-independent IRI), or one that cannot be
                // retrieved here at all because it was never published to the registry we read.
                // The listing queries filter these out, but anything may still be referenced by
                // ID, so say what happened without a stack trace. Callers treat null as "not a
                // template".
                logger.warn("Not a template: {} ({})", npId, ex.getMessage());
                return null;
            } catch (Exception ex) {
                logger.error("Could not load template: {}", npId, ex);
                return null;
            }
        }
        return null;
    }

    /**
     * Registers a template from a Nanopub object directly, without fetching it from the registry.
     * This is useful for previewing templates that have not yet been published.
     *
     * @param np the Nanopub containing the template definition
     * @return the Template object, or null if the nanopub is not a valid template
     */
    public Template registerTemplate(Nanopub np) {
        String id = np.getUri().stringValue();
        Template template = templateMap.get(id);
        if (template != null) return template;
        try {
            Template t = new Template(np);
            templateMap.put(id, t);
            templateMap.put(t.getId(), t);
            return t;
        } catch (Exception ex) {
            logger.error("Exception registering template from nanopub: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Resolves a template ID to the ID of the latest version of that template.
     * A version declaring {@code gen:governedBy} resolves space-based: the newest
     * version of its (kind, space) pair signed by a current member+ of the
     * governing space, checked server-side by the
     * {@link QueryApiAccess#GET_LATEST_GOVERNED_VERSION} query, with the pinned
     * version as the floor on an empty result or failure. A version without it
     * follows the supersedes chain of the containing nanopublication, and so does
     * a governed version whose kind isn't a maintained resource of the space yet
     * (answered by the same governed-version query). A supersedes chain that ends
     * at a version handing the template over to a space is followed on into that
     * space's versions ({@link GovernedVersions#resolve(org.nanopub.Nanopub)}). Accepts
     * either ID form (nanopublication URI or embedded template-node IRI) and
     * returns the latest version's canonical ID ({@link Template#getId()}) — so
     * even when there is no newer version, the given ID is normalized to canonical
     * form. Falls back to the given ID if the template cannot be loaded.
     *
     * @param templateId the ID of the template
     * @return the canonical ID of the latest version, or the given ID as fallback
     */
    public String getLatestTemplateId(String templateId) {
        Template pinned = getTemplate(templateId);
        if (pinned == null || pinned.getNanopub() == null) {
            String latestNpId = QueryApiAccess.getLatestVersionId(Utils.stripToNanopubId(templateId));
            Template latest = getTemplate(latestNpId);
            return latest != null ? latest.getId() : templateId;
        }
        String resolvedId = resolveTemplateId(pinned);
        return resolvedId != null ? resolvedId : pinned.getId();
    }


    /**
     * Resolves a template reference that declares space-based governance to the version its
     * governing space currently vouches for, and leaves every other reference untouched.
     * <p>
     * This is the narrow counterpart of {@link #getLatestTemplateId(String)}, for the places
     * that load a provenance or pubinfo template. A reference that says nothing about
     * governance resolves exactly as before — in particular it is <em>not</em> moved along its
     * {@code npx:supersedes} chain — so every template predating this mechanism behaves
     * identically whichever Nanodash version serves it. A reference that does declare
     * {@code dct:isVersionOf} plus {@code gen:governedBy} floats within its
     * {@code (resource, space)} pair, which is what declaring those asks for.
     * <p>
     * That split is what makes the two directions safe while a release rolls out: an older
     * Nanodash meeting a governed provenance or pubinfo template keeps using the exact version
     * referenced (the declaration is inert, never broken), and a newer Nanodash meeting an
     * ungoverned one behaves as the old one did. Instances on either side therefore keep
     * producing valid nanopublications from the same links.
     *
     * @param templateId the template reference to resolve
     * @return the governed version's ID, or the given ID unchanged
     */
    public String getGovernedTemplateId(String templateId) {
        if (templateId == null) return null;
        Template pinned = getTemplate(templateId);
        if (!isSpaceGoverned(pinned) || pinned.getNanopub() == null) return templateId;
        String governedId = resolveTemplateId(pinned);
        return governedId != null ? governedId : templateId;
    }

    /**
     * Whether a template declares space-based governance, i.e. both a version-independent IRI
     * ({@code dct:isVersionOf}) and a governing space ({@code gen:governedBy}). Only these are
     * floated by {@link #getGovernedTemplateId(String)}.
     *
     * @param template the template to check, may be null
     * @return true if the template declares both
     */
    public static boolean isSpaceGoverned(Template template) {
        return template != null && template.getGoverningSpace() != null && template.getTemplateKindIri() != null;
    }
    /**
     * Resolves the pinned template with {@link GovernedVersions#resolve(org.nanopub.Nanopub)}:
     * the latest space-governed version of its (kind, space) pair, or, if it declares no
     * governance or the space doesn't maintain the kind, the head of the pin's own supersedes
     * chain, followed on into a space's versions where that head hands the template over.
     *
     * @return the canonical ID of the version reached, or null if resolution stays at the pin,
     * yields nothing that can be loaded, or fails (the caller then keeps the pin)
     */
    private String resolveTemplateId(Template pinned) {
        try {
            String npId = pinned.getNanopub().getUri().stringValue();
            GovernedVersions.Resolution resolution = GovernedVersions.resolve(pinned.getNanopub());
            if (resolution.staysAt(npId)) return null;
            Template resolved = getTemplate(resolution.versionIri() != null
                    ? resolution.versionIri() : resolution.nanopubId());
            if (resolved != null) return resolved.getId();
        } catch (Exception ex) {
            logger.error("Error resolving latest version for template: {}", pinned.getId(), ex);
        }
        return null;
    }

    /**
     * Returns a Template object for the template of the given Nanopub.
     *
     * @param np the Nanopub from which to extract the template
     * @return the Template object if found, or null if not found or invalid
     */
    public Template getTemplate(Nanopub np) {
        IRI templateId = getTemplateId(np);
        if (templateId == null) return null;
        return getTemplate(templateId.stringValue());
    }

    /**
     * Returns a Template object for the provenance template of the given Nanopub.
     *
     * @param np the Nanopub from which to extract the provenance template
     * @return the Template object if found, or null if not found or invalid
     */
    public Template getProvenanceTemplate(Nanopub np) {
        IRI templateId = getProvenanceTemplateId(np);
        if (templateId == null) return null;
        return getTemplate(templateId.stringValue());
    }

    /**
     * Returns a set of Template objects for the publication information templates of the given Nanopub.
     *
     * @param np the Nanopub from which to extract the publication information templates
     * @return a set of Template objects
     */
    public Set<Template> getPubinfoTemplates(Nanopub np) {
        Set<Template> templates = new HashSet<>();
        for (IRI id : getPubinfoTemplateIds(np)) {
            templates.add(getTemplate(id.stringValue()));
        }
        return templates;
    }

    /**
     * Returns the template ID of the given Nanopub.
     *
     * @param nanopub the Nanopub from which to extract the template ID
     * @return the IRI of the template ID, or null if not found
     */
    public IRI getTemplateId(Nanopub nanopub) {
        for (Statement st : nanopub.getPubinfo()) {
            if (!st.getSubject().equals(nanopub.getUri())) continue;
            if (!st.getPredicate().equals(NTEMPLATE.WAS_CREATED_FROM_TEMPLATE)) continue;
            if (!(st.getObject() instanceof IRI)) continue;
            return (IRI) st.getObject();
        }
        return null;
    }

    /**
     * Returns the provenance template ID of the given Nanopub: the first provenance template
     * its pubinfo says it was created with. A link that does not lead to a provenance template
     * this instance can load is passed over, so that callers fall back as if it were not there.
     * Nanopublications made outside Nanodash have been seen to name, for instance, the
     * assertion template for making provenance templates instead (issue #908).
     *
     * @param nanopub the Nanopub from which to extract the provenance template ID
     * @return the IRI of the provenance template ID, or null if not found
     */
    public IRI getProvenanceTemplateId(Nanopub nanopub) {
        for (Statement st : nanopub.getPubinfo()) {
            if (!st.getSubject().equals(nanopub.getUri())) continue;
            if (!st.getPredicate().equals(NTEMPLATE.WAS_CREATED_FROM_PROVENANCE_TEMPLATE)) continue;
            if (!(st.getObject() instanceof IRI iri)) continue;
            if (!isProvenanceTemplate(iri.stringValue())) continue;
            return iri;
        }
        return null;
    }

    /**
     * Returns the set of publication information template IDs for the given Nanopub, i.e. the
     * pubinfo templates its pubinfo says it was created with. As for
     * {@link #getProvenanceTemplateId(Nanopub)}, links that do not lead to a pubinfo template
     * this instance can load are left out.
     *
     * @param nanopub the Nanopub from which to extract the publication information template IDs
     * @return a set of IRI objects representing the publication information template IDs
     */
    public Set<IRI> getPubinfoTemplateIds(Nanopub nanopub) {
        Set<IRI> iriSet = new HashSet<>();
        for (Statement st : nanopub.getPubinfo()) {
            if (!st.getSubject().equals(nanopub.getUri())) continue;
            if (!st.getPredicate().equals(NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE)) continue;
            if (!(st.getObject() instanceof IRI iri)) continue;
            if (!isPubinfoTemplate(iri.stringValue())) continue;
            iriSet.add(iri);
        }
        return iriSet;
    }

    /**
     * Checks whether the given ID leads to a provenance template this instance can load.
     *
     * @param templateId the template ID
     * @return true if it is a loadable provenance template
     */
    public boolean isProvenanceTemplate(String templateId) {
        Template t = getTemplate(templateId);
        if (t != null && t.isProvenanceTemplate()) return true;
        if (t != null) logger.debug("Not a provenance template: {}", templateId);
        return false;
    }

    /**
     * Checks whether the given ID leads to a publication info template this instance can load.
     *
     * @param templateId the template ID
     * @return true if it is a loadable publication info template
     */
    public boolean isPubinfoTemplate(String templateId) {
        Template t = getTemplate(templateId);
        if (t != null && t.isPubinfoTemplate()) return true;
        if (t != null) logger.debug("Not a publication info template: {}", templateId);
        return false;
    }

    /**
     * Returns a list of Template objects from the given ApiResponse.
     *
     * @param apiResponse the ApiResponse containing template entries
     * @return a list of Template objects
     */
    public static List<Template> getTemplateList(ApiResponse apiResponse) {
        List<Template> templates = new ArrayList<>();
        for (ApiResponseEntry e : apiResponse.getData()) {
            String templateNpId = e.get("template_np");
            if (templateNpId == null) templateNpId = e.get("np");
            templates.add(TemplateData.get().getTemplate(templateNpId));
        }
        return templates;
    }

    private static final TemplateComparator templateComparator = new TemplateComparator();

    private static class TemplateComparator implements Comparator<ApiResponseEntry>, Serializable {

        /**
         * Compares two Template objects based on their labels.
         *
         * @param o1 the first object to be compared.
         * @param o2 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
         */
        @Override
        public int compare(ApiResponseEntry o1, ApiResponseEntry o2) {
            return o1.get("label").compareTo(o2.get("label"));
        }

    }

}
