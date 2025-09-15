package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.component.NanopubResults;
import com.knowledgepixels.nanodash.component.PublishForm;
import com.knowledgepixels.nanodash.page.OrcidLoginPage;
import com.knowledgepixels.nanodash.page.ProfilePage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.xml.bind.DatatypeConverter;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.extra.security.*;
import org.nanopub.extra.setting.IntroNanopub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Represents a session in the Nanodash application.
 */
public class NanodashSession extends WebSession {

    private static final long serialVersionUID = -7920814788717089213L;
    private transient HttpSession httpSession;
    private static final Logger logger = LoggerFactory.getLogger(NanodashSession.class);

    /**
     * Retrieves the current Nanodash session.
     *
     * @return The current NanodashSession instance.
     */
    public static NanodashSession get() {
        return (NanodashSession) Session.get();
    }

    /**
     * Constructs a new NanodashSession for the given request.
     * Initializes the HTTP session and loads profile information.
     *
     * @param request The HTTP request.
     */
    public NanodashSession(Request request) {
        super(request);
        httpSession = ((HttpServletRequest) request.getContainerRequest()).getSession();
        bind();
        loadProfileInfo();
    }

    private static ValueFactory vf = SimpleValueFactory.getInstance();

//	private IntroExtractor introExtractor;

    private String userDir = System.getProperty("user.home") + "/.nanopub/";
    private NanopubResults.ViewMode nanopubResultsViewMode = NanopubResults.ViewMode.GRID;

    private KeyPair keyPair;
    private IRI userIri;
    private ConcurrentMap<IRI, IntroNanopub> introNps;
//	private Boolean isOrcidLinked;
//	private String orcidLinkError;

    private Integer localIntroCount = null;
    private IntroNanopub localIntro = null;

    private Date lastTimeIntroPublished = null;

    // We should store here some sort of form model and not the forms themselves, but I couldn't figure
    // how to do it, so doing it like this for the moment...
    private ConcurrentMap<String, PublishForm> formMap = new ConcurrentHashMap<>();

    /**
     * Associates a form object with a specific ID.
     *
     * @param formObjId The ID of the form object.
     * @param formObj   The form object to associate.
     */
    public void setForm(String formObjId, PublishForm formObj) {
        formMap.put(formObjId, formObj);
    }

    /**
     * Checks if a form object with the given ID exists.
     *
     * @param formObjId The ID of the form object.
     * @return True if the form object exists, false otherwise.
     */
    public boolean hasForm(String formObjId) {
        return formMap.containsKey(formObjId);
    }

    /**
     * Retrieves the form object associated with the given ID.
     *
     * @param formObjId The ID of the form object.
     * @return The associated form object, or null if not found.
     */
    public PublishForm getForm(String formObjId) {
        return formMap.get(formObjId);
    }

    /**
     * Loads profile information for the user.
     * Initializes user-related data such as keys and introductions.
     */
    public void loadProfileInfo() {
        localIntroCount = null;
        localIntro = null;
        NanodashPreferences prefs = NanodashPreferences.get();
        if (prefs.isOrcidLoginMode()) {
            File usersDir = new File(System.getProperty("user.home") + "/.nanopub/nanodash-users/");
            if (!usersDir.exists()) usersDir.mkdir();
        }
        if (userIri == null && !prefs.isReadOnlyMode() && !prefs.isOrcidLoginMode()) {
            if (getOrcidFile().exists()) {
                try {
                    String orcid = FileUtils.readFileToString(getOrcidFile(), StandardCharsets.UTF_8).trim();
                    //String orcid = Files.readString(orcidFile.toPath(), StandardCharsets.UTF_8).trim();
                    if (orcid.matches(ProfilePage.ORCID_PATTERN)) {
                        userIri = vf.createIRI("https://orcid.org/" + orcid);
                        if (httpSession != null) httpSession.setMaxInactiveInterval(24 * 60 * 60);  // 24h
                    }
                } catch (IOException ex) {
                    logger.error("Couldn't read ORCID file", ex);
                }
            }
        }
        if (userIri != null && keyPair == null) {
            File keyFile = getKeyFile();
            if (keyFile.exists()) {
                try {
                    keyPair = SignNanopub.loadKey(keyFile.getPath(), SignatureAlgorithm.RSA);
                } catch (Exception ex) {
                    logger.error("Couldn't load key pair", ex);
                }
            } else {
                // Automatically generate new keys
                makeKeys();
            }
        }
        if (userIri != null && keyPair != null && introNps == null) {
            introNps = new ConcurrentHashMap<>(User.getIntroNanopubs(getPubkeyString()));
        }
//		checkOrcidLink();
    }

