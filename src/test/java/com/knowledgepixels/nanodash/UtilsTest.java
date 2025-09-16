package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.utils.TestUtils;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.*;
import org.nanopub.vocabulary.FIP;
import org.nanopub.vocabulary.NPX;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UtilsTest {

    private final String ARTIFACT_CODE = "RAAnO3U0Lc56gbYHz5MZD440460c88Qfiz8cTfP58nvvs";
    private final String BASE_URI = "https://w3id.org/np/";
    private final String NANOPUB_IRI = BASE_URI + ARTIFACT_CODE;

    @Test
    void getShortNameFromURI() {
        IRI iri = Values.iri("http://knowledgepixels.com/resource#any12345");
        String shortName = "any12345";
        String shortNameRetrieved = Utils.getShortNameFromURI(iri);
        assertEquals(shortName, shortNameRetrieved);
    }

    @Test
    void getShortNameFromURIAsString() {
        String iriAsString = Values.iri("http://knowledgepixels.com/resource#any12345").stringValue();
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
        IRI orcidId = Values.iri("https://orcid.org/0000-0000-0000-0000");
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
    void getUriPostfixWithHash() {
        String uri = BASE_URI + "example#" + ARTIFACT_CODE;
        String postfix = Utils.getUriPostfix(uri);
        assertEquals(ARTIFACT_CODE, postfix);
    }

    @Test
    void getUriPrefix() {
        String prefix = Utils.getUriPrefix(NANOPUB_IRI);
        assertEquals(BASE_URI, prefix);
    }

    @Test
    void getUriPrefixWithHash() {
        String uri = BASE_URI + "example#" + ARTIFACT_CODE;
        String prefix = Utils.getUriPrefix(uri);
        assertEquals(BASE_URI + "example#", prefix);
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
    void isNanopubOfClass() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        IRI classIri = Values.iri("http://knowledgepixels.com/nanopubIri#any");
        boolean isNanopubOfClass = Utils.isNanopubOfClass(nanopub, classIri);
        assertTrue(isNanopubOfClass);
    }

    @Test
    void getTypesExcludesSpecificFairTerms() {
        Nanopub nanopub = mock(Nanopub.class);
        MockedStatic<NanopubUtils> mockStatic = mockStatic(NanopubUtils.class);
        IRI excludedType1 = FIP.AVAILABLE_FAIR_ENABLING_RESOURCE;
        IRI excludedType2 = FIP.FAIR_ENABLING_RESOURCE_TO_BE_DEVELOPED;
        IRI includedType = Values.iri("http://knowledgepixels.com/nanopubIri#ValidType");
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
        String url = BASE_URI + "RA12345678abcdefghij1234567890123456789012345678901234567890";
        String expected = BASE_URI + "RA12345678...123456789012345678901234567890";
        String result = Utils.getUriLabel(url);
        assertEquals(expected, result);
    }

    @Test
    void getUriLabelWithTrustyUriPattern() {
        String url = BASE_URI + "RAAnO3U0Lc56gbYHz5MZD440460c88Qfiz8cTfP58nvvs/extra";
        String result = Utils.getUriLabel(url);
        String expected = BASE_URI + "RAAnO3U0Lc.../extra";
        assertEquals(expected, result);
    }

    @Test
    void getUriLabelWithShortTrustyUriPattern() {
        String url = BASE_URI + "RAAnO3U0Lc";
        String result = Utils.getUriLabel(url);
        String expected = BASE_URI + "RAAnO3U0Lc";
        assertEquals(expected, result);
    }

    @Test
    void getUriLabelTruncatesLongUriWithoutTrustyUriPattern() {
        String uri = BASE_URI + "verylonguriwithmorethan70charactersandadditionaldata";
        String expected = BASE_URI + "verylongur...n70charactersandadditionaldata";
        String result = Utils.getUriLabel(uri);
        assertEquals(expected, result);
    }

    @Test
    void getUriLabelReturnsUriAsIsForShortUri() {
        String uri = BASE_URI + "short";
        String result = Utils.getUriLabel(uri);
        assertEquals(uri, result);
    }

    @Test
    void getUriLabelHandlesUriWithTrustyUriPatternButShortLength() {
        String uri = BASE_URI + "RA12345678abcdefghij123456789012345678901234567890";
        String result = Utils.getUriLabel(uri);
        assertEquals(uri, result);
    }

    @Test
    void getTypeLabelReturnsShortLabelForKnownFairEnablingResource() {
        String result = Utils.getTypeLabel(FIP.FAIR_ENABLING_RESOURCE);
        assertEquals("FER", result);
    }

    @Test
    void getTypeLabelReturnsShortLabelForKnownFairSupportingResource() {
        String result = Utils.getTypeLabel(FIP.FAIR_SUPPORTING_RESOURCE);
        assertEquals("FSR", result);
    }

    @Test
    void getTypeLabelReturnsShortLabelForKnownFairImplementationProfile() {
        String result = Utils.getTypeLabel(FIP.FAIR_IMPLEMENTATION_PROFILE);
        assertEquals("FIP", result);
    }

    @Test
    void getTypeLabelReturnsShortLabelForDeclaredBy() {
        String result = Utils.getTypeLabel(NPX.DECLARED_BY);
        assertEquals("user intro", result);
    }

    @Test
    void getTypeLabelTruncatesLongTypeName() {
        IRI typeIri = Values.iri("http://w3id.org/veryLongTypeNameThatExceedsTwentyFiveCharacters");
        String result = Utils.getTypeLabel(typeIri);
        assertEquals("veryLongTypeNameThat...", result);
    }

    @Test
    void getTypeLabelRemovesNanopubSuffixFromTypeName() {
        IRI typeIri = Values.iri("http://w3id.org/ExampleNanopub");
        String result = Utils.getTypeLabel(typeIri);
        assertEquals("Example", result);
    }

    @Test
    void getTypeLabelReturnsLastSegmentOfUri() {
        IRI typeIri = Values.iri("http://w3id.org/SomeType");
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
        IRI predicateIri = Values.iri("http://knowledgepixels.com/resource#anyPredicate");
        Statement statement = mock(Statement.class);

        when(statement.getPredicate()).thenReturn(predicateIri);
        when(nanopub.getAssertion()).thenReturn(Set.of(statement));

        boolean result = Utils.usesPredicateInAssertion(nanopub, predicateIri);

        assertTrue(result);
    }

    @Test
    void usesPredicateInAssertionReturnsFalseWhenPredicateDoesNotExist() {
        Nanopub nanopub = mock(Nanopub.class);
        IRI predicateIri = Values.iri("http://knowledgepixels.com/resource#anyPredicate");
        IRI otherPredicateIri = Values.iri("http://knowledgepixels.com/resource#anotherPredicate");
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
        IRI subject1 = Values.iri("http://knowledgepixels.com/subject1");
        IRI subject2 = Values.iri("http://knowledgepixels.com/subject2");
        Literal name1 = literal("Name1");
        Literal name2 = literal("Name2");

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

        when(statement.getPredicate()).thenReturn(iri("http://knowledgepixels.com/otherPredicate"));
        when(nanopub.getPubinfo()).thenReturn(Set.of(statement));

        Map<String, String> result = Utils.getFoafNameMap(nanopub);

        assertTrue(result.isEmpty());
    }

    @Test
    void getFoafNameMapIgnoresNonLiteralObjects() {
        Nanopub nanopub = mock(Nanopub.class);
        Statement statement = mock(Statement.class);
        IRI subject = Values.iri("http://knowledgepixels.com/subject");
        IRI nonLiteralObject = Values.iri("http://knowledgepixels.com/nonLiteralObject");

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

    @Test
    void getIntroducedIriIds() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator npCreator = TestUtils.getNanopubCreator();
        npCreator.addAssertionStatement(TestUtils.anyIri, TestUtils.anyIri, TestUtils.anyIri);
        npCreator.addProvenanceStatement(npCreator.getAssertionUri(), TestUtils.anyIri, TestUtils.anyIri);
        npCreator.addPubinfoStatement(NPX.INTRODUCES, TestUtils.anyIri);

        IRI randomIri1 = TestUtils.randomIri();
        npCreator.addPubinfoStatement(NPX.INTRODUCES, randomIri1);

        npCreator.addPubinfoStatement(TestUtils.randomIri(), TestUtils.randomIri());
        npCreator.addPubinfoStatement(NPX.INTRODUCES, literal("not an IRI"));
        npCreator.addPubinfoStatement(TestUtils.randomIri(), NPX.INTRODUCES, TestUtils.randomIri());

        Nanopub nanopub = npCreator.finalizeNanopub();

        Set<String> expectedIntroducedIriIds = Set.of(
                TestUtils.anyIri.stringValue(),
                randomIri1.stringValue()
        );

        Set<String> actualIriIds = Utils.getIntroducedIriIds(nanopub);
        assertEquals(expectedIntroducedIriIds, actualIriIds);
    }

    @Test
    void getEmbeddedIriIds() throws NanopubAlreadyFinalizedException, MalformedNanopubException {
        NanopubCreator npCreator = TestUtils.getNanopubCreator();
        npCreator.addAssertionStatement(TestUtils.anyIri, TestUtils.anyIri, TestUtils.anyIri);
        npCreator.addProvenanceStatement(npCreator.getAssertionUri(), TestUtils.anyIri, TestUtils.anyIri);
        npCreator.addPubinfoStatement(NPX.EMBEDS, TestUtils.anyIri);

        IRI randomIri1 = TestUtils.randomIri();
        npCreator.addPubinfoStatement(NPX.EMBEDS, randomIri1);

        npCreator.addPubinfoStatement(TestUtils.randomIri(), TestUtils.randomIri());
        npCreator.addPubinfoStatement(NPX.EMBEDS, literal("not an IRI"));
        npCreator.addPubinfoStatement(TestUtils.randomIri(), NPX.EMBEDS, TestUtils.randomIri());

        Nanopub nanopub = npCreator.finalizeNanopub();

        Set<String> expectedEmbeddedIris = Set.of(
                TestUtils.anyIri.stringValue(),
                randomIri1.stringValue()
        );

        Set<String> result = Utils.getEmbeddedIriIds(nanopub);

        assertEquals(expectedEmbeddedIris, result);
    }

    @Test
    void isValidLiteralSerializationReturnsTrueForPlainLiteral() {
        String literal = "\"plain literal\"";
        assertTrue(Utils.isValidLiteralSerialization(literal));
    }

    @Test
    void isValidLiteralSerializationReturnsTrueForLangTaggedLiteral() {
        String literal = "\"literal with lang tag\"@en";
        assertTrue(Utils.isValidLiteralSerialization(literal));
    }

    @Test
    void isValidLiteralSerializationReturnsTrueForDatatypeLiteral() {
        String literal = "\"literal with datatype\"^^<http://www.w3.org/2001/XMLSchema#string>";
        assertTrue(Utils.isValidLiteralSerialization(literal));
    }

    @Test
    void isValidLiteralSerializationReturnsFalseForInvalidPlainLiteral() {
        String literal = "plain literal without quotes";
        assertFalse(Utils.isValidLiteralSerialization(literal));
    }

    @Test
    void isValidLiteralSerializationReturnsFalseForInvalidLangTaggedLiteral() {
        String literal = "\"literal with invalid lang tag\"@e";
        assertFalse(Utils.isValidLiteralSerialization(literal));
    }

    @Test
    void isValidLiteralSerializationReturnsFalseForEmptyString() {
        String literal = "";
        assertFalse(Utils.isValidLiteralSerialization(literal));
    }

    @Test
    void isValidLiteralSerializationReturnsFalseForNullInput() {
        assertThrows(NullPointerException.class, () -> Utils.isValidLiteralSerialization(null));
    }

    @Test
    void getUnescapedLiteralStringReturnsUnescapedStringForEscapedQuotes() {
        String escapedString = "\\\"escaped\\\"";
        String result = Utils.getUnescapedLiteralString(escapedString);
        assertEquals("\"escaped\"", result);
    }

    @Test
    void getUnescapedLiteralStringReturnsUnescapedStringForEscapedBackslashes() {
        String escapedString = "\\\\escaped\\\\";
        String result = Utils.getUnescapedLiteralString(escapedString);
        assertEquals("\\escaped\\", result);
    }

    @Test
    void getUnescapedLiteralStringHandlesMixedEscapedCharacters() {
        String escapedString = "\\\"mixed\\\\escaped\\\"";
        String result = Utils.getUnescapedLiteralString(escapedString);
        assertEquals("\"mixed\\escaped\"", result);
    }

    @Test
    void getUnescapedLiteralStringReturnsSameStringForNoEscapedCharacters() {
        String escapedString = "noEscapedCharacters";
        String result = Utils.getUnescapedLiteralString(escapedString);
        assertEquals("noEscapedCharacters", result);
    }

    @Test
    void getUnescapedLiteralStringHandlesEmptyString() {
        String escapedString = "";
        String result = Utils.getUnescapedLiteralString(escapedString);
        assertEquals("", result);
    }

    @Test
    void getUnescapedLiteralStringHandlesNullInput() {
        assertThrows(NullPointerException.class, () -> Utils.getUnescapedLiteralString(null));
    }

    @Test
    void getEscapedLiteralStringEscapesQuotesCorrectly() {
        String input = "This is a \"quote\"";
        String result = Utils.getEscapedLiteralString(Utils.getUnescapedLiteralString(input));
        assertEquals("This is a \"quote\"", result);
    }

    @Test
    void getEscapedLiteralStringEscapesBackslashesCorrectly() {
        String input = "This is a backslash \\";
        String result = Utils.getEscapedLiteralString(Utils.getUnescapedLiteralString(input));
        assertEquals("This is a backslash \\\\", result);
    }

    @Test
    void getEscapedLiteralStringReturnsSameStringForNoSpecialCharacters() {
        String input = "No special characters";
        String result = Utils.getEscapedLiteralString(input);
        assertEquals("No special characters", result);
    }

    @Test
    void getEscapedLiteralStringHandlesEmptyString() {
        String input = "";
        String result = Utils.getEscapedLiteralString(input);
        assertEquals("", result);
    }

    @Test
    void getEscapedLiteralStringHandlesNullInput() {
        assertThrows(NullPointerException.class, () -> Utils.getEscapedLiteralString(null));
    }

    @Test
    void getParsedLiteralReturnsPlainLiteral() {
        String serializedLiteral = "\"plain literal\"";
        Literal result = Utils.getParsedLiteral(serializedLiteral);
        assertEquals("plain literal", result.stringValue());
        assertFalse(result.getLanguage().isPresent());
        assertEquals(XSD.STRING, result.getDatatype());
    }

    @Test
    void getParsedLiteralReturnsLangTaggedLiteral() {
        String serializedLiteral = "\"literal with lang tag\"@en";
        Literal result = Utils.getParsedLiteral(serializedLiteral);
        assertEquals("literal with lang tag", result.stringValue());
        assertTrue(result.getLanguage().isPresent());
        assertEquals("en", result.getLanguage().get());
    }

    @Test
    void getParsedLiteralReturnsDatatypeLiteral() {
        String serializedLiteral = "\"literal with datatype\"^^<http://www.w3.org/2001/XMLSchema#string>";
        Literal result = Utils.getParsedLiteral(serializedLiteral);
        assertEquals("literal with datatype", result.stringValue());
        assertEquals(Values.iri("http://www.w3.org/2001/XMLSchema#string"), result.getDatatype());
    }

    @Test
    void getParsedLiteralThrowsExceptionForInvalidPlainLiteral() {
        String serializedLiteral = "plain literal without quotes";
        assertThrows(IllegalArgumentException.class, () -> Utils.getParsedLiteral(serializedLiteral));
    }

    @Test
    void getSerializedLiteralReturnsCorrectSerializationForPlainLiteral() {
        Literal literal = Values.literal("plain literal");
        String result = Utils.getSerializedLiteral(literal);
        assertEquals("\"plain literal\"", result);
    }

    @Test
    void getSerializedLiteralReturnsCorrectSerializationForLangTaggedLiteral() {
        Literal literal = Values.literal("literal with lang tag", "en");
        String result = Utils.getSerializedLiteral(literal);
        assertEquals("\"literal with lang tag\"@en", result);
    }

    @Test
    void getSerializedLiteralReturnsCorrectSerializationForDatatypeLiteral() {
        Literal literal = Values.literal("literal with datatype", XSD.STRING);
        String result = Utils.getSerializedLiteral(literal);
        assertEquals("\"literal with datatype\"", result);
    }

    @Test
    void getSerializedLiteralReturnsCorrectSerializationForCustomDatatypeLiteral() {
        IRI customDatatype = Values.iri("http://example.org/customDatatype");
        Literal literal = Values.literal("custom datatype literal", customDatatype);
        String result = Utils.getSerializedLiteral(literal);
        assertEquals("\"custom datatype literal\"^^<http://example.org/customDatatype>", result);
    }

    @Test
    void getSerializedLiteralHandlesEmptyLiteralString() {
        Literal literal = Values.literal("");
        String result = Utils.getSerializedLiteral(literal);
        assertEquals("\"\"", result);
    }

    @Test
    void getUriLinkWithLocalIri() {
        WicketTester tester = new WicketTester();
        String markupId = "uri";
        String uri = LocalUri.of("label").stringValue();
        ExternalLink el = Utils.getUriLink(markupId, uri);
        assertEquals(markupId, el.getId());
        assertEquals("", el.getDefaultModel().getObject());
        assertEquals(Utils.getUriLabel(uri), el.getBody().getObject());
    }

    @Test
    void getUriLinkWithoutLocalIri() {
        WicketTester tester = new WicketTester();
        String markupId = "uri";
        String uri = NANOPUB_IRI;
        ExternalLink el = Utils.getUriLink(markupId, uri);
        assertEquals(markupId, el.getId());
        assertEquals(uri, el.getDefaultModel().getObject());
        assertEquals(Utils.getUriLabel(uri), el.getBody().getObject());
    }

}