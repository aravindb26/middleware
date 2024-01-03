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

package com.openexchange.smtp.dataobjects;

import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.mail.dataobjects.compose.DataMailPart;
import com.openexchange.session.Session;

/**
 * {@link SMTPDataPart}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public final class SMTPDataPart extends DataMailPart {

    private static final long serialVersionUID = -764546421175762516L;

    /**
	 * Initializes a new {@link SMTPDataPart}
	 *
	 * @param data
	 *            The data
	 * @param dataProperties
	 *            The data properties
	 * @param session
	 *            The session
	 * @throws OXException
	 *             If data part cannot be initialized
	 */
	public SMTPDataPart(final Object data, final Map<String, String> dataProperties, final Session session) throws OXException {
		super(data, dataProperties, session);
	}

}
