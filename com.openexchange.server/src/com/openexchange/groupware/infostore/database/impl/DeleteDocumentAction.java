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

package com.openexchange.groupware.infostore.database.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.session.Session;
import com.openexchange.tools.arrays.Arrays;
import gnu.trove.list.TIntList;

public class DeleteDocumentAction extends AbstractDocumentListAction {

    private static final int batchSize = 1000;

    /**
     * Initializes a new {@link DeleteDocumentAction}.
     */
    public DeleteDocumentAction(Session session) {
        super(session);
    }

    /**
     * Initializes a new {@link DeleteDocumentAction}.
     *
     * @param provider The database provider
     * @param queryCatalog The query catalog
     * @param context The context
     * @param document The document to delete
     */
    public DeleteDocumentAction(DBProvider provider, InfostoreQueryCatalog queryCatalog, Context context, DocumentMetadata document, Session session) {
        this(provider, queryCatalog, context, Collections.singletonList(document), session);
    }

    /**
     * Initializes a new {@link DeleteDocumentAction}.
     *
     * @param provider The database provider
     * @param queryCatalog The query catalog
     * @param context The context
     * @param documents The documents to delete
     */
    public DeleteDocumentAction(DBProvider provider, InfostoreQueryCatalog queryCatalog, Context context, List<DocumentMetadata> documents, Session session) {
        super(provider, queryCatalog, context, documents, session);
    }

    @Override
    protected void undoAction() throws OXException {
        List<DocumentMetadata> documents = getDocuments();
        if (documents.isEmpty()) {
            return;
        }

        UpdateBlock[] updates = new UpdateBlock[documents.size()];
        int i = 0;
        for (DocumentMetadata doc : documents) {
            updates[i++] = new Update(getQueryCatalog().getDocumentInsert()) {

                @Override
                public void fillStatement() throws SQLException {
                    fillStmt(stmt, getQueryCatalog().getWritableDocumentFields(), doc, Long.valueOf(System.currentTimeMillis()), Integer.valueOf(getContext().getContextId()));
                }

            };
        }

        doUpdates(updates);
    }

    @Override
    public void perform() throws OXException {
        List<DocumentMetadata> documents = getDocuments();
        if (documents.isEmpty()) {
            return;
        }

        List<UpdateBlock> updates = new ArrayList<UpdateBlock>(4);
        groupByFolder(documents).forEachEntry((folderId, docIds) -> addUpdatesFor(docIds, folderId, updates));

        doUpdates(updates);
    }

    private boolean addUpdatesFor(TIntList docIds, long folderId, List<UpdateBlock> updates) {
        int contextId = getContext().getContextId();
        for (int[] batch : Arrays.partition(docIds.toArray(), batchSize)) {
            List<String> deleteStmts = getQueryCatalog().getDelete(InfostoreQueryCatalog.Table.INFOSTORE, batch, folderId, false);
            if (deleteStmts.size() == 1) {
                updates.add(new Update(deleteStmts.get(0)) {

                    @Override
                    public void fillStatement() throws SQLException {
                        stmt.setInt(1, contextId);
                    }
                });
            } else {
                for (String deleteStmt : deleteStmts) {
                    updates.add(new Update(deleteStmt){

                        @Override
                        public void fillStatement() throws SQLException {
                            stmt.setInt(1, contextId);
                        }
                    });
                }
            }
        }
        return true;
    }

    @Override
    protected Object[] getAdditionals(final DocumentMetadata doc) {
        return null;
    }

}
