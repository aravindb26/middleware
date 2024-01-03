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

package com.openexchange.drive.client.windows.rest.servlet;

import static com.openexchange.drive.client.windows.rest.service.internal.Utils.buildRequestContext;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpStatus;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.drive.client.windows.rest.DriveClientProperty;
import com.openexchange.drive.client.windows.rest.osgi.Services;
import com.openexchange.drive.client.windows.rest.service.DriveClientWindowsUpdateService;
import com.openexchange.drive.client.windows.rest.service.internal.DriveClientWindowsExceptionCodes;
import com.openexchange.drive.client.windows.rest.service.internal.DriveClientWindowsUpdateServiceImpl;
import com.openexchange.drive.client.windows.rest.service.internal.Utils;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionStrings;
import com.openexchange.framework.request.RequestContextHolder;
import com.openexchange.java.Streams;
import com.openexchange.login.Interface;
import com.openexchange.templating.OXTemplate;
import com.openexchange.templating.TemplateService;
import com.openexchange.tools.servlet.http.Tools;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.tools.webdav.OXServlet;

/**
 *
 * {@link UpdatesXMLServlet} is a servlet which provides update informations about a drive client
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @author <a href="mailto:ralf.wurm@open-xchange.com">Ralf Wurm</a>
 * @since v7.8.1
 */
public class UpdatesXMLServlet extends OXServlet {

    private static final long serialVersionUID = -7945036709270719526L;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(UpdatesXMLServlet.class);

    private final TemplateService templateService;

    private final DriveClientWindowsUpdateServiceImpl updateService;

    private static final String HEAD = "<Products>";
    private static final String TAIL = "</Products>";

    /**
     * Initializes a new {@link UpdatesXMLServlet}.
     *
     * @param templateService The template service to use
     * @param updateService The update service to use
     */
    public UpdatesXMLServlet(TemplateService templateService, DriveClientWindowsUpdateService updateService) {
        super();
        this.templateService = templateService;
        this.updateService = (DriveClientWindowsUpdateServiceImpl) updateService;
    }

    @Override
    protected Interface getInterface() {
        return Interface.DRIVE_UPDATER;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        PrintWriter writer = null;
        try {
            LeanConfigurationService leanConfigServie = Services.getServiceSafe(LeanConfigurationService.class);
            ServerSession session = getServerSession(req);

            if (!Utils.hasCapability(session, updateService.getRequiredCapabilities())) {
                LOG.debug("Missing capabilities. Permission {} is required!", updateService.getRequiredCapabilities());
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.setContentType("text/plain");
                return;
            }

            RequestContextHolder.set(buildRequestContext(session, req));
            try {
                OXTemplate productsTemplate = templateService.loadTemplate(leanConfigServie.getProperty(DriveClientProperty.TEMPLATING_UPDATER_FILE));
                Map<String, Object> map = null;
                try {
                    map = updateService.getTemplateValues(session);
                } catch (NullPointerException e) {
                    LOG.error("Branding properties imcomplete!", e);
                    throw DriveClientWindowsExceptionCodes.MISSING_PROPERTIES.create();
                }
                Tools.disableCaching(resp);
                writer = resp.getWriter();
                writeHead(writer);
                productsTemplate.process(map, writer);
                writeTail(writer);
                writer.flush();
            } finally {
                RequestContextHolder.reset();
            }
        } catch (OXException e) {
            LOG.error(e.getMessage(), e);
            if (DriveClientWindowsExceptionCodes.SERVICE_REQUEST_FAILED.equals(e)) {
                resp.sendError(HttpStatus.SC_GATEWAY_TIMEOUT, DriveClientWindowsExceptionCodes.SERVICE_REQUEST_FAILED.getDisplayMessage());
            } else if (DriveClientWindowsExceptionCodes.REMOTE_CLIENT_ERROR.equals(e)) {
                resp.sendError(HttpStatus.SC_BAD_REQUEST, DriveClientWindowsExceptionCodes.REMOTE_CLIENT_ERROR.getDisplayMessage());
            } else if (DriveClientWindowsExceptionCodes.REMOTE_SERVER_ERROR.equals(e)) {
                resp.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR, DriveClientWindowsExceptionCodes.REMOTE_SERVER_ERROR.getDisplayMessage());
            } else if (DriveClientWindowsExceptionCodes.MISSING_RESOURCE.equals(e)) {
                resp.sendError(HttpStatus.SC_NOT_FOUND, DriveClientWindowsExceptionCodes.MISSING_RESOURCE.getDisplayMessage());
            } else {
                resp.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR, OXExceptionStrings.MESSAGE);
            }
            resp.setContentType("text/html");
        } finally {
            Streams.close(writer);
        }
    }

    @Override
    protected boolean useCookies() {
        return false;
    }

    private static void writeHead(PrintWriter writer) {
        writer.println(HEAD);
    }

    private static void writeTail(PrintWriter writer) {
        writer.println(TAIL);
    }

    private static ServerSession getServerSession(final HttpServletRequest req) throws OXException {
        return new ServerSessionAdapter(getSession(req));
    }

    @Override
    protected void decrementRequests() {
        // Not used
    }

    @Override
    protected void incrementRequests() {
        // Not used
    }
}
