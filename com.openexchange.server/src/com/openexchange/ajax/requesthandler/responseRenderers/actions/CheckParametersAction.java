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

package com.openexchange.ajax.requesthandler.responseRenderers.actions;

import static com.openexchange.ajax.requesthandler.responseRenderers.FileResponseRendererProperties.ALLOW_CONTENT_DISPOSITION_INLINE;
import static com.openexchange.java.Strings.isEmpty;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.AJAXUtility;
import com.openexchange.ajax.requesthandler.responseRenderers.FileResponseRenderer;
import com.openexchange.ajax.requesthandler.responseRenderers.FileResponseRenderer.FileResponseRendererActionException;
import com.openexchange.ajax.requesthandler.responseRenderers.FileResponseRendererProperties;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.java.Strings;
import com.openexchange.server.services.ServerServiceRegistry;

/**
 * {@link CheckParametersAction} Check certain parameters
 *
 * Influence the following IDataWrapper attributes:
 * <ul>
 * <li>delivery
 * <li>contentType
 * <li>contentTypeByParameter
 * <li>contentDisposition
 * </ul>
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.0
 */
public class CheckParametersAction implements IFileResponseRendererAction {

    private static final String DELIVERY = AJAXServlet.PARAMETER_DELIVERY;
    private final String allowContentDispositionInlineProperty = FileResponseRendererProperties.ALLOW_CONTENT_DISPOSITION_INLINE.getFQPropertyName();

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(FileResponseRenderer.class);


    @Override
    public void call(IDataWrapper data) throws FileResponseRendererActionException {
        final boolean allowContentDispositionInline = isContentDispositionInlineAllowed();

        // Check certain parameters
        setDelivery(data, allowContentDispositionInline);
        data.setContentType(AJAXUtility.encodeUrl(data.getRequest().getParameter(IDataWrapper.PARAMETER_CONTENT_TYPE), true));
        data.setContentTypeByParameter(Boolean.FALSE);
        if (Strings.isEmpty(data.getContentType())) {
            if (IDataWrapper.DOWNLOAD.equalsIgnoreCase(data.getDelivery())) {
                data.setContentType(IDataWrapper.SAVE_AS_TYPE);
            } else {
                data.setContentType(data.getFileContentType());
            }
        } else {
            data.setContentTypeByParameter(Boolean.TRUE);
        }
        data.setContentType(unquote(data.getContentType()));
        // Delivery is set to "view", but Content-Type is indicated as application/octet-stream
        if (IDataWrapper.VIEW.equalsIgnoreCase(data.getDelivery()) && (null != data.getContentType() && data.getContentType().startsWith(IDataWrapper.SAVE_AS_TYPE))) {
            data.setContentType(FileResponseRenderer.getContentTypeByFileName(data.getFileName()));
            if (null == data.getContentType()) {
                // Not known
                data.setContentType(IDataWrapper.SAVE_AS_TYPE);
            }
        }
        setContentDisposition(data, allowContentDispositionInline);

        if (null == data.getFile()) {
            // Quit with 404
            throw new FileResponseRenderer.FileResponseRendererActionException(HttpServletResponse.SC_NOT_FOUND, "File not found.");
        }
    }

    /**
     * Checks if a content disposition of inline is allowed or not
     *
     * @return <code>true</code> if inline is allowed, <code>false</code> otherwise
     */
    private boolean isContentDispositionInlineAllowed() {
        LeanConfigurationService lcs = ServerServiceRegistry.getServize(LeanConfigurationService.class);
        if (lcs == null) {
            LOG.debug("The LeanConfigurationService could not be found, so the value for {} was set to false.", allowContentDispositionInlineProperty);
            return false;
        }
        return lcs.getBooleanProperty(FileResponseRendererProperties.ALLOW_CONTENT_DISPOSITION_INLINE);
    }

    /**
     * Sets the delivery type
     *
     * @param data The data to set the type to
     * @param allowContentDispositionInline Whether inline is allowed as a content disposition
     */
    private void setDelivery(IDataWrapper data, boolean allowContentDispositionInline) {
        String deliveryParameter = AJAXUtility.sanitizeParam(data.getRequest().getParameter(DELIVERY));
        if (IDataWrapper.VIEW.equalsIgnoreCase(deliveryParameter) && false == allowContentDispositionInline) {
            LOG.debug("Because {} is set to false, the parameter 'delivery' has been ignored", ALLOW_CONTENT_DISPOSITION_INLINE.getFQPropertyName());
            deliveryParameter = null;
        }
        data.setDelivery(deliveryParameter);
        if (data.getDelivery() == null) {
            data.setDelivery(data.getFile().getDelivery());
        }
    }

    /**
     * Sets the content disposition
     *
     * @param data The data to set the disposition to
     * @param allowContentDispositionInline Whether inline is allowed as a content disposition
     */
    private void setContentDisposition(IDataWrapper data, boolean allowContentDispositionInline) {
        String contentDispositionParameter = AJAXUtility.encodeUrl(data.getRequest().getParameter(IDataWrapper.PARAMETER_CONTENT_DISPOSITION));
        if (Strings.isNotEmpty(contentDispositionParameter) && contentDispositionParameter.trim().toLowerCase().startsWith("inline") && false == allowContentDispositionInline) {
            LOG.debug("Because {} is set to false, the parameter 'content_disposition' has been ignored.", ALLOW_CONTENT_DISPOSITION_INLINE.getFQPropertyName());
            contentDispositionParameter = null;
        }
        data.setContentDisposition(contentDispositionParameter);
        if (Strings.isEmpty(data.getContentDisposition())) {
            if (IDataWrapper.VIEW.equalsIgnoreCase(data.getDelivery())) {
                data.setContentDisposition("inline");
            } else if (IDataWrapper.DOWNLOAD.equalsIgnoreCase(data.getDelivery())) {
                data.setContentDisposition("attachment");
            } else {
                data.setContentDisposition(data.getFile().getDisposition());
            }
        }
        data.setContentDisposition(unquote(data.getContentDisposition()));

    }

    /**
     * Removes single or double quotes from a string if its quoted.
     *
     * @param s The value to be unquoted
     * @return The unquoted value or <code>null</code>
     */
    private String unquote(final String s) {
        if (!isEmpty(s) && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

}
