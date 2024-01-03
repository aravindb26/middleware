
package com.openexchange.test.osgi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.openexchange.annotation.NonNullByDefault;
import com.openexchange.annotation.Nullable;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.MockConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.test.mock.InjectionFieldConstants;
import com.openexchange.test.mock.MockUtils;
import com.openexchange.test.mock.impl.ServiceLookupMock;

@SuppressWarnings("restriction")
@NonNullByDefault
public abstract class AbstractHousekeepingActivatorTest {

    public AbstractHousekeepingActivatorTest() {
        super();
    }

    @Test
    public void testHappyPath() throws Exception {
        ActivatorExtender activatorExtender = getActivator();
        prepareServices(activatorExtender, activatorExtender.getNeededServices());
        activatorExtender.doStartBundle();
        Class<?>[] startedServices = activatorExtender.getStartedServices();
        boolean checked = false;
        if (null != startedServices && startedServices.length > 0) {
            testByClassName(activatorExtender, startedServices);
            checked = true;
        }
        Class<?>[] services = activatorExtender.getServices();
        if (null != services && services.length > 0) {
            testByServiceName(activatorExtender, services);
            checked = true;
        }
        if (!checked) {
            assertThat(activatorExtender.getRegisteredServices(), is(emptyIterable()));
        }
    }

    @Test
    public void testNotSoHappyPath() throws Exception {
        Class<?>[] services = getActivator().getNeededServices();
        Set<Class<?>> ignoredServices = getActivator().getIgnoredServices();
        for (Class<?> exclude : services) {
            if (ignoredServices.remove(exclude)) {
                continue;
            }
            ActivatorExtender activatorExtender = getActivator();
            prepareAllExceptExcludedServices(activatorExtender, activatorExtender.getNeededServices(), exclude);
            try {
                activatorExtender.doStartBundle();
                fail("No Exception thrown for class: " + exclude.getCanonicalName());
            } catch (OXException e) {
                assertThat(e, instanceOf(OXException.class));
                assertThat(e.getMessage(), containsString("SRV-0001 Categories=TRY_AGAIN Message='The required service"));
                assertThat(e.getMessage(), containsString(exclude.getCanonicalName()));
            }
        }
        assertThat("ingored Services contains an element that is not in getNeededServices", ignoredServices, is(empty()));
    }

    private void testByServiceName(ActivatorExtender activatorExtender, Class<?>[] services) {
        assertThat(activatorExtender.getRegisteredServices(), containsInAnyOrder(services));
    }

    private void testByClassName(ActivatorExtender activatorExtender, Class<?>[] startedServices) {
        assertThat(getServiceRegistrations(activatorExtender), containsInAnyOrder(startedServices));
    }

    private ArrayList<Class<?>> getServiceRegistrations(ActivatorExtender activatorExtender) {
        assertThat(activatorExtender.getServiceRegistrations(), is(notNullValue()));
        assertThat(activatorExtender.getServiceRegistrations().keys(), is(notNullValue()));
        Collection<Class<?>> serviceRegistrations = activatorExtender.getServiceRegistrations().keys();
        ArrayList<Class<?>> list = new ArrayList<>();
        for (Object serviceRegistration : serviceRegistrations) {
            assertThat(serviceRegistration, is(notNullValue()));
            list.add(serviceRegistration.getClass());
        }
        return list;
    }

    public abstract ActivatorExtender getActivator();

    protected void prepareServices(ActivatorExtender activatorExtender, Class<?>[] needed) {
        for (Class<?> clazz : needed) {
            if (null == clazz) {
                continue;
            }
            handleServices(activatorExtender, clazz);
        }
    }

    public void prepareAllExceptExcludedServices(ActivatorExtender activatorExtender, Class<?>[] needed, Class<?> exlcude) {
        for (Class<?> clazz : needed) {
            if (null == clazz) {
                continue;
            }
            if (clazz.equals(exlcude)) {
                continue;
            }
            handleServices(activatorExtender, clazz);
        }
    }

    private void handleServices(ActivatorExtender activatorExtender, Class<?> clazz) {
        if (clazz.equals(ConfigurationService.class)) {
            activatorExtender.getServiceMocker().addService(ConfigurationService.class, activatorExtender.getConfigurationService());
            return;
        }
        if (servicePrepared(clazz, activatorExtender)) {
            return;
        }
        activatorExtender.getServiceMocker().mock(clazz);
    }

