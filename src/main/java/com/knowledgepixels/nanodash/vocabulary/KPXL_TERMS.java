package com.knowledgepixels.nanodash.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.nanopub.vocabulary.VocabUtils;

/**
 * Vocabulary for Knowledge Pixels Terms.
 */
public class KPXL_TERMS {

    public static final String NAMESPACE = "https://w3id.org/kpxl/gen/terms/";
    public static final String PREFIX = "kpxl_terms";
    public static final Namespace NS = VocabUtils.createNamespace(PREFIX, NAMESPACE);

    public static final IRI ACTIVATED_VIEW_DISPLAY = VocabUtils.createIRI(NAMESPACE, "ActivatedViewDisplay");
    public static final IRI DEACTIVATED_VIEW_DISPLAY = VocabUtils.createIRI(NAMESPACE, "DeactivatedViewDisplay");
    public static final IRI VIEW_ENTITY = VocabUtils.createIRI(NAMESPACE, "ViewEntity");
    public static final IRI RESOURCE_VIEW = VocabUtils.createIRI(NAMESPACE, "ResourceView");
    public static final IRI TABULAR_VIEW = VocabUtils.createIRI(NAMESPACE, "TabularView");
    public static final IRI LIST_VIEW = VocabUtils.createIRI(NAMESPACE, "ListView");
    public static final IRI PLAIN_PARAGRAPH_VIEW = VocabUtils.createIRI(NAMESPACE, "PlainParagraphView");
    public static final IRI NANOPUB_SET_VIEW = VocabUtils.createIRI(NAMESPACE, "NanopubSetView");
    public static final IRI ITEM_LIST_VIEW = VocabUtils.createIRI(NAMESPACE, "ItemListView");

    /**
     * Marks a view as a query-form view: instead of query results, it renders a form
     * for the query's placeholders that are not auto-filled from the page context;
     * submitting leads to the full results page. Orthogonal to the display types
     * above (a view can carry both), so the display type determines how the results
     * page renders. Deliberately not part of {@link com.knowledgepixels.nanodash.View}'s
     * supported display types.
     */
    public static final IRI QUERY_FORM_VIEW = VocabUtils.createIRI(NAMESPACE, "QueryFormView");
    public static final IRI VIEW_DISPLAY = VocabUtils.createIRI(NAMESPACE, "ViewDisplay");
    public static final IRI COLUMN_WIDTH_1_OF_12 = VocabUtils.createIRI(NAMESPACE, "ColumnWidth01of12");
    public static final IRI COLUMN_WIDTH_2_OF_12 = VocabUtils.createIRI(NAMESPACE, "ColumnWidth02of12");
    public static final IRI COLUMN_WIDTH_3_OF_12 = VocabUtils.createIRI(NAMESPACE, "ColumnWidth03of12");
    public static final IRI COLUMN_WIDTH_4_OF_12 = VocabUtils.createIRI(NAMESPACE, "ColumnWidth04of12");
    public static final IRI COLUMN_WIDTH_5_OF_12 = VocabUtils.createIRI(NAMESPACE, "ColumnWidth05of12");
    public static final IRI COLUMN_WIDTH_6_OF_12 = VocabUtils.createIRI(NAMESPACE, "ColumnWidth06of12");
    public static final IRI COLUMN_WIDTH_7_OF_12 = VocabUtils.createIRI(NAMESPACE, "ColumnWidth07of12");
    public static final IRI COLUMN_WIDTH_8_OF_12 = VocabUtils.createIRI(NAMESPACE, "ColumnWidth08of12");
    public static final IRI COLUMN_WIDTH_9_OF_12 = VocabUtils.createIRI(NAMESPACE, "ColumnWidth09of12");
    public static final IRI COLUMN_WIDTH_10_OF_12 = VocabUtils.createIRI(NAMESPACE, "ColumnWidth10of12");
    public static final IRI COLUMN_WIDTH_11_OF_12 = VocabUtils.createIRI(NAMESPACE, "ColumnWidth11of12");
    public static final IRI COLUMN_WIDTH_12_OF_12 = VocabUtils.createIRI(NAMESPACE, "ColumnWidth12of12");
    public static final IRI VIEW_ACTION = VocabUtils.createIRI(NAMESPACE, "ViewAction");
    public static final IRI VIEW_RESULT_ACTION = VocabUtils.createIRI(NAMESPACE, "ViewResultAction");
    public static final IRI VIEW_ENTRY_ACTION = VocabUtils.createIRI(NAMESPACE, "ViewEntryAction");

