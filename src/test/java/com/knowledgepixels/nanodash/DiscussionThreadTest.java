package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reading a conversation out of a query answer: what the rows say the thread is, and what the
 * thread does with rows that say something impossible.
 */
class DiscussionThreadTest {

    private static final String ROOT = "http://example.org/statement";

    @Test
    void responsesNestUnderWhatTheyRespondTo() {
        DiscussionThread thread = DiscussionThread.of(response(
                row(ROOT, null, null, "Coffee reduces the risk of type 2 diabetes", "2026-08-14"),
                row("http://example.org/d1", ROOT, "disputes", "The effect disappears when adjusting", "2026-08-16"),
                row("http://example.org/d2", "http://example.org/d1", "disputes", "The re-analysis excluded over-60s", "2026-08-17")));

        assertEquals(1, thread.getRoots().size());
        DiscussionThread.Node root = thread.getRoots().get(0);
        assertEquals(ROOT, root.getId());
        assertEquals(DiscussionThread.Relation.STATEMENT, root.getRelation());
        assertEquals(1, root.getChildren().size());

        DiscussionThread.Node dispute = root.getChildren().get(0);
        assertEquals(DiscussionThread.Relation.DISPUTES, dispute.getRelation());
        assertEquals(1, dispute.getChildren().size());
        assertEquals("http://example.org/d2", dispute.getChildren().get(0).getId());

        assertEquals(2, root.getDescendantCount(), "both responses hang below the statement");
        assertEquals(2, root.getDepthBelow());
        assertEquals(3, thread.getResponseCount());
    }

    @Test
    void aResponseOfAResponseSaysWhatItRespondsTo() {
        DiscussionThread thread = DiscussionThread.of(response(
                row(ROOT, null, null, "A statement", "2026-08-14"),
                row("http://example.org/d1", ROOT, "disputes", "A dispute", "2026-08-16"),
                row("http://example.org/d2", "http://example.org/d1", "disputes", "A dispute of it", "2026-08-17")));

        DiscussionThread.Node root = thread.getRoots().get(0);
        DiscussionThread.Node dispute = root.getChildren().get(0);
        DiscussionThread.Node deeper = dispute.getChildren().get(0);

        assertEquals("statement", root.getRelation().getBadge(null));
        assertEquals("disputes", dispute.getRelation().getBadge(dispute.getParentRelation()));
        assertEquals("disputes this dispute", deeper.getRelation().getBadge(deeper.getParentRelation()));
    }

    @Test
    void responsesOfOneLevelReadOldestFirst() {
        DiscussionThread thread = DiscussionThread.of(response(
                row(ROOT, null, null, "A statement", "2026-08-14"),
                row("http://example.org/late", ROOT, "replies", "Later", "2026-09-01"),
                row("http://example.org/early", ROOT, "replies", "Earlier", "2026-08-15")));

        List<DiscussionThread.Node> children = thread.getRoots().get(0).getChildren();
        assertEquals(List.of("http://example.org/early", "http://example.org/late"),
                children.stream().map(DiscussionThread.Node::getId).toList());
    }

    @Test
    void aRelationIsReadWhicheverWayTheQuerySpellsIt() {
        assertEquals(DiscussionThread.Relation.DISPUTES, DiscussionThread.Relation.parse("disputes"));
        assertEquals(DiscussionThread.Relation.DISPUTES, DiscussionThread.Relation.parse("Dispute"));
        assertEquals(DiscussionThread.Relation.DISPUTES,
                DiscussionThread.Relation.parse("https://w3id.org/kpxl/gen/terms/disputes"));
        assertEquals(DiscussionThread.Relation.ASKS, DiscussionThread.Relation.parse("question"));
        assertNull(DiscussionThread.Relation.parse(""), "nothing named is not a relation");
        assertEquals(DiscussionThread.Relation.RESPONDS, DiscussionThread.Relation.parse("grumbles-at"),
                "a relation this Nanodash does not know is still a response");
    }

    @Test
    void aResponseWhoseParentIsNotInTheAnswerStandsOnItsOwn() {
        // The query was asked for one branch: the response it starts from names a parent that
        // the answer does not carry. Dropping it would leave the branch invisible.
        DiscussionThread thread = DiscussionThread.of(response(
                row("http://example.org/d1", "http://example.org/elsewhere", "disputes", "A dispute", "2026-08-16"),
                row("http://example.org/d2", "http://example.org/d1", "replies", "A reply to it", "2026-08-17")));

        assertEquals(1, thread.getRoots().size());
        assertEquals("http://example.org/d1", thread.getRoots().get(0).getId());
        assertEquals(1, thread.getRoots().get(0).getChildren().size());
    }

    @Test
    void aCycleIsBrokenRatherThanFollowed() {
        // Nothing stops a nanopublication from responding to something that responds to it;
        // rendering that as a tree would not terminate.
        DiscussionThread thread = DiscussionThread.of(response(
                row("http://example.org/a", "http://example.org/b", "replies", "A", "2026-08-16"),
                row("http://example.org/b", "http://example.org/a", "replies", "B", "2026-08-17")));

        assertEquals(1, thread.getRoots().size(), "one of the two becomes the root");
        DiscussionThread.Node root = thread.getRoots().get(0);
        assertEquals(1, root.getDescendantCount());
        assertNotNull(root.getChildren().get(0));
        assertTrue(root.getChildren().get(0).getChildren().isEmpty(), "and the loop is not closed again");
    }

