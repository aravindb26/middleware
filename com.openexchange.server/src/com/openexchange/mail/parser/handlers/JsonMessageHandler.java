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

package com.openexchange.mail.parser.handlers;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Strings.isEmpty;
import static com.openexchange.mail.mime.utils.MimeMessageUtility.decodeMultiEncodedHeader;
import static com.openexchange.mail.parser.MailMessageParser.generateFilename;
import static com.openexchange.mail.parser.MailMessageParser.getFileName;
import static com.openexchange.mail.utils.MailFolderUtility.prepareFullname;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.idn.IDNA;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.common.collect.ImmutableSet;
import com.google.common.html.HtmlEscapers;
import com.openexchange.ajax.fields.DataFields;
import com.openexchange.ajax.fields.FolderChildFields;
import com.openexchange.ajax.tools.JSONCoercion;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.alias.UserAliasStorage;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.html.HtmlExceptionCodes;
import com.openexchange.html.HtmlExceptionMessages;
import com.openexchange.html.HtmlSanitizeResult;
import com.openexchange.html.HtmlService;
import com.openexchange.image.ImageLocation;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailJSONField;
import com.openexchange.mail.MailListField;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.api.IMailProperties;
import com.openexchange.mail.attachment.AttachmentToken;
import com.openexchange.mail.attachment.AttachmentTokenConstants;
import com.openexchange.mail.attachment.AttachmentTokenService;
import com.openexchange.mail.authenticity.CustomPropertyJsonHandler;
import com.openexchange.mail.authenticity.MailAuthenticityResultKey;
import com.openexchange.mail.authenticity.MailAuthenticityStatus;
import com.openexchange.mail.authenticity.mechanism.MailAuthenticityMechanismResult;
import com.openexchange.mail.compose.HeaderUtility;
import com.openexchange.mail.compose.SharedAttachmentReference;
import com.openexchange.mail.config.MailAccountProperties;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.conversion.InlineImageDataSource;
import com.openexchange.mail.dataobjects.MailAuthenticityResult;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.dataobjects.MailStructure;
import com.openexchange.mail.dataobjects.SecurityInfo;
import com.openexchange.mail.dataobjects.SecurityResult;
import com.openexchange.mail.dataobjects.SignatureResult;
import com.openexchange.mail.json.compose.share.AttachmentStorageRegistry;
import com.openexchange.mail.json.compose.share.FileItem;
import com.openexchange.mail.json.compose.share.spi.AttachmentStorage;
import com.openexchange.mail.json.osgi.MailJSONActivator;
import com.openexchange.mail.json.writer.MessageWriter;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.HeaderName;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.MimeType2ExtMap;
import com.openexchange.mail.mime.MimeTypes;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.mime.utils.MimeMessageUtility;
import com.openexchange.mail.parser.ContentProvider;
import com.openexchange.mail.parser.MailMessageHandler;
import com.openexchange.mail.parser.MailMessageParser;
import com.openexchange.mail.text.Enriched2HtmlConverter;
import com.openexchange.mail.text.HtmlProcessing;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.utils.DisplayMode;
import com.openexchange.mail.utils.MaxBytesExceededIOException;
import com.openexchange.mail.utils.MessageUtility;
import com.openexchange.mail.utils.SizePolicy;
import com.openexchange.mail.uuencode.UUEncodedPart;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.TimeZoneUtils;
import com.openexchange.tools.filename.FileNameTools;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;
import gnu.trove.procedure.TCharProcedure;
import gnu.trove.set.TCharSet;
import gnu.trove.set.hash.TCharHashSet;

