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

package com.openexchange.mail.exportpdf;

import java.util.LinkedList;
import java.util.List;
import com.openexchange.exception.OXException;

/**
 * {@link DefaultMailExportResult}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class DefaultMailExportResult implements MailExportResult {

    private List<OXException> warnings;
    private String fileId;

    /**
     * Initialises a new {@link DefaultMailExportResult}.
     */
    public DefaultMailExportResult() {
        this(null, new LinkedList<>());
    }

    /**
     * Initialises a new {@link DefaultMailExportResult}.
     * 
     * @param fileId The file id of the exported mail
     * @param warnings The warnings that might have occurred during mail export
     */
    public DefaultMailExportResult(String fileId, List<OXException> warnings) {
        super();
        this.warnings = null == warnings ? new LinkedList<>() : new LinkedList<>(warnings);
        this.fileId = fileId;
    }

    /**
     * Sets the warnings
     * 
     * @param warnings the warnings to set
     */
    public void setWarnings(List<OXException> warnings) {
        this.warnings = warnings;
    }

    /**
     * Adds warnings to the list
     * 
     * @param warnings the warnings to add
     */
    public void addWarnings(List<OXException> warnings) {
        this.warnings.addAll(warnings);
    }

    /**
     * Adds a warning to the list
     * 
     * @param warning the warning to add
     */
    public void addWarning(OXException warning) {
        warnings.add(warning);
    }

    @Override
    public List<OXException> getWarnings() {
        return warnings;
    }

    /**
     * Sets the file id
     * 
     * @param fileId the file id to set
     */
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    @Override
    public String getFileId() {
        return fileId;
    }
}