    @Test
    void peopleAreCountedOnce() {
        // Two of the three are by the same person.
        DiscussionThread thread = DiscussionThread.of(response(
                by(row(ROOT, null, null, "A statement", "2026-08-14"), "http://example.org/elle"),
                by(row("http://example.org/r1", ROOT, "replies", "One", "2026-08-15"), "http://example.org/elle"),
                by(row("http://example.org/r2", ROOT, "replies", "Two", "2026-08-16"), "http://example.org/regina")));

        assertEquals(3, thread.getResponseCount());
        assertEquals(2, thread.getPeopleCount());
    }

    @Test
    void theColumnsArePublishedGetThreadQuerysOwn() {
        // Pinned against the query the view actually runs
        // (https://w3id.org/np/RAhdldevdeHe4nb9fM3CKA_cUnufQPq_xj4fuRyjTkq1o/get-thread), whose
        // columns these are. Reading rows under any other names is how the view first came up
        // empty on a resource that has four replies.
        ApiResponse response = new ApiResponse();
        response.setHeader(new String[]{"level", "reply", "reply_label", "relation", "in_reply_to",
                "in_reply_to_label", "by", "by_label", "date", "reply_target", "np", "np_label"});
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("reply", "http://example.org/reply1");
        entry.add("reply_label", "Example comment: a filter would help");
        entry.add("relation", "discusses");
        entry.add("in_reply_to", "http://example.org/suggestion");
        entry.add("by", "https://orcid.org/0000-0002-1267-0234");
        entry.add("by_label", "Tobias Kuhn");
        entry.add("date", "2026-09-01");
        response.add(entry);

        DiscussionThread thread = DiscussionThread.of(response);
        assertEquals(1, thread.getRoots().size(), "the reply is read");
        DiscussionThread.Node node = thread.getRoots().get(0);
        assertEquals("http://example.org/reply1", node.getId());
        assertEquals("Example comment: a filter would help", node.getLabel());
        assertEquals("Tobias Kuhn", node.getUserLabel());
        assertEquals("2026-09-01", node.getDate());
        assertEquals(1, thread.getPeopleCount());
    }

    @Test
    void aRelationTheVocabularyGrewKeepsItsOwnWord() {
        // CiTO alone has dozens of these; the badge says what the query said rather than
        // flattening every unknown one to "responds to".
        DiscussionThread thread = DiscussionThread.of(response(
                row(ROOT, null, null, "A statement", "2026-08-14"),
                row("http://example.org/r1", ROOT, "agreesWith", "One", "2026-08-15"),
                row("http://example.org/r2", ROOT, null, "Two", "2026-08-16")));

        List<DiscussionThread.Node> children = thread.getRoots().get(0).getChildren();
        assertEquals("agrees with", children.get(0).getBadge(), "camel case reads as words");
        assertEquals("replies", children.get(1).getBadge(),
                "and a response that names no relation is a plain reply");
    }

    @Test
    void aQuerysOwnIndentationIsNotShownTwice() {
        // get-thread prefixes deeper texts with "· · " for a flat list; here the nesting is
        // the indentation.
        DiscussionThread thread = DiscussionThread.of(response(
                row(ROOT, null, null, "· · A nested reply", "2026-08-14")));

        assertEquals("A nested reply", thread.getRoots().get(0).getLabel());
    }

    @Test
    void anAnswerWithNothingInItIsAnEmptyThread() {
        DiscussionThread thread = DiscussionThread.of(response());
        assertTrue(thread.isEmpty());
        assertEquals(0, thread.getResponseCount());
        assertTrue(thread.getRoots().isEmpty());
    }

    @Test
    void aRowWithoutAResponseIsSkipped() {
        DiscussionThread thread = DiscussionThread.of(response(
                row(ROOT, null, null, "A statement", "2026-08-14"),
                row("", ROOT, "replies", "Nameless", "2026-08-15")));

        assertEquals(1, thread.getResponseCount());
        assertTrue(thread.getRoots().get(0).getChildren().isEmpty());
    }

    private static ApiResponse response(ApiResponseEntry... rows) {
        ApiResponse response = new ApiResponse();
        response.setHeader(new String[]{DiscussionThread.COL_RESPONSE, DiscussionThread.COL_RESPONDS_TO,
                DiscussionThread.COL_RELATION, DiscussionThread.COL_LABEL, DiscussionThread.COL_DATE,
                DiscussionThread.COL_USER});
        for (ApiResponseEntry row : rows) response.add(row);
        return response;
    }

    private static ApiResponseEntry row(String id, String respondsTo, String relation, String label, String date) {
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add(DiscussionThread.COL_RESPONSE, id == null ? "" : id);
        entry.add(DiscussionThread.COL_RESPONDS_TO, respondsTo == null ? "" : respondsTo);
        entry.add(DiscussionThread.COL_RELATION, relation == null ? "" : relation);
        entry.add(DiscussionThread.COL_LABEL, label);
        entry.add(DiscussionThread.COL_DATE, date);
        return entry;
    }

    private static ApiResponseEntry by(ApiResponseEntry row, String userIri) {
        row.add(DiscussionThread.COL_USER, userIri);
        return row;
    }

}
