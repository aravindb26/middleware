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

package com.openexchange.chronos.json.action;

import static com.openexchange.chronos.json.action.ChronosJsonParameters.parseParameters;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.common.collect.ImmutableList;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.antivirus.AntiVirusEncapsulatedContent;
import com.openexchange.antivirus.AntiVirusEncapsulationUtil;
import com.openexchange.antivirus.AntiVirusResult;
import com.openexchange.antivirus.AntiVirusResultEvaluatorService;
import com.openexchange.antivirus.AntiVirusService;
import com.openexchange.antivirus.exceptions.AntiVirusServiceExceptionCodes;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.Attachment;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.json.action.attachments.AttachmentHandler;
import com.openexchange.chronos.json.action.attachments.ContentIDAttachmentHandler;
import com.openexchange.chronos.json.action.attachments.DriveAttachmentHandler;
import com.openexchange.chronos.json.converter.EventConflictResultConverter;
import com.openexchange.chronos.json.converter.mapper.AlarmMapper;
import com.openexchange.chronos.json.converter.mapper.EventMapper;
import com.openexchange.chronos.json.oauth.ChronosOAuthScope;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccess;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccessFactory;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.EventID;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.attach.AttachmentConfig;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.groupware.upload.impl.UploadEvent;
import com.openexchange.java.Strings;
import com.openexchange.java.util.TimeZones;
import com.openexchange.principalusecount.PrincipalUseCountService;
import com.openexchange.search.SearchTerm;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.tools.id.IDMangler;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link ChronosAction}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.0
 */
@RestrictedAction(type = RestrictedAction.Type.READ, module = ChronosAction.MODULE)
public abstract class ChronosAction extends AbstractChronosAction {

    public static final String MODULE = ChronosOAuthScope.MODULE;

    private static final String DRIVE_HANDLER_SCHEME = "drive";
    private static final String CID_HANDLER_SCHEME = "cid";

    protected static final String EVENT = "event";
    protected static final String EVENTS = "events";
    protected static final String BODY_PARAM_COMMENT = "comment";
    protected static final String PARAM_USED_GROUP = "usedGroups";

    private final Map<String, AttachmentHandler> attachmentHandlers = new HashMap<>(2);

    /**
     * Initializes a new {@link ChronosAction}.
     *
     * @param services A service lookup reference
     */
    protected ChronosAction(ServiceLookup services) {
        super(services);

        attachmentHandlers.put(DRIVE_HANDLER_SCHEME, new DriveAttachmentHandler(services));
        attachmentHandlers.put(CID_HANDLER_SCHEME, new ContentIDAttachmentHandler());
    }

    @Override
    public AJAXRequestResult perform(AJAXRequestData requestData, ServerSession session) throws OXException {
        AJAXRequestResult result;
        IDBasedCalendarAccess calendarAccess = initCalendarAccess(requestData);
        boolean committed = false;
        try {
            calendarAccess.startTransaction();
            result = perform(calendarAccess, requestData);
            calendarAccess.commit();
            committed = true;
            if (!EventConflictResultConverter.INPUT_FORMAT.equals(result.getFormat())) {
                incrementGroupUseCount(requestData);
            }
        } finally {
            if (false == committed) {
                calendarAccess.rollback();
            }
            calendarAccess.finish();
        }
        List<OXException> warnings = calendarAccess.getWarnings();
        if (null != warnings && 0 < warnings.size()) {
            result.addWarnings(warnings);
        }
        return result;
    }

    /**
     * Gets the timezone used to interpret <i>Time</i> parameters in for the underlying request.
     *
     * @param requestData The request data sent by the client
     * @return The timezone
     */
    protected static TimeZone getTimeZone(AJAXRequestData requestData) {
        String timezoneId = requestData.getParameter("timezone");
        if (Strings.isEmpty(timezoneId)) {
            ServerSession session = requestData.getSession();
            if (session != null) {
                timezoneId = session.getUser().getTimeZone();
            }
        }
        return CalendarUtils.optTimeZone(timezoneId, TimeZones.UTC);
    }

    /**
     * Returns the {@link SearchTerm} from the JSON array field 'filter' in the specified request.
     *
     * @return the search term The search term
     * @throws OXException if the data or the field 'filter' is missing or any other error is occurred
     */
    protected static SearchTerm<?> getSearchTerm(AJAXRequestData requestData) throws OXException {
        JSONObject data = getJSONData(requestData);
        JSONArray jsonArray = data.optJSONArray(AJAXServlet.PARAMETER_FILTER);
        if (null == jsonArray) {
            throw OXJSONExceptionCodes.MISSING_FIELD.create(AJAXServlet.PARAMETER_FILTER);
        }
        return ChronosSearchTermParser.INSTANCE.parseSearchTerm(jsonArray);
    }

