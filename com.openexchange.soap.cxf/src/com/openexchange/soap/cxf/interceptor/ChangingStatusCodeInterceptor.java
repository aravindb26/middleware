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


package com.openexchange.soap.cxf.interceptor;

import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import com.openexchange.servlet.HttpStatusCode;


/**
 * {@link ChangingStatusCodeInterceptor}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public class ChangingStatusCodeInterceptor extends AbstractPhaseInterceptor<Message> {

    /**
     * Initializes a new {@link ChangingStatusCodeInterceptor}.
     */
    public ChangingStatusCodeInterceptor() {
        super(Phase.PRE_MARSHAL);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        if (MessageUtils.isOutbound(message)) {
            if (MessageUtils.isFault(message)) {
                handleOutboundFault(message);
            }
        }
    }

    @Override
    public void handleFault(Message message) {
        if (MessageUtils.isOutbound(message)) {
            handleOutboundFault(message);
        }
    }

    private static final QName QNAME_CLIENT = new QName("Client");
    private static final int SC_BAD_REQUEST = HttpStatusCode.BAD_REQUEST_400.getStatusCode();

    private void handleOutboundFault(Message message) {
        Exception exception = message.getContent(Exception.class);
        if (!(exception instanceof Fault)) {
            return;
        }

        Fault f = (Fault) exception;
        if (isClientFault(f.getCause())) {
            // Align status code since CXF uses "500 Internal Server Error" as default for all SOAP faults
            f.setFaultCode(QNAME_CLIENT);
            f.setStatusCode(SC_BAD_REQUEST);
            Exchange exchange = message.getExchange();
            Message outMessage = Optional.ofNullable(exchange.getOutMessage()).orElse(exchange.getOutFaultMessage());
            HttpServletResponse httpResponse = (HttpServletResponse) outMessage.get(AbstractHTTPDestination.HTTP_RESPONSE);
            if (httpResponse != null) {
                httpResponse.setStatus(SC_BAD_REQUEST);
            }
        }
    }

    private static final String[] PREFIXES = new String[] {".NoSuch", ".Invalid", ".Context", ".Database", ".User"};

    private static boolean isClientFault(Throwable possibleClientFault) {
        if (possibleClientFault == null) {
            return false;
        }

        String className = possibleClientFault.getClass().getName();
        if (className.startsWith("com.openexchange.") == false) {
            return false;
        }

        for (String prefix : PREFIXES) {
            if (className.indexOf(prefix) >= 0) {
                return true;
            }
        }
        return false;
    }

}
