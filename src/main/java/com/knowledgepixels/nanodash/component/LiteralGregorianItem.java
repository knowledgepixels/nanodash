package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.Month;
import java.time.MonthDay;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A component for literal placeholders typed with one of the XSD Gregorian datatypes:
 * {@code xsd:gYear}, {@code xsd:gYearMonth}, {@code xsd:gMonth}, {@code xsd:gMonthDay} and
 * {@code xsd:gDay}. They name a point on the calendar that is coarser than a date, which is
 * why a date picker cannot express them: "2026" and "--05-17" are values a full date has no
 * way to leave unsaid.
 * <p>
 * Each part is entered through a control that can only produce a value the datatype allows —
 * a number field for the year, dropdowns for month and day — and the parts are assembled into
 * the lexical form the datatype prescribes.
 */
public class LiteralGregorianItem extends AbstractContextComponent {

    private static final long serialVersionUID = 1L;

    private final static Logger logger = LoggerFactory.getLogger(LiteralGregorianItem.class);

    /**
     * A timezone may be appended to any of these values. Nothing here offers to add one, but a
     * value being filled in from an existing nanopublication may carry one, and dropping it
     * would silently change what the nanopub says.
     */
    private static final String ZONE = "(Z|[+-]\\d{2}:\\d{2})?";

    /**
     * The XSD Gregorian datatypes, with the lexical form each one prescribes.
     */
    public enum GregorianType {

        G_YEAR(XSD.GYEAR, true, false, false, "(-?\\d{4,})" + ZONE),
        G_YEAR_MONTH(XSD.GYEARMONTH, true, true, false, "(-?\\d{4,})-(\\d{2})" + ZONE),
        G_MONTH(XSD.GMONTH, false, true, false, "--(\\d{2})" + ZONE),
        G_MONTH_DAY(XSD.GMONTHDAY, false, true, true, "--(\\d{2})-(\\d{2})" + ZONE),
        G_DAY(XSD.GDAY, false, false, true, "---(\\d{2})" + ZONE);

        private final IRI datatype;
        private final boolean hasYear, hasMonth, hasDay;
        private final Pattern pattern;

        GregorianType(IRI datatype, boolean hasYear, boolean hasMonth, boolean hasDay, String regex) {
            this.datatype = datatype;
            this.hasYear = hasYear;
            this.hasMonth = hasMonth;
            this.hasDay = hasDay;
            this.pattern = Pattern.compile("^" + regex + "$");
        }

        /**
         * The type for a datatype IRI, or null if the datatype is not a Gregorian one.
         *
         * @param datatype the datatype IRI, which may be null
         * @return the matching type, or null
         */
        public static GregorianType of(IRI datatype) {
            if (datatype == null) return null;
            for (GregorianType t : values()) {
                if (t.datatype.equals(datatype)) return t;
            }
            return null;
        }

        /**
         * @return the datatype IRI this type stands for
         */
        public IRI getDatatype() {
            return datatype;
        }

        /**
         * @return whether values of this type name a year
         */
        public boolean hasYear() {
            return hasYear;
        }

        /**
         * @return whether values of this type name a month
         */
        public boolean hasMonth() {
            return hasMonth;
        }

        /**
         * @return whether values of this type name a day
         */
        public boolean hasDay() {
            return hasDay;
        }

        /**
         * Assembles a lexical value from its parts, or the empty string if a part this type
         * needs is missing. Half-entered values are never assembled into something the
         * datatype would reject; the form reports them instead (see the partiality check in
         * the constructor).
         *
         * @param year  the year, e.g. "2026" (empty if not entered)
         * @param month the month as two digits, e.g. "05" (empty if not entered)
         * @param day   the day as two digits, e.g. "17" (empty if not entered)
         * @param zone  a timezone suffix to append, e.g. "Z" (usually empty)
         * @return the lexical value, or ""
         */
        public String assemble(String year, String month, String day, String zone) {
            if (hasYear && year.isEmpty()) return "";
            if (hasMonth && month.isEmpty()) return "";
            if (hasDay && day.isEmpty()) return "";
            String suffix = (zone == null) ? "" : zone;
            return switch (this) {
                case G_YEAR -> year + suffix;
                case G_YEAR_MONTH -> year + "-" + month + suffix;
                case G_MONTH -> "--" + month + suffix;
                case G_MONTH_DAY -> "--" + month + "-" + day + suffix;
                case G_DAY -> "---" + day + suffix;
            };
        }