/**
 * {@link JsonMessageHandler} - Generates a JSON message representation considering user-sensitive data.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class JsonMessageHandler implements MailMessageHandler {

    private static final Property PARSE_NESTED_IMAGES = DefaultProperty.valueOf("com.openexchange.mail.handler.image.parseNested", Boolean.TRUE);

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(JsonMessageHandler.class);

    private static final String CONTENT = MailJSONField.CONTENT.getKey();
    private static final String DISPOSITION = MailJSONField.DISPOSITION.getKey();
    private static final String SIZE = MailJSONField.SIZE.getKey();
    private static final String CONTENT_TYPE = MailJSONField.CONTENT_TYPE.getKey();
    private static final String ID = MailListField.ID.getKey();
    private static final String PRIORITY = MailJSONField.PRIORITY.getKey();
    private static final String NESTED_MESSAGES = MailJSONField.NESTED_MESSAGES.getKey();
    private static final String ATTACHMENTS = MailJSONField.ATTACHMENTS.getKey();
    private static final String ACCOUNT_ID = MailJSONField.ACCOUNT_ID.getKey();
    private static final String MALICIOUS = "malicious";
    private static final String ACCOUNT_NAME = MailJSONField.ACCOUNT_NAME.getKey();
    private static final String HAS_ATTACHMENTS = MailJSONField.HAS_ATTACHMENTS.getKey();
    private static final String HAS_REAL_ATTACHMENTS = "real_attachment";
    private static final String UNREAD = MailJSONField.UNREAD.getKey();
    private static final String ATTACHMENT_FILE_NAME = MailJSONField.ATTACHMENT_FILE_NAME.getKey();
    private static final String FROM = MailJSONField.FROM.getKey();
    private static final String SENDER = MailJSONField.SENDER.getKey();
    private static final String REPLY_TO = MailJSONField.REPLY_TO.getKey();
    private static final String CID = MailJSONField.CID.getKey();
    private static final String COLOR_LABEL = MailJSONField.COLOR_LABEL.getKey();
    private static final String RECIPIENT_CC = MailJSONField.RECIPIENT_CC.getKey();
    private static final String RECIPIENT_BCC = MailJSONField.RECIPIENT_BCC.getKey();
    private static final String HEADERS = MailJSONField.HEADERS.getKey();
    private static final String ORIGINAL_ID = MailJSONField.ORIGINAL_ID.getKey();
    private static final String ORIGINAL_FOLDER_ID = MailJSONField.ORIGINAL_FOLDER_ID.getKey();
    private static final String SECURITY = MailJSONField.SECURITY.getKey();
    private static final String SECURITY_INFO = MailJSONField.SECURITY_INFO.getKey();
    private static final String TEXT_PREVIEW = MailJSONField.TEXT_PREVIEW.getKey();
    private static final String AUTHENTICATION_RESULTS = MailJSONField.AUTHENTICITY.getKey();

    private static final String TRUNCATED = MailJSONField.TRUNCATED.getKey();
    private static final String SANITIZED = "sanitized";

    private static final String VIRTUAL = "___VIRTUAL___";
    private static final String MULTIPART_ID = "___MP-ID___";

    private static final String HTML_PREFIX = HtmlExceptionCodes.PREFIX;

    private static final class PlainTextContent {

        final String id;
        final String contentType;
        final String content;
        final boolean truncated;

        PlainTextContent(String id, String contentType, String content, boolean truncated) {
            super();
            this.id = id;
            this.contentType = contentType;
            this.content = content;
            this.truncated = truncated;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder(256);
            builder.append("PlainTextContent [");
            if (id != null) {
                builder.append("id=").append(id).append(", ");
            }
            if (contentType != null) {
                builder.append("contentType=").append(contentType).append(", ");
            }
            if (content != null) {
                builder.append("content=").append(content);
            }
            builder.append(']');
            return builder.toString();
        }
    } // End of class PlainTextContent

    private static final class MultipartInfo {

        final String mpId;
        final ContentType contentType;

        MultipartInfo(String mpId, ContentType contentType) {
            super();
            this.mpId = mpId;
            this.contentType = contentType;
        }

        boolean isSubType(String subtype) {
            return null != contentType && contentType.startsWith("multipart/" + subtype);
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder(256);
            builder.append("MultipartInfo [");
            if (mpId != null) {
                builder.append("mpId=").append(mpId).append(", ");
            }
            if (contentType != null) {
                builder.append("contentType=").append(contentType);
            }
            builder.append(']');
            return builder.toString();
        }

    } // End of class MultipartInfo

    // -----------------------------------------------------------------------------------------------------------------

    private final List<OXException> warnings;
    private final Session session;
    private final Context ctx;
    private final LinkedList<MultipartInfo> multiparts;
    private TimeZone timeZone;
    private final UserSettingMail usm;
    private final DisplayMode displayMode;
    private final int accountId;
    private final MailPath mailPath;
    private final MailPath originalMailPath;
    private final JSONObject jsonObject;
    private AttachmentListing attachmentListing;
    private JSONArray nestedMsgsArr;
    private boolean isAlternative;
    private String altId;
    private boolean textAppended;
    private boolean textWasEmpty;
    private final boolean[] modified;
    private PlainTextContent plainText;
    private String tokenFolder;
    private String tokenMailId;
    private final boolean token;
    private final int ttlMillis;
    private final boolean sanitize;
    private final boolean embedded;
    private final boolean asMarkup;
    private boolean attachHTMLAlternativePart;
    private boolean includePlainText;
    private SizePolicy sizePolicy;
    private final int maxContentSize;
    private int currentNestingLevel = 0;
    private final int maxNestedMessageLevels;
    private String initialiserSequenceId;
    private final IMailProperties mailProperties;
    private boolean handleNestedMessageAsAttachment;
    private AttachmentStorage attachmentStorage = null;
    private final boolean parseNestedImages;
    private final MailStructure mailStructure;

    /**
     * Initializes a new {@link JsonMessageHandler}
     *
     * @param accountId The account ID
     * @param mailPath The unique mail path
     * @param displayMode The display mode
     * @param embedded <code>true</code> for embedded display (CSS prefixed, &lt;body&gt; replaced with &lt;div&gt;); otherwise <code>false</code>
     * @param asMarkup <code>true</code> if the content is supposed to be rendered as HTML (be it HTML or plain text); otherwise <code>false</code> to keep content as-is (plain text is left as such)
     * @param session The session providing needed user data
     * @param usm The mail settings used for preparing message content if <code>displayVersion</code> is set to <code>true</code>; otherwise it is ignored.
     * @throws OXException If JSON message handler cannot be initialized
     */
    public JsonMessageHandler(int accountId, String mailPath, DisplayMode displayMode, boolean embedded, boolean asMarkup, Session session, UserSettingMail usm, boolean token, int ttlMillis) throws OXException {
        this(accountId, new MailPath(mailPath), null, displayMode, true, embedded, asMarkup, session, usm, getContext(session), token, ttlMillis, -1, null);
    }

    /**
     * Initializes a new {@link JsonMessageHandler}
     *
     * @param accountId The account ID
     * @param mailPath The unique mail path
     * @param displayMode The display mode
     * @param sanitize Whether HTML/CSS content is supposed to be sanitized (against white-list)
     * @param embedded <code>true</code> for embedded display (CSS prefixed, &lt;body&gt; replaced with &lt;div&gt;); otherwise <code>false</code>
     * @param asMarkup <code>true</code> if the content is supposed to be rendered as HTML (be it HTML or plain text); otherwise <code>false</code> to keep content as-is (plain text is left as such)
     * @param session The session providing needed user data
     * @param usm The mail settings used for preparing message content if <code>displayVersion</code> is set to <code>true</code>; otherwise it is ignored.
     * @throws OXException If JSON message handler cannot be initialized
     */
    public JsonMessageHandler(int accountId, String mailPath, DisplayMode displayMode, boolean sanitize, boolean embedded, boolean asMarkup, Session session, UserSettingMail usm, boolean token, int ttlMillis) throws OXException {
        this(accountId, new MailPath(mailPath), null, displayMode, sanitize, embedded, asMarkup, session, usm, getContext(session), token, ttlMillis, -1, null);
    }

    /**
     * Initializes a new {@link JsonMessageHandler}
     *
     * @param accountId The account ID
     * @param mailPath The unique mail path
     * @param mail The mail message to add JSON fields not set by message parser traversal
     * @param displayMode The display mode
     * @param embedded <code>true</code> for embedded display (CSS prefixed, &lt;body&gt; replaced with &lt;div&gt;); otherwise <code>false</code>
     * @param asMarkup <code>true</code> if the content is supposed to be rendered as HTML (be it HTML or plain text); otherwise <code>false</code> to keep content as-is (plain text is left as such)
     * @param session The session providing needed user data
     * @param usm The mail settings used for preparing message content if <code>displayVersion</code> is set to <code>true</code>; otherwise it is ignored.
     * @param token <code>true</code> to add attachment tokens
     * @param ttlMillis The tokens' timeout
     * @throws OXException If JSON message handler cannot be initialized
     */
    public JsonMessageHandler(int accountId, MailPath mailPath, MailMessage mail, DisplayMode displayMode, boolean embedded, boolean asMarkup, Session session, UserSettingMail usm, boolean token, int ttlMillis) throws OXException {
        this(accountId, mailPath, mail, displayMode, true, embedded, asMarkup, session, usm, getContext(session), token, ttlMillis, -1, null);
    }

    /**
     * Initializes a new {@link JsonMessageHandler}
     *
     * @param accountId The account ID
     * @param mailPath The unique mail path
     * @param mail The mail message to add JSON fields not set by message parser traversal
     * @param displayMode The display mode
     * @param sanitize Whether HTML/CSS content is supposed to be sanitized (against white-list)
     * @param embedded <code>true</code> for embedded display (CSS prefixed, &lt;body&gt; replaced with &lt;div&gt;); otherwise <code>false</code>
     * @param asMarkup <code>true</code> if the content is supposed to be rendered as HTML (be it HTML or plain text); otherwise <code>false</code> to keep content as-is (plain text is left as such)
     * @param session The session providing needed user data
     * @param usm The mail settings used for preparing message content if <code>displayVersion</code> is set to <code>true</code>; otherwise it is ignored.
     * @param token <code>true</code> to add attachment tokens
     * @param ttlMillis The tokens' timeout
     * @throws OXException If JSON message handler cannot be initialized
     */
    public JsonMessageHandler(int accountId, MailPath mailPath, MailMessage mail, DisplayMode displayMode, boolean sanitize, boolean embedded, boolean asMarkup, Session session, UserSettingMail usm, boolean token, int ttlMillis) throws OXException {
        this(accountId, mailPath, mail, displayMode, sanitize, embedded, asMarkup, session, usm, getContext(session), token, ttlMillis, -1, null);
    }

    /**
     * Initializes a new {@link JsonMessageHandler}
     *
     * @param accountId The account ID
     * @param mailPath The unique mail path
     * @param mail The mail message to add JSON fields not set by message parser traversal
     * @param displayMode The display mode
     * @param sanitize Whether HTML/CSS content is supposed to be sanitized (against white-list)
     * @param embedded <code>true</code> for embedded display (CSS prefixed, &lt;body&gt; replaced with &lt;div&gt;); otherwise <code>false</code>
     * @param asMarkup <code>true</code> if the content is supposed to be rendered as HTML (be it HTML or plain text); otherwise <code>false</code> to keep content as-is (plain text is left as such)
     * @param session The session providing needed user data
     * @param usm The mail settings used for preparing message content if <code>displayVersion</code> is set to <code>true</code>; otherwise it is ignored.
     * @param token <code>true</code> to add attachment tokens
     * @param ttlMillis The tokens' timeout
     * @param maxContentSize maximum number of bytes that is will be returned for content. '<=0' means unlimited.
     * @param maxNestedMessageLevels The number of levels in which deep-parsing of nested messages takes place; otherwise only ID information is set; '<=0' falls back to default value (10)
     * @throws OXException If JSON message handler cannot be initialized
     */
    public JsonMessageHandler(int accountId, MailPath mailPath, MailMessage mail, DisplayMode displayMode, boolean sanitize, boolean embedded, boolean asMarkup, Session session, UserSettingMail usm, boolean token, int ttlMillis, int maxContentSize, int maxNestedMessageLevels) throws OXException {
        this(accountId, mailPath, mail, displayMode, sanitize, embedded, asMarkup, session, usm, getContext(session), token, ttlMillis, maxContentSize, null);
    }

    private static Context getContext(Session session) throws OXException {
        if (session instanceof ServerSession) {
            return ((ServerSession) session).getContext();
        }
        return ContextStorage.getStorageContext(session.getContextId());
    }

    /**
     * Initializes a new {@link JsonMessageHandler} for internal usage
     */
    private JsonMessageHandler(int accountId, MailPath mailPath, MailMessage mail, DisplayMode displayMode, boolean sanitize, boolean embedded, boolean asMarkup, Session session, UserSettingMail usm, Context ctx, boolean token, int ttlMillis, int maxContentSize, MailStructure mailStructure) throws OXException {
        super();
        this.warnings = new LinkedList<>();
        this.multiparts = new LinkedList<MultipartInfo>();
        this.sanitize = sanitize;
        this.embedded = DisplayMode.DOCUMENT.equals(displayMode) ? false : embedded;
        this.asMarkup = asMarkup;
        this.attachHTMLAlternativePart = !usm.isSuppressHTMLAlternativePart();
        this.ttlMillis = ttlMillis;
        this.token = token;
        this.accountId = accountId;
        this.modified = new boolean[1];
        this.session = session;
        this.mailProperties = new MailAccountProperties(null, session.getUserId(), session.getContextId());
        this.ctx = ctx;
        this.usm = usm;
        this.displayMode = displayMode;
        this.mailPath = mailPath;
        this.maxContentSize = maxContentSize;
        this.jsonObject = new JSONObject(32);
        this.maxNestedMessageLevels = 1;
        this.handleNestedMessageAsAttachment = false;
        LeanConfigurationService configurationService = ServerServiceRegistry.getServize(LeanConfigurationService.class, true);
        this.parseNestedImages = configurationService.getBooleanProperty(PARSE_NESTED_IMAGES);
        this.mailStructure = mailStructure == null ? (mail == null ? null : mail.getMailStructure()) : mailStructure;
        try {
            if (DisplayMode.MODIFYABLE.equals(this.displayMode) && null != mailPath) {
                jsonObject.put(MailJSONField.MSGREF.getKey(), mailPath.toString());
            }
            MailPath originalMailPath = null;
            if (null != mail) {
                /*
                 * Add missing fields
                 */
                final String mailId = mail.getMailId();
                if (mail.containsFolder() && mailId != null) {
                    tokenFolder = prepareFullname(accountId, mail.getFolder());
                    jsonObject.put(FolderChildFields.FOLDER_ID, tokenFolder);
                    tokenMailId = mailId;
                    jsonObject.put(DataFields.ID, mailId);
                }
                final int unreadMessages = mail.getUnreadMessages();
                if (unreadMessages >= 0) {
                    jsonObject.put(UNREAD, unreadMessages);
                }
                jsonObject.put(HAS_ATTACHMENTS, mail.hasAttachment());
                jsonObject.put(CONTENT_TYPE, mail.getContentType().getBaseType());
                jsonObject.put(SIZE, mail.getSize());
                jsonObject.put(ACCOUNT_NAME, mail.getAccountName());
                jsonObject.put(ACCOUNT_ID, mail.getAccountId());
                jsonObject.put(MALICIOUS, usm.isSuppressLinks());
                if (mail.containsTextPreview()) {
                    jsonObject.put(TEXT_PREVIEW, mail.getTextPreview());
                }
                MailAuthenticityResult mailAuthenticityResult = mail.getAuthenticityResult();
                jsonObject.put(AUTHENTICATION_RESULTS, null == mailAuthenticityResult ? null : JsonMessageHandler.authenticationMechanismResultsToJson(mailAuthenticityResult));
                // Guard info
                if (mail.containsSecurityInfo()) {
                    SecurityInfo securityInfo = mail.getSecurityInfo();
                    if (null != securityInfo) {
                        jsonObject.put(SECURITY_INFO, securityInfoToJSON(securityInfo));
                    }
                }
                if (mail.hasSecurityResult()) {
                    SecurityResult securityResult = mail.getSecurityResult();
                    if (null != securityResult) {
                        jsonObject.put(SECURITY, securityResultToJSON(securityResult));
                    }
                }

                this.initialiserSequenceId = mail.getSequenceId();

                String originalId = null;
                if (mail.containsOriginalId()) {
                    originalId = mail.getOriginalId();
                    if (null != originalId) {
                        jsonObject.put(ORIGINAL_ID, originalId);
                    }
                }
                FullnameArgument originalFolder = null;
                if (mail.containsOriginalFolder()) {
                    originalFolder = mail.getOriginalFolder();
                    if (null != originalFolder) {
                        jsonObject.put(ORIGINAL_FOLDER_ID, originalFolder.getPreparedName());
                    }
                }
                if (null != originalId && null != originalFolder) {
                    originalMailPath = new MailPath(originalFolder.getAccountId(), originalFolder.getFullName(), originalId);
                }
            }
            this.originalMailPath = originalMailPath;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Transforms given security result to JSON.
     *
     * @param result The security result associated with the mail
     * @return The JSON representation of security result
     * @throws JSONException If JSON cannot be returned
     */
    public JSONObject securityResultToJSON(SecurityResult result) throws JSONException {
        JSONObject json = new JSONObject(10);
        json.put("decrypted", result.getSuccess());
        json.put("type", result.getType().toString());
        if (result.hasError()) {
            json.put("error", result.getError());
        }
        json.put("pgpInline", result.isPgpInline());
        if (result.hasSignatureResults()) {
            List<SignatureResult> signatureResults = result.getSignatureResults();
            JSONArray signatures = new JSONArray(signatureResults.size());
            for (SignatureResult res : signatureResults) {
                JSONObject sig = new JSONObject(4);
                sig.put("verified", res.isVerified());
                sig.put("missing", res.isMissing());
                sig.put("date", res.getDate());
                sig.put("issuerKeyId", res.getIssuerKeyId());
                sig.put("issuerKeyFingerprint",res.getIssuerKeyFingerprint());
                sig.put("issuerUserIds", res.getIssuerUserIds());
                if (res.getError() != null) {
                    sig.put("error", res.getError());
                }
                signatures.put(sig);
            }
            json.put("signatures", signatures);
        }
        return json;
    }

    /**
     * Creates the JSON representation for specified <code>SecurityInfo</code> instance.
     *
     * @return The JSON representation
     * @throws JSONException If JSON representation cannot be returned
     */
    public JSONObject securityInfoToJSON(SecurityInfo info) throws JSONException {
        JSONObject security = new JSONObject(2);
        security.put("encrypted", info.isEncrypted());
        security.put("signed", info.isSigned());
        security.put("type", info.getType());
        return security;
    }

    /**
     * Creates the JSON representation of the essential information for the specified {@link MailAuthenticityResult} instance.
     * That is the <code>status</code> and the <code>trustedDomain</code> (if present)
     *
     * @param authenticityResult The authenticity result to create the JSON representation for
     * @return The JSON representation or <code>null</code> if no authenticity result available
     * @throws JSONException If JSON representation cannot be returned
     */
    public static JSONObject authenticityOverallResultToJson(MailAuthenticityResult authenticityResult) throws JSONException {
        if (null == authenticityResult) {
            return null;
        }

        JSONObject result = new JSONObject(2);
        result.put("status", authenticityResult.getStatus().getTechnicalName());
        if (MailAuthenticityStatus.TRUSTED.equals(authenticityResult.getStatus()) && authenticityResult.getAttribute(MailAuthenticityResultKey.IMAGE) != null) {
            result.put("image", authenticityResult.getAttribute(MailAuthenticityResultKey.IMAGE));
        }
        return result;
    }

    /**
     * Creates the JSON representation for specified <code>MailAuthenticityResult</code> instance.
     *
     * @param authenticityResult The authenticity result to create the JSON representation for
     * @return The JSON representation or <code>null</code> if no authenticity result available
     * @throws JSONException If JSON representation cannot be returned
     */
    @SuppressWarnings("unchecked")
    public static JSONObject authenticationMechanismResultsToJson(MailAuthenticityResult authenticityResult) throws JSONException {
        if (null == authenticityResult) {
            return null;
        }

        JSONObject result;
        Map<MailAuthenticityResultKey, Object> attributes = authenticityResult.getAttributes();
        int numOfAttributes = attributes.size();
        if (numOfAttributes > 0) {
            result = new JSONObject(numOfAttributes);
            JSONArray unconsideredResults = new JSONArray();
            for (Entry<MailAuthenticityResultKey, Object> entry : attributes.entrySet()) {
                if (!entry.getKey().isVisible()) {
                    continue;
                }
                Object object = entry.getValue();
                if (object instanceof Collection<?>) {
                    Collection<?> col = (Collection<?>) object;

                    for (Object o : col) {
                        if (o instanceof MailAuthenticityMechanismResult) {
                            MailAuthenticityMechanismResult mechResult = (MailAuthenticityMechanismResult) o;
                            JSONObject mailAuthMechResultJson = new JSONObject();
                            mailAuthMechResultJson.put("result", mechResult.getResult().getTechnicalName());
                            mailAuthMechResultJson.put("reason", mechResult.getReason());
                            for (String k : mechResult.getProperties().keySet()) {
                                mailAuthMechResultJson.put(k, mechResult.getProperties().get(k));
                            }
                            result.put(mechResult.getMechanism().getTechnicalName(), mailAuthMechResultJson);
                        } else if (o instanceof Map) {
                            unconsideredResults.put(JSONCoercion.coerceToJSON(o));
                        } else {
                            unconsideredResults.put(o);
                        }
                    }
                } else {
                    result.put(entry.getKey().getKey(), entry.getValue());
                }
            }
            if (MailAuthenticityStatus.TRUSTED.equals(authenticityResult.getStatus()) && authenticityResult.getAttribute(MailAuthenticityResultKey.IMAGE) != null) {
                result.put("image", authenticityResult.getAttribute(MailAuthenticityResultKey.IMAGE));
            }
            result.put("unconsidered_results", unconsideredResults);
        } else {
            result = new JSONObject();
        }

        result.put("status", authenticityResult.getStatus().getTechnicalName());

        if (numOfAttributes > 0) {
            CustomPropertyJsonHandler customPropertyJsonHandler = MailJSONActivator.SERVICES.get().getOptionalService(CustomPropertyJsonHandler.class);
            if (customPropertyJsonHandler != null) {
                result.put("custom", customPropertyJsonHandler.toJson(authenticityResult.getAttribute(MailAuthenticityResultKey.CUSTOM_PROPERTIES, Map.class)));
            }
        }

        return result;
    }

    /**
     * Sets whether to set the exact length of mail parts.
     *
     * @param exactLength <code>true</code> to set the exact length of mail parts; otherwise use mail system's size estimation
     * @return This {@link JsonMessageHandler} with new behaviour applied
     */
    public JsonMessageHandler setSizePolicy(SizePolicy sizePolicy) {
        this.sizePolicy = sizePolicy;
        return this;
    }

    /**
     * Sets to handle nested messages as attachments
     *
     * @return This {@link JsonMessageHandler} with new behavior applied
     */
    public JsonMessageHandler setHandleNestedMessageAsAttachment() {
        this.handleNestedMessageAsAttachment = true;
        return this;
    }

    /**
     * Sets whether the HTML part of a <i>multipart/alternative</i> content shall be attached.
     *
     * @param attachHTMLAlternativePart Whether the HTML part of a <i>multipart/alternative</i> content shall be attached
     * @return This {@link JsonMessageHandler} with new behavior applied
     */
    public JsonMessageHandler setAttachHTMLAlternativePart(boolean attachHTMLAlternativePart) {
        this.attachHTMLAlternativePart = attachHTMLAlternativePart;
        return this;
    }

    /**
     * Sets whether to include raw plain-text in generated JSON object.
     *
     * @param includePlainText <code>true</code> to include raw plain-text; otherwise <code>false</code>
     * @return This {@link JsonMessageHandler} with new behavior applied
     */
    public JsonMessageHandler setIncludePlainText(boolean includePlainText) {
        this.includePlainText = includePlainText;
        return this;
    }

    public String getInitialiserSequenceId() {
        return initialiserSequenceId;
    }

    public void setInitialiserSequenceId(String initialiserSequenceId) {
        this.initialiserSequenceId = initialiserSequenceId;
    }

    /**
     * Gets the attachment storage for Link Mail.
     *
     * @return The attachment storage
     * @throws OXException If attachment storage cannot be returned
     */
    private AttachmentStorage getAttachmentStorage() throws OXException {
        AttachmentStorage attachmentStorage = this.attachmentStorage;
        if (attachmentStorage == null) {
            AttachmentStorageRegistry attachmentStorageRegistry = ServerServiceRegistry.getInstance().getService(AttachmentStorageRegistry.class, true);
            attachmentStorage = attachmentStorageRegistry.getAttachmentStorageFor(session);
            this.attachmentStorage = attachmentStorage;
        }
        return attachmentStorage;
    }

    private AttachmentListing getAttachmentListing() {
        if (attachmentListing == null) {
            attachmentListing = new AttachmentListing();
        }
        return attachmentListing;
    }

    private JSONArray getNestedMsgsArr() throws JSONException {
        if (nestedMsgsArr == null) {
            nestedMsgsArr = new JSONArray();
            jsonObject.put(NESTED_MESSAGES, nestedMsgsArr);
        }
        return nestedMsgsArr;
    }

    private TimeZone getTimeZone() throws OXException {
        if (timeZone == null) {
            if (session instanceof ServerSession) {
                timeZone = TimeZoneUtils.getTimeZone(((ServerSession) session).getUser().getTimeZone());
            } else {
                timeZone = TimeZoneUtils.getTimeZone(UserStorage.getInstance().getUser(session.getUserId(), ctx).getTimeZone());
            }
        }
        return timeZone;
    }

    /**
     * Sets the time zone.
     *
     * @param timeZone The time zone
     * @return This handler with time zone applied
     */
    public JsonMessageHandler setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    private void addToken(JSONObject jsonObject, String attachmentId) {
        if (token && null != tokenFolder && null != tokenMailId) {
            /*
             * Token
             */
            try {
                final AttachmentToken token = new AttachmentToken(ttlMillis <= 0 ? AttachmentTokenConstants.DEFAULT_TIMEOUT : ttlMillis);
                token.setAccessInfo(accountId, session);
                token.setAttachmentInfo(tokenFolder, tokenMailId, attachmentId);
                AttachmentTokenService service = ServerServiceRegistry.getInstance().getService(AttachmentTokenService.class, true);
                service.putToken(token, session);
                final JSONObject attachmentObject = new JSONObject(2);
                attachmentObject.put("id", token.getId());
                attachmentObject.put("jsessionid", token.getJSessionId());
                jsonObject.put("token", attachmentObject);
            } catch (Exception e) {
                LOG.warn("Adding attachment token failed.", e);
            }
        }
    }

    @Override
    public boolean handleAttachment(MailPart part, boolean isInline, String baseContentType, String fileName, String id) throws OXException {
        if (isInline && isAlternative && null != altId && id.startsWith(altId) && baseContentType.startsWith("text/xml")) {
            // Ignore
            return true;
        }

        // Handle attachment
        return handleAttachment0(part, isInline, null, baseContentType, fileName, id, false);
    }

    private boolean handleAttachment0(MailPart part, boolean isInline, String disposition, String baseContentType, String fileName, String id, boolean handleAsInlineImage) throws OXException {
        try {
            final JSONObject jsonObject = new JSONObject(8);
            /*
             * Sequence ID
             */
            final String attachmentId = part.containsSequenceId() ? part.getSequenceId() : id;
            jsonObject.put(ID, attachmentId);
            /*
             * Filename
             */
            if (fileName == null) {
                final Object val;
                if (isInline) {
                    val = JSONObject.NULL;
                } else {
                    val = generateFilename(id, baseContentType);
                    part.setFileName((String) val);
                }
                jsonObject.put(ATTACHMENT_FILE_NAME, val);
            } else {
                jsonObject.put(ATTACHMENT_FILE_NAME, FileNameTools.sanitizeFilename(fileName));
            }
            /*
             * Size
             */
            {
                boolean checkSize = true;
                /*
                 * Check for shared attachment
                 */
                String headerValue = HeaderUtility.decodeHeaderValue(part.getFirstHeader(HeaderUtility.HEADER_X_OX_SHARED_ATTACHMENT_REFERENCE));
                if (headerValue != null) {
                    SharedAttachmentReference sharedAttachmentRef = HeaderUtility.headerValue2SharedAttachmentReference(headerValue);
                    if (sharedAttachmentRef != null) {
                        FileItem sharedAttachment = getAttachmentStorage().getAttachment(sharedAttachmentRef.getAttachmentId(), sharedAttachmentRef.getFolderId(), ServerSessionAdapter.valueOf(session));
                        jsonObject.put(SIZE, sharedAttachment.getSize());
                        checkSize = false;
                    }
                }
                if (checkSize) {
                    if (SizePolicy.EXACT == sizePolicy) {
                        try {
                            jsonObject.put(SIZE, Streams.countInputStream(part.getInputStream()));
                            checkSize = false;
                        } catch (Exception e) {
                            // Failed counting part's content
                            LOG.debug("{}", e.getMessage(), e);
                        }
                    } else if (SizePolicy.ESTIMATE == sizePolicy) {
                        if (part.containsSize()) {
                            String transferEncoding = Strings.asciiLowerCase(part.getFirstHeader(MessageHeaders.HDR_CONTENT_TRANSFER_ENC));
                            if ("base64".equals(transferEncoding)) {
                                jsonObject.put(SIZE, (int) (0.75 * part.getSize()));
                                checkSize = false;
                            }
                        }
                    }
                }
                if (checkSize && part.containsSize()) {
                    jsonObject.put(SIZE, part.getSize());
                }
            }
            /*
             * Disposition
             */
            jsonObject.put(DISPOSITION, null == disposition ? Part.ATTACHMENT : disposition);
            /*
             * Content-ID
             */
            if (part.containsContentId()) {
                final String contentId = part.getContentId();
                if (contentId != null) {
                    jsonObject.put(CID, contentId);
                }
            }
            /*
             * Content-Type
             */
            {
                ContentType clone = new ContentType();
                clone.setContentType(part.getContentType());
                clone.removeNameParameter();
                jsonObject.put(CONTENT_TYPE, clone.toString());
            }
            /*
             * Content
             */
            jsonObject.put(CONTENT, JSONObject.NULL);
            /*
             * Add token
             */
            addToken(jsonObject, attachmentId);
            /*
             * Add attachment
             */
            if (handleAsInlineImage) {
                getAttachmentListing().addRemainder(jsonObject);
            } else {
                getAttachmentListing().add(jsonObject);
            }
            return true;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public boolean handleBccRecipient(InternetAddress[] recipientAddrs) throws OXException {
        try {
            jsonObject.put(RECIPIENT_BCC, MessageWriter.getAddressesAsArray(recipientAddrs));
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
        return true;
    }

    @Override
    public boolean handleCcRecipient(InternetAddress[] recipientAddrs) throws OXException {
        try {
            jsonObject.put(RECIPIENT_CC, MessageWriter.getAddressesAsArray(recipientAddrs));
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
        return true;
    }

    @Override
    public boolean handleColorLabel(int colorLabel) throws OXException {
        try {
            jsonObject.put(COLOR_LABEL, colorLabel);
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
        return true;
    }

    @Override
    public boolean handleContentId(String contentId) throws OXException {
        try {
            jsonObject.put(CID, contentId);
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
        return true;
    }

    @Override
    public boolean handleFrom(InternetAddress[] fromAddrs) throws OXException {
        try {
            jsonObject.put(FROM, MessageWriter.getAddressesAsArray(fromAddrs));
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
        return true;
    }

    @Override
    public boolean handleSender(InternetAddress[] senderAddrs) throws OXException {
        try {
            jsonObject.put(SENDER, MessageWriter.getAddressesAsArray(senderAddrs));
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
        return true;
    }

    @Override
    public boolean handleReplyTo(InternetAddress[] replyToAddrs) throws OXException {
        try {
            jsonObject.put(REPLY_TO, MessageWriter.getAddressesAsArray(replyToAddrs));
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
        return true;
    }

    /**
     * These headers are covered by fields of {@link MailMessage}
     */
    private static final Set<HeaderName> COVERED_HEADER_NAMES = Set.of(MessageHeaders.CONTENT_DISPOSITION, MessageHeaders.CONTENT_ID, MessageHeaders.CONTENT_TYPE, MessageHeaders.BCC, MessageHeaders.CC, MessageHeaders.DATE, MessageHeaders.DISP_NOT_TO, MessageHeaders.FROM, MessageHeaders.X_PRIORITY, MessageHeaders.SUBJECT, MessageHeaders.TO, MessageHeaders.SENDER);

    @Override
    public boolean handleHeaders(int size, Iterator<Entry<String, String>> iter) throws OXException {
        if (size == 0) {
            return true;
        }
        try {
            final JSONObject hdrObject = new JSONObject(size);
            for (int i = size; i-- > 0;) {
                final Map.Entry<String, String> entry = iter.next();
                final String headerName = entry.getKey();
                if (MessageHeaders.HDR_DISP_NOT_TO.equalsIgnoreCase(headerName)) {
                    /*
                     * This special header is handled through handleDispositionNotification()
                     */
                    continue;
                } else if (MessageHeaders.HDR_IMPORTANCE.equalsIgnoreCase(headerName)) {
                    /*
                     * Priority
                     */
                    int priority = MailMessage.PRIORITY_NORMAL;
                    if (null != entry.getValue()) {
                        priority = MimeMessageUtility.parseImportance(entry.getValue());
                        jsonObject.put(PRIORITY, priority);
                    }
                } else if (MessageHeaders.HDR_X_PRIORITY.equalsIgnoreCase(headerName)) {
                    if (!jsonObject.has(PRIORITY)) {
                        /*
                         * Priority
                         */
                        int priority = MailMessage.PRIORITY_NORMAL;
                        if (null != entry.getValue()) {
                            priority = MimeMessageUtility.parsePriority(entry.getValue());
                        }
                        jsonObject.put(PRIORITY, priority);
                    }
                } else if (MessageHeaders.HDR_X_MAILER.equalsIgnoreCase(headerName)) {
                    hdrObject.put(headerName, entry.getValue());
                } else if (MessageHeaders.HDR_X_OX_VCARD.equalsIgnoreCase(headerName)) {
                    jsonObject.put(MailJSONField.VCARD.getKey(), true);
                } else if (MessageHeaders.HDR_X_OX_NOTIFICATION.equalsIgnoreCase(headerName)) {
                    jsonObject.put(MailJSONField.DISPOSITION_NOTIFICATION_TO.getKey(), entry.getValue());
                } else {
                    if (!COVERED_HEADER_NAMES.contains(HeaderName.valueOf(headerName))) {
                        if (hdrObject.has(headerName)) {
                            final Object previous = hdrObject.get(headerName);
                            if (previous instanceof JSONArray) {
                                final JSONArray ja = (JSONArray) previous;
                                ja.put(prepareHeaderValue(headerName, entry.getValue()));
                            } else {
                                final JSONArray ja = new JSONArray();
                                ja.put(previous);
                                ja.put(prepareHeaderValue(headerName, entry.getValue()));
                                hdrObject.put(headerName, ja);
                            }
                        } else {
                            hdrObject.put(headerName, prepareHeaderValue(headerName, entry.getValue()));
                        }
                    }
                }
            }
            jsonObject.put(HEADERS, hdrObject);
            return true;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private static final Set<String> ADDRESS_HEADERS = ImmutableSet.of("resent-from", "resent-to", "resent-sender", "resent-cc",
        "resent-bcc", "from", "sender", "reply-to", "to", "cc", "bcc");

    private static String prepareHeaderValue(String headerName, String headerValue) {
        if (ADDRESS_HEADERS.contains(Strings.asciiLowerCase(headerName)) == false) {
            // Regular header
            return decodeMultiEncodedHeader(headerValue);
        }

        try {
            InternetAddress[] addresses = QuotedInternetAddress.parse(headerValue, true);
            if (addresses.length <= 0) {
                return "";
            }

            StringBuilder unicodeAddressLine = new StringBuilder(addresses.length << 4);
            unicodeAddressLine.append(addresses[0].toUnicodeString());
            for (int i = 1; i < addresses.length; i++) {
                unicodeAddressLine.append(", ").append(addresses[1].toUnicodeString());
            }
            return unicodeAddressLine.toString();
        } catch (AddressException e) {
            LOG.debug("", e);
            return decodeMultiEncodedHeader(headerValue);
        }
    }

    @Override
    public boolean handleImagePart(MailPart part, String imageCID, String baseContentType, boolean isInline, String fileName, String id) throws OXException {
        // Check for inline image
        boolean considerAsInline = isInline || (!part.getContentDisposition().isAttachment() && part.containsHeader("Content-Id"));

        // Handle it...
        if (asMarkup && considerAsInline && (DisplayMode.MODIFYABLE.getMode() < displayMode.getMode())) {
            final MultipartInfo mpInfo = multiparts.peek();
            if (null != mpInfo && textAppended && id.startsWith(mpInfo.mpId) && mpInfo.isSubType("mixed")) {
                try {
                    List<JSONObject> attachments = getAttachmentListing().getAttachments();
                    int len = attachments.size();
                    String keyContentType = CONTENT_TYPE;
                    String keyContent = CONTENT;
                    String keySize = SIZE;
                    MailPath mailPath = this.mailPath;

                    boolean b = true;
                    for (int i = len; b && i-- > 0;) {
                        JSONObject jAttachment = attachments.get(i);
                        if (jAttachment.getString(keyContentType).startsWith("text/plain") && null != mailPath) {
                            try {
                                final String imageURL;
                                {
                                    final InlineImageDataSource imgSource = InlineImageDataSource.getInstance();
                                    final ImageLocation imageLocation = new ImageLocation.Builder(fileName).folder(prepareFullname(accountId, mailPath.getFolder())).id(mailPath.getMailID()).build();
                                    imageURL = imgSource.generateUrl(imageLocation, session);
                                }
                                final String imgTag = "<img src=\"" + imageURL + "&scaleType=contain&width=800\" alt=\"\" style=\"display: block\" id=\"" + HtmlEscapers.htmlEscaper().escape(fileName) + "\">";
                                final String content = jAttachment.getString(keyContent);
                                final String newContent = content + imgTag;
                                jAttachment.put(keyContent, newContent);
                                jAttachment.put(keySize, newContent.length());
                                b = false;
                            } catch (Exception e) {
                                LOG.error("Error while inlining image part.", e);
                            }
                        }
                    }

                    if (b) { // No suitable text/plain
                        try {
                            for (int i = len; b && i-- > 0;) {
                                JSONObject jAttachment = attachments.get(i);
                                // Is HTML and in same multipart
                                if (jAttachment.optString(CONTENT_TYPE, "").startsWith("text/htm") && mpInfo.mpId.equals(jAttachment.optString(MULTIPART_ID, null))) {
                                    String content = jAttachment.optString(CONTENT, "null");
                                    if (!"null".equals(content) && null != mailPath) {
                                        try {
                                            // Append to first one
                                            final String imageURL;
                                            {
                                                final InlineImageDataSource imgSource = InlineImageDataSource.getInstance();
                                                final ImageLocation imageLocation = new ImageLocation.Builder(fileName).folder(prepareFullname(accountId, mailPath.getFolder())).id(mailPath.getMailID()).build();
                                                imageURL = imgSource.generateUrl(imageLocation, session);
                                            }
                                            final String imgTag = "<img src=\"" + imageURL + "&scaleType=contain&width=800\" alt=\"\" style=\"display: block\" id=\"" + HtmlEscapers.htmlEscaper().escape(fileName) + "\">";
                                            content = new StringBuilder(content).append(imgTag).toString();
                                            jAttachment.put(CONTENT, content);
                                            b = false;
                                        } catch (Exception e) {
                                            LOG.error("Error while inlining image part.", e);
                                        }
                                    }
                                }
                            }
                        } catch (RuntimeException e) {
                            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
                        }
                    }
                } catch (JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        }

        // Swallow images with Content-Disposition simply set to "inline" having no file name
        if (mailProperties.hideInlineImages() && MessageUtility.seemsInlineImage(part, true)) {
            return true;
        }

        return handleAttachment0(part, considerAsInline, considerAsInline ? Part.INLINE : Part.ATTACHMENT, baseContentType, fileName, id, considerAsInline);
    }

    @Override
    public boolean handleInlineHtml(ContentProvider contentProvider, ContentType contentType, long size, String fileName, String id) throws OXException {
        String htmlContent;
        try {
            htmlContent = contentProvider.getContent();
        } catch (OXException x) {
            Throwable cause = x.getCause();
            if (cause instanceof MaxBytesExceededIOException) {
                MaxBytesExceededIOException mbe = (MaxBytesExceededIOException) cause;
                if (plainText != null) {
                    OXException e = HtmlExceptionCodes.TOO_BIG.create(x, L(mbe.getMaxSize()), L(mbe.getSize()));
                    warnings.add(e.setCategory(Category.CATEGORY_WARNING).setDisplayMessage(HtmlExceptionMessages.PARSING_FAILED_WITH_FAILOVERMSG, e.getDisplayArgs()));
                    asRawContent(plainText.id, plainText.contentType, new HtmlSanitizeResult(plainText.content, plainText.truncated));
                    textAppended = true;
                    return true;
                }
            }

            throw x;
        }
        String identifier = id;
        /*
         * Adjust DI if virtually inserted; e.g. MimeForward
         */
        if (isVirtual(contentType)) {
            identifier = "0";
        }
        if (textAppended) {
            /*
             * A text part has already been detected as message's body
             */
            MailPath mailPath = this.mailPath;
            if (isAlternative) {
                if (DisplayMode.DISPLAY.isIncluded(displayMode)) {
                    /*
                     * Check if previously appended text part was empty
                     */
                    if (textWasEmpty) {
                        if (usm.isDisplayHtmlInlineContent()) {
                            JSONObject jsonObject = asDisplayHtml(identifier, contentType.getBaseType(), htmlContent, contentType.getCharsetParameter());
                            if (includePlainText) {
                                try {
                                    String plainText = html2text(htmlContent);
                                    jsonObject.put("plain_text", plainText);
                                } catch (JSONException e) {
                                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                                }
                            }
                        } else {
                            try {
                                asDisplayText(identifier, contentType.getBaseType(), htmlContent, fileName, false);
                                getAttachmentListing().removeFirst();
                            } catch (RuntimeException e) {
                                throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
                            }
                        }
                    }
                    /*
                     * Check if nested in same multipart
                     */
                    try {
                        MultipartInfo mpInfo = multiparts.peek();
                        List<JSONObject> attachments = getAttachmentListing().getAttachments();
                        int length = attachments.size();
                        for (int i = length; i-- > 0;) {
                            JSONObject jAttachment = attachments.get(i);
                            // Is HTML and in same multipart
                            if (jAttachment.optString(CONTENT_TYPE, "").startsWith("text/htm") && null != mpInfo && mpInfo.mpId.equals(jAttachment.optString(MULTIPART_ID, null)) && mpInfo.isSubType("mixed")) {
                                String content = jAttachment.optString(CONTENT, "null");
                                if (!"null".equals(content) && null != mailPath) {
                                    // Append to first one
                                    HtmlSanitizeResult sanitizeResult = HtmlProcessing.formatHTMLForDisplay(htmlContent, contentType.getCharsetParameter(), session, mailPath, originalMailPath, usm, modified, displayMode, sanitize, embedded, asMarkup, maxContentSize, mailStructure);
                                    content = new StringBuilder(content).append(sanitizeResult.getContent()).toString();
                                    jAttachment.put(CONTENT, content);
                                    return true;
                                }
                            }
                        }
                    } catch (JSONException e) {
                        throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                    }
                    /*
                     * Add HTML alternative part as attachment
                     */
                    if (attachHTMLAlternativePart) {
                        try {
                            JSONObject attachment = asAttachment(identifier, contentType.getBaseType(), htmlContent.length(), fileName, null);
                            attachment.put(VIRTUAL, true);
                        } catch (JSONException e) {
                            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                        }
                    }
                } else if (DisplayMode.RAW.equals(displayMode)) {
                    /*
                     * Return HTML content as-is
                     */
                    asRawContent(identifier, contentType.getBaseType(), new HtmlSanitizeResult(htmlContent));
                } else {
                    /*
                     * Discard
                     */
                    return true;
                }
            } else {
                try {
                    MultipartInfo mpInfo = multiparts.peek();
                    List<JSONObject> attachments = getAttachmentListing().getAttachments();
                    int length = attachments.size();
                    for (int i = length; i-- > 0;) {
                        JSONObject jAttachment = attachments.get(i);
                        // Is HTML and in same multipart
                        if (jAttachment.optString(CONTENT_TYPE, "").startsWith("text/htm") && null != mpInfo && mpInfo.mpId.equals(jAttachment.optString(MULTIPART_ID, null)) && mpInfo.isSubType("mixed")) {
                            String content = jAttachment.optString(CONTENT, "null");
                            if (!"null".equals(content) && null != mailPath) {
                                // Append to first one
                                HtmlSanitizeResult sanitizeResult = HtmlProcessing.formatHTMLForDisplay(htmlContent, contentType.getCharsetParameter(), session, mailPath, originalMailPath, usm, modified, displayMode, sanitize, embedded, asMarkup, maxContentSize, mailStructure);
                                content = new StringBuilder(content).append(sanitizeResult.getContent()).toString();
                                jAttachment.put(CONTENT, content);
                                return true;
                            }
                        }
                    }
                } catch (JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
                /*
                 * Add HTML part as attachment
                 */
                try {
                    JSONObject attachment = asAttachment(identifier, contentType.getBaseType(), htmlContent.length(), fileName, null);
                    attachment.put(VIRTUAL, true);
                } catch (JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        } else {
            /*
             * No text part was present before
             */
            if (DisplayMode.MODIFYABLE.getMode() <= displayMode.getMode()) {
                if (usm.isDisplayHtmlInlineContent()) {
                    /*
                     * Check if HTML is empty or has an empty body section
                     */
                    if ((com.openexchange.java.Strings.isEmpty(htmlContent) || (htmlContent.length() < 1024 && hasNoImage(htmlContent) && isEmpty(html2text(htmlContent)))) && plainText != null) {
                        /*
                         * No text present
                         */
                        asRawContent(plainText.id, plainText.contentType, new HtmlSanitizeResult(plainText.content, plainText.truncated));
                    } else {
                        JSONObject jsonObject = asDisplayHtml(identifier, contentType.getBaseType(), htmlContent, contentType.getCharsetParameter());
                        if (includePlainText) {
                            try {
                                /*
                                 * Try to convert the given HTML to regular text
                                 */
                                String plainText = html2text(htmlContent);
                                jsonObject.put("plain_text", plainText);
                            } catch (JSONException e) {
                                throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                            }
                        }
                    }
                } else {
                    asDisplayText(identifier, contentType.getBaseType(), htmlContent, fileName, DisplayMode.DISPLAY.isIncluded(displayMode));
                }
            } else if (DisplayMode.RAW.equals(displayMode)) {
                /*
                 * Return HTML content as-is
                 */
                asRawContent(identifier, contentType.getBaseType(), new HtmlSanitizeResult(htmlContent));
            } else {
                try {
                    JSONObject jsonObject = new JSONObject(6);
                    jsonObject.put(ID, identifier);
                    jsonObject.put(CONTENT_TYPE, contentType.getBaseType());
                    jsonObject.put(SIZE, htmlContent.length());
                    jsonObject.put(DISPOSITION, Part.INLINE);
                    jsonObject.put(CONTENT, htmlContent);
                    getAttachmentListing().add(jsonObject);
                } catch (JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
            textAppended = true;
        }
        return true;
    }

    private static final Enriched2HtmlConverter ENRCONV = new Enriched2HtmlConverter();

    @Override
    public boolean handleInlinePlainText(String plainTextContentArg, ContentType contentType, long size, String fileName, String id) throws OXException {
        String identifier = id;
        if (isAlternative && usm.isDisplayHtmlInlineContent() && (DisplayMode.RAW.getMode() < displayMode.getMode()) && contentType.startsWith(MimeTypes.MIME_TEXT_PLAIN)) {
            /*
             * User wants to see message's alternative content
             */
            if (null == plainText) {
                /*
                 * Remember plain-text content
                 */
                HtmlSanitizeResult sanitizeResult = HtmlProcessing.formatTextForDisplay(plainTextContentArg, usm, displayMode, asMarkup, maxContentSize);
                plainText = new PlainTextContent(identifier, contentType.getBaseType(), sanitizeResult.getContent(), sanitizeResult.isTruncated());
            }
            return true;
        }
        try {
            /*
             * Adjust DI if virtually inserted; e.g. MimeForward
             */
            if (isVirtual(contentType)) {
                identifier = "0";
            }
            if (contentType.startsWith(MimeTypes.MIME_TEXT_ENRICHED) || contentType.startsWith(MimeTypes.MIME_TEXT_RICHTEXT) || contentType.startsWith(MimeTypes.MIME_TEXT_RTF)) {
                if (textAppended) {
                    if (DisplayMode.DISPLAY.isIncluded(displayMode)) {
                        /*
                         * Add alternative part as attachment
                         */
                        try {
                            JSONObject attachment = asAttachment(identifier, contentType.getBaseType(), plainTextContentArg.length(), fileName, null);
                            attachment.put(VIRTUAL, true);
                        } catch (JSONException e) {
                            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                        }
                        return true;
                    } else if (DisplayMode.RAW.equals(displayMode)) {
                        /*
                         * Return plain-text content as-is
                         */
                        asRawContent(identifier, contentType.getBaseType(), new HtmlSanitizeResult(plainTextContentArg));
                    }
                    /*
                     * Discard
                     */
                    return true;
                }
                /*
                 * No text part was present before
                 */
                if (DisplayMode.MODIFYABLE.getMode() <= displayMode.getMode()) {
                    final JSONObject textObject;
                    if (usm.isDisplayHtmlInlineContent()) {
                        textObject = asDisplayHtml(identifier, contentType.getBaseType(), getHtmlDisplayVersion(contentType, plainTextContentArg), contentType.getCharsetParameter());
                    } else {
                        textObject = asDisplayText(identifier, contentType.getBaseType(), getHtmlDisplayVersion(contentType, plainTextContentArg), fileName, DisplayMode.DISPLAY.isIncluded(displayMode));
                    }
                    if (includePlainText && !textObject.has("plain_text")) {
                        textObject.put("plain_text", plainTextContentArg);
                    }
                } else if (DisplayMode.RAW.equals(displayMode)) {
                    /*
                     * Return plain-text content as-is
                     */
                    asRawContent(identifier, contentType.getBaseType(), new HtmlSanitizeResult(plainTextContentArg));
                } else {
                    final JSONObject jsonObject = new JSONObject(6);
                    jsonObject.put(ID, identifier);
                    jsonObject.put(DISPOSITION, Part.INLINE);
                    jsonObject.put(CONTENT_TYPE, contentType.getBaseType());
                    jsonObject.put(SIZE, plainTextContentArg.length());
                    jsonObject.put(CONTENT, plainTextContentArg);
                    if (includePlainText) {
                        jsonObject.put("plain_text", plainTextContentArg);
                    }
                    getAttachmentListing().add(jsonObject);
                }
                textAppended = true;
                return true;
            }
            /*
             * Just common plain text
             */
            if (plainText != null && !textAppended && !isAlternative) {
                /*
                 * There is a plain text part from previous multipart/alternative processing
                 */
                HtmlSanitizeResult sanitizeResult = new HtmlSanitizeResult(plainText.content, plainText.truncated);
                if (null != sanitizeResult.getContent() && 0 != sanitizeResult.getContent().length()) {
                    // Not empty...
                    JSONObject textObject = asPlainText(plainText.id, plainText.contentType, sanitizeResult);
                    if (includePlainText) {
                        textObject.put("plain_text", plainTextContentArg);
                    }
                    textAppended = true;
                    textWasEmpty = false;
                }
            }
            if (textAppended) {
                if (textWasEmpty) {
                    final HtmlSanitizeResult content = HtmlProcessing.formatTextForDisplay(plainTextContentArg, usm, displayMode, asMarkup, maxContentSize);
                    final JSONObject textObject = asPlainText(identifier, contentType.getBaseType(), content);
                    if (includePlainText) {
                        textObject.put("plain_text", plainTextContentArg);
                    }
                    textWasEmpty = (null == content.getContent() || 0 == content.getContent().length());
                } else {
                    if (usm.isDisplayHtmlInlineContent()) {
                        // Assume HTML content has been appended before
                        if (DisplayMode.DISPLAY.isIncluded(displayMode)) {
                            /*
                             * Add alternative part as attachment
                             */
                            if (null != contentType.getParameter("realfilename") && plainTextContentArg.length() > 0) {
                                try {
                                    JSONObject attachment = asAttachment(identifier, contentType.getBaseType(), plainTextContentArg.length(), fileName, null);
                                    attachment.put(VIRTUAL, true);
                                } catch (JSONException e) {
                                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                                }
                                return true;
                            }
                        } else if (DisplayMode.RAW.equals(displayMode)) {
                            /*
                             * Return plain-text content as-is
                             */
                            asRawContent(identifier, contentType.getBaseType(), new HtmlSanitizeResult(plainTextContentArg));
                            return true;
                        }
                    }

                    // A plain text message body has already been detected
                    final HtmlSanitizeResult sanitizeResult = HtmlProcessing.formatTextForDisplay(plainTextContentArg, usm, displayMode, asMarkup, maxContentSize);
                    final MultipartInfo mpInfo = multiparts.peek();
                    if (null != mpInfo && (DisplayMode.RAW.getMode() < displayMode.getMode()) && identifier.startsWith(mpInfo.mpId) && mpInfo.isSubType("mixed")) {
                        List<JSONObject> attachments = getAttachmentListing().getAttachments();
                        int len = attachments.size();
                        String keyContentType = CONTENT_TYPE;
                        String keyContent = CONTENT;
                        String keySize = SIZE;
                        boolean b = true;
                        for (int i = len - 1; b && i >= 0; i--) {
                            JSONObject jObject = attachments.get(i);
                            if (jObject.getString(keyContentType).startsWith("text/plain") && jObject.hasAndNotNull(keyContent)) {
                                String currentContent = jObject.getString(keyContent);
                                String toAppend = sanitizeResult.getContent();
                                String newContent;
                                if (currentContent.endsWith("\n") || toAppend.startsWith("\n")) {
                                    newContent = currentContent + toAppend;
                                } else {
                                    newContent = currentContent + "\r\n" + toAppend;
                                }
                                jObject.put(keyContent, newContent);
                                jObject.put(keySize, newContent.length());
                                if (includePlainText && jObject.has("plain_text")) {
                                    jObject.put("plain_text", jObject.getString("plain_text") + plainTextContentArg);
                                }
                                b = false;
                            }
                        }
                    } else {
                        /*
                         * Append inline text as an attachment, too
                         */
                        JSONObject textObject = asAttachment(identifier, contentType.getBaseType(), plainTextContentArg.length(), fileName, new HtmlSanitizeResult(sanitizeResult.getContent()));
                        textObject.put(VIRTUAL, true);
                        if (includePlainText) {
                            textObject.put("plain_text", plainTextContentArg);
                        }
                    }
                }
            } else {
                HtmlSanitizeResult sanitizeResult = HtmlProcessing.formatTextForDisplay(plainTextContentArg, usm, displayMode, asMarkup, maxContentSize);
                final JSONObject textObject = asPlainText(identifier, contentType.getBaseType(), sanitizeResult);
                if (includePlainText) {
                    textObject.put("plain_text", plainTextContentArg);
                }
                textAppended = true;
                textWasEmpty = (null == sanitizeResult.getContent() || 0 == sanitizeResult.getContent().length());
            }
            return true;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private String getHtmlDisplayVersion(ContentType contentType, String src) throws OXException {
        final String baseType = contentType.getBaseType().toLowerCase(Locale.ENGLISH);
        if (baseType.startsWith(MimeTypes.MIME_TEXT_ENRICHED) || baseType.startsWith(MimeTypes.MIME_TEXT_RICHTEXT)) {
            return HtmlProcessing.formatHTMLForDisplay(ENRCONV.convert(src), contentType.getCharsetParameter(), session, mailPath, originalMailPath, usm, modified, displayMode, embedded, asMarkup, mailStructure);
        }
        return HtmlProcessing.formatTextForDisplay(src, usm, displayMode);
    }

    @Override
    public boolean handleInlineUUEncodedAttachment(UUEncodedPart part, String id) throws OXException {
        try {
            final JSONObject jsonObject = new JSONObject(8);
            jsonObject.put(ID, id);
            String contentType = MimeTypes.MIME_APPL_OCTET;
            String filename = part.getFileName();
            try {
                TCharSet separators = new TCharHashSet(new char[] {'/', '\\', File.separatorChar});
                final String fn = filename;
                boolean containsSeparatorChar = false == separators.forEach(new TCharProcedure() {

                    @Override
                    public boolean execute(char separator) {
                        return fn.indexOf(separator) < 0;
                    }
                });

                File file = new File(filename);
                if (containsSeparatorChar) {
                    filename = file.getName();
                    file = new File(filename);
                }

                contentType = Strings.asciiLowerCase(MimeType2ExtMap.getContentType(file.getName()));
            } catch (Exception e) {
                final Throwable t = new Throwable(new StringBuilder("Unable to fetch content/type for '").append(filename).append("': ").append(e).toString());
                LOG.warn("", t);
            }
            jsonObject.put(CONTENT_TYPE, contentType);
            jsonObject.put(ATTACHMENT_FILE_NAME, filename);
            jsonObject.put(SIZE, part.getFileSize());
            jsonObject.put(DISPOSITION, Part.ATTACHMENT);
            /*
             * Content-type indicates mime type text/
             */
            if (contentType.startsWith("text/")) {
                /*
                 * Attach link-object with text content
                 */
                jsonObject.put(CONTENT, part.getPart().toString());
            } else {
                /*
                 * Attach link-object.
                 */
                jsonObject.put(CONTENT, JSONObject.NULL);
            }
            getAttachmentListing().addRemainder(jsonObject);
            return true;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public boolean handleInlineUUEncodedPlainText(String decodedTextContent, ContentType contentType, int size, String fileName, String id) throws OXException {
        return handleInlinePlainText(decodedTextContent, contentType, size, fileName, id);
    }

    @Override
    public void handleMessageEnd(MailMessage mail) throws OXException {
        // Since we obviously touched message's content, mark its corresponding message object as seen
        mail.setFlags(mail.getFlags() | MailMessage.FLAG_SEEN);

        // Check if we did not append any text so far
        if (plainText != null) {
            if (!textAppended) {
                // Append the plain text...
                asRawContent(plainText.id, plainText.contentType, new HtmlSanitizeResult(plainText.content, plainText.truncated), true);
            } else if (textWasEmpty) {
                // Replace the plain text...
                getAttachmentListing().removeFirst();
                asRawContent(plainText.id, plainText.contentType, new HtmlSanitizeResult(plainText.content, plainText.truncated), true);
            }
        }

        // Attachments
        String attachKey = ATTACHMENTS;
        {
            AttachmentListing attachmentListing = this.attachmentListing;
            if (null != attachmentListing) {
                try {
                    jsonObject.put(attachKey, attachmentListing.toJsonArray());
                } catch (JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        }

        try {
            String headersKey = HEADERS;
            if (!jsonObject.hasAndNotNull(headersKey)) {
                jsonObject.put(headersKey, new JSONObject(1));
            }

            String dispKey = DISPOSITION;
            if (jsonObject.hasAndNotNull(attachKey)) {
                JSONArray jAttachments = jsonObject.getJSONArray(attachKey);
                int len = jAttachments.length();
                for (int i = 0; i < len; i++) {
                    JSONObject jAttachment = jAttachments.getJSONObject(i);
                    if (this.initialiserSequenceId != null) {
                        jAttachment.put(ID, this.initialiserSequenceId + "." + jAttachment.getString(ID));
                    }
                    jAttachment.remove(MULTIPART_ID);
                    if (jAttachment.hasAndNotNull(dispKey) && Part.ATTACHMENT.equalsIgnoreCase(jAttachment.getString(dispKey))) {
                        if (jAttachment.hasAndNotNull(VIRTUAL) && jAttachment.getBoolean(VIRTUAL)) {
                            jAttachment.remove(VIRTUAL);
                        } else {
                            if (jsonObject.has(HAS_ATTACHMENTS)) {
                                // Do not overwrite existing "has-attachment" information in a mail's JSON representation
                                // See bug 42695 & 42862
                                jsonObject.put(HAS_REAL_ATTACHMENTS, true);
                            } else {
                                jsonObject.put(HAS_ATTACHMENTS, true);
                            }
                        }
                        if (token && !jAttachment.hasAndNotNull("token")) {
                            try {
                                addToken(jAttachment, jAttachment.getString(ID));
                            } catch (Exception e) {
                                // Missing field "id"
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            LOG.error("", e);
        }
    }

    private boolean isVirtual(ContentType contentType) {
        return "virtual".equals(contentType.getParameter("nature"));
    }

    @Override
    public boolean handleMultipart(MailPart mp, int bodyPartCount, String id) throws OXException {
        /*
         * Determine if message is of MIME type multipart/alternative
         */
        if (mp.getContentType().startsWith(MimeTypes.MIME_MULTIPART_ALTERNATIVE) && bodyPartCount >= 2) {
            isAlternative = true;
            altId = id;
        } else if (null != altId && !id.startsWith(altId)) {
            /*
             * No more within multipart/alternative since current ID is not nested below remembered ID
             */
            isAlternative = false;
        }
        multiparts.push(new MultipartInfo(id, mp.getContentType()));
        return true;
    }

    @Override
    public boolean handleMultipartEnd(MailPart mp, String id) throws OXException {
        if (null != altId && altId.equals(id)) {
            // Leaving multipart/alternative part
            altId = null;
            isAlternative = false;
        }
        multiparts.pop();
        return true;
    }

    private static final String PRIMARY_RFC822 = "message/rfc822";

    @Override
    public boolean handleNestedMessage(MailPart mailPart, String id) throws OXException {
        if (handleNestedMessageAsAttachment) {
            final String disposition = mailPart.containsContentDisposition() ? mailPart.getContentDisposition().getDisposition() : null;
            boolean inline = Part.INLINE.equalsIgnoreCase(disposition) || ((disposition == null) && (mailPart.getFileName() == null));
            ContentType contentType = mailPart.containsContentType() ? mailPart.getContentType() : ContentType.APPLICATION_OCTETSTREAM_CONTENT_TYPE;
            String fileName = getFileName(mailPart.getFileName(), contentType, id, PRIMARY_RFC822);
            return handleAttachment(mailPart, inline, PRIMARY_RFC822, fileName, id);
        }

        String nestedMessageFullId = "";
        try {
            JSONObject nestedObject;
            if (currentNestingLevel < maxNestedMessageLevels) {
                // Get nested message from part
                MailMessage nestedMail = MailMessageParser.getMessageContentFrom(mailPart);
                if (null == nestedMail) {
                    LOG.warn("Ignoring nested message. Cannot handle part's content which should be a RFC822 message according to its content type.");
                    return true;
                }

                // Generate a dedicated JsonMessageHandler instance to parse the nested message
                JsonMessageHandler msgHandler = new JsonMessageHandler(accountId, this.parseNestedImages ? this.mailPath : null, null, displayMode, sanitize, embedded, asMarkup, session, usm, ctx, token, ttlMillis, maxContentSize, mailStructure);
                msgHandler.setTimeZone(timeZone);
                msgHandler.includePlainText = includePlainText;
                msgHandler.attachHTMLAlternativePart = attachHTMLAlternativePart;
                msgHandler.tokenFolder = tokenFolder;
                msgHandler.tokenMailId = tokenMailId;
                msgHandler.sizePolicy = sizePolicy;
                // msgHandler.originalMailPath = originalMailPath;
                msgHandler.currentNestingLevel = currentNestingLevel + 1;
                if (this.initialiserSequenceId != null) {
                    nestedMessageFullId = this.initialiserSequenceId + "." + id;
                    msgHandler.setInitialiserSequenceId(initialiserSequenceId);
                }
                new MailMessageParser().parseMailMessage(nestedMail, msgHandler, id);
                nestedObject = msgHandler.getJSONObject();
                if (nestedMessageFullId.length() != 0) {
                    nestedObject.put(ID, nestedMessageFullId);
                }
                this.warnings.addAll(msgHandler.warnings);
            } else {
                // Only basic information
                nestedObject = new JSONObject(3);

            }
            /*
             * Sequence ID
             */
            if (!nestedObject.has(ID) && this.initialiserSequenceId == null) {
                nestedObject.put(ID, mailPart.containsSequenceId() ? mailPart.getSequenceId() : id);
            } else if (this.initialiserSequenceId != null && this.initialiserSequenceId.length() != 0) {
                nestedObject.put(ID, this.initialiserSequenceId + "." + id);
            }
            /*
             * Filename (if present)
             */
            if (mailPart.containsFileName()) {
                final String name = mailPart.getFileName();
                if (null != name) {
                    nestedObject.put(ATTACHMENT_FILE_NAME, name);
                }
            }
            getNestedMsgsArr().put(nestedObject);
            return true;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public boolean handlePriority(int priority) throws OXException {
        try {
            jsonObject.put(PRIORITY, priority);
            return true;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public boolean handleMsgRef(String msgRef) throws OXException {
        try {
            jsonObject.put(MailJSONField.MSGREF.getKey(), msgRef);
            return true;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public boolean handleDispositionNotification(InternetAddress dispositionNotificationTo, boolean acknowledged) throws OXException {
        try {
            if (!acknowledged) {
                // Check if given address is covered by user's aliases
                UserAliasStorage aliasStorage = ServerServiceRegistry.getInstance().getService(UserAliasStorage.class);
                if (aliasStorage == null || false == determineUserAliases(aliasStorage).contains(dispositionNotificationTo.getAddress())) {
                    // Given address is not covered by user's aliases. Therefore, advertise "disp_notification_to" field in JSON representation
                    jsonObject.put(MailJSONField.DISPOSITION_NOTIFICATION_TO.getKey(), dispositionNotificationTo.toUnicodeString());
                }
            }
            return true;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private Set<String> determineUserAliases(UserAliasStorage aliasStorage) {
        try {
            Set<String> aliases = aliasStorage.getAliases(session.getContextId(), session.getUserId());
            Set<String> preparedAliases = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            for (String address : aliases) {
                String sAddr = IDNA.toIDN(address);
                if (containsControlWhitespaceOrNonAscii(sAddr)) {
                    try {
                        QuotedInternetAddress addr = new QuotedInternetAddress(sAddr, false);
                        preparedAliases.add(addr.getIDNAddress());
                        preparedAliases.add(QuotedInternetAddress.toACE(addr.getAddress()));
                    } catch (Exception e) {
                        // Failed to parse as E-Mail address
                        preparedAliases.add(IDNA.toIDN(extractRealMailAddressFrom(sAddr)));
                    }
                } else {
                    preparedAliases.add(IDNA.toIDN(extractRealMailAddressFrom(sAddr)));
                }
            }
            return preparedAliases;
        } catch (Exception e) {
            LOG.warn("Failed to determine aliases for user {} in context {}", I(session.getUserId()), I(session.getContextId()), e);
            return Collections.emptySet();
        }
    }

    private static boolean containsControlWhitespaceOrNonAscii(String address) {
        int len = address.length();
        for (int i = len; i-- > 0;) {
            char c = address.charAt(i);
            if (c <= 32 || c >= 127) {
                // Address contains control/whitespace or non-ascii character
                return true;
            }
        }
        return false;
    }

    private static String extractRealMailAddressFrom(String sAddress) {
        int index = sAddress.indexOf('<');
        return index < 0 ? sAddress : sAddress.substring(index + 1, sAddress.indexOf('>'));
    }

    @Override
    public boolean handleReceivedDate(Date receivedDate) throws OXException {
        try {
            Object value = receivedDate == null ? JSONObject.NULL : Long.valueOf(MessageWriter.addUserTimezone(receivedDate.getTime(), getTimeZone()));
            jsonObject.put(MailJSONField.RECEIVED_DATE.getKey(), value);
            if (false == MailProperties.getInstance().isPreferSentDate(session.getUserId(), session.getContextId())) {
                jsonObject.put(MailJSONField.DATE.getKey(), value);
            }
            return true;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public boolean handleSentDate(Date sentDate) throws OXException {
        try {
            Object value = sentDate == null ? JSONObject.NULL : Long.valueOf(MessageWriter.addUserTimezone(sentDate.getTime(), getTimeZone()));
            jsonObject.put(MailJSONField.SENT_DATE.getKey(), value);
            if (MailProperties.getInstance().isPreferSentDate(session.getUserId(), session.getContextId())) {
                jsonObject.put(MailJSONField.DATE.getKey(), value);
            }
            return true;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public boolean handleSpecialPart(MailPart part, String baseContentType, String fileName, String id) throws OXException {
        final ContentType contentType = part.getContentType();
        /*
         * When creating a JSON message object from a message we do not distinguish special parts or image parts from "usual" attachments.
         * Therefore invoke the handleAttachment method. Maybe we need a separate handling in the future for vcards.
         */
        if (null == plainText) {
            /*
             * Remember plain-text content
             */
            if (MailMessageParser.isRfc3464Or3798(baseContentType)) {
                try {
                    String content = MimeMessageUtility.readContent(part, contentType, true);
                    HtmlSanitizeResult sanitizeResult = HtmlProcessing.formatTextForDisplay(content, usm, displayMode, asMarkup, maxContentSize);
                    plainText = new PlainTextContent(id, "text/plain", sanitizeResult.getContent(), sanitizeResult.isTruncated());
                } catch (IOException e) {
                    throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
                }
            }
        }
        return handleAttachment0(part, false, null, baseContentType, fileName, id, false);
    }

    @Override
    public boolean handleSubject(String subject) throws OXException {
        try {
            jsonObject.put(MailJSONField.SUBJECT.getKey(), subject == null ? "" : subject.trim());
            return true;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public boolean handleSystemFlags(int flags) throws OXException {
        try {
            int prevFlags = jsonObject.optInt(MailJSONField.FLAGS.getKey(), 0);
            if (prevFlags > 0) {
                jsonObject.put(MailJSONField.FLAGS.getKey(), prevFlags | flags);
            } else {
                jsonObject.put(MailJSONField.FLAGS.getKey(), flags);
            }
            return true;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public boolean handleToRecipient(InternetAddress[] recipientAddrs) throws OXException {
        try {
            jsonObject.put(MailJSONField.RECIPIENT_TO.getKey(), MessageWriter.getAddressesAsArray(recipientAddrs));
            return true;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public boolean handleUserFlags(String[] userFlags) throws OXException {
        if (userFlags == null) {
            return true;
        }
        try {
            final JSONArray userFlagsArr = new JSONArray(userFlags.length);
            for (String userFlag : userFlags) {
                if (MailMessage.isColorLabel(userFlag)) {
                    jsonObject.put(COLOR_LABEL, MailMessage.getColorLabelIntValue(userFlag));
                } else if (MailMessage.USER_FORWARDED.equalsIgnoreCase(userFlag)) {
                    int flags = jsonObject.optInt(MailJSONField.FLAGS.getKey(), 0);
                    if (flags > 0) {
                        jsonObject.put(MailJSONField.FLAGS.getKey(), flags | MailMessage.FLAG_FORWARDED);
                    } else {
                        jsonObject.put(MailJSONField.FLAGS.getKey(), MailMessage.FLAG_FORWARDED);
                    }
                } else if (MailMessage.USER_READ_ACK.equalsIgnoreCase(userFlag)) {
                    int flags = jsonObject.optInt(MailJSONField.FLAGS.getKey(), 0);
                    if (flags > 0) {
                        jsonObject.put(MailJSONField.FLAGS.getKey(), flags | MailMessage.FLAG_READ_ACK);
                    } else {
                        jsonObject.put(MailJSONField.FLAGS.getKey(), MailMessage.FLAG_READ_ACK);
                    }
                } else {
                    userFlagsArr.put(userFlag);
                }
            }
            if (jsonObject.has(COLOR_LABEL) == false && MailProperties.getInstance().isSupportAppleMailFlags()) {
                int colorLabel = MailMessage.getAppleMailFlag(Arrays.stream(userFlags).collect(Collectors.toSet()));
                if (colorLabel == 0 && ((jsonObject.getInt(MailJSONField.FLAGS.getKey()) & MailMessage.FLAG_FLAGGED) > 0)) {
                    colorLabel = MailMessage.COLOR_LABEL_RED;
                }
                if (colorLabel > 0) {
                    jsonObject.put(COLOR_LABEL, colorLabel);
                }
            }
            jsonObject.put(MailJSONField.USER.getKey(), userFlagsArr);
            return true;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Gets the filled instance of {@link JSONObject}
     *
     * @return The filled instance of {@link JSONObject}
     */
    public JSONObject getJSONObject() {
        if (!jsonObject.has(MailJSONField.MODIFIED.getKey())) {
            try {
                jsonObject.put(MailJSONField.MODIFIED.getKey(), modified[0] ? 1 : 0);
            } catch (JSONException e) {
                /*
                 * Cannot occur
                 */
                LOG.error("", e);
            }
        }
        return jsonObject;
    }

    /**
     * Gets the warnings
     *
     * @return The warnings
     */
    public List<OXException> getWarnings() {
        return warnings;
    }

    private JSONObject asAttachment(String id, String baseContentType, int len, String fileName, HtmlSanitizeResult sanitizeResult) throws OXException {
        try {
            final JSONObject jsonObject = new JSONObject(8);
            jsonObject.put(ID, id);
            jsonObject.put(CONTENT_TYPE, baseContentType);
            jsonObject.put(SIZE, len);
            jsonObject.put(DISPOSITION, Part.ATTACHMENT);
            if ((null != sanitizeResult) && (sanitizeResult.getContent() != null)) {
                jsonObject.put(CONTENT, sanitizeResult.getContent());
                jsonObject.put(TRUNCATED, sanitizeResult.isTruncated());
                jsonObject.put(SANITIZED, true);
            } else {
                jsonObject.put(CONTENT, JSONObject.NULL);
            }
            if (fileName == null) {
                jsonObject.put(ATTACHMENT_FILE_NAME, JSONObject.NULL);
            } else {
                jsonObject.put(ATTACHMENT_FILE_NAME, MimeMessageUtility.decodeMultiEncodedHeader(fileName));
            }
            /*
             * Add token
             */
            addToken(jsonObject, id);
            getAttachmentListing().add(jsonObject);
            return jsonObject;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private void asRawContent(String id, String baseContentType, HtmlSanitizeResult sanitizeResult) throws OXException {
        asRawContent(id, baseContentType, sanitizeResult, false);
    }

    private void asRawContent(String id, String baseContentType, HtmlSanitizeResult sanitizeResult, boolean asFirstAttachment) throws OXException {
        try {
            final JSONObject jsonObject = new JSONObject(6);
            jsonObject.put(ID, id);
            jsonObject.put(CONTENT_TYPE, baseContentType);
            jsonObject.put(SIZE, sanitizeResult.getContent().length());
            jsonObject.put(DISPOSITION, Part.INLINE);
            jsonObject.put(TRUNCATED, sanitizeResult.isTruncated());
            jsonObject.put(SANITIZED, true);
            jsonObject.put(CONTENT, sanitizeResult.getContent());
            final MultipartInfo mpInfo = multiparts.peek();
            jsonObject.put(MULTIPART_ID, null == mpInfo ? JSONObject.NULL : mpInfo.mpId);

            if (asFirstAttachment) {
                getAttachmentListing().addFirst(jsonObject);
            } else {
                getAttachmentListing().add(jsonObject);
            }

        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private JSONObject asDisplayHtml(String id, String baseContentType, String htmlContent, String charset) throws OXException {
        try {
            final JSONObject jsonObject = new JSONObject(6);
            jsonObject.put(ID, id);

            HtmlSanitizeResult sanitizeResult;
            try {
                sanitizeResult = HtmlProcessing.formatHTMLForDisplay(htmlContent, charset, session, mailPath, originalMailPath, usm, modified, displayMode, sanitize, embedded, asMarkup, maxContentSize, mailStructure);
                jsonObject.put(CONTENT_TYPE, baseContentType);
            } catch (OXException e) {
                if (!HTML_PREFIX.equals(e.getPrefix())) {
                    // Re-throw... Not an HTML error
                    throw e;
                }

                warnings.add(e.setCategory(Category.CATEGORY_WARNING).setDisplayMessage(HtmlExceptionMessages.PARSING_FAILED_WITH_FAILOVERMSG, e.getDisplayArgs()));
                if (plainText != null) {
                    sanitizeResult = new HtmlSanitizeResult(plainText.content);
                    jsonObject.put(CONTENT_TYPE, plainText.contentType);
                } else {
                    String text = html2text(htmlContent);
                    sanitizeResult = HtmlProcessing.formatTextForDisplay(text, usm, displayMode, asMarkup, maxContentSize);
                    jsonObject.put(CONTENT_TYPE, MimeTypes.MIME_TEXT_PLAIN);
                }
            }

            String content = sanitizeResult.getContent();
            jsonObject.put(TRUNCATED, sanitizeResult.isTruncated());
            jsonObject.put(SANITIZED, true);
            jsonObject.put(SIZE, content.length());
            jsonObject.put(DISPOSITION, Part.INLINE);
            jsonObject.put(CONTENT, content);
            final MultipartInfo mpInfo = multiparts.peek();
            jsonObject.put(MULTIPART_ID, null == mpInfo ? JSONObject.NULL : mpInfo.mpId);
            getAttachmentListing().add(jsonObject);
            return jsonObject;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private JSONObject asDisplayText(String id, String baseContentType, String htmlContent, String fileName, boolean addAttachment) throws OXException {
        try {
            final JSONObject jsonObject = new JSONObject(6);
            jsonObject.put(ID, id);
            jsonObject.put(CONTENT_TYPE, MimeTypes.MIME_TEXT_PLAIN);
            /*
             * Try to convert the given HTML to regular text
             */
            final String plainText = html2text(htmlContent);
            if (includePlainText) {
                jsonObject.put("plain_text", plainText);
            }
            HtmlSanitizeResult sanitizeResult = HtmlProcessing.formatTextForDisplay(plainText, usm, displayMode, asMarkup, maxContentSize);
            final String content = sanitizeResult.getContent();
            jsonObject.put(TRUNCATED, sanitizeResult.isTruncated());
            jsonObject.put(SANITIZED, true);
            jsonObject.put(DISPOSITION, Part.INLINE);
            jsonObject.put(SIZE, content.length());
            jsonObject.put(CONTENT, content);
            final MultipartInfo mpInfo = multiparts.peek();
            jsonObject.put(MULTIPART_ID, null == mpInfo ? JSONObject.NULL : mpInfo.mpId);
            getAttachmentListing().add(jsonObject);
            if (addAttachment) {
                /*
                 * Create attachment object for original HTML content
                 */
                final JSONObject originalVersion = new JSONObject(6);
                originalVersion.put(ID, id);
                originalVersion.put(CONTENT_TYPE, baseContentType);
                originalVersion.put(DISPOSITION, Part.ATTACHMENT);
                originalVersion.put(SIZE, htmlContent.length());
                originalVersion.put(CONTENT, JSONObject.NULL);
                originalVersion.put(VIRTUAL, true);
                if (fileName == null) {
                    originalVersion.put(ATTACHMENT_FILE_NAME, JSONObject.NULL);
                } else {
                    originalVersion.put(ATTACHMENT_FILE_NAME, MimeMessageUtility.decodeMultiEncodedHeader(fileName));
                }
                getAttachmentListing().add(originalVersion);
            }
            return jsonObject;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private JSONObject asPlainText(String id, String baseContentType, HtmlSanitizeResult sanitizeResult) throws OXException {
        try {
            final JSONObject jsonObject = new JSONObject(6);
            jsonObject.put(ID, id);
            jsonObject.put(DISPOSITION, Part.INLINE);
            jsonObject.put(CONTENT_TYPE, baseContentType);
            jsonObject.put(SIZE, sanitizeResult.getContent().length());
            jsonObject.put(CONTENT, sanitizeResult.getContent());
            jsonObject.put(TRUNCATED, sanitizeResult.isTruncated());
            jsonObject.put(SANITIZED, true);
            getAttachmentListing().add(jsonObject);
            return jsonObject;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private String html2text(String htmlContent) {
        final HtmlService htmlService = ServerServiceRegistry.getInstance().getService(HtmlService.class);
        return null == htmlService ? null : htmlService.html2text(htmlContent, true);
    }

    private boolean hasNoImage(String htmlContent) {
        return null == htmlContent || (com.openexchange.java.Strings.toLowerCase(htmlContent).indexOf("<img ") < 0);
    }

    // --------------------------------------------------------------------------------------------------------------------------

    private static class AttachmentListing {

        private List<JSONObject> attachments;
        private List<JSONObject> remainder;

        /**
         * Initializes a new {@link JsonMessageHandler.AttachmentListing}.
         */
        AttachmentListing() {
            super();
        }

        List<JSONObject> getAttachments() {
            return null == attachments ? Collections.<JSONObject> emptyList() : attachments;
        }

        void add(JSONObject jAttachment) {
            List<JSONObject> attachments = this.attachments;
            if (null == attachments) {
                attachments = new ArrayList<JSONObject>(6);
                this.attachments = attachments;
            }
            attachments.add(jAttachment);
        }

        void addFirst(JSONObject jAttachment) {
            List<JSONObject> attachments = this.attachments;
            if (null == attachments) {
                attachments = new ArrayList<JSONObject>(6);
                this.attachments = attachments;
            }
            attachments.add(0, jAttachment);
        }

        void removeFirst() {
            List<JSONObject> attachments = this.attachments;
            if (null != attachments) {
                attachments.remove(0);
            }
        }

        void addRemainder(JSONObject jAttachment) {
            List<JSONObject> remainder = this.remainder;
            if (null == remainder) {
                remainder = new ArrayList<JSONObject>(4);
                this.remainder = remainder;
            }
            remainder.add(jAttachment);
        }

        JSONArray toJsonArray() {
            if (attachments == null) {
                if (remainder == null) {
                    return JSONArray.EMPTY_ARRAY;
                }
                JSONArray jArray = new JSONArray(remainder.size());
                for (JSONObject jAttachment : remainder) {
                    jArray.put(jAttachment);
                }
                return jArray;
            }
            if (remainder == null) {
                JSONArray jArray = new JSONArray(attachments.size());
                for (JSONObject jAttachment : attachments) {
                    jArray.put(jAttachment);
                }
                return jArray;
            }
            JSONArray jArray = new JSONArray(attachments.size() + remainder.size());
            for (JSONObject jAttachment : attachments) {
                jArray.put(jAttachment);
            }
            for (JSONObject jAttachment : remainder) {
                jArray.put(jAttachment);
            }
            return jArray;
        }
    }

}
