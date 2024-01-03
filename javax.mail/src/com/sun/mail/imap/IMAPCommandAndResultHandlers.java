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

package com.sun.mail.imap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * {@link IMAPCommandAndResultHandlers} - Utility class for command and result handlers.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class IMAPCommandAndResultHandlers {

    /**
     * Initializes a new {@link IMAPCommandAndResultHandlers}.
     */
    private IMAPCommandAndResultHandlers() {
        super();
    }

    /**
     * Injects given handler into specified properties.
     *
     * @param handler The handler to inject
     * @param props The properties to inject to
     */
    public static void injectCommandAndResultHandler(IMAPCommandAndResultHandler handler, Properties props) {
        Object obj = props.get("mail.imap.commandAndResultHandler");
        if (obj != null) {
            if (obj instanceof Collection) {
                ((Collection<IMAPCommandAndResultHandler>) obj).add(handler);
            } else if (obj instanceof IMAPCommandAndResultHandler) {
                List<IMAPCommandAndResultHandler> handlers = new ArrayList<>(2);
                handlers.add((IMAPCommandAndResultHandler) obj);
                handlers.add(handler);
                props.put("mail.imap.commandAndResultHandler", handlers);
            }
        } else {
            props.put("mail.imap.commandAndResultHandler", handler);
        }
    }

    /**
     * Detaches given handler from specified properties.
     *
     * @param handler The handler to detach
     * @param props The properties to detach from
     */
    public static void detachCommandAndResultHandler(IMAPCommandAndResultHandler handler, Properties props) {
        Object obj = props.get("mail.imap.commandAndResultHandler");
        if (obj == null) {
            return;
        }

        if (obj instanceof Collection) {
            Collection<IMAPCommandAndResultHandler> collection = (Collection<IMAPCommandAndResultHandler>) obj;
            if (collection.remove(handler)) {
                switch (collection.size()) {
                    case 0:
                        props.remove("mail.imap.commandAndResultHandler");
                        break;
                    case 1:
                        props.put("mail.imap.commandAndResultHandler", collection.iterator().next());
                        break;
                    default:
                        // Nothing to do...
                }
            }
        } else if (obj.equals(handler)) {
            props.remove("mail.imap.commandAndResultHandler");
        }
    }

}
