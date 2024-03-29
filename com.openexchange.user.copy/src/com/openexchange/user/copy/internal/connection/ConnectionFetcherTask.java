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

package com.openexchange.user.copy.internal.connection;

import static com.openexchange.java.Autoboxing.i;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import org.slf4j.Logger;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.user.copy.CopyUserTaskService;
import com.openexchange.user.copy.ObjectMapping;
import com.openexchange.user.copy.UserCopyExceptionCodes;
import com.openexchange.user.copy.internal.CopyTools;
import com.openexchange.user.copy.internal.context.ContextLoadTask;

/**
 * Fetches a read connection for the source context and a write connection for the destination context. Puts connection for the destination
 * context into transaction mode.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class ConnectionFetcherTask implements CopyUserTaskService {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ConnectionFetcherTask.class);
    }

    private final DatabaseService service;

    private Context srcCtx;
    private int dstCtxId;
    private Connection srcCon;
    private Connection dstCon;

    public ConnectionFetcherTask(final DatabaseService service) {
        super();
        this.service = service;
    }

    @Override
    public String[] getAlreadyCopied() {
        return new String[] { ContextLoadTask.class.getName() };
    }

    @Override
    public String getObjectName() {
        return Connection.class.getName();
    }

    @Override
    public ConnectionHolder copyUser(final Map<String, ObjectMapping<?>> copied) throws OXException {
        final CopyTools tools = new CopyTools(copied);
        srcCtx = tools.getSourceContext();
        dstCtxId = i(tools.getDestinationContextId());

        Connection dstCon = null;
        Connection srcCon = service.getReadOnly(srcCtx);
        try {
            this.srcCon = srcCon;
            dstCon = service.getForUpdateTask(dstCtxId);
            this.dstCon = dstCon;

            Databases.startTransaction(dstCon);

            ConnectionHolder retval = new ConnectionHolder();
            retval.addMapping(srcCtx.getContextId(), newNonCloseableConnectionProxy(srcCon), dstCtxId, newNonCloseableConnectionProxy(dstCon));

            srcCon = null; // Avoid premature closing
            dstCon = null; // Avoid premature closing

            return retval;
        } catch (SQLException e) {
            throw UserCopyExceptionCodes.SQL_PROBLEM.create(e);
        } finally {
            if (srcCon != null) {
                this.srcCon = null;
                service.backReadOnly(srcCtx, srcCon);
            }
            if (dstCon != null) {
                this.dstCon = null;
                service.backForUpdateTask(dstCtxId, dstCon);
            }
        }
    }

    @Override
    public void done(final Map<String, ObjectMapping<?>> copied, final boolean failed) {
        if (null != dstCon) {
            try {
                if (failed) {
                    dstCon.rollback();
                } else {
                    dstCon.commit();
                }
            } catch (SQLException e) {
                LoggerHolder.LOG.error("", e);
            }
            service.backForUpdateTask(dstCtxId, dstCon);
            dstCon = null;
        }
        if (null != srcCon) {
            service.backReadOnly(srcCtx, srcCon);
            srcCon = null;
        }
        dstCtxId = 0;
        srcCtx = null;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private Connection newNonCloseableConnectionProxy(Connection connection) {
        return (Connection) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { Connection.class }, new NonCloseableConnection(connection));
    }

    private static class NonCloseableConnection implements InvocationHandler {

        private final Connection connection;

        /**
         * Initializes a new {@link NonCloseableConnection}.
         *
         * @param connection The connection to delegate to
         */
        NonCloseableConnection(Connection connection) {
            super();
            this.connection = connection;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("close".equals(method.getName())) {
                // Suppress close invocation
                return null;
            }

            return method.invoke(connection, args);
        }
    }
}
