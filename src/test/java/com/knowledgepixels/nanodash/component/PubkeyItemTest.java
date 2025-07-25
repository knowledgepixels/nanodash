package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

class PubkeyItemTest {

    private WicketTester tester;
    private NanodashSession nanodashSessionMock;
    private MockedStatic<NanodashSession> nanodashSessionMockedStatic;


    @BeforeEach
    void setUp() {
        tester = new WicketTester();
        nanodashSessionMock = mock(NanodashSession.class);
        nanodashSessionMockedStatic = mockStatic(NanodashSession.class);
    }

    @AfterEach
    void tearDown() {
        if (nanodashSessionMockedStatic != null) {
            nanodashSessionMockedStatic.close();
        }
        tester.destroy();
    }

    @Test
    void rendersPlaceholderWhenPubkeyIsNull() {
        PubkeyItem pubkeyItem = new PubkeyItem("pubkeyItem", null);

        tester.startComponentInPage(pubkeyItem);
        tester.assertLabel("pubkeyItem:label", "..");
        tester.assertLabel("pubkeyItem:notes", "");

    }

    @Test
    void rendersPlaceholderWhenPubkeyIsEmpty() {
        PubkeyItem pubkeyItem = new PubkeyItem("pubkeyItem", "");

        tester.startComponentInPage(pubkeyItem);
        tester.assertLabel("pubkeyItem:label", "..");
        tester.assertLabel("pubkeyItem:notes", "");
    }

    @Test
    void rendersShortNameAndApprovedNotesWhenPubkeyIsApproved() {
        nanodashSessionMockedStatic.when(NanodashSession::get).thenReturn(nanodashSessionMock);
        when(nanodashSessionMock.isPubkeyApproved()).thenReturn(true);

        PubkeyItem pubkeyItem = new PubkeyItem("pubkeyItem", "samplePubkey");

        tester.startComponentInPage(pubkeyItem);
        tester.assertLabel("pubkeyItem:label", Utils.getShortPubkeyName("samplePubkey"));
        tester.assertLabel("pubkeyItem:notes", "It is <strong class=\"positive\">approved</strong> by the community.");
    }

    @Test
    void rendersShortNameAndNotApprovedNotesWhenPubkeyIsNotApproved() {
        nanodashSessionMockedStatic.when(NanodashSession::get).thenReturn(nanodashSessionMock);
        when(nanodashSessionMock.isPubkeyApproved()).thenReturn(false);

        PubkeyItem pubkeyItem = new PubkeyItem("pubkeyItem", "samplePubkey");

        tester.startComponentInPage(pubkeyItem);
        tester.assertLabel("pubkeyItem:label", Utils.getShortPubkeyName("samplePubkey"));
        tester.assertLabel("pubkeyItem:notes", "It is so far <strong class=\"negative\">not approved</strong> by the community.");
    }

}