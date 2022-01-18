/*-
 * **************************************************-
 * ingrid-external-service-gemet
 * ==================================================
 * Copyright (C) 2014 - 2022 wemove digital solutions GmbH
 * ==================================================
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 * 
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl5
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * **************************************************#
 */
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
