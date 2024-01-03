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
package com.openexchange.request.analyzer.impl;

import static com.openexchange.java.Autoboxing.L;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.RankingAwareNearRegistryServiceTracker;
import com.openexchange.request.analyzer.AnalyzeResult;
import com.openexchange.request.analyzer.RequestAnalyzer;
import com.openexchange.request.analyzer.RequestAnalyzerService;
import com.openexchange.request.analyzer.RequestData;

/**
 * {@link RequestAnalyzerServiceImpl} is an implementation of the {@link RequestAnalyzerService} which uses a list of OSGi-wise tracked
 * {@link RequestAnalyzer} instances.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class RequestAnalyzerServiceImpl extends RankingAwareNearRegistryServiceTracker<RequestAnalyzer> implements RequestAnalyzerService {

    private static final Logger LOG = LoggerFactory.getLogger(RequestAnalyzerService.class);

    /**
     * Initializes a new {@link RequestAnalyzerServiceImpl}.
     *
     * @param context The bundle context
     */
    public RequestAnalyzerServiceImpl(BundleContext context) {
        super(context, RequestAnalyzer.class);
    }

    @Override
    public AnalyzeResult analyzeRequest(RequestData request) throws OXException {
        boolean traceEnabled = LOG.isTraceEnabled(); // Remember once per invocation
        if (traceEnabled) {
            long start = System.nanoTime();
            try {
                return analyzeRequestInternal(request, true);
            } catch (OXException e) {
                LOG.error("Failed to analyze request", e);
                throw e;
            } finally {
                logTime(System.nanoTime() - start, request.getUrl());
            }
        }

        // Without TRACE logging...
        try {
            return analyzeRequestInternal(request, false);
        } catch (OXException e) {
            LOG.error("Failed to analyze request", e);
            throw e;
        }
    }

    /**
     * Analyzes the request data by iterating over all registered {@link RequestAnalyzer}s
     *
     * @param request The request to analyze
     * @param traceEnabled Whether TRACE logging is enabled for this invocation
     * @return The {@link AnalyzeResult}
     * @throws OXException
     */
    private AnalyzeResult analyzeRequestInternal(RequestData request, boolean traceEnabled) throws OXException {
        if (traceEnabled) {
            for (RequestAnalyzer analyzer : getServiceList()) {
                long start = System.nanoTime();
                Optional<AnalyzeResult> optResult = analyzer.analyze(request);
                logAnalyzerTime(System.nanoTime() - start, analyzer.getClass().getSimpleName(), request.getUrl(), optResult);
                if (optResult.isPresent()) {
                    return optResult.get();
                }
            }
        } else {
            // Without TRACE logging...
            for (RequestAnalyzer analyzer : getServiceList()) {
                Optional<AnalyzeResult> optResult = analyzer.analyze(request);
                if (optResult.isPresent()) {
                    return optResult.get();
                }
            }
        }
        return AnalyzeResult.UNKNOWN;
    }

    // ------------------- logging helpers -----------------

    /**
     * Logs the overall request analysis duration.
     *
     * @param durationNanos The time elapsed during analysis in nanoseconds
     * @param url The URL of the analyzed request
     */
    private static void logTime(long durationNanos, String url) {
        LOG.trace("Request analysis took {} ms to complete for URL '{}'", L(TimeUnit.NANOSECONDS.toMillis(durationNanos)), url);
    }

    /**
     * Logs the duration of an analyzer.
     *
     * @param durationNanos The time elapsed during analysis in nanoseconds
     * @param name The name of the analyzer
     * @param uri The analyzed URL
     * @param optResult The optional intermediate result
     */
    private static void logAnalyzerTime(long durationNanos, String name, String url, Optional<AnalyzeResult> optResult) {
        String result = optResult.isPresent() ? new StringBuilder("result ").append(optResult.get()).toString() : "no result";
        LOG.trace("'{}' analyzer took {} ms to complete for URL '{}' with {}", name, L(TimeUnit.NANOSECONDS.toMillis(durationNanos)), url, result);
    }

}