        /**
         * Splits a lexical value into its parts.
         *
         * @param value the lexical value
         * @return the parts, or null if the value is not a valid value of this type
         */
        public String[] split(String value) {
            if (value == null) return null;
            Matcher m = pattern.matcher(value.trim());
            if (!m.matches()) return null;
            String year = "", month = "", day = "";
            int group = 1;
            if (hasYear) year = m.group(group++);
            if (hasMonth) month = m.group(group++);
            if (hasDay) day = m.group(group++);
            String zone = m.group(m.groupCount());
            return new String[]{year, month, day, zone == null ? "" : zone};
        }

        /**
         * @param value the lexical value to check
         * @return whether the value is a valid value of this type
         */
        public boolean isValid(String value) {
            return split(value) != null;
        }

    }

    /**
     * Whether this component handles the given datatype.
     *
     * @param datatype the datatype IRI, which may be null
     * @return true if the datatype is one of the XSD Gregorian types
     */
    public static boolean supports(IRI datatype) {
        return GregorianType.of(datatype) != null;
    }

    /**
     * The value as its parts, presented to the rest of the form as the single lexical string
     * the placeholder's datatype prescribes. Keeping the parts rather than the string is what
     * lets a half-entered value stay on screen: "May" survives the moment before a day is
     * picked, while {@link #getObject()} reports nothing until the value is complete.
     */
    private static class GregorianModel implements IModel<String> {

        private final GregorianType type;
        private String year = "", month = "", day = "", zone = "";

        GregorianModel(GregorianType type) {
            this.type = type;
        }

        @Override
        public String getObject() {
            return type.assemble(year, month, day, zone);
        }

        @Override
        public void setObject(String value) {
            String[] parts = (value == null) ? null : type.split(value);
            if (parts == null) {
                year = month = day = zone = "";
            } else {
                year = parts[0];
                month = parts[1];
                day = parts[2];
                zone = parts[3];
            }
        }

    }

    /**
     * A view of one part of the value, so that each control edits its own part while they all
     * share the one model the rest of the form sees.
     */
    private static class PartModel implements IModel<String> {

        private enum Part {YEAR, MONTH, DAY}

        private final GregorianModel value;
        private final Part part;

        PartModel(GregorianModel value, Part part) {
            this.value = value;
            this.part = part;
        }

        @Override
        public String getObject() {
            return switch (part) {
                case YEAR -> value.year;
                case MONTH -> value.month;
                case DAY -> value.day;
            };
        }

        @Override
        public void setObject(String object) {
            String v = (object == null) ? "" : object.trim();
            switch (part) {
                case YEAR -> value.year = v;
                case MONTH -> value.month = v;
                case DAY -> value.day = v;
            }
        }

    }

    private final GregorianType type;
    private final IRI iri;
    private final String regex;
    private final GregorianModel model;
    private final List<FormComponent<String>> fields = new ArrayList<>();
    private final FormComponent<String> yearField, monthField, dayField;

