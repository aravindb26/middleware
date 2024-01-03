
package com.openexchange.ajax.framework;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.openexchange.exception.Category;
import com.openexchange.testing.httpclient.models.CommonResponse;

/**
 * Class for common HTTP client tooling.
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 */
public class ClientCommons {

    /**
     * HTTP Header which is used to route all API calls from a single test to one specific pod
     */
    public static final String X_OX_HTTP_TEST_HEADER_NAME = "X-OX-HTTP-Test";

    /**
     * Checks if a response doesn't contain any errors
     *
     * @param response The common response to check
     */
    public static void checkResponse(CommonResponse response) {
        assertNull(response.getErrorDesc(), response.getError());
    }

    /**
     * Checks if a response doesn't contain any errors
     *
     * @param error The error element of the response
     * @param errorDesc The error description element of the response
     */
    public static void checkResponse(String error, String errorDesc) {
        assertNull(error, errorDesc);
    }

    /**
     * Checks if a response doesn't contain any errors.
     *
     * @param error The error element of the response
     * @param errorDesc The error description element of the response
     * @param data The data element of the response
     * @return The data
     */
    public static <T> T checkResponse(String error, String errorDesc, T data) {
        return checkResponse(error, errorDesc, null, data);
    }

    /**
     * Checks if a response doesn't contain any errors. Errors of category "WARNING" are ignored implicitly.
     *
     * @param error The error element of the response
     * @param errorDesc The error description element of the response
     * @param categories The error categories if the response
     * @param data The data element of the response
     * @return The data
     */
    public static <T> T checkResponse(String error, String errorDesc, String categories, T data) {
        return checkResponse(error, errorDesc, categories, true, data);
    }

    /**
     * Checks if a response doesn't contain any errors
     *
     * @param error The error element of the response
     * @param errorDesc The error description element of the response
     * @param categories The error categories of the response
     * @param ignoreWarnings <code>true</code> to ignore warnings (as indicated through the categories), <code>false</code>, otherwise
     * @param data The data element of the response
     * @return The data
     */
    public static <T> T checkResponse(String error, String errorDesc, String categories, boolean ignoreWarnings, T data) {
        if (false == ignoreWarnings || false == Category.EnumType.WARNING.name().equals(categories)) {
            assertNull(error, errorDesc);
        }
        assertNotNull(data);
        return data;
    }

}
