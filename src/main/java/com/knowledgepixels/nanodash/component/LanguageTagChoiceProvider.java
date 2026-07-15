package com.knowledgepixels.nanodash.component;

import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Provides the language-tag choices for a language-tag-selectable literal
 * placeholder: either the template's restricted set, or the full ISO 639-1
 * list (with free entry of other BCP 47 tags handled by select2 tags mode).
 */
public class LanguageTagChoiceProvider extends ChoiceProvider<String> {

    private static final List<String> ISO_LANGUAGES = Arrays.asList(Locale.getISOLanguages());

    private final List<String> possibleTags;

    /**
     * Creates a provider offering the given tags, or the ISO 639-1 languages if null.
     *
     * @param possibleTags the allowed language tags, or null for the unrestricted list
     */
    public LanguageTagChoiceProvider(List<String> possibleTags) {
        this.possibleTags = possibleTags;
    }

    @Override
    public String getDisplayValue(String tag) {
        if (tag == null || tag.isEmpty()) return "";
        String name = Locale.forLanguageTag(tag).getDisplayName(Locale.ENGLISH);
        if (name == null || name.isEmpty() || name.equals(tag)) return tag;
        return tag + " — " + name;
    }

    @Override
    public String getIdValue(String object) {
        return object;
    }

    // Getting strange errors with Tomcat if this method is not overridden:
    @Override
    public void detach() {
    }

    @Override
    public void query(String term, int page, Response<String> response) {
        List<String> tags = (possibleTags == null) ? ISO_LANGUAGES : possibleTags;
        if (term == null || term.isEmpty()) {
            response.addAll(tags);
            return;
        }
        String t = term.toLowerCase();
        for (String tag : tags) {
            if (tag.toLowerCase().contains(t) || getDisplayValue(tag).toLowerCase().contains(t)) {
                response.add(tag);
            }
        }
    }

    @Override
    public Collection<String> toChoices(Collection<String> ids) {
        return ids;
    }

}
