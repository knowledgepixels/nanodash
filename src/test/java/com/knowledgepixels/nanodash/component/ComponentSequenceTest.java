package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class ComponentSequenceTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
    }

    @Test
    void rendersAllComponentsWithSeparators() {
        List<Component> components = List.of(
                new Label("component", "First"),
                new Label("component", "Second")
        );
        ComponentSequence sequence = new ComponentSequence("sequence", ",", components);

        tester.startComponentInPage(sequence);
        tester.assertComponent("sequence", ComponentSequence.class);
        tester.assertComponent("sequence:components", ListView.class);

        // Label components
        tester.assertComponent("sequence:components:0:component", Label.class);
        tester.assertComponent("sequence:components:1:component", Label.class);
        tester.assertLabel("sequence:components:0:component", "First");
        tester.assertLabel("sequence:components:1:component", "Second");

        // Separators
        tester.assertInvisible("sequence:components:0:separator");
        tester.assertComponent("sequence:components:1:separator", Label.class);
        tester.assertLabel("sequence:components:1:separator", ",");
    }

    @Test
    void rendersWithoutSeparatorForSingleComponent() {
        List<Component> components = List.of(new Label("component", "Only"));
        ComponentSequence sequence = new ComponentSequence("sequence", ",", components);

        tester.startComponentInPage(sequence);
        tester.assertComponent("sequence:components:0", ListItem.class);
        tester.assertInvisible("sequence:components:0:separator");
        tester.assertLabel("sequence:components:0:component", "Only");
    }

    @Test
    void rendersCustomSeparatorBetweenComponents() {
        List<Component> components = List.of(
                new Label("component", "First"),
                new Label("component", "Second")
        );
        ComponentSequence sequence = new ComponentSequence("sequence", " | ", components);

        tester.startComponentInPage(sequence);
        tester.assertLabel("sequence:components:1:separator", " | ");
    }

}