    /**
     * Checks if the user's profile is complete.
     *
     * @return True if the profile is complete, false otherwise.
     */
    public boolean isProfileComplete() {
        return userIri != null && keyPair != null && introNps != null;
    }

    /**
     * Redirects the user to the login page if their profile is incomplete.
     *
     * @param path       The path to redirect to after login.
     * @param parameters The page parameters for the redirect.
     */
    public void redirectToLoginIfNeeded(String path, PageParameters parameters) {
        String loginUrl = getLoginUrl(path, parameters);
        if (loginUrl == null) return;
        throw new RedirectToUrlException(loginUrl);
    }

    /**
     * Retrieves the login URL for the user.
     *
     * @param path       The path to redirect to after login.
     * @param parameters The page parameters for the redirect.
     * @return The login URL, or null if the user is already logged in.
     */
    public String getLoginUrl(String path, PageParameters parameters) {
        if (isProfileComplete()) return null;
        if (NanodashPreferences.get().isOrcidLoginMode()) {
            return OrcidLoginPage.getOrcidLoginUrl(path, parameters);
        } else {
            return ProfilePage.MOUNT_PATH;
        }
    }

    /**
     * Retrieves the public key as a Base64-encoded string.
     *
     * @return The public key string, or null if the key pair is not set.
     */
    public String getPubkeyString() {
        if (keyPair == null) return null;
        return DatatypeConverter.printBase64Binary(keyPair.getPublic().getEncoded()).replaceAll("\\s", "");
    }

    /**
     * Retrieves the public key hash for the user.
     *
     * @return The SHA-256 hash of the public key, or null if the public key is not set.
     */
    public String getPubkeyhash() {
        String pubkey = getPubkeyString();
        if (pubkey == null) return null;
        return Utils.createSha256HexHash(pubkey);
    }

    /**
     * Checks if the user's public key is approved.
     *
     * @return True if the public key is approved, false otherwise.
     */
    public boolean isPubkeyApproved() {
        if (keyPair == null || userIri == null) return false;
        return User.isApprovedPubkeyhashForUser(getPubkeyhash(), userIri);
    }

    /**
     * Retrieves the user's key pair.
     *
     * @return The key pair.
     */
    public KeyPair getKeyPair() {
        return keyPair;
    }

    /**
     * Generates a new key pair for the user.
     */
    public void makeKeys() {
        try {
            MakeKeys.make(getKeyFile().getAbsolutePath().replaceFirst("_rsa$", ""), SignatureAlgorithm.RSA);
            keyPair = SignNanopub.loadKey(getKeyFile().getPath(), SignatureAlgorithm.RSA);
        } catch (Exception ex) {
            logger.error("Couldn't create key pair", ex);
        }
    }

    /**
     * Retrieves the user's IRI.
     *
     * @return The user's IRI, or null if not set.
     */
    public IRI getUserIri() {
        return userIri;
    }

    /**
     * Retrieves the user's introduction nanopublications.
     *
     * @return A list of user's introduction nanopublications.
     */
    public List<IntroNanopub> getUserIntroNanopubs() {
        return User.getIntroNanopubs(userIri);
    }

    /**
     * Counts the number of local introduction nanopublications.
     *
     * @return The count of local introduction nanopublications.
     */
    public int getLocalIntroCount() {
        if (localIntroCount == null) {
            localIntroCount = 0;
            for (IntroNanopub inp : getUserIntroNanopubs()) {
                if (isIntroWithLocalKey(inp)) {
                    localIntroCount++;
                    localIntro = inp;
                }
            }
            if (localIntroCount > 1) localIntro = null;
        }
        return localIntroCount;
    }

    /**
     * Retrieves the local introduction nanopublication.
     *
     * @return The local introduction nanopublication, or null if not found.
     */
    public IntroNanopub getLocalIntro() {
        getLocalIntroCount();
        return localIntro;
    }

    /**
     * Checks if the given introduction nanopublication is associated with the local key.
     *
     * @param inp The introduction nanopublication.
     * @return True if associated with the local key, false otherwise.
     */
    public boolean isIntroWithLocalKey(IntroNanopub inp) {
        IRI location = Utils.getLocation(inp);
        NanopubSignatureElement el = Utils.getNanopubSignatureElement(inp);
        String siteUrl = NanodashPreferences.get().getWebsiteUrl();
        if (location != null && siteUrl != null) {
            String l = location.stringValue();
            // TODO: Solve the name change recognition in a better way:
            if (!l.equals(siteUrl) && !l.replace("nanobench", "nanodash").equals(siteUrl)) return false;
        }
        if (!getPubkeyString().equals(el.getPublicKeyString())) return false;
        for (KeyDeclaration kd : inp.getKeyDeclarations()) {
            if (getPubkeyString().equals(kd.getPublicKeyString())) return true;
        }
        return false;
    }