    /**
     * Constructs a LiteralGregorianItem with the specified ID, IRI, optional flag, and template
     * context.
     *
     * @param id       the component ID
     * @param iri      the IRI of the placeholder
     * @param optional whether this field is optional
     * @param context  the template context containing models and parameters
     */
    public LiteralGregorianItem(String id, final IRI iri, boolean optional, TemplateContext context) {
        super(id, context);
        final Template template = context.getTemplate();
        this.iri = iri;
        this.type = GregorianType.of(template.getDatatype(iri));
        this.regex = template.getRegex(iri);

        Map<IRI, IModel<?>> componentModels = context.getComponentModels();
        // A repeated statement group hands the same placeholder to a second item, which has to
        // go on editing the value the first one holds.
        if (componentModels.get(iri) instanceof GregorianModel existing && existing.type == type) {
            model = existing;
        } else {
            model = new GregorianModel(type);
            Object previous = (componentModels.get(iri) == null) ? null : componentModels.get(iri).getObject();
            if (previous != null) model.setObject(previous.toString());
            componentModels.put(iri, model);
            String postfix = Utils.getUriPostfix(iri);
            if (previous == null && context.hasParam(postfix)) {
                model.setObject(context.getParam(postfix));
            }
        }

        // Each part of a two-part value requires the other, but only in a request that carries
        // that other one: its raw input decides, not its stored value. An Ajax update of one
        // part alone is then stored rather than rejected for the absence of a part the user has
        // not reached yet -- being rejected is what used to leave both parts unstored, so the
        // value could never be completed -- while a submit carries both and reports a half one.
        // The same idiom keeps a language-tagged literal from being published untagged.
        yearField = new TextField<>("year", new PartModel(model, PartModel.Part.YEAR)) {
            @Override
            public boolean isRequired() {
                return super.isRequired() || (type.hasMonth() && hasInput(monthField));
            }
        };
        // A number input gives the spinner and the numeric keypad; the pattern is what actually
        // decides, since the input is a text field again in browsers that ignore the type.
        yearField.add(new AttributeModifier("type", "number"));
        yearField.add(new AttributeModifier("placeholder", "YYYY"));
        yearField.add((IValidator<String>) v -> {
            if (!v.getValue().matches("-?\\d{4,}")) {
                v.error(new ValidationError("A year is four or more digits, e.g. 2026"));
            }
        });
        monthField = new DropDownChoice<>("month",
                new PartModel(model, PartModel.Part.MONTH), monthChoices(), monthRenderer()) {
            @Override
            public boolean isRequired() {
                return super.isRequired()
                        || (type.hasYear() && hasInput(yearField))
                        || (type.hasDay() && hasInput(dayField));
            }
        };
        dayField = new DropDownChoice<>("day",
                new PartModel(model, PartModel.Part.DAY), dayChoices(), dayRenderer()) {
            @Override
            public boolean isRequired() {
                return super.isRequired() || (type.hasMonth() && hasInput(monthField));
            }
        };
        String label = template.getLabel(iri);
        String of = (label == null) ? "" : " of '" + label + "'";
        yearField.setLabel(Model.of("year" + of));
        monthField.setLabel(Model.of("month" + of));
        dayField.setLabel(Model.of("day" + of));

        addField(yearField, type.hasYear(), optional, template);
        addField(monthField, type.hasMonth(), optional, template);
        addField(dayField, type.hasDay(), optional, template);

        // These judge the whole value rather than one part of it, so they go on every part: a
        // pattern that only the month breaks has to be applied when the month is what changed.
        // They read the value as the controls hold it now -- reading the stored value would
        // judge the previous entry, so the first value rejected would be measured against the
        // pattern from then on and no correction could ever pass.
        if (regex != null) {
            addValueCheck(v -> {
                String candidate = candidateValue();
                if (!candidate.isEmpty() && !candidate.matches(regex)) {
                    v.error(new ValidationError("Value '" + candidate + "' doesn't match the pattern '" + regex + "'"));
                }
            });
        }
        if (type == GregorianType.G_MONTH_DAY) {
            // February has 29 days at most, April 31 never: a day the month cannot have is not
            // a value of this datatype, and the dropdowns alone cannot rule it out.
            addValueCheck(v -> {
                String month = partInput(monthField), day = partInput(dayField);
                if (month.isEmpty() || day.isEmpty()) return;
                try {
                    MonthDay.of(Integer.parseInt(month), Integer.parseInt(day));
                } catch (DateTimeException ex) {
                    v.error(new ValidationError(Month.of(Integer.parseInt(month))
                            .getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " has no day " + Integer.parseInt(day)));
                }
            });
        }

        Label datatypeComp = new Label("datatype",
                Model.of("(" + type.getDatatype().stringValue().replace(XSD.NAMESPACE, "xsd:") + ")"));
        add(datatypeComp);
    }

