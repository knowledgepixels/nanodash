package com.knowledgepixels.nanodash.template;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.util.List;
import java.util.stream.Collectors;

/**
 * How much of a point in time a value of a date placeholder names, from the year alone to the
 * full date and time. Each precision is published as the XSD datatype that says exactly that
 * much, so the datatype of the literal follows from what was entered (issue #726).
 * <p>
 * The precisions are ordered from coarsest to finest, and each one names everything the
 * previous one does and one part more.
 */
public enum DatePrecision {

    YEAR(XSD.GYEAR, "a year"),
    YEAR_MONTH(XSD.GYEARMONTH, "a year and month"),
    DATE(XSD.DATE, "a date"),
    DATE_TIME(XSD.DATETIME, "a date and time");

    /**
     * The precisions a date placeholder accepts when its template does not list any.
     */
    public static final List<DatePrecision> ALL = List.of(values());

    private final IRI datatype;
    private final String description;

    DatePrecision(IRI datatype, String description) {
        this.datatype = datatype;
        this.description = description;
    }

    /**
     * The precision a datatype stands for.
     *
     * @param datatype the datatype IRI, which may be null
     * @return the precision, or null if the datatype is not one a date placeholder can publish
     */
    public static DatePrecision of(IRI datatype) {
        if (datatype == null) return null;
        for (DatePrecision precision : values()) {
            if (precision.datatype.equals(datatype)) return precision;
        }
        return null;
    }

    /**
     * Describes a set of precisions the way a message to the user names them, e.g.
     * "a year, a date or a date and time".
     *
     * @param precisions the precisions, coarsest first
     * @return the description
     */
    public static String describe(List<DatePrecision> precisions) {
        List<String> descriptions = precisions.stream().map(DatePrecision::getDescription).collect(Collectors.toList());
        if (descriptions.size() <= 1) return String.join("", descriptions);
        String allButLast = String.join(", ", descriptions.subList(0, descriptions.size() - 1));
        return allButLast + " or " + descriptions.get(descriptions.size() - 1);
    }

    /**
     * Returns the datatype IRI a value of this precision is published with.
     *
     * @return the datatype IRI
     */
    public IRI getDatatype() {
        return datatype;
    }

    /**
     * Returns how a value of this precision is named in messages, e.g. "a year and month".
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

}
