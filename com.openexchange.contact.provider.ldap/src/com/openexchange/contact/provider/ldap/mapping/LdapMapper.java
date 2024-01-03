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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.configuration.ConfigurationExceptionCodes;
import com.openexchange.contact.SortOptions;
import com.openexchange.contact.SortOrder;
import com.openexchange.contact.provider.ldap.LdapContactsExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.search.Order;
import com.openexchange.groupware.tools.mappings.DefaultMapper;
import com.openexchange.search.Operand;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.controls.SortKey;

/**
 * {@link LdapMapper}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since 7.10.6
 */
public class LdapMapper extends DefaultMapper<Contact, ContactField> {

    private final EnumMap<ContactField, LdapMapping<? extends Object>> mappings;
    private final Set<String> mappedAttributeNames;

    /**
     * Initializes a new {@link LdapMapper} using the supplied contact field to LDAP attribute mappings.
     * 
     * @param attributeMappings The attribute mappings to initialize the mapper with
     * @return The contact mapper
     */
    public static LdapMapper init(Map<String, Object> attributeMappings) throws OXException {
        if (null == attributeMappings) {
            throw ConfigurationExceptionCodes.PROPERTY_MISSING.create("mappings");
        }
        return new LdapMapper(LdapMappingFactory.parseMappings(attributeMappings));
    }

    public LdapMapper(EnumMap<ContactField, LdapMapping<? extends Object>> mappings) {
        super();
        this.mappings = mappings;
        Set<String> attributeNames = new HashSet<String>(mappings.size());
        for (LdapMapping<? extends Object> mapping : mappings.values()) {
            if (null != mapping.getAttributeNames()) {
                for (String attributeName : mapping.getAttributeNames()) {
                    attributeNames.add(attributeName);
                }
            }
        }
        this.mappedAttributeNames = Collections.unmodifiableSet(attributeNames);
    }

    public EnumMap<ContactField, LdapMapping<? extends Object>> getLdapMappings() {
        return mappings;
    }

    @Override
    public Contact newInstance() {
        return new Contact();
    }

    @Override
    public ContactField[] newArray(int size) {
        return new ContactField[size];
    }

    @Override
    public LdapMapping<? extends Object> get(ContactField field) throws OXException {
        LdapMapping<? extends Object> mapping = this.opt(field);
        if (null == mapping) {
            throw OXException.notFound(field.toString());
        }
        return mapping;
    }

    @Override
    public LdapMapping<? extends Object> opt(ContactField field) {
        if (null == field) {
            throw new IllegalArgumentException("field");
        }
        return getMappings().get(field);
    }

    @Override
    protected EnumMap<ContactField, LdapMapping<? extends Object>> getMappings() {
        return mappings;
    }

    /**
     * Gets an LDAP search filter for the supplied search term. 
     * 
     * @param term The search term to get the filter for
     * @return The LDAP search filter
     * @throws OXException In case there is no LDAP representation for the term (unsupported, or not mapped column operand)
     */
    public Filter getFilter(SingleSearchTerm term) throws OXException {
        /*
         * extract column operand and corresponding contact field mapping
         */
        LdapMapping<? extends Object> mapping = getMapping(term);

        /*
         * extract format arguments for operands per attribute name of mapping, then build resulting filter
         */
        Map<String, Object[]> formatArgsPerAttribute = extractFormatArgs(term.getOperands(), mapping);
        return createFilter(term.getOperation(), formatArgsPerAttribute);
    }

    public LdapMapping<?> getMapping(SingleSearchTerm term) throws OXException {
        return get(getMappedField(term));
    }