    private <S> boolean servicePrepared(Class<? extends S> clazz, ActivatorExtender activatorExtender) {
        S instance = activatorExtender.getPreparedService(clazz);
        if (instance == null) {
            return false;
        }
        activatorExtender.getServiceMocker().addService(clazz, instance);
        return true;
    }

    public static class ActivatorExtender {

        private final HousekeepingActivator                   delegate;
        private final SetMultimap<Class<?>, ServiceRegistration<?>> serviceRegistrations;
        private final ServiceLookupMock                       serviceMocker;
        private final List<Class<?>>                          registeredServices            = new ArrayList<>();
        private final Map<ServiceReference<?>, Object>        registeredServicesByReference = new HashMap<>();
        private Set<Class<?>>                                 ignoredServices               = new HashSet<>();
        private Class<?>[]                                    services                      = new Class<?>[] {};
        private @Nullable ConfigurationService                          configService                 = null;
        private Map<Class<?>, Object>                         preparedServices              = new HashMap<>();
        private final Map<String, ServiceListener>                  serviceListeners              = new HashMap<>();

        public ActivatorExtender(HousekeepingActivator activator) {
            this.delegate = PowerMockito.spy(activator);
            BundleContext bundleContext = Mockito.mock(BundleContext.class);

            mockRegisterService(bundleContext);
            Bundle bundle = Mockito.mock(Bundle.class);
            this.serviceMocker = new ServiceLookupMock();
            MockUtils.injectValueIntoPrivateField(this.getDelegate(), InjectionFieldConstants.SERVICES, serviceMocker.getServices());

            // CONTEXT
            Mockito.when(bundleContext.getBundle()).thenReturn(bundle);
            Mockito.when(bundle.getVersion()).thenReturn(new Version(1, 1, 1));
            MockUtils.injectValueIntoPrivateField(this.getDelegate(), InjectionFieldConstants.CONTEXT, bundleContext);
            this.serviceRegistrations = Multimaps.synchronizedSetMultimap(HashMultimap.create(6, 2));
            MockUtils.injectValueIntoPrivateField(this.getDelegate(), InjectionFieldConstants.SERVICE_REGISTRATIONS, serviceRegistrations);

        }

        @SuppressWarnings("unchecked")
        public <S> S getPreparedService(Class<? extends S> clazz) {
            return (S) preparedServices.get(clazz);
        }

        @SuppressWarnings("unchecked")
        private void mockRegisterService(BundleContext bundleContext) {
            Mockito.when(bundleContext.registerService(Mockito.any(Class.class), Mockito.isA(Object.class), Mockito.any(Dictionary.class))).thenAnswer(new Answer<ServiceRegistration<?>>() {

                @Override
                public @Nullable ServiceRegistration<?> answer(@Nullable InvocationOnMock invocation) throws Throwable {
                    return invocation != null ? handleRegistration(invocation) : null;
                }

            });
            Mockito.when(bundleContext.registerService(Mockito.any(Class.class), Mockito.isA(Object.class), Mockito.isNull())).thenAnswer(new Answer<ServiceRegistration<?>>() {

                @Override
                public @Nullable ServiceRegistration<?> answer(@Nullable InvocationOnMock invocation) throws Throwable {
                    return invocation != null ? handleRegistration(invocation) : null;
                }
            });
            Mockito.when(bundleContext.getService(Mockito.any(ServiceReference.class))).thenAnswer(new Answer<Object>() {

                @Override
                public Object answer(@Nullable InvocationOnMock invocation) throws Throwable {
                    return registeredServicesByReference.get(invocation.getArgument(0));
                }
            });

            try {
                Mockito.doAnswer(
                    invocation -> {
                        ServiceListener serviceTracker = invocation.getArgument(0);
                        String filter = invocation.getArgument(1);

                        FilterImpl filterImpl = FilterImpl.newInstance(filter, true);
                        // only use object filters
                        String clazz = filterImpl.getRequiredObjectClass();
                        if (null != clazz) {
                            serviceListeners.put(clazz, serviceTracker);
                        }
                        return null;
                    }).when(bundleContext).addServiceListener(Mockito.any(ServiceListener.class), Mockito.any(String.class));
            } catch (Exception e) {
                Assert.fail("Exception: " + e.getMessage());
            }
        }

