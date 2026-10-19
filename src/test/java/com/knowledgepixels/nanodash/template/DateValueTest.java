package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.Utils;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The value of a date placeholder, whose precision decides the datatype it is published with.
 */
class DateValueTest {

    @Test
    void theDatatypeFollowsFromWhatWasEntered() {
        assertEquals(DatePrecision.YEAR, DateValue.parse("2026").getPrecision());
        assertEquals(DatePrecision.YEAR_MONTH, DateValue.parse("2026-11").getPrecision());
        assertEquals(DatePrecision.DATE, DateValue.parse("2026-11-16").getPrecision());
        assertEquals(DatePrecision.DATE_TIME, DateValue.parse("2026-11-16T09:00:00+01:00").getPrecision());

        assertEquals(XSD.GYEAR, DatePrecision.YEAR.getDatatype());
        assertEquals(XSD.GYEARMONTH, DatePrecision.YEAR_MONTH.getDatatype());
        assertEquals(XSD.DATE, DatePrecision.DATE.getDatatype());
        assertEquals(XSD.DATETIME, DatePrecision.DATE_TIME.getDatatype());
    }

    @Test
    void aValueIsWrittenAsItsDatatypePrescribes() {
        assertEquals("2026", new DateValue("2026", "", "", "", "").toLexical());
        assertEquals("2026-11", new DateValue("2026", "11", "", "", "").toLexical());
        assertEquals("2026-11-16", new DateValue("2026", "11", "16", "", "").toLexical());
        assertEquals("2026-11-16T09:00:00+01:00", new DateValue("2026", "11", "16", "09:00", "+01:00").toLexical(),
                "a time without seconds is written with :00");
        assertEquals("2026-11-16T09:00:30.5Z", new DateValue("2026", "11", "16", "09:00:30.5", "Z").toLexical());
    }

    @Test
    void theFirstPartLeftEmptyEndsTheValue() {
        DateValue value = new DateValue("2026", "", "16", "09:00", "+01:00");

        assertEquals(DatePrecision.YEAR, value.getPrecision());
        assertEquals("2026+01:00", value.toLexical(), "a day without its month is not part of the value");
        assertNull(new DateValue("", "11", "16", "", "").getPrecision(), "nothing without a year");
    }

    @Test
    void aValueReadIsWrittenBackUnchanged() {
        for (String lexical : List.of("2026", "-0044", "12026", "2026Z", "2026-11", "2026-11-16", "2026-11-16-05:00",
                "2026-11-16T09:00:00+01:00", "2026-11-16T09:00:00", "2026-11-16T09:00:30.125Z")) {
            assertEquals(lexical, DateValue.parse(lexical).toLexical());
        }
    }

    @Test
    void whatIsNotAPointInTimeIsNotRead() {
        assertNull(DateValue.parse("26"));
        assertNull(DateValue.parse("2026-13"));
        assertNull(DateValue.parse("2026-02-29"), "2026 is no leap year");
        assertNotNull(DateValue.parse("2028-02-29"), "2028 is one");
        assertNull(DateValue.parse("2026-11-16T25:00:00Z"));
        assertNull(DateValue.parse("2026-11-16T09:00Z"), "a dateTime literal has seconds");
        assertNull(DateValue.parse("2026-11-16+15:00"));
        assertNull(DateValue.parse("--11-16"), "a gMonthDay is not a precision of a date placeholder");
        assertNull(DateValue.parse(null));
    }

    @Test
    void aProblemIsNamed() {
        assertEquals("February 2026 has no day 30", new DateValue("2026", "02", "30", "", "").getProblem());
        assertEquals("'25:00' is not a time of day, e.g. 09:00", new DateValue("2026", "02", "03", "25:00", "Z").getProblem());
        assertNull(new DateValue("2026", "02", "03", "23:59", "Z").getProblem());
    }

    @Test
    void precisionsAreNamedInWords() {
        assertEquals("a date", DatePrecision.describe(List.of(DatePrecision.DATE)));
        assertEquals("a date or a date and time", DatePrecision.describe(List.of(DatePrecision.DATE, DatePrecision.DATE_TIME)));
        assertEquals("a year, a year and month, a date or a date and time", DatePrecision.describe(DatePrecision.ALL));
    }

    @Test
    void valuesOfMixedPrecisionOrderByTime() {
        List<String> values = new ArrayList<>(List.of("2026-11-16T09:00:00+01:00", "2027", "2026-11", "2025-12-31", "2026", "2026-11-16"));
        values.sort(Utils::compareValues);

        assertEquals(List.of("2025-12-31", "2026", "2026-11", "2026-11-16", "2026-11-16T09:00:00+01:00", "2027"), values,
                "a coarser value orders before the finer ones it contains, and all of them by time");
    }

}
