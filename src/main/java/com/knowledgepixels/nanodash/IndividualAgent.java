package com.knowledgepixels.nanodash;

import java.util.HashMap;
import java.util.Map;

// TODO Merge this class with User or otherwise make them aligned.
public class IndividualAgent extends ProfiledResource {

    private static Map<String,IndividualAgent> instanceMap = new HashMap<>();

    public static IndividualAgent get(String id) {
        if (!instanceMap.containsKey(id)) {
            instanceMap.put(id, new IndividualAgent(id));
        }
        return instanceMap.get(id);
    }

    private IndividualAgent(String id) {
        super(id);
    }

}