    /**
     * Sets the user's ORCID identifier.
     *
     * @param orcid The ORCID identifier.
     */
    public void setOrcid(String orcid) {
        if (!orcid.matches(ProfilePage.ORCID_PATTERN)) {
            throw new RuntimeException("Illegal ORCID identifier: " + orcid);
        }
        if (NanodashPreferences.get().isOrcidLoginMode()) {
            userDir = System.getProperty("user.home") + "/.nanopub/nanodash-users/" + orcid + "/";
            File f = new File(userDir);
            if (!f.exists()) f.mkdir();
        } else {
            try {
                FileUtils.writeStringToFile(getOrcidFile(), orcid + "\n", StandardCharsets.UTF_8);
                //			Files.writeString(orcidFile.toPath(), orcid + "\n");
            } catch (IOException ex) {
                logger.error("Couldn't write ORCID file", ex);
            }
        }
        userIri = vf.createIRI("https://orcid.org/" + orcid);
        loadProfileInfo();
        if (httpSession != null) httpSession.setMaxInactiveInterval(24 * 60 * 60);  // 24h
    }

    /**
     * Logs out the user and invalidates the session.
     */
    public void logout() {
        userIri = null;
        invalidateNow();
    }

    /**
     * Retrieves the user's introduction nanopublications as a map.
     *
     * @return A map of introduction nanopublications.
     */
    public Map<IRI, IntroNanopub> getIntroNanopubs() {
        return introNps;
    }

//	public void checkOrcidLink() {
//		if (isOrcidLinked == null && userIri != null) {
//			orcidLinkError = "";
//			introExtractor = null;
//			try {
//				introExtractor = IntroNanopub.extract(userIri.stringValue(), null);
//				if (introExtractor.getIntroNanopub() == null) {
//					orcidLinkError = "ORCID account is not linked.";
//					isOrcidLinked = false;
//				} else {
//					IntroNanopub inp = IntroNanopub.get(userIri.stringValue(), introExtractor);
//					if (introNps != null && introNps.containsKey(inp.getNanopub().getUri())) {
//						// TODO: also check whether introduction contains local key
//						isOrcidLinked = true;
//					} else {
//						isOrcidLinked = false;
//						orcidLinkError = "Error: ORCID is linked to another introduction nanopublication.";
//					}
//				}
//			} catch (Exception ex) {
//				logger.error("ORCID check failed");
//				orcidLinkError = "ORCID check failed.";
//			}
//		}
//	}
//
//	public void resetOrcidLinked() {
//		isOrcidLinked = null;
//	}
//
//	public boolean isOrcidLinked() {
//		checkOrcidLink();
//		return isOrcidLinked != null && isOrcidLinked == true;
//	}
//
//	public String getOrcidLinkError() {
//		return orcidLinkError;
//	}
//
//	public String getOrcidName() {
//		if (introExtractor == null || introExtractor.getName() == null) return null;
//		if (introExtractor.getName().trim().isEmpty()) return null;
//		return introExtractor.getName();
//	}

    /**
     * Retrieves the file for storing the user's ORCID identifier.
     *
     * @return The ORCID file.
     */
    private File getOrcidFile() {
        return new File(userDir + "orcid");
    }

    /**
     * Retrieves the file for storing the user's private key.
     *
     * @return The key file.
     */
    public File getKeyFile() {
        return new File(userDir + "id_rsa");
    }

    /**
     * Sets the time when the introduction was last published.
     */
    public void setIntroPublishedNow() {
        lastTimeIntroPublished = new Date();
    }

    /**
     * Checks if the introduction has been published.
     *
     * @return True if the introduction has been published, false otherwise.
     */
    public boolean hasIntroPublished() {
        return lastTimeIntroPublished != null;
    }

    /**
     * Calculates the time since the last introduction was published.
     *
     * @return The time in milliseconds since the last introduction was published, or Long.MAX_VALUE if it has never been published.
     */
    public long getTimeSinceLastIntroPublished() {
        if (lastTimeIntroPublished == null) return Long.MAX_VALUE;
        return new Date().getTime() - lastTimeIntroPublished.getTime();
    }

    /**
     * Sets the view mode for displaying nanopublication results.
     *
     * @param viewMode The desired view mode (e.g., GRID or LIST).
     */
    public void setNanopubResultsViewMode(NanopubResults.ViewMode viewMode) {
        this.nanopubResultsViewMode = viewMode;
    }

    /**
     * Retrieves the current view mode for displaying nanopublication results.
     *
     * @return The current view mode.
     */
    public NanopubResults.ViewMode getNanopubResultsViewMode() {
        return this.nanopubResultsViewMode;
    }

}
