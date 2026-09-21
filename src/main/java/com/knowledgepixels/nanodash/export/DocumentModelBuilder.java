package com.knowledgepixels.nanodash.export;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDataFetcher;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.export.DocumentModel.Block;
import com.knowledgepixels.nanodash.export.DocumentModel.HtmlBlock;
import com.knowledgepixels.nanodash.export.DocumentModel.Inline;
import com.knowledgepixels.nanodash.export.DocumentModel.ListBlock;
import com.knowledgepixels.nanodash.export.DocumentModel.Paragraph;
import com.knowledgepixels.nanodash.export.DocumentModel.Section;
import com.knowledgepixels.nanodash.export.DocumentModel.TableBlock;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Builds a {@link DocumentModel} from a resource's view displays and their query
 * results, mirroring the cell-rendering conventions of the QueryResult* components
 * (label columns, multi-value columns, hidden action columns, source columns).
 */
public final class DocumentModelBuilder {

    private static final Logger logger = LoggerFactory.getLogger(DocumentModelBuilder.class);

    private DocumentModelBuilder() {
    }

    /**
     * Builds the document model for the given page type and resource, resolving the
     * resource the same way as the RDF download.
     *
     * @param type       "user", "space", "resource", or "part"
     * @param id         the resource (or part) identifier
     * @param parameters the page parameters (for the part context)
     * @return the document model
     */
    public static DocumentModel build(String type, String id, PageParameters parameters) {
        AbstractResourceWithProfile resource;
        String partId = null;
        Set<IRI> partClasses = null;
        String nanopubRef = null;

        switch (type) {
            case "user" -> {
                resource = IndividualAgent.get(id);
                if (resource == null) {
                    throw new IllegalArgumentException("User not found: " + id);
                }
            }
            case "space" -> {
                resource = SpaceRepository.get().findById(id);
                if (resource == null) {
                    throw new IllegalArgumentException("Space not found: " + id);
                }
            }
            case "resource" -> {
                resource = MaintainedResourceRepository.get().findById(id);
                if (resource == null) {
                    throw new IllegalArgumentException("Resource not found: " + id);
                }
            }
            case "part" -> {
                String contextId = parameters.get("context").toString();
                if (contextId == null) {
                    throw new IllegalArgumentException("Parameter 'context' is required for type=part");
                }
                resource = ViewDataFetcher.resolveContextResource(contextId);
                partId = id;
                partClasses = ViewDataFetcher.resolvePartClasses(id, contextId, resource);
                nanopubRef = ViewDataFetcher.resolvePartNanopubRef(id, contextId, resource);
            }
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        }

        return build(resource, partId, partClasses, nanopubRef);
    }

    static DocumentModel build(AbstractResourceWithProfile resource, String partId, Set<IRI> partClasses, String nanopubRef) {
        String title = partId != null ? Utils.getShortNameFromURI(partId) : resource.getLabel();
        String iri = partId != null ? partId : resource.getId();

        String targetId = partId != null ? partId : resource.getId();
        String targetNpId = nanopubRef != null ? nanopubRef : resource.getNanopubId();

        List<Section> sections = new ArrayList<>();
        for (ViewDisplay vd : resource.fetchViewDisplaysSync(partId, partClasses)) {
            Section section = buildSection(vd, resource, targetId, targetNpId);
            if (section != null) {
                sections.add(section);
            }
        }
        return new DocumentModel(title, iri, sections);
    }

