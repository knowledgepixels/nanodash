package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

/**
 * The conversation behind a thread view.
 *
 * @see com.knowledgepixels.nanodash.component.QueryResultThread
 */
public class DiscussionThread implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The result column naming the response itself.
     */
    public static final String COL_RESPONSE = "reply";
    /**
     * The result column naming what a response responds to; empty for a root.
     */
    public static final String COL_RESPONDS_TO = "in_reply_to";
    /**
     * The result column naming how a response relates to what it responds to.
     */
    public static final String COL_RELATION = "relation";
    /**
     * The result column carrying the response's text.
     */
    public static final String COL_LABEL = "reply_label";
    /**
     * The result column naming the response's author.
     */
    public static final String COL_USER = "by";
    /**
     * The result column carrying the author's display name.
     */
    public static final String COL_USER_LABEL = "by_label";
    /**
     * The result column carrying the response's date.
     */
    public static final String COL_DATE = "date";

    /**
     * How a response relates to what it responds to.
     */
    public enum Relation {

        /**
         * A response that continues the conversation without agreeing or
         * disagreeing.
         */
        REPLIES("replies", "reply", "reply", "replies"),
        /**
         * A question put to what it responds to.
         */
        ASKS("asks", "question", "ask", "asks"),
        /**
         * An answer to a question.
         */
        ANSWERS("answers", "answer", "answer", "answers"),
        /**
         * A response that argues for what it responds to.
         */
        SUPPORTS("supports", "support", "support", "supports"),
        /**
         * A response that argues against what it responds to.
         */
        DISPUTES("disputes", "dispute", "dispute", "disputes"),
        /**
         * The relation a row declares that this Nanodash does not know.
         */
        RESPONDS("responds", "response", "respond", "responds to"),
        /**
         * Not a response at all: the statement a thread starts from.
         */
        STATEMENT("statement", "statement", null, "statement");

        private final String id;
        private final String noun;
        private final String actionLabel;
        private final String badge;

        Relation(String id, String noun, String actionLabel, String badge) {
            this.id = id;
            this.noun = noun;
            this.actionLabel = actionLabel;
            this.badge = badge;
        }

        /**
         * @return the word the query uses for this relation
         */
        public String getId() {
            return id;
        }

        /**
         * @return what one such response is called, e.g. "dispute"
         */
        public String getNoun() {
            return noun;
        }

        /**
         * @return the word on the button that makes such a response, or null
         * where there is no such button (a thread's root statement is not
         * something one publishes from within the thread)
         */
        public String getActionLabel() {
            return actionLabel;
        }

        /**
         * The badge above a response. Below the first level it says what is
         * being responded to as well, so that a dispute of a dispute reads
         * "disputes this dispute" rather than repeating the bare relation.
         *
         * @param parent the relation of what this responds to, or null at the
         * first level
         * @return the badge text
         */
        public String getBadge(Relation parent) {
            if (parent == null || parent == STATEMENT || this == STATEMENT) {
                return badge;
            }
            return badge + " this " + parent.getNoun();
        }

        /**
         * @return the modifier the CSS colours this relation by
         */
        public String getCssClass() {
            return "thread-" + id;
        }

        /**
         * The relation a result value names, however it spells it.
         *
         * @param value the raw {@code relation} value, an IRI or a bare word,
         * possibly null
         * @return the relation, {@link #RESPONDS} for one that is named but
         * unknown, and null for none named at all
         */
        public static Relation parse(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            String name = value.trim();
            int cut = Math.max(name.lastIndexOf('/'), name.lastIndexOf('#'));
            if (cut >= 0 && cut < name.length() - 1) {
                name = name.substring(cut + 1);
            }
            name = name.toLowerCase(Locale.ENGLISH);
            for (Relation r : values()) {
                if (r.id.equals(name) || r.noun.equals(name)) {
                    return r;
                }
            }
            return RESPONDS;
        }

    }

    /**
     * One response, with the responses to it. Carries the row it was read from,
     * so that a view action's query mappings can be applied to it like to any
     * other result row.
     */
    public static class Node implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String id;
        private final ApiResponseEntry row;
        private final Relation relation;
        private final String relationLabel;
        private final List<Node> children = new ArrayList<>();
        private Node parent;
        private int descendantCount = 0;
        private int depthBelow = 0;

        private Node(String id, ApiResponseEntry row, Relation relation, String relationLabel) {
            this.id = id;
            this.row = row;
            this.relation = relation;
            this.relationLabel = relationLabel;
        }

        /**
         * The badge this response carries: the relation it stands in, and below the first
         * level what it responds to as well ("disputes this dispute").
         *
         * @return the badge text
         */
        public String getBadge() {
            if (relation == Relation.RESPONDS && relationLabel != null) {
                Relation parent = getParentRelation();
                if (parent == null || parent == Relation.STATEMENT || parent == Relation.RESPONDS) {
                    return relationLabel;
                }
                return relationLabel + " this " + parent.getNoun();
            }
            return relation.getBadge(getParentRelation());
        }

        /**
         * @return the IRI of the response
         */
        public String getId() {
            return id;
        }

        /**
         * @return the result row this response was read from
         */
        public ApiResponseEntry getRow() {
            return row;
        }

        /**
         * @return how this responds to its parent, or
         * {@link Relation#STATEMENT} at the root
         */
        public Relation getRelation() {
            return relation;
        }

        /**
         * @return the relation of what this responds to, or null at the root
         */
        public Relation getParentRelation() {
            return parent == null ? null : parent.relation;
        }

        /**
         * @return the responses to this one, oldest first
         */
        public List<Node> getChildren() {
            return Collections.unmodifiableList(children);
        }

        /**
         * @return the responses to this one, and to those, and so on
         */
        public int getDescendantCount() {
            return descendantCount;
        }

        /**
         * @return how many levels of responses stand below this one
         */
        public int getDepthBelow() {
            return depthBelow;
        }

        /**
         * @return the response's text, or its IRI where the query gave no text
         */
        public String getLabel() {
            String label = value(COL_LABEL);
            if (label == null || label.isBlank()) {
                return id;
            }
            return label.replaceFirst("^(?:[\u00b7\u2022]\\s*)+", "");
        }

        /**
         * @return the author's IRI, or null
         */
        public String getUserIri() {
            return value(COL_USER);
        }

        /**
         * @return the author's display name, or null
         */
        public String getUserLabel() {
            return value(COL_USER_LABEL);
        }

        /**
         * @return the response's date, or null
         */
        public String getDate() {
            return value(COL_DATE);
        }

        private String value(String column) {
            if (row == null) {
                return null;
            }
            String v = row.get(column);
            return (v == null || v.isBlank()) ? null : v;
        }

    }

    private final List<Node> roots;
    private final int responseCount;
    private final int peopleCount;

    private DiscussionThread(List<Node> roots, int responseCount, int peopleCount) {
        this.roots = roots;
        this.responseCount = responseCount;
        this.peopleCount = peopleCount;
    }

    /**
     * @return the responses nothing in this answer responds to — normally the
     * one statement the discussion started from, but a query is free to return
     * several
     */
    public List<Node> getRoots() {
        return Collections.unmodifiableList(roots);
    }

    /**
     * @return how many responses the thread holds, the roots included
     */
    public int getResponseCount() {
        return responseCount;
    }

    /**
     * @return how many distinct authors the thread holds
     */
    public int getPeopleCount() {
        return peopleCount;
    }

    /**
     * @return whether the answer held nothing to show
     */
    public boolean isEmpty() {
        return roots.isEmpty();
    }

    /**
     * Reads the thread out of a query answer.
     *
     * @param response the answer, possibly null
     * @return the thread, empty where there was nothing to read
     */
    public static DiscussionThread of(ApiResponse response) {
        if (response == null || response.getData() == null) {
            return new DiscussionThread(List.of(), 0, 0);
        }
        Map<String, Node> byId = new LinkedHashMap<>();
        Map<String, String> parentIds = new HashMap<>();
        Set<String> people = new HashSet<>();
        for (ApiResponseEntry row : response.getData()) {
            String id = row.get(COL_RESPONSE);
            if (id == null || id.isBlank()) {
                continue;
            }
            if (byId.containsKey(id)) {
                continue;
            }
            String parentId = row.get(COL_RESPONDS_TO);
            String rawRelation = row.get(COL_RELATION);
            Relation relation = Relation.parse(rawRelation);
            if (parentId == null || parentId.isBlank() || parentId.equals(id)) {
                parentId = null;
                if (relation == null) {
                    relation = Relation.STATEMENT;
                }
            } else if (relation == null) {
                relation = Relation.REPLIES;
            }
            byId.put(id, new Node(id, row, relation, humanizeRelation(rawRelation)));
            if (parentId != null) {
                parentIds.put(id, parentId);
            }
            String user = row.get(COL_USER);
            if (user != null && !user.isBlank()) {
                people.add(user);
            }
        }

        List<Node> roots = new ArrayList<>();
        for (Node node : byId.values()) {
            Node parent = byId.get(parentIds.get(node.id));
            if (parent == null || createsCycle(node, parent)) {
                roots.add(node);
            } else {
                node.parent = parent;
                parent.children.add(node);
            }
        }

        for (Node node : byId.values()) {
            sortChildren(node);
        }
        roots.sort(DiscussionThread::compareByDate);
        for (Node root : roots) {
            measure(root);
        }
        return new DiscussionThread(roots, byId.size(), people.size());
    }

    /**
     * The word a query used for a relation, as a badge reads it: the local name of an IRI,
     * with the camel case vocabularies write it in turned back into words ({@code agreesWith}
     * becomes "agrees with").
     *
     * @param value the raw relation value, possibly null
     * @return the readable word, or null where the query named no relation
     */
    private static String humanizeRelation(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String name = value.trim();
        int cut = Math.max(name.lastIndexOf('/'), name.lastIndexOf('#'));
        if (cut >= 0 && cut < name.length() - 1) {
            name = name.substring(cut + 1);
        }
        return name.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ").toLowerCase(Locale.ENGLISH);
    }

    private static boolean createsCycle(Node node, Node candidateParent) {
        for (Node a = candidateParent; a != null; a = a.parent) {
            if (a == node) {
                return true;
            }
        }
        return false;
    }

    private static void sortChildren(Node node) {
        node.children.sort(DiscussionThread::compareByDate);
    }

    /**
     * Oldest first, which is how a conversation reads. Rows without a date keep
     * the order the query gave them, after the dated ones.
     */
    private static int compareByDate(Node a, Node b) {
        String da = a.getDate(), db = b.getDate();
        if (da == null && db == null) {
            return 0;
        }
        if (da == null) {
            return 1;
        }
        if (db == null) {
            return -1;
        }
        return da.compareTo(db);
    }

    /**
     * Fills in each node's descendant count and the depth of responses below
     * it.
     */
    private static void measure(Node node) {
        int count = 0;
        int depth = 0;
        for (Node child : node.children) {
            measure(child);
            count += 1 + child.descendantCount;
            depth = Math.max(depth, 1 + child.depthBelow);
        }
        node.descendantCount = count;
        node.depthBelow = depth;
    }

}
