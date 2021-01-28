package org.petapico.nanobench;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class NanobenchPreferences {

	private List<String> nanopubActions;

	public List<String> getNanopubActions() {
		return nanopubActions;
	}

	public void setNanopubActions(List<String> nanopubActions) {
		this.nanopubActions = nanopubActions;
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
				ex.printStackTrace();
			}
		}
		return obj;
	}

}
