package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.utils.TestUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;

import java.util.List;
import java.util.Set;

import static com.knowledgepixels.nanodash.Utils.vf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class UtilsTest {

    private final String ARTIFACT_CODE = "RAAnO3U0Lc56gbYHz5MZD440460c88Qfiz8cTfP58nvvs";
    private final String NANOPUB_IRI = "https://w3id.org/np/" + ARTIFACT_CODE;

    @Test
    void getShortName_fromURI() {
        IRI iri = vf.createIRI("http://knowledgepixels.com/resource#any12345");
        String shortName = "any12345";
        String shortNameRetrieved = Utils.getShortNameFromURI(iri);
        assertEquals(shortNameRetrieved, shortName);
    }

    @Test
    void getShortName_fromURIAsString() {
        String iriAsString = vf.createIRI("http://knowledgepixels.com/resource#any12345").stringValue();
        String shortName = "any12345";
        String shortNameRetrieved = Utils.getShortNameFromURI(iriAsString);
        assertEquals(shortNameRetrieved, shortName);
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
    void sanitizeHtmlPolicy_allowsSafeHtmlElements() {
        String rawHtml = "<p>Paragraph</p><a href=\"https://example.com\">Link</a><img src=\"image.jpg\">";
        String sanitizedHtml = Utils.sanitizeHtml(rawHtml);
        assertEquals("<p>Paragraph</p><a href=\"https://example.com\" rel=\"nofollow\">Link</a><img src=\"image.jpg\" />", sanitizedHtml);
    }

    @Test
    void sanitizeHtmlPolicy_removesUnsafeAttributes() {
        String rawHtml = "<img src=\"image.jpg\" onerror=\"alert('XSS')\">";
        String sanitizedHtml = Utils.sanitizeHtml(rawHtml);
        assertEquals("<img src=\"image.jpg\" />", sanitizedHtml);
    }

    @Test
    void sanitizeHtmlPolicy_preservesAllowedProtocols() {
        String rawHtml = "<a href=\"mailto:test@example.com\">Email</a>";
        String sanitizedHtml = Utils.sanitizeHtml(rawHtml);
        assertEquals("<a href=\"mailto:test&#64;example.com\" rel=\"nofollow\">Email</a>", sanitizedHtml);
    }

    @Test
    void testIsNanopubOfClass() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();
        IRI classIri = vf.createIRI("http://knowledgepixels.com/nanopubIri#any");
        boolean isNanopubOfClass = Utils.isNanopubOfClass(nanopub, classIri);
        assertTrue(isNanopubOfClass);
    }

    @Test
    void getTypes_excludesSpecificFairTerms() {
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
    void getTypes_returnsEmptyListForNoTypes() {
        Nanopub nanopub = mock(Nanopub.class);
        MockedStatic<NanopubUtils> mockStatic = mockStatic(NanopubUtils.class);

        mockStatic.when(() -> NanopubUtils.getTypes(nanopub)).thenReturn(Set.of());
        List<IRI> result = Utils.getTypes(nanopub);
        assertTrue(result.isEmpty());
        mockStatic.close();
    }

    @Test
    void getTypes_handlesNullNanopub() {
        assertThrows(NullPointerException.class, () -> Utils.getTypes(null));
    }

    @Test
    void getUriLabel_returnsEmptyStringForNullUri() {
        String result = Utils.getUriLabel(null);
        assertEquals("", result);
    }

    @Test
    void getUriLabel_truncatesLongUriWithTrustyUriPattern() {
        String url = "https://w3id.org/np/RA12345678abcdefghij1234567890123456789012345678901234567890";
        String expected = "https://w3id.org/np/RA12345678...123456789012345678901234567890";
        String result = Utils.getUriLabel(url);
        assertEquals(expected, result);
    }

    @Test
    void getUriLabel_truncatesLongUriWithoutTrustyUriPattern() {
        String uri = "https://w3id.org/np/verylonguriwithmorethan70charactersandadditionaldata";
        String expected = "https://w3id.org/np/verylongur...n70charactersandadditionaldata";
        String result = Utils.getUriLabel(uri);
        assertEquals(expected, result);
    }

    @Test
    void getUriLabel_returnsUriAsIsForShortUri() {
        String uri = "https://w3id.org/np/short";
        String result = Utils.getUriLabel(uri);
        assertEquals(uri, result);
    }

    @Test
    void getUriLabel_handlesUriWithTrustyUriPatternButShortLength() {
        String uri = "https://w3id.org/np/RA12345678abcdefghij123456789012345678901234567890";
        String result = Utils.getUriLabel(uri);
        assertEquals(uri, result);
    }

    @Test
    void getTypeLabel_returnsShortLabelForKnownFairEnablingResource() {
        IRI typeIri = vf.createIRI("https://w3id.org/fair/fip/terms/FAIR-Enabling-Resource");
        String result = Utils.getTypeLabel(typeIri);
        assertEquals("FER", result);
    }

    @Test
    void getTypeLabel_returnsShortLabelForKnownFairSupportingResource() {
        IRI typeIri = vf.createIRI("https://w3id.org/fair/fip/terms/FAIR-Supporting-Resource");
        String result = Utils.getTypeLabel(typeIri);
        assertEquals("FSR", result);
    }

    @Test
    void getTypeLabel_returnsShortLabelForKnownFairImplementationProfile() {
        IRI typeIri = vf.createIRI("https://w3id.org/fair/fip/terms/FAIR-Implementation-Profile");
        String result = Utils.getTypeLabel(typeIri);
        assertEquals("FIP", result);
    }

    @Test
    void getTypeLabel_returnsShortLabelForDeclaredBy() {
        IRI typeIri = vf.createIRI("http://purl.org/nanopub/x/declaredBy");
        String result = Utils.getTypeLabel(typeIri);
        assertEquals("user intro", result);
    }

    @Test
    void getTypeLabel_truncatesLongTypeName() {
        IRI typeIri = vf.createIRI("http://w3id.org/veryLongTypeNameThatExceedsTwentyFiveCharacters");
        String result = Utils.getTypeLabel(typeIri);
        assertEquals("veryLongTypeNameThat...", result);
    }

    @Test
    void getTypeLabel_removesNanopubSuffixFromTypeName() {
        IRI typeIri = vf.createIRI("http://w3id.org/ExampleNanopub");
        String result = Utils.getTypeLabel(typeIri);
        assertEquals("Example", result);
    }

    @Test
    void getTypeLabel_returnsLastSegmentOfUri() {
        IRI typeIri = vf.createIRI("http://w3id.org/SomeType");
        String result = Utils.getTypeLabel(typeIri);
        assertEquals("SomeType", result);
    }

    @Test
    void getPageParametersAsString_encodesSingleParameter() {
        PageParameters params = new PageParameters();
        params.add("key", "value");
        String result = Utils.getPageParametersAsString(params);
        assertEquals("key=value", result);
    }

    @Test
    void getPageParametersAsString_encodesMultipleParameters() {
        PageParameters params = new PageParameters();
        params.add("key1", "value1");
        params.add("key2", "value2");
        String result = Utils.getPageParametersAsString(params);
        assertEquals("key1=value1&key2=value2", result);
    }

    @Test
    void getPageParametersAsString_handlesEmptyParameters() {
        PageParameters params = new PageParameters();
        String result = Utils.getPageParametersAsString(params);
        assertEquals("", result);
    }

    @Test
    void getPageParametersAsString_encodesSpecialCharacters() {
        PageParameters params = new PageParameters();
        params.add("key", "value with spaces & special=chars");
        String result = Utils.getPageParametersAsString(params);
        assertEquals("key=value+with+spaces+%26+special%3Dchars", result);
    }

}