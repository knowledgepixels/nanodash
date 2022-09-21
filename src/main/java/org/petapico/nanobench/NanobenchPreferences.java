package org.petapico.nanobench;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class NanobenchPreferences implements Serializable {

	private static final long serialVersionUID = 1L;
	private static NanobenchPreferences obj;

	public static NanobenchPreferences get() {
		if (obj == null) {
			File prefFile = new File(System.getProperty("user.home") + "/.nanopub/nanobench-preferences.yml");
			if (!prefFile.exists()) {
				return new NanobenchPreferences();
			}
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			try {
				obj = mapper.readValue(prefFile, NanobenchPreferences.class);
			} catch (IOException ex) {
				obj = new NanobenchPreferences();
				ex.printStackTrace();
			}
		}
		return obj;
	}

	private List<String> nanopubActions = new ArrayList<>();
	private boolean readOnlyMode = false;
	private String websiteUrl = "http://localhost:37373/";
	private boolean orcidLoginMode = false;
	private String orcidClientId;
	private String orcidClientSecret;

	public List<String> getNanopubActions() {
		String s = System.getenv("NANOBENCH_NANOPUB_ACTIONS");
		if (!(s == null) && !s.isEmpty()) return Arrays.asList(s.split(" "));
		return nanopubActions;
	}

	public void setNanopubActions(List<String> nanopubActions) {
		this.nanopubActions = nanopubActions;
	}

	public boolean isReadOnlyMode() {
		if ("true".equals(System.getenv("NANOBENCH_READ_ONLY_MODE"))) return true;
		return readOnlyMode;
	}

	public void setReadOnlyMode(boolean readOnlyMode) {
		this.readOnlyMode = readOnlyMode;
	}

	public String getWebsiteUrl() {
		String s = System.getenv("NANOBENCH_WEBSITE_URL");
		if (!(s == null) && !s.isEmpty()) return s;
		return websiteUrl;
	}

	public void setWebsiteUrl(String websiteUrl) {
		this.websiteUrl = websiteUrl;
	}

	public boolean isOrcidLoginMode() {
		if ("true".equals(System.getenv("NANOBENCH_ORCID_LOGIN_MODE"))) return true;
		return orcidLoginMode;
	}

	public void setOrcidLoginMode(boolean orcidLoginMode) {
		this.orcidLoginMode = orcidLoginMode;
	}

	public String getOrcidClientId() {
		String s = System.getenv("NANOPUB_ORCID_CLIENT_ID");
		if (!(s == null) && !s.isEmpty()) return s;
		return orcidClientId;
	}

	public void setOrcidClientId(String orcidClientId) {
		this.orcidClientId = orcidClientId;
	}

	public String getOrcidClientSecret() {
		String s = System.getenv("NANOPUB_ORCID_CLIENT_SECRET");
		if (!(s == null) && !s.isEmpty()) return s;
		return orcidClientSecret;
	}

	public void setOrcidClientSecret(String orcidClientSecret) {
		this.orcidClientSecret = orcidClientSecret;
	}

}
