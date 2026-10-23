package com.knowledgepixels.nanodash.component;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A result cell naming a person is a plain {@code href} rather than a {@link NanodashLink},
 * so neither its routing nor the navigation-parameter behaviors reach it. That is how the
 * people a space lists — its organizers, its programme committee — kept leading to the
 * general profile after links everywhere else had learnt to lead to the person's page in
 * that space (issue #822).
 * <p>
 * Both cell builders now go through {@code QueryResult.agentCellUrl}, which asks the same
 * question. A hand-built {@code /user?id=} would quietly opt out again, so there must not
 * be one.
 */
class AgentCellLinkRoutingTest {

    private static final List<Path> CELL_BUILDERS = List.of(
            Path.of("src/main/java/com/knowledgepixels/nanodash/component/QueryResultList.java"),
            Path.of("src/main/java/com/knowledgepixels/nanodash/component/QueryResultItemList.java"));

    @Test
    void noCellBuilderHandBuildsALinkToTheGeneralUserPage() throws IOException {
        for (Path builder : CELL_BUILDERS) {
            String source = Files.readString(builder);
            assertFalse(source.contains("UserPage.MOUNT_PATH"),
                    builder + " builds a user-page link by hand; route it through agentCellUrl instead");
            assertTrue(source.contains("agentCellUrl("),
                    builder + " no longer routes its agent cells through agentCellUrl");
        }
    }

}
