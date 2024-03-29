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

package com.openexchange.osgi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import com.google.common.collect.ImmutableList;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.osgi.annotation.OptionalService;
import com.openexchange.osgi.annotation.SingletonService;
import com.openexchange.osgi.console.DeferredActivatorServiceStateLookup;
import com.openexchange.osgi.console.ServiceStateLookup;
import com.openexchange.server.ServiceLookup;

/**
 * {@link DeferredActivator} - Supports the deferred starting of a bundle which highly depends on other services.
 * <p>
 * The needed services are specified through providing their classes by {@link #getNeededServices()}.
 * <p>
 * When all needed services are available, the {@link #startBundle()} method is invoked. For each absent service the
 * {@link #handleUnavailability(Class)} method is triggered to let the programmer decide which actions are further taken. In turn, the
 * {@link #handleAvailability(Class)} method is invoked if a service re-appears.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class DeferredActivator implements BundleActivator, ServiceLookup {

    /** The logger. */
    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DeferredActivator.class);

    private static final DeferredActivatorServiceStateLookup STATE_LOOKUP = new DeferredActivatorServiceStateLookup();

    /**
     * Gets the {@link ServiceStateLookup} for deferred activators.
     *
     * @return The {@link ServiceStateLookup} for deferred activators.
     */
    public static ServiceStateLookup getLookup() {
        return STATE_LOOKUP;
    }

    /**
     * The empty class array.
     */
    protected static final Class<?>[] EMPTY_CLASSES = new Class<?>[0];

    private static final class ReferencedService<S> {
        final ServiceReference<S> reference;
        final S service;

        ReferencedService(S service, ServiceReference<S> reference) {
            super();
            this.service = service;
            this.reference = reference;
        }
    }

    private <S> DeferredServiceTracker<S> newDeferredTracker(BundleContext context, Class<S> clazz, int index, boolean stopOnUnavailability) {
        return new DeferredServiceTracker<S>(context, clazz, index, stopOnUnavailability);
    }

    private final class DeferredServiceTracker<S> extends ServiceTracker<S, S> {

        private static final String SERVICE_RANKING = Constants.SERVICE_RANKING;

        private final Class<? extends S> clazz;
        private final int index;
        private final boolean stopOnUnavailability;

        /**
         * Initializes a new {@link DeferredServiceTracker}.
         *
         * @param context The bundle context
         * @param clazz The service's clazz
         * @param index The index
         * @param stopOnUnavailability Whether to stop the activator in case a needed service becomes unavailbale
         */
        protected DeferredServiceTracker(BundleContext context, Class<S> clazz, int index, boolean stopOnUnavailability) {
            super(context, clazz, null);
            this.clazz = clazz;
            this.index = index;
            this.stopOnUnavailability = stopOnUnavailability;
        }

        @SuppressWarnings("unchecked")
        @Override
        public S addingService(ServiceReference<S> reference) {
            S service = super.addingService(reference);
            try {
                // Get provider
                ServiceProvider<S> serviceProvider = (ServiceProvider<S>) services.get(clazz);
                if (null == serviceProvider) {
                    ServiceProvider<S> newProvider = new DefaultServiceProvider<S>();
                    serviceProvider = (ServiceProvider<S>) services.putIfAbsent(clazz, newProvider);
                    if (null == serviceProvider) {
                        serviceProvider = newProvider;
                    }
                }

                // Add service to provider
                int ranking = 0;
                {
                    Object oRanking = reference.getProperty(SERVICE_RANKING);
                    if (null != oRanking) {
                        if (oRanking instanceof Integer) {
                            ranking = ((Integer) oRanking).intValue();
                        } else {
                            try {
                                ranking = Integer.parseInt(oRanking.toString().trim());
                            } catch (NumberFormatException e) {
                                ranking = 0;
                            }
                        }
                    }
                }
                serviceProvider.addService(service, ranking);

                // Signal availability
                signalAvailability(index, clazz, context);
                updateServiceState();
                return service;
            } catch (Exception e) {
                LOG.error("Failed to add service {}", service.getClass().getName(), e);
                context.ungetService(reference);
                return null;
            }
        }

        @Override
        public void removedService(org.osgi.framework.ServiceReference<S> reference, S service) {
            // Signal unavailability
            signalUnavailability(index, clazz, stopOnUnavailability, context);

            // ... and remove from services
            ConcurrentMap<Class<?>, ServiceProvider<?>> services = DeferredActivator.this.services;
            if (services != null) {
                ServiceProvider<?> serviceProvider = services.get(clazz);
                if (serviceProvider instanceof DefaultServiceProvider) {
                    if (((DefaultServiceProvider<?>) serviceProvider).removeService(service)) {
                        services.remove(clazz);
                    }
                }
            }

            updateServiceState();
            Tools.ungetServiceSafe(reference, context);
        }

    }

    // ------------------------------------------------------------------------------------------------------------------------------ //

    /**
     * An atomic boolean to keep track of started/stopped status.
     */
    protected final AtomicBoolean started;

    /**
     * A flag to indicate that a stop has been performed.
     */
    protected volatile boolean stopPerformed;

    /**
     * The bit mask reflecting already tracked needed services.
     */
    private long availability;

    /**
     * The bit mask if all needed services are available.
     */
    private long allAvailable;

    /**
     * The execution context of the bundle.
     */
    protected volatile BundleContext context;

    /**
     * The initialized service trackers for needed services.
     */
    private ServiceTracker<?, ?>[] neededServiceTrackers;

    /**
     * The available service instances.
     */
    protected ConcurrentMap<Class<?>, ServiceProvider<?>> services;

    /**
     * Additionally fetched services.
     */
    protected final ConcurrentMap<Class<?>, ReferencedService<?>> additionalServices;

    /**
     * Initializes a new {@link DeferredActivator}.
     */
    protected DeferredActivator() {
        super();
        started = new AtomicBoolean();
        additionalServices = new ConcurrentHashMap<Class<?>, ReferencedService<?>>(6, 0.9f, 1);
    }

    /**
     * Specifies whether this activator is supposed to perform a stop operation once a needed service becomes unavailable.
     *
     * @return <code>true</code> to stop on service absence; otherwise <code>false</code>
     */
    protected boolean stopOnServiceUnavailability() {
        return false;
    }

    /**
     * Checks if start-up of associated bundle is allowed.
     *
     * @return <code>true</code> if start-up is allowed; otherwise <code>false</code>
     */
    protected boolean startUpAllowed() {
        return true;
    }

    /**
     * Gets the classes of the services which need to be available to start this activator.
     * <p>
     * <div style="background-color:#FFDDDD; padding:6px; margin:0px;">
     * <b>Note</b>: Listed services are supposed to be singleton services!<br>
     * Please do not list such OSGi services that may get registered multiple times by different bundles.<br>
     * &nbsp;See also: <i><code>com.openexchange.osgi.annotation.SingletonService</code></i> annotation.
     * </div>
     *
     * @return The array of {@link Class} instances of needed services
     */
    protected abstract Class<?>[] getNeededServices();

    /**
     * Handles the (possibly temporary) unavailability of a needed service. The specific activator may decide which actions are further done
     * dependent on given service's class.
     * <p>
     * On the one hand, if the service in question is not needed to further keep on running, it may be discarded. On the other hand, if the
     * service is absolutely needed; the {@link #stopBundle()} method can be invoked.
     *
     * @param clazz The service's class
     */
    protected abstract void handleUnavailability(final Class<?> clazz);

    /**
     * Handles the re-availability of a needed service. The specific activator may decide which actions are further done dependent on given
     * service's class.
     *
     * @param clazz The service's class
     */
    protected abstract void handleAvailability(final Class<?> clazz);

    private static final String MARKER = " ---=== /!\\ ===--- ";

    /**
     * Initializes this deferred activator's members.
     *
     * @throws Exception If no needed services are specified and immediately starting bundle fails
     */
    private final void init(final BundleContext context) throws Exception {
        updateServiceState();
        final Class<?>[] classes = getNeededServices();
        if (null == classes || 0 == classes.length) {
            services = new ConcurrentHashMap<Class<?>, ServiceProvider<?>>(1, 0.9f, 1);
            neededServiceTrackers = new ServiceTracker[0];
            availability = allAvailable = 0;
            startUp(context);
        } else {
            final int len = classes.length;
            if (len > 0 && new HashSet<Class<?>>(Arrays.asList(classes)).size() != len) {
                throw new IllegalArgumentException("Duplicate class/interface provided through getNeededServices()");
            }
            {
                String sep = Strings.getLineSeparator();
                for (Class<?> trackedService : classes) {
                    if (null == trackedService.getAnnotation(SingletonService.class) && trackedService.getName().startsWith("com.openexchange.")) {
                        LOG.debug("{}{}\t{} tracks needed service {} that is not annotated as a {}{}", sep, sep, getClass().getName(), trackedService.getName(), SingletonService.class.getSimpleName(), sep);
                    }
                    if (null != trackedService.getAnnotation(OptionalService.class)) {
                        LOG.warn("{}{}\t{}{} tracks optional service {} as needed service, which is not guaranteed to appear. Potentially this activator will therefore not start.{}{}", sep, sep, MARKER, getClass().getName(), trackedService.getName(), MARKER, sep);
                    }
                }
            }
            services = new ConcurrentHashMap<Class<?>, ServiceProvider<?>>(len, 0.9f, 1);
            neededServiceTrackers = new ServiceTracker[len];
            availability = 0;
            allAvailable = (1L << len) - 1;
            /*
             * Initialize service trackers for needed services
             */
            boolean stopOnUnavailability = stopOnServiceUnavailability();
            for (int i = 0; i < len; i++) {
                final Class<? extends Object> clazz = classes[i];
                final DeferredServiceTracker<? extends Object> tracker = newDeferredTracker(context, clazz, i, stopOnUnavailability);
                tracker.open();
                ServiceTracker<?, ?>[] neededServiceTrackers = this.neededServiceTrackers;
                if (null != neededServiceTrackers) {
                    // During tracker.open() an exception can occur and then the reset() method is called, which sets the
                    // neededServiceTrackers to null.
                    neededServiceTrackers[i] = tracker;
                }
            }
            if (len == 0) {
                startUp(context);
            }
        }
    }

    /**
     * Resets this deferred activator's members.
     *
     * @param context The bundle context
     */
    private final void reset(BundleContext context) {
        // Close trackers
        ServiceTracker<?, ?>[] neededServiceTrackers = this.neededServiceTrackers;
        if (null != neededServiceTrackers) {
            for (int i = 0; i < neededServiceTrackers.length; i++) {
                ServiceTracker<?, ?> tracker = neededServiceTrackers[i];
                if (tracker != null) {
                    tracker.close();
                    neededServiceTrackers[i] = null;
                }
            }
            this.neededServiceTrackers = null;
        }
        availability = 0;
        allAvailable = -1;

        // Unget additional services
        for (ReferencedService<?> referencedService : additionalServices.values()) {
            context.ungetService(referencedService.reference);
        }
        additionalServices.clear();

        // Empty tracked services
        ConcurrentMap<Class<?>, ServiceProvider<?>> services = this.services;
        if (null != services) {
            services.clear();
            this.services = null;
        }

        // Release context reference
        this.context = null;
    }

    /**
     * Updates this bundles service state entry
     */
    protected final void updateServiceState() {
        if (null == context) {
            return;
        }
        final Class<?>[] classes = getNeededServices();
        if (null == classes) {
            STATE_LOOKUP.setState(context.getBundle().getSymbolicName() + ':' + getClass().getSimpleName(), new ArrayList<String>(0), new ArrayList<String>());
            return;
        }
        final ImmutableList.Builder<String> missing = ImmutableList.builder();
        final ImmutableList.Builder<String> present = ImmutableList.builder();

        ConcurrentMap<Class<?>, ServiceProvider<?>> services = this.services;
        for (final Class<?> clazz : classes) {
            if (services != null && services.containsKey(clazz)) {
                present.add(clazz.getName());
            } else {
                missing.add(clazz.getName());
            }
        }
        STATE_LOOKUP.setState(context.getBundle().getSymbolicName() + ':' + getClass().getSimpleName(), missing.build(), present.build());
    }

    /**
     * Signals availability of the class whose index in array provided by {@link #getNeededServices()} is equal to given <code>index</code>
     * argument.
     * <p>
     * If not started, yet, and all needed services are available, then the {@link #startBundle()} method is invoked. If already started the
     * service's re-availability is propagated.
     *
     * @param index The class' index
     * @param clazz The service's class
     * @param context The associated bundle context
     */
    protected final void signalAvailability(final int index, final Class<?> clazz, BundleContext context) {
        availability |= (1L << index);
        if (started.get()) {
            /*
             * Signal availability of single service
             */
            handleAvailability(clazz);
        } else {
            if (availability == allAvailable) {
                /*
                 * Start bundle
                 */
                try {
                    startUp(context);
                } catch (Exception e) {
                    Throwable t = e;
                    if (t.getCause() instanceof BundleException) {
                        t = t.getCause();
                    }
                    final Bundle bundle = context.getBundle();
                    String errorMsg = t instanceof OXException ? ((OXException) t).getLogMessage() : t.getMessage();
                    if (null == errorMsg || "null".equals(errorMsg)) {
                        errorMsg = t.getClass().getName();
                    }
                    LOG.error("{}Start-up of bundle \"{}\" failed: {}", Strings.getLineSeparator(), bundle.getSymbolicName(), errorMsg, t);
                    reset(context);
                    /*
                     * Shut-down
                     */
                    if (Bundle.STARTING == bundle.getState()) {
                        /*
                         * Bundle cannot be stopped by same thread if still in STARTING state
                         */
                        new Thread(new Runnable() {

                            @Override
                            public void run() {
                                shutDownBundle(bundle);
                            }
                        }).start();
                    } else {
                        shutDownBundle(bundle);
                    }
                }
            }
        }
    }

    /**
     * Shuts-down specified bundle.
     *
     * @param bundle The bundle to shut-down
     */
    protected static void shutDownBundle(Bundle bundle) {
        String sep = Strings.getLineSeparator();
        try {
            /*
             * Stop with Bundle.STOP_TRANSIENT set to zero
             */
            bundle.stop();
            LOG.error("{}{}Bundle \"{}\" stopped.{}", sep, sep, bundle.getSymbolicName(), sep);
        } catch (BundleException e) {
            LOG.error("{}{}Bundle \"{}\" could not be stopped.{}", sep, sep, bundle.getSymbolicName(), sep, e);
        }
    }

    /**
     * Marks the class whose index in array provided by {@link #getNeededServices()} is equal to given <code>index</code> argument as absent
     * and notifies its unavailability.
     *
     * @param index The class' index
     * @param clazz The service's class
     * @param stop Whether to stop this activator
     * @param context The associated bundle context
     */
    final void signalUnavailability(int index, Class<?> clazz, boolean stop, BundleContext context) {
        availability &= ~(1L << index);
        if (started.get()) {
            if (stop) {
                try {
                    doStop(context);
                } catch (Exception e) {
                    LOG.error("", e);
                }
            } else {
                handleUnavailability(clazz);
            }
        }
    }

    @Override
    public void start(final BundleContext context) throws Exception {
        try {
            this.context = context;
            init(context);
        } catch (org.osgi.framework.ServiceException e) {
            LOG.error("", e);
            // Do not re-throw!
        } catch (Exception e) {
            LOG.error("", e);
            throw e;
        }
    }

    private void startUp(BundleContext context) throws Exception {
        stopPerformed = false;
        this.context = context;
        if (startUpAllowed()) {
            startBundle();
        }
        started.set(true);
    }

    /**
     * Called when this bundle is started; meaning all needed services are available. So the framework can perform the bundle-specific
     * activities necessary to start this bundle. This method can be used to register services or to allocate any resources that this bundle
     * needs.
     * <p>
     * This method must complete and return to its caller in a timely manner.
     *
     * @throws Exception If this method throws an exception, this bundle is marked as stopped and the Framework will remove this bundle's
     *             listeners, unregister all services registered by this bundle, and release all services used by this bundle.
     */
    protected abstract void startBundle() throws Exception;

    @Override
    public void stop(final BundleContext context) throws Exception {
        try {
            doStop(context);
        } catch (Exception e) {
            LOG.error("", e);
            throw e;
        } finally {
            stopPerformed = true;
            reset(context);
        }
    }

    /**
     * Performs the stop operation.
     *
     * @param context The bundle context
     * @throws Exception If stop operation fails
     */
    private void doStop(BundleContext context) throws Exception {
        if (started.compareAndSet(true, false)) {
            this.context = context;
            stopBundle();
        }
    }

    /**
     * Called when this bundle is stopped so the framework can perform the bundle-specific activities necessary to stop the bundle. In
     * general, this method should undo the work that the BundleActivator.start method started. There should be no active threads that were
     * started by this bundle when this bundle returns. A stopped bundle must not call any Framework objects.
     * <p>
     * This method must complete and return to its caller in a timely manner.
     *
     * @throws Exception If this method throws an exception, the bundle is still marked as stopped, and the Framework will remove the
     *             bundle's listeners, unregister all services registered by the bundle, and release all services used by the bundle.
     */
    protected abstract void stopBundle() throws Exception;

    /**
     * Returns {@link ServiceTracker#getService()} invoked on the service tracker bound to specified class.
     *
     * @param <S> Type of service's class
     * @param clazz The service's class
     * @return The service obtained by service tracker or <code>null</code>
     * @throws ShutDownRuntimeException If system is currently shutting down
     */
    @Override
    public <S extends Object> S getService(final Class<? extends S> clazz) {
        if (stopPerformed) {
            throw new ShutDownRuntimeException();
        }

        ConcurrentMap<Class<?>, ServiceProvider<?>> services = this.services;
        if (null == services) {
            /*
             * Services not initialized
             */
            return null;
        }
        final ServiceProvider<?> serviceProvider = services.get(clazz);
        if (null == serviceProvider) {
            return null;
        }
        final Object service = serviceProvider.getService();
        if (null == service) {
            /*
             * Given class is not tracked by any service tracker
             */
            return null;
        }
        return clazz.cast(service);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends Object> S getOptionalService(final Class<? extends S> clazz) {
        if (stopPerformed) {
            throw new ShutDownRuntimeException();
        }

        ServiceProvider<?> serviceProvider = services.get(clazz);
        if (null != serviceProvider) {
            Object service = serviceProvider.getService();
            if (null != service) {
                return clazz.cast(service);
            }
        }

        ConcurrentMap<Class<?>, ReferencedService<?>> additionalServices = this.additionalServices;
        ReferencedService<S> referencedService = (ReferencedService<S>) additionalServices.get(clazz);
        if (null == referencedService) {
            ServiceReference<S> serviceReference = (ServiceReference<S>) context.getServiceReference(clazz);
            if (serviceReference == null) {
                return null;
            }

            S service = context.getService(serviceReference);
            ReferencedService<S> newReferencedService = new ReferencedService<S>(service, serviceReference);
            referencedService = (ReferencedService<S>) additionalServices.putIfAbsent(clazz, newReferencedService);
            if (null == referencedService) {
                referencedService = newReferencedService;
            } else {
                context.ungetService(serviceReference);
            }
        }
        return referencedService.service;
    }

    /**
     * Adds specified service (if absent).
     *
     * @param <S> Type of service's class
     * @param clazz The service's class
     * @param service The service to add
     * @return <code>true</code> if service is added; otherwise <code>false</code> if not initialized or such a service already exists
     */
    protected <S> boolean addService(final Class<S> clazz, final S service) {
        ConcurrentMap<Class<?>, ServiceProvider<?>> services = this.services;
        if (null == services || !clazz.isInstance(service)) {
            /*
             * Services not initialized
             */
            return false;
        }
        return (null == services.putIfAbsent(clazz, new SimpleServiceProvider<S>(service)));
    }

    /**
     * Adds specified service (if absent).
     *
     * @param <S> Type of service's class
     * @param clazz The service's class
     * @param service The service to add
     * @return <code>true</code> if service is added; otherwise <code>false</code> if not initialized or such a service already exists
     */
    protected <S> boolean addServiceAlt(final Class<? extends S> clazz, final S service) {
        ConcurrentMap<Class<?>, ServiceProvider<?>> services = this.services;
        if (null == services || !clazz.isInstance(service)) {
            /*
             * Services not initialized
             */
            return false;
        }
        return (null == services.putIfAbsent(clazz, new SimpleServiceProvider<S>(service)));
    }

    /**
     * Removes specified service.
     *
     * @param <S> Type of service's class
     * @param clazz The service's class
     * @return <code>true</code> if service is removed; otherwise <code>false</code> if not initialized or absent
     */
    protected <S> boolean removeService(final Class<? extends S> clazz) {
        ConcurrentMap<Class<?>, ServiceProvider<?>> services = this.services;
        if (null == services) {
            /*
             * Services not initialized
             */
            return false;
        }
        return (null != services.remove(clazz));
    }

    /**
     * Checks if activator currently holds all needed services.
     *
     * @return <code>true</code> if activator currently holds all needed services; otherwise <code>false</code>
     */
    protected final boolean allAvailable() {
        final Class<?>[] classes = getNeededServices();
        if (null == classes) {
            /*
             * This deferred activator waits for no services
             */
            return true;
        }
        final int len = classes.length;
        if (len == 0) {
            /*
             * This deferred activator waits for no services
             */
            return true;
        }
        for (int i = 0; i < len; i++) {
            if (!services.containsKey(classes[i])) {
                return false;
            }
        }
        return true;
    }

}