    /**
     * Returns the folder identifiers from the specified request data body.
     *
     * @param requestData The request data
     * @return The folder identifiers as {@link List}
     * @throws OXException if no body present or any of the required fields are absent
     */
    protected static List<String> getFolderIds(AJAXRequestData requestData) throws OXException {
        JSONObject data = getJSONData(requestData);
        try {
            JSONArray jsonArray = data.optJSONArray("folders");
            if (null == jsonArray) {
                return ImmutableList.of();
            }
            List<String> folderIds = new ArrayList<>(jsonArray.length());
            for (int i = 0; i < jsonArray.length(); i++) {
                folderIds.add(jsonArray.getString(i));
            }
            return folderIds;
        } catch (JSONException e) {
            throw AjaxExceptionCodes.INVALID_JSON_REQUEST_BODY.create();
        }
    }

    /**
     * Performs a request.
     *
     * @param calendarAccess The initialized calendar access to use
     * @param requestData The underlying request data
     * @return The request result
     */
    protected abstract AJAXRequestResult perform(IDBasedCalendarAccess calendarAccess, AJAXRequestData requestData) throws OXException;

    /**
     * Handles the specified {@link OXException} and if it is of type
     * {@link Category#CATEGORY_CONFLICT}, it returns the correct response.
     * Otherwise, the original exception is re-thrown
     *
     * @param e The {@link OXException} to handle
     * @return The proper conflict response
     * @throws OXException The original {@link OXException} if not of type {@link Category#CATEGORY_CONFLICT}
     */
    AJAXRequestResult handleConflictException(OXException e) throws OXException {
        if (isConflict(e)) {
            return new AJAXRequestResult(e.getProblematics(), EventConflictResultConverter.INPUT_FORMAT);
        }
        throw e;
    }

    /**
     * Initializes the calendar access for a request and parses all known parameters supplied by the client, throwing an appropriate
     * exception in case a required parameters is missing.
     *
     * @param requestData The underlying request data
     * @return The initialized calendar access
     */
    protected IDBasedCalendarAccess initCalendarAccess(AJAXRequestData requestData) throws OXException {
        CalendarParameters calendarParameters = parseParameters(requestData, getRequiredParameters(), getOptionalParameters());
        return requireService(IDBasedCalendarAccessFactory.class).createAccess(requestData.getSession(), calendarParameters);
    }

    /**
     * Parses the {@link Event} from the payload object of the specified {@link AJAXRequestData}.
     * Any {@link Attachment} uploads will also be handled and properly attached to the {@link Event}.
     *
     * @param requestData The {@link AJAXRequestData}
     * @return The parsed {@link Event}
     * @throws OXException if a parsing error occurs
     */
    protected Event parseEvent(AJAXRequestData requestData) throws OXException {
        return parseEvent(requestData, null);
    }

