package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.utils.TestUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.knowledgepixels.nanodash.Utils.vf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UtilsTest {

    private final String ARTIFACT_CODE = "RAAnO3U0Lc56gbYHz5MZD440460c88Qfiz8cTfP58nvvs";
    private final String NANOPUB_IRI = "https://w3id.org/np/" + ARTIFACT_CODE;

    @Test
    void getShortNameFromURI() {
        IRI iri = vf.createIRI("http://knowledgepixels.com/resource#any12345");
        String shortName = "any12345";
        String shortNameRetrieved = Utils.getShortNameFromURI(iri);
        assertEquals(shortName, shortNameRetrieved);
    }

    @Test
    void getShortNameFromURIAsString() {
        String iriAsString = vf.createIRI("http://knowledgepixels.com/resource#any12345").stringValue();
        String shortName = "any12345";
        String shortNameRetrieved = Utils.getShortNameFromURI(iriAsString);
        assertEquals(shortName, shortNameRetrieved);
    }

    @Test
    void getShortNanopubId() {
        String shortNanopubId = Utils.getShortNanopubId(NANOPUB_IRI);
        String expectedShortNanopubId = "RAAnO3U0Lc";
        assertEquals(expectedShortNanopubId, shortNanopubId);
    }

    @Test
    void getArtifactCode() {
        String artifactCode = Utils.getArtifactCode(NANOPUB_IRI);
        assertEquals(ARTIFACT_CODE, artifactCode);
    }

    @Test
    void getShortOrcidId() {
        IRI orcidId = vf.createIRI("https://orcid.org/0000-0000-0000-0000");
        String shortOrcidId = Utils.getShortOrcidId(orcidId);
        String expectedShortOrcidId = "0000-0000-0000-0000";
        assertEquals(expectedShortOrcidId, shortOrcidId);
    }

    @Test
    void getUriPostfix() {
        String postfix = Utils.getUriPostfix(NANOPUB_IRI);
        assertEquals(ARTIFACT_CODE, postfix);
    }

    @Test
    void getUriPrefix() {
        String prefix = Utils.getUriPrefix(NANOPUB_IRI);
        String expectedPrefix = "https://w3id.org/np/";
        assertEquals(expectedPrefix, prefix);
    }

    @Test
    void isUriPostfix() {
        String postfix = Utils.getUriPostfix(NANOPUB_IRI);
        assertTrue(Utils.isUriPostfix(postfix));
        assertFalse(Utils.isUriPostfix("wrong:Postfix"));
    }

    @Test
    void sanitizeHtmlPolicyAllowsSafeHtmlElements() {
        String rawHtml = "<p>Paragraph</p><a href=\"https://knowledgepixels.com\">Link</a><img src=\"image.jpg\">";
        String sanitizedHtml = Utils.sanitizeHtml(rawHtml);
        assertEquals("<p>Paragraph</p><a href=\"https://knowledgepixels.com\" rel=\"nofollow\">Link</a><img src=\"image.jpg\" />", sanitizedHtml);
    }

    @Test
    void sanitizeHtmlPolicyRemovesUnsafeAttributes() {
        String rawHtml = "<img src=\"image.jpg\" onerror=\"alert('XSS')\">";
        String sanitizedHtml = Utils.sanitizeHtml(rawHtml);
        assertEquals("<img src=\"image.jpg\" />", sanitizedHtml);
    }

    @Test
    void sanitizeHtmlPolicyPreservesAllowedProtocols() {
        String rawHtml = "<a href=\"mailto:test@knowledgepixels.com\">Email</a>";
        String sanitizedHtml = Utils.sanitizeHtml(rawHtml);
        assertEquals("<a href=\"mailto:test&#64;knowledgepixels.com\" rel=\"nofollow\">Email</a>", sanitizedHtml);
    }

    @Test
    void testIsNanopubOfClass() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();
        IRI classIri = vf.createIRI("http://knowledgepixels.com/nanopubIri#any");
        boolean isNanopubOfClass = Utils.isNanopubOfClass(nanopub, classIri);
        assertTrue(isNanopubOfClass);
    }

    @Test
    void getTypesExcludesSpecificFairTerms() {
        Nanopub nanopub = mock(Nanopub.class);
        MockedStatic<NanopubUtils> mockStatic = mockStatic(NanopubUtils.class);
        IRI excludedType1 = vf.createIRI("https://w3id.org/fair/fip/terms/Available-FAIR-Enabling-Resource");
        IRI excludedType2 = vf.createIRI("https://w3id.org/fair/fip/terms/FAIR-Enabling-Resource-to-be-Developed");
        IRI includedType = vf.createIRI("http://knowledgepixels.com/nanopubIri#ValidType");
        mockStatic.when(() -> NanopubUtils.getTypes(nanopub)).thenReturn(Set.of(excludedType1, excludedType2, includedType));

        List<IRI> result = Utils.getTypes(nanopub);

        assertEquals(1, result.size());
        assertTrue(result.contains(includedType));
        mockStatic.close();
    }

    @Test
    void getTypesReturnsEmptyListForNoTypes() {
        Nanopub nanopub = mock(Nanopub.class);
        MockedStatic<NanopubUtils> mockStatic = mockStatic(NanopubUtils.class);

        mockStatic.when(() -> NanopubUtils.getTypes(nanopub)).thenReturn(Set.of());
        List<IRI> result = Utils.getTypes(nanopub);
        assertTrue(result.isEmpty());
        mockStatic.close();
    }

    @Test
    void getTypesHandlesNullNanopub() {
        assertThrows(NullPointerException.class, () -> Utils.getTypes(null));
    }

    @Test
    void getUriLabelReturnsEmptyStringForNullUri() {
        String result = Utils.getUriLabel(null);
        assertEquals("", result);
    }

    @Test
    void getUriLabelTruncatesLongUriWithTrustyUriPattern() {
        String url = "https://w3id.org/np/RA12345678abcdefghij1234567890123456789012345678901234567890";
        String expected = "https://w3id.org/np/RA12345678...123456789012345678901234567890";
        String result = Utils.getUriLabel(url);
        assertEquals(expected, result);
    }

    @Test
    void uriLabelWithTrustyUriPattern() {
        String url = "https://w3id.org/np/RAAnO3U0Lc56gbYHz5MZD440460c88Qfiz8cTfP58nvvs/extra";
        String result = Utils.getUriLabel(url);
        String expected = "https://w3id.org/np/RAAnO3U0Lc.../extra";
        assertEquals(expected, result);
    }

    @Test
    void getUriLabelTruncatesLongUriWithoutTrustyUriPattern() {
        String uri = "https://w3id.org/np/verylonguriwithmorethan70charactersandadditionaldata";
        String expected = "https://w3id.org/np/verylongur...n70charactersandadditionaldata";
        String result = Utils.getUriLabel(uri);
        assertEquals(expected, result);
    }

    @Test
    void getUriLabelReturnsUriAsIsForShortUri() {
        String uri = "https://w3id.org/np/short";
        String result = Utils.getUriLabel(uri);
        assertEquals(uri, result);
    }

    @Test
    void getUriLabelHandlesUriWithTrustyUriPatternButShortLength() {
        String uri = "https://w3id.org/np/RA12345678abcdefghij123456789012345678901234567890";
        String result = Utils.getUriLabel(uri);
        assertEquals(uri, result);
    }

    @Test
    void getTypeLabelReturnsShortLabelForKnownFairEnablingResource() {
        IRI typeIri = vf.createIRI("https://w3id.org/fair/fip/terms/FAIR-Enabling-Resource");
        String result = Utils.getTypeLabel(typeIri);
        assertEquals("FER", result);
    }

    @Test
    void getTypeLabelReturnsShortLabelForKnownFairSupportingResource() {
        IRI typeIri = vf.createIRI("https://w3id.org/fair/fip/terms/FAIR-Supporting-Resource");
        String result = Utils.getTypeLabel(typeIri);
        assertEquals("FSR", result);
    }

    @Test
    void getTypeLabelReturnsShortLabelForKnownFairImplementationProfile() {
        IRI typeIri = vf.createIRI("https://w3id.org/fair/fip/terms/FAIR-Implementation-Profile");
        String result = Utils.getTypeLabel(typeIri);
        assertEquals("FIP", result);
    }

    @Test
    void getTypeLabelReturnsShortLabelForDeclaredBy() {
        IRI typeIri = vf.createIRI("http://purl.org/nanopub/x/declaredBy");
        String result = Utils.getTypeLabel(typeIri);
        assertEquals("user intro", result);
    }

    @Test
    void getTypeLabelTruncatesLongTypeName() {
        IRI typeIri = vf.createIRI("http://w3id.org/veryLongTypeNameThatExceedsTwentyFiveCharacters");
        String result = Utils.getTypeLabel(typeIri);
        assertEquals("veryLongTypeNameThat...", result);
    }

    @Test
    void getTypeLabelRemovesNanopubSuffixFromTypeName() {
        IRI typeIri = vf.createIRI("http://w3id.org/ExampleNanopub");
        String result = Utils.getTypeLabel(typeIri);
        assertEquals("Example", result);
    }

    @Test
    void getTypeLabelReturnsLastSegmentOfUri() {
        IRI typeIri = vf.createIRI("http://w3id.org/SomeType");
        String result = Utils.getTypeLabel(typeIri);
        assertEquals("SomeType", result);
    }

    @Test
    void getPageParametersAsStringEncodesSingleParameter() {
        PageParameters params = new PageParameters();
        params.add("key", "value");
        String result = Utils.getPageParametersAsString(params);
        assertEquals("key=value", result);
    }

    @Test
    void getPageParametersAsStringEncodesMultipleParameters() {
        PageParameters params = new PageParameters();
        params.add("key1", "value1");
        params.add("key2", "value2");
        String result = Utils.getPageParametersAsString(params);
        assertEquals("key1=value1&key2=value2", result);
    }

    @Test
    void getPageParametersAsStringHandlesEmptyParameters() {
        PageParameters params = new PageParameters();
        String result = Utils.getPageParametersAsString(params);
        assertEquals("", result);
    }

    @Test
    void getPageParametersAsStringEncodesSpecialCharacters() {
        PageParameters params = new PageParameters();
        params.add("key", "value with spaces & special=chars");
        String result = Utils.getPageParametersAsString(params);
        assertEquals("key=value+with+spaces+%26+special%3Dchars", result);
    }

    @Test
    void usesPredicateInAssertionReturnsTrueWhenPredicateExists() {
        Nanopub nanopub = mock(Nanopub.class);
        IRI predicateIri = vf.createIRI("http://knowledgepixels.com/resource#anyPredicate");
        Statement statement = mock(Statement.class);

        when(statement.getPredicate()).thenReturn(predicateIri);
        when(nanopub.getAssertion()).thenReturn(Set.of(statement));

        boolean result = Utils.usesPredicateInAssertion(nanopub, predicateIri);

        assertTrue(result);
    }

    @Test
    void usesPredicateInAssertionReturnsFalseWhenPredicateDoesNotExist() {
        Nanopub nanopub = mock(Nanopub.class);
        IRI predicateIri = vf.createIRI("http://knowledgepixels.com/resource#anyPredicate");
        IRI otherPredicateIri = vf.createIRI("http://knowledgepixels.com/resource#anotherPredicate");
        Statement statement = mock(Statement.class);

        when(statement.getPredicate()).thenReturn(otherPredicateIri);
        when(nanopub.getAssertion()).thenReturn(Set.of(statement));

        boolean result = Utils.usesPredicateInAssertion(nanopub, predicateIri);

        assertFalse(result);
    }

    @Test
    void createSha256HexHashReturnsCorrectHashForValidString() {
        String input = "testString";
        String expectedHash = "4acf0b39d9c4766709a3689f553ac01ab550545ffa4544dfc0b2cea82fba02a3";
        String result = Utils.createSha256HexHash(input);
        assertEquals(expectedHash, result);
    }

    @Test
    void getFoafNameMapReturnsCorrectMappingForValidStatements() {
        Nanopub nanopub = mock(Nanopub.class);
        Statement statement1 = mock(Statement.class);
        Statement statement2 = mock(Statement.class);
        IRI subject1 = vf.createIRI("http://knowledgepixels.com/subject1");
        IRI subject2 = vf.createIRI("http://knowledgepixels.com/subject2");
        Literal name1 = vf.createLiteral("Name1");
        Literal name2 = vf.createLiteral("Name2");

        when(statement1.getPredicate()).thenReturn(FOAF.NAME);
        when(statement1.getSubject()).thenReturn(subject1);
        when(statement1.getObject()).thenReturn(name1);
        when(statement2.getPredicate()).thenReturn(FOAF.NAME);
        when(statement2.getSubject()).thenReturn(subject2);
        when(statement2.getObject()).thenReturn(name2);
        when(nanopub.getPubinfo()).thenReturn(Set.of(statement1, statement2));

        Map<String, String> result = Utils.getFoafNameMap(nanopub);

        assertEquals(2, result.size());
        assertEquals("Name1", result.get(subject1.stringValue()));
        assertEquals("Name2", result.get(subject2.stringValue()));
    }

    @Test
    void getFoafNameMapReturnsEmptyMapForNoMatchingStatements() {
        Nanopub nanopub = mock(Nanopub.class);
        Statement statement = mock(Statement.class);

        when(statement.getPredicate()).thenReturn(vf.createIRI("http://knowledgepixels.com/otherPredicate"));
        when(nanopub.getPubinfo()).thenReturn(Set.of(statement));

        Map<String, String> result = Utils.getFoafNameMap(nanopub);

        assertTrue(result.isEmpty());
    }

    @Test
    void getFoafNameMapIgnoresNonLiteralObjects() {
        Nanopub nanopub = mock(Nanopub.class);
        Statement statement = mock(Statement.class);
        IRI subject = vf.createIRI("http://knowledgepixels.com/subject");
        IRI nonLiteralObject = vf.createIRI("http://knowledgepixels.com/nonLiteralObject");

        when(statement.getPredicate()).thenReturn(FOAF.NAME);
        when(statement.getSubject()).thenReturn(subject);
        when(statement.getObject()).thenReturn(nonLiteralObject);
        when(nanopub.getPubinfo()).thenReturn(Set.of(statement));

        Map<String, String> result = Utils.getFoafNameMap(nanopub);

        assertTrue(result.isEmpty());
    }

    @Test
    void urlEncodeHandlesNullInput() {
        String result = Utils.urlEncode(null);
        assertEquals("", result);
    }

    @Test
    void urlEncodeEncodesSpecialCharacters() {
        String input = "value with spaces & special=chars";
        String result = Utils.urlEncode(input);
        assertEquals("value+with+spaces+%26+special%3Dchars", result);
    }

    @Test
    void urlEncodeEncodesEmptyString() {
        String result = Utils.urlEncode("");
        assertEquals("", result);
    }

    @Test
    void urlDecodeHandlesNullInput() {
        String result = Utils.urlDecode(null);
        assertEquals("", result);
    }

    @Test
    void urlDecodeDecodesEncodedString() {
        String input = "value%20with%20spaces%20%26%20special%3Dchars";
        String result = Utils.urlDecode(input);
        assertEquals("value with spaces & special=chars", result);
    }

    @Test
    void urlDecodeHandlesEmptyString() {
        String result = Utils.urlDecode("");
        assertEquals("", result);
    }

    @Test
    void getUrlWithParametersReturnsUrlWithSingleParameter() {
        PageParameters params = new PageParameters();
        params.add("key", "value");
        String result = Utils.getUrlWithParameters("https://knowledgepixels.com", params);
        assertEquals("https://knowledgepixels.com?key=value", result);
    }

    @Test
    void getUrlWithParametersReturnsUrlWithMultipleParameters() {
        PageParameters params = new PageParameters();
        params.add("key1", "value1");
        params.add("key2", "value2");
        String result = Utils.getUrlWithParameters("https://knowledgepixels.com", params);
        assertEquals("https://knowledgepixels.com?key1=value1&key2=value2", result);
    }

    @Test
    void getUrlWithParametersHandlesEmptyParameters() {
        PageParameters params = new PageParameters();
        String result = Utils.getUrlWithParameters("https://knowledgepixels.com", params);
        assertEquals("https://knowledgepixels.com", result);
    }

    @Test
    void getUrlWithParametersEncodesSpecialCharacters() {
        PageParameters params = new PageParameters();
        params.add("key", "value with spaces & special=chars");
        String result = Utils.getUrlWithParameters("https://knowledgepixels.com", params);
        assertEquals("https://knowledgepixels.com?key=value+with+spaces+%26+special%3Dchars", result);
    }

    @Test
    void getUrlWithParametersHandlesInvalidBaseUrl() {
        PageParameters params = new PageParameters();
        params.add("key", "value");
        String result = Utils.getUrlWithParameters("https://knowledgepixels.com?key=value+ space", params);
        assertEquals("/", result);
    }

    @Test
    void getShortPubkeyNameReturnsShortenedKeyForValidInput() {
        String pubkey = "A1234567890123456789012345678901234567890B12345";
        String result = Utils.getShortPubkeyName(pubkey);
        assertEquals("A..0B123..", result);
    }

    @Test
    void getShortPubkeyNameHandlesEmptyString() {
        String pubkey = "";
        String result = Utils.getShortPubkeyName(pubkey);
        assertEquals("", result);
    }

}