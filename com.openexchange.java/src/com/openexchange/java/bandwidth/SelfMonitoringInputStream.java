/*
 *
 *    OPEN-XCHANGE legal information
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    In some countries OX, OX Open-Xchange, open xchange and OXtender
 *    as well as the corresponding Logos OX Open-Xchange and OX are registered
 *    trademarks of the OX Software GmbH group of companies.
 *    The use of the Logos is not covered by the GNU General Public License.
 *    Instead, you are allowed to use these Logos according to the terms and
 *    conditions of the Creative Commons License, Version 2.5, Attribution,
 *    Non-commercial, ShareAlike, and the interpretation of the term
 *    Non-commercial applicable to the aforementioned license is published
 *    on the web site http://www.open-xchange.com/EN/legal/index.html.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original and derivative code shall remain
 *    under the copyright of the copyright holder(s) and/or original author(s)per
 *    the Attribution and Assignment Agreement that can be located at
 *    http://www.open-xchange.com/EN/developer/. The contributing author shall be
 *    given Attribution for the derivative code and a license granting use.
 *
 *     Copyright (C) 2016-2020 OX Software GmbH
 *     Mail: info@open-xchange.com
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License, Version 2 as published
 *     by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *     or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *     for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc., 59
 *     Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package com.openexchange.java.bandwidth;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * {@link SelfMonitoringInputStream}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.6.3
 */
public class SelfMonitoringInputStream extends FilterInputStream {

    private final DataMonitor monitor;
    private final ClosedHandler closedHandler;
    
    /**
     * Initializes a new {@link SelfMonitoringInputStream}.
     *
     * @param in The input stream to monitor
     */
    public SelfMonitoringInputStream(InputStream in) {
        this(in, null);
    }

    /**
     * Initializes a new {@link SelfMonitoringInputStream}.
     *
     * @param in The input stream to monitor
     * @param closedHandler Invoked when this self-monitoring input stream gets closed or <code>null</code>
     */
    public SelfMonitoringInputStream(InputStream in, ClosedHandler closedHandler) {
        super(in);
        this.closedHandler = closedHandler;
        monitor = new DataMonitor();
    }

    @Override
    public int read() throws IOException {
        long start = System.currentTimeMillis();
        int b = super.read();
        monitor.addSample(1, start, System.currentTimeMillis());
        return b;
    }

    @Override
    public int read(byte data[]) throws IOException {
        long start = System.currentTimeMillis();
        int cnt = super.read(data);
        monitor.addSample(cnt, start, System.currentTimeMillis());
        return cnt;
    }

    @Override
    public int read(byte data[], int off, int len) throws IOException {
        long start = System.currentTimeMillis();
        int cnt = super.read(data, off, len);
        monitor.addSample(cnt, start, System.currentTimeMillis());
        return cnt;
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        if (closedHandler != null) {
            closedHandler.onClosed(this);
        }
    }

    /**
     * Gets the average rate over all samples.
     *
     * @return The average rate
     */
    public float averageRate() {
        return monitor.getAverageRate();
    }

    /**
     * Gets the rate of the last sample
     *
     * @return The rate
     */
    public float lastRate() {
        return monitor.getLastRate();
    }

}