        protected <S> ServiceRegistration<S> handleRegistration(InvocationOnMock invocation) {
            Class<S> argument = invocation.getArgument(0);
            registeredServices.add(argument);
            Object service = invocation.getArgument(1);
            @SuppressWarnings("unchecked") ServiceReference<S> serviceReference = Mockito.mock(ServiceReference.class);
            registeredServicesByReference.put(serviceReference, service);
            ServiceListener serviceListener = serviceListeners.get(argument.getName());
            if (null != serviceListener) {
                serviceListener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, serviceReference));
            }
            @SuppressWarnings("unchecked") ServiceRegistration<S> serviceReg = Mockito.mock(ServiceRegistration.class);
            Mockito.when(serviceReg.getReference()).thenReturn(serviceReference);
            return serviceReg;
        }

        public ConfigurationService getConfigurationService() {
            ConfigurationService configService = this.getConfigService();
            if (null == configService) {
                return new MockConfigurationService();
            }
            return configService;
        }

        public static ActivatorExtender of(HousekeepingActivator activator) {
            return new ActivatorExtender(activator);
        }

        /**
         * Can be called with
         *
         * <pre class="code">
         * <code class="java">
         * ActivatorExtender.builder(new Activator())
         * .withServices(new Class<?>[] {
         * Service1.class,
         * Service2.class
         * }).build();
         * </pre>
         *
         * </code>
         *
         * @param activator
         * @return
         */
        public static Builder builder(HousekeepingActivator activator) {
            return new Builder(activator);
        }

        public static class Builder {

            @Nullable HousekeepingActivator               activator        = null;
            @Nullable Class<?>[]                          services         = null;
            @Nullable Class<?>[]                          ingoredServices  = null;
            private @Nullable ConfigurationService        config           = null;
            private final Map<Class<?>, Object> preparedServices = new HashMap<>();

            private Builder(HousekeepingActivator activator) {
                this.activator = activator;
            }

            public Builder withServices(Class<?>... services) {
                this.services = services;
                return this;
            }

            public Builder withIgnoredServices(Class<?>... ingoredServices) {
                this.ingoredServices = ingoredServices;
                return this;
            }

            public Builder withConfigService(ConfigurationService config) {
                this.config = config;
                return this;
            }

            public Builder withConfigService(Object... parameters) {
                this.config = MockConfigurationService.forValues(parameters);
                return this;
            }

            public <S> Builder withPreparedService(Class<S> clazz, final S service) {
                this.preparedServices.put(clazz, service);
                return this;
            }

            public ActivatorExtender build() {
                HousekeepingActivator activator = this.activator;
                if (null == activator) {
                    throw new IllegalArgumentException("activator is null");
                }
                ActivatorExtender activatorExtender = ActivatorExtender.of(activator);
                Class<?>[] services = this.services;
                if (null != services) {
                    activatorExtender.setServices(services);
                }
                if (null != ingoredServices) {
                    ImmutableSet.Builder<Class<?>> builder = ImmutableSet.builder();
                    for (Class<?> class1 : ingoredServices) {
                        builder.add(class1);
                    }
                    activatorExtender.setIgnoredServices(builder.build());
                }
                ConfigurationService config = this.config;
                if (null != config) {
                    activatorExtender.setConfigService(config);
                }
                activatorExtender.setPreparedServices(preparedServices);
                return activatorExtender;
            }

            public Map<Class<?>, Object> getPreparedServices() {
                return preparedServices;
            }

        }

        public Class<?>[] getNeededServices() {
            try {
                return Whitebox.invokeMethod(getDelegate(), "getNeededServices");
            } catch (Exception e) {
                throw new IllegalArgumentException("something is wrong: " + e.toString());
            }
        }

        public void setPreparedServices(Map<Class<?>, Object> preparedServices) {
            this.preparedServices = preparedServices;
        }

        public void doStartBundle() throws Exception {
            Whitebox.invokeMethod(getDelegate(), "startBundle");
        }

        @Deprecated
        public Class<?>[] getStartedServices() {
            return new Class<?>[] {};
        }

        public Class<?>[] getServices() {
            return services;
        }

        public SetMultimap<Class<?>, ServiceRegistration<?>> getServiceRegistrations() {
            return serviceRegistrations;
        }

        public ServiceLookupMock getServiceMocker() {
            return serviceMocker;
        }

        public HousekeepingActivator getDelegate() {
            return delegate;
        }

        public List<Class<?>> getRegisteredServices() {
            return registeredServices;
        }

        public void setServices(Class<?>[] services) {
            this.services = services;
        }

        public @Nullable ConfigurationService getConfigService() {
            return configService;
        }

        public void setConfigService(ConfigurationService configService) {
            this.configService = configService;
        }

        public Set<Class<?>> getIgnoredServices() {
            return new HashSet<>(ignoredServices);
        }

        public void setIgnoredServices(Set<Class<?>> ignoredServices) {
            this.ignoredServices = ignoredServices;
        }

    }
}
