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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.Transp;
import com.openexchange.chronos.compat.ShownAsTransparency;
import com.openexchange.chronos.scheduling.changes.ContextSensitiveMessages;
import com.openexchange.chronos.scheduling.changes.ContextSensitiveMessages.Context;
import com.openexchange.chronos.scheduling.changes.MessageContext;
import com.openexchange.chronos.scheduling.changes.Sentence;
import com.openexchange.chronos.scheduling.common.description.DefaultMessageContext;
import com.openexchange.chronos.scheduling.common.description.FormattableArgument;
import com.openexchange.chronos.scheduling.common.description.TypeWrapper;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.regional.RegionalSettings;

/**
 * {@link SentenceImpl}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a> - Moved with v7.10.3
 */
public class SentenceImpl implements Sentence {

    private final static Logger LOGGER = LoggerFactory.getLogger(SentenceImpl.class);

    private final String message;
    private final List<Object> arguments = new ArrayList<Object>();
    private final List<ArgumentType> types = new ArrayList<ArgumentType>();
    private final List<Object[]> extra = new ArrayList<Object[]>();

    /**
     * Initializes a new {@link SentenceImpl}.
     *
     * @param message The message to translate
     */
    public SentenceImpl(String message) {
        this.message = message;
    }

    @Override
    public SentenceImpl add(Object argument) {
        return add(argument, ArgumentType.NONE);
    }

    @Override
    public SentenceImpl addStatus(ParticipationStatus status) {
        return add("", ArgumentType.STATUS, status);
    }

    @Override
    public SentenceImpl add(Object argument, ArgumentType type, Object... extra) {
        arguments.add(argument);
        types.add(type);
        this.extra.add(extra);
        return this;
    }

    @Override
    public String getMessage(String format, Locale locale, TimeZone timeZone, RegionalSettings regionalSettings) {
        return getMessage(new DefaultMessageContext(format, locale, timeZone, regionalSettings));
    }

    @Override
    public String getMessage(MessageContext context) {
        TypeWrapper wrapper = getWrapper(context.getFormat());
        List<String> wrapped = new ArrayList<String>(arguments.size());
        StringHelper sh = StringHelper.valueOf(context.getLocale());

        for (int i = 0, size = arguments.size(); i < size; i++) {
            Object argument = arguments.get(i);
            if ((argument instanceof FormattableArgument)) {
                argument = ((FormattableArgument) argument).format(context);
            }
            ArgumentType type = types.get(i);
            Object[] extraInfo = extra.get(i);

            switch (type) {
                case NONE:
                    wrapped.add(wrapper.none(argument));
                    break;
                case ORIGINAL:
                    wrapped.add(wrapper.original(argument));
                    break;
                case UPDATED:
                    wrapped.add(wrapper.updated(argument));
                    break;
                case PARTICIPANT:
                    wrapped.add(wrapper.participant(argument));
                    break;
                case STATUS:
                    ParticipationStatus status = (ParticipationStatus) extraInfo[0];
                    ContextSensitiveMessages contextSensitiveMessages = new ContextSensitiveMessagesImpl(context.getLocale());
                    if (ParticipationStatus.ACCEPTED.matches(status)) {
                        argument = contextSensitiveMessages.accepted(Context.VERB);
                    } else if (ParticipationStatus.DECLINED.matches(status)) {
                        argument = contextSensitiveMessages.declined(Context.VERB);
                    } else if (ParticipationStatus.TENTATIVE.matches(status)) {
                        argument = contextSensitiveMessages.tentative(Context.VERB);
                    } else {
                        argument = sh.getString((String) argument);
                    }
                    wrapped.add(wrapper.state(argument, (ParticipationStatus) extraInfo[0]));
                    break;
                case EMPHASIZED:
                    wrapped.add(wrapper.emphasiszed(argument));
                    break;
                case ITALIC:
                    wrapped.add(wrapper.italic(argument));
                    break;
                case REFERENCE:
                    wrapped.add(wrapper.reference(argument));
                    break;
                case SHOWN_AS:
                    if (argument instanceof String) {
                        String str = (String) argument;
                        if (str.trim().length() != 0) {
                            argument = sh.getString(str);
                        }
                    }
                    if (null != extraInfo && 0 < extraInfo.length && null != extraInfo[0]) {
                        if ((extraInfo[0] instanceof ShownAsTransparency)) {
                            wrapped.add(wrapper.shownAs(argument, (ShownAsTransparency) extraInfo[0]));
                        } else if ((extraInfo[0] instanceof Transp)) {
                            ShownAsTransparency shownAs = Transp.TRANSPARENT.equals(extraInfo[0]) ? ShownAsTransparency.FREE : ShownAsTransparency.RESERVED;
                            wrapped.add(wrapper.shownAs(argument, shownAs));
                        } else {
                            LOGGER.warn("Unexpected transparency {}, skipping.", extraInfo[0]);
                        }
                    }
                    break;

                default:
                    LOGGER.debug("Unknown ArgumentType {}", argument);
                    break;
            }
        }

        String localized = sh.getString(message);
        return String.format(localized, wrapped.toArray(new Object[wrapped.size()]));
    }

    private TypeWrapper getWrapper(String format) {
        TypeWrapper typeWrapper = TypeWrapper.WRAPPER.get(format);
        if (null == typeWrapper) {
            return TypeWrapper.WRAPPER.get("text");
        }
        return typeWrapper;
    }

    @Override
    public String toString() {
        return "Sentence [message=" + message + ", arguments=" + arguments + ", types=" + types + "]";
    }

}
