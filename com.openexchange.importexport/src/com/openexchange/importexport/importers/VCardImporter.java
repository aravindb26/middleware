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

package com.openexchange.importexport.importers;

import static com.openexchange.java.Autoboxing.I;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.openexchange.contact.similarity.ContactSimilarityService;
import com.openexchange.contact.vcard.VCardImport;
import com.openexchange.contact.vcard.VCardParameters;
import com.openexchange.contact.vcard.VCardService;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXException.Generic;
import com.openexchange.exception.OXExceptionConstants;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.importexport.ImportResult;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.importexport.Format;
import com.openexchange.importexport.exceptions.ImportExportExceptionCodes;
import com.openexchange.importexport.osgi.ImportExportServices;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.session.ServerSession;

/**
 * This importer translates VCards into contacts for the OX.
 *
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias 'Tierlieb' Prinz</a> (minor: changes to new interface)
 */
public class VCardImporter extends ContactImporter implements OXExceptionConstants {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(VCardImporter.class);
    private static final String MAX_SIMILARITY = "maxSimilarity";

    public VCardImporter(ServiceLookup services) {
        super(services);
    }

    @Override
    public boolean canImport(final ServerSession session, final Format format, final List<String> folders, final Map<String, String[]> optionalParams) throws OXException {
        if (!format.equals(Format.VCARD)) {
            return false;
        }
        if (!UserConfigurationStorage.getInstance().getUserConfigurationSafe(session.getUserId(), session.getContext()).hasContact()) {
            throw ImportExportExceptionCodes.CONTACTS_DISABLED.create().setGeneric(Generic.NO_PERMISSION);
        }
        final Iterator<String> iterator = folders.iterator();
        while (iterator.hasNext()) {
            try {
                return ImportExportServices.getContactsAccess().canCreateObjectsInFolder(session, iterator.next());
            } catch (@SuppressWarnings("unused") Exception e) {
                return false;
            }
        }
        return true;
    }


    @SuppressWarnings("resource")
    @Override
    public ImportResults importData(final ServerSession session, final Format format, final InputStream is, final List<String> folders, final Map<String, String[]> optionalParams) throws OXException {
        final String folderId = folders.get(0);
        if(!canImport(session, format, folders, optionalParams)) {
            throw ImportExportExceptionCodes.CANNOT_IMPORT.create(folders.get(0), format);
        }

        final List<ImportResult> list = new ArrayList<ImportResult>();

        SearchIterator<VCardImport> importVCards = null;
        try {
            int count = 0;
            int limit = getLimit(session);

            VCardService vCardService = ImportExportServices.getVCardService();

            VCardParameters vCardParameters = vCardService.createParameters(session);
            vCardParameters.setKeepOriginalVCard(true);
            importVCards = vCardService.importVCards(is, vCardParameters);
            if (false == importVCards.hasNext()) {
                throw ImportExportExceptionCodes.NO_VCARD_FOUND.create();
            }

            String maxSimilarity = null;
            if (optionalParams.containsKey(MAX_SIMILARITY)) {
                String[] tmpArray = optionalParams.get(MAX_SIMILARITY);
                if (tmpArray.length == 1) {
                    maxSimilarity = tmpArray[0];
                }
            }

            while (importVCards.hasNext()) {
                ImportResult importResult = new ImportResult();
                if (limit <= 0 || count <= limit) {
                    try (VCardImport vCardImport = importVCards.next()) {
                        if (vCardImport.getWarnings() != null && vCardImport.getWarnings().size() > 0) {
                            // just take the first warning and add it as exception (even when it is a warning and there might be more)
                            // TODO correct and consistent handling of 'warnings' and 'exceptions' for all ContactImporter
                            importResult.setException(vCardImport.getWarnings().get(0));
                        }
                        Contact contactObj = vCardImport.getContact();
                        contactObj.setFolderId(folderId);

                        if (maxSimilarity!=null){
                            Contact duplicate = checkSimilarity(session, contactObj, Float.parseFloat(maxSimilarity));
                            if (duplicate != null) {
                                importResult.setException(ImportExportExceptionCodes.CONTACT_TOO_SIMILAR.create(contactObj.getUid(), duplicate.getUid(), I(duplicate.getParentFolderID())));
                                list.add(importResult);
                                continue;
                            }
                        }
                        importResult.setDate(new Date());
                        try {
                            super.createContact(session, contactObj, folderId, vCardImport.getVCard());
                            count++;
                        } catch (OXException oxEx) {
                            if (CATEGORY_USER_INPUT.equals(oxEx.getCategory())) {
                                LOG.debug("", oxEx);
                            } else {
                                LOG.error("", oxEx);
                            }
                            importResult.setException(oxEx);
                            LOG.debug("cannot import contact object", oxEx);
                        }
                        importResult.setFolder(folderId);
                        importResult.setObjectId(contactObj.getId());
                        importResult.setDate(contactObj.getLastModified());
                    }
                    list.add(importResult);
                } else {
                    importResult.setException(ImportExportExceptionCodes.LIMIT_EXCEEDED.create(I(limit)));
                    list.add(importResult);
                    break;
                }
            }
        } catch (UnsupportedEncodingException e) {
            LOG.error("", e);
            throw ImportExportExceptionCodes.UTF8_ENCODE_FAILED.create(e);
        } catch (IOException e) {
            LOG.error("", e);
            throw ImportExportExceptionCodes.VCARD_PARSING_PROBLEM.create(e, e.getMessage());
        } finally {
            SearchIterators.close(importVCards);
        }

        return new DefaultImportResults(list);
    }

    private Contact checkSimilarity(ServerSession session, Contact con, float maxSimilarity) throws OXException {
        ContactSimilarityService similarityService = services.getOptionalService(ContactSimilarityService.class);
        if (similarityService == null) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(ContactSimilarityService.class.getSimpleName());
        }
        return similarityService.getSimilar(session, con, maxSimilarity);
    }

}