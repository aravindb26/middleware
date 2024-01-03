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

package com.openexchange.chronos.ical.ical4j.mapping.freebusy;

import com.openexchange.chronos.ExtendedProperties;
import com.openexchange.chronos.ExtendedProperty;
import com.openexchange.chronos.FreeBusyData;
import com.openexchange.chronos.ical.ical4j.mapping.ICalTextMapping;
import com.openexchange.java.Strings;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VFreeBusy;
import net.fortuna.ical4j.model.property.XProperty;

/**
 * {@link MaskUidMapping} - Mapping for {@value #PROPERTY_NAME}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 * @see <a href="https://raw.githubusercontent.com/apple/ccs-calendarserver/master/doc/Extensions/icalendar-maskuids.txt">iCalendar Mask UIDs</a>
 */
public class MaskUidMapping extends ICalTextMapping<VFreeBusy, FreeBusyData> {

    /**
     * The extended property {@value #PROPERTY_NAME}
     * 
     * @see <a href="https://raw.githubusercontent.com/apple/ccs-calendarserver/master/doc/Extensions/icalendar-maskuids.txt">iCalendar Mask UIDs</a>
     */
    public static final String PROPERTY_NAME = "X-CALENDARSERVER-MASK-UID";

    /**
     * Initializes a new {@link MaskUidMapping}.
     */
    public MaskUidMapping() {
        super(PROPERTY_NAME);
    }

    @Override
    protected String getValue(FreeBusyData object) {
        if (null != object.getExtendedProperties() && object.getExtendedProperties().contains(PROPERTY_NAME)) {
            Object value = object.getExtendedProperties().get(PROPERTY_NAME).getValue();
            return null != value ? value.toString() : null;
        }
        return null;
    }

    @Override
    protected void setValue(FreeBusyData object, String value) {
        if (Strings.isEmpty(value)) {
            return;
        }
        ExtendedProperties extendedProperties = object.getExtendedProperties();
        if (null == extendedProperties) {
            extendedProperties = new ExtendedProperties();
            object.setExtendedProperties(extendedProperties);
        }
        extendedProperties.add(new ExtendedProperty(PROPERTY_NAME, value));
    }

    @Override
    protected Property createProperty() {
        return new XProperty(PROPERTY_NAME);
    }

}
