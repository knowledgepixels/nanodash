package com.knowledgepixels.nanodash;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Runs SPARQL queries against a Nanopub Query instance that exposes the
 * {@code /repo/spaces} endpoint (nanopub-query &gt;= 1.11). Used for
 * space-related calculations (admin closure, role attachments, members,
 * sub-space links, etc.) that are materialised server-side.
 *
 * <p>Today only {@code https://query.nanodash.net/} carries this repo, so we
 * default to it directly rather than going through the general main-query URL
 * (which can resolve to instances without the spaces repo). Override with the
 * {@code NANODASH_SPACES_REPO_URL} env var. Once the spaces repo is widely
 * deployed, the default should fall back to {@link Utils#getMainQueryUrl()}.
 *
 * <p>Empty results are treated as authoritative; callers do not fall back to
 * a legacy code path.
 */
public class SpacesRepoAccess {

    private static final Logger logger = LoggerFactory.getLogger(SpacesRepoAccess.class);

    private static final String DEFAULT_SPACES_REPO_URL = "https://query.nanodash.net/repo/spaces";

    private static final SpacesRepoAccess INSTANCE = new SpacesRepoAccess();

    /**
     * Standard SPARQL prologue used by space-related queries. Includes the
     * pointer-resolution pattern that joins the current {@code npass:*}
     * state-graph IRI in {@code npa:graph} so reads stay atomic across
     * trust-state flips (see {@code design-space-repositories.md} §Querying).
     */
    public static final String PREFIXES = ""
            + "PREFIX npa: <http://purl.org/nanopub/admin/>\n"
            + "PREFIX npx: <http://purl.org/nanopub/x/>\n"
            + "PREFIX np: <http://www.nanopub.org/nschema#>\n"
            + "PREFIX gen: <https://w3id.org/kpxl/gen/terms/>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX dct: <http://purl.org/dc/terms/>\n"
            + "PREFIX schema: <http://schema.org/>\n"
            + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n";

    /** Pointer-resolution boilerplate; bind {@code ?g} to the current space-state graph. */
    public static final String CURRENT_STATE_POINTER =
            "  GRAPH npa:graph { npa:thisRepo npa:hasCurrentSpaceState ?g . }\n";

    /**
     * Singleton accessor.
     *
     * @return the shared instance
     */
    public static SpacesRepoAccess get() {
        return INSTANCE;
    }

    private volatile SPARQLRepository repo;
    private volatile String endpointUrl;

    private SpacesRepoAccess() {
    }

    /**
     * Endpoint URL currently used for spaces-repo queries. Resolved from
     * {@link Utils#getMainQueryUrl()} the first time the helper is touched.
     *
     * @return the spaces-repo SPARQL endpoint URL
     */
    public String getEndpointUrl() {
        ensureInitialized();
        return endpointUrl;
    }

    private void ensureInitialized() {
        if (repo == null) {
            synchronized (this) {
                if (repo == null) {
                    String envOverride = System.getenv("NANODASH_SPACES_REPO_URL");
                    String ep = envOverride != null && !envOverride.isBlank()
                            ? envOverride
                            : DEFAULT_SPACES_REPO_URL;
                    SPARQLRepository r = new SPARQLRepository(ep);
                    r.init();
                    endpointUrl = ep;
                    repo = r;
                    logger.info("Initialised spaces-repo SPARQL endpoint at {}{}",
                            ep, envOverride != null ? " (from NANODASH_SPACES_REPO_URL)" : " (default)");
                }
            }
        }
    }

    /**
     * Runs a SELECT query and maps each result row via the supplied mapper.
     *
     * @param sparql the SPARQL query body (must include any prefixes the query needs)
     * @param bindings parameter bindings to apply, or {@code null}/empty for none
     * @param mapper turns each {@link BindingSet} into an element of the result list;
     *               return {@code null} to skip a row
     * @return list of mapped rows in result order, never {@code null}
     * @param <T> row type
     */
    public <T> List<T> select(String sparql, Map<String, Value> bindings,
            Function<BindingSet, T> mapper) {
        ensureInitialized();
        List<T> out = new ArrayList<>();
        try (RepositoryConnection conn = repo.getConnection()) {
            TupleQuery query = conn.prepareTupleQuery(sparql);
            if (bindings != null) {
                for (Map.Entry<String, Value> e : bindings.entrySet()) {
                    query.setBinding(e.getKey(), e.getValue());
                }
            }
            try (TupleQueryResult result = query.evaluate()) {
                while (result.hasNext()) {
                    T mapped = mapper.apply(result.next());
                    if (mapped != null) out.add(mapped);
                }
            }
        } catch (Exception ex) {
            logger.error("Spaces-repo SPARQL failed at {}: {}", endpointUrl, ex.toString());
        }
        return out;
    }

}
