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

package com.openexchange.contact.vcard.impl.mapping;

import java.util.List;
import com.openexchange.contact.vcard.VCardParameters;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import ezvcard.VCard;
import ezvcard.parameter.AddressType;
import ezvcard.property.Address;

/**
 * {@link AddressMapping}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class AddressMapping extends AbstractMapping {

    /**
     * Initializes a new {@link AddressMapping}.
     */
    public AddressMapping() {
        super("ADR", ContactField.STREET_BUSINESS, ContactField.CITY_BUSINESS, ContactField.STATE_BUSINESS,
            ContactField.POSTAL_CODE_BUSINESS, ContactField.COUNTRY_BUSINESS, ContactField.BUSINESS_ADDRESS,
            ContactField.STREET_HOME, ContactField.CITY_HOME, ContactField.STATE_HOME, ContactField.POSTAL_CODE_HOME,
            ContactField.COUNTRY_HOME, ContactField.HOME_ADDRESS, ContactField.STREET_OTHER, ContactField.CITY_OTHER,
            ContactField.STATE_OTHER, ContactField.POSTAL_CODE_OTHER, ContactField.COUNTRY_OTHER, ContactField.OTHER_ADDRESS
        );
    }

    @Override
    public void exportContact(Contact contact, VCard vCard, VCardParameters parameters, List<OXException> warnings) {
        List<Address> addresses = vCard.getAddresses();
        /*
         * business address - type "WORK"
         */
        Address businessAddress = getAddressWithType(addresses, AddressType.WORK);
        if (hasBusinessAddress(contact)) {
            if (null == businessAddress) {
                businessAddress = new Address();
                vCard.addAddress(businessAddress);
                businessAddress.getTypes().add(AddressType.WORK);
            }
            businessAddress.setStreetAddress(contact.getStreetBusiness());
            businessAddress.setLocality(contact.getCityBusiness());
            businessAddress.setRegion(contact.getStateBusiness());
            businessAddress.setPostalCode(contact.getPostalCodeBusiness());
            businessAddress.setCountry(contact.getCountryBusiness());
            businessAddress.setLabel(contact.getAddressBusiness());
            addTypeIfMissing(businessAddress, AddressType.PREF.getValue());
        } else if (null != businessAddress) {
            vCard.removeProperty(businessAddress);
        }
        /*
         * home address - type "HOME"
         */
        Address homeAddress = getAddressWithType(addresses, AddressType.HOME);
        if (hasHomeAddress(contact)) {
            if (null == homeAddress) {
                homeAddress = new Address();
                vCard.addAddress(homeAddress);
                homeAddress.getTypes().add(AddressType.HOME);
            }
            homeAddress.setStreetAddress(contact.getStreetHome());
            homeAddress.setLocality(contact.getCityHome());
            homeAddress.setRegion(contact.getStateHome());
            homeAddress.setPostalCode(contact.getPostalCodeHome());
            homeAddress.setCountry(contact.getCountryHome());
            homeAddress.setLabel(contact.getAddressHome());
        } else if (null != homeAddress) {
            vCard.removeProperty(homeAddress);
        }
        /*
         * other address - type "X-OTHER", or no specific type
         */
        Address otherAddress = getAddressWithType(addresses, TYPE_OTHER);
        if (null == otherAddress) {
            otherAddress = getAddressWithType(addresses, "OTHER");
            if (null == otherAddress) {
                otherAddress = getPropertyWithoutTypes(addresses, 0, AddressType.WORK.getValue(), AddressType.HOME.getValue(), TYPE_OTHER);
            }
            if (null != otherAddress) {
                otherAddress.addParameter(ezvcard.parameter.VCardParameters.TYPE, TYPE_OTHER);
            }
        }
        if (hasOtherAddress(contact)) {
            if (null == otherAddress) {
                otherAddress = new Address();
                vCard.addAddress(otherAddress);
                otherAddress.addParameter(ezvcard.parameter.VCardParameters.TYPE, TYPE_OTHER);
            }
            otherAddress.setStreetAddress(contact.getStreetOther());
            otherAddress.setLocality(contact.getCityOther());
            otherAddress.setRegion(contact.getStateOther());
            otherAddress.setPostalCode(contact.getPostalCodeOther());
            otherAddress.setCountry(contact.getCountryOther());
            otherAddress.setLabel(contact.getAddressOther());
        } else if (null != otherAddress) {
            vCard.removeProperty(otherAddress);
        }
    }

    @Override
    public void importVCard(VCard vCard, Contact contact, VCardParameters parameters, List<OXException> warnings) {
        List<Address> addresses = vCard.getAddresses();
        /*
         * business address - type "WORK"
         */
        Address businessAddress = getAddressWithType(addresses, AddressType.WORK);
        if (null == businessAddress) {
            contact.setStreetBusiness(null);
            contact.setCityBusiness(null);
            contact.setStateBusiness(null);
            contact.setPostalCodeBusiness(null);
            contact.setCountryBusiness(null);
            contact.setAddressBusiness(null);
        } else {
            contact.setStreetBusiness(businessAddress.getStreetAddress());
            contact.setCityBusiness(businessAddress.getLocality());
            contact.setStateBusiness(businessAddress.getRegion());
            contact.setPostalCodeBusiness(businessAddress.getPostalCode());
            contact.setCountryBusiness(businessAddress.getCountry());
            contact.setAddressBusiness(businessAddress.getLabel());
        }
        /*
         * home address - type "HOME"
         */
        Address homeAddress = getAddressWithType(addresses, AddressType.HOME);
        if (null == homeAddress) {
            contact.setStreetHome(null);
            contact.setCityHome(null);
            contact.setStateHome(null);
            contact.setPostalCodeHome(null);
            contact.setCountryHome(null);
            contact.setAddressHome(null);
        } else {
            contact.setStreetHome(homeAddress.getStreetAddress());
            contact.setCityHome(homeAddress.getLocality());
            contact.setStateHome(homeAddress.getRegion());
            contact.setPostalCodeHome(homeAddress.getPostalCode());
            contact.setCountryHome(homeAddress.getCountry());
            contact.setAddressHome(homeAddress.getLabel());
        }
        /*
         * other address - type "X-OTHER", or no specific type
         */
        Address otherAddress = getAddressWithType(addresses, TYPE_OTHER);
        if (null == otherAddress) {
            otherAddress = getAddressWithType(addresses, "OTHER");
            if (null == otherAddress) {
                otherAddress = getPropertyWithoutTypes(addresses, 0, AddressType.WORK.getValue(), AddressType.HOME.getValue(), TYPE_OTHER);
            }
        }
        if (null == otherAddress) {
            contact.setStreetOther(null);
            contact.setCityOther(null);
            contact.setStateOther(null);
            contact.setPostalCodeOther(null);
            contact.setCountryOther(null);
            contact.setAddressOther(null);
        } else {
            contact.setStreetOther(otherAddress.getStreetAddress());
            contact.setCityOther(otherAddress.getLocality());
            contact.setStateOther(otherAddress.getRegion());
            contact.setPostalCodeOther(otherAddress.getPostalCode());
            contact.setCountryOther(otherAddress.getCountry());
            contact.setAddressOther(otherAddress.getLabel());
        }
    }

    private static Address getAddressWithType(List<Address> addresses, AddressType type) {
        Address matchingAddress = null;
        if (null != addresses && 0 < addresses.size()) {
            for (Address address : addresses) {
                List<AddressType> types = address.getTypes();
                if (null != types && types.contains(type)) {
                    if (types.contains(AddressType.PREF)) {
                        /*
                         * prefer the preferred address
                         */
                        return address;
                    }
                    if (null == matchingAddress) {
                        /*
                         * take over first possible match
                         */
                        matchingAddress = address;
                    }
                }
            }
        }
        return matchingAddress;
    }

    private static Address getAddressWithType(List<Address> addresses, String type) {
        Address matchingAddress = null;
        if (null != addresses && 0 < addresses.size()) {
            for (Address address : addresses) {
                List<AddressType> types = address.getTypes();
                if (null != types && 0 < types.size()) {
                    for (AddressType addressType : types) {
                        String value = addressType.getValue();
                        if (null != value && value.equalsIgnoreCase(type)) {
                            if (types.contains(AddressType.PREF)) {
                                /*
                                 * prefer the preferred address
                                 */
                                return address;
                            }
                            if (null == matchingAddress) {
                                /*
                                 * take over first possible match
                                 */
                                matchingAddress = address;
                            }
                        }
                    }
                }
            }
        }
        return matchingAddress;
    }

    private static boolean hasBusinessAddress(Contact contact) {
        return hasOneOf(contact, Contact.ADDRESS_BUSINESS, Contact.STREET_BUSINESS, Contact.CITY_BUSINESS, Contact.STATE_BUSINESS,
            Contact.POSTAL_CODE_BUSINESS, Contact.COUNTRY_BUSINESS);
    }

    private static boolean hasHomeAddress(Contact contact) {
        return hasOneOf(contact, Contact.ADDRESS_HOME, Contact.STREET_HOME, Contact.CITY_HOME, Contact.STATE_HOME,
            Contact.POSTAL_CODE_HOME, Contact.COUNTRY_HOME);
    }

    private static boolean hasOtherAddress(Contact contact) {
        return hasOneOf(contact, Contact.ADDRESS_OTHER, Contact.STREET_OTHER, Contact.CITY_OTHER, Contact.STATE_OTHER,
            Contact.POSTAL_CODE_OTHER, Contact.COUNTRY_OTHER);
    }

}
