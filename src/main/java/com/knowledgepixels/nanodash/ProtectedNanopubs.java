package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.template.Template;
import org.nanopub.Nanopub;
import org.nanopub.extra.server.NanopubServerUtils;

import java.util.Collection;

/**
 * The policy for protected nanopublications (issue #671): nanopublications typed
 * {@code npx:ProtectedNanopub} in their publication info, which local/private Nanopub Registry
 * instances accept, store and serve, and which the public network refuses.
 * <p>
 * The marker itself is added by an ordinary pubinfo template ({@link #TEMPLATE_ID}), so that the
 * resulting statement has a template behind it like every other one and round-trips through
 * {@code ValueFiller} when a protected nanopublication is superseded. The template is unlisted,
 * though: it is not a description of the content but a decision about where the nanopublication
 * may be stored, and it deserves an affordance of its own rather than a line in the "add
 * element..." dropdown, which is behind "show more" and easy to get subtly wrong (the generic
 * "Nanopublication type" template, for one, produces {@code npx:hasNanopubType} — <em>not</em>
 * what makes a nanopublication protected, so a nanopublication that looks protected in the form
 * would be published to the public network).
 * <p>
 * Whether protection is on by default is a deployment decision, because both kinds of local
 * instance exist: mostly-public ones with occasional protected content, and private-by-default
 * ones. See {@link NanodashPreferences#isProtectedByDefault()}.
 */
public class ProtectedNanopubs {

    private ProtectedNanopubs() {
    }  // no instances allowed

    /**
     * The pubinfo template that adds the {@code npx:ProtectedNanopub} type to the nanopublication
     * being created. It has no placeholders: adding it adds the marker.
     */
    public static final String TEMPLATE_ID = "https://w3id.org/np/RAjTlfGJgWb8K7cOspGdwodPpnu859q78Rps40xDGdgZs";

    /**
     * What the protected checkbox says about a nanopublication that carries the marker. It is a
     * statement of fact, not a consent text: nothing is openly published, so there is nothing to
     * consent to (see {@code PublishForm.isConsentGiven}).
     */
    public static final String STAYS_LOCAL_NOTE =
            "This nanopublication will stay on the local instance this Nanodash is connected to.";

    /**
     * Returns whether this deployment can store protected nanopublications at all, and should
     * therefore offer to make them. Only a local/private registry accepts them; on a public
     * deployment the option would do nothing but make publishing fail.
     *
     * @return true if the main registry is a local instance
     */
    public static boolean isOffered() {
        return ServiceMode.isRegistryLocal();
    }

    /**
     * Returns whether the publish form should start with protection turned on.
     *
     * @return true on a private-by-default deployment that can store protected nanopublications
     */
    public static boolean isOnByDefault() {
        return isOffered() && NanodashPreferences.get().isProtectedByDefault();
    }

    /**
     * Returns whether the given nanopublication carries the protected marker.
     *
     * @param np the nanopublication to check, may be null
     * @return true if it is typed {@code npx:ProtectedNanopub} in its own publication info
     */
    public static boolean isProtected(Nanopub np) {
        return np != null && NanopubServerUtils.isProtectedNanopub(np);
    }

    /**
     * Returns why a nanopublication built on the given sources has to be protected, or null if
     * the choice is free.
     * <p>
     * A new version of, or a nanopublication derived from, a protected one would otherwise expose
     * the content it repeats; a nanopublication made with a protected template would be a public
     * nanopublication whose {@code nt:wasCreatedFromTemplate} link nobody outside the local
     * instance can follow. Both are decided here rather than left to the user.
     * <p>
     * Not covered: a nanopublication governed by a protected space. The governing space is
     * identified by a resource IRI rather than by the nanopublication that defines it, so
     * answering that would take a query; in practice the templates such a space governs are
     * themselves stored on the local instance, which the template check below catches.
     *
     * @param fillSource the nanopublication the form is filled from (superseded, derived from,
     *                   overridden or improved), or null
     * @param templates  the templates the form is built on
     * @return a phrase naming the reason, to be shown to the user, or null if not forced
     */
    public static String getForcedReason(Nanopub fillSource, Collection<Template> templates) {
        if (isProtected(fillSource)) {
            return "the nanopublication it is based on is protected";
        }
        if (templates != null) {
            for (Template template : templates) {
                if (template != null && isProtected(template.getNanopub())) {
                    return "the template it is based on is protected";
                }
            }
        }
        return null;
    }

}
