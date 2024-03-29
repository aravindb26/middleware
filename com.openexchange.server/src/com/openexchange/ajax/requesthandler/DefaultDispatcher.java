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

package com.openexchange.ajax.requesthandler;

import static com.openexchange.ajax.AJAXServlet.PARAMETER_ALLOW_ENQUEUE;
import static com.openexchange.ajax.requesthandler.AJAXRequestDataTools.parseBoolParameter;
import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.exception.ExceptionUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.metrics.HttpMetricUtil;
import com.openexchange.ajax.requesthandler.AJAXRequestResult.ResultType;
import com.openexchange.ajax.requesthandler.jobqueue.DefaultJob;
import com.openexchange.ajax.requesthandler.jobqueue.EnqueuedException;
import com.openexchange.ajax.requesthandler.jobqueue.JobKey;
import com.openexchange.ajax.requesthandler.jobqueue.JobQueueExceptionCodes;
import com.openexchange.ajax.requesthandler.jobqueue.JobQueueService;
import com.openexchange.continuation.Continuation;
import com.openexchange.continuation.ContinuationException;
import com.openexchange.continuation.ContinuationExceptionCodes;
import com.openexchange.continuation.ContinuationRegistryService;
import com.openexchange.continuation.ContinuationResponse;
import com.openexchange.exception.OXException;
import com.openexchange.framework.request.DefaultRequestContext;
import com.openexchange.framework.request.RequestContext;
import com.openexchange.framework.request.RequestContextHolder;
import com.openexchange.groupware.notify.hostname.HostData;
import com.openexchange.java.IReference;
import com.openexchange.java.ImmutableReference;
import com.openexchange.java.util.UUIDs;
import com.openexchange.log.LogProperties;
import com.openexchange.net.ssl.config.UserAwareSSLConfigurationService;
import com.openexchange.net.ssl.exception.SSLExceptionCode;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.servlet.http.Tools;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link DefaultDispatcher} - The default {@link Dispatcher dispatcher} implementation.
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class DefaultDispatcher implements Dispatcher {

    private static final String UNKNOWN_METRIC_RECORD = "UNKNOWN";

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultDispatcher.class);

    private static final String OK_RECORD_STATUS = "OK";

    private final DispatcherListenerRegistry listenerRegistry;

    private final Cache<ModuleAndAction, IReference<DispatcherNotes>> dispatcherNotesCache;
    private final ConcurrentMap<String, AJAXActionServiceFactory> actionFactories;
    private final Queue<AJAXActionCustomizerFactory> customizerFactories;
    private final Queue<AJAXActionAnnotationProcessor> annotationProcessors;

    /**
     * Initializes a new {@link DefaultDispatcher}.
     */
    public DefaultDispatcher() {
        this(null);
    }

    /**
     * Initializes a new {@link DefaultDispatcher}.
     */
    public DefaultDispatcher(DispatcherListenerRegistry listenerRegistry) {
        super();
        dispatcherNotesCache = CacheBuilder.newBuilder().initialCapacity(128).expireAfterAccess(30, TimeUnit.MINUTES).build();
        actionFactories = new ConcurrentHashMap<>(64, 0.9f, 1);
        customizerFactories = new ConcurrentLinkedQueue<>();
        annotationProcessors = new ConcurrentLinkedQueue<>();

        this.listenerRegistry = null == listenerRegistry ? DispatcherListenerRegistry.NOOP_REGISTRY : listenerRegistry;
    }

    @Override
    public AJAXState begin() throws OXException {
        return new AJAXState();
    }

    @Override
    public void end(final AJAXState state) {
        if (null != state) {
            state.close();
        }
    }

    @Override
    public boolean handles(final String module) {
        return actionFactories.containsKey(module);
    }

    @Override
    public AJAXRequestResult perform(final AJAXRequestData requestData, final AJAXState state, final ServerSession session) throws OXException {
        final long startTime = System.currentTimeMillis();
        String moduleToRecord = UNKNOWN_METRIC_RECORD;
        String actionToRecord = UNKNOWN_METRIC_RECORD;

        if (null == session) {
            recordRequest(moduleToRecord, actionToRecord, AjaxExceptionCodes.MISSING_PARAMETER.getCategory().toString(), System.currentTimeMillis() - startTime);
            throw AjaxExceptionCodes.MISSING_PARAMETER.create(AJAXServlet.PARAMETER_SESSION);
        }

        addLogProperties(requestData, false);
        List<AJAXActionCustomizer> customizers = determineCustomizers(requestData, session);
        try {
            // Customize request data
            AJAXRequestData modifiedRequestData = customizeRequest(requestData, customizers, session);

            // Set request context
            RequestContext requestContext = buildRequestContext(modifiedRequestData);
            RequestContextHolder.set(requestContext);

            // Determine action factory and yield an action executing the request
            FactoryAndModule factoryAndModule = optFactoryAndModule(modifiedRequestData.getModule()).orElseThrow(() -> AjaxExceptionCodes.UNKNOWN_MODULE.create(modifiedRequestData.getModule()));
            AJAXActionServiceFactory factory = factoryAndModule.factory;
            moduleToRecord = factoryAndModule.module;

            AJAXActionService action = factory.createActionService(modifiedRequestData.getAction());
            if (action == null) {
                throw AjaxExceptionCodes.UNKNOWN_ACTION_IN_MODULE.create(modifiedRequestData.getAction(), modifiedRequestData.getModule());
            }
            actionToRecord = modifiedRequestData.getAction();

            // Load request body if stream is not preferred
            if (false == preferStream(modifiedRequestData.getModule(), modifiedRequestData.getAction(), action)) {
                AJAXRequestDataTools.loadRequestBody(modifiedRequestData);
            }

            // Validate request headers for caching
            {
                AJAXRequestResult etagResult = checkResultNotModified(action, modifiedRequestData, session);
                if (etagResult != null) {
                    return etagResult;
                }
            }

            // Validate request headers for resume
            {
                AJAXRequestResult failedResult = checkRequestPreconditions(action, modifiedRequestData, session);
                if (failedResult != null) {
                    return failedResult;
                }
            }

            // Check for action annotations
            for (AJAXActionAnnotationProcessor annotationProcessor : annotationProcessors) {
                if (annotationProcessor.handles(action)) {
                    annotationProcessor.process(action, modifiedRequestData, session);
                }
            }

            if (parseBoolParameter(PARAMETER_ALLOW_ENQUEUE, modifiedRequestData)) {
                // Check if action service is enqueue-able
                EnqueuableAJAXActionService.Result enqueueableResult = enqueueable(modifiedRequestData.getModule(), modifiedRequestData.getAction(), action, modifiedRequestData, session);
                if (enqueueableResult.isEnqueueable()) {
                    // Enqueue as dispatcher job and watch it
                    JobQueueService jobQueue = ServerServiceRegistry.getInstance().getService(JobQueueService.class);
                    if (null != jobQueue) {
                        // Check for already running job of that kind
                        JobKey optionalKey = enqueueableResult.getOptionalKey();
                        if (optionalKey != null) {
                            UUID contained = jobQueue.contains(optionalKey);
                            if (null != contained) {
                                throw JobQueueExceptionCodes.ALREADY_RUNNING.create(UUIDs.getUnformattedString(contained), I(session.getUserId()), I(session.getContextId()));
                            }
                        }

                        // Prepare for being submitted to job queue
                        {
                            EnqueuableAJAXActionService optionalEnqueuableAction = enqueueableResult.getEnqueuableAction();
                            if (optionalEnqueuableAction != null) {
                                optionalEnqueuableAction.prepareForEnqueue(modifiedRequestData, session);
                            }
                        }

                        // Build job
                        DefaultJob.Builder job = DefaultJob.builder(true, this)
                            .setAction(action)
                            .setCustomizers(customizers)
                            .setFactory(factory)
                            .setRequestContext(requestContext)
                            .setRequestData(modifiedRequestData)
                            .setSession(session)
                            .setState(state)
                            .setKey(optionalKey);

                        // Enqueue (if not computed in time)
                        try {
                            long maxRequestAgeMillis = jobQueue.getMaxRequestAgeMillis();
                            final AJAXRequestResult result = jobQueue.enqueueAndWait(job.build(), maxRequestAgeMillis, TimeUnit.MILLISECONDS).get(true);
                            recordRequest(moduleToRecord, actionToRecord, OK_RECORD_STATUS, System.currentTimeMillis() - startTime);
                            return result;
                        } catch (InterruptedException e) {
                            // Keep interrupted state
                            Thread.currentThread().interrupt();
                            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, "Interrupted");
                        } catch (EnqueuedException e) {
                            // Result not computed in time
                            LOG.debug("Action \"{}\" of module \"{}\" could not be executed in time for user {} in context {}.", requestData.getAction(), requestData.getModule(), I(session.getUserId()), I(session.getContextId()), e);
                            recordRequest(moduleToRecord, actionToRecord, OK_RECORD_STATUS, System.currentTimeMillis() - startTime);
                            return new AJAXRequestResult(e.getJobInfo(), "enqueued");
                        }
                    }
                }
            }

            AJAXRequestResult result = doPerform(action, factory, modifiedRequestData, state, customizers, session);
            recordRequest(moduleToRecord, actionToRecord, OK_RECORD_STATUS, System.currentTimeMillis() - startTime);
            return result;
        } catch (OXException e) {
            for (AJAXActionCustomizer customizer : customizers) {
                if (customizer instanceof AJAXExceptionHandler) {
                    try {
                        ((AJAXExceptionHandler) customizer).exceptionOccurred(requestData, e, session);
                    } catch (Exception x) {
                        // Discard. Not our problem, we need to get on with this!
                    }
                }
            }
            if (SSLExceptionCode.PREFIX.equals(e.getPrefix())) {
                UserAwareSSLConfigurationService userAwareSSLConfigurationService = ServerServiceRegistry.getInstance().getService(UserAwareSSLConfigurationService.class);
                if (null != userAwareSSLConfigurationService) {
                    if (userAwareSSLConfigurationService.isAllowedToDefineTrustLevel(session.getUserId(), session.getContextId())) {
                        recordRequest(moduleToRecord, actionToRecord, SSLExceptionCode.UNTRUSTED_CERT_USER_CONFIG.getCategory().toString(), System.currentTimeMillis() - startTime);
                        throw SSLExceptionCode.UNTRUSTED_CERT_USER_CONFIG.create(e.getDisplayArgs());
                    }
                }
            }
            // MW-445: Try to unpack and throw the OXException
            if (e.getCause() instanceof SSLException) {
                Throwable t = ExceptionUtils.getRootCause(e);
                if (t instanceof OXException oxe) {
                    recordRequest(moduleToRecord, actionToRecord, oxe.getCategory().toString(), System.currentTimeMillis() - startTime);
                    throw oxe;
                }
            }
            recordRequest(moduleToRecord, actionToRecord, e.getCategory().toString(), System.currentTimeMillis() - startTime);
            throw e;
        } catch (RuntimeException e) {
            if ("org.mozilla.javascript.WrappedException".equals(e.getClass().getName())) {
                // Handle special Rhino wrapper error
                Throwable wrapped = e.getCause();
                if (wrapped instanceof OXException) {
                    recordRequest(moduleToRecord, actionToRecord, ((OXException)wrapped).getCategory().toString(), System.currentTimeMillis() - startTime);
                    throw (OXException) wrapped;
                }
            }

            // Wrap unchecked exception
            addLogProperties(requestData, true);
            recordRequest(moduleToRecord, actionToRecord, AjaxExceptionCodes.UNEXPECTED_ERROR.getCategory().toString(), System.currentTimeMillis() - startTime);
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            RequestContextHolder.reset();
        }
    }

    /**
     * Checks if given action wants to read data form possibly available request body stream by itself.
     *
     * @param module The module identifier
     * @param action The action identifier
     * @param actionService The action service to perform
     * @return <code>true</code> if stream is preferred; otherwise <code>false</code>
     */
    private boolean preferStream(String module, String action, AJAXActionService actionService) {
        ModuleAndAction key = new ModuleAndAction(module, action);
        IReference<DispatcherNotes> actionMetadataReference = dispatcherNotesCache.getIfPresent(key);
        if (actionMetadataReference == null) {
            DispatcherNotes actionMetadata = getActionMetadata(actionService);
            actionMetadataReference = ImmutableReference.<DispatcherNotes> immutableReferenceFor(actionMetadata);
            dispatcherNotesCache.put(key, actionMetadataReference);
        }

        DispatcherNotes actionMetadata = actionMetadataReference.getValue();
        return actionMetadata == null ? false : actionMetadata.preferStream();
    }

    /**
     * Checks if given action is enqueue-able.
     *
     * @param module The module identifier
     * @param action The action identifier
     * @param actionService The action service to perform
     * @param requestData The AJAX request data
     * @param session The associated session
     * @return <code>true</code> if enqueue-able; otherwise <code>false</code>
     * @throws OXException If check fails
     */
    private EnqueuableAJAXActionService.Result enqueueable(String module, String action, AJAXActionService actionService, AJAXRequestData requestData, ServerSession session) throws OXException {
        // Check by action service instance
        if (actionService instanceof EnqueuableAJAXActionService enqueuableAction) {
            return enqueuableAction.isEnqueueable(requestData, session);
        }

        // Check by dispatcher annotation
        ModuleAndAction key = new ModuleAndAction(module, action);
        IReference<DispatcherNotes> actionMetadataReference = dispatcherNotesCache.getIfPresent(key);
        if (actionMetadataReference == null) {
            DispatcherNotes actionMetadata = getActionMetadata(actionService);
            actionMetadataReference = ImmutableReference.<DispatcherNotes> immutableReferenceFor(actionMetadata);
            dispatcherNotesCache.put(key, actionMetadataReference);
        }

        DispatcherNotes actionMetadata = actionMetadataReference.getValue();
        return EnqueuableAJAXActionService.resultFor(actionMetadata == null ? false : actionMetadata.enqueueable());
    }

    private static RequestContext buildRequestContext(AJAXRequestData requestData) throws OXException {
        HostData hostData = requestData.getHostData();
        if (hostData == null) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create("Host data was null. AJAX request data has not been initialized correctly!");
        }

        DefaultRequestContext context = new DefaultRequestContext();
        context.setHostData(hostData);
        context.setUserAgent(requestData.getUserAgent());
        context.setSession(requestData.getSession());
        return context;
    }

    /**
     * Determines all {@link AJAXActionCustomizer} instances that can potentially modify the request data.
     *
     * @param requestData The request data
     * @param session The session
     * @return A list of customizers meant to be called for the request object.
     */
    private List<AJAXActionCustomizer> determineCustomizers(AJAXRequestData requestData, ServerSession session) {
        /*
         * Create customizers
         */
        List<AJAXActionCustomizer> todo = new ArrayList<>(4);
        for (AJAXActionCustomizerFactory customizerFactory : customizerFactories) {
            AJAXActionCustomizer customizer = customizerFactory.createCustomizer(requestData, session);
            if (customizer != null) {
                todo.add(customizer);
            }
        }
        return todo;
    }

    /**
     * Customizes a request by calling {@link AJAXActionCustomizer#incoming(AJAXRequestData, ServerSession)} on every
     * passed customizer with the given request data. After this call returns, the list of customizers contains all
     * instances that need to be called after the requests action was performed.
     *
     * @param requestData The request data
     * @param customizers The customizers to call. Must be mutable.
     * @param session The session
     * @return The (potentially) modified request data
     * @throws OXException
     */
    private static AJAXRequestData customizeRequest(AJAXRequestData requestData, List<AJAXActionCustomizer> customizers, ServerSession session) throws OXException {
        /*
         * Iterate customizers for AJAXRequestData
         */
        AJAXRequestData modifiedRequestData = requestData;
        List<AJAXActionCustomizer> outgoing = new ArrayList<>(4);
        while (!customizers.isEmpty()) {
            final Iterator<AJAXActionCustomizer> iterator = customizers.iterator();
            while (iterator.hasNext()) {
                final AJAXActionCustomizer customizer = iterator.next();
                try {
                    final AJAXRequestData modified = customizer.incoming(modifiedRequestData, session);
                    if (modified != null) {
                        modifiedRequestData = modified;
                    }
                    outgoing.add(customizer);
                    iterator.remove();
                } catch (FlowControl.Later l) {
                    // Remains in list and is therefore retried
                }
            }
        }

        customizers.clear();
        customizers.addAll(outgoing);
        return modifiedRequestData;
    }

    /**
     * Checks if potential HTTP preconditions are fulfilled. Namely the following headers are checked:
     * <ul>
     * <li>If-Match</li>
     * <li>If-Unmodified-Since</li>
     * </ul>
     *
     * For every header that is part of the request and supported by the given action, the precondition is
     * checked.
     *
     * @param action The target action for the given request
     * @param requestData The request data
     * @param session The session
     * @return <code>null</code> if the request shall be processed normally. If a precondition fails, an
     *         according {@link AJAXRequestResult} with code {@link HttpServletResponse#SC_PRECONDITION_FAILED} is returned,
     *         which should be directly written out to the client.
     */
    private static AJAXRequestResult checkRequestPreconditions(AJAXActionService action, AJAXRequestData requestData, ServerSession session) throws OXException {
        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = requestData.getHeader("If-Match");
        if (ifMatch != null && (action instanceof ETagAwareAJAXActionService) && (("*".equals(ifMatch)) || ((ETagAwareAJAXActionService) action).checkETag(ifMatch, requestData, session))) {
            final AJAXRequestResult failedResult = new AJAXRequestResult();
            failedResult.setHttpStatusCode(HttpServletResponse.SC_PRECONDITION_FAILED);
            return failedResult;
        }

        // If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
        if (action instanceof LastModifiedAwareAJAXActionService) {
            long ifUnmodifiedSince = Tools.optHeaderDate(requestData.getHeader("If-Unmodified-Since"));
            if (ifUnmodifiedSince >= 0 && ((LastModifiedAwareAJAXActionService) action).checkLastModified(ifUnmodifiedSince + 1000, requestData, session)) {
                final AJAXRequestResult failedResult = new AJAXRequestResult();
                failedResult.setHttpStatusCode(HttpServletResponse.SC_PRECONDITION_FAILED);
                return failedResult;
            }
        }

        return null;
    }

    /**
     * Checks if the requested result has not changed since the last request in terms of HTTP caching headers.
     * Namely the following headers are checked:
     * <ul>
     * <li>If-None-Match</li>
     * <li>If-Modified-Since</li>
     * </ul>
     *
     * @param action The target action for the given request
     * @param requestData The request data
     * @param session The session
     * @return An {@link AJAXRequestResult} with {@link ResultType#ETAG}, that causes a <code>304 Not Modified</code> with
     *         no further processing of the request. If checks against those headers are not supported by the underlying action or
     *         if the result has changed since the last request <code>null</code> is returned.
     * @throws OXException
     */
    private static AJAXRequestResult checkResultNotModified(AJAXActionService action, AJAXRequestData requestData, ServerSession session) throws OXException {
        // If-None-Match header should contain "*" or ETag. If so, then return 304.
        final String eTag = requestData.getETag();
        if (null != eTag && (action instanceof ETagAwareAJAXActionService) && (("*".equals(eTag)) || ((ETagAwareAJAXActionService) action).checkETag(eTag, requestData, session))) {
            final AJAXRequestResult etagResult = new AJAXRequestResult();
            etagResult.setType(AJAXRequestResult.ResultType.ETAG);
            final long newExpires = requestData.getExpires();
            if (newExpires > 0) {
                etagResult.setExpires(newExpires);
            }
            return etagResult;
        }

        // If-Modified-Since header should be greater than LastModified. If so, then return 304.
        // This header is ignored if any If-None-Match header is specified.
        if (null == eTag && (action instanceof LastModifiedAwareAJAXActionService)) {
            final long lastModified = requestData.getLastModified();
            if (lastModified >= 0 && ((LastModifiedAwareAJAXActionService) action).checkLastModified(lastModified + 1000, requestData, session)) {
                final AJAXRequestResult etagResult = new AJAXRequestResult();
                etagResult.setType(AJAXRequestResult.ResultType.ETAG);
                final long newExpires = requestData.getExpires();
                if (newExpires > 0) {
                    etagResult.setExpires(newExpires);
                }
                return etagResult;
            }
        }

        return null;
    }

    /**
     * Records the duration of a request
     *
     * @param module The name of the action's module
     * @param action The name of the action
     * @param status The status code of the request
     * @param durationMillis The duration in milliseconds
     */
    private static void recordRequest(String module, String action, String status, long durationMillis) {
        HttpMetricUtil.getInstance().recordRequest(module, action, status, durationMillis);
    }


    // ------------------------------------------------ Execution stuff -------------------------------------------------------

    /**
     * Actually performs this dispatcher task
     *
     * @param action The action to perform
     * @param factory The associated factory
     * @param requestData The request data
     * @param state The option state
     * @param customizers Available customizers
     * @param session The associated session
     * @return The result
     * @throws OXException If handling fails
     */
    public AJAXRequestResult doPerform(AJAXActionService action, AJAXActionServiceFactory factory, AJAXRequestData requestData, AJAXState state, List<AJAXActionCustomizer> customizers, ServerSession session) throws OXException {
        // State already initialized for module?
        if (state != null && factory instanceof AJAXStateHandler handler) {
            if (state.addInitializer(requestData.getModule(), handler)) {
                handler.initialize(state);
            }
        }
        requestData.setState(state);

        // Ensure requested format
        if (requestData.getFormat() == null) {
            requestData.setFormat("apiResponse");
        }

        // Grab applicable dispatcher listeners
        List<DispatcherListener> dispatcherListeners = listenerRegistry.getDispatcherListenersFor(requestData);

        // Perform request
        AJAXRequestResult result = callAction(action, requestData, dispatcherListeners, session);
        if (AJAXRequestResult.ResultType.DIRECT == result.getType()) {
            // No further processing
            return contributeDispatcherListeners(result, dispatcherListeners);
        }
        if (AJAXRequestResult.ResultType.ENQUEUED == result.getType()) {
            // No further processing
            return contributeDispatcherListeners(result, dispatcherListeners);
        }
        result = customizeResult(requestData, result, customizers, session);
        return contributeDispatcherListeners(result, dispatcherListeners);
    }

    private static AJAXRequestResult contributeDispatcherListeners(AJAXRequestResult requestResult, List<DispatcherListener> dispatcherListeners) {
        if (null != requestResult && null != dispatcherListeners) {
            requestResult.addPostProcessor(new DispatcherListenerPostProcessor(dispatcherListeners));
        }
        return requestResult;
    }

    /**
     * Finally calls the requested action with the given request data and returns the result.
     *
     * @param action The action to call
     * @param requestData The request data
     * @param optListeners The optional dispatcher listeners that receive call-backs or <code>null</code>
     * @param session The session
     * @return The actions result
     * @throws OXException If action fails to handle the request data
     */
    private AJAXRequestResult callAction(AJAXActionService action, AJAXRequestData requestData, List<DispatcherListener> optListeners, ServerSession session) throws OXException {
        if ((null == optListeners) || (optListeners.isEmpty())) {
            return doCallAction(action, requestData, session);
        }

        AJAXRequestResult result = null;
        Exception exc = null;
        try {
            triggerOnRequestInitialized(requestData, optListeners);
            result = doCallAction(action, requestData, session);
            return result;
        } catch (Exception e) {
            exc = e;
            throw e;
        } finally {
            triggerOnRequestPerformed(requestData, result, exc, optListeners);
        }
    }

    private static AJAXRequestResult doCallAction(AJAXActionService action, AJAXRequestData requestData, ServerSession session) throws OXException {
        try {
            AJAXRequestResult result = action.perform(requestData, session);
            if (null == result) {
                // Huh...?!
                addLogProperties(requestData, true);
                throw AjaxExceptionCodes.UNEXPECTED_RESULT.create(AJAXRequestResult.class.getSimpleName(), "null");
            }
            return result.setRequestData(requestData);
        } catch (IllegalStateException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof OXException) {
                throw (OXException) cause;
            }
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (ContinuationException e) {
            return handleContinuationException(e, session).setRequestData(requestData);
        } finally {
            requestData.cleanUploads();
        }
    }

    /**
     * Customizes a result object by calling {@link AJAXActionCustomizer#outgoing(AJAXRequestData, AJAXRequestResult, ServerSession)} on every
     * passed customizer with the given request data and result object.
     *
     * @param requestData The request data
     * @param result The result object
     * @param session The session
     * @return The potentially modified result object
     * @throws OXException
     */
    private static AJAXRequestResult customizeResult(AJAXRequestData requestData, AJAXRequestResult result, List<AJAXActionCustomizer> customizers, ServerSession session) throws OXException {
        /*
         * Iterate customizers in reverse oder for request data and result pair
         */
        Collections.reverse(customizers);
        List<AJAXActionCustomizer> outgoing = new LinkedList<>(customizers);
        AJAXRequestResult modifiedResult = result;
        while (!outgoing.isEmpty()) {
            final Iterator<AJAXActionCustomizer> iterator = outgoing.iterator();
            while (iterator.hasNext()) {
                final AJAXActionCustomizer customizer = iterator.next();
                try {
                    final AJAXRequestResult modified = customizer.outgoing(requestData, modifiedResult, session);
                    if (modified != null) {
                        modifiedResult = modified;

                        // Check (again) for direct result type
                        if (AJAXRequestResult.ResultType.DIRECT == modifiedResult.getType()) {
                            // No further processing
                            return modifiedResult;
                        }
                    }
                    iterator.remove();
                } catch (FlowControl.Later l) {
                    // Remains in list and is therefore retried
                }
            }
        }

        return modifiedResult;
    }

    private static void triggerOnRequestInitialized(AJAXRequestData requestData, List<DispatcherListener> dispatcherListeners) throws OXException {
        for (DispatcherListener dispatcherListener : dispatcherListeners) {
            dispatcherListener.onRequestInitialized(requestData);
        }
    }

    private static void triggerOnRequestPerformed(AJAXRequestData requestData, AJAXRequestResult requestResult, Exception e, List<DispatcherListener> dispatcherListeners) throws OXException {
        for (DispatcherListener dispatcherListener : dispatcherListeners) {
            dispatcherListener.onRequestPerformed(requestData, requestResult, e);
        }
    }

    /**
     * Handles specified <code>ContinuationException</code> instance.
     *
     * @param e The exception to handle
     * @param session The associated session
     * @return The AJAX result
     * @throws OXException If <code>ContinuationException</code> does not signal special error code <code>CONTINUATION-0003</code>
     *             (Scheduled for continuation: &lt;uuid&gt;)
     */
    private static AJAXRequestResult handleContinuationException(final ContinuationException e, final ServerSession session) throws OXException {
        if (!ContinuationExceptionCodes.SCHEDULED_FOR_CONTINUATION.equals(e)) {
            throw e;
        }
        final UUID uuid = e.getUuid();
        if (null == uuid) {
            throw e;
        }
        final ContinuationRegistryService continuationRegistry = ServerServiceRegistry.getInstance().getService(ContinuationRegistryService.class);
        if (null == continuationRegistry) {
            throw e;
        }
        final Continuation<Object> continuation = continuationRegistry.getContinuation(uuid, session);
        if (null == continuation) {
            throw e;
        }
        try {
            final ContinuationResponse<Object> cr = continuation.getNextResponse(1000, TimeUnit.NANOSECONDS);
            return new AJAXRequestResult(cr.getValue(), cr.getTimeStamp(), cr.getFormat()).setContinuationUuid(cr.isCompleted() ? null : uuid);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(ie, ie.getMessage());
        }
    }

    /**
     * Adds request-specific log properties (action and module information) as well as optional query string information.
     *
     * @param requestData The request data
     * @param withQueryString Whether to include query string or not
     */
    private static void addLogProperties(final AJAXRequestData requestData, final boolean withQueryString) {
        if (null != requestData) {
            LogProperties.putProperty(LogProperties.Name.AJAX_ACTION, requestData.getAction());
            LogProperties.putProperty(LogProperties.Name.AJAX_MODULE, requestData.getModule());

            if (withQueryString) {
                Map<String, String> parameters = requestData.getParameters();
                StringBuilder sb = new StringBuilder(256);
                sb.append('"');
                boolean first = true;
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    String name = entry.getKey();
                    if (name.equalsIgnoreCase("json")) {
                        // Artificially added form field from a multipart/form-data POST request. Ignore...
                        continue;
                    }
                    if (first) {
                        sb.append('?');
                        first = false;
                    } else {
                        sb.append('&');
                    }
                    String value = LogProperties.getSanitizedValue(name, entry.getValue());
                    sb.append(name).append('=').append(value);
                }
                sb.append('"');
                LogProperties.putProperty(LogProperties.Name.SERVLET_QUERY_STRING, sb.toString());
            }
        }
    }

    /**
     * Looks up the appropriate instance of <code>AJAXActionServiceFactory</code> for the given module.
     * <p>
     * If none is found it tries to find one by removing the last path segment of the given module until one is found or until the last path
     * segment is reached; e.g.
     * <pre>
     *   mail/theAttachmentName -> mail
     * </pre>
     *
     * @param module The module path (e.g. <code>mail/theAttachmentName</code> or <code>folders</code>)
     * @return The suitable factory and module tuple or empty
     */
    private Optional<FactoryAndModule> optFactoryAndModule(String module) {
        String candidate = module;
        AJAXActionServiceFactory serviceFactory = actionFactories.get(candidate);
        if (null == serviceFactory) {
            for (int index; serviceFactory == null && (index = candidate.lastIndexOf('/')) > 0;) {
                candidate = candidate.substring(0, index);
                serviceFactory = actionFactories.get(candidate);
            }
        }
        return serviceFactory == null ? Optional.empty() : Optional.of(new FactoryAndModule(candidate, serviceFactory));
    }

    @Override
    public AJAXActionServiceFactory lookupFactory(final String module) {
        Optional<FactoryAndModule> optionalResult = optFactoryAndModule(module);
        return optionalResult.isPresent() ? optionalResult.get().factory : null;
    }

    private static DispatcherNotes getActionMetadata(final AJAXActionService action) {
        return null == action ? null : action.getClass().getAnnotation(DispatcherNotes.class);
    }

    /**
     * Registers specified factory under given module.
     *
     * @param module The module
     * @param factory The factory (possibly annotated with {@link Module})
     */
    public void register(final String module, final AJAXActionServiceFactory factory) {
        synchronized (actionFactories) {
            AJAXActionServiceFactory current = actionFactories.putIfAbsent(module, factory);
            if (null != current) {
                try {
                    current = actionFactories.get(module);
                    final Module moduleAnnotation = current.getClass().getAnnotation(Module.class);
                    if (null == moduleAnnotation) {
                        LOG.warn("There is already a factory associated with module \"{}\": {}. Therefore registration is denied for factory \"{}\". Unless these two factories provide the \"{}\" annotation to specify what actions are supported by each factory.", module, current.getClass().getName(), factory.getClass().getName(), Module.class.getName());
                    } else {
                        final CombinedActionFactory combinedFactory;
                        if (current instanceof CombinedActionFactory) {
                            combinedFactory = (CombinedActionFactory) current;
                        } else {
                            combinedFactory = new CombinedActionFactory();
                            combinedFactory.add(current);
                            actionFactories.put(module, combinedFactory);
                        }
                        combinedFactory.add(factory);
                    }
                } catch (IllegalArgumentException e) {
                    LOG.error(e.getMessage());
                }
            }
        }
    }

    /**
     * Adds specified customizer factory.
     *
     * @param factory The customizer factory
     */
    public void addCustomizer(final AJAXActionCustomizerFactory factory) {
        this.customizerFactories.add(factory);
    }

    /**
     * Removes the specified customizer factory
     *
     * @param factory The customizer factory
     */
    public void removeCustomizer(AJAXActionCustomizerFactory factory) {
        this.customizerFactories.remove(factory);
    }

    /**
     * Releases specified factory from given module.
     *
     * @param module The module
     * @param factory The factory (possibly annotated with {@link Module})
     */
    public void remove(final String module, final AJAXActionServiceFactory factory) {
        synchronized (actionFactories) {
            final AJAXActionServiceFactory removed = actionFactories.remove(module);
            if (removed instanceof CombinedActionFactory combinedFactory) {
                combinedFactory.remove(factory);
                if (!combinedFactory.isEmpty()) {
                    actionFactories.put(module, combinedFactory);
                }
            }
        }
    }

    /**
     * Adds an {@link AJAXActionAnnotationProcessor}.
     *
     * @param processor The processor
     */
    public void addAnnotationProcessor(AJAXActionAnnotationProcessor processor) {
        if (!annotationProcessors.contains(processor)) {
            annotationProcessors.add(processor);
        }
    }

    /**
     * Removes an {@link AJAXActionAnnotationProcessor}.
     *
     * @param processor The processor
     */
    public void removeAnnotationProcessor(AJAXActionAnnotationProcessor processor) {
        annotationProcessors.remove(processor);
    }

    private static AJAXActionService getActionServiceSafe(final String action, final AJAXActionServiceFactory factory) {
        try {
            return factory.createActionService(action);
        } catch (Exception e) {
            LOG.trace("Failed to create action {} from factory {}", action, factory.getClass().getName(), e);
            return null;
        }
    }

    private Optional<IReference<DispatcherNotes>> determineActionMetadata(String module, String action) {
        ModuleAndAction key = new ModuleAndAction(module, action);
        IReference<DispatcherNotes> actionMetadataReference = dispatcherNotesCache.getIfPresent(key);
        if (actionMetadataReference == null) {
            AJAXActionServiceFactory factory = lookupFactory(module);
            if (factory == null) {
                // No such factory
                return Optional.empty();
            }

            DispatcherNotes actionMetadata = getActionMetadata(getActionServiceSafe(action, factory));
            actionMetadataReference = ImmutableReference.<DispatcherNotes> immutableReferenceFor(actionMetadata);
            dispatcherNotesCache.put(key, actionMetadataReference);
        }
        return Optional.of(actionMetadataReference);
    }

    @Override
    public boolean mayUseFallbackSession(final String module, final String action) throws OXException {
        Optional<IReference<DispatcherNotes>> optionalActionMetadataReference = determineActionMetadata(module, action);
        if (!optionalActionMetadataReference.isPresent()) {
            return false;
        }

        DispatcherNotes actionMetadata = optionalActionMetadataReference.get().getValue();
        return actionMetadata == null ? false : actionMetadata.allowPublicSession();
    }

    @Override
    public boolean mayPerformPublicSessionAuth(final String module, final String action) throws OXException {
        Optional<IReference<DispatcherNotes>> optionalActionMetadataReference = determineActionMetadata(module, action);
        if (!optionalActionMetadataReference.isPresent()) {
            return false;
        }

        DispatcherNotes actionMetadata = optionalActionMetadataReference.get().getValue();
        return actionMetadata == null ? false : actionMetadata.publicSessionAuth();
    }

    @Override
    public boolean mayOmitSession(final String module, final String action) throws OXException {
        Optional<IReference<DispatcherNotes>> optionalActionMetadataReference = determineActionMetadata(module, action);
        if (!optionalActionMetadataReference.isPresent()) {
            return false;
        }

        DispatcherNotes actionMetadata = optionalActionMetadataReference.get().getValue();
        return actionMetadata == null ? false : actionMetadata.noSession();
    }

    @Override
    public boolean noSecretCallback(String module, String action) throws OXException {
        Optional<IReference<DispatcherNotes>> optionalActionMetadataReference = determineActionMetadata(module, action);
        if (!optionalActionMetadataReference.isPresent()) {
            return false;
        }

        DispatcherNotes actionMetadata = optionalActionMetadataReference.get().getValue();
        return actionMetadata == null ? false : actionMetadata.noSecretCallback();
    }

    private static final class ModuleAndAction {

        final String module;
        final String action;
        private final int hash;

        ModuleAndAction(String module, String action) {
            super();
            this.module = module;
            this.action = action;
            final int prime = 31;
            int result = 1;
            result = prime * result + ((module == null) ? 0 : module.hashCode());
            result = prime * result + ((action == null) ? 0 : action.hashCode());
            hash = result;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ModuleAndAction other)) {
                return false;
            }
            if (module == null) {
                if (other.module != null) {
                    return false;
                }
            } else if (!module.equals(other.module)) {
                return false;
            }
            if (action == null) {
                if (other.action != null) {
                    return false;
                }
            } else if (!action.equals(other.action)) {
                return false;
            }
            return true;
        }

    } // End of class ModuleAndAction

    /**
     * {@link FactoryAndModule} - The result of a factory lookup. See {@link DefaultDispatcher#lookupFactory(String)}
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v7.10.4
     */
    private static final class FactoryAndModule {

        final String module;
        final AJAXActionServiceFactory factory;

        /**
         * Initializes a new {@link FactoryAndModule}.
         *
         * @param module The module of the factory
         * @param factory The factory
         */
        FactoryAndModule(String module, AJAXActionServiceFactory factory) {
            super();
            this.module = module;
            this.factory = factory;
        }
    }
}
