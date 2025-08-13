package com.knowledgepixels.nanodash;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.github.jsonldjava.shaded.com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

/**
 * This class is used to access registry account lists of Nanopub Registry instances.
 */
public class RegistryAccountInfo {

	private static Gson g = new Gson();
	private static Type listType = new TypeToken<ArrayList<RegistryAccountInfo>>(){}.getType();

	/**
	 * @param url e.g. "https://registry.knowledgepixels.com/list.json"
	 */
	public static List<RegistryAccountInfo> fromUrl(String url) throws JsonIOException, JsonSyntaxException, MalformedURLException, IOException {
		return g.fromJson(new InputStreamReader(new URL(url).openStream()), listType);
	}

	private String agent;
	private String pubkey;
	private String status;
	private int depth;
	private int pathCount;
	private long quota;
	private double ratio;

	public static Type getListType() {
		return listType;
	}

	public String getAgent() {
		return agent;
	}

	public IRI getAgentIri() {
		return vf.createIRI(agent);
	}

	public String getPubkey() {
		return pubkey;
	}

	public String getStatus() {
		return status;
	}

	public int getDepth() {
		return depth;
	}

	public int getPathCount() {
		return pathCount;
	}

	public long getQuota() {
		return quota;
	}

	public double getRatio() {
		return ratio;
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

}