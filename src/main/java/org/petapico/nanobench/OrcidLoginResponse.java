package org.petapico.nanobench;

import com.google.gson.Gson;

public class OrcidLoginResponse {

	private static Gson g = new Gson();

	public static OrcidLoginResponse fromJson(String jsonString) {
		return g.fromJson(jsonString, OrcidLoginResponse.class);
	}

	private String access_token;
	private String token_type;
	private String refresh_token;
	private long expires_in;
	private String scope;
	private String name;
	private String orcid;

	public String getAccessToken() {
		return access_token;
	}

	public String getTokenType() {
		return token_type;
	}

	public String getRefreshToken() {
		return refresh_token;
	}

	public long getExpiresIn() {
		return expires_in;
	}

	public String getScope() {
		return scope;
	}

	public String getName() {
		return name;
	}

	public String getOrcid() {
		return orcid;
	}

}
