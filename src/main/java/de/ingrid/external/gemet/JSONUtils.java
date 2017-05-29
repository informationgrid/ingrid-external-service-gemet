package de.ingrid.external.gemet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class JSONUtils {

    public static String getId(JSONObject json) {
        return (String) json.get( "uri" );
    }

    public static String getName(JSONObject json) {
        return (String) ((JSONObject) json.get( "preferredLabel" )).get( "string" );
    }

    public static String getType(JSONObject json) {
        return (String) json.get( "thesaurus" );
    }

    public static String getTarget(JSONObject json) {
        return (String) json.get( "target" );
    }

    public static JSONArray toJSONArray(JSONObject jsonObj) {
        JSONArray jsonArray = new JSONArray();
        jsonArray.add( jsonObj );
        
        return jsonArray;
    }
}