    /**
     * Parses the {@link Event} from the payload object of the specified {@link AJAXRequestData}.
     * Any {@link Attachment} uploads will also be handled and properly attached to the {@link Event}.
     *
     * @param requestData The {@link AJAXRequestData}
     * @param params Optional parameter map to be filled, if the payload is contained in a sub object
     * @return The parsed {@link Event}
     * @throws OXException if a parsing error occurs
     */
    protected Event parseEvent(AJAXRequestData requestData, Map<String, Object> params) throws OXException {
        Map<String, UploadFile> uploads = new HashMap<>();
        JSONObject jsonEvent;
        long maxUploadSize = AttachmentConfig.getMaxUploadSize();
        if (requestData.hasUploads(-1, maxUploadSize > 0 ? maxUploadSize : -1L)) {
            jsonEvent = handleUploads(requestData, uploads, params);
        } else {
            jsonEvent = extractJsonBody(requestData, params);
        }
        try {
            Event event = EventMapper.getInstance().deserialize(jsonEvent, EventMapper.getInstance().getMappedFields(), getTimeZone(requestData));
            processAttachments(requestData.getSession(), uploads, event);
            return event;
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_READ_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Parses a list of alarms from the supplied json array.
     *
     * @param jsonArray The json array to parse the alarms from
     * @param timeZone The timezone to consider, or <code>null</code> if not relevant
     * @return The parsed alarms, or <code>null</code> if passed json array was <code>null</code>
     */
    protected List<Alarm> parseAlarms(JSONArray jsonArray, TimeZone timeZone) throws OXException {
        if (null == jsonArray) {
            return null;
        }
        try {
            List<Alarm> alarms = new ArrayList<Alarm>(jsonArray.length());
            for (int i = 0; i < jsonArray.length(); i++) {
                alarms.add(AlarmMapper.getInstance().deserialize(jsonArray.getJSONObject(i), AlarmMapper.getInstance().getMappedFields(), timeZone));
            }
            return alarms;
        } catch (JSONException e) {
            throw AjaxExceptionCodes.INVALID_JSON_REQUEST_BODY.create(e);
        }
    }

    /**
     * Processes any attachments the {@link Event} might have. It simply sets
     * the 'data' field of each attachment, if applicable.
     *
     * @param session The session
     * @param uploads The uploaded attachments' data
     * @param event The event that contains the metadata of the attachments
     * @throws OXException if there are missing references between attachment metadata and attachment body parts
     */
    private void processAttachments(Session session, Map<String, UploadFile> uploads, Event event) throws OXException {
        if (!event.containsAttachments() || null == event.getAttachments()) {
            return;
        }
        for (Attachment attachment : event.getAttachments()) {
            // Skip all non-URI- and managed attachments
            if (null == attachment.getUri() || 0 < attachment.getManagedId()) {
                continue;
            }
            String scheme = null;
            try {
                scheme = new URI(attachment.getUri()).getScheme();
            } catch (URISyntaxException e) {
                LOG.debug("Error interpreting client-supplied attachment URI '{}', passing as-is.", attachment.getUri(), e);
            }
            if (null == scheme) {
                continue;
            }
            AttachmentHandler handler = attachmentHandlers.get(scheme);
            if (handler == null) {
                continue;
            }
            handler.handle(session, uploads, attachment);
        }
    }

    /**
     * Handles the file uploads and extracts the {@link JSONObject} payload from the upload request.
     *
     * @param requestData The {@link AJAXRequestData}
     * @param uploads The {@link Map} with the uploads
     * @param params Optional parameter map to be filled, if the payload is contained in a sub object
     * @return The {@link JSONObject} payload of the POST request
     * @throws OXException if an error is occurred
     */
    private JSONObject handleUploads(AJAXRequestData requestData, Map<String, UploadFile> uploads, Map<String, Object> params) throws OXException {
        UploadEvent uploadEvent = requestData.getUploadEvent();
        final List<UploadFile> uploadFiles = uploadEvent.getUploadFiles();
        for (UploadFile uploadFile : uploadFiles) {
            String contentId = uploadFile.getContentId();
            if (Strings.isEmpty(contentId)) {
                contentId = uploadFile.getFieldName(); // fallback to 'name'
            }
            if (Strings.isEmpty(contentId)) {
                throw AjaxExceptionCodes.BAD_REQUEST_CUSTOM.create("Unable to extract the Content-ID for the attachment.");
            }
            uploads.put(contentId, uploadFile);
        }

        return extractJsonBody(uploadEvent, params);
    }

    /**
     * Extracts the {@link JSONObject} payload from the specified {@link AJAXRequestData}
     *
     * @param requestData the {@link AJAXRequestData} to extract the {@link JSONObject} payload from
     * @return The extracted {@link JSONObject} payload
     * @throws OXException if the payload is missing, or a parsing error occurs
     */
    protected JSONObject extractJsonBody(AJAXRequestData requestData) throws OXException {
        Object data = requestData.getData();
        if (!(data instanceof JSONObject)) {
            throw AjaxExceptionCodes.ILLEGAL_REQUEST_BODY.create();
        }
        return (JSONObject) data;
    }

    /**
     * Extracts the {@link JSONObject} payload from the specified {@link AJAXRequestData}
     *
     * @param requestData the {@link AJAXRequestData} to extract the {@link JSONObject} payload from
     * @param params Optional parameter map to be filled, if the payload is contained in a sub object
     * @return The extracted {@link JSONObject} payload
     * @throws OXException if the payload is missing, or a parsing error occurs
     */
    protected JSONObject extractJsonBody(AJAXRequestData requestData, Map<String, Object> params) throws OXException {
        // Check if there is an upload event from drive, i.e. just a JSON payload
        // with the attachments from the file-store
        UploadEvent uploadEvent = requestData.getUploadEvent();
        if (uploadEvent != null) {
            return extractJsonBody(uploadEvent, params);
        }

        Object data = requestData.getData();
        if (!(data instanceof JSONObject)) {
            throw AjaxExceptionCodes.ILLEGAL_REQUEST_BODY.create();
        }
        try {
            return getEventJSONObject(params, (JSONObject) data);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Extracts the {@link JSONObject} payload from the specified {@link UploadEvent}
     *
     * @param upload the {@link UploadEvent} to extract the {@link JSONObject} payload from
     * @return The extracted {@link JSONObject} payload
     * @throws OXException if the payload is missing, or a parsing error occurs
     */
    private JSONObject extractJsonBody(UploadEvent upload, Map<String, Object> params) throws OXException {
        try {
            final String obj = upload.getFormField("json_0");
            if (Strings.isEmpty(obj)) {
                throw AjaxExceptionCodes.ILLEGAL_REQUEST_BODY.create();
            }

            JSONObject json = new JSONObject();
            json.reset();
            json.parseJSONString(obj);

            return getEventJSONObject(params, json);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Extracts the event object from the specified payload
     *
     * @param params The params
     * @param payload The payload
     * @return The event
     * @throws JSONException if a JSON error is occurred
     */
    private JSONObject getEventJSONObject(Map<String, Object> params, JSONObject payload) throws JSONException {
        if (!payload.has(EVENT) || params == null) {
            return payload;
        }
        for (String key : payload.keySet()) {
            if (key.equals(EVENT)) {
                continue;
            }
            params.put(key, payload.get(key));
        }
        return payload.getJSONObject(EVENT);
    }

    /**
     * Scans the specified IFileHolder and sends a 403 error to the client if the enclosed stream is infected.
     *
     * @param requestData The {@link AJAXRequestData}
     * @param fileHolder The {@link IFileHolder}
     * @param uniqueId the unique identifier
     * @return <code>true</code> if a scan was performed; <code>false</code> otherwise
     * @throws OXException if the file is too large, or if the {@link AntiVirusService} is absent,
     *             or if the file is infected, or if a timeout or any other error is occurred.
     */
    protected boolean scan(AJAXRequestData requestData, IFileHolder fileHolder, String uniqueId) throws OXException {
        String scan = requestData.getParameter("scan");
        Boolean s = Strings.isEmpty(scan) ? Boolean.FALSE : Boolean.valueOf(scan);
        if (false == s.booleanValue()) {
            LOG.debug("No anti-virus scanning was performed.");
            return false;
        }
        AntiVirusService antiVirusService = services.getOptionalService(AntiVirusService.class);
        if (antiVirusService == null) {
            throw AntiVirusServiceExceptionCodes.ANTI_VIRUS_SERVICE_ABSENT.create();
        }
        if (false == antiVirusService.isEnabled(requestData.getSession())) {
            return false;
        }
        AntiVirusEncapsulatedContent content = AntiVirusEncapsulationUtil.encapsulate(requestData.optHttpServletRequest(), requestData.optHttpServletResponse());
        AntiVirusResult result = antiVirusService.scan(fileHolder, uniqueId, content);
        services.getServiceSafe(AntiVirusResultEvaluatorService.class).evaluate(result, fileHolder.getName());
        return result.isStreamScanned();
    }

    /**
     * Retrieves a unique id for the attachment
     *
     * @param contextId The context id
     * @param eventId The {@link EventID}
     * @param managedId The managed ID
     * @return A unique ID for the attachment to scan
     */
    protected String getUniqueId(int contextId, EventID eventId, String managedId) {
        // Use also the occurrence id to distinguish any exceptions in the series
        // and in case that exception may have different attachments that the master series?
        return IDMangler.mangle(Integer.toString(contextId), eventId.getFolderID(), eventId.getObjectID(), /* eventId.getRecurrenceID().toString(), */ managedId);
    }

    /**
     * Increments the use-count for used groups
     *
     * @param requestData The {@link AJAXRequestData}
     */
    private void incrementGroupUseCount(AJAXRequestData requestData) {

        String groupsString = requestData.getParameter(PARAM_USED_GROUP);
        if (Strings.isEmpty(groupsString)) {
            // Nothing to do here
            return;
        }
        String[] groups = Strings.splitByCommaNotInQuotes(groupsString);
        PrincipalUseCountService principalUseCountService = services.getOptionalService(PrincipalUseCountService.class);
        if (principalUseCountService == null) {
            LOG.debug("Missing {} service.", PrincipalUseCountService.class.getName());
            return;
        }

        ThreadPoolService threadPoolService = services.getOptionalService(ThreadPoolService.class);
        if (threadPoolService != null) {
            threadPoolService.getExecutor().execute(() -> {
                incrementGroupUseCount(requestData.getSession(), principalUseCountService, groups);
            });
        } else {
            incrementGroupUseCount(requestData.getSession(), principalUseCountService, groups);
        }

    }

    /**
     * Increments the use-count for the given groups
     *
     * @param session The user session
     * @param principalUseCountService The {@link PrincipalUseCountService} to use
     * @param groups The groups to increase
     */
    private void incrementGroupUseCount(Session session, PrincipalUseCountService principalUseCountService, String[] groups) {
        for (String group : groups) {
            try {
                principalUseCountService.increment(session, Integer.parseInt(group));
            } catch (NumberFormatException e) {
                LOG.warn("Unable to parse group id: {}", e.getMessage());
            } catch (OXException e) {
                // Nothing to do here
                LOG.error(e.getMessage(), e);
            }
        }
    }
}
