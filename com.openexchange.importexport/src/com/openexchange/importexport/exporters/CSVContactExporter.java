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

package com.openexchange.importexport.exporters;

import static com.openexchange.importexport.formats.csv.CSVLibrary.CELL_DELIMITER;
import static com.openexchange.importexport.formats.csv.CSVLibrary.ROW_DELIMITER;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.config.ConfigurationService;
import com.openexchange.contacts.json.mapping.ContactMapper;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.contact.helpers.ContactGetter;
import com.openexchange.groupware.contact.helpers.ContactStringGetter;
import com.openexchange.groupware.container.Contact;
import com.openexchange.importexport.Exporter;
import com.openexchange.importexport.Format;
import com.openexchange.importexport.actions.exporter.ContactExportAction;
import com.openexchange.importexport.exceptions.ImportExportExceptionCodes;
import com.openexchange.importexport.helpers.ExportFileNameCreator;
import com.openexchange.importexport.helpers.SizedInputStream;
import com.openexchange.importexport.osgi.ImportExportServices;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.tools.session.ServerSession;
import gnu.trove.set.TCharSet;
import gnu.trove.set.hash.TCharHashSet;

/**
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 * @author <a href="mailto:Jan-Oliver.Huhn@open-xchange.com">Jan-Oliver Huhn</a> batch export
 */
