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

package com.openexchange.contact.provider.ldap.mapping;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import com.openexchange.configuration.ConfigurationExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;
import com.openexchange.java.Strings;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Filter;

/**
 * {@link LdapMappingFactory}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @param <T>
 * @since 7.10.6
 */
public class LdapMappingFactory {

    /**
     * Initializes the contact mappings from the supplied contact field to LDAP attribute mappings.
     * 
     * @param attributeMappings The attribute mappings to create the contact mappings for, as read from a .yaml configuration file
     * @return The contact mappings
     */
    static EnumMap<ContactField, LdapMapping<? extends Object>> parseMappings(Map<String, Object> attributeMappings) throws OXException {
        EnumMap<ContactField, LdapMapping<? extends Object>> mappings = new EnumMap<ContactField, LdapMapping<? extends Object>>(ContactField.class);
        for (Entry<String, Object> entry : attributeMappings.entrySet()) {
            if (null != entry.getValue() && (entry.getValue() instanceof String)) {
                String value = (String) entry.getValue();
                String[] flags = null;
                int delimiterIndex = value.lastIndexOf(';');
                if (0 <= delimiterIndex && delimiterIndex < value.length()) {
                    flags = Strings.splitByComma(value.substring(delimiterIndex + 1));
                    value = value.substring(0, delimiterIndex);
                }
                String[] attributeNames = Strings.splitByComma(value);
                ContactField field = parseContactField(entry.getKey());
                mappings.put(field, getMapping(field, attributeNames, flags));
            }
        }
        return mappings;
    }

    private static ContactField parseContactField(String fieldName) throws OXException {
        try {
            return LdapMapper.getField(fieldName);
        } catch (IllegalArgumentException e) {
            throw ConfigurationExceptionCodes.INVALID_CONFIGURATION.create(e, "Unknown contact field " + fieldName);
        }
    }

    private static LdapMapping<? extends Object> getMapping(ContactField field, String[] attributeNames, String... flags) throws OXException {

        switch (field) {
            case DISPLAY_NAME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setDisplayName(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsDisplayName();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getDisplayName();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeDisplayName();
                    }
                };

            case SUR_NAME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setSurName(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsSurName();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getSurName();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeSurName();
                    }
                };

