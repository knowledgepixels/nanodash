package com.knowledgepixels.nanodash.component;

import java.util.Map;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

public class StatsPanel extends Panel {
	
	private static final long serialVersionUID = 1L;

	private Map<String,String> statsMap;

	public StatsPanel(String id, String userId, String pubkeyHashes, Map<String,String> statsMap) {
		super(id);
		this.statsMap = statsMap;
		setOutputMarkupId(true);
		
		add(new Label("latestcount", getLong("validNpCount")));
		add(new Label("previouscount", getLong("invalidatedNpCount")));
		add(new Label("acceptedcount", getLong("acceptedNpCount")));
	}

	private long getLong(String key) {
		String value = statsMap.get(key);
		if (value == null) return 0l;
		return Long.parseLong(value);
	}

}
