package com.rationaleemotions.formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class JsonDataContainer {
    Map<String, Object> featureMap = new ConcurrentHashMap<>();
    List<Map> beforeHooks = new ArrayList<>();
    List<Map<String, Object>> featureMaps = new ArrayList<>();
}
