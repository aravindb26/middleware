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

package com.openexchange.client.onboarding.plist;

import static com.openexchange.java.Autoboxing.I;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.client.onboarding.BuiltInProvider;
import com.openexchange.client.onboarding.CommonInput;
import com.openexchange.client.onboarding.OnboardingAction;
import com.openexchange.client.onboarding.OnboardingExceptionCodes;
import com.openexchange.client.onboarding.OnboardingProvider;
import com.openexchange.client.onboarding.OnboardingRequest;
import com.openexchange.client.onboarding.OnboardingSMSConstants;
import com.openexchange.client.onboarding.OnboardingStrings;
import com.openexchange.client.onboarding.OnboardingUtility;
import com.openexchange.client.onboarding.Result;
import com.openexchange.client.onboarding.ResultObject;
import com.openexchange.client.onboarding.ResultReply;
import com.openexchange.client.onboarding.SimpleResultObject;
import com.openexchange.client.onboarding.download.DownloadLinkProvider;
import com.openexchange.client.onboarding.download.DownloadOnboardingStrings;
import com.openexchange.client.onboarding.notification.mail.OnboardingProfileCreatedNotificationMail;
import com.openexchange.client.onboarding.plist.osgi.Services;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.dataobjects.compose.ComposeType;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.transport.MailTransport;
import com.openexchange.mail.transport.TransportProvider;
import com.openexchange.mail.transport.TransportProviderRegistry;
import com.openexchange.notification.mail.MailAttachments;
import com.openexchange.notification.mail.MailData;
import com.openexchange.notification.mail.NotificationMailFactory;
import com.openexchange.plist.PListDict;
import com.openexchange.plist.PListWriter;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.session.Session;
import com.openexchange.sms.SMSServiceSPI;
import com.openexchange.sms.tools.SMSBucketExceptionCodes;
import com.openexchange.sms.tools.SMSBucketService;

/**
 * {@link PlistResult} - A plist result.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.1
 */
public class PlistResult implements Result {

    private final PListDict pListDict;
    private final ResultReply reply;

    private static final String SMS_KEY = "sms";

    /**
     * Initializes a new {@link PlistResult}.
     *
     * @param pListDict The plist object
     * @param reply The reply
     */
    public PlistResult(PListDict pListDict, ResultReply reply) {
        super();
        this.pListDict = pListDict;
        this.reply = reply;
    }

    /**
     * Gets the plist object
     *
     * @return The plist object
     */
    public PListDict getPListDict() {
        return pListDict;
    }

    @Override
    public ResultObject getResultObject(OnboardingRequest request, Session session) throws OXException {
        OnboardingAction action = request.getAction();
        switch (action) {
            case DOWNLOAD:
                return generatePListResult(request, session);
            case EMAIL:
                return sendEmailResult(request, session);
            case SMS:
                return generateSMSResult(request, session);
            default:
                throw OnboardingExceptionCodes.UNSUPPORTED_ACTION.create(action.getId());
        }
    }

    @Override
    public ResultReply getReply() {
        return reply;
    }

    // --------------------------------------------- E-Mail utils --------------------------------------------------------------

    private static TransportProvider getTransportProvider() {
        return TransportProviderRegistry.getTransportProvider("smtp");
    }

