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

package com.openexchange.mail.exportpdf.converters.header;

import java.text.DateFormat;
import java.util.Optional;
import java.util.TimeZone;
import com.openexchange.regional.RegionalSettings;
import com.openexchange.regional.RegionalSettingsService;
import com.openexchange.regional.RegionalSettingsUtil;

/**
 * {@link MailHeader} - Defines various mail headers for the mail export
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public enum MailHeader {

    SUBJECT("Subject", MailHeaderStrings.SUBJECT),
    DATE("Date", MailHeaderStrings.DATE, (session, services, contentHolder) -> {
        if (contentHolder.getSentDate() == null) {
            return null;
        }
        RegionalSettingsService regionalSettingsService = services.getOptionalService(RegionalSettingsService.class);
        RegionalSettings regionalSettings = null != regionalSettingsService ? regionalSettingsService.get(session.getContextId(), session.getUserId()) : null;
        DateFormat dateFormat = RegionalSettingsUtil.getDateTimeFormat(regionalSettings, session.getUser().getLocale(), DateFormat.MEDIUM, DateFormat.MEDIUM);
        dateFormat.setTimeZone(TimeZone.getTimeZone(session.getUser().getTimeZone()));
        return dateFormat.format(contentHolder.getSentDate());
    }),
    FROM("From", MailHeaderStrings.FROM),
    SENDER("Sender", MailHeaderStrings.VIA),
    TO("To", MailHeaderStrings.TO),
    CC("Cc", MailHeaderStrings.CC),
    BCC("Bcc", MailHeaderStrings.BCC);

    private final String mailHeaderName;
    private final String displayName;
    private final HeaderValueFormatter formatter;

    /**
     * Initialises a new {@link MailHeader}
     *
     * @param mailHeaderName The mail header name
     * @param displayName The mail header display name
     */
    MailHeader(String mailHeaderName, String displayName) {
        this(mailHeaderName, displayName, null);
    }

    /**
     * Initialises a new {@link MailHeader}
     *
     * @param mailHeaderName The mail header name
     * @param displayName The mail header display name
     * @param formatter The header value formatter
     */
    MailHeader(String mailHeaderName, String displayName, HeaderValueFormatter formatter) {
        this.mailHeaderName = mailHeaderName;
        this.displayName = displayName;
        this.formatter = formatter;
    }

    /**
     * Returns the mail header name
     *
     * @return the mail header name
     */
    public String getMailHeaderName() {
        return mailHeaderName;
    }

    /**
     * Returns the mail header display name
     *
     * @return the mail header display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * The optional formatter
     *
     * @return the optional formatter
     */
    public Optional<HeaderValueFormatter> getFormatter() {
        return Optional.ofNullable(formatter);
    }
}
