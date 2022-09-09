/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.netarchivesuite.solrwayback.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Ensures that URLs are  absolute, relative to a provided base URL.
 * If the base URL is null, the input URLs are returned unmodified.
 */
public class URLAbsoluter {
    private static final Logger log = LoggerFactory.getLogger(URLAbsoluter.class);

    private static final Pattern IS_ABSOLUTE_URL = Pattern.compile("^https?:");

    final String baseURLString;
    URL baseURL = null; // Construction is lazy to spare unneeded overhead
    boolean isConstructed = false;
    final boolean normalise;

    public URLAbsoluter(String baseURL, boolean normalise) {
        this.baseURLString = baseURL;
        this.normalise = normalise;
    }

    public URL getBaseURL() {
        if (!isConstructed) {
            try {
                baseURL = new URL(baseURLString);
            } catch (MalformedURLException e) {
                log.debug("urlNormaliser: Unable to parse baseURL '" + baseURLString + "', unable to use baseURL " +
                          "to create absolute URLs from relative URLs");
            }
            isConstructed = true;
        }
        return baseURL;
    }

    /**
     * Trims the url and makes sure it is absolute, relative to the base URL given when constructing URLAbsoluter.
     * @param url an URL that can be relative or absolute.
     * @return an absolute URL.
     */
    public String apply(String url) {
        if (url == null || getBaseURL() == null) {
            return url;
        }
        if (normalise) {
            url = url.trim().replace("/../", "/");
        }
        if (url.isEmpty()) {
            return url;
        }
        if (url.startsWith("data:")) { // Typically used for embedding image data in HTML
            return url;
        }

        try {
            if (getBaseURL() != null && !IS_ABSOLUTE_URL.matcher(url).matches()) {
                url = new URL(getBaseURL(), url).toString();
            }
        } catch (MalformedURLException ex) {
            log.debug("urlNormaliser: Unable to create an absolute URL using new URL('" + getBaseURL() + "', '" +
                      url + "'), the problematic URL will be passed as-is");
        }
        if (normalise) {
            url = Normalisation.canonicaliseURL(url);
        }
        return url;
    }

    public String toString() {
        return "URLAbsoluter(base='" + baseURLString + "')";
    }
}