    @SuppressWarnings("resource")
    private ResultObject sendEmailResult(OnboardingRequest request, Session session) throws OXException {
        Map<String, Object> input = request.getInput();
        if (null == input) {
            throw OnboardingExceptionCodes.MISSING_INPUT_FIELD.create(CommonInput.EMAIL_ADDRESS.getFirstElementName());
        }

        String emailAddress = (String) input.get(CommonInput.EMAIL_ADDRESS.getFirstElementName());
        if (Strings.isEmpty(emailAddress)) {
            throw OnboardingExceptionCodes.MISSING_INPUT_FIELD.create(CommonInput.EMAIL_ADDRESS.getFirstElementName());
        }

        ThresholdFileHolder fileHolder = null;
        boolean error = true;
        MailTransport transport = getTransportProvider().createNewNoReplyTransport(session.getContextId());
        try {
            NotificationMailFactory notify = Services.getService(NotificationMailFactory.class);

            String name = request.getScenario().getId() + ".mobileconfig";
            MailData data = OnboardingProfileCreatedNotificationMail.createProfileNotificationMail(emailAddress, request.getHostData().getHost(), name, session);

            fileHolder = new ThresholdFileHolder();
            fileHolder.setDisposition("attachment; filename=" + name);
            fileHolder.setName(name);
            fileHolder.setContentType("application/x-apple-aspen-config; charset=UTF-8; name=" + name);// Or application/x-plist ?
            new PListWriter().write(pListDict, fileHolder.asOutputStream());

            fileHolder = sign(fileHolder, session);

            ComposedMailMessage message = notify.createMail(data, Collections.singleton(MailAttachments.newMailAttachment(fileHolder)));
            transport.sendMailMessage(message, ComposeType.NEW);

            ResultObject resultObject = new SimpleResultObject(OnboardingUtility.getTranslationFor(OnboardingStrings.RESULT_MAIL_SENT, session), "string");
            error = false;
            return resultObject;
        } catch (IOException e) {
            throw OnboardingExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } finally {
            transport.close();
            if (error) {
                Streams.close(fileHolder);
            }
        }
    }

    // --------------------------------------------- PLIST utils --------------------------------------------------------------

    @SuppressWarnings("resource")
    private ResultObject generatePListResult(OnboardingRequest request, Session session) throws OXException {
        ThresholdFileHolder fileHolder = null;
        boolean error = true;
        try {
            fileHolder = new ThresholdFileHolder();
            fileHolder.setDisposition("attachment");
            fileHolder.setName(request.getScenario().getId() + ".mobileconfig");
            fileHolder.setContentType("application/x-apple-aspen-config");// Or application/x-plist ?
            fileHolder.setDelivery("download");
            new PListWriter().write(pListDict, fileHolder.asOutputStream());

            // Sign it
            fileHolder = sign(fileHolder, session);

            ResultObject resultObject = new SimpleResultObject(fileHolder, "file");
            error = false;
            return resultObject;
        } catch (IOException e) {
            throw OnboardingExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } finally {
            if (error) {
                Streams.close(fileHolder);
            }
        }
    }

    private static ThresholdFileHolder sign(ThresholdFileHolder fileHolder, Session session) throws OXException, IOException {
        ThresholdFileHolder tfh = null;
        boolean error = true;
        try {
            PListSigner signer = Services.getService(PListSigner.class);
            IFileHolder signed = signer.signPList(fileHolder, session);

            if (signed instanceof ThresholdFileHolder) {
                error = false;
                return ThresholdFileHolder.class.cast(signed);
            }

            tfh = new ThresholdFileHolder(signed);
            signed.close();
            error = false;
            return tfh;
        } finally {
            if (error) {
                Streams.close(tfh, fileHolder);
            }
        }
    }

    // --------------------------------------------- SMS utils --------------------------------------------------------------

    private static ResultObject generateSMSResult(OnboardingRequest request, Session session) throws OXException {
        SMSServiceSPI smsService = Services.optService(SMSServiceSPI.class);
        if (smsService == null) {
            throw ServiceExceptionCode.absentService(SMSServiceSPI.class);
        }

        SMSBucketService smsBucketService = Services.optService(SMSBucketService.class);
        if (smsBucketService == null) {
            throw ServiceExceptionCode.absentService(SMSBucketService.class);
        }

        long ratelimit = getSMSRateLimit(session);
        int smsRemaining = checkSMSLimit(session, ratelimit, smsBucketService);
        String untranslatedText;
        {
            List<OnboardingProvider> providers = request.getScenario().getProviders(session);
            if (seemsLikeMail(providers)) {
                untranslatedText = DownloadOnboardingStrings.MAIL_MESSAGE;
            } else if (seemsLikeDav(providers)) {
                untranslatedText = DownloadOnboardingStrings.DAV_MESSAGE;
            } else if (seemsLikeEas(providers)) {
                untranslatedText = DownloadOnboardingStrings.EAS_MESSAGE;
            } else {
                untranslatedText = DownloadOnboardingStrings.DEFAULT_MESSAGE;
            }
        }

        String text = OnboardingUtility.getTranslationFor(untranslatedText, session);
        //get url
        DownloadLinkProvider smsLinkProvider = Services.getService(DownloadLinkProvider.class);
        String link = smsLinkProvider.getLink(request.getHostData(), session.getUserId(), session.getContextId(), request.getScenario().getId(), request.getDevice().getId());
        text = text + link;

        Map<String, Object> input = request.getInput();
        if (input == null) {
            throw OnboardingExceptionCodes.MISSING_INPUT_FIELD.create(SMS_KEY);
        }
        String number = (String) input.get(SMS_KEY);
        if (number == null) {
            throw OnboardingExceptionCodes.MISSING_INPUT_FIELD.create(SMS_KEY);
        }

        number = sanitizeNumber(number);
        setRateLimitTime(ratelimit, session);
        smsService.sendMessage(new String[] { number }, text, session.getUserId(), session.getContextId());

        ResultObject resultObject;
        resultObject = new SimpleResultObject(OnboardingUtility.getTranslationFor(OnboardingStrings.RESULT_SMS_SENT, session), "string");
        if (smsRemaining == 2) {
            int hours = smsBucketService.getRefreshInterval(session);
            resultObject.addWarning(SMSBucketExceptionCodes.NEXT_TO_LAST_SMS_SENT.create(I(hours)));
        }

        return resultObject;
    }

