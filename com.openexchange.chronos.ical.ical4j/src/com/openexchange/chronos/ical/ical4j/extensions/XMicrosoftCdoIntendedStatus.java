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

import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactory;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.util.ParameterValidator;

/**
 * {@link XMicrosoftCdoIntendedStatus}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class XMicrosoftCdoIntendedStatus extends Property {

    private static final long serialVersionUID = -6643503441181132264L;

    public static final String PROPERTY_NAME = "X-MICROSOFT-CDO-INTENDEDSTATUS";

    public static final PropertyFactory FACTORY = new Factory();

    private String value;

    public XMicrosoftCdoIntendedStatus(PropertyFactory factory) {
        super(PROPERTY_NAME, factory);
    }

    public XMicrosoftCdoIntendedStatus(ParameterList aList, PropertyFactory factory, String value) {
        super(PROPERTY_NAME, aList, factory);
        setValue(value);
    }

    @Override
    public void setValue(String aValue) {
        this.value = aValue;
    }

    @Override
    public void validate() throws ValidationException {
        ParameterValidator.getInstance().assertOne(Parameter.VALUE, getParameters());
    }

    @Override
    public String getValue() {
        return value;
    }

    private static class Factory implements PropertyFactory {

        private static final long serialVersionUID = -5322390296359819977L;

        @Override
        public Property createProperty(String name) {
            return new XMicrosoftCdoIntendedStatus(this);
        }

        @Override
        public Property createProperty(String name, ParameterList parameters, String value) {
            return new XMicrosoftCdoIntendedStatus(parameters, this, value);
        }
    }

}
