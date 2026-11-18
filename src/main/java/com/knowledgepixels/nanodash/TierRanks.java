package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;

/**
 * The numeric ranks of the role tiers, for threshold comparisons. Kept apart from
 * {@link SpaceMemberRole}, whose class initialisation builds the predefined admin role from
 * template data, so that comparing ranks — which every part page does — needs nothing loaded.
 */
public final class TierRanks {

    /**
     * Rank below every role-holder: anyone at all, role or not. Only meaningful for whose
     * nanopublications define a part (see {@link #partDefinitionRank}); access gating has no
     * use for it, since an action visible to {@code gen:EveryoneRole} needs no rank comparison.
     */
    public static final int ANYONE = -1;
    /**
     * The "everyone" floor of access gating (no role held), and any role-holder when asked
     * whose definitions of a part count.
     */
    public static final int EVERYONE = 0;
    public static final int OBSERVER = 1;
    public static final int MEMBER = 2;
    public static final int MAINTAINER = 3;
    public static final int ADMIN = 4;

    private TierRanks() {
    }

    /**
     * Numeric rank of a role-tier IRI. Unknown or null tiers (the "everyone" floor) rank
     * below observer.
     *
     * @param tier a role-tier IRI, or null
     * @return the rank: admin=4, maintainer=3, member=2, observer=1, else 0
     */
    public static int tierRank(IRI tier) {
        if (KPXL_TERMS.ADMIN_ROLE_TYPE.equals(tier)) return ADMIN;
        if (KPXL_TERMS.MAINTAINER_ROLE.equals(tier)) return MAINTAINER;
        if (KPXL_TERMS.MEMBER_ROLE.equals(tier)) return MEMBER;
        if (KPXL_TERMS.OBSERVER_ROLE.equals(tier)) return OBSERVER;
        return EVERYONE;
    }

    /**
     * Numeric rank of a {@code gen:hasPartDefinitionTier} value: the lowest tier whose
     * nanopublications count as definitions of a part. The tiers rank as in
     * {@link #tierRank}, and no declaration ({@code null}) keeps the original rule, every
     * role-holder ({@link #EVERYONE}). {@code gen:EveryoneRole} means what it means for
     * {@code gen:isVisibleTo}: anyone, role or not ({@link #ANYONE}).
     *
     * @param tier the declared tier IRI, or null
     * @return the rank: admin=4 … observer=1, any role-holder=0, anyone=-1
     */
    public static int partDefinitionRank(IRI tier) {
        if (KPXL_TERMS.EVERYONE_ROLE.equals(tier)) return ANYONE;
        return tierRank(tier);
    }

}
