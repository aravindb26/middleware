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

package com.openexchange.mail.json.actions;

import static com.openexchange.mail.mime.utils.MimeMessageUtility.unfold;
import static com.openexchange.mail.utils.DateUtils.getDateRFC822;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.mail.MessageRemovedException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.ContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeConfig;
import org.apache.james.mime4j.stream.MimeConfig.Builder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.fields.DataFields;
import com.openexchange.ajax.fields.FolderChildFields;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestDataTools;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.importexport.MailImportResult;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.java.Reference;
import com.openexchange.java.Streams;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.json.MailRequest;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.MimeDefaultSession;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.mime.converters.DefaultConverterConfig;
import com.openexchange.mail.mime.converters.FileBackedMimeMessage;
import com.openexchange.mail.mime.converters.MimeMessageConverter;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.server.ServiceLookup;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.threadpool.AbstractTrackableTask;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.ThreadRenamer;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link ImportAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@RestrictedAction(module = AbstractMailAction.MODULE, type = RestrictedAction.Type.WRITE)
public final class ImportAction extends AbstractMailAction {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ImportAction.class);

    /**
     * Initializes a new {@link ImportAction}.
     *
     * @param services
     */
    public ImportAction(ServiceLookup services) {
        super(services);
    }

    private static final String PLAIN_JSON = "plainJson";

    @Override
    protected AJAXRequestResult perform(MailRequest mailRequest) throws OXException {
        AJAXRequestData request = mailRequest.getRequest();
        List<OXException> warnings = new ArrayList<OXException>();
        try {
            String folder = mailRequest.checkParameter(AJAXServlet.PARAMETER_FOLDERID);
            int flags;
            {
                String tmp = mailRequest.getParameter("flags");
                if (null == tmp) {
                    flags = 0;
                } else {
                    try {
                        flags = Integer.parseInt(tmp.trim());
                    } catch (NumberFormatException e) {
                        flags = 0;
                    }
                }
            }
            boolean force;
            {
                String tmp = mailRequest.getParameter("force");
                force = null == tmp ? false : AJAXRequestDataTools.parseBoolParameter(tmp.trim());
            }
            boolean preserveReceivedDate = mailRequest.optBool("preserveReceivedDate", false);
            boolean strictParsing = mailRequest.optBool("strictParsing", true);
            /*
             * Iterate upload files
             */
            ServerSession session = mailRequest.getSession();
            UserSettingMail usm = session.getUserSettingMail();
            QuotedInternetAddress defaultSendAddr = new QuotedInternetAddress(usm.getSendAddr(), false);
            MailServletInterface mailInterface = MailServletInterface.getInstance(session);
            BlockingQueue<MimeMessage> queue = new ArrayBlockingQueue<MimeMessage>(100);
            Future<Object> future = null;
            {
                ThreadPoolService service = ServerServiceRegistry.getInstance().getService(ThreadPoolService.class, true);
                AppenderTask task = new AppenderTask(mailInterface, folder, force, flags, queue);
                try {
                    // Initialize iterator
                    Iterator<UploadFile> iter;
                    {
                        long maxFileSize = usm.getUploadQuotaPerFile();
                        if (maxFileSize <= 0) {
                            maxFileSize = -1L;
                        }
                        long maxSize = usm.getUploadQuota();
                        if (maxSize <= 0) {
                            maxSize = -1L;
                        }
                        iter = request.getFiles(maxFileSize, maxSize).iterator();
                    }

                    // Iterate uploaded messages
                    boolean keepgoing = true;
                    if (keepgoing && iter.hasNext()) {
                        future = service.submit(task);
                        do {
                            UploadFile uploadFile = iter.next();
                            File tmpFile = uploadFile.getTmpFile();
                            boolean first = true;
                            if (null != tmpFile) {
                                try {
                                    // Validate content
                                    if (strictParsing) {
                                        validateRfc822Message(tmpFile);
                                    }
                                    first = false;

                                    // Parse & add to queue
                                    MimeMessage message = newMimeMessagePreservingReceivedDate(tmpFile, preserveReceivedDate);
                                    message.removeHeader("x-original-headers");
                                    String fromAddr = message.getHeader(MessageHeaders.HDR_FROM, null);
                                    if (isEmpty(fromAddr)) {
                                        // Add from address
                                        message.setFrom(defaultSendAddr);
                                    }
                                    while (keepgoing && !queue.offer(message, 1, TimeUnit.SECONDS)) {
                                        keepgoing = !future.isDone();
                                    }
                                } catch (OXException e) {
                                    if (first && !iter.hasNext()) {
                                        throw e;
                                    }
                                    // Otherwise add to warnings
                                    warnings.add(e);
                                }
                            }
                        } while (keepgoing && iter.hasNext());
                    }
                } finally {
                    task.stop();
                }
            }

            MailImportResult[] mirs;
            if (null == future) {
                mirs = new MailImportResult[0];
            } else {
                /*
                 * Ensure release from BlockingQueue.take();
                 */
                try {
                    future.get();
                } catch (ExecutionException e) {
                    Throwable t = e.getCause();
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    if (t instanceof Error) {
                        throw (Error) t;
                    }
                    if (t instanceof OXException) {
                        throw (OXException) t;
                    }
                    throw new IllegalStateException("Not unchecked", t);
                }
                MailImportResult[] alreadyImportedOnes = mailInterface.getMailImportResults();
                /*
                 * Still some in queue?
                 */
                if (queue.isEmpty()) {
                    mirs = alreadyImportedOnes;
                } else {
                    List<MimeMessage> messages = new ArrayList<MimeMessage>(16);
                    queue.drainTo(messages);
                    messages.remove(POISON);
                    List<MailMessage> mails = new ArrayList<MailMessage>(messages.size());
                    for (MimeMessage message : messages) {
                        message.getHeader("Date", null);
                        MailMessage mm = MimeMessageConverter.convertMessage(message);
                        mails.add(mm);
                    }
                    messages.clear();
                    mailInterface = getMailInterface(mailRequest);
                    {
                        String[] ids = mailInterface.importMessages(folder, mails.toArray(new MailMessage[mails.size()]), force);
                        mails.clear();
                        if (flags > 0) {
                            mailInterface.updateMessageFlags(folder, ids, flags, true);
                        }
                    }
                    MailImportResult[] byCaller = mailInterface.getMailImportResults();
                    warnings.addAll(mailInterface.getWarnings());
                    mirs = new MailImportResult[alreadyImportedOnes.length + byCaller.length];
                    System.arraycopy(alreadyImportedOnes, 0, mirs, 0, alreadyImportedOnes.length);
                    System.arraycopy(byCaller, 0, mirs, alreadyImportedOnes.length, byCaller.length);
                }
            }

            JSONArray respArray = new JSONArray();
            for (MailImportResult m : mirs) {
                if (m.hasError()) {
                    JSONObject responseObj = new JSONObject();
                    responseObj.put(FolderChildFields.FOLDER_ID, folder);
                    responseObj.put(MailImportResult.FILENAME, m.getMail().getFileName());
                    responseObj.put(MailImportResult.ERROR, m.getException().getMessage());
                    respArray.put(responseObj);
                } else {
                    JSONObject responseObj = new JSONObject();
                    responseObj.put(FolderChildFields.FOLDER_ID, folder);
                    responseObj.put(DataFields.ID, m.getId());
                    respArray.put(responseObj);
                }
            }

            // Create response object
            AJAXRequestResult result = new AJAXRequestResult(respArray, "json");
            result.setParameter(PLAIN_JSON, Boolean.TRUE);
            result.addWarnings(warnings);
            return result;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        } catch (IOException e) {
            if ((e.getCause() instanceof com.sun.mail.util.MessageRemovedIOException) || (e.getCause() instanceof MessageRemovedException)) {
                throw MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.create(e);
            }
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        } catch (MessageRemovedException e) {
            throw MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.create(e);
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw MailExceptionCode.INTERRUPT_ERROR.create(e);
        } catch (RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    private static void validateRfc822Message(File rfc822File) throws IOException, OXException {
        MimeConfig config = new Builder()
            .setStrictParsing(true)
            .setMaxLineLen(-1)
            .setMaxHeaderLen(-1)
            .setMaxHeaderCount(250)
            .build();

        MimeStreamParser parser = new MimeStreamParser(config);
        parser.setContentHandler(DO_NOTHING_HANDLER);

        InputStream in = new FileInputStream(rfc822File);
        try {
            parser.parse(in);
        } catch (MimeException e) {
            throw MailExceptionCode.INVALID_MESSAGE.create(e, e.getMessage());
        } finally {
            Streams.close(in);
        }
    }

    /**
     * The poison element to quit message import immediately.
     */
    private static final MimeMessage POISON = new MimeMessage(MimeDefaultSession.getDefaultSession());

    private static final class AppenderTask extends AbstractTrackableTask<Object> {

        private final AtomicBoolean keepgoing;
        private final MailServletInterface mailInterface;
        private final String folder;
        private final boolean force;
        private final int flags;
        private final BlockingQueue<MimeMessage> queue;

        protected AppenderTask(MailServletInterface mailInterface, String folder, boolean force, int flags, BlockingQueue<MimeMessage> queue) {
            super();
            keepgoing = new AtomicBoolean(true);
            this.mailInterface = mailInterface;
            this.folder = folder;
            this.force = force;
            this.flags = flags;
            this.queue = queue;
        }

        protected void stop() throws OXException {
            keepgoing.set(false);
            /*
             * Feed poison element to enforce quit
             */
            try {
                queue.put(POISON);
            } catch (InterruptedException e) {
                /*
                 * Cannot occur, but keep interrupted state
                 */
                Thread.currentThread().interrupt();
                throw MailExceptionCode.INTERRUPT_ERROR.create(e);
            }
        }

        @Override
        public Object call() throws Exception {
            List<String> idList = new ArrayList<String>();
            try {
                List<MimeMessage> messages = new ArrayList<MimeMessage>(16);
                List<MailMessage> mails = new ArrayList<MailMessage>(16);
                while (keepgoing.get() || !queue.isEmpty()) {
                    messages.clear();
                    mails.clear();
                    if (queue.isEmpty()) {
                        // Blocking wait for at least 1 message to arrive.
                        MimeMessage msg = queue.take();
                        if (POISON == msg) {
                            return null;
                        }
                        messages.add(msg);
                    }
                    queue.drainTo(messages);
                    boolean quit = messages.remove(POISON);
                    for (MimeMessage message : messages) {
                        message.getHeader("Date", null);
                        MailMessage mm = MimeMessageConverter.convertMessage(message, new DefaultConverterConfig(mailInterface.getMailConfig(), true, false));
                        mails.add(mm);
                    }
                    String[] ids = mailInterface.importMessages(folder, mails.toArray(new MailMessage[mails.size()]), force);
                    idList.clear();
                    idList.addAll(Arrays.asList(ids));
                    if (flags > 0) {
                        mailInterface.updateMessageFlags(folder, ids, flags, true);
                    }
                    if (quit) {
                        return null;
                    }
                }
            } catch (OXException e) {
                throw e;
            } catch (MessagingException e) {
                throw MimeMailException.handleMessagingException(e);
            } catch (InterruptedException e) {
                // Keep interrupted status
                Thread.currentThread().interrupt();
                throw MailExceptionCode.INTERRUPT_ERROR.create(e);
            } finally {
                mailInterface.close(true);
            }
            return null;
        }

        @Override
        public void setThreadName(ThreadRenamer threadRenamer) {
            threadRenamer.rename("Mail Import Thread");
        }
    }

    private static MimeMessage newMimeMessagePreservingReceivedDate(File tempFile, boolean preserveReceivedDate) throws MessagingException, IOException {
        MimeMessage tmp;
        if (preserveReceivedDate) {
            tmp = new FileBackedMimeMessage(MimeDefaultSession.getDefaultSession(), tempFile, null) {

                private Reference<Date> receivedDateReference;

                @Override
                public Date getReceivedDate() throws MessagingException {
                    if (receivedDateReference == null) {
                        receivedDateReference = new Reference<Date>(null);
                        String[] receivedHdrs = getHeader(MessageHeaders.HDR_RECEIVED);
                        if (null != receivedHdrs) {
                            List<String> nonNullHdrs = Arrays.stream(receivedHdrs).filter(Objects::nonNull).collect(Collectors.toList());
                            Long lastReceived = null;
                            for (String receivedHdr : nonNullHdrs) {
                                String hdr = unfold(receivedHdr);
                                int pos = hdr.lastIndexOf(';');
                                if (pos >= 0) {
                                    String dateString = hdr.substring(pos + 1).trim();
                                    try {
                                        long parsedTime = getDateRFC822(dateString).getTime();
                                        lastReceived = Long.valueOf(lastReceived == null ? parsedTime : Math.max(lastReceived.longValue(), parsedTime));
                                    } catch (Exception e) {
                                        LOG.warn("Failed to parse date string from \"Received\" header: {}", dateString, e);
                                    }
                                }
                            }

                            if (lastReceived != null) {
                                receivedDateReference.setValue(new Date(lastReceived.longValue()));
                            }
                        }
                    }

                    return receivedDateReference.getValue();
                }
            };
        } else {
            tmp = new FileBackedMimeMessage(MimeDefaultSession.getDefaultSession(), tempFile, null);
        }
        return tmp;
    }

    private static final ContentHandler DO_NOTHING_HANDLER = new ContentHandler() {

        @Override
        public void startMessage() throws MimeException {
            // Nothing
        }

        @Override
        public void endMessage() throws MimeException {
            // Nothing
        }

        @Override
        public void startBodyPart() throws MimeException {
            // Nothing
        }

        @Override
        public void endBodyPart() throws MimeException {
            // Nothing
        }

        @Override
        public void startHeader() throws MimeException {
            // Nothing
        }

        @Override
        public void field(Field rawField) throws MimeException {
            // Nothing
        }

        @Override
        public void endHeader() throws MimeException {
            // Nothing
        }

        @Override
        public void preamble(InputStream is) throws MimeException, IOException {
            // Nothing
        }

        @Override
        public void epilogue(InputStream is) throws MimeException, IOException {
            // Nothing
        }

        @Override
        public void startMultipart(BodyDescriptor bd) throws MimeException {
            // Nothing
        }

        @Override
        public void endMultipart() throws MimeException {
            // Nothing
        }

        @Override
        public void body(BodyDescriptor bd, InputStream is) throws MimeException, IOException {
            // Nothing
        }

        @Override
        public void raw(InputStream is) throws MimeException, IOException {
            // Nothing
        }
    };

}
