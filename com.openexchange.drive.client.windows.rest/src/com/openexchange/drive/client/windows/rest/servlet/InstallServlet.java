package com.openexchange.drive.client.windows.rest.servlet;

import static com.openexchange.drive.client.windows.rest.service.internal.Utils.buildRequestContext;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.ajax.SessionServlet;
import com.openexchange.drive.client.windows.rest.service.DriveClientWindowsUpdateService;
import com.openexchange.drive.client.windows.rest.service.internal.DriveClientWindowsUpdateServiceImpl;
import com.openexchange.drive.client.windows.rest.service.internal.Utils;
import com.openexchange.exception.OXException;
import com.openexchange.framework.request.RequestContextHolder;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link InstallServlet} is a servlet to initially download the drive client.
 *
 * @author <a href="mailto:ralf.wurm@open-xchange.com">Ralf Wurm</a>
 * @since v8.0.0
 */
public class InstallServlet extends SessionServlet {

    private static final long serialVersionUID = -215119659899164992L;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(InstallServlet.class);

    private final DriveClientWindowsUpdateServiceImpl updateService;

    /**
     * Initializes a new {@link InstallServlet}.
     *
     * @param updateService The update service to use
     */
    public InstallServlet(DriveClientWindowsUpdateService updateService) {
        super();
        this.updateService = (DriveClientWindowsUpdateServiceImpl) updateService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            ServerSession session = getSessionObject(req);
            if (null == session) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (Utils.hasCapability(session, updateService.getRequiredCapabilities())) {
                RequestContextHolder.set(buildRequestContext(session, req));
                try {
                    resp.sendRedirect(updateService.getInstallerDownloadUrl(session));
                } finally {
                    RequestContextHolder.reset();
                }
            } else {
                LOG.debug("Missing capabilities. Permission {} is required!", updateService.getRequiredCapabilities());
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.setContentType("text/plain");
                return;
            }
        } catch (OXException e) {
            LOG.error("", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
