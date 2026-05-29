package com.knowledgepixels.nanodash.component;

import java.time.Duration;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;

/**
 * Ajax behavior for filter text fields that applies the filter as the user
 * types, without requiring an Enter key press or focus change.
 * <p>
 * It listens on the {@code input} event (which fires on every keystroke,
 * unlike {@code change}, which only fires on blur/Enter) and throttles
 * requests so that rapid typing is coalesced into a single server round-trip.
 */
public abstract class FilterUpdatingBehavior extends AjaxFormComponentUpdatingBehavior {

    private static final Duration THROTTLE_DELAY = Duration.ofMillis(300);

    public FilterUpdatingBehavior() {
        super("input");
    }

    @Override
    protected abstract void onUpdate(AjaxRequestTarget target);

    @Override
    protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
        super.updateAjaxAttributes(attributes);
        // postponeTimerOnUpdate = true: reset the timer on each keystroke so the
        // request fires only after the user pauses typing (debounce semantics).
        attributes.setThrottlingSettings(new ThrottlingSettings(THROTTLE_DELAY, true));
    }

}
