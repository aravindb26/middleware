/*
 * @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
 * @license AGPL-3.0
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
 *
 * Any use of the work other than as authorized under this license or copyright law is prohibited.
 *
 */

package com.openexchange.java.util;

/**
 * {@link HttpStatusCode} - An enum with HTTP status codes and their descriptions according to
 * <a href="https://www.iana.org/assignments/http-status-codes/http-status-codes.xhtml">HTTP status code registry</a>.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public enum HttpStatusCode {

    //1xx: Informational

    /**
     * This interim response indicates that the client should continue the request or ignore the response if the request is already finished.
     */
    CONTINUE(100, "Continue"),
    /**
     * This code is sent in response to an <code>Upgrade</code> request header from the client and indicates the protocol the server
     * is switching to
     */
    SWITCHING_PROTOCOLS(101, "Switching Protocols"),
    /**
     * This code indicates that the server has received and is processing the request, but no response is available yet. (WebDAV)
     */
    PROCESSING(102, "Processing"),
    /**
     * This status code is primarily intended to be used with the <code>Link</code> header, letting the user agent start preloading
     * resources while the server prepares a response.
     */
    EARLY_HINTS(103, "Early Hints"),

    //2xx: Success

    /**
     * The request succeeded. The result meaning of "success" depends on the HTTP method:
     * <ul>
     * <li>GET: The resource has been fetched and transmitted in the message body.
     * <li>HEAD: The representation headers are included in the response without any message body.
     * <li>PUT or POST: The resource describing the result of the action is transmitted in the message body.
     * <li>TRACE: The message body contains the request message as received by the server.
     * </ul>
     */
    OK(200, "OK"),
    /**
     * The request succeeded, and a new resource was created as a result.
     * <p>
     * This is typically the response sent after <code>POST</code> requests, or some <code>PUT</code> requests.
     */
    CREATED(201, "Created"),
    /**
     * The request has been received but not yet acted upon. It is noncommittal, since there is no way in HTTP to later
     * send an asynchronous response indicating the outcome of the request. It is intended for cases where another process
     * or server handles the request, or for batch processing.
     */
    ACCEPTED(202, "Accepted"),
    /**
     * This response code means the returned metadata is not exactly the same as is available from the origin server,
     * but is collected from a local or a third-party copy. This is mostly used for mirrors or backups of another resource.
     * Except for that specific case, the 200 OK response is preferred to this status.
     */
    NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
    /**
     * There is no content to send for this request, but the headers may be useful.
     * The user agent may update its cached headers for this resource with the new ones.
     */
    NO_CONTENT(204, "No Content"),
    /**
     * Tells the user agent to reset the document which sent this request.
     */
    RESET_CONTENT(205, "Reset Content"),
    /**
     * This response code is used when the <code>Range</code> header is sent from the client to request only part of a resource.
     */
    PARTIAL_CONTENT(206, "Partial Content"),
    /**
     * Conveys information about multiple resources, for situations where multiple status codes might be appropriate. (WebDAV)
     */
    MULTI_STATUS(207, "Multi-Status"),
    /**
     * Used inside a <code>&lt;dav:propstat&gt;</code> response element to avoid repeatedly enumerating the internal members of multiple
     * bindings to the same collection. (WebDAV)
     */
    ALREADY_REPORTED(208, "Already Reported"),
    /**
     * The server has fulfilled a <code>GET</code> request for the resource, and the response is a representation of the result
     * of one or more instance-manipulations applied to the current instance.
     * (<a href="https://datatracker.ietf.org/doc/html/rfc3229">HTTP Delta encoding</a>)
     */
    IM_USED(226, "IM Used"),

    //3xx: Redirection

    /**
     * The request has more than one possible response. The user agent or user should choose one of them.
     * <p>
     * (There is no standardized way of choosing one of the responses, but HTML links to the possibilities are recommended so the user can pick.)
     */
    MULTIPLE_CHOICES(300, "Multiple Choice"),
    /**
     * The URL of the requested resource has been changed permanently. The new URL is given in the response.
     */
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    /**
     * This response code means that the URI of requested resource has been changed temporarily.
     * Further changes in the URI might be made in the future. Therefore, this same URI should be used by the client in future requests.
     */
    FOUND(302, "Found"),
    /**
     * The server sent this response to direct the client to get the requested resource at another URI with a <code>GET</code> request.
     */
    SEE_OTHER(303, "See Other"),
    /**
     * This is used for caching purposes. It tells the client that the response has not been modified, so the client can continue
     * to use the same cached version of the response.
     */
    NOT_MODIFIED(304, "Not Modified"),
    /**
     * Defined in a previous version of the HTTP specification to indicate that a requested response must be accessed by a proxy.
     * It has been deprecated due to security concerns regarding in-band configuration of a proxy.
     */
    USE_PROXY(305, "Use Proxy"),
    /**
     * The server sends this response to direct the client to get the requested resource at another URI with the same method that
     * was used in the prior request. This has the same semantics as the <code>302 Found</code> HTTP response code, with the exception that
     * the user agent must not change the HTTP method used:
     * if a <code>POST</code> was used in the first request, a <code>POST</code> must be used in the second request.
     */
    TEMPORARY_REDIRECT(307, "Temporary Redirect"),
    /**
     * This means that the resource is now permanently located at another URI, specified by the <code>Location:</code> HTTP Response header.
     * This has the same semantics as the <code>301 Moved Permanently</code> HTTP response code,
     * with the exception that the user agent must not change the HTTP method used:
     * if a <code>POST</code> was used in the first request, a <code>POST</code> must be used in the second request.
     */
    PERMANENT_REDIRECT(308, "Permanent Redirect"),

    //4xx: Client Error
    /**
     * The server cannot or will not process the request due to something that is perceived to be a client error
     * (e.g., malformed request syntax, invalid request message framing, or deceptive request routing).
     */
    BAD_REQUEST(400, "Bad Request"),
    /**
     * Although the HTTP standard specifies "unauthorized", semantically this response means "unauthenticated".
     * That is, the client must authenticate itself to get the requested response.
     */
    UNAUTHORIZED(401, "Unauthorized"),
    /**
     * This response code is reserved for future use. The initial aim for creating this code was using it for
     * digital payment systems, however this status code is used very rarely and no standard convention exists.
     */
    PAYMENT_REQUIRED(402, "Payment Required"),
    /**
     * The client does not have access rights to the content; that is, it is unauthorized, so the server is refusing
     * to give the requested resource. Unlike 401 Unauthorized, the client's identity is known to the server.
     */
    FORBIDDEN(403, "Forbidden"),
    /**
     * The server cannot find the requested resource. In the browser, this means the URL is not recognized.
     * In an API, this can also mean that the endpoint is valid but the resource itself does not exist.
     * Servers may also send this response instead of 403 Forbidden to hide the existence of a resource from an unauthorized client.
     * This response code is probably the most well known due to its frequent occurrence on the web.
     */
    NOT_FOUND(404, "Not Found"),
    /**
     * The request method is known by the server but is not supported by the target resource.
     * For example, an API may not allow calling <code>DELETE</code> to remove a resource.
     */
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    /**
     * This response is sent when the web server, after performing <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Content_negotiation#server-driven_negotiation">server-driven content negotiation</a>,
     * doesn't find any content that conforms to the criteria given by the user agent.
     */
    NOT_ACCEPTABLE(406, "Not Acceptable"),
    /**
     * This is similar to <code>401 Unauthorized</code> but authentication is needed to be done by a proxy.
     */
    PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
    /**
     * This response is sent on an idle connection by some servers, even without any previous request by the client.
     * It means that the server would like to shut down this unused connection.
     * <p>
     * This response is used much more since some browsers, like Chrome, Firefox 27+, or IE9, use HTTP pre-connection
     * mechanisms to speed up surfing. Also note that some servers merely shut down the connection without sending this message.
     */
    REQUEST_TIMEOUT(408, "Request Timeout"),
    /**
     * This response is sent when a request conflicts with the current state of the server.
     */
    CONFLICT(409, "Conflict"),
    /**
     * This response is sent when the requested content has been permanently deleted from server, with no forwarding address.
     * Clients are expected to remove their caches and links to the resource.
     * The HTTP specification intends this status code to be used for "limited-time, promotional services".
     * APIs should not feel compelled to indicate resources that have been deleted with this status code.
     */
    GONE(410, "Gone"),
    /**
     * Server rejected the request because the <code>Content-Length</code> header field is not defined and the server requires it.
     */
    LENGTH_REQUIRED(411, "Length Required"),
    /**
     * The client has indicated preconditions in its headers which the server does not meet.
     */
    PRECONDITION_FAILED(412, "Precondition Failed"),
    /**
     * Request entity is larger than limits defined by server.
     * The server might close the connection or return an <code>Retry-After</code> header field.
     */
    REQUEST_TOO_LONG(413, "Payload Too Large"),
    /**
     * The URI requested by the client is longer than the server is willing to interpret.
     */
    REQUEST_URI_TOO_LONG(414, "URI Too Long"),
    /**
     * The media format of the requested data is not supported by the server, so the server is rejecting the request.
     */
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    /**
     * The range specified by the <code>Range</code> header field in the request cannot be fulfilled.
     * It's possible that the range is outside the size of the target URI's data.
     */
    REQUESTED_RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable"),
    /**
     * This response code means the expectation indicated by the <code>Expect</code> request header field cannot be met by the server.
     */
    EXPECTATION_FAILED(417, "Expectation Failed"),
    /**
     * The server refuses the attempt to brew coffee with a teapot.
     */
    IM_A_TEAPOT(418, "I'm A Teapot"),
    /**
     * The request was directed at a server that is not able to produce a response. This can be sent by a
     * server that is not configured to produce responses for the combination of scheme and authority
     * that are included in the request URI.
     */
    MISDIRECTED_REQUEST(421, "Misdirected Request"),
    /**
     * The request was well-formed but was unable to be followed due to semantic errors. (WebDAV)
     */
    UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),
    /**
     * The resource that is being accessed is locked. (WebDAV)
     */
    LOCKED(423, "Locked"),
    /**
     * The request failed due to failure of a previous request. (WebDAV)
     */
    FAILED_DEPENDENCY(424, "Failed Dependency"),
    /**
     * Indicates that the server is unwilling to risk processing a request that might be replayed.
     */
    TOO_EARLY(425, "Too Early"),
    /**
     * The server refuses to perform the request using the current protocol but might be willing to do so
     * after the client upgrades to a different protocol. The server sends an <code>Upgrade</code> header in a <code>426</code> response
     * to indicate the required protocol(s).
     */
    UPGRADE_REQUIRED(426, "Upgrade Required"),
    /**
     * The origin server requires the request to be conditional.
     * This response is intended to prevent the 'lost update' problem, where a client <code>GET</code>s a resource's state,
     * modifies it and PUTs it back to the server, when meanwhile a third party has modified the state on the server,
     * leading to a conflict.
     */
    PRECONDITION_REQUIRED(428, "Precondition Required"),
    /**
     * The user has sent too many requests in a given amount of time ("rate limiting").
     */
    TOO_MANY_REQUESTS(429, "Too Many Requests"),
    /**
     * The server is unwilling to process the request because its header fields are too large.
     * The request may be resubmitted after reducing the size of the request header fields.
     */
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),
    /**
     * The user agent requested a resource that cannot legally be provided, such as a web page censored by a government.
     */
    UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons"),

    //5xx: Server Error

    /**
     * The server has encountered a situation it does not know how to handle.
     */
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    /**
     * The request method is not supported by the server and cannot be handled. The only methods that servers are required to support
     * (and therefore that must not return this code) are <code>GET</code> and <code>HEAD</code>.
     */
    NOT_IMPLEMENTED(501, "Not Implemented"),
    /**
     * This error response means that the server, while working as a gateway to get a response needed to handle the request,
     * got an invalid response.
     */
    BAD_GATEWAY(502, "Bad Gateway"),
    /**
     * The server is not ready to handle the request. Common causes are a server that is down for maintenance or that is overloaded.
     * Note that together with this response, a user-friendly page explaining the problem should be sent.
     * This response should be used for temporary conditions and the <code>Retry-After</code> HTTP header should, if possible,
     * contain the estimated time before the recovery of the service.
     * The webmaster must also take care about the caching-related headers that are sent along with this response,
     * as these temporary condition responses should usually not be cached.
     */
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    /**
     * This error response is given when the server is acting as a gateway and cannot get a response in time.
     */
    GATEWAY_TIMEOUT(504, "Gateway Timeout"),
    /**
     * The HTTP version used in the request is not supported by the server.
     */
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported"),
    /**
     * The server has an internal configuration error: the chosen variant resource is configured to engage in transparent content
     * negotiation itself, and is therefore not a proper end point in the negotiation process.
     */
    VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates"),
    /**
     * The method could not be performed on the resource because the server is unable to store the representation needed to
     * successfully complete the request.
     */
    INSUFFICIENT_STORAGE(507, "Insufficient Storage"),
    /**
     * The server detected an infinite loop while processing the request.
     */
    LOOP_DETECTED(508, "Loop Detected"),
    /**
     * Further extensions to the request are required for the server to fulfill it.
     */
    NOT_EXTENDED(510, "Not Extended"),
    /**
     * Indicates that the client needs to authenticate to gain network access.
     */
    NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required");

    private final int value;
    private final String description;

    /**
     * Initializes a new {@link HttpStatusCode}.
     *
     * @param code The numeric code
     * @param description The description
     */
    private HttpStatusCode(int code, String description) {
        this.value = code;
        this.description = description;
    }

    /**
     * Gets the numeric code.
     *
     * @return The code
     */
    public int getCode() {
        return value;
    }

    /**
     * Gets the description; e.g. <code>"Internal Server Error"</code>.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return value + " " + description;
    }

    /**
     * Gets the HTTP status code for specified code.
     *
     * @param code The code to look-up
     * @return The HTTP status code or <code>null</code>
     */
    public static HttpStatusCode httpStatusCodeFor(int code) {
        for (HttpStatusCode status : values()) {
            if (status.value == code) {
                return status;
            }
        }
        return null;
    }

    /**
     * Checks if specified HTTP status code falls in informational responses (<code>100</code> – <code>199</code>).
     *
     * @param code The code to check
     * @return <code>true</code> if HTTP status code falls in informational responses; otherwise <code>false</code>
     */
    public static boolean isInformational(HttpStatusCode code) {
        return code != null && isInformational(code.getCode());
    }

    /**
     * Checks if specified numeric HTTP status code falls in informational responses (<code>100</code> – <code>199</code>).
     *
     * @param code The code to check
     * @return <code>true</code> if numeric HTTP status code falls in informational responses; otherwise <code>false</code>
     */
    public static boolean isInformational(int code) {
        return code >= 100 && code <= 199;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Checks if specified HTTP status code falls in success responses (<code>200</code> – <code>299</code>).
     *
     * @param code The code to check
     * @return <code>true</code> if HTTP status code falls in success responses; otherwise <code>false</code>
     */
    public static boolean isSucess(HttpStatusCode code) {
        return code != null && isSucess(code.getCode());
    }

    /**
     * Checks if specified numeric HTTP status code falls in success responses (<code>200</code> – <code>299</code>).
     *
     * @param code The code to check
     * @return <code>true</code> if numeric HTTP status code falls in success responses; otherwise <code>false</code>
     */
    public static boolean isSucess(int code) {
        return code >= 200 && code <= 299;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Checks if specified HTTP status code falls in redirection messages (<code>300</code> – <code>399</code>).
     *
     * @param code The code to check
     * @return <code>true</code> if HTTP status code falls in redirection messages; otherwise <code>false</code>
     */
    public static boolean isRedirection(HttpStatusCode code) {
        return code != null && isRedirection(code.getCode());
    }

    /**
     * Checks if specified numeric HTTP status code falls in redirection messages (<code>300</code> – <code>399</code>).
     *
     * @param code The code to check
     * @return <code>true</code> if numeric HTTP status code falls in redirection messages; otherwise <code>false</code>
     */
    public static boolean isRedirection(int code) {
        return code >= 300 && code <= 399;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Checks if specified HTTP status code falls in client error responses (<code>400</code> – <code>499</code>).
     *
     * @param code The code to check
     * @return <code>true</code> if HTTP status code falls in client error responses; otherwise <code>false</code>
     */
    public static boolean isClientError(HttpStatusCode code) {
        return code != null && isClientError(code.getCode());
    }

    /**
     * Checks if specified numeric HTTP status code falls in client error responses (<code>400</code> – <code>499</code>).
     *
     * @param code The code to check
     * @return <code>true</code> if numeric HTTP status code falls in client error responses; otherwise <code>false</code>
     */
    public static boolean isClientError(int code) {
        return code >= 400 && code <= 499;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Checks if specified HTTP status code falls in server error responses (<code>500</code> – <code>599</code>).
     *
     * @param code The code to check
     * @return <code>true</code> if HTTP status code falls in server error responses; otherwise <code>false</code>
     */
    public static boolean isServerError(HttpStatusCode code) {
        return code != null && isServerError(code.getCode());
    }

    /**
     * Checks if specified numeric HTTP status code falls in server error responses (<code>500</code> – <code>599</code>).
     *
     * @param code The code to check
     * @return <code>true</code> if numeric HTTP status code falls in server error responses; otherwise <code>false</code>
     */
    public static boolean isServerError(int code) {
        return code >= 500 && code <= 599;
    }

}
