package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import java.util.Map;

/**
 * StatsPanel displays statistics related to Nanodash.
 * It shows the latest count, previous count, and accepted count of Nano transactions.
 */
public class StatsPanel extends Panel {

    private final Map<String, String> statsMap;

    static final String VALID_NP_COUNT_KEY = "validNpCount";
    static final String INVALIDATED_NP_COUNT_KEY = "invalidatedNpCount";
    static final String ACCEPTED_NP_COUNT_KEY = "acceptedNpCount";

    /**
     * Constructor for StatsPanel.
     *
     * @param id           the component id
     * @param userId       the user ID (not used in this panel)
     * @param pubkeyHashes the public key hashes (not used in this panel)
     * @param statsMap     a map containing statistics data
     */
    public StatsPanel(String id, String userId, String pubkeyHashes, Map<String, String> statsMap) {
        super(id);
        this.statsMap = statsMap;
        setOutputMarkupId(true);

        add(new Label("latestcount", getLong(VALID_NP_COUNT_KEY)));
        add(new Label("previouscount", getLong(INVALIDATED_NP_COUNT_KEY)));
        add(new Label("acceptedcount", getLong(ACCEPTED_NP_COUNT_KEY)));
    }

    /**
     * Retrieves a long value from the stats map for the given key.
     * If the key does not exist, it returns 0.
     *
     * @param key the key to look up in the stats map
     * @return the long value associated with the key, or 0 if the key is not found
     */
    long getLong(String key) {
        String value = statsMap.get(key);
        if (value == null) return 0L;
        return Long.parseLong(value);
    }

}
