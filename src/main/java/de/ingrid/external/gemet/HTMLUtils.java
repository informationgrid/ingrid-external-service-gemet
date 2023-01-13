/*-
 * **************************************************-
 * ingrid-external-service-gemet
 * ==================================================
 * Copyright (C) 2014 - 2023 wemove digital solutions GmbH
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HTMLUtils {

    private final static Logger log = LogManager.getLogger( HTMLUtils.class );

    public static String prepareUrl(String url) {
        if (url.endsWith( "/" ))
            return url;
        else
            return url + "/";
    }

    public static String encodeForURL(String input) {
        String output = input;
        try {
            output = URLEncoder.encode( input, "UTF-8" );
        } catch (UnsupportedEncodingException e) {
            log.error("Problems encoding input for URL, we keep input: {}", input, e );
        }

        return output;
    }
}