public class CSVContactExporter implements Exporter {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CSVContactExporter.class);

    /**
     * All possible contact fields as used by the CSV contact exporter
     */
    protected static final EnumSet<ContactField> POSSIBLE_FIELDS = EnumSet.of(
        ContactField.CREATED_BY, ContactField.CREATION_DATE, ContactField.LAST_MODIFIED, ContactField.MODIFIED_BY,
        // CommonObject.PRIVATE_FLAG, // CommonObject.CATEGORIES,
        ContactField.CATEGORIES,
        ContactField.SUR_NAME, ContactField.ANNIVERSARY, ContactField.ASSISTANT_NAME, ContactField.BIRTHDAY, ContactField.BRANCHES,
        ContactField.BUSINESS_CATEGORY, ContactField.CATEGORIES, ContactField.CELLULAR_TELEPHONE1, ContactField.CELLULAR_TELEPHONE2,
        ContactField.CITY_BUSINESS, ContactField.CITY_HOME, ContactField.CITY_OTHER, ContactField.COMMERCIAL_REGISTER,
        ContactField.COMPANY, ContactField.COUNTRY_BUSINESS, ContactField.COUNTRY_HOME, ContactField.COUNTRY_OTHER,
        ContactField.DEPARTMENT, ContactField.DISPLAY_NAME, ContactField.DISTRIBUTIONLIST, ContactField.EMAIL1, ContactField.EMAIL2,
        ContactField.EMAIL3, ContactField.EMPLOYEE_TYPE, ContactField.FAX_BUSINESS, ContactField.FAX_HOME, ContactField.FAX_OTHER,
        // ContactFieldObject.FILE_AS,
        ContactField.GIVEN_NAME,
        // ContactFieldObject.IMAGE1, // ContactFieldObject.IMAGE1_CONTENT_TYPE,
        ContactField.INFO, ContactField.INSTANT_MESSENGER1, ContactField.INSTANT_MESSENGER2,
        // ContactFieldObject.LINKS,
        ContactField.MARK_AS_DISTRIBUTIONLIST,
        ContactField.MANAGER_NAME, ContactField.MARITAL_STATUS, ContactField.MIDDLE_NAME, ContactField.NICKNAME, ContactField.NOTE,
        ContactField.NUMBER_OF_CHILDREN, ContactField.NUMBER_OF_EMPLOYEE, ContactField.POSITION, ContactField.POSTAL_CODE_BUSINESS,
        ContactField.POSTAL_CODE_HOME, ContactField.POSTAL_CODE_OTHER,
        // ContactFieldObject.PRIVATE_FLAG,
        ContactField.PROFESSION, ContactField.ROOM_NUMBER, ContactField.SALES_VOLUME, ContactField.SPOUSE_NAME,
        ContactField.STATE_BUSINESS,  ContactField.STATE_HOME, ContactField.STATE_OTHER, ContactField.STREET_BUSINESS,
        ContactField.STREET_HOME, ContactField.STREET_OTHER, ContactField.SUFFIX, ContactField.TAX_ID, ContactField.TELEPHONE_ASSISTANT,
        ContactField.TELEPHONE_BUSINESS1, ContactField.TELEPHONE_BUSINESS2, ContactField.TELEPHONE_CALLBACK, ContactField.TELEPHONE_CAR,
        ContactField.TELEPHONE_COMPANY, ContactField.TELEPHONE_HOME1, ContactField.TELEPHONE_HOME2, ContactField.TELEPHONE_IP,
        ContactField.TELEPHONE_ISDN, ContactField.TELEPHONE_OTHER, ContactField.TELEPHONE_PAGER, ContactField.TELEPHONE_PRIMARY,
        ContactField.TELEPHONE_RADIO, ContactField.TELEPHONE_TELEX, ContactField.TELEPHONE_TTYTDD, ContactField.TITLE, ContactField.URL,
        ContactField.USERFIELD01, ContactField.USERFIELD02, ContactField.USERFIELD03, ContactField.USERFIELD04, ContactField.USERFIELD05,
        ContactField.USERFIELD06, ContactField.USERFIELD07, ContactField.USERFIELD08, ContactField.USERFIELD09, ContactField.USERFIELD10,
        ContactField.USERFIELD11, ContactField.USERFIELD12, ContactField.USERFIELD13, ContactField.USERFIELD14, ContactField.USERFIELD15,
        ContactField.USERFIELD16, ContactField.USERFIELD17, ContactField.USERFIELD18, ContactField.USERFIELD19, ContactField.USERFIELD20,
        ContactField.DEFAULT_ADDRESS, ContactField.YOMI_FIRST_NAME, ContactField.YOMI_LAST_NAME, ContactField.YOMI_COMPANY,
        ContactField.HOME_ADDRESS, ContactField.BUSINESS_ADDRESS, ContactField.OTHER_ADDRESS
    );

    /**
     * The default columns
     */
    protected static final EnumSet<ContactField> DEFAULT_FIELDS = EnumSet.of(
        ContactField.CREATED_BY, ContactField.CREATION_DATE, ContactField.LAST_MODIFIED, ContactField.MODIFIED_BY,
        ContactField.CATEGORIES,
        ContactField.SUR_NAME, ContactField.ANNIVERSARY, ContactField.ASSISTANT_NAME, ContactField.BIRTHDAY, ContactField.BRANCHES,
        ContactField.BUSINESS_CATEGORY, ContactField.CATEGORIES, ContactField.CELLULAR_TELEPHONE1, ContactField.CELLULAR_TELEPHONE2,
        ContactField.CITY_BUSINESS, ContactField.CITY_HOME, ContactField.CITY_OTHER, ContactField.COMMERCIAL_REGISTER,
        ContactField.COMPANY, ContactField.COUNTRY_BUSINESS, ContactField.COUNTRY_HOME, ContactField.COUNTRY_OTHER,
        ContactField.DEPARTMENT, ContactField.DISPLAY_NAME, ContactField.DISTRIBUTIONLIST, ContactField.EMAIL1, ContactField.EMAIL2,
        ContactField.EMAIL3, ContactField.EMPLOYEE_TYPE, ContactField.FAX_BUSINESS, ContactField.FAX_HOME, ContactField.FAX_OTHER,
        ContactField.GIVEN_NAME,
        ContactField.INFO, ContactField.INSTANT_MESSENGER1, ContactField.INSTANT_MESSENGER2,
        ContactField.MARK_AS_DISTRIBUTIONLIST,
        ContactField.MANAGER_NAME, ContactField.MARITAL_STATUS, ContactField.MIDDLE_NAME, ContactField.NICKNAME, ContactField.NOTE,
        ContactField.NUMBER_OF_CHILDREN, ContactField.NUMBER_OF_EMPLOYEE, ContactField.POSITION, ContactField.POSTAL_CODE_BUSINESS,
        ContactField.POSTAL_CODE_HOME, ContactField.POSTAL_CODE_OTHER,
        ContactField.PROFESSION, ContactField.ROOM_NUMBER, ContactField.SALES_VOLUME, ContactField.SPOUSE_NAME,
        ContactField.STATE_BUSINESS,  ContactField.STATE_HOME, ContactField.STATE_OTHER, ContactField.STREET_BUSINESS,
        ContactField.STREET_HOME, ContactField.STREET_OTHER, ContactField.SUFFIX, ContactField.TAX_ID, ContactField.TELEPHONE_ASSISTANT,
        ContactField.TELEPHONE_BUSINESS1, ContactField.TELEPHONE_BUSINESS2, ContactField.TELEPHONE_CALLBACK, ContactField.TELEPHONE_CAR,
        ContactField.TELEPHONE_COMPANY, ContactField.TELEPHONE_HOME1, ContactField.TELEPHONE_HOME2, ContactField.TELEPHONE_IP,
        ContactField.TELEPHONE_ISDN, ContactField.TELEPHONE_OTHER, ContactField.TELEPHONE_PAGER, ContactField.TELEPHONE_PRIMARY,
        ContactField.TELEPHONE_RADIO, ContactField.TELEPHONE_TELEX, ContactField.TELEPHONE_TTYTDD, ContactField.TITLE, ContactField.URL,
        ContactField.USERFIELD01, ContactField.USERFIELD02, ContactField.USERFIELD03, ContactField.USERFIELD04, ContactField.USERFIELD05,
        ContactField.USERFIELD06, ContactField.USERFIELD07, ContactField.USERFIELD08, ContactField.USERFIELD09, ContactField.USERFIELD10,
        ContactField.USERFIELD11, ContactField.USERFIELD12, ContactField.USERFIELD13, ContactField.USERFIELD14, ContactField.USERFIELD15,
        ContactField.USERFIELD16, ContactField.USERFIELD17, ContactField.USERFIELD18, ContactField.USERFIELD19, ContactField.USERFIELD20,
        ContactField.DEFAULT_ADDRESS
    );

    /**
     * The possible contact fields as array
     */
    protected static final ContactField[] DEFAULT_FIELDS_ARRAY = DEFAULT_FIELDS.toArray(new ContactField[DEFAULT_FIELDS.size()]);

    @Override
    public boolean canExport(final ServerSession session, final Format format, final String folder, final Map<String, Object> optionalParams) {
        if (!format.equals(Format.CSV)) {
            return false;
        }

        try {
            return ImportExportServices.getContactsAccess().canReadFolder(session, folder);
        } catch (@SuppressWarnings("unused") OXException e) {
            return false;
        }
    }

    @Override
    public boolean canExportBatch(ServerSession session, Format format, Map.Entry<String, List<String>> batchIds, Map<String, Object> optionalParams) {
        return canExport(session, format, batchIds.getKey(), optionalParams);
    }

    @Override
    public SizedInputStream exportFolderData(final ServerSession sessObj, final Format format, final String folder, final int[] fieldsToBeExported, final Map<String, Object> optionalParams) throws OXException {
        if (!canExport(sessObj, format, folder, optionalParams)) {
            throw ImportExportExceptionCodes.CANNOT_EXPORT.create(folder, format);
        }

        ContactField[] fields;
        if (fieldsToBeExported == null || fieldsToBeExported.length == 0) {
            fields = DEFAULT_FIELDS_ARRAY;
        } else {
            EnumSet<ContactField> illegalFields = EnumSet.complementOf(POSSIBLE_FIELDS);
            fields = ContactMapper.getInstance().getFields(fieldsToBeExported, illegalFields, (ContactField[]) null);
        }

        final boolean exportDlists;
        if (optionalParams == null) {
            exportDlists = true;
        } else {
            Object oExportDlists = optionalParams.get(ContactExportAction.PARAMETER_EXPORT_DLISTS);
            exportDlists = null == oExportDlists ? true : Boolean.parseBoolean(oExportDlists.toString());
        }

        final boolean sanitize;
        {
            Object oSanitize = optionalParams == null ? null : optionalParams.get(ContactExportAction.PARAMETER_SANITIZE);
            if (oSanitize == null) {
                sanitize = isSanitizingEnabled();
            } else {
                sanitize = Boolean.parseBoolean(oSanitize.toString());
            }
        }

        List<Contact> contacts = null;
        try {
            List<ContactField> fieldList = Arrays.asList(fields.clone());
            if (!fieldList.contains(ContactField.MARK_AS_DISTRIBUTIONLIST)) {
                fieldList = new ArrayList<>(fieldList);
                fieldList.add(ContactField.MARK_AS_DISTRIBUTIONLIST);
            }
            contacts = ImportExportServices.getContactsAccess().getContacts(sessObj, folder, fieldList.toArray(new ContactField[fieldList.size()]));
        } catch (OXException e) {
            throw ImportExportExceptionCodes.LOADING_CONTACTS_FAILED.create(e);
        }

        boolean error = true;
        ThresholdFileHolder sink = new ThresholdFileHolder(); // NOSONARLINT
        try {
            OutputStreamWriter writer = new OutputStreamWriter(sink.asOutputStream(), Charsets.UTF_8);
            StringBuilder bob = new StringBuilder(1024);
            writer.write(convertToLine(com.openexchange.importexport.formats.csv.CSVLibrary.convertToList(fields), false, bob));
            for (Contact current : contacts) {
                try {
                    if (!exportDlists && current.getMarkAsDistribtuionlist()) {
                        continue;
                    }
                    writer.write(convertToLine(convertToList(current, fields), sanitize, bob));
                } catch (Exception e) {
                    LOG.error("Could not retrieve contact from folder {}, Exception was: ", folder, e);
                }

            }
            writer.flush();
            SizedInputStream sizedInputStream = new SizedInputStream(sink.getClosingStream(), sink.getLength(), Format.CSV);
            error = false;
            return sizedInputStream;
        } catch (IOException e) {
            throw ImportExportExceptionCodes.IOEXCEPTION.create(e, e.getMessage());
        } finally {
            if (error) {
                Streams.close(sink);
            }
        }
    }

    @Override
    public SizedInputStream exportBatchData(ServerSession sessObj, Format format, Map<String, List<String>> batchIds, int[] fieldsToBeExported, Map<String, Object> optionalParams) throws OXException {
        for (Map.Entry<String, List<String>> batchEntry : batchIds.entrySet()) {
            if (!canExportBatch(sessObj, format, batchEntry, optionalParams)) {
                throw ImportExportExceptionCodes.CANNOT_EXPORT.create(batchEntry.getKey(), format);
            }
        }
        ContactField[] fields;
        if (fieldsToBeExported == null || fieldsToBeExported.length == 0) {
            fields = DEFAULT_FIELDS_ARRAY;
        } else {
            EnumSet<ContactField> illegalFields = EnumSet.complementOf(POSSIBLE_FIELDS);
            fields = ContactMapper.getInstance().getFields(fieldsToBeExported, illegalFields, (ContactField[]) null);
        }
        final boolean exportDlists;
        if (optionalParams == null) {
            exportDlists = true;
        } else {
            Object oExportDlists = optionalParams.get(ContactExportAction.PARAMETER_EXPORT_DLISTS);
            exportDlists = null == oExportDlists ? true : Boolean.parseBoolean(oExportDlists.toString());
        }
        final boolean sanitize;
        {
            Object oSanitize = optionalParams == null ? null : optionalParams.get(ContactExportAction.PARAMETER_SANITIZE);
            if (oSanitize == null) {
                sanitize = isSanitizingEnabled();
            } else {
                sanitize = Boolean.parseBoolean(oSanitize.toString());
            }
        }
        List<ContactField> fieldList = Arrays.asList(fields.clone());
        if (!fieldList.contains(ContactField.MARK_AS_DISTRIBUTIONLIST)) {
            fieldList = new ArrayList<>(fieldList);
            fieldList.add(ContactField.MARK_AS_DISTRIBUTIONLIST);
        }

        boolean error = true;
        ThresholdFileHolder sink = new ThresholdFileHolder(); // NOSONARLINT
        try {
            OutputStreamWriter writer = new OutputStreamWriter(sink.asOutputStream(), Charsets.UTF_8);
            StringBuilder bob = new StringBuilder(1024);
            writer.write(convertToLine(com.openexchange.importexport.formats.csv.CSVLibrary.convertToList(fields), false, bob));
            for (Map.Entry<String, List<String>> batchEntry : batchIds.entrySet()) {
                List<String> contacts = batchEntry.getValue();
                try {
                    List<Contact> res = ImportExportServices.getContactsAccess().getContacts(sessObj, batchEntry.getKey(), contacts, fieldList.toArray(new ContactField[fieldList.size()]));
                    for (Contact current : res) {
                        try {
                            if (!exportDlists && current.getMarkAsDistribtuionlist()) {
                                continue;
                            }
                            writer.write(convertToLine(convertToList(current, fields), sanitize, bob));
                        } catch (Exception e) {
                            LOG.error("Could not retrieve contact from folder {}, Exception was: ", batchEntry.getKey(), e);
                        }
                    }
                } catch (OXException e) {
                    throw ImportExportExceptionCodes.LOADING_CONTACTS_FAILED.create(e);
                }
            }
            writer.flush();
            SizedInputStream sizedInputStream = new SizedInputStream(sink.getClosingStream(), sink.getLength(), Format.CSV);
            error = false;
            return sizedInputStream;
        } catch (IOException e) {
            throw ImportExportExceptionCodes.IOEXCEPTION.create(e, e.getMessage());
        } finally {
            if (error) {
                Streams.close(sink);
            }
        }
    }

    private static List<String> convertToList(Contact conObj, ContactField[] fields) {
        List<String> l = new ArrayList<>(fields.length);
        ContactStringGetter getter = new ContactStringGetter();
        getter.setDelegate(new ContactGetter());
        for (ContactField field : fields) {
            try {
                l.add((String) field.doSwitch(getter, conObj));
            } catch (Exception e) {
                l.add("");
            }
        }
        return l;
    }

    private static String convertToLine(List<String> line, boolean sanitize, StringBuilder bob) {
        bob.setLength(0);
        boolean first = true;
        for (String token : line) {
            if (first) {
                first = false;
            } else {
                bob.append(CELL_DELIMITER);
            }
            appendEscapedForCsv(token, sanitize, bob);
        }
        bob.append(ROW_DELIMITER);
        return bob.toString();
    }

    private static final TCharSet CSV_SPECIALS = new TCharHashSet(new char[] {'=', '+', '-', '@', '\t', '\n'});

    private static void appendEscapedForCsv(String cellContent, boolean sanitize, StringBuilder bob) {
        if (Strings.isEmpty(cellContent)) {
            bob.append("\"\"");
            return;
        }

        String toExamine = cellContent.trim();
        bob.append('"');
        if (sanitize && CSV_SPECIALS.contains(toExamine.charAt(0))) {
            bob.append('\'');
        }
        bob.append(toExamine.replace("\"", "\"\""));
        bob.append('"');
    }

    private static boolean isSanitizingEnabled() {
        ConfigurationService conf = ImportExportServices.getConfigurationService();
        return conf != null && conf.getBoolProperty("com.openexchange.export.csv.sanitize", false);
    }

    @Override
    public String getFolderExportFileName(ServerSession sessionObj, String folder, String extension) {
        return ExportFileNameCreator.createFolderExportFileName(sessionObj, folder, extension);
    }

    @Override
    public String getBatchExportFileName(ServerSession sessionObj, Map<String, List<String>> batchIds, String extension) {
        return ExportFileNameCreator.createBatchExportFileName(sessionObj, batchIds, extension);
    }

}
