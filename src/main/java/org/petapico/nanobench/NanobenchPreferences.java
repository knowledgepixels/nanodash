package org.petapico.nanobench;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class NanobenchPreferences {

	private List<String> nanopubActions = new ArrayList<>();
	private boolean readOnlyMode = false;

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

	private static NanobenchPreferences obj;

	public static NanobenchPreferences get() {
		if (obj == null) {
			File prefFile = new File(System.getProperty("user.home") + "/.nanopub/nanobench-preferences.yml");
			if (!prefFile.exists()) return null;
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

}
