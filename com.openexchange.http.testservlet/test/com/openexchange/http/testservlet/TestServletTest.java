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

package com.openexchange.http.testservlet;

import static com.openexchange.java.Autoboxing.B;
import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

/**
 * Unit tests for {@link TestServlet}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since 7.4
 */
public class TestServletTest {

    /**
     * The class to test
     */
    private TestServlet testServlet = null;

    /**
     * Mock of {@HttpServletRequest}
     */
    private HttpServletRequest httpServletRequest = null;

    /**
     * Mock of {@HttpServletRequest}
     */
    private HttpServletResponse httpServletResponse = null;

    /**
     * Header and attribute name parameters
     */
    private Enumeration<String> parameters;

    /**
     */
    @Before
    public void setUp() {
        // MEMBERS
        this.testServlet = new TestServlet();
        this.httpServletRequest = PowerMockito.mock(HttpServletRequest.class);
        this.httpServletResponse = PowerMockito.mock(HttpServletResponse.class);
        this.parameters = PowerMockito.mock(Enumeration.class);

        // MEMBER BEHAVIOUR
        PowerMockito.when(B(this.parameters.hasMoreElements())).thenReturn(B(false));
        PowerMockito.when(this.httpServletRequest.getHeaderNames()).thenReturn(this.parameters);
        try {
            ServletOutputStream servletOutputStream = PowerMockito.mock(ServletOutputStream.class);
            PowerMockito.when(this.httpServletResponse.getOutputStream()).thenReturn(servletOutputStream);
        } catch (IOException ioException) {
            // will not happen
        }

    }

    @Test(timeout = 2000)
    public void testDoGet_ThreadSleepNotExecuted_ReturnedWithin2000ms() throws ServletException, IOException {
        this.testServlet.doGet(this.httpServletRequest, this.httpServletResponse);
    }

    @Test(timeout = 2000)
    public void testDoPut_ThreadSleepNotExecuted_ReturnedWithin2000ms() throws ServletException, IOException {
        this.testServlet.doPut(this.httpServletRequest, this.httpServletResponse);
    }
}
