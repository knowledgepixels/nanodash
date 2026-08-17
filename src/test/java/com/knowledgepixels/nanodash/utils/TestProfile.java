package com.knowledgepixels.nanodash.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.extra.security.MakeKeys;
import org.nanopub.extra.security.SignatureAlgorithm;

/**
 * Gives the tests a nanopub profile of their own -- an ORCID and a signing key
 * under a home directory in {@code target/} -- so that they do not depend on
 * whoever runs them having set one up.
 * <p>
 * Anything that publishes reads the profile from {@code ~/.nanopub}: without it
 * {@code NanodashSession.getUserIri()} is null, the provenance template's
 * "attributed to" field has nothing to be filled with, and the publish form
 * never validates. That is the difference between a developer's machine, where
 * a profile is usually present, and CI, where it is not, and it makes a test
 * that submits a publish form pass locally and fail there.
 * <p>
 * Call {@link #install()} before the first {@code WicketTester} of a test. It
 * is idempotent and safe to call from every test: the keys are generated once
 * per build.
 */
public final class TestProfile {

    private TestProfile() {
    }

    private static final String TEST_HOME_DIRECTORY = "test-home";

    /**
     * A reserved-for-documentation ORCID, so it can never collide with a real
     * account.
     */
    public static final String ORCID = "0000-0002-1825-0097";

    private static final IRI USER_IRI = SimpleValueFactory.getInstance().createIRI("https://orcid.org/" + ORCID);

    private static boolean installed;

    /**
     * Points {@code user.home} at a test home holding this profile, creating it
     * the first time.
     * <p>
     * The real home is deliberately left alone: tests must neither read a
     * developer's keys nor write into their profile.
     */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        try {
            Path home = testHome();
            Path nanopubDir = home.resolve(".nanopub");
            Files.createDirectories(nanopubDir);
            Files.writeString(nanopubDir.resolve("orcid"), ORCID + "\n", StandardCharsets.UTF_8);
            File key = nanopubDir.resolve("id_rsa").toFile();
            if (!key.exists()) {
                // Writes id_rsa and id_rsa.pub, which is where NanodashSession looks for them.
                MakeKeys.make(nanopubDir.resolve("id").toString(), SignatureAlgorithm.RSA);
            }
            System.setProperty("user.home", home.toString());
            installed = true;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not set up the test nanopub profile", ex);
        }
    }

    /**
     * Returns the user the tests publish as.
     */
    public static IRI userIri() {
        return USER_IRI;
    }

    /**
     * Returns the home directory the test profile lives in, under the build
     * directory so that it is cleaned along with everything else.
     */
    private static Path testHome() {
        String buildDir = System.getProperty("project.build.directory", "target");
        return Paths.get(buildDir, TEST_HOME_DIRECTORY).toAbsolutePath();
    }

}
