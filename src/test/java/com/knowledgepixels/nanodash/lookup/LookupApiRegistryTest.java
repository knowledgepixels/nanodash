package com.knowledgepixels.nanodash.lookup;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Each API URL is handled by the lookup API registered for it, in the order of precedence the
 * registrations are made in, and any other URL by the canonical parser.
 */
class LookupApiRegistryTest {

    @ParameterizedTest
    @CsvSource({
            "https://w3id.org/np/l/nanopub-query-1.1/api/RAyMrQ89RECTi9gZK5q7gjL1wKTiP8StkLy0NIkkCiyew/find-things?type=x, NanopubQueryLookup",
            "http://purl.org/nanopub/api/find_signed_things?type=x, NanopubQueryLookup",
            "https://w3id.org/np/l/nanopub-query-1.0/api/RAxyz/find-things?q=, SparqlResultsParser",
            "https://grlc.knowledgepixels.com/api-git/knowledgepixels/fairconnect-api/fer_search?query=, SparqlResultsParser",
            "https://vocab.nerc.ac.uk/sparql/sparql?query=, SparqlResultsParser",
            "https://www.ebi.ac.uk/ols/api/select?ontology=bto&q=, OlsParser",
            "https://api.openaire.eu/graph/v3/projects?search=, OpenAireParser",
            "https://api.gbif.org/v1/species/suggest?q=, GbifParser",
            "https://api.catalogueoflife.org/dataset/3LR/nameusage/search?q=, CatalogueOfLifeParser",
            "https://vodex.petapico.org/ncbitaxon/query?q=label:, VodexParser",
            "https://api.ror.org/organizations?query=, RorParser",
            "https://name-resolution-sri.renci.org/lookup?limit=10&string=, NameResolutionParser",
            "https://www.wikidata.org/w/api.php?action=wbsearchentities&language=en&format=json&search=, CanonicalParser",
            "http://data.bioontology.org/search?ontologies=DOID&q=, CanonicalParser",
            "https://example.github.io/my-ontology/terms.json?q=, CanonicalParser"
    })
    void eachUrlIsHandledByItsLookupApi(String apiUrl, String expectedLookupApi) {
        assertEquals(expectedLookupApi, LookupApiRegistry.forUrl(apiUrl).getClass().getSimpleName());
    }

}
