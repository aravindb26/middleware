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

package com.openexchange.chronos.ical.ical4j.extensions;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactory;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.property.DtStamp;

/**
 * {@link LenientDtStamp}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class LenientDtStamp extends DtStamp {

    public static final PropertyFactory FACTORY = new Factory();
    public static final String PROPERTY_NAME = Property.DTSTAMP;

    private static final long serialVersionUID = -4174897483634216534L;

    public LenientDtStamp() {
        super();
    }

    public LenientDtStamp(ParameterList aList, String aValue) throws ParseException {
        super(aList, aValue);
    }

    @Override
    public void setDateTime(final DateTime dateTime) {
        // time must be in UTC..
        if (dateTime != null) {
            final DateTime utcDateTime = new DateTime(dateTime);
            utcDateTime.setUtc(true);
            setDate(utcDateTime);
        } else {
            setDate(dateTime);
        }
    }

    @Override
    public void setTimeZone(TimeZone timezone) {
        // ignore
        if (null != timezone && false == "UTC".equals(timezone.getID())) {
            org.slf4j.LoggerFactory.getLogger(LenientDtStamp.class).warn("Ignoring attempt to set timezone \"{}\" for DTSTAMP property", timezone.getID());
        }
    }

    private static class Factory implements PropertyFactory {

        private static final long serialVersionUID = 1030126682318456760L;

        /**
         * Initializes a new {@link Factory}.
         */
        Factory() {
            super();
        }

        @Override
        public Property createProperty(String name, ParameterList parameters, String value) throws IOException, URISyntaxException, ParseException {
            return new LenientDtStamp(parameters, value);
        }

        @Override
        public Property createProperty(String name) {
            return new LenientDtStamp();
        }

    }

}
