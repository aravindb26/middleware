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

package com.openexchange.request.analyzer.rest;

import java.util.Base64;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.openexchange.ajax.writer.ResponseWriter;
import com.openexchange.exception.OXException;
import com.openexchange.net.ClientIPUtil;
import com.openexchange.request.analyzer.AnalyzeResult;
import com.openexchange.request.analyzer.ByteArrayBodyData;
import com.openexchange.request.analyzer.RequestAnalyzerExceptionCodes;
import com.openexchange.request.analyzer.RequestAnalyzerService;
import com.openexchange.request.analyzer.RequestData;
import com.openexchange.request.analyzer.RequestData.Builder;
import com.openexchange.request.analyzer.rest.data.AnalyzeResultWrapper;
import com.openexchange.request.analyzer.rest.data.HeaderDeserializer;
import com.openexchange.request.analyzer.rest.data.RequestDataPOJO;
import com.openexchange.rest.services.CustomStatus;
import com.openexchange.rest.services.JAXRSService;
import com.openexchange.rest.services.annotation.Role;
import com.openexchange.rest.services.annotation.RoleAllowed;
import com.openexchange.server.ServiceLookup;
import com.openexchange.servlet.Header;
import com.openexchange.servlet.Headers;
import com.openexchange.tools.functions.ErrorAwareSupplier;

/**
 * {@link RequestAnalyzerServlet} is a servlet which allows to analyze request data.
 *
 * This data is then mapped to a marker if possible.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
@RoleAllowed(Role.BASIC_AUTHENTICATED)
@Path("/request-analysis/v1/")
public class RequestAnalyzerServlet extends JAXRSService {

    private static final Logger LOG = LoggerFactory.getLogger(RequestAnalyzerServlet.class);

    private static final CustomStatus MISSING_BODY_STATUS_TYPE = new CustomStatus(422, "Unprocessable Entity: Require request body to analyze the request");

    private static final Response NOT_FOUND_RESPONSE = Response.status(Status.NOT_FOUND).build();
    private static final Response BODY_MISSING_RESPONSE = Response.status(MISSING_BODY_STATUS_TYPE).build();

    private final ErrorAwareSupplier<RequestAnalyzerService> service;
    private final ErrorAwareSupplier<ClientIPUtil> ipUtilService;

    private final Gson gson;

    /**
     * Initializes a new {@link RequestAnalyzerServlet}.
     *
     * @param bundleContext The bundle context
     * @param services The {@link ServiceLookup}
     */
    public RequestAnalyzerServlet(BundleContext bundleContext, ServiceLookup services) {
        super(bundleContext);
        this.service = () -> services.getServiceSafe(RequestAnalyzerService.class);
        this.ipUtilService = () -> services.getServiceSafe(ClientIPUtil.class);
        this.gson = new GsonBuilder().disableHtmlEscaping()
                                     .registerTypeAdapter(Header.class, new HeaderDeserializer())
                                     .create();
    }

    /**
     * Analyzes the request and determines a marker for it if possible
     *
     * @param body The body defining the request
     * @return The response
     */
    @POST
    @Path("/analyze")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response analyze(String body) {
        try {
            RequestData data = convert2RequestData(body);
            AnalyzeResult result = service.get().analyzeRequest(data);
            return switch (result.getType()) {
                case SUCCESS -> Response.ok(convert2json(result))
                                        .build();
                case MISSING_BODY -> BODY_MISSING_RESPONSE;
                case UNKNOWN -> NOT_FOUND_RESPONSE;
            };
        } catch (OXException e) {
            if (e.getPrefix().equals(RequestAnalyzerExceptionCodes.PREFIX)) {
                return Response.status(Status.BAD_REQUEST)
                               .entity(e.getDisplayMessage(Locale.US))
                               .build();
            }
            return generateInternalServerError(e);
        } catch (JsonParseException e) {
            return Response.status(Status.BAD_REQUEST)
                           .entity(e.getMessage())
                           .build();
        }
    }

    // ----------------------- private methods -------------------------

    /**
     * Converts the given body to a {@link RequestData} object
     *
     * @param data The json data send by the client
     * @return The {@link RequestData}
     * @throws JsonSyntaxException in case the headers are missing
     * @throws OXException In case the body is invalid
     */
    private RequestData convert2RequestData(String data) throws JsonSyntaxException, OXException {
        RequestDataPOJO wrapper = gson.fromJson(data, RequestDataPOJO.class);
        List<Header> headerList = wrapper.headers();
        if (headerList == null) {
            throw new JsonSyntaxException("Missing headers");
        }
        Headers headers = new Headers(headerList);
        Builder builder = RequestData.builder()
                                     .withHeaders(headers)
                                     .withMethod(wrapper.method())
                                     .withUrl(wrapper.url())
                                     .withClientIp(getClientIP(wrapper.remoteIP(), headers));
        if (wrapper.containsBody()) {
            builder.withBody(new ByteArrayBodyData(Base64.getDecoder().decode(wrapper.body())));
        }
        return builder.build();
    }

    /**
     * Gets the client ip address
     *
     * @param remoteIP The remote ip
     * @param headers the request headers
     * @return The ip address
     * @throws OXException in case the ip service is missing
     */
    private String getClientIP(String remoteIP, Headers headers) throws OXException {
        return ipUtilService.get().getIP(remoteIP, headers);
    }

    /**
     * Converts a {@link AnalyzeResult} to a json object
     *
     * @param resultObj The {@link AnalyzeResult} to convert
     * @return The json object
     */
    private String convert2json(AnalyzeResult resultObj) {
        return gson.toJson(new AnalyzeResultWrapper(resultObj));
    }

    /**
     * Generates an internal server error response
     *
     * @param e The error causing this response
     * @return The response
     */
    private static Response generateInternalServerError(OXException e) {
        try {
            JSONObject entity = new JSONObject();
            ResponseWriter.addException(entity, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                           .entity(entity)
                           .build();
        } catch (JSONException ex) {
            LOG.error("Error while generating error for client.", ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
