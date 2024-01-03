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

import java.util.ArrayList;
import java.util.List;

/**
 * {@link DataMonitor}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.6.3
 */
public class DataMonitor {

    private final List<DataSample> samples;
    private final long epoch;

    /**
     * Initializes a new {@link DataMonitor}.
     */
    public DataMonitor() {
        super();
        samples = new ArrayList<DataSample>();
        epoch = System.currentTimeMillis();
    }

    /**
     * Adds a sample with a start and finish time.
     *
     * @param byteCount The byte count
     * @param ts The start time
     * @param tf The end time
     */
    public void addSample(long byteCount, long ts, long tf) {
        samples.add(new DataSample(byteCount, ts, tf));
    }

    /**
     * Gets the data rate of a given sample.
     *
     * @param sidx The index position
     * @return The rate
     */
    public float getRateFor(int sidx) {
        float rate = 0.0f;
        int scnt = samples.size();
        if (scnt > sidx && sidx >= 0) {
            DataSample s = samples.get(sidx);
            long start = s.start;
            long end = s.end;
            if (start < 0 && sidx >= 1) {
                DataSample prev = samples.get(sidx - 1);
                start = prev.end;
            }
            if (start >= 0 && end >= 0) {
                long msec = end - start;
                rate = 1000 * (float) s.byteCount / msec;
            }
        }
        return rate;
    }

    /**
     * Gets the rate of the last sample
     *
     * @return The rate
     */
    public float getLastRate() {
        int scnt = samples.size();
        return getRateFor(scnt - 1);
    }

    /**
     * Gets the average rate over all samples.
     *
     * @return The average rate
     */
    public float getAverageRate() {
        long msCount = 0;
        long byteCount = 0;
        long start;
        long finish;

        int scnt = samples.size();
        for (int i = 0; i < scnt; i++) {
            DataSample ds = samples.get(i);

            if (ds.start >= 0) {
                start = ds.start;
            } else if (i > 0) {
                DataSample prev = samples.get(i - 1);
                start = prev.end;
            } else {
                start = epoch;
            }

            if (ds.end >= 0) {
                finish = ds.end;
            } else if (i < scnt - 1) {
                DataSample next = samples.get(i + 1);
                finish = next.start;
            } else {
                finish = System.currentTimeMillis();
            }

            // Only include this sample if we could figure out a start and finish time for it.
            if (start >= 0 && finish >= 0) {
                byteCount += ds.byteCount;
                msCount += finish - start;
            }
        }

        float rate = -1;
        if (msCount > 0) {
            rate = 1000 * (float) byteCount / msCount;
        }
        return rate;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class DataSample {

        final long byteCount;
        final long start;
        final long end;

        DataSample(long bc, long ts, long tf) {
            super();
            byteCount = bc;
            start = ts;
            end = tf;
        }
    }

}
