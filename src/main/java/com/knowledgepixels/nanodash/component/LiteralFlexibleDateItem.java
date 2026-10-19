package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.template.DatePrecision;
import com.knowledgepixels.nanodash.template.DateValue;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.wicketstuff.jquery.core.JQueryBehavior;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.form.datetime.AjaxTimePicker;
import java.io.Serializable;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A component for date placeholders (see {@link Template#DATE_PLACEHOLDER}), which take a point
 * in time at whatever precision the user has it and publish it typed by what was entered: a
 * year alone as an {@code xsd:gYear}, a year and month as an {@code xsd:gYearMonth}, a full date
 * as an {@code xsd:date}, and a date with a time of day as an {@code xsd:dateTime} (issue #726).
 * <p>
 * Every part up to the finest precision the template accepts is offered from the start, so the
 * field is as open as the template allows. The parts the coarsest accepted precision needs have
 * to be filled in; the others may be left out, from the finest one backwards.
 */
public class LiteralFlexibleDateItem extends AbstractContextComponent {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(LiteralFlexibleDateItem.class);

    /**
     * The value as its parts, presented to the rest of the form as the lexical form of a literal
     * of the precision the parts reach. Keeping the parts rather than the string is what lets a
     * value be entered one part at a time.
     */
    static class DateModel implements IModel<String> {

        private String year = "", month = "", day = "", time = "";

        /**
         * The timezone a coarser value than a date and time was filled in with, kept so that
         * filling in an existing value does not drop it. Empty if it had none.
         */
        private String coarseZone = "";

        /**
         * The timezone of a date and time: the one picked, or the one of the value filled in.
         * Null as long as neither has decided it, in which case the user's own applies.
         */
        private String timeZone;

        /**
         * {@inheritDoc}
         */
        @Override
        public String getObject() {
            DateValue value = assemble(year, month, day, time, null);
            return value.getProblem() == null ? value.toLexical() : "";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setObject(String lexical) {
            DateValue value = DateValue.parse(lexical);
            if (value == null) {
                year = month = day = time = coarseZone = "";
                timeZone = null;
                return;
            }
            year = value.year();
            month = value.month();
            day = value.day();
            time = asShownInField(value.time());
            boolean isDateTime = value.getPrecision() == DatePrecision.DATE_TIME;
            coarseZone = isDateTime ? "" : value.zone();
            timeZone = isDateTime ? value.zone() : null;
        }

        /**
         * Assembles a value from the given parts with the timezone that goes with its precision.
         *
         * @param year  the year
         * @param month the month as two digits
         * @param day   the day as two digits
         * @param time  the time of day
         * @param zone  the timezone the zone field holds right now, or null to take the stored one
         * @return the value
         */
        DateValue assemble(String year, String month, String day, String time, String zone) {
            DateValue parts = new DateValue(year, month, day, time, "");
            if (parts.getPrecision() != DatePrecision.DATE_TIME) return parts.withZone(coarseZone);
            return parts.withZone(zone != null ? zone : getTimeZone());
        }

        /**
         * Returns the timezone of a date and time: the one decided by the user or by the value
         * filled in, or else the offset the user's own timezone has at the date and time entered.
         *
         * @return the timezone, e.g. "+01:00" (empty for a date and time without one)
         */
        String getTimeZone() {
            if (timeZone != null) return timeZone;
            return ZoneChoice.getUserOffsetAt(enteredDateTime().orElse(null)).getId();
        }

        private Optional<LocalDateTime> enteredDateTime() {
            try {
                LocalDate date = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
                return Optional.of(LocalDateTime.of(date, LocalTime.parse(time)));
            } catch (RuntimeException ex) {
                return Optional.empty();
            }
        }

        /**
         * Whether the value filled in was a date and time without a timezone, which is then
         * kept without one unless the user picks one.
         *
         * @return true if the date and time has no timezone
         */
        boolean hasNoTimeZone() {
            return "".equals(timeZone);
        }

        /**
         * Clears a day the month no longer has, as when 31 was picked for January and the month
         * is then changed to February, rather than keeping a value that is no point in time.
         */
        void dropDayTheMonthDoesNotHave() {
            if (!day.isEmpty() && Integer.parseInt(day) > daysInMonth(year, month)) day = "";
        }

        private static String asShownInField(String time) {
            return time.endsWith(":00") && time.length() == "HH:mm:ss".length() ? time.substring(0, "HH:mm".length()) : time;
        }

    }

    /**
     * The parts of a value given as text, each edited by a control of its own.
     */
    private enum Part {YEAR, MONTH, DAY}

    /**
     * A model that edits one part of a value, which tells the parts of a value apart from the
     * parts of any other value on the form.
     */
    private interface PartOfValue extends Serializable {

        /**
         * Returns the value this is a part of.
         *
         * @return the model of the whole value
         */
        DateModel getValue();

    }

    /**
     * A view of the year, month or day of the value, so that each control edits its own part
     * while they all share the one model the rest of the form sees.
     */
    private static class PartModel implements IModel<String>, PartOfValue {

        private final DateModel value;
        private final Part part;

        PartModel(DateModel value, Part part) {
            this.value = value;
            this.part = part;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DateModel getValue() {
            return value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getObject() {
            return switch (part) {
                case YEAR -> value.year;
                case MONTH -> value.month;
                case DAY -> value.day;
            };
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setObject(String object) {
            String v = (object == null) ? "" : object.trim();
            switch (part) {
                case YEAR -> value.year = v;
                case MONTH -> value.month = v;
                case DAY -> value.day = v;
            }
            if (part != Part.DAY) value.dropDayTheMonthDoesNotHave();
        }

    }

    /**
     * A view of the time of day of the value, as the time picker edits it: a {@link Date} on
     * the first day of 1970, read in the server's timezone. A time the picker hands back
     * unchanged keeps the form it was written in, down to more digits of a second than a
     * {@link Date} holds.
     */
    private static class TimeModel implements IModel<Date>, PartOfValue {

        private final DateModel value;

        TimeModel(DateModel value) {
            this.value = value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DateModel getValue() {
            return value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Date getObject() {
            return value.time.isEmpty() ? null : toDate(LocalTime.parse(value.time));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setObject(Date date) {
            if (date == null) {
                value.time = "";
                return;
            }
            LocalTime time = toLocalTime(date);
            if (!value.time.isEmpty() && LocalTime.parse(value.time).truncatedTo(ChronoUnit.MILLIS).equals(time)) return;
            value.time = time.toString();
        }

    }

    /**
     * A view of the timezone of the value, as the timezone choice edits it. Only picking a
     * timezone in the dropdown decides it, which its own Ajax update does as soon as it is
     * picked: a submitted form carries the timezone the dropdown showed when it was sent, which
     * for a preselected one may still be the offset for the date entered before, when a change to
     * the date or time arrives together with the submit. A preselected timezone therefore goes on
     * following the date until it is picked.
     */
    private class ZoneModel implements IModel<ZoneId>, PartOfValue {

        /**
         * {@inheritDoc}
         */
        @Override
        public DateModel getValue() {
            return model;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ZoneId getObject() {
            String zone = model.getTimeZone();
            return zone.isEmpty() ? null : ZoneId.of(zone);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setObject(ZoneId zone) {
            if (model.timeZone == null && isFormSubmitted()) return;
            model.timeZone = (zone == null) ? "" : zone.getId();
        }

    }

    private boolean isFormSubmitted() {
        Form<?> form = findParent(Form.class);
        return form != null && form.isSubmitted();
    }

    /**
     * Reads and writes the time of day as the time picker shows it: hours and minutes, and the
     * seconds where the time has them. A time typed with seconds is taken with them, even while
     * the picker only asks for minutes.
     */
    private static class TimeConverter implements IConverter<Date> {

        /**
         * {@inheritDoc}
         */
        @Override
        public Date convertToObject(String text, Locale locale) {
            if (text == null || text.isBlank()) return null;
            try {
                return toDate(LocalTime.parse(text.trim()));
            } catch (DateTimeParseException ex) {
                throw new ConversionException("'" + text + "' is not a time of day, e.g. 09:00")
                        .setSourceValue(text)
                        .setResourceKey("LiteralFlexibleDateItem.time");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String convertToString(Date date, Locale locale) {
            return date == null ? "" : toLocalTime(date).toString();
        }

    }

    private static Date toDate(LocalTime time) {
        return Date.from(LocalDate.EPOCH.atTime(time).atZone(ZoneId.systemDefault()).toInstant());
    }

    private static LocalTime toLocalTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalTime();
    }

    private final IRI iri;
    private final String regex;
    private final List<DatePrecision> precisions;
    private final DateModel model;
    private final List<FormComponent<?>> fields = new ArrayList<>();
    private final FormComponent<String> yearField, monthField, dayField;
    private final AjaxTimePicker timeField;
    private final ZoneChoice zoneChoice;
    private final FormComponent<ZoneId> zoneField;
    private final Label datatypeLabel;

    /**
     * Constructs a LiteralFlexibleDateItem with the specified ID, IRI, optional flag, and template
     * context.
     *
     * @param id       the component ID
     * @param iri      the IRI of the placeholder
     * @param optional whether this field is optional
     * @param context  the template context containing models and parameters
     */
    public LiteralFlexibleDateItem(String id, final IRI iri, boolean optional, TemplateContext context) {
        super(id, context);
        final Template template = context.getTemplate();
        this.iri = iri;
        this.regex = template.getRegex(iri);
        this.precisions = template.getDatePrecisions(iri);
        this.model = obtainModel(context);

        yearField = newYearField();
        monthField = newPartChoice("month", Part.MONTH, () -> numberedChoices(12), monthRenderer(),
                !isNeeded(DatePrecision.YEAR_MONTH));
        dayField = newPartChoice("day", Part.DAY,
                () -> numberedChoices(daysInMonth(partInput(yearField), partInput(monthField))), dayRenderer(),
                !isNeeded(DatePrecision.DATE));
        timeField = newTimeField();
        zoneChoice = newZoneChoice();
        zoneField = zoneChoice.getDropDown();

        addField(yearField, yearField, accepts(DatePrecision.YEAR), !optional, template);
        addField(monthField, monthField, accepts(DatePrecision.YEAR_MONTH), !optional && isNeeded(DatePrecision.YEAR_MONTH), template);
        addField(dayField, dayField, accepts(DatePrecision.DATE), !optional && isNeeded(DatePrecision.DATE), template);
        addField(timeField, timeField, accepts(DatePrecision.DATE_TIME), !optional && isNeeded(DatePrecision.DATE_TIME), template);
        addField(zoneChoice, zoneField, accepts(DatePrecision.DATE_TIME), false, template);

        addValueCheck(this::checkNoPartSkipped);
        addValueCheck(this::checkPointInTime);
        addValueCheck(this::checkAcceptedPrecision);
        if (regex != null) addValueCheck(this::checkRegex);

        datatypeLabel = new Label("datatype", () -> describeDatatype(model.getObject()));
        datatypeLabel.setOutputMarkupId(true);
        add(datatypeLabel);
    }

    /**
     * Returns the model of this placeholder's value, creating it if no item has yet. A repeated
     * statement group hands the same placeholder to a second item, which has to go on editing
     * the value the first one holds.
     */
    private DateModel obtainModel(TemplateContext context) {
        Map<IRI, IModel<?>> componentModels = context.getComponentModels();
        if (componentModels.get(iri) instanceof DateModel existing) return existing;
        DateModel created = new DateModel();
        Object previous = (componentModels.get(iri) == null) ? null : componentModels.get(iri).getObject();
        if (previous != null) created.setObject(previous.toString());
        componentModels.put(iri, created);
        String postfix = Utils.getUriPostfix(iri);
        if (previous == null && context.hasParam(postfix)) {
            created.setObject(context.getParam(postfix));
            context.setParamFilled(iri);
        }
        return created;
    }

    /**
     * Whether a part is needed by every precision the template accepts, so that it cannot be
     * left out: a placeholder that takes a date at least needs the year, month and day.
     *
     * @param precision the precision that adds the part
     * @return true if even the coarsest accepted precision includes the part
     */
    private boolean isNeeded(DatePrecision precision) {
        return precisions.get(0).compareTo(precision) >= 0;
    }

    private boolean accepts(DatePrecision precision) {
        return precisions.stream().anyMatch(p -> p.compareTo(precision) >= 0);
    }

    private FormComponent<String> newYearField() {
        TextField<String> field = new TextField<>("year", new PartModel(model, Part.YEAR));
        field.add(new AttributeModifier("type", "number"));
        field.add(new AttributeModifier("placeholder", "YYYY"));
        field.add(new LiteralGregorianItem.CurrentYearOnFirstStep());
        field.add((IValidator<String>) v -> {
            if (!v.getValue().matches("-?\\d{4,}")) {
                v.error(new ValidationError("A year is four or more digits, e.g. 2026"));
            }
        });
        field.setLabel(Model.of("year of " + getFieldLabel(iri)));
        return field;
    }

    /**
     * Creates the time picker, the same one fields for a date and time use. It asks for minutes,
     * and for seconds too once the time has them, so that a time filled in with seconds is shown
     * and kept in full. Its own Ajax update is what stores the time, as the picker reports a
     * change through its widget rather than through the input.
     */
    private AjaxTimePicker newTimeField() {
        AjaxTimePicker field = new AjaxTimePicker("time", new TimeModel(model), "HH:mm") {

            @Override
            public void onConfigure(JQueryBehavior behavior) {
                behavior.setOption("format", Options.asString(timeHasSeconds() ? "HH:mm:ss" : "HH:mm"));
                super.onConfigure(behavior);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <C> IConverter<C> getConverter(Class<C> type) {
                if (Date.class.isAssignableFrom(type)) return (IConverter<C>) new TimeConverter();
                return super.getConverter(type);
            }

            @Override
            public void onValueChanged(IPartialPageRequestHandler handler) {
                refreshParts(this, handler);
            }

        };
        field.add(new AttributeModifier("autocomplete", "off"));
        field.add(new AttributeModifier("data-form-type", "other"));
        field.add(AttributeModifier.append("class", () -> timeHasSeconds() ? "with-seconds" : null));
        field.setLabel(Model.of("time of " + getFieldLabel(iri)));
        return field;
    }

    private boolean timeHasSeconds() {
        return model.time.length() > "HH:mm".length();
    }

    private FormComponent<String> newPartChoice(String id, Part part, IModel<List<String>> choices, IChoiceRenderer<String> renderer,
                                                boolean canBeLeftOut) {
        DropDownChoice<String> field = new DropDownChoice<>(id, new PartModel(model, part), choices, renderer) {
            @Override
            protected String getNullKeyDisplayValue() {
                return "choose " + id;
            }

            @Override
            protected String getNullValidDisplayValue() {
                return "no " + id;
            }
        };
        field.setNullValid(canBeLeftOut);
        field.setLabel(Model.of(id + " of " + getFieldLabel(iri)));
        return field;
    }

    /**
     * Creates the timezone choice, the same one fields for a date and time use. It offers "no
     * timezone" only to keep a date and time filled in without one.
     */
    private ZoneChoice newZoneChoice() {
        ZoneChoice choice = new ZoneChoice("zone", new ZoneModel()) {

            @Override
            protected boolean isNoZoneAllowed() {
                return model.hasNoTimeZone();
            }

            @Override
            protected LocalDateTime getDateTime() {
                return model.enteredDateTime().orElse(null);
            }

        };
        choice.getDropDown().setLabel(Model.of("timezone of " + getFieldLabel(iri)));
        return choice;
    }

    /**
     * Adds one part control to the form, or a hidden placeholder for a part finer than the
     * template accepts.
     *
     * @param container the component that holds the control, which may be the control itself
     * @param field     the form component of the control
     * @param used      whether the template accepts a precision that has this part
     * @param required  whether the part has to be filled in
     * @param template  the template of the form
     */
    private void addField(Component container, FormComponent<?> field, boolean used, boolean required, Template template) {
        add(container);
        if (!used) {
            container.setVisible(false);
            return;
        }
        field.setRequired(required);
        if (template.getLabel(iri) != null) {
            field.add(new AttributeModifier("title", template.getLabel(iri)));
        }
        field.setOutputMarkupId(true);
        if (!(field instanceof AjaxTimePicker)) field.add(new RefreshPartsOnChange(field));
        field.add(new InvalidityHighlighting());
        lockIfNeeded(field, iri);
        context.getComponents().add(field);
        fields.add(field);
    }

    /**
     * Stores a part as soon as it changes and repaints the other parts of the same value, whose
     * default timezone follows the date entered, along with the datatype the value is going to
     * have.
     * It is the only updating behavior of a part: a second one would store the part again from
     * an input that is empty by then (see {@link LiteralGregorianItem}, issue #670).
     */
    private class RefreshPartsOnChange extends OnChangeAjaxBehavior {

        private final FormComponent<?> source;

        RefreshPartsOnChange(FormComponent<?> source) {
            this.source = source;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onUpdate(AjaxRequestTarget target) {
            refreshParts(source, target);
        }

    }

    /**
     * Repaints what a change to one part can change elsewhere: the day, whose choices are the
     * days of the month picked, the timezone, whose preselected offset follows the date, and the
     * datatype the value is going to have. The other parts of
     * this item are left as they are, so that a time typed but not yet reported by the time
     * picker is not painted over. A second item editing the same value in a repeated statement
     * group, found by the value behind its parts, is repainted in full.
     *
     * @param source the part that changed
     * @param target the handler to repaint them with
     */
    private void refreshParts(Component source, IPartialPageRequestHandler target) {
        for (Component c : context.getComponents()) {
            if (c == source) continue;
            if (fields.contains(c) && c != zoneField && c != dayField) continue;
            if (c.getDefaultModel() instanceof PartOfValue other && other.getValue() == model) {
                c.modelChanged();
                target.add(c);
            }
        }
        target.add(datatypeLabel);
    }

    /**
     * A check of the value as a whole, which every part runs.
     */
    @FunctionalInterface
    private interface ValueCheck extends Serializable {

        /**
         * Checks the value, reporting what is wrong with it on the part being validated.
         *
         * @param validatable the part being validated
         */
        void check(IValidatable<?> validatable);

    }

    /**
     * Adds a check of the value as a whole to every part, so that it runs whichever part the
     * user changed. Only the first part to report keeps its message: on a submit, where every
     * part is validated, the same complaint would otherwise be made once per part.
     */
    private void addValueCheck(ValueCheck check) {
        for (FormComponent<?> field : fields) {
            addCheck(field, check);
        }
    }

    private <T> void addCheck(FormComponent<T> field, ValueCheck check) {
        field.add((IValidator<T>) v -> {
            if (fields.stream().anyMatch(FormComponent::hasErrorMessage)) return;
            check.check(v);
        });
    }

    /**
     * Reports a part filled in while a coarser one is left out, such as a day without a month,
     * which would otherwise be dropped from the value without a word. This is only decided on a
     * submit, so that the parts can be filled in in any order.
     */
    private void checkNoPartSkipped(IValidatable<?> v) {
        if (!yearField.getForm().isSubmitted()) return;
        List<FormComponent<?>> parts = List.of(yearField, monthField, dayField, timeField);
        List<String> names = List.of("year", "month", "day", "time");
        for (int finer = parts.size() - 1; finer > 0; finer--) {
            if (partInput(parts.get(finer)).isEmpty()) continue;
            for (int coarser = 0; coarser < finer; coarser++) {
                if (partInput(parts.get(coarser)).isEmpty()) {
                    v.error(new ValidationError("'" + getFieldLabel(iri) + "' has a " + names.get(finer)
                            + " but no " + names.get(coarser)));
                    return;
                }
            }
            return;
        }
    }

    private void checkPointInTime(IValidatable<?> v) {
        String problem = candidateValue().getProblem();
        if (problem != null) v.error(new ValidationError(problem));
    }

    /**
     * Reports a value coarser than the template accepts, or of a precision in between that it
     * does not. This is only decided on a submit: while the value is being entered part by part,
     * a year that is not yet a date may be a value on its way to becoming one.
     */
    private void checkAcceptedPrecision(IValidatable<?> v) {
        if (!yearField.getForm().isSubmitted()) return;
        DatePrecision precision = candidateValue().getPrecision();
        if (precision == null || precisions.contains(precision)) return;
        v.error(new ValidationError("Enter " + DatePrecision.describe(precisions) + " for '" + getFieldLabel(iri) + "'"));
    }

    private void checkRegex(IValidatable<?> v) {
        String candidate = candidateValue().toLexical();
        if (!candidate.isEmpty() && !candidate.matches(regex)) {
            v.error(new ValidationError("Value '" + candidate + "' of '" + getFieldLabel(iri) + "' doesn't match the pattern '" + regex + "'"));
        }
    }

    /**
     * The value the controls hold right now, rather than the one last accepted: judging the
     * stored value would measure every correction against the entry it corrects.
     */
    private DateValue candidateValue() {
        String zone = zoneField.isVisibleInHierarchy() ? emptyAsNull(partInput(zoneField)) : null;
        String year = partInput(yearField), month = partInput(monthField);
        return model.assemble(year, month, dayInput(year, month), partInput(timeField), zone);
    }

    /**
     * The day as the value will have it: while a part is being changed on its own, a day the
     * month entered does not have is left out, as changing the month clears it, so that picking
     * February after the 31st is not refused. Only a submit judges the day against the month.
     */
    private String dayInput(String year, String month) {
        String day = partInput(dayField);
        if (day.isEmpty() || isFormSubmitted()) return day;
        return Integer.parseInt(day) > daysInMonth(year, month) ? "" : day;
    }

    /**
     * What a control holds right now: the input being validated where there is one, and the
     * stored part otherwise.
     */
    private static String partInput(FormComponent<?> field) {
        if (!field.isVisibleInHierarchy()) return "";
        String value = field.getValue();
        return (value == null) ? "" : value.trim();
    }

    private static String emptyAsNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private static String describeDatatype(String lexical) {
        DateValue value = DateValue.parse(lexical);
        if (value == null) return "";
        return "(" + Utils.getDatatypeLabel(value.getPrecision().getDatatype()) + ")";
    }

    /**
     * Returns how many days the given month has, the way a calendar does: February has 29 days
     * in a leap year and 28 in any other, and as long as the year is not known it may have 29.
     * While no month is picked, any day up to the 31st may be.
     *
     * @param year  the year, which may be empty or not yet a year
     * @param month the month as two digits, which may be empty
     * @return the number of days
     */
    static int daysInMonth(String year, String month) {
        if (month == null || !month.matches("(0[1-9]|1[0-2])")) return 31;
        Month monthOfYear = Month.of(Integer.parseInt(month));
        if (year == null || !year.matches("-?\\d{4,}")) return monthOfYear.maxLength();
        return monthOfYear.length(Year.isLeap(Long.parseLong(year)));
    }

    private static List<String> numberedChoices(int count) {
        List<String> choices = new ArrayList<>();
        for (int n = 1; n <= count; n++) choices.add(String.format("%02d", n));
        return choices;
    }

    private static IChoiceRenderer<String> monthRenderer() {
        return partRenderer(month -> Month.of(Integer.parseInt(month)).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
    }

    private static IChoiceRenderer<String> dayRenderer() {
        return partRenderer(day -> String.valueOf(Integer.parseInt(day)));
    }

    /**
     * A renderer that submits the part itself rather than its position in the list, so that the
     * input read while validating is the part and not an index into the choices.
     */
    private static IChoiceRenderer<String> partRenderer(SerializableFunction<String, String> label) {
        return new IChoiceRenderer<>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Object getDisplayValue(String part) {
                return label.apply(part);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String getIdValue(String part, int index) {
                return part;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String getObject(String id, IModel<? extends List<? extends String>> choices) {
                return id;
            }

        };
    }

    /**
     * A {@link java.util.function.Function} that survives page serialization, as everything a
     * Wicket component holds on to has to.
     *
     * @param <T> the type of the input
     * @param <R> the type of the result
     */
    @FunctionalInterface
    private interface SerializableFunction<T, R> extends java.util.function.Function<T, R>, java.io.Serializable {
    }

    /**
     * The value as the rest of the form sees it: the lexical form of the precision entered, or ""
     * while it is not a point in time.
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
     * <p>
     * A value of any accepted precision unifies, whichever precision the field holds: a
     * nanopublication being superseded may well have been published with less, or more, than
     * the user now knows.
     */
    @Override
    public boolean isUnifiableWith(Value v) {
        if (v == null) return true;
        if (!(v instanceof Literal vL)) return false;
        DatePrecision precision = DatePrecision.of(vL.getDatatype());
        if (precision == null || !precisions.contains(precision)) return false;
        DateValue value = DateValue.parse(vL.stringValue());
        if (value == null || value.getPrecision() != precision) return false;
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
        return "[Literal flexible date item]";
    }

}
