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

package com.openexchange.templating.impl.osgi;

import com.openexchange.config.ConfigurationService;
import com.openexchange.groupware.infostore.InfostoreFacade;
import com.openexchange.html.HtmlService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.templating.TemplateService;
import com.openexchange.templating.impl.OXTemplateImpl;
import com.openexchange.templating.impl.TemplateServiceImpl;
import com.openexchange.tools.strings.StringParser;

/**
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 */
public class TemplatingActivator extends HousekeepingActivator {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TemplatingActivator.class);

    @Override
    public void startBundle() throws Exception {
        ConfigurationService config = getService(ConfigurationService.class);
        final TemplateServiceImpl templates = new TemplateServiceImpl(config);
        registerService(TemplateService.class, templates);

        OXTemplateImpl.services = this;

        final boolean hasProperty = config.getProperty(TemplateServiceImpl.PATH_PROPERTY) != null;
        if (!hasProperty) {
            final IllegalStateException exception = new IllegalStateException("Missing Property " + TemplateServiceImpl.PATH_PROPERTY);
            exception.fillInStackTrace();

            LOG.error("{} is not set. Templating will remain inactive.", TemplateServiceImpl.PATH_PROPERTY, exception);
        }
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { ConfigurationService.class, InfostoreFacade.class, StringParser.class, HtmlService.class };
    }

}
