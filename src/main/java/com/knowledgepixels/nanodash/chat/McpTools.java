package com.knowledgepixels.nanodash.chat;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The read-only tool catalog exposed to Claude Code through the MCP endpoint.
 * See docs/claude-code-chat.md.
 */
public class McpTools {

    private McpTools() {
    }

    private static final int MAX_ROWS = 50;

    /**
     * A single MCP tool: its self-description for {@code tools/list} and its executor.
     */
    public static class Tool {

        public final String name;
        public final String description;
        public final JsonObject inputSchema;
        public final Function<JsonObject, String> executor;

        Tool(String name, String description, JsonObject inputSchema, Function<JsonObject, String> executor) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
            this.executor = executor;
        }

    }

    private static final Map<String, Tool> tools = new LinkedHashMap<>();

    static {
        register("search_nanopubs",
                "Free-text search over published nanopublications. Returns matching nanopub URIs with labels and dates.",
                schema(prop("query", "string", "free-text search query", true),
                        prop("pubkey", "string", "optional: restrict to nanopubs signed with this public key", false)),
                McpTools::searchNanopubs);
        register("get_nanopub",
                "Fetch a nanopublication by URI and return its full content in TriG format.",
                schema(prop("uri", "string", "the nanopublication URI (trusty URI)", true)),
                McpTools::getNanopub);
        register("list_templates",
                "List published nanopublication templates of a given kind (assertion, provenance, or pubinfo).",
                schema(prop("type", "string", "template kind: 'assertion' (default), 'provenance', or 'pubinfo'", false)),
                McpTools::listTemplates);
        register("get_template",
                "Fetch a nanopublication template by URI: returns its label, description, and full TriG content of its latest version.",
                schema(prop("uri", "string", "the template URI (nanopublication URI)", true)),
                McpTools::getTemplate);
        register("run_query",
                "Run a published grlc/nanopub query by its full ID (e.g. 'RA.../get-most-recent-nanopubs') with named parameters. Returns the result rows.",
                schema(prop("query_id", "string", "full query ID: artifact code, slash, query name", true),
                        prop("params", "object", "query parameters as name/value string pairs", false)),
                McpTools::runQuery);
    }

    /**
     * Get the tool with the given name, or null if there is none.
     *
     * @param name the tool name
     * @return the tool or null
     */
    public static Tool get(String name) {
        return tools.get(name);
    }

    /**
     * Get all registered tools.
     *
     * @return the tools in registration order
     */
    public static Iterable<Tool> getAll() {
        return tools.values();
    }

    private static void register(String name, String description, JsonObject inputSchema, Function<JsonObject, String> executor) {
        tools.put(name, new Tool(name, description, inputSchema, executor));
    }

    private static String searchNanopubs(JsonObject args) {
        String query = requireString(args, "query");
        Multimap<String, String> params = HashMultimap.create();
        params.put("query", query);
        if (args.has("pubkey")) params.put("pubkey", args.get("pubkey").getAsString());
        return rowsToJson(forcedGet(new QueryRef(QueryApiAccess.FULLTEXT_SEARCH, params)));
    }

    private static String getNanopub(JsonObject args) {
        String uri = requireString(args, "uri");
        Nanopub np = Utils.getAsNanopub(uri);
        if (np == null) {
            throw new IllegalArgumentException("Not a known nanopublication URI: " + uri);
        }
        return toTrig(np);
    }

    private static String listTemplates(JsonObject args) {
        String type = args.has("type") ? args.get("type").getAsString() : "assertion";
        TemplateData td = TemplateData.get();
        var rows = switch (type) {
            case "assertion" -> td.getAssertionTemplates();
            case "provenance" -> td.getProvenanceTemplates();
            case "pubinfo" -> td.getPubInfoTemplates();
            default -> throw new IllegalArgumentException("Unknown template type: " + type);
        };
        JsonArray array = new JsonArray();
        for (ApiResponseEntry row : rows) {
            if (array.size() >= MAX_ROWS) break;
            array.add(entryToJson(row));
        }
        return array.toString();
    }

    private static String getTemplate(JsonObject args) {
        String uri = requireString(args, "uri");
        Template template = TemplateData.get().getTemplate(uri);
        if (template == null) {
            throw new IllegalArgumentException("Not a known template URI: " + uri);
        }
        JsonObject result = new JsonObject();
        result.addProperty("id", template.getId());
        result.addProperty("label", template.getLabel());
        result.addProperty("description", template.getDescription());
        result.addProperty("trig", toTrig(template.getNanopub()));
        return result.toString();
    }

    private static String runQuery(JsonObject args) {
        String queryId = requireString(args, "query_id");
        if (!queryId.matches("^RA[A-Za-z0-9-_]{43}/[^/#]+$")) {
            throw new IllegalArgumentException("Not a full query ID of the form 'RA.../query-name': " + queryId);
        }
        Multimap<String, String> params = HashMultimap.create();
        if (args.has("params")) {
            for (Map.Entry<String, com.google.gson.JsonElement> e : args.getAsJsonObject("params").entrySet()) {
                params.put(e.getKey(), e.getValue().getAsString());
            }
        }
        return rowsToJson(forcedGet(new QueryRef(queryId, params)));
    }

    private static ApiResponse forcedGet(QueryRef queryRef) {
        try {
            ApiResponse response = QueryApiAccess.get(queryRef);
            if (response == null) throw new RuntimeException("Query returned no response: " + queryRef.getQueryId());
            return response;
        } catch (Exception ex) {
            throw new RuntimeException("Query failed: " + ex.getMessage(), ex);
        }
    }

    private static String rowsToJson(ApiResponse response) {
        JsonObject result = new JsonObject();
        JsonArray rows = new JsonArray();
        for (ApiResponseEntry row : response.getData()) {
            if (rows.size() >= MAX_ROWS) break;
            rows.add(entryToJson(row));
        }
        result.addProperty("totalRows", response.getData().size());
        result.addProperty("returnedRows", rows.size());
        result.add("rows", rows);
        return result.toString();
    }

    private static JsonObject entryToJson(ApiResponseEntry entry) {
        JsonObject o = new JsonObject();
        for (String key : entry.getKeys()) {
            o.addProperty(key, entry.get(key));
        }
        return o;
    }

    private static String toTrig(Nanopub np) {
        try {
            return NanopubUtils.writeToString(np, RDFFormat.TRIG);
        } catch (Exception ex) {
            throw new RuntimeException("Could not serialize nanopub: " + ex.getMessage(), ex);
        }
    }

    private static String requireString(JsonObject args, String name) {
        if (args == null || !args.has(name) || args.get(name).getAsString().isBlank()) {
            throw new IllegalArgumentException("Missing required argument: " + name);
        }
        return args.get(name).getAsString();
    }

    private static JsonObject schema(JsonObject... props) {
        JsonObject properties = new JsonObject();
        JsonArray required = new JsonArray();
        for (JsonObject p : props) {
            String name = p.remove("_name").getAsString();
            if (p.remove("_required").getAsBoolean()) required.add(name);
            properties.add(name, p);
        }
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        schema.add("required", required);
        return schema;
    }

    private static JsonObject prop(String name, String type, String description, boolean required) {
        JsonObject p = new JsonObject();
        p.addProperty("_name", name);
        p.addProperty("_required", required);
        p.addProperty("type", type);
        p.addProperty("description", description);
        return p;
    }

}