            case GIVEN_NAME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setGivenName(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsGivenName();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getGivenName();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeGivenName();
                    }
                };

            case MIDDLE_NAME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setMiddleName(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsMiddleName();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getMiddleName();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeMiddleName();
                    }
                };

            case SUFFIX:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setSuffix(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsSuffix();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getSuffix();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeSuffix();
                    }
                };

            case TITLE:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTitle(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTitle();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTitle();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTitle();
                    }
                };

            case STREET_HOME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setStreetHome(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsStreetHome();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getStreetHome();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeStreetHome();
                    }
                };

            case POSTAL_CODE_HOME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setPostalCodeHome(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsPostalCodeHome();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getPostalCodeHome();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removePostalCodeHome();
                    }
                };

            case CITY_HOME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setCityHome(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsCityHome();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getCityHome();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeCityHome();
                    }
                };

            case STATE_HOME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setStateHome(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsStateHome();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getStateHome();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeStateHome();
                    }
                };

            case COUNTRY_HOME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setCountryHome(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsCountryHome();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getCountryHome();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeCountryHome();
                    }
                };

            case MARITAL_STATUS:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setMaritalStatus(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsMaritalStatus();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getMaritalStatus();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeMaritalStatus();
                    }
                };

            case NUMBER_OF_CHILDREN:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setNumberOfChildren(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsNumberOfChildren();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getNumberOfChildren();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeNumberOfChildren();
                    }
                };

            case PROFESSION:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setProfession(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsProfession();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getProfession();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeProfession();
                    }
                };

            case NICKNAME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setNickname(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsNickname();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getNickname();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeNickname();
                    }
                };

            case SPOUSE_NAME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setSpouseName(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsSpouseName();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getSpouseName();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeSpouseName();
                    }
                };

            case NOTE:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setNote(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsNote();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getNote();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeNote();
                    }
                };

            case COMPANY:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setCompany(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsCompany();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getCompany();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeCompany();
                    }
                };

            case DEPARTMENT:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setDepartment(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsDepartment();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getDepartment();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeDepartment();
                    }
                };

            case POSITION:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setPosition(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsPosition();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getPosition();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removePosition();
                    }
                };

            case EMPLOYEE_TYPE:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setEmployeeType(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsEmployeeType();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getEmployeeType();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeEmployeeType();
                    }
                };

            case ROOM_NUMBER:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setRoomNumber(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsRoomNumber();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getRoomNumber();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeRoomNumber();
                    }
                };

            case STREET_BUSINESS:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setStreetBusiness(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsStreetBusiness();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getStreetBusiness();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeStreetBusiness();
                    }
                };

            case POSTAL_CODE_BUSINESS:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setPostalCodeBusiness(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsPostalCodeBusiness();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getPostalCodeBusiness();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removePostalCodeBusiness();
                    }
                };

            case CITY_BUSINESS:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setCityBusiness(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsCityBusiness();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getCityBusiness();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeCityBusiness();
                    }
                };

            case STATE_BUSINESS:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setStateBusiness(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsStateBusiness();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getStateBusiness();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeStateBusiness();
                    }
                };

            case COUNTRY_BUSINESS:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setCountryBusiness(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsCountryBusiness();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getCountryBusiness();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeCountryBusiness();
                    }
                };

            case NUMBER_OF_EMPLOYEE:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setNumberOfEmployee(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsNumberOfEmployee();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getNumberOfEmployee();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeNumberOfEmployee();
                    }
                };

            case SALES_VOLUME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setSalesVolume(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsSalesVolume();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getSalesVolume();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeSalesVolume();
                    }
                };

            case TAX_ID:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTaxID(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTaxID();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTaxID();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTaxID();
                    }
                };

            case COMMERCIAL_REGISTER:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setCommercialRegister(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsCommercialRegister();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getCommercialRegister();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeCommercialRegister();
                    }
                };

            case BRANCHES:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setBranches(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsBranches();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getBranches();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeBranches();
                    }
                };

            case BUSINESS_CATEGORY:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setBusinessCategory(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsBusinessCategory();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getBusinessCategory();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeBusinessCategory();
                    }
                };

            case INFO:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setInfo(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsInfo();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getInfo();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeInfo();
                    }
                };

            case MANAGER_NAME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setManagerName(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsManagerName();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getManagerName();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeManagerName();
                    }
                };

            case ASSISTANT_NAME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setAssistantName(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsAssistantName();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getAssistantName();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeAssistantName();
                    }
                };

            case STREET_OTHER:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setStreetOther(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsStreetOther();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getStreetOther();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeStreetHome();
                    }
                };

            case POSTAL_CODE_OTHER:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setPostalCodeOther(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsPostalCodeOther();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getPostalCodeOther();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removePostalCodeHome();
                    }
                };

            case CITY_OTHER:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setCityOther(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsCityOther();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getCityOther();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeCityOther();
                    }
                };

            case STATE_OTHER:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setStateOther(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsStateOther();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getStateOther();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeStateOther();
                    }
                };

            case COUNTRY_OTHER:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setCountryOther(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsCountryOther();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getCountryOther();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeCountryOther();
                    }
                };

            case TELEPHONE_ASSISTANT:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTelephoneAssistant(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTelephoneAssistant();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTelephoneAssistant();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTelephoneAssistant();
                    }
                };

            case TELEPHONE_BUSINESS1:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTelephoneBusiness1(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTelephoneBusiness1();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTelephoneBusiness1();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTelephoneBusiness1();
                    }
                };

            case TELEPHONE_BUSINESS2:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTelephoneBusiness2(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTelephoneBusiness2();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTelephoneBusiness2();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTelephoneBusiness2();
                    }
                };

            case FAX_BUSINESS:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setFaxBusiness(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsFaxBusiness();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getFaxBusiness();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeFaxBusiness();
                    }
                };

            case TELEPHONE_CALLBACK:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTelephoneCallback(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTelephoneCallback();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTelephoneCallback();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTelephoneCallback();
                    }
                };

            case TELEPHONE_CAR:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTelephoneCar(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTelephoneCar();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTelephoneCar();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTelephoneCar();
                    }
                };

            case TELEPHONE_COMPANY:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTelephoneCompany(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTelephoneCompany();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTelephoneCompany();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTelephoneCompany();
                    }
                };

            case TELEPHONE_HOME1:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTelephoneHome1(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTelephoneHome1();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTelephoneHome1();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTelephoneHome1();
                    }
                };

            case TELEPHONE_HOME2:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTelephoneHome2(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTelephoneHome2();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTelephoneHome2();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTelephoneHome2();
                    }
                };

            case FAX_HOME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setFaxHome(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsFaxHome();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getFaxHome();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeFaxHome();
                    }
                };

            case TELEPHONE_ISDN:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTelephoneISDN(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTelephoneISDN();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTelephoneISDN();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTelephoneISDN();
                    }
                };

            case CELLULAR_TELEPHONE1:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setCellularTelephone1(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsCellularTelephone1();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getCellularTelephone1();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeCellularTelephone1();
                    }
                };

            case CELLULAR_TELEPHONE2:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setCellularTelephone2(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsCellularTelephone2();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getCellularTelephone2();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeCellularTelephone2();
                    }
                };

            case TELEPHONE_OTHER:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTelephoneOther(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTelephoneOther();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTelephoneOther();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTelephoneOther();
                    }
                };

            case FAX_OTHER:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setFaxOther(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsFaxOther();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getFaxOther();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeFaxOther();
                    }
                };

            case TELEPHONE_PAGER:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTelephonePager(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTelephonePager();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTelephonePager();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTelephonePager();
                    }
                };

            case TELEPHONE_PRIMARY:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTelephonePrimary(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTelephonePrimary();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTelephonePrimary();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTelephonePrimary();
                    }
                };

            case TELEPHONE_RADIO:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTelephoneRadio(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTelephoneRadio();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTelephoneRadio();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTelephoneRadio();
                    }
                };

            case TELEPHONE_TELEX:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTelephoneTelex(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTelephoneTelex();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTelephoneTelex();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTelephoneTelex();
                    }
                };

            case TELEPHONE_TTYTDD:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTelephoneTTYTTD(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTelephoneTTYTTD();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTelephoneTTYTTD();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTelephoneTTYTTD();
                    }
                };

            case INSTANT_MESSENGER1:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setInstantMessenger1(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsInstantMessenger1();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getInstantMessenger1();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeInstantMessenger1();
                    }
                };

            case INSTANT_MESSENGER2:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setInstantMessenger2(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsInstantMessenger2();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getInstantMessenger2();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeInstantMessenger2();
                    }
                };

            case TELEPHONE_IP:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setTelephoneIP(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsTelephoneIP();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getTelephoneIP();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeTelephoneIP();
                    }
                };

            case EMAIL1:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setEmail1(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsEmail1();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getEmail1();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeEmail1();
                    }
                };

            case EMAIL2:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setEmail2(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsEmail2();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getEmail2();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeEmail2();
                    }
                };

            case EMAIL3:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setEmail3(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsEmail3();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getEmail3();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeEmail3();
                    }
                };

            case URL:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setURL(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsURL();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getURL();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeURL();
                    }
                };

            case CATEGORIES:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setCategories(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsCategories();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getCategories();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeCategories();
                    }
                };

            case USERFIELD01:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField01(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField01();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField01();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField01();
                    }
                };

            case USERFIELD02:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField02(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField02();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField02();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField02();
                    }
                };

            case USERFIELD03:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField03(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField03();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField03();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField03();
                    }
                };

            case USERFIELD04:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField04(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField04();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField04();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField04();
                    }
                };

            case USERFIELD05:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField05(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField05();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField05();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField05();
                    }
                };

            case USERFIELD06:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField06(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField06();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField06();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField06();
                    }
                };

            case USERFIELD07:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField07(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField07();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField07();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField07();
                    }
                };

            case USERFIELD08:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField08(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField08();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField08();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField08();
                    }
                };

            case USERFIELD09:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField09(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField09();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField09();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField09();
                    }
                };

            case USERFIELD10:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField10(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField10();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField10();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField10();
                    }
                };

            case USERFIELD11:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField11(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField11();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField11();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField11();
                    }
                };

            case USERFIELD12:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField12(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField12();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField12();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField12();
                    }
                };

            case USERFIELD13:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField13(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField13();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField13();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField13();
                    }
                };

            case USERFIELD14:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField14(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField14();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField14();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField14();
                    }
                };

            case USERFIELD15:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField15(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField15();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField15();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField15();
                    }
                };

            case USERFIELD16:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField16(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField16();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField16();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField16();
                    }
                };

            case USERFIELD17:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField17(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField17();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField17();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField17();
                    }
                };

            case USERFIELD18:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField18(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField18();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField18();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField18();
                    }
                };

            case USERFIELD19:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField19(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField19();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField19();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField19();
                    }
                };

            case USERFIELD20:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUserField20(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUserField20();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUserField20();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUserField20();
                    }
                };

            case OBJECT_ID:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setId(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsId();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getId();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeId();
                    }
                };

            case CONTEXTID:
                if (null != flags && com.openexchange.tools.arrays.Arrays.contains(flags, "logininfo")) {
                    return new LdapMapPropertyMapping(attributeNames, LdapMapPropertyMapping.PROP_LOGIN_CONTEXT_INFO, flags);
                }
                return new LdapIntegerMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, Integer value) {
                        contact.setContextId(null == value ? 0 : i(value));
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsContextId();
                    }

                    @Override
                    public Integer get(Contact contact) {
                        return I(contact.getContextId());
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeContextID();
                    }
                };

            case PRIVATE_FLAG:
                return new LdapBooleanMapping(extractAttributesAndValues(attributeNames)) {

                    @Override
                    public void set(Contact contact, Boolean value) {
                        contact.setPrivateFlag(null != value && value.booleanValue());
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsPrivateFlag();
                    }

                    @Override
                    public Boolean get(Contact contact) {
                        return Boolean.valueOf(contact.getPrivateFlag());
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removePrivateFlag();
                    }
                };

            case CREATED_BY:
                return new LdapIntegerMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, Integer value) {
                        contact.setCreatedBy(value.intValue());
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsCreatedBy();
                    }

                    @Override
                    public Integer get(Contact contact) {
                        return I(contact.getCreatedBy());
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeCreatedBy();
                    }
                };

            case MODIFIED_BY:
                return new LdapIntegerMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, Integer value) {
                        contact.setModifiedBy(value.intValue());
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsModifiedBy();
                    }

                    @Override
                    public Integer get(Contact contact) {
                        return I(contact.getModifiedBy());
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeModifiedBy();
                    }
                };

            case CREATION_DATE:
                return new LdapDateMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, Date value) {
                        contact.setCreationDate(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsCreationDate();
                    }

                    @Override
                    public Date get(Contact contact) {
                        return contact.getCreationDate();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeCreationDate();
                    }
                };

            case LAST_MODIFIED:
                return new LdapDateMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, Date value) {
                        contact.setLastModified(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsLastModified();
                    }

                    @Override
                    public Date get(Contact contact) {
                        return contact.getLastModified();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeLastModified();
                    }
                };

            case BIRTHDAY:
                return new LdapDateMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, Date value) {
                        contact.setBirthday(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsBirthday();
                    }

                    @Override
                    public Date get(Contact contact) {
                        return contact.getBirthday();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeBirthday();
                    }
                };

            case ANNIVERSARY:
                return new LdapDateMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, Date value) {
                        contact.setAnniversary(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsAnniversary();
                    }

                    @Override
                    public Date get(Contact contact) {
                        return contact.getAnniversary();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeAnniversary();
                    }
                };

            case IMAGE1:
                return new LdapMapping<byte[]>(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, byte[] value) {
                        contact.setImage1(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsImage1();
                    }

                    @Override
                    public byte[] get(Contact contact) {
                        return contact.getImage1();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeImage1();
                    }

                    @Override
                    protected byte[] get(Attribute attribute) {
                        return attribute.getValueByteArray();
                    }

                    @Override
                    public String encodeForFilter(String attributeName, Object value) throws OXException {
                        return "*".equals(value) ? "*" : super.encodeForFilter(attributeName, value);
                    }

                    @Override
                    protected String encode(byte[] value) {
                        return Filter.encodeValue(value);
                    }
                };

            case IMAGE1_CONTENT_TYPE:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setImageContentType(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsImageContentType();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getImageContentType();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeImageContentType();
                    }

                    @Override
                    protected String get(Attribute attribute) {
                        /*
                         * mapping determines it's property value based on the presence of an ldap image
                         */
                        Object value = attribute.getValueByteArray();
                        return null != value ? "image/jpeg" : null;
                    }
                };

            case IMAGE_LAST_MODIFIED:
                return new LdapDateMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, Date value) {
                        contact.setImageLastModified(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsImageLastModified();
                    }

                    @Override
                    public Date get(Contact contact) {
                        return contact.getImageLastModified();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeImageLastModified();
                    }

                    @Override
                    protected Date get(Attribute attribute) {
                        /*
                         * mapping determines it's property value based on the presence of an ldap image
                         */
                        Object value = attribute.getValueByteArray();
                        return null != value ? new Date(0L) : null;
                    }
                };

            case INTERNAL_USERID:
                if (null != flags && com.openexchange.tools.arrays.Arrays.contains(flags, "logininfo")) {
                    return new LdapMapPropertyMapping(attributeNames, LdapMapPropertyMapping.PROP_LOGIN_USER_INFO, flags);
                }
                return new LdapIntegerMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, Integer value) {
                        contact.setInternalUserId(null == value ? 0 : i(value));
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsInternalUserId();
                    }

                    @Override
                    public Integer get(Contact contact) {
                        return I(contact.getInternalUserId());
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeInternalUserId();
                    }
                };

            case COLOR_LABEL:
                return new LdapIntegerMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, Integer value) {
                        contact.setLabel(null == value ? 0 : i(value));
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsLabel();
                    }

                    @Override
                    public Integer get(Contact contact) {
                        return I(contact.getLabel());
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeLabel();
                    }
                };

            case FILE_AS:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setFileAs(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsFileAs();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getFileAs();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeFileAs();
                    }
                };

            case DEFAULT_ADDRESS:
                return new LdapIntegerMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, Integer value) {
                        contact.setDefaultAddress(value.intValue());
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsDefaultAddress();
                    }

                    @Override
                    public Integer get(Contact contact) {
                        return I(contact.getDefaultAddress());
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeDefaultAddress();
                    }
                };

            case MARK_AS_DISTRIBUTIONLIST:
                return new LdapBooleanMapping(extractAttributesAndValues(attributeNames)) {

                    @Override
                    public void set(Contact contact, Boolean value) {
                        contact.setMarkAsDistributionlist(null != value && value.booleanValue());
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsMarkAsDistributionlist();
                    }

                    @Override
                    public Boolean get(Contact contact) {
                        return Boolean.valueOf(contact.getMarkAsDistribtuionlist());
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeMarkAsDistributionlist();
                    }
                };

            case DISTRIBUTIONLIST:
                return new LdapDistListMapping(attributeNames, flags) {

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsDistributionLists();
                    }

                    @Override
                    public void set(Contact contact, DistributionListEntryObject[] value) throws OXException {
                        contact.setDistributionList(value);
                    }

                    @Override
                    public DistributionListEntryObject[] get(Contact contact) {
                        return contact.getDistributionList();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeDistributionLists();
                    }
                };

            case NUMBER_OF_ATTACHMENTS:
                return new LdapIntegerMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, Integer value) {
                        contact.setNumberOfAttachments(value.intValue());
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsNumberOfAttachments();
                    }

                    @Override
                    public Integer get(Contact contact) {
                        return I(contact.getNumberOfAttachments());
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeNumberOfAttachments();
                    }
                };

            case YOMI_FIRST_NAME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setYomiFirstName(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsYomiFirstName();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getYomiFirstName();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeYomiFirstName();
                    }
                };

            case YOMI_LAST_NAME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setYomiLastName(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsYomiLastName();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getYomiLastName();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeYomiLastName();
                    }
                };

            case YOMI_COMPANY:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setYomiCompany(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsYomiCompany();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getYomiCompany();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeYomiCompany();
                    }
                };

            case NUMBER_OF_IMAGES:
                return new LdapBitMapping(extractAttributesAndValues(attributeNames)) {

                    @Override
                    public void set(Contact contact, Integer value) {
                        contact.setNumberOfImages(null != value ? i(value) : 0);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        //TODO: create Contact.containsNumberOfImages() method
                        return contact.containsImage1();
                    }

                    @Override
                    public Integer get(Contact contact) {
                        return I(contact.getNumberOfImages());
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.setNumberOfImages(0);
                    }
                };

            case HOME_ADDRESS:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setAddressHome(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsAddressHome();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getAddressHome();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeAddressHome();
                    }
                };

            case BUSINESS_ADDRESS:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setAddressBusiness(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsAddressBusiness();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getAddressBusiness();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeAddressBusiness();
                    }
                };

            case OTHER_ADDRESS:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setAddressOther(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsAddressOther();

                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getAddressOther();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeAddressOther();
                    }
                };

            case UID:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setUid(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsUid();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getUid();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeUid();
                    }
                };

            case FILENAME:
                return new LdapStringMapping(attributeNames, flags) {

                    @Override
                    public void set(Contact contact, String value) {
                        contact.setFilename(value);
                    }

                    @Override
                    public boolean isSet(Contact contact) {
                        return contact.containsFilename();
                    }

                    @Override
                    public String get(Contact contact) {
                        return contact.getFilename();
                    }

                    @Override
                    public void remove(Contact contact) {
                        contact.removeFilename();
                    }
                };

            default:
                throw ConfigurationExceptionCodes.INVALID_CONFIGURATION.create("Unknown contact field " + field);
        }
    }

    private static Map<String, String> extractAttributesAndValues(String[] attributeNames) throws OXException {
        Map<String, String> attributesAndValues = new LinkedHashMap<String, String>(attributeNames.length);
        for (String attributeName : attributeNames) {
            String[] splitted = Strings.splitBy(attributeName, '=', true);
            if (2 != splitted.length) {
                throw ConfigurationExceptionCodes.INVALID_CONFIGURATION.create("need \"attributeName=valueToMatch\" notation" + attributeName);
            }
            attributesAndValues.put(splitted[0], splitted[1]);
        }
        return attributesAndValues;
    }

}