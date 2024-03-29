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

package com.openexchange.chronos.scheduling.changes.impl;

import java.util.Locale;
import com.openexchange.chronos.scheduling.changes.ContextSensitiveMessages;
import com.openexchange.chronos.scheduling.changes.Sentence;
import com.openexchange.chronos.scheduling.changes.SentenceFactory;
import com.openexchange.chronos.scheduling.changes.impl.osgi.Services;
import com.openexchange.i18n.I18nServiceRegistry;

/**
 * 
 * {@link SentenceFactoryImpl} - Used to prepare the {@link I18nServiceRegistry} over the {@link Services} class
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class SentenceFactoryImpl implements SentenceFactory {

    /**
     * Initializes a new {@link SentenceFactoryImpl}.
     *
     */
    public SentenceFactoryImpl() {
        super();
    }

    @Override
    public Sentence create(String message) {
        return new SentenceImpl(message);
    }

    @Override
    public ContextSensitiveMessages create(Locale locale) {
        return new ContextSensitiveMessagesImpl(locale);
    }

}
