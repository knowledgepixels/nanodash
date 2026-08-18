package com.knowledgepixels.nanodash.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An About panel is built in a follow-up Ajax request while any of the views it resolves is
 * still unresolved, and directly once they are all in hand. The page decides that from the
 * panel's {@code REQUIRED_VIEWS}, so a list that does not match the panel's actual
 * {@code View.get} calls breaks the tab in one of two ways: a view it forgot to list can
 * block the page render, and a view listed that the panel never resolves never becomes
 * cached, so the tab reloads over Ajax on every single visit.
 * <p>
 * That is not something the compiler can check, and all four pages had drifted, so it is
 * checked here: the panel's source is read and its {@code View.get(CONSTANT)} calls are
 * compared with what {@code REQUIRED_VIEWS} names.
 */
class AboutPanelRequiredViewsTest {

    private static final Path SOURCE_DIR = Path.of("src/main/java/com/knowledgepixels/nanodash/component");

    private static final Pattern VIEW_GET = Pattern.compile("View\\.get\\(([A-Z][A-Z0-9_]*)\\)");
    private static final Pattern REQUIRED = Pattern.compile(
            "REQUIRED_VIEWS\\s*=\\s*\\{(.*?)}", Pattern.DOTALL);

    private void assertListsMatch(String panel) throws IOException {
        Path source = SOURCE_DIR.resolve(panel + ".java");
        assertTrue(Files.exists(source), "cannot find " + source.toAbsolutePath());
        String code = Files.readString(source);

        Set<String> resolved = new TreeSet<>();
        Matcher m = VIEW_GET.matcher(code);
        while (m.find()) resolved.add(m.group(1));

        Matcher r = REQUIRED.matcher(code);
        assertTrue(r.find(), panel + " should declare REQUIRED_VIEWS");
        Set<String> declared = new TreeSet<>();
        for (String entry : r.group(1).split(",")) {
            String name = entry.trim();
            if (!name.isEmpty()) declared.add(name);
        }

        assertEquals(resolved, declared,
                panel + "'s REQUIRED_VIEWS must name exactly the views it resolves with View.get");
    }

    @Test
    @DisplayName("every About panel's REQUIRED_VIEWS matches the views it actually resolves")
    void requiredViewsMatchTheResolvedOnes() throws IOException {
        assertListsMatch("AboutSpacePanel");
        assertListsMatch("AboutUserPanel");
        assertListsMatch("AboutResourcePanel");
        assertListsMatch("AboutPartPanel");
    }

}
