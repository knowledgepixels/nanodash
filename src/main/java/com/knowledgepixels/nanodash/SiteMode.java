package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.ProfilePicture;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;

import java.util.regex.Pattern;

/**
 * Site mode: a deployment that presents itself as the website of one space rather than as
 * Nanodash (issue #692). It is switched on by configuring that space (see
 * {@link NanodashPreferences#getSiteSpace()}), and everything here answers as if there were no
 * site at all while it is not: an instance that has not been given a site space behaves exactly
 * as before.
 * <p>
 * In site mode the space is the navigation context of every page that has none of its own,
 * its page is the home page, its name and picture replace Nanodash's in the title bar and in
 * the page metadata, and links to what lies outside the site are plain links to the resource
 * itself instead of links into the general Nanodash interface. See docs/site-views.md.
 */
public final class SiteMode {

    private SiteMode() {
    }  // no instances allowed

    /**
     * The artifact code of a trusty URI. A nanopublication, and anything minted inside one, is
     * reachable on every instance, so it counts as part of any site.
     */
    private static final Pattern ARTIFACT_CODE = Pattern.compile("RA[A-Za-z0-9_-]{43}");

    /**
     * Whether this instance is a site for one space.
     *
     * @return true if a site space is configured
     */
    public static boolean isEnabled() {
        return getSpaceId() != null;
    }

    /**
     * The IRI of the site's space.
     *
     * @return the space IRI, or null when this instance is not a site
     */
    public static String getSpaceId() {
        return NanodashPreferences.get().getSiteSpace();
    }

    /**
     * Whether the given id names the site's space, under its own IRI or one of its
     * alternative IRIs.
     *
     * @param id the id to check, or null
     * @return true if it is the site's space
     */
    public static boolean isSiteSpace(String id) {
        String siteId = getSpaceId();
        if (siteId == null || id == null) return false;
        if (id.equals(siteId)) return true;
        Space space = getSpace();
        return space != null && (id.equals(space.getId()) || space.getAltIDs().contains(id));
    }

    /**
     * The site's space, as far as the repository knows it. Null while the repository has not
     * loaded it yet (a cold start) and, of course, when this instance is not a site.
     *
     * @return the space, or null
     */
    public static Space getSpace() {
        String siteId = getSpaceId();
        if (siteId == null) return null;
        Space space = SpaceRepository.get().findById(siteId);
        if (space == null) space = SpaceRepository.get().findByAltId(siteId);
        return space;
    }

    /**
     * The site's name: the configured one, else the space's label, else the last part of
     * its IRI while the space is not loaded yet.
     *
     * @return the name; null when this instance is not a site
     */
    public static String getName() {
        String siteId = getSpaceId();
        if (siteId == null) return null;
        String configured = NanodashPreferences.get().getSiteName();
        if (configured != null) return configured;
        Space space = getSpace();
        if (space != null && space.getLabel() != null && !space.getLabel().isBlank()) return space.getLabel();
        return Utils.getShortNameFromURI(siteId);
    }

    /**
     * What to show as the site's logo: the configured image, else the space's own profile
     * picture ({@code schema:image}, issue #632).
     *
     * @return an image source for an {@code <img>} tag, or null when the site has no logo
     */
    public static String getLogoSrc() {
        if (!isEnabled()) return null;
        String configured = NanodashPreferences.get().getSiteLogo();
        if (configured != null) return configured;
        Space space = getSpace();
        if (space == null) return null;
        ProfilePicture picture = space.getProfilePicture();
        return picture == null ? null : picture.getSrc();
    }

    /**
     * Whether a link to the given URI should be a plain link to the resource itself rather
     * than a link into this instance: in site mode, with external links switched on, for
     * everything that does not belong to the site.
     *
     * @param uri the URI a link would point at
     * @return true if the link should leave the site
     */
    public static boolean rendersExternally(String uri) {
        return isEnabled() && NanodashPreferences.get().isSiteExternalLinks() && !belongsToSite(uri);
    }

    /**
     * Whether the given URI is part of the site: the site's space itself and anything under
     * its IRI (or one of its alternative IRIs), its sub-spaces and the resources they maintain
     * along with what lies in those resources' namespaces, nanopublications and what is minted
     * inside them, and users. Users and nanopublications are in every site: a nanopublication
     * published from the site is one the user must be able to look at afterwards, whoever
     * it is about (see docs/site-views.md), and a user's page is where their own
     * publications are found.
     * <p>
     * Without a site, everything belongs.
     *
     * @param uri the URI to check
     * @return true if the URI is part of the site
     */
    public static boolean belongsToSite(String uri) {
        String siteId = getSpaceId();
        if (siteId == null) return true;
        if (uri == null) return false;
        if (isUnder(uri, siteId)) return true;
        if (ARTIFACT_CODE.matcher(uri).find()) return true;
        if (IndividualAgent.isOrcidIri(uri) || IndividualAgent.isUser(uri)) return true;
        Space site = getSpace();
        if (site != null) {
            for (String altId : site.getAltIDs()) {
                if (isUnder(uri, altId)) return true;
            }
        }
        Space space = SpaceRepository.get().findById(uri);
        if (space == null) space = SpaceRepository.get().findByAltId(uri);
        if (space != null) return isWithinSite(space);
        MaintainedResource resource = MaintainedResourceRepository.get().findById(uri);
        if (resource == null) {
            resource = MaintainedResourceRepository.get().findByNamespace(MaintainedResource.getNamespace(uri));
        }
        return resource != null && isWithinSite(resource.getSpace());
    }

    /**
     * Whether the URI is the given base or lies under it as a path or fragment.
     */
    private static boolean isUnder(String uri, String base) {
        if (base == null || base.isEmpty()) return false;
        if (uri.equals(base)) return true;
        if (base.endsWith("/") || base.endsWith("#")) return uri.startsWith(base);
        return uri.startsWith(base + "/") || uri.startsWith(base + "#");
    }

    /**
     * Whether the space is the site's space or one of its sub-spaces, at any depth.
     */
    private static boolean isWithinSite(Space space) {
        if (space == null) return false;
        if (isSiteSpace(space.getId())) return true;
        for (AbstractResourceWithProfile ancestor : space.getAllSuperSpacesUntilRoot()) {
            if (isSiteSpace(ancestor.getId())) return true;
        }
        return false;
    }

}