    /**
     * Adds one part control to the form, or a hidden placeholder for the parts this datatype
     * does not name.
     */
    private void addField(FormComponent<String> field, boolean used, boolean optional, Template template) {
        add(field);
        if (!used) {
            field.setVisible(false);
            return;
        }
        if (!optional) field.setRequired(true);
        if (template.getLabel(iri) != null) {
            field.add(new AttributeModifier("title", template.getLabel(iri)));
        }
        // Exactly one updating behavior per field. A second one -- KeepValueAfterRefreshBehavior,
        // which every other item adds on top of its own -- runs updateModel() again in the same
        // request, by which point the input it reads is empty, and writes that emptiness over
        // the value this one just stored. A field whose entry had been rejected then ignored
        // every correction (issue #670). The Ajax round-trip this behavior already makes is
        // what keeps the value across a refresh, which is all the other one was there for.
        field.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                for (Component c : context.getComponents()) {
                    if (c == field) continue;
                    // The parts of one value have models of their own, so components sharing
                    // this placeholder are found by the value behind them, not by the model in
                    // front of it.
                    if (c.getDefaultModel() instanceof PartModel other && other.value == model) {
                        c.modelChanged();
                        target.add(c);
                    }
                }
            }
        });
        field.add(new InvalidityHighlighting());
        context.getComponents().add(field);
        fields.add(field);
    }

    /**
     * What a control holds right now: the input being validated where there is one, and the
     * stored part otherwise. A value assembled from these reflects the entry being made rather
     * than the one last accepted.
     */
    private static String partInput(FormComponent<String> field) {
        if (!field.isVisible()) return "";
        String value = field.getValue();
        return (value == null) ? "" : value.trim();
    }

    /**
     * Adds a check of the value as a whole to every part, so that it runs whichever part the
     * user changed. Only the first part to report keeps its message: on a submit, where every
     * part is validated, the same complaint would otherwise be made once per part.
     */
    private void addValueCheck(IValidator<String> check) {
        for (FormComponent<String> field : fields) {
            field.add((IValidator<String>) v -> {
                if (fields.stream().anyMatch(FormComponent::hasErrorMessage)) return;
                check.validate(v);
            });
        }
    }

    /**
     * The value the controls hold right now, assembled as the datatype prescribes, or "" while
     * a part is missing.
     */
    private String candidateValue() {
        return type.assemble(partInput(yearField), partInput(monthField), partInput(dayField), model.zone);
    }

    /**
     * Whether a part was submitted with something in it in this request. Reading the raw input
     * rather than the value is what tells a submit, which carries every field, apart from an
     * Ajax update of one field alone.
     */
    private static boolean hasInput(FormComponent<String> field) {
        if (field == null) return false;
        String raw = field.getInput();
        return raw != null && !raw.isBlank();
    }

    private static List<String> monthChoices() {
        List<String> months = new ArrayList<>();
        for (int m = 1; m <= 12; m++) months.add(String.format("%02d", m));
        return months;
    }

    private static List<String> dayChoices() {
        List<String> days = new ArrayList<>();
        for (int d = 1; d <= 31; d++) days.add(String.format("%02d", d));
        return days;
    }

    private static IChoiceRenderer<String> monthRenderer() {
        return partRenderer(month -> Month.of(Integer.parseInt(month)).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
    }

    private static IChoiceRenderer<String> dayRenderer() {
        return partRenderer(day -> String.valueOf(Integer.parseInt(day)));
    }

    /**
     * A renderer that submits the part itself rather than its position in the list. Wicket's
     * default submits the index, which every reader of the field would then have to translate
     * back -- and a reader that forgets to would see the day before the one that was picked.
     */
    private static IChoiceRenderer<String> partRenderer(SerializableFunction<String, String> label) {
        return new IChoiceRenderer<>() {

            @Override
            public Object getDisplayValue(String part) {
                return label.apply(part);
            }

            @Override
            public String getIdValue(String part, int index) {
                return part;
            }

            @Override
            public String getObject(String id, IModel<? extends List<? extends String>> choices) {
                return id;
            }

        };
    }

    /**
     * A {@link java.util.function.Function} that survives page serialization, as everything a
     * Wicket component holds on to has to.
     */
    private interface SerializableFunction<T, R> extends java.util.function.Function<T, R>, java.io.Serializable {
    }

    /**
     * The value as the rest of the form sees it: the assembled lexical form, or "" while the
     * value is incomplete.
     *
     * @return the value model
     */
    IModel<String> getValueModel() {
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromContext() {
        context.getComponents().removeAll(fields);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnifiableWith(Value v) {
        if (v == null) return true;
        if (!(v instanceof Literal vL)) return false;
        if (!type.getDatatype().equals(vL.getDatatype())) return false;
        if (!type.isValid(vL.stringValue())) return false;
        if (regex != null && !vL.stringValue().matches(regex)) return false;
        String current = model.getObject();
        return current.isEmpty() || current.equals(vL.stringValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unifyWith(Value v) throws UnificationException {
        if (v == null) return;
        if (!isUnifiableWith(v)) throw new UnificationException(v.stringValue());
        model.setObject(v.stringValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillFinished() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finalizeValues() {
        Value defaultValue = context.getTemplate().getDefault(iri);
        if (isUnifiableWith(defaultValue)) {
            try {
                unifyWith(defaultValue);
            } catch (UnificationException ex) {
                logger.error("Could not unify with default value.", ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[Literal " + type.getDatatype().stringValue().replace(XSD.NAMESPACE, "xsd:") + " item]";
    }

}