    // Presets (issue #302): a named bundle of default views and roles, and the
    // assignment of such a bundle to a resource. Mirrors the view-display model.
    // Resource types (the values used in gen:appliesToInstancesOf).
    public static final IRI SPACE = VocabUtils.createIRI(NAMESPACE, "Space");
    public static final IRI MAINTAINED_RESOURCE = VocabUtils.createIRI(NAMESPACE, "MaintainedResource");
    public static final IRI INDIVIDUAL_AGENT = VocabUtils.createIRI(NAMESPACE, "IndividualAgent");
    public static final IRI EVENT = VocabUtils.createIRI(NAMESPACE, "Event");

    public static final IRI PRESET = VocabUtils.createIRI(NAMESPACE, "Preset");
    public static final IRI PRESET_ASSIGNMENT = VocabUtils.createIRI(NAMESPACE, "PresetAssignment");
    public static final IRI ACTIVATED_PRESET_ASSIGNMENT = VocabUtils.createIRI(NAMESPACE, "ActivatedPresetAssignment");
    public static final IRI DEACTIVATED_PRESET_ASSIGNMENT = VocabUtils.createIRI(NAMESPACE, "DeactivatedPresetAssignment");

    public static final IRI HAS_DISPLAY_WIDTH = VocabUtils.createIRI(NAMESPACE, "hasDisplayWidth");
    public static final IRI HAS_VIEW_QUERY = VocabUtils.createIRI(NAMESPACE, "hasViewQuery");
    public static final IRI HAS_VIEW_QUERY_TARGET_FIELD = VocabUtils.createIRI(NAMESPACE, "hasViewQueryTargetField");
    public static final IRI APPLIES_TO_INSTANCES_OF = VocabUtils.createIRI(NAMESPACE, "appliesToInstancesOf");
    public static final IRI APPLIES_TO_NAMESPACE = VocabUtils.createIRI(NAMESPACE, "appliesToNamespace");
    public static final IRI APPLIES_TO = VocabUtils.createIRI(NAMESPACE, "appliesTo");
    public static final IRI HAS_VIEW_ACTION = VocabUtils.createIRI(NAMESPACE, "hasViewAction");
    public static final IRI HAS_ACTION_TEMPLATE = VocabUtils.createIRI(NAMESPACE, "hasActionTemplate");
    public static final IRI HAS_ACTION_TEMPLATE_TARGET_FIELD = VocabUtils.createIRI(NAMESPACE, "hasActionTemplateTargetField");
    public static final IRI HAS_ACTION_TEMPLATE_PART_FIELD = VocabUtils.createIRI(NAMESPACE, "hasActionTemplatePartField");
    public static final IRI HAS_ACTION_TEMPLATE_QUERY_MAPPING = VocabUtils.createIRI(NAMESPACE, "hasActionTemplateQueryMapping");
    public static final IRI HAS_PAGE_SIZE = VocabUtils.createIRI(NAMESPACE, "hasPageSize");
    public static final IRI HAS_STRUCTURAL_POSITION = VocabUtils.createIRI(NAMESPACE, "hasStructuralPosition");
    public static final IRI IS_DISPLAY_OF_VIEW = VocabUtils.createIRI(NAMESPACE, "isDisplayOfView");
    public static final IRI IS_DISPLAY_FOR = VocabUtils.createIRI(NAMESPACE, "isDisplayFor");

    /**
     * Links a view/preset version to the space that governs its {@code (kind, space)}
     * pair for authority-scoped latest-version resolution. A label, not a grant:
     * authority comes from the kind being maintained by that space plus the version
     * being signed by a current member+ of it. See
     * docs/views-and-presets-as-maintained-resources.md.
     */
    public static final IRI GOVERNED_BY = VocabUtils.createIRI(NAMESPACE, "governedBy");

    // Preset properties (issue #302):
    public static final IRI HAS_TOP_LEVEL_VIEW = VocabUtils.createIRI(NAMESPACE, "hasTopLevelView");
    public static final IRI HAS_VIEW = VocabUtils.createIRI(NAMESPACE, "hasView");
    public static final IRI HAS_ROLE = VocabUtils.createIRI(NAMESPACE, "hasRole");
    public static final IRI IS_ASSIGNMENT_OF_PRESET = VocabUtils.createIRI(NAMESPACE, "isAssignmentOfPreset");
    public static final IRI IS_ASSIGNMENT_FOR = VocabUtils.createIRI(NAMESPACE, "isAssignmentFor");

