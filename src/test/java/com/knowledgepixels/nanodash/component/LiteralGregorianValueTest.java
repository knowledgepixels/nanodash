package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.component.LiteralGregorianItem.GregorianType;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The lexical forms of the XSD Gregorian datatypes, which is what this component has to get
 * right: what it assembles ends up verbatim in a published nanopublication.
 */
class LiteralGregorianValueTest {

    @Test
    void everyGregorianDatatypeIsRecognized() {
        assertEquals(GregorianType.G_YEAR, GregorianType.of(XSD.GYEAR));
        assertEquals(GregorianType.G_YEAR_MONTH, GregorianType.of(XSD.GYEARMONTH));
        assertEquals(GregorianType.G_MONTH, GregorianType.of(XSD.GMONTH));
        assertEquals(GregorianType.G_MONTH_DAY, GregorianType.of(XSD.GMONTHDAY));
        assertEquals(GregorianType.G_DAY, GregorianType.of(XSD.GDAY));
    }

    @Test
    void otherDatatypesAreLeftToTheirOwnComponents() {
        assertFalse(LiteralGregorianItem.supports(XSD.DATE), "xsd:date has a date picker");
        assertFalse(LiteralGregorianItem.supports(XSD.DATETIME), "xsd:dateTime has its own picker");
        assertFalse(LiteralGregorianItem.supports(XSD.STRING));
        assertFalse(LiteralGregorianItem.supports(XSD.INTEGER), "a gYear is not an integer field");
        assertFalse(LiteralGregorianItem.supports(null), "an untyped literal has no datatype");
    }

    @Test
    void partsAssembleIntoTheFormEachDatatypePrescribes() {
        assertEquals("2026", GregorianType.G_YEAR.assemble("2026", "", "", ""));
        assertEquals("2026-05", GregorianType.G_YEAR_MONTH.assemble("2026", "05", "", ""));
        assertEquals("--05", GregorianType.G_MONTH.assemble("", "05", "", ""));
        assertEquals("--05-17", GregorianType.G_MONTH_DAY.assemble("", "05", "17", ""));
        assertEquals("---17", GregorianType.G_DAY.assemble("", "", "17", ""));
    }

    @Test
    void anIncompleteValueAssemblesToNothing() {
        // Rather than to "2026-", which would be published as an invalid literal.
        assertEquals("", GregorianType.G_YEAR_MONTH.assemble("2026", "", "", ""));
        assertEquals("", GregorianType.G_YEAR_MONTH.assemble("", "05", "", ""));
        assertEquals("", GregorianType.G_MONTH_DAY.assemble("", "05", "", ""));
        assertEquals("", GregorianType.G_MONTH_DAY.assemble("", "", "17", ""));
        assertEquals("", GregorianType.G_YEAR.assemble("", "", "", ""));
    }

    @Test
    void everyFormSplitsBackIntoTheSameParts() {
        assertArrayEquals(new String[]{"2026", "", "", ""}, GregorianType.G_YEAR.split("2026"));
        assertArrayEquals(new String[]{"2026", "05", "", ""}, GregorianType.G_YEAR_MONTH.split("2026-05"));
        assertArrayEquals(new String[]{"", "05", "", ""}, GregorianType.G_MONTH.split("--05"));
        assertArrayEquals(new String[]{"", "05", "17", ""}, GregorianType.G_MONTH_DAY.split("--05-17"));
        assertArrayEquals(new String[]{"", "", "17", ""}, GregorianType.G_DAY.split("---17"));
    }

    @Test
    void aTimezoneIsKeptRatherThanDroppedOnTheWayBack() {
        // A value filled in from an existing nanopublication may carry one; re-publishing it
        // without would change what the nanopub says.
        assertArrayEquals(new String[]{"2026", "", "", "Z"}, GregorianType.G_YEAR.split("2026Z"));
        assertArrayEquals(new String[]{"", "05", "17", "+02:00"}, GregorianType.G_MONTH_DAY.split("--05-17+02:00"));
        assertEquals("2026Z", GregorianType.G_YEAR.assemble("2026", "", "", "Z"));
        assertEquals("--05-17+02:00", GregorianType.G_MONTH_DAY.assemble("", "05", "17", "+02:00"));
    }

    @Test
    void yearsOutsideTheOrdinaryRangeAreStillYears() {
        assertTrue(GregorianType.G_YEAR.isValid("-0044"), "XSD years can be negative");
        assertTrue(GregorianType.G_YEAR.isValid("12026"), "and can have more than four digits");
        assertArrayEquals(new String[]{"-0044", "", "", ""}, GregorianType.G_YEAR.split("-0044"));
    }

    @Test
    void aValueOfTheWrongShapeIsNotValid() {
        assertFalse(GregorianType.G_YEAR.isValid("26"), "a year is at least four digits");
        assertFalse(GregorianType.G_YEAR.isValid("2026-05"), "that is a gYearMonth");
        assertFalse(GregorianType.G_MONTH.isValid("05"), "a gMonth carries its two leading dashes");
        assertFalse(GregorianType.G_DAY.isValid("--17"), "a gDay carries three");
        assertFalse(GregorianType.G_MONTH_DAY.isValid("--05-17-01"));
        assertFalse(GregorianType.G_YEAR.isValid(""));
        assertNull(GregorianType.G_YEAR.split(null));
    }

}
