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

package com.openexchange.mail.exportpdf.impl;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.mail.exportpdf.MailExportExceptionCode.TOO_MANY_CONCURRENT_EXPORTS;
import java.util.concurrent.atomic.AtomicInteger;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.DefaultInterests;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.mail.exportpdf.MailExportOptions;
import com.openexchange.mail.exportpdf.MailExportProperty;
import com.openexchange.mail.exportpdf.MailExportResult;
import com.openexchange.mail.exportpdf.MailExportService;
import com.openexchange.session.Session;

/**
 * {@link ThrottlingMailExportService}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ThrottlingMailExportService implements MailExportService, Reloadable {

    private final MailExportService delegate;
    private final AtomicInteger currentExportOperations;
    private int maximumConcurrentExports;

    /**
     * Initializes a new {@link ThrottlingMailExportService}.
     *
     * @param leanConfigurationService The lean configuration service
     * @param delegate The mail export service delegate
     */
    public ThrottlingMailExportService(LeanConfigurationService leanConfigurationService, MailExportService delegate) {
        super();
        this.delegate = delegate;
        this.currentExportOperations = new AtomicInteger();
        this.maximumConcurrentExports = leanConfigurationService.getIntProperty(MailExportProperty.concurrentExports);
    }

    @Override
    public MailExportResult exportMail(Session session, MailExportOptions options) throws OXException {
        boolean incremented = false;
        try {
            if (maximumConcurrentExports == 0) {
                return delegate.exportMail(session, options);
            }
            int currentExportOps = currentExportOperations.incrementAndGet();
            incremented = true;
            if (currentExportOps > maximumConcurrentExports) {
                throw TOO_MANY_CONCURRENT_EXPORTS.create(I(maximumConcurrentExports), MailExportProperty.concurrentExports.getFQPropertyName());
            }
            return delegate.exportMail(session, options);
        } finally {
            if (incremented) {
                currentExportOperations.decrementAndGet();
            }
        }
    }

    @Override
    public Interests getInterests() {
        return DefaultInterests.builder().propertiesOfInterest(MailExportProperty.concurrentExports.getFQPropertyName()).build();
    }

    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        maximumConcurrentExports = configService.getIntProperty(MailExportProperty.concurrentExports.getFQPropertyName(), i(MailExportProperty.concurrentExports.getDefaultValue(Integer.class)));
    }
}