    // TODO Remove these deprecated terms.
    // Deprecated:
    public static final IRI TOP_LEVEL_VIEW_DISPLAY = VocabUtils.createIRI(NAMESPACE, "TopLevelViewDisplay");
    public static final IRI PART_LEVEL_VIEW_DISPLAY = VocabUtils.createIRI(NAMESPACE, "PartLevelViewDisplay");
    public static final IRI HAS_VIEW_TARGET_CLASS = VocabUtils.createIRI(NAMESPACE, "hasViewTargetClass");

    /**
     * The predicate to assign the admins of the space.
     */
    public static final IRI HAS_ADMIN = VocabUtils.createIRI(NAMESPACE, "hasAdmin");

    /**
     * The predicate for pinned templates in the space.
     */
    public static final IRI HAS_PINNED_TEMPLATE = VocabUtils.createIRI(NAMESPACE, "hasPinnedTemplate");

    /**
     * The predicate for pinned queries in the space.
     */
    public static final IRI HAS_PINNED_QUERY = VocabUtils.createIRI(NAMESPACE, "hasPinnedQuery");

    /**
     * The IRI for the "hasOwner" predicate.
     */
    public static final IRI HAS_OWNER = VocabUtils.createIRI(NAMESPACE, "hasOwner");
    /**
     * The IRI for the "hasAdmin" predicate.
     */
    public static final IRI HAS_ADMIN_PREDICATE = VocabUtils.createIRI(NAMESPACE, "hasAdmin");

    public static final IRI HAS_DEFAULT_LICENSE = VocabUtils.createIRI(NAMESPACE, "hasDefaultLicense");

    // Role-instantiation direction pinning (nanodash #525 / nanopub-query #136 Part B).
    // A role-assigning nanopub can carry a pubinfo pin `<predicate> a
    // gen:InverseRoleProperty` (or gen:RegularRoleProperty) so nanopub-query's
    // SpacesExtractor can resolve a custom role predicate's direction from the nanopub
    // alone. Templates declare the pin as a type on the (constant or placeholder)
    // predicate; nanodash lifts it into pubinfo and also attaches ROLE_INSTANTIATION,
    // which the extractor's pin-resolution branch is gated on.
    public static final IRI ROLE_INSTANTIATION = VocabUtils.createIRI(NAMESPACE, "RoleInstantiation");
    public static final IRI INVERSE_ROLE_PROPERTY = VocabUtils.createIRI(NAMESPACE, "InverseRoleProperty");
    public static final IRI REGULAR_ROLE_PROPERTY = VocabUtils.createIRI(NAMESPACE, "RegularRoleProperty");

    // Role tiers (subclasses of gen:SpaceMemberRole; materialized server-side by
    // nanopub-query as the npa:hasRoleType value). Ordered admin > maintainer >
    // member > observer; observer is the default when a role declares no tier.
    // Used to gate view-display visibility by role tier; see
    // docs/role-specific-views.md.
    public static final IRI ADMIN_ROLE_TYPE = VocabUtils.createIRI(NAMESPACE, "AdminRole");
    public static final IRI MAINTAINER_ROLE = VocabUtils.createIRI(NAMESPACE, "MaintainerRole");
    public static final IRI MEMBER_ROLE = VocabUtils.createIRI(NAMESPACE, "MemberRole");
    public static final IRI OBSERVER_ROLE = VocabUtils.createIRI(NAMESPACE, "ObserverRole");

    /**
     * Visibility sentinel tier meaning "everyone, including anonymous viewers"
     * (the rank-0 floor). Unlike the tiers above it is <em>not</em> a
     * nanopub-query grant tier — it is never granted, only used as a
     * {@code gen:isVisibleTo} value/default to express "no restriction"
     * explicitly (needed because a view-creation template cannot leave the
     * per-action visibility statement optional). See docs/role-specific-views.md.
     */
    public static final IRI EVERYONE_ROLE = VocabUtils.createIRI(NAMESPACE, "EveryoneRole");

    /**
     * Restricts a view display (or view) to viewers holding the given role tier
     * (one of the role-tier IRIs above) or a specific role IRI. Absent means
     * visible to everyone. See docs/role-specific-views.md.
     */
    public static final IRI IS_VISIBLE_TO = VocabUtils.createIRI(NAMESPACE, "isVisibleTo");

}