    public ContactField getMappedField(SingleSearchTerm term) throws OXException {
        try {
            Operand<?> columnOperand = getSingleColumnOperand(term);
            return getField(columnOperand.getValue());
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            throw LdapContactsExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Gets the LDAP attributes associated with the supplied contact fields.
     * <p/>
     * Unmapped fields are skipped.
     * 
     * @param fields The contact fields to get the attributes for, or <code>null</code> to collect the attributes from all mapped fields
     * @param additionalFields Additional fields to add the associated attributes for
     * @return The attributes
     */
    public String[] getAttributes(ContactField[] fields, ContactField... additionalFields) {
        if (null == fields) {
            return mappedAttributeNames.toArray(new String[mappedAttributeNames.size()]);
        }
        Set<String> attributes = new HashSet<String>(fields.length);
        collectAttributes(fields, attributes);
        if (null != additionalFields) {
            collectAttributes(additionalFields, attributes);
        }
        return attributes.toArray(new String[attributes.size()]);
    }

    /**
     * Gets the LDAP attributes associated with the supplied contact fields and adds them to a collection.
     * <p/>
     * Unmapped fields are skipped.
     * 
     * @param fields The contact fields to get the attributes for, or <code>null</code> to collect the attributes from all mapped fields
     * @param attributes The attributes collection to populate
     */
    public void collectAttributes(ContactField[] fields, Collection<String> attributes) {
        if (null == fields) {
            attributes.addAll(mappedAttributeNames);
            return;
        }
        for (ContactField field : fields) {
            LdapMapping<? extends Object> mapping = opt(field);
            if (null != mapping && null != mapping.getAttributeNames()) {
                for (String attributeName : mapping.getAttributeNames()) {
                    attributes.add(attributeName);
                }
            }
        }
    }

    /**
     * Gets the sort keys for passing the requested sort order in an LDAP search.
     *
     * @param sortOptions The sort options to get the sort keys for
     * @return The sort keys, or <code>null</code> if not defined or not mapped
     */
    public SortKey[] getSortKeys(SortOptions sortOptions) {
        if (null != sortOptions && false == SortOptions.EMPTY.equals(sortOptions)
            && null != sortOptions.getOrder() && 0 < sortOptions.getOrder().length) {
            List<SortKey> sortKeys = new ArrayList<SortKey>();
            for (SortOrder sortOrder : sortOptions.getOrder()) {
                LdapMapping<? extends Object> mapping = opt(sortOrder.getBy());
                if (null != mapping && null != mapping.getAttributeNames()) {
                    for (String attributeName : mapping.getAttributeNames()) {
                        sortKeys.add(new SortKey(attributeName, Order.DESCENDING.equals(sortOrder.getOrder())));
                    }
                }
            }
            if (0 < sortKeys.size()) {
                return sortKeys.toArray(new SortKey[sortKeys.size()]);
            }
        }
        return null;
    }

    /**
     * Initializes a new {@link Contact} and sets all mapped properties from the associated LDAP attributes found in an LDAP entry.
     * 
     * @param entry The LDAP entry to create the contact from
     * @param fields The fields to consider, or <code>null</code> to apply all mapped fields
     * @return The contact
     * @throws OXException
     */
    public Contact fromEntry(Entry entry, ContactField[] fields) throws OXException {
        Contact contact = newInstance();
        /*
         * assign mapped attributes
         */
        if (null == fields) {
            for (LdapMapping<?> mapping : mappings.values()) {
                mapping.set(entry, contact);
            }
        } else {
            for (ContactField field : fields) {
                LdapMapping<? extends Object> mapping = opt(field);
                if (null != mapping) {
                    mapping.set(entry, contact);
                }
            }
        }
        return contact;
    }

    public static ContactField getField(Object value) {
        ContactField field = null;
        if ((value instanceof ContactField)) {
            field = (ContactField) value;
        } else {
            field = getField(String.valueOf(value));
        }
        if (null == field) {
            throw new IllegalArgumentException("unable to determine contact field for value: " + value);
        }
        return field;
    }

    private static ContactField getField(String name) {
        try {
            return ContactField.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // try by similarity
            ContactField field = getBySimilarity(name);
            if (null == field) {
                throw e;
            }
            return field;
        }
    }

    private static ContactField getBySimilarity(String name) {
        String normalized = name.replaceAll("[_\\. ]", "").toLowerCase();
        for (ContactField field : ContactField.values()) {
            if (field.toString().replaceAll("[_\\. ]", "").toLowerCase().equals(normalized)) {
                return field;
            }
        }
        return null;
    }

    private static Map<String, Object[]> extractFormatArgs(Operand<?>[] operands, LdapMapping<? extends Object> mapping) throws OXException {
        String[] attributeNames = mapping.getAttributeNames();
        Map<String, Object[]> formatArgsPerAttribute = new LinkedHashMap<String, Object[]>(attributeNames.length);
        for (String attributeName : attributeNames) {
            formatArgsPerAttribute.put(attributeName, new String[operands.length]);
        }
        for (int i = 0; i < operands.length; i++) {
            switch (operands[i].getType()) {
                case COLUMN:
                    /*
                     * inject attribute name for column operand
                     */
                    for (String attributeName : attributeNames) {
                        formatArgsPerAttribute.get(attributeName)[i] = attributeName;
                    }
                    break;
                case CONSTANT:
                    /*
                     * inject encoded value for constant operand
                     */
                    for (String attributeName : attributeNames) {
                        formatArgsPerAttribute.get(attributeName)[i] = mapping.encodeForFilter(attributeName, operands[i].getValue());
                    }
                    break;
                default:
                    throw LdapContactsExceptionCodes.UNEXPECTED_ERROR.create("unknown type in operand: " + operands[i].getType());
            }
        }
        return formatArgsPerAttribute;
    }

    private static Filter createFilter(SingleOperation operation, Map<String, Object[]> formatArgsPerAttribute) throws OXException {
        Iterator<Map.Entry<String, Object[]>> iterator = formatArgsPerAttribute.entrySet().iterator();
        Map.Entry<String, Object[]> firstAttributeEntry = iterator.next();
        try {
            Filter filter = Filter.create('(' + String.format(operation.getLdapRepresentation(), firstAttributeEntry.getValue()) + ')');
            while (iterator.hasNext()) {
                Map.Entry<String, Object[]> attributeEntry = iterator.next();
                Filter firstAttributeIsNullFilter = Filter.createNOTFilter(Filter.createPresenceFilter(firstAttributeEntry.getKey()));
                Filter alternativeFilter = Filter.create('(' + String.format(operation.getLdapRepresentation(), attributeEntry.getValue()) + ')');
                filter = Filter.createORFilter(filter, Filter.createANDFilter(firstAttributeIsNullFilter, alternativeFilter));
            }
            return filter;
        } catch (LDAPException e) {
            throw LdapContactsExceptionCodes.LDAP_ERROR.create(e, e.getMessage());
        }
    }

    private static Operand<?> getSingleColumnOperand(SingleSearchTerm term) throws IllegalArgumentException, UnsupportedOperationException {
        Operand<?> singleOperand = null;
        for (Operand<?> operand : term.getOperands()) {
            if (Operand.Type.COLUMN.equals(operand.getType())) {
                if (null != singleOperand) {
                    throw new IllegalArgumentException("Unable to handle more than one COLUMN-type operand in single search term.");
                }
                singleOperand = operand;
            }
        }
        if (null == singleOperand) {
            throw new IllegalArgumentException("No column operand found in term " + term);
        }
        return singleOperand;
    }

}