    private static Section buildSection(ViewDisplay vd, AbstractResourceWithProfile resource, String targetId, String targetNpId) {
        View view = vd.getView();
        if (view == null) return null;

        // Header views have no query (issue #572); they become a heading-only section.
        if (KPXL_TERMS.HEADER_VIEW.equals(view.getViewType())) {
            return new Section(orEmpty(vd.getTitle()), view.getDescription(), List.of());
        }
        // Query-form views are interactive and have no fixed result to export.
        if (view.hasQueryForm()) return null;

        String heading = vd.getTitle();
        if ((heading == null || heading.isBlank()) && view.getQuery() != null) {
            heading = view.getQuery().getLabel();
        }
        heading = orEmpty(heading);

        try {
            QueryRef queryRef = ViewDataFetcher.buildQueryRef(vd, resource, targetId, targetNpId);
            if (queryRef == null) {
                return new Section(heading, null, List.of(Paragraph.of("(no data)")));
            }
            ApiResponse response = ViewDataFetcher.retrieveResponseWithWait(queryRef);
            if (response == null) {
                return new Section(heading, null, List.of(Paragraph.of("(no data)")));
            }
            if (response.getData().isEmpty()) {
                return new Section(heading, null, List.of(Paragraph.of("(nothing found)")));
            }
            IRI viewType = view.getViewType();
            if (KPXL_TERMS.TABULAR_VIEW.equals(viewType)) {
                return new Section(heading, null, List.of(buildTable(view, response)));
            } else if (KPXL_TERMS.LIST_VIEW.equals(viewType)) {
                return new Section(heading, null, List.of(buildList(view, response)));
            } else if (KPXL_TERMS.ITEM_LIST_VIEW.equals(viewType)) {
                return new Section(heading, null, List.of(buildItemList(response)));
            } else if (KPXL_TERMS.PLAIN_PARAGRAPH_VIEW.equals(viewType)) {
                return new Section(heading, null, buildParagraphBlocks(response));
            } else if (KPXL_TERMS.NANOPUB_SET_VIEW.equals(viewType)) {
                return new Section(heading, null, List.of(buildNanopubList(response)));
            } else {
                return new Section(heading, null, List.of(Paragraph.of("(view type not supported in document export)")));
            }
        } catch (Exception ex) {
            logger.error("Error building document section for view display {}: {}", vd.getId(), ex.getMessage());
            return new Section(heading, null, List.of(Paragraph.of("(no data)")));
        }
    }

    /**
     * Builds a table block, mirroring QueryResultTable's column conventions: label
     * and action-mapping columns are skipped, the np/nps source column is dropped,
     * "_noheader" columns get an empty header, and the header row is dropped when
     * no column has a visible header.
     */
    static TableBlock buildTable(View view, ApiResponse response) {
        Set<String> hiddenColumns = view != null ? view.getActionMappingSourceColumns() : Collections.emptySet();
        String sourceColumnKey = null;
        for (String h : response.getHeader()) {
            if (h.equals("np") || h.equals("nps")) {
                sourceColumnKey = h;
            }
        }

        List<String> dataKeys = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        boolean anyHeaderShown = false;
        for (String h : response.getHeader()) {
            if (h.endsWith("_label") || h.endsWith("_label_multi")
                || hiddenColumns.contains(h) || h.equals(sourceColumnKey)) {
                continue;
            }
            boolean noHeader = h.endsWith("_noheader");
            String key = noHeader ? h.substring(0, h.length() - "_noheader".length()) : h;
            String displayLabel = key;
            if (displayLabel.endsWith("_multi_iri")) {
                displayLabel = displayLabel.substring(0, displayLabel.length() - "_multi_iri".length());
            } else if (displayLabel.endsWith("_multi_val")) {
                displayLabel = displayLabel.substring(0, displayLabel.length() - "_multi_val".length());
            } else if (displayLabel.endsWith("_multi")) {
                displayLabel = displayLabel.substring(0, displayLabel.length() - "_multi".length());
            } else if (displayLabel.endsWith("_iri")) {
                displayLabel = displayLabel.substring(0, displayLabel.length() - "_iri".length());
            }
            dataKeys.add(h);
            keys.add(key);
            if (noHeader) {
                headers.add("");
            } else {
                anyHeaderShown = true;
                headers.add(displayLabel.replaceAll("_", " "));
            }
        }

        List<List<List<Inline>>> rows = new ArrayList<>();
        for (ApiResponseEntry entry : response.getData()) {
            List<List<Inline>> row = new ArrayList<>();
            for (int i = 0; i < dataKeys.size(); i++) {
                row.add(renderCell(entry, keys.get(i), entry.get(dataKeys.get(i))));
            }
            rows.add(row);
        }
        return new TableBlock(anyHeaderShown ? headers : null, rows);
    }

