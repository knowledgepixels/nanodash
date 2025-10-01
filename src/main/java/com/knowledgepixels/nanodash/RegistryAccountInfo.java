package com.knowledgepixels.nanodash;

import com.github.jsonldjava.shaded.com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to access registry account lists of Nanopub Registry instances.
 */
public class RegistryAccountInfo {

    private static Gson g = new Gson();
    private static Type listType = new TypeToken<ArrayList<RegistryAccountInfo>>() {
    }.getType();

    /**
     * Fetches and parses a list of registry accounts from the given URL.
     *
     * @param url e.g. "<a href="https://registry.knowledgepixels.com/list.json">https://registry.knowledgepixels.com/list.json</a>"
     * @return List of RegistryAccountInfo objects
     * @throws JsonIOException     if there is a problem reading from the URL
     * @throws JsonSyntaxException if the JSON is not in the expected format
     * @throws IOException         if there is a problem with the network connection
     * @throws URISyntaxException  if the URL is not valid
     */
    public static List<RegistryAccountInfo> fromUrl(String url) throws JsonIOException, JsonSyntaxException, IOException, URISyntaxException {
        return g.fromJson(new InputStreamReader(new URI(url).toURL().openStream()), listType);
    }

    private String agent;
    private String pubkey;
    private String status;
    private int depth;
    private int pathCount;
    private long quota;
    private double ratio;

    /**
     * Returns the Type object representing a list of RegistryAccountInfo objects.
     *
     * @return Type object
     */
    public static Type getListType() {
        return listType;
    }

    /**
     * Returns the agent URI as a string.
     *
     * @return Agent URI string
     */
    public String getAgent() {
        return agent;
    }

    /**
     * Returns the agent URI as an IRI object.
     *
     * @return Agent IRI
     */
    public IRI getAgentIri() {
        return Values.iri(agent);
    }

    /**
     * Returns the public key associated with the account.
     *
     * @return Public key string
     */
    public String getPubkey() {
        return pubkey;
    }

    /**
     * Returns the status of the account.
     *
     * @return Status string
     */
    public String getStatus() {
        return status;
    }

    /**
     * Returns the depth of the account.
     *
     * @return Depth integer
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Returns the path count of the account.
     *
     * @return Path count integer
     */
    public int getPathCount() {
        return pathCount;
    }

    /**
     * Returns the quota of the account.
     *
     * @return Quota long
     */
    public long getQuota() {
        return quota;
    }

    /**
     * Returns the ratio of the account.
     *
     * @return Ratio double
     */
    public double getRatio() {
        return ratio;
    }

}