package com.knowledgepixels.nanodash.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Vocabulary for Knowledge Pixels Terms.
 */
public class KPXL_TERMS {

    public static final String NAMESPACE = "https://w3id.org/kpxl/gen/terms/";
    public static final String PREFIX = "kpxl_terms";
    public static final Namespace NS = VocabUtils.createNamespace(PREFIX, NAMESPACE);

    public static final IRI TOP_LEVEL_VIEW_DISPLAY = VocabUtils.createIRI(NAMESPACE, "TopLevelViewDisplay");
    public static final IRI PART_LEVEL_VIEW_DISPLAY = VocabUtils.createIRI(NAMESPACE, "PartLevelViewDisplay");
    public static final IRI RESOURCE_VIEW = VocabUtils.createIRI(NAMESPACE, "ResourceView");
    public static final IRI TABULAR_VIEW = VocabUtils.createIRI(NAMESPACE, "TabularView");
    public static final IRI LIST_VIEW = VocabUtils.createIRI(NAMESPACE, "ListView");
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

    public static final IRI HAS_DISPLAY_WIDTH = VocabUtils.createIRI(NAMESPACE, "hasDisplayWidth");
    public static final IRI HAS_VIEW_QUERY = VocabUtils.createIRI(NAMESPACE, "hasViewQuery");
    public static final IRI HAS_VIEW_QUERY_TARGET_FIELD = VocabUtils.createIRI(NAMESPACE, "hasViewQueryTargetField");
    public static final IRI HAS_VIEW_TARGET_CLASS = VocabUtils.createIRI(NAMESPACE, "hasViewTargetClass");
    public static final IRI HAS_ELEMENT_NAMESPACE = VocabUtils.createIRI(NAMESPACE, "hasElementNamespace");
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

}
