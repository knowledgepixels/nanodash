package com.knowledgepixels.nanodash.lookup;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Picks the {@link LookupApi} for an API URL given by {@code nt:possibleValuesFromApi}. The
 * lookup APIs are registered on the URLs they handle, mostly by URL prefix, and the first
 * registration that matches wins. A URL that none matches is handled by the
 * {@link CanonicalParser}.
 */
public final class LookupApiRegistry {

    private static final LookupApi FALLBACK = new CanonicalParser();

    private static final List<Registration> REGISTRATIONS = new ArrayList<>();

    static {
        register(startsWithAny(NanopubQueryLookup.QUERY_API_PREFIX, NanopubQueryLookup.LEGACY_FIND_THINGS_PREFIX), new NanopubQueryLookup());
        register(startsWithAny("https://w3id.org/np/l/nanopub-query", "https://grlc.").or(url -> url.contains("/sparql?")), new SparqlResultsParser());
        register(startsWithAny("https://www.ebi.ac.uk/ols/api/select"), new OlsParser());
        register(startsWithAny("https://api.openaire.eu/graph/v"), new OpenAireParser());
        register(startsWithAny("https://api.gbif.org/v1/species/suggest"), new GbifParser());
        register(startsWithAny("https://api.catalogueoflife.org/dataset/3LR/nameusage/search"), new CatalogueOfLifeParser());
        register(startsWithAny("https://vodex."), new VodexParser());
        register(startsWithAny("https://api.ror.org/organizations"), new RorParser());
        register(startsWithAny("https://name-resolution-sri.renci.org/"), new NameResolutionParser());
    }

    private LookupApiRegistry() {
    }

    /**
     * Returns the lookup API that handles the given API URL.
     *
     * @param apiUrl the API URL as given in the template
     * @return the registered lookup API for the URL, or the canonical parser if none is registered
     */
    public static LookupApi forUrl(String apiUrl) {
        for (Registration registration : REGISTRATIONS) {
            if (registration.handles().test(apiUrl)) return registration.lookupApi();
        }
        return FALLBACK;
    }

    private static void register(Predicate<String> handles, LookupApi lookupApi) {
        REGISTRATIONS.add(new Registration(handles, lookupApi));
    }

    private static Predicate<String> startsWithAny(String... prefixes) {
        return url -> {
            for (String prefix : prefixes) {
                if (url.startsWith(prefix)) return true;
            }
            return false;
        };
    }

    /**
     * A lookup API together with the URLs it handles.
     *
     * @param handles   tells whether an API URL is handled by the lookup API
     * @param lookupApi the lookup API
     */
    private record Registration(Predicate<String> handles, LookupApi lookupApi) {
    }

}
