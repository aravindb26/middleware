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

package com.openexchange.dav.useragent;

import static com.openexchange.dav.DAVUserAgent.CALDAV_SYNC;
import static com.openexchange.dav.DAVUserAgent.CARDDAV_SYNC;
import static com.openexchange.dav.DAVUserAgent.DAVDROID;
import static com.openexchange.dav.DAVUserAgent.DAVX5;
import static com.openexchange.dav.DAVUserAgent.EM_CLIENT;
import static com.openexchange.dav.DAVUserAgent.IOS;
import static com.openexchange.dav.DAVUserAgent.IOS_REMINDERS;
import static com.openexchange.dav.DAVUserAgent.MAC_CALENDAR;
import static com.openexchange.dav.DAVUserAgent.MAC_CONTACTS;
import static com.openexchange.dav.DAVUserAgent.OUTLOOK_CALDAV_SYNCHRONIZER;
import static com.openexchange.dav.DAVUserAgent.OX_SYNC;
import static com.openexchange.dav.DAVUserAgent.SMOOTH_SYNC;
import static com.openexchange.dav.DAVUserAgent.THUNDERBIRD_CARDBOOK;
import static com.openexchange.dav.DAVUserAgent.THUNDERBIRD_LIGHTNING;
import static com.openexchange.dav.DAVUserAgent.UNKNOWN;
import static com.openexchange.dav.DAVUserAgent.WINDOWS;
import static com.openexchange.dav.DAVUserAgent.WINDOWS_PHONE;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.DefaultInterests;
import com.openexchange.config.DefaultInterests.Builder;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.dav.DAVUserAgent;
import com.openexchange.dav.DAVUserAgentParser;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceLookup;

/**
 * {@link DAVUserAgentParserImpl}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 */
public class DAVUserAgentParserImpl implements Reloadable, DAVUserAgentParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(DAVUserAgentParserImpl.class);

    private final ServiceLookup services;
    private final static Map<DAVUserAgent, String> SEARCHABLE_AGENTS = new HashMap<>(15);
    static {
        SEARCHABLE_AGENTS.put(MAC_CALENDAR, "(?=.*(?:Mac OS|macOS|Mac\\+OS))(?=.*(?:CalendarStore|CalendarAgent|dataaccessd))");
        SEARCHABLE_AGENTS.put(MAC_CONTACTS, "(?:Mac OS|macOS|Mac_OS).*AddressBook.*");
        SEARCHABLE_AGENTS.put(IOS, "iOS.*dataaccessd(?!.*Android)(?<!Android)");
        SEARCHABLE_AGENTS.put(IOS_REMINDERS, "iOS.*remindd(?!.*Android)(?<!Android)");
        SEARCHABLE_AGENTS.put(THUNDERBIRD_LIGHTNING, "(?=.*Thunderbird).*Mozilla");
        SEARCHABLE_AGENTS.put(THUNDERBIRD_CARDBOOK, "(?=.*Thunderbird).*CardBook");
        SEARCHABLE_AGENTS.put(EM_CLIENT, "eM(\\s)?Client");
        SEARCHABLE_AGENTS.put(OX_SYNC, "com\\.openexchange\\.mobile\\.syncapp\\.enterprise");
        SEARCHABLE_AGENTS.put(CALDAV_SYNC, "org\\.dmfs\\.caldav\\.sync");
        SEARCHABLE_AGENTS.put(CARDDAV_SYNC, "org\\.dmfs\\.carddav\\.sync");
        SEARCHABLE_AGENTS.put(SMOOTH_SYNC, "SmoothSync");
        SEARCHABLE_AGENTS.put(DAVDROID, "DAVdroid");
        SEARCHABLE_AGENTS.put(DAVX5, "DAVx5");
        SEARCHABLE_AGENTS.put(OUTLOOK_CALDAV_SYNCHRONIZER, "CalDavSynchronizer");
        SEARCHABLE_AGENTS.put(WINDOWS_PHONE, "MSFT-WP");
        SEARCHABLE_AGENTS.put(WINDOWS, "MSFT-WIN");
    }

    private Map<DAVUserAgent, Pattern> patterns;

    /**
     * Initializes a new {@link DAVUserAgentParserImpl}.
     * 
     * @param services The services
     * @param configViewFactory
     *
     */
    public DAVUserAgentParserImpl(ServiceLookup services, ConfigViewFactory configViewFactory) {
        super();
        this.services = services;
        this.patterns = initialize(configViewFactory);
    }

    private Map<DAVUserAgent, Pattern> initialize(ConfigViewFactory configViewFactory) {
        ConfigView view = null;
        try {
            view = null != configViewFactory ? configViewFactory.getView() : null;
        } catch (OXException e) {
            LOGGER.debug("Unable to get global config view. Using pre-defnied patterns for user agent matching.", e);
        }

        Map<DAVUserAgent, Pattern> agents = new HashMap<>(SEARCHABLE_AGENTS.size());
        for (Entry<DAVUserAgent, String> entry : SEARCHABLE_AGENTS.entrySet()) {
            DAVUserAgent agent = entry.getKey();
            String regex = null;
            if (null != view) {
                try {
                    regex = view.get(getpropertyName(agent), String.class);
                } catch (OXException e) {
                    LOGGER.debug("Unable to find property {}. Using pre-defnied pattern for user agent matching of {}.", getpropertyName(agent), agent.name(), e);
                }
            }
            agents.put(agent, Pattern.compile(Strings.isEmpty(regex) ? entry.getValue() : regex));
        }
        return agents;
    }

    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        initialize(services.getService(ConfigViewFactory.class));
    }

    @Override
    public Interests getInterests() {
        Builder builder = DefaultInterests.builder();
        for (DAVUserAgent davUserAgent : SEARCHABLE_AGENTS.keySet()) {
            builder.propertiesOfInterest(getpropertyName(davUserAgent));
        }
        return builder.build();
    }

    @Override
    public DAVUserAgent parse(String userAgent) {
        if (Strings.isEmpty(userAgent)) {
            return UNKNOWN;
        }
        for (Entry<DAVUserAgent, Pattern> entry : patterns.entrySet()) {
            Matcher matcher = entry.getValue().matcher(userAgent);
            if (matcher.find()) {
                return entry.getKey();
            }
        }
        return UNKNOWN;
    }

    private String getpropertyName(DAVUserAgent agent) {
        return "com.openexchange.dav.useragent." + agent.name().toLowerCase();
    }

}