    /**
     * Builds a list block, mirroring QueryResultList: per row, the visible columns
     * joined with " · "; the "^"-labeled source column is dropped.
     */
    static ListBlock buildList(View view, ApiResponse response) {
        Set<String> hiddenColumns = view != null ? view.getActionMappingSourceColumns() : Collections.emptySet();
        List<List<Inline>> items = new ArrayList<>();
        for (ApiResponseEntry entry : response.getData()) {
            List<Inline> item = new ArrayList<>();
            for (String key : response.getHeader()) {
                if (key.endsWith("_label") || key.endsWith("_label_multi") || hiddenColumns.contains(key)) {
                    continue;
                }
                String value = entry.get(key);
                if (value == null || value.isBlank()) {
                    continue;
                }
                // The "^"-labeled column is the row's source link, not row content.
                if ("^".equals(entry.get(key + "_label"))) {
                    continue;
                }
                List<Inline> cell = renderCell(entry, key, value);
                if (cell.isEmpty()) {
                    continue;
                }
                if (!item.isEmpty()) {
                    item.add(new Inline(" · ", null));
                }
                item.addAll(cell);
            }
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        return new ListBlock(items);
    }

    /**
     * Builds a list block from an item-list view, mirroring QueryResultItemList:
     * per row only the first non-empty visible column is rendered.
     */
    static ListBlock buildItemList(ApiResponse response) {
        List<List<Inline>> items = new ArrayList<>();
        for (ApiResponseEntry entry : response.getData()) {
            for (String key : response.getHeader()) {
                if (key.endsWith("_label") || key.endsWith("_label_multi")) {
                    continue;
                }
                String value = entry.get(key);
                if (value == null || value.isBlank()) {
                    continue;
                }
                List<Inline> cell = renderCell(entry, key, value);
                if (!cell.isEmpty()) {
                    items.add(cell);
                }
                break;
            }
        }
        return new ListBlock(items);
    }

    /**
     * Builds the blocks of a plain-paragraph view: per row an optional title
     * paragraph plus the sanitized HTML content with a plain-text fallback.
     */
    static List<Block> buildParagraphBlocks(ApiResponse response) {
        List<Block> blocks = new ArrayList<>();
        for (ApiResponseEntry entry : response.getData()) {
            String title = entry.get("title");
            if (title != null && !title.isBlank()) {
                blocks.add(Paragraph.of(title));
            }
            String content = entry.get("content");
            if (content != null && !content.isBlank()) {
                String sanitized = Utils.sanitizeHtml(content);
                blocks.add(new HtmlBlock(sanitized, htmlToPlainText(sanitized)));
            }
        }
        return blocks;
    }

    /**
     * Builds a list of links to the nanopubs of a nanopub-set view (full card
     * rendering is not supported in document export).
     */
    static ListBlock buildNanopubList(ApiResponse response) {
        List<List<Inline>> items = new ArrayList<>();
        for (ApiResponseEntry entry : response.getData()) {
            String npUri = entry.get("np");
            if (npUri != null && !npUri.isBlank()) {
                items.add(List.of(new Inline(Utils.getShortNameFromURI(npUri), npUri)));
            }
            String npMulti = entry.get("np_multi_iri");
            if (npMulti != null) {
                for (String uri : npMulti.split("\\s+")) {
                    if (!uri.isBlank()) {
                        items.add(List.of(new Inline(Utils.getShortNameFromURI(uri), uri)));
                    }
                }
            }
        }
        return new ListBlock(items);
    }

    /**
     * Renders a single column value as inlines, mirroring the QueryResult* cell
     * conventions. IRIs link to the IRI itself (a downloaded document should carry
     * resolvable links, not app-relative ones); labels equal to the URI are treated
     * as absent, falling back to the short name derived from the URI.
     *
     * @param entry the response row
     * @param key   the logical column key (with any "_noheader" marker stripped)
     * @param value the column value (may be null)
     * @return the inlines (empty for a null/blank value)
     */
    static List<Inline> renderCell(ApiResponseEntry entry, String key, String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Inline> inlines = new ArrayList<>();
        if (key.endsWith("_multi_iri")) {
            String labelKey = key.substring(0, key.length() - "_multi_iri".length()) + "_label_multi";
            String labelValue = entry.get(labelKey);
            String[] uris = value.split("\\s+");
            String[] labels = labelValue != null ? labelValue.split("\n", -1) : null;
            for (int i = 0; i < uris.length; i++) {
                String uri = uris[i];
                if (uri.isBlank()) continue;
                String label = (labels != null && i < labels.length && !labels[i].isBlank()) ? Utils.unescapeMultiValue(labels[i]) : null;
                if (label == null || label.equals(uri)) {
                    label = Utils.getShortNameFromURI(uri);
                }
                appendSeparated(inlines, new Inline(label, uri));
            }
        } else if (key.endsWith("_multi_val")) {
            String labelKey = key.substring(0, key.length() - "_multi_val".length()) + "_label_multi";
            String labelValue = entry.get(labelKey);
            String[] parts = value.split("\n", -1);
            String[] labels = labelValue != null ? labelValue.split("\n", -1) : null;
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                String label = (labels != null && i < labels.length && !labels[i].isBlank()) ? Utils.unescapeMultiValue(labels[i]) : null;
                if (Utils.isUriValue(part)) {
                    if (label == null || label.equals(part)) {
                        label = Utils.getShortNameFromURI(part);
                    }
                    appendSeparated(inlines, new Inline(label, part));
                } else {
                    String display = label != null ? label : Utils.unescapeMultiValue(part);
                    appendSeparated(inlines, new Inline(plainText(display), null));
                }
            }
        } else if (key.endsWith("_multi")) {
            String labelKey = key.substring(0, key.length() - "_multi".length()) + "_label_multi";
            String labelValue = entry.get(labelKey);
            String[] parts = value.split("\n", -1);
            String[] labels = labelValue != null ? labelValue.split("\n", -1) : null;
            for (int i = 0; i < parts.length; i++) {
                boolean hasLabel = labels != null && i < labels.length && !labels[i].isBlank();
                String display = hasLabel ? Utils.unescapeMultiValue(labels[i]) : Utils.unescapeMultiValue(parts[i]);
                appendSeparated(inlines, new Inline(plainText(display), null));
            }
        } else if (Utils.isUriValue(value)) {
            String label = entry.get(key + "_label");
            if (label == null || label.isBlank() || label.equals(value)) {
                label = Utils.getShortNameFromURI(value);
            }
            inlines.add(new Inline(label, value));
        } else {
            String label = entry.get(key + "_label");
            String display = (label != null && !label.isBlank()) ? label : value;
            inlines.add(new Inline(plainText(display), null));
        }
        return inlines;
    }

    private static void appendSeparated(List<Inline> inlines, Inline inline) {
        if (!inlines.isEmpty()) {
            inlines.add(new Inline(", ", null));
        }
        inlines.add(inline);
    }

    /**
     * Reduces an HTML-looking literal to plain text (rich fragments are only kept
     * for paragraph-view content, not inside cells).
     */
    private static String plainText(String value) {
        if (Utils.looksLikeHtml(value)) {
            return htmlToPlainText(Utils.sanitizeHtml(value));
        }
        return value;
    }

    /**
     * Reduces an HTML fragment to plain text; see {@link Utils#htmlToPlainText(String)},
     * which the document export shares with label creation.
     *
     * @param html the HTML fragment
     * @return the plain text
     */
    static String htmlToPlainText(String html) {
        return Utils.htmlToPlainText(html);
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

}