    /**
     * Removes all non digits from the number string except the leading '+'
     *
     * @param number Expect a internationalized phone number
     * @return The sanitized phone number
     * @throws OXException if the number does not start with a '+' sign
     */
    private static String sanitizeNumber(String number) throws OXException {
        if (!number.startsWith("+")) {
            throw OnboardingExceptionCodes.INVALID_PHONE_NUMBER.create(number);
        }

        String num = number;
        num = num.substring(1);
        num = num.replaceAll("[^0-9]", "");

        return "+" + num;
    }

    private static boolean seemsLikeDav(List<OnboardingProvider> providers) {
        if (null == providers) {
            return false;
        }

        for (OnboardingProvider onboardingProvider : providers) {
            String id = onboardingProvider.getId();
            if (BuiltInProvider.CALDAV.getId().equals(id) || BuiltInProvider.CARDDAV.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static boolean seemsLikeMail(List<OnboardingProvider> providers) {
        if (null == providers) {
            return false;
        }

        for (OnboardingProvider onboardingProvider : providers) {
            String id = onboardingProvider.getId();
            if (BuiltInProvider.MAIL.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static boolean seemsLikeEas(List<OnboardingProvider> providers) {
        if (null == providers) {
            return false;
        }

        for (OnboardingProvider onboardingProvider : providers) {
            String id = onboardingProvider.getId();
            if (BuiltInProvider.EAS.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static long getSMSRateLimit(Session session) throws OXException {
        ConfigViewFactory confFactory = Services.getService(ConfigViewFactory.class);
        ConfigView view = confFactory.getView(session.getUserId(), session.getContextId());

        ComposedConfigProperty<Long> property = view.property(OnboardingSMSConstants.SMS_RATE_LIMIT_PROPERTY, Long.class);
        if (null == property) {
            return -1L;
        }

        Long value = property.get();
        return value == null ? -1L : value.longValue();
    }

    private static int checkSMSLimit(Session session, long ratelimit, SMSBucketService smsBucketService) throws OXException {
        if (ratelimit > 0) {
            Long lastSMSSend = (Long) session.getParameter(OnboardingSMSConstants.SMS_LAST_SEND_TIMESTAMP);

            if ((lastSMSSend != null) && ((lastSMSSend.longValue() + ratelimit) > System.currentTimeMillis())) {
                throw OnboardingExceptionCodes.SENT_QUOTA_EXCEEDED.create(Long.valueOf(ratelimit / 1000));
            }
        }

        int remainingSMS = -1;
        if (smsBucketService.isEnabled(session)) {
            remainingSMS = smsBucketService.getSMSToken(session);
        }
        return remainingSMS;
    }

    private static void setRateLimitTime(long rateLimit, Session session) {
        if (rateLimit > 0) {
            session.setParameter(OnboardingSMSConstants.SMS_LAST_SEND_TIMESTAMP, Long.valueOf(System.currentTimeMillis()));
        }
    }

}
