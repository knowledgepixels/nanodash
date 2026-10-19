package com.knowledgepixels.nanodash.template;

import java.io.Serializable;
import java.time.Month;
import java.time.Year;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A value of a date placeholder, held as its parts: the year, month, day, time of day and
 * timezone. How many of the parts are given decides the {@link DatePrecision} of the value, and
 * so the datatype it is published with.
 * <p>
 * The parts are filled from the coarsest one onwards: the first one left empty ends the value,
 * and whatever follows it is not part of it.
 *
 * @param year  the year, e.g. "2026" or "-0044" (empty if not given)
 * @param month the month as two digits, e.g. "11" (empty if not given)
 * @param day   the day as two digits, e.g. "16" (empty if not given)
 * @param time  the time of day, e.g. "09:00" or "09:00:30.5" (empty if not given)
 * @param zone  the timezone, e.g. "Z" or "+01:00" (empty if not given)
 */
public record DateValue(String year, String month, String day, String time, String zone) implements Serializable {

    private static final Pattern LEXICAL = Pattern.compile(
            "^(-?\\d{4,})(?:-(\\d{2})(?:-(\\d{2})(?:T(\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?))?)?)?(Z|[+-]\\d{2}:\\d{2})?$");

    private static final Pattern YEAR = Pattern.compile("^-?\\d{4,}$");

    private static final Pattern TIME = Pattern.compile("^(\\d{2}):(\\d{2})(?::(\\d{2})(\\.\\d+)?)?$");

    private static final Pattern ZONE = Pattern.compile("^(?:Z|[+-](\\d{2}):(\\d{2}))$");

    private static final int MAX_ZONE_HOURS = 14;

    /**
     * Creates a value from its parts, any of which may be null for not given.
     *
     * @param year  the year
     * @param month the month as two digits
     * @param day   the day as two digits
     * @param time  the time of day
     * @param zone  the timezone
     */
    public DateValue {
        year = orEmpty(year);
        month = orEmpty(month);
        day = orEmpty(day);
        time = orEmpty(time);
        zone = orEmpty(zone);
    }

    /**
     * Reads the lexical form of a gYear, gYearMonth, date or dateTime literal.
     *
     * @param lexical the lexical form
     * @return the value, or null if the lexical form is not a valid value of any of these types
     */
    public static DateValue parse(String lexical) {
        if (lexical == null) return null;
        Matcher m = LEXICAL.matcher(lexical.trim());
        if (!m.matches()) return null;
        DateValue value = new DateValue(m.group(1), m.group(2), m.group(3), m.group(4), m.group(5));
        return value.getProblem() == null ? value : null;
    }

    /**
     * Returns how much of a point in time this value names: as far as its parts go, counted from
     * the year onwards.
     *
     * @return the precision, or null if not even the year is given
     */
    public DatePrecision getPrecision() {
        if (year.isEmpty()) return null;
        if (month.isEmpty()) return DatePrecision.YEAR;
        if (day.isEmpty()) return DatePrecision.YEAR_MONTH;
        if (time.isEmpty()) return DatePrecision.DATE;
        return DatePrecision.DATE_TIME;
    }

    /**
     * Returns this value with another timezone.
     *
     * @param newZone the timezone, e.g. "+01:00" (empty or null for none)
     * @return the value with the given timezone
     */
    public DateValue withZone(String newZone) {
        return new DateValue(year, month, day, time, newZone);
    }

    /**
     * Says what makes this value not a point in time, as far as its precision goes: a month
     * that does not exist, a day the month does not have, a time that is not one of the day.
     *
     * @return a message naming the problem, or null if there is none
     */
    public String getProblem() {
        DatePrecision precision = getPrecision();
        if (precision == null) return null;
        if (!YEAR.matcher(year).matches()) return "A year is four or more digits, e.g. 2026";
        if (precision == DatePrecision.YEAR) return getZoneProblem();
        int monthNumber = Integer.parseInt(month);
        if (monthNumber < 1 || monthNumber > 12) return "There is no month " + month;
        if (precision == DatePrecision.YEAR_MONTH) return getZoneProblem();
        Month monthOfYear = Month.of(monthNumber);
        int dayNumber = Integer.parseInt(day);
        if (dayNumber < 1 || dayNumber > monthOfYear.length(Year.isLeap(Long.parseLong(year)))) {
            return monthOfYear.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year + " has no day " + dayNumber;
        }
        if (precision == DatePrecision.DATE) return getZoneProblem();
        if (!isTimeOfDay(time)) return "'" + time + "' is not a time of day, e.g. 09:00";
        return getZoneProblem();
    }

    /**
     * Writes this value in the lexical form its datatype prescribes. A time of day without
     * seconds is written with :00, as a dateTime literal has to have them.
     *
     * @return the lexical form, or "" if not even the year is given
     */
    public String toLexical() {
        DatePrecision precision = getPrecision();
        if (precision == null) return "";
        String date = switch (precision) {
            case YEAR -> year;
            case YEAR_MONTH -> year + "-" + month;
            case DATE -> year + "-" + month + "-" + day;
            case DATE_TIME -> year + "-" + month + "-" + day + "T" + withSeconds(time);
        };
        return date + zone;
    }

    /**
     * Checks whether the given text is a time of day as this value takes it: hours and minutes,
     * with seconds and a fraction of a second optional.
     *
     * @param text the text to check
     * @return true if the text is a time of day
     */
    public static boolean isTimeOfDay(String text) {
        Matcher m = TIME.matcher(orEmpty(text));
        if (!m.matches()) return false;
        if (Integer.parseInt(m.group(1)) > 23 || Integer.parseInt(m.group(2)) > 59) return false;
        return m.group(3) == null || Integer.parseInt(m.group(3)) <= 59;
    }

    private String getZoneProblem() {
        if (zone.isEmpty()) return null;
        Matcher m = ZONE.matcher(zone);
        if (!m.matches()) return "'" + zone + "' is not a timezone";
        if (m.group(1) == null) return null;
        int hours = Integer.parseInt(m.group(1));
        int minutes = Integer.parseInt(m.group(2));
        if (hours > MAX_ZONE_HOURS || minutes > 59 || (hours == MAX_ZONE_HOURS && minutes > 0)) {
            return "'" + zone + "' is not a timezone";
        }
        return null;
    }

    private static String withSeconds(String time) {
        return time.length() == "HH:mm".length() ? time + ":00" : time;
    }

    private static String orEmpty(String part) {
        return part == null ? "" : part.trim();
    }

}
