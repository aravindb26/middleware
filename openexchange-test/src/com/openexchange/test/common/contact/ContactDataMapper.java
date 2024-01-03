///*
//* @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
//* @license AGPL-3.0
//*
//* This code is free software: you can redistribute it and/or modify
//* it under the terms of the GNU Affero General Public License as published by
//* the Free Software Foundation, either version 3 of the License, or
//* (at your option) any later version.
//*
//* This program is distributed in the hope that it will be useful,
//* but WITHOUT ANY WARRANTY; without even the implied warranty of
//* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//* GNU Affero General Public License for more details.
//*
//* You should have received a copy of the GNU Affero General Public License
//* along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
//*
//* Any use of the work other than as authorized under this license or copyright law is prohibited.
//*
//*/
//
//package com.openexchange.test.common.contact;
//
//import static com.openexchange.java.Autoboxing.I;
//import static com.openexchange.java.Autoboxing.b;
//import static com.openexchange.java.Autoboxing.i;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Date;
//import java.util.EnumMap;
//import java.util.EnumSet;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map.Entry;
//import java.util.Set;
//import java.util.TimeZone;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//import com.openexchange.ajax.fields.CommonFields;
//import com.openexchange.ajax.fields.ContactFields;
//import com.openexchange.ajax.fields.DataFields;
//import com.openexchange.ajax.fields.DistributionListFields;
//import com.openexchange.ajax.fields.FolderChildFields;
//import com.openexchange.contacts.json.mapping.SpecialAlphanumSortDistributionListMemberComparator;
//import com.openexchange.exception.OXException;
//import com.openexchange.groupware.contact.ContactUtil;
//import com.openexchange.groupware.contact.helpers.ContactField;
//import com.openexchange.groupware.container.CommonObject;
//import com.openexchange.groupware.container.Contact;
//import com.openexchange.groupware.container.DataObject;
//import com.openexchange.groupware.container.DistributionListEntryObject;
//import com.openexchange.groupware.tools.mappings.json.ArrayMapping;
//import com.openexchange.groupware.tools.mappings.json.BooleanMapping;
//import com.openexchange.groupware.tools.mappings.json.DateMapping;
//import com.openexchange.groupware.tools.mappings.json.DefaultJsonMapper;
//import com.openexchange.groupware.tools.mappings.json.DefaultJsonMapping;
//import com.openexchange.groupware.tools.mappings.json.IntegerMapping;
//import com.openexchange.groupware.tools.mappings.json.JsonMapping;
//import com.openexchange.groupware.tools.mappings.json.ListMapping;
//import com.openexchange.groupware.tools.mappings.json.StringMapping;
//import com.openexchange.groupware.tools.mappings.json.TimeMapping;
//import com.openexchange.java.Strings;
//import com.openexchange.mail.MailField;
//import com.openexchange.mail.mime.QuotedInternetAddress;
//import com.openexchange.mail.mime.utils.MimeMessageUtility;
//import com.openexchange.session.Session;
//import com.openexchange.testing.httpclient.models.ContactData;
//import com.openexchange.testing.httpclient.models.DistributionListMember;
//import com.openexchange.testing.httpclient.models.DistributionListMember.MailFieldEnum;
//import com.openexchange.tools.session.ServerSessionAdapter;
//
///**
// * {@link ContactMapper} - JSON mapper for contacts.
// *
// * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
// */
//public class ContactDataMapper extends DefaultJsonMapper<ContactData, ContactField> {
//
//    private static final ContactDataMapper INSTANCE = new ContactDataMapper();
//
//    private ContactField[] allFields = null;
//
//    /**
//     * Gets the ContactMapper instance.
//     *
//     * @return The ContactMapper instance.
//     */
//    public static ContactDataMapper getInstance() {
//        return INSTANCE;
//    }
//
//    /**
//     * Initializes a new {@link ContactMapper}.
//     */
//    private ContactDataMapper() {
//        super();
//
//    }
//
//    public ContactField[] getAssignedFields(final ContactData contact, ContactField... mandatoryFields) {
//        if (null == contact) {
//            throw new IllegalArgumentException("contact");
//        }
//        Set<ContactField> setFields = new HashSet<>();
//        for (Entry<ContactField, ? extends JsonMapping<? extends Object, ContactData>> entry : getMappings().entrySet()) {
//            JsonMapping<? extends Object, ContactData> mapping = entry.getValue();
//            if (mapping.isSet(contact)) {
//                ContactField field = entry.getKey();
//                setFields.add(field);
//                if (ContactField.IMAGE1.equals(field)) {
//                    setFields.add(ContactField.IMAGE1_URL); // assume virtual IMAGE1_URL is set, too
//                } else if (ContactField.LAST_MODIFIED.equals(field)) {
//                    setFields.add(ContactField.LAST_MODIFIED_UTC); // assume virtual LAST_MODIFIED_UTC is set, too
//                }
//            }
//        }
//        if (null != mandatoryFields) {
//            setFields.addAll(Arrays.asList(mandatoryFields));
//        }
//        return setFields.toArray(newArray(setFields.size()));
//    }
//
//    @Override
//    public ContactData newInstance() {
//        return new ContactData();
//    }
//
//    public ContactField[] getAllFields() {
//        if (null == allFields) {
//            this.allFields = this.mappings.keySet().toArray(newArray(this.mappings.keySet().size()));
//        }
//        return this.allFields;
//    }
//
//    public ContactField[] getAllFields(EnumSet<ContactField> illegalFields) {
//        List<ContactField> fields = new ArrayList<>();
//        for (ContactField field : getAllFields()) {
//            if (false == illegalFields.contains(field)) {
//                fields.add(field);
//            }
//        }
//        return fields.toArray(new ContactField[fields.size()]);
//    }
//
//    @Override
//    public ContactField[] newArray(int size) {
//        return new ContactField[size];
//    }
//
//    @Override
//    public EnumMap<ContactField, ? extends JsonMapping<? extends Object, ContactData>> getMappings() {
//        return this.mappings;
//    }
//
//    @Override
//    protected EnumMap<ContactField, ? extends JsonMapping<? extends Object, ContactData>> createMappings() {
//
//        final EnumMap<ContactField, JsonMapping<? extends Object, ContactData>> mappings = new EnumMap<>(ContactField.class);
//
//        mappings.put(ContactField.DISPLAY_NAME, new StringMapping<ContactData>(ContactFields.DISPLAY_NAME, I(Contact.DISPLAY_NAME)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setDisplayName(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getDisplayName();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.SUR_NAME, new StringMapping<ContactData>(ContactFields.LAST_NAME, I(Contact.SUR_NAME)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setLastName(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getLastName();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.GIVEN_NAME, new StringMapping<ContactData>(ContactFields.FIRST_NAME, I(501)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setFirstName(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getFirstName();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.MIDDLE_NAME, new StringMapping<ContactData>(ContactFields.SECOND_NAME, I(503)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setSecondName(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getSecondName();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.SUFFIX, new StringMapping<ContactData>(ContactFields.SUFFIX, I(504)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setSuffix(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getSuffix();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TITLE, new StringMapping<ContactData>(ContactFields.TITLE, I(505)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTitle(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTitle();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.STREET_HOME, new StringMapping<ContactData>(ContactFields.STREET_HOME, I(506)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setStreetHome(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getStreetHome();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.POSTAL_CODE_HOME, new StringMapping<ContactData>(ContactFields.POSTAL_CODE_HOME, I(507)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setPostalCodeHome(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getPostalCodeHome();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.CITY_HOME, new StringMapping<ContactData>(ContactFields.CITY_HOME, I(508)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setCityHome(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getCityHome();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.STATE_HOME, new StringMapping<ContactData>(ContactFields.STATE_HOME, I(509)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setStateHome(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getStateHome();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.COUNTRY_HOME, new StringMapping<ContactData>(ContactFields.COUNTRY_HOME, I(510)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setCountryHome(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getCountryHome();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.MARITAL_STATUS, new StringMapping<ContactData>(ContactFields.MARITAL_STATUS, I(512)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setMaritalStatus(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getMaritalStatus();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.NUMBER_OF_CHILDREN, new StringMapping<ContactData>(ContactFields.NUMBER_OF_CHILDREN, I(513)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setNumberOfChildren(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getNumberOfChildren();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.PROFESSION, new StringMapping<ContactData>(ContactFields.PROFESSION, I(514)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setProfession(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getProfession();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.NICKNAME, new StringMapping<ContactData>(ContactFields.NICKNAME, I(515)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setNickname(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getNickname();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.SPOUSE_NAME, new StringMapping<ContactData>(ContactFields.SPOUSE_NAME, I(516)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setSpouseName(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getSpouseName();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.NOTE, new StringMapping<ContactData>(ContactFields.NOTE, I(518)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setNote(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getNote();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.COMPANY, new StringMapping<ContactData>(ContactFields.COMPANY, I(569)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setCompany(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getCompany();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.DEPARTMENT, new StringMapping<ContactData>(ContactFields.DEPARTMENT, I(519)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setDepartment(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getDepartment();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.POSITION, new StringMapping<ContactData>(ContactFields.POSITION, I(520)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setPosition(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getPosition();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.EMPLOYEE_TYPE, new StringMapping<ContactData>(ContactFields.EMPLOYEE_TYPE, I(521)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setEmployeeType(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getEmployeeType();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.ROOM_NUMBER, new StringMapping<ContactData>(ContactFields.ROOM_NUMBER, I(522)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setRoomNumber(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getRoomNumber();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.STREET_BUSINESS, new StringMapping<ContactData>(ContactFields.STREET_BUSINESS, I(523)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setStreetBusiness(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getStreetBusiness();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.POSTAL_CODE_BUSINESS, new StringMapping<ContactData>(ContactFields.POSTAL_CODE_BUSINESS, I(525)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setPostalCodeBusiness(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getPostalCodeBusiness();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.CITY_BUSINESS, new StringMapping<ContactData>(ContactFields.CITY_BUSINESS, I(526)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setCityBusiness(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getCityBusiness();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.STATE_BUSINESS, new StringMapping<ContactData>(ContactFields.STATE_BUSINESS, I(527)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setStateBusiness(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getStateBusiness();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.COUNTRY_BUSINESS, new StringMapping<ContactData>(ContactFields.COUNTRY_BUSINESS, I(528)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setCountryBusiness(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getCountryBusiness();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.NUMBER_OF_EMPLOYEE, new StringMapping<ContactData>(ContactFields.NUMBER_OF_EMPLOYEE, I(529)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setNumberOfEmployees(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getNumberOfEmployees();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.SALES_VOLUME, new StringMapping<ContactData>(ContactFields.SALES_VOLUME, I(530)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setSalesVolume(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getSalesVolume();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TAX_ID, new StringMapping<ContactData>(ContactFields.TAX_ID, I(531)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTaxId(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTaxId();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.COMMERCIAL_REGISTER, new StringMapping<ContactData>(ContactFields.COMMERCIAL_REGISTER, I(532)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setCommercialRegister(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getCommercialRegister();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.BRANCHES, new StringMapping<ContactData>(ContactFields.BRANCHES, I(533)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setBranches(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getBranches();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.BUSINESS_CATEGORY, new StringMapping<ContactData>(ContactFields.BUSINESS_CATEGORY, I(534)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setBusinessCategory(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getBusinessCategory();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.INFO, new StringMapping<ContactData>(ContactFields.INFO, I(535)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setInfo(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getInfo();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.MANAGER_NAME, new StringMapping<ContactData>(ContactFields.MANAGER_NAME, I(536)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setManagerName(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getManagerName();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.ASSISTANT_NAME, new StringMapping<ContactData>(ContactFields.ASSISTANT_NAME, I(537)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setAssistantName(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getAssistantName();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.STREET_OTHER, new StringMapping<ContactData>(ContactFields.STREET_OTHER, I(538)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setStreetOther(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getStreetOther();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.POSTAL_CODE_OTHER, new StringMapping<ContactData>(ContactFields.POSTAL_CODE_OTHER, I(540)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setPostalCodeOther(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getPostalCodeOther();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.CITY_OTHER, new StringMapping<ContactData>(ContactFields.CITY_OTHER, I(539)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setCityOther(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getCityOther();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.STATE_OTHER, new StringMapping<ContactData>(ContactFields.STATE_OTHER, I(598)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setStateOther(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getStateOther();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.COUNTRY_OTHER, new StringMapping<ContactData>(ContactFields.COUNTRY_OTHER, I(541)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setCountryOther(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getCountryOther();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TELEPHONE_ASSISTANT, new StringMapping<ContactData>(ContactFields.TELEPHONE_ASSISTANT, I(568)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTelephoneAssistant(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTelephoneAssistant();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TELEPHONE_BUSINESS1, new StringMapping<ContactData>(ContactFields.TELEPHONE_BUSINESS1, I(542)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTelephoneBusiness1(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTelephoneBusiness1();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TELEPHONE_BUSINESS2, new StringMapping<ContactData>(ContactFields.TELEPHONE_BUSINESS2, I(543)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTelephoneBusiness2(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTelephoneBusiness2();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.FAX_BUSINESS, new StringMapping<ContactData>(ContactFields.FAX_BUSINESS, I(544)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setFaxBusiness(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getFaxBusiness();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TELEPHONE_CALLBACK, new StringMapping<ContactData>(ContactFields.TELEPHONE_CALLBACK, I(545)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTelephoneCallback(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTelephoneCallback();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TELEPHONE_CAR, new StringMapping<ContactData>(ContactFields.TELEPHONE_CAR, I(546)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTelephoneCar(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTelephoneCar();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TELEPHONE_COMPANY, new StringMapping<ContactData>(ContactFields.TELEPHONE_COMPANY, I(547)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTelephoneCompany(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTelephoneCompany();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TELEPHONE_HOME1, new StringMapping<ContactData>(ContactFields.TELEPHONE_HOME1, I(548)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTelephoneHome1(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTelephoneHome1();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TELEPHONE_HOME2, new StringMapping<ContactData>(ContactFields.TELEPHONE_HOME2, I(549)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTelephoneHome2(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTelephoneHome2();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.FAX_HOME, new StringMapping<ContactData>(ContactFields.FAX_HOME, I(550)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setFaxHome(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getFaxHome();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TELEPHONE_ISDN, new StringMapping<ContactData>(ContactFields.TELEPHONE_ISDN, I(559)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTelephoneIsdn(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTelephoneIsdn();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.CELLULAR_TELEPHONE1, new StringMapping<ContactData>(ContactFields.CELLULAR_TELEPHONE1, I(551)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setCellularTelephone1(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getCellularTelephone1();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.CELLULAR_TELEPHONE2, new StringMapping<ContactData>(ContactFields.CELLULAR_TELEPHONE2, I(552)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setCellularTelephone2(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getCellularTelephone2();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TELEPHONE_OTHER, new StringMapping<ContactData>(ContactFields.TELEPHONE_OTHER, I(553)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTelephoneOther(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTelephoneOther();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.FAX_OTHER, new StringMapping<ContactData>(ContactFields.FAX_OTHER, I(554)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setFaxOther(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getFaxOther();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TELEPHONE_PAGER, new StringMapping<ContactData>(ContactFields.TELEPHONE_PAGER, I(560)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTelephonePager(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTelephonePager();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TELEPHONE_PRIMARY, new StringMapping<ContactData>(ContactFields.TELEPHONE_PRIMARY, I(561)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTelephonePrimary(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTelephonePrimary();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TELEPHONE_RADIO, new StringMapping<ContactData>(ContactFields.TELEPHONE_RADIO, I(562)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTelephoneRadio(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTelephoneRadio();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TELEPHONE_TELEX, new StringMapping<ContactData>(ContactFields.TELEPHONE_TELEX, I(563)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTelephoneTelex(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTelephoneTelex();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TELEPHONE_TTYTDD, new StringMapping<ContactData>(ContactFields.TELEPHONE_TTYTDD, I(564)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTelephoneTtytdd(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTelephoneTtytdd();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.INSTANT_MESSENGER1, new StringMapping<ContactData>(ContactFields.INSTANT_MESSENGER1, I(565)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setInstantMessenger1(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getInstantMessenger1();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.INSTANT_MESSENGER2, new StringMapping<ContactData>(ContactFields.INSTANT_MESSENGER2, I(566)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setInstantMessenger2(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getInstantMessenger2();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.TELEPHONE_IP, new StringMapping<ContactData>(ContactFields.TELEPHONE_IP, I(567)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setTelephoneIp(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getTelephoneIp();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.EMAIL1, new StringMapping<ContactData>(ContactFields.EMAIL1, I(555)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setEmail1(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return addr2String(contact.getEmail1());
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.EMAIL2, new StringMapping<ContactData>(ContactFields.EMAIL2, I(556)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setEmail2(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return addr2String(contact.getEmail2());
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.EMAIL3, new StringMapping<ContactData>(ContactFields.EMAIL3, I(557)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setEmail3(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return addr2String(contact.getEmail3());
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.URL, new StringMapping<ContactData>(ContactFields.URL, I(558)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUrl(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUrl();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.CATEGORIES, new StringMapping<ContactData>(CommonFields.CATEGORIES, I(100)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setCategories(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getCategories();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD01, new StringMapping<ContactData>(ContactFields.USERFIELD01, I(571)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield01(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield01();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD02, new StringMapping<ContactData>(ContactFields.USERFIELD02, I(572)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield02(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield02();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD03, new StringMapping<ContactData>(ContactFields.USERFIELD03, I(573)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield03(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield03();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD04, new StringMapping<ContactData>(ContactFields.USERFIELD04, I(574)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield04(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield04();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD05, new StringMapping<ContactData>(ContactFields.USERFIELD05, I(575)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield05(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield05();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD06, new StringMapping<ContactData>(ContactFields.USERFIELD06, I(576)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield06(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield06();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD07, new StringMapping<ContactData>(ContactFields.USERFIELD07, I(577)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield07(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield07();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD08, new StringMapping<ContactData>(ContactFields.USERFIELD08, I(578)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield08(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield08();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD09, new StringMapping<ContactData>(ContactFields.USERFIELD09, I(579)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield09(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield09();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD10, new StringMapping<ContactData>(ContactFields.USERFIELD10, I(580)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield10(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield10();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD11, new StringMapping<ContactData>(ContactFields.USERFIELD11, I(581)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield11(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield11();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD12, new StringMapping<ContactData>(ContactFields.USERFIELD12, I(582)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield12(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield12();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD13, new StringMapping<ContactData>(ContactFields.USERFIELD13, I(583)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield13(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield13();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD14, new StringMapping<ContactData>(ContactFields.USERFIELD14, I(584)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield14(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield14();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD15, new StringMapping<ContactData>(ContactFields.USERFIELD15, I(585)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield15(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield15();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD16, new StringMapping<ContactData>(ContactFields.USERFIELD16, I(586)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield16(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield16();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD17, new StringMapping<ContactData>(ContactFields.USERFIELD17, I(587)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield17(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield17();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD18, new StringMapping<ContactData>(ContactFields.USERFIELD18, I(588)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield18(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield18();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD19, new StringMapping<ContactData>(ContactFields.USERFIELD19, I(589)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield19(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield19();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USERFIELD20, new StringMapping<ContactData>(ContactFields.USERFIELD20, I(590)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUserfield20(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUserfield20();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.OBJECT_ID, new StringMapping<ContactData>(DataFields.ID, I(DataObject.OBJECT_ID)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                int parsedId = Strings.getUnsignedInt(value);
//                if (-1 != parsedId) {
//                    contact.setId(value);
//                }
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return null != get(contact) ? contact.getId() : null;
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.NUMBER_OF_DISTRIBUTIONLIST, new IntegerMapping<ContactData>(ContactFields.NUMBER_OF_DISTRIBUTIONLIST, I(594)) {
//
//            @Override
//            public void set(ContactData contact, Integer value) {
//                if (value != null) {
//                    contact.setNumberOfDistributionList(value);
//                } else {
//                    remove(contact);
//                }
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public Integer get(ContactData contact) {
//                return contact.getNumberOfDistributionList();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.DISTRIBUTIONLIST, new ListMapping<DistributionListMember, ContactData>(ContactFields.DISTRIBUTIONLIST, I(Contact.DISTRIBUTIONLIST)) {
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public void set(ContactData contact, List<DistributionListMember> value) {
//                contact.setDistributionList(value);
//            }
//
//            @Override
//            public List<DistributionListMember> get(ContactData contact) {
//                return contact.getDistributionList();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//
//            @Override
//            protected DistributionListMember deserialize(JSONArray array, int index, TimeZone timeZone) throws JSONException, OXException {
//                JSONObject entry = array.getJSONObject(index);
//                DistributionListMember member = new DistributionListMember();
//                //FIXME: ui sends wrong values for "id": ===========> Bug #21894
//                // "distribution_list":[{"id":"","mail_field":0,"mail":"otto@example.com","display_name":"otto"},
//                //                      {"id":1,"mail_field":1,"mail":"horst@example.com","display_name":"horst"}]
//                if (entry.hasAndNotNull(DataFields.ID) && 0 < entry.getString(DataFields.ID).length()) {
//                    member.setId(entry.getString(DataFields.ID));
//                }
//                if (entry.hasAndNotNull(FolderChildFields.FOLDER_ID)) {
//                    member.setFolderId(entry.getString(FolderChildFields.FOLDER_ID));
//                }
//                if (entry.hasAndNotNull(ContactFields.DISPLAY_NAME)) {
//                    member.setDisplayName(entry.getString(ContactFields.DISPLAY_NAME));
//                }
//                if (entry.hasAndNotNull(DistributionListFields.MAIL)) {
//                    member.setMail(entry.getString(DistributionListFields.MAIL));
//                }
//                if (entry.hasAndNotNull(DistributionListFields.MAIL_FIELD)) {
//                    member.setMailField(MailFieldEnum.fromValue(I(entry.getInt(DistributionListFields.MAIL_FIELD))));
//                }
//                return member;
//            }
//
//            @Override
//            public Object serialize(ContactData from, TimeZone timeZone, Session session) throws JSONException {
//                JSONArray jsonArray = null;
//                List<DistributionListMember> distributionList = this.get(from);
//                if (null != distributionList) {
//                    Locale locale;
//                    try {
//                        locale = ServerSessionAdapter.valueOf(session).getUser().getLocale();
//                    } catch (OXException e) {
//                        locale = null;
//                    }
//                    jsonArray = new JSONArray();
//                    for (DistributionListMember tmp : distributionList) {
//                        JSONObject entry = new JSONObject();
//                        int emailField = tmp.getMailField().ordinal();
//                        if (DistributionListEntryObject.INDEPENDENT != emailField) {
//                            entry.put(DataFields.ID, tmp.getId());
//                            entry.put(FolderChildFields.FOLDER_ID, tmp.getFolderId());
//                        }
//                        entry.put(DistributionListFields.MAIL, tmp.getMail());
//                        entry.put(ContactFields.DISPLAY_NAME, tmp.getDisplayName());
//                        if (null != tmp.getSortName()) {
//                            entry.put(ContactFields.SORT_NAME, tmp.getSortName());
//                        }
//                        entry.put(DistributionListFields.MAIL_FIELD, emailField);
//                        jsonArray.put(entry);
//                    }
//                }
//                return jsonArray;
//            }
//
//        });
//
//        mappings.put(ContactField.PRIVATE_FLAG, new BooleanMapping<ContactData>(CommonFields.PRIVATE_FLAG, I(CommonObject.PRIVATE_FLAG)) {
//
//            @Override
//            public void set(ContactData contact, Boolean value) {
//                contact.setPrivateFlag(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public Boolean get(ContactData contact) {
//                return contact.getPrivateFlag();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.CREATED_BY, new IntegerMapping<ContactData>(DataFields.CREATED_BY, I(2)) {
//
//            @Override
//            public void set(ContactData contact, Integer value) {
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public Integer get(ContactData contact) {
//                return contact.getCreatedBy();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//            }
//        });
//
//        mappings.put(ContactField.MODIFIED_BY, new IntegerMapping<ContactData>(DataFields.MODIFIED_BY, I(3)) {
//
//            @Override
//            public void set(ContactData contact, Integer value) {
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public Integer get(ContactData contact) {
//                return contact.getModifiedBy();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//            }
//        });
//
//        mappings.put(ContactField.CREATION_DATE, new TimeMapping<ContactData>(DataFields.CREATION_DATE, I(4)) {
//
//            @Override
//            public void set(ContactData contact, Date value) {
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public Date get(ContactData contact) {
//                Long creationDate = contact.getCreationDate();
//                if(null != creationDate) {
//                return new Date(creationDate.longValue());
//                }
//                return null;
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//            }
//        });
//
//        mappings.put(ContactField.LAST_MODIFIED, new TimeMapping<ContactData>(DataFields.LAST_MODIFIED, I(DataObject.LAST_MODIFIED)) {
//
//            @Override
//            public void set(ContactData contact, Date value) {
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public Date get(ContactData contact) {
//                return new Date(contact.getLastModified());
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//            }
//        });
//
//        mappings.put(ContactField.BIRTHDAY, new DateMapping<ContactData>(ContactFields.BIRTHDAY, I(511)) {
//
//            @Override
//            public void set(ContactData contact, Date value) {
//                contact.setBirthday(value.getTime());
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public Date get(ContactData contact) {
//                return contact.getBirthday();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.ANNIVERSARY, new DateMapping<ContactData>(ContactFields.ANNIVERSARY, I(517)) {
//
//            @Override
//            public void set(ContactData contact, Date value) {
//                contact.setAnniversary(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public Date get(ContactData contact) {
//                return contact.getAnniversary();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.IMAGE1, new DefaultJsonMapping<byte[], ContactData>(ContactFields.IMAGE1, I(Contact.IMAGE1)) {
//
//            @Override
//            public void set(ContactData contact, byte[] value) {
//                contact.setImage1(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public byte[] get(ContactData contact) {
//                return contact.getImage1();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//
//            @Override
//            public void deserialize(JSONObject from, ContactData to) throws JSONException {
//                Object value = from.get(getAjaxName());
//                if (null == value || JSONObject.NULL.equals(value) || 0 == value.toString().length()) {
//                    to.setImage1(null);
//                } else if ((value instanceof byte[])) {
//                    to.setImage1((byte[]) value);
//                } else {
//                    throw new JSONException("unable to deserialize image data");
//                }
//            }
//
//            @Override
//            public void serialize(ContactData from, JSONObject to) throws JSONException {
//                // always serialize as URL
//                try {
//                    ContactDataMapper.getInstance().get(ContactField.IMAGE1_URL).serialize(from, to);
//                } catch (OXException e) {
//                    throw new JSONException(e);
//                }
//            }
//
//            @Override
//            public void serialize(ContactData from, JSONObject to, TimeZone timeZone) throws JSONException {
//                // always serialize as URL
//                try {
//                    ContactDataMapper.getInstance().get(ContactField.IMAGE1_URL).serialize(from, to, timeZone);
//                } catch (OXException e) {
//                    throw new JSONException(e);
//                }
//            }
//
//            @Override
//            public void serialize(ContactData from, JSONObject to, TimeZone timeZone, Session session) throws JSONException {
//                // always serialize as URL
//                try {
//                    ContactDataMapper.getInstance().get(ContactField.IMAGE1_URL).serialize(from, to, timeZone, session);
//                } catch (OXException e) {
//                    throw new JSONException(e);
//                }
//            }
//
//            @Override
//            public Object serialize(ContactData from, TimeZone timeZone, Session session) throws JSONException {
//                // always serialize as URL
//                try {
//                    return ContactDataMapper.getInstance().get(ContactField.IMAGE1_URL).serialize(from, timeZone, session);
//                } catch (OXException e) {
//                    throw new JSONException(e);
//                }
//            }
//        });
//
//        mappings.put(ContactField.IMAGE1_URL, new StringMapping<ContactData>(ContactFields.IMAGE1_URL, I(Contact.IMAGE1_URL)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
////                LOG.debug("Ignoring request to set 'image_url' in contact to '{}'.", value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return 0 < contact.getNumberOfImages() || null != get(contact);Image1() && null != contact.getImage1();
//            }
//
//            @Override
//            public Object serialize(ContactData from, TimeZone timeZone, Session session) throws JSONException {
//                try {
//                    String url = ContactUtil.generateImageUrl(session, from);
//                    if (url == null) {
//                        return JSONObject.NULL;
//                    }
//                    return url;
//                } catch (OXException e) {
//                    throw new JSONException(e);
//                }
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return null;
//            }
//
//            @Override
//            public void remove(ContactData contact) {
////                LOG.debug("Ignoring request to remove 'image_url' from contact.");
//            }
//        });
//
//        mappings.put(ContactField.IMAGE_LAST_MODIFIED, new DateMapping<ContactData>("image_last_modified", I(597)) {
//
//            @Override
//            public void set(ContactData contact, Date value) {
//                contact.setImageLastModified(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public Date get(ContactData contact) {
//                return contact.getImageLastModified();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.INTERNAL_USERID, new IntegerMapping<ContactData>(ContactFields.USER_ID, I(524)) {
//
//            @Override
//            public void set(ContactData contact, Integer value) {
//                contact.setInternalUserId(null == value ? 0 : i(value));
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public Integer get(ContactData contact) {
//                return I(contact.getInternalUserId());
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.COLOR_LABEL, new IntegerMapping<ContactData>(CommonFields.COLORLABEL, I(102)) {
//
//            @Override
//            public void set(ContactData contact, Integer value) {
//                contact.setLabel(null == value ? 0 : i(value));
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public Integer get(ContactData contact) {
//                return I(contact.getLabel());
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.FILE_AS, new StringMapping<ContactData>(ContactFields.FILE_AS, I(Contact.FILE_AS)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setFileAs(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getFileAs();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//
//            @Override
//            public void serialize(ContactData from, JSONObject to) throws JSONException, OXException {
//                if (this.isSet(from)) {
//                    // only serialize if set; workaround for bug #13960
//                    super.serialize(from, to);
//                }
//            }
//
//            @Override
//            public void serialize(ContactData from, JSONObject to, TimeZone timeZone) throws JSONException, OXException {
//                if (this.isSet(from)) {
//                    // only serialize if set; workaround for bug #13960
//                    super.serialize(from, to, timeZone);
//                }
//            }
//
//            @Override
//            public void serialize(ContactData from, JSONObject to, TimeZone timeZone, Session session) throws JSONException, OXException {
//                if (isSet(from)) {
//                    // only serialize if set; workaround for bug #13960
//                    super.serialize(from, to, timeZone, session);
//                }
//            }
//
//            @Override
//            public Object serialize(ContactData from, TimeZone timeZone, Session session) throws JSONException, OXException {
//                if (isSet(from)) {
//                    // only serialize if set; workaround for bug #13960
//                    return super.serialize(from, timeZone, session);
//                }
//                return null;
//            }
//
//        });
//
//        mappings.put(ContactField.DEFAULT_ADDRESS, new IntegerMapping<ContactData>(ContactFields.DEFAULT_ADDRESS, I(Contact.DEFAULT_ADDRESS)) {
//
//            @Override
//            public void set(ContactData contact, Integer value) {
//                contact.setDefaultAddress(null == value ? 0 : i(value));
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public Integer get(ContactData contact) {
//                return I(contact.getDefaultAddress());
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//
//            @Override
//            public void serialize(ContactData from, JSONObject to) throws JSONException, OXException {
//                if (this.isSet(from)) {
//                    // only serialize if set; workaround for bug #13960
//                    super.serialize(from, to);
//                }
//            }
//
//            @Override
//            public void serialize(ContactData from, JSONObject to, TimeZone timeZone) throws JSONException, OXException {
//                if (this.isSet(from)) {
//                    // only serialize if set; workaround for bug #13960
//                    super.serialize(from, to, timeZone);
//                }
//            }
//
//            @Override
//            public void serialize(ContactData from, JSONObject to, TimeZone timeZone, Session session) throws JSONException, OXException {
//                if (isSet(from)) {
//                    // only serialize if set; workaround for bug #13960
//                    super.serialize(from, to, timeZone, session);
//                }
//            }
//
//            @Override
//            public Object serialize(ContactData from, TimeZone timeZone, Session session) throws JSONException, OXException {
//                if (isSet(from)) {
//                    // only serialize if set; workaround for bug #13960
//                    return super.serialize(from, timeZone, session);
//                }
//                return null;
//            }
//
//        });
//
//        mappings.put(ContactField.MARK_AS_DISTRIBUTIONLIST, new BooleanMapping<ContactData>(ContactFields.MARK_AS_DISTRIBUTIONLIST, I(602)) {
//
//            @Override
//            public void set(ContactData contact, Boolean value) {
//                contact.setMarkAsDistributionlist(null == value ? false : b(value));
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public Boolean get(ContactData contact) {
//                return Boolean.valueOf(contact.getMarkAsDistribtuionlist());
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.NUMBER_OF_ATTACHMENTS, new IntegerMapping<ContactData>(CommonFields.NUMBER_OF_ATTACHMENTS, I(104)) {
//
//            @Override
//            public void set(ContactData contact, Integer value) {
//                contact.setNumberOfAttachments(null == value ? 0 : i(value));
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public Integer get(ContactData contact) {
//                return I(contact.getNumberOfAttachments());
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.YOMI_FIRST_NAME, new StringMapping<ContactData>(ContactFields.YOMI_FIRST_NAME, I(Contact.YOMI_FIRST_NAME)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setYomiFirstName(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getYomiFirstName();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.YOMI_LAST_NAME, new StringMapping<ContactData>(ContactFields.YOMI_LAST_NAME, I(Contact.YOMI_LAST_NAME)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setYomiLastName(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getYomiLastName();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.YOMI_COMPANY, new StringMapping<ContactData>(ContactFields.YOMI_COMPANY, I(Contact.YOMI_COMPANY)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setYomiCompany(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getYomiCompany();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.NUMBER_OF_IMAGES, new IntegerMapping<ContactData>(ContactFields.NUMBER_OF_IMAGES, I(596)) {
//
//            @Override
//            public void set(ContactData contact, Integer value) {
//                contact.setNumberOfImages(null == value ? 0 : i(value));
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                //TODO: create ContactData.containsNumberOfImages() method
//                return null != get(contact);
//            }
//
//            @Override
//            public Integer get(ContactData contact) {
//                return I(contact.getNumberOfImages());
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                //TODO: create ContactData.containsNumberOfImages() method
//            }
//        });
//
//        mappings.put(ContactField.IMAGE1_CONTENT_TYPE, new StringMapping<ContactData>("image1_content_type", I(Contact.IMAGE1_CONTENT_TYPE)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setImageContentType(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getImageContentType();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.LAST_MODIFIED_OF_NEWEST_ATTACHMENT, new DateMapping<ContactData>(CommonFields.LAST_MODIFIED_OF_NEWEST_ATTACHMENT_UTC, I(105)) {
//
//            @Override
//            public void set(ContactData contact, Date value) {
//                contact.setLastModifiedOfNewestAttachment(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public Date get(ContactData contact) {
//                return contact.getLastModifiedOfNewestAttachment();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.USE_COUNT, new IntegerMapping<ContactData>(ContactFields.USE_COUNT, I(608)) {
//
//            @Override
//            public void set(ContactData contact, Integer value) {
//                contact.setUseCount(null == value ? 0 : i(value));
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public Integer get(ContactData contact) {
//                return I(contact.getUseCount());
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.LAST_MODIFIED_UTC, new DateMapping<ContactData>(DataFields.LAST_MODIFIED_UTC, I(DataObject.LAST_MODIFIED_UTC)) {
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public void set(ContactData contact, Date value) {
//                contact.setLastModified(value);
//            }
//
//            @Override
//            public Date get(ContactData contact) {
//                return contact.getLastModified();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.HOME_ADDRESS, new StringMapping<ContactData>(ContactFields.ADDRESS_HOME, I(Contact.ADDRESS_HOME)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setAddressHome(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getAddressHome();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.BUSINESS_ADDRESS, new StringMapping<ContactData>(ContactFields.ADDRESS_BUSINESS, I(Contact.ADDRESS_BUSINESS)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setAddressBusiness(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getAddressBusiness();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.OTHER_ADDRESS, new StringMapping<ContactData>(ContactFields.ADDRESS_OTHER, I(Contact.ADDRESS_OTHER)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setAddressOther(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getAddressOther();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.UID, new StringMapping<ContactData>(CommonFields.UID, I(CommonObject.UID)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                contact.setUid(value);
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return null != get(contact);
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getUid();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                set(contact, null);
//            }
//        });
//
//        mappings.put(ContactField.SORT_NAME, new StringMapping<ContactData>(ContactFields.SORT_NAME, I(Contact.SPECIAL_SORTING)) {
//
//            @Override
//            public void set(ContactData contact, String value) {
//                // no
//            }
//
//            @Override
//            public boolean isSet(ContactData contact) {
//                return true;
//            }
//
//            @Override
//            public String get(ContactData contact) {
//                return contact.getSortName();
//            }
//
//            @Override
//            public void remove(ContactData contact) {
//                // no
//            }
//
//            @Override
//            public Object serialize(ContactData from, TimeZone timeZone, Session session) throws OXException {
//                Object value;
//                if (null != session) {
//                    value = from.getSortName(ServerSessionAdapter.valueOf(session).getUser().getLocale());
//                } else {
//                    value = from.getSortName();
//                }
//                return null != value ? value : JSONObject.NULL;
//            }
//        });
//
//        return mappings;
//    }
//
//    static String addr2String(final String primaryAddress) {
//        if (null == primaryAddress) {
//            return primaryAddress;
//        }
//        try {
//            final QuotedInternetAddress addr = new QuotedInternetAddress(primaryAddress);
//            final String sAddress = addr.getAddress();
//            if (sAddress == null) {
//                return addr.toUnicodeString();
//            }
//            final int pos = sAddress.indexOf('/');
//            if (pos <= 0) {
//                // No slash character present
//                return addr.toUnicodeString();
//            }
//
//            String suffix = sAddress.substring(pos);
//            if (!"/TYPE=PLMN".equals(Strings.toUpperCase(suffix))) {
//                // Not an MSISDN address
//                return addr.toUnicodeString();
//            }
//
//            // A MSISDN address; e.g. "+491234567890/TYPE=PLMN"
//            StringBuilder sb = new StringBuilder(32);
//            String personal = addr.getPersonal();
//            if (null == personal) {
//                sb.append(MimeMessageUtility.prepareAddress(sAddress.substring(0, pos)));
//            } else {
//                sb.append(preparePersonal(personal));
//                sb.append(" <").append(MimeMessageUtility.prepareAddress(sAddress.substring(0, pos))).append('>');
//            }
//            return sb.toString();
//        } catch (Exception e) {
//            return primaryAddress;
//        }
//    }
//
//    /**
//     * Prepares specified personal string by surrounding it with quotes if needed.
//     *
//     * @param personal The personal
//     * @return The prepared personal
//     */
//    static String preparePersonal(final String personal) {
//        return MimeMessageUtility.quotePhrase(personal, false);
//    }
//
//}
