/**
 * Copyright (c) 2012, Ben Fortuna
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  o Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 *  o Neither the name of Ben Fortuna nor the names of any other contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.fortuna.ical4j.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.rest.client.httpclient.HttpClientService;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.httpclient.HttpClientServiceProvider;
import net.fortuna.ical4j.httpclient.TimeZoneRegistryHttpProperties;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.TzUrl;
import net.fortuna.ical4j.util.CompatibilityHints;
import net.fortuna.ical4j.util.Configurator;
import net.fortuna.ical4j.util.ResourceLoader;

/**
 * $Id$
 *
 * Created on 18/09/2005
 *
 * The default implementation of a <code>TimeZoneRegistry</code>. This implementation will search the classpath for
 * applicable VTimeZone definitions used to back the provided TimeZone instances.
 * @author Ben Fortuna
 */
public class TimeZoneRegistryImpl implements TimeZoneRegistry {

    private static final String DEFAULT_RESOURCE_PREFIX = "zoneinfo/";

    private static final Pattern TZ_ID_SUFFIX = Pattern.compile("(?<=/)[^/]*/[^/]*$");

    private static final String UPDATE_ENABLED = "net.fortuna.ical4j.timezone.update.enabled";

    private static final ConcurrentMap<String, ConcurrentMap<String, TimeZoneLoader>> DEFAULT_TIMEZONES = new ConcurrentHashMap<>();

    private static final Properties ALIASES = new Properties();
    static {
        try (InputStream in = ResourceLoader.getResourceAsStream("net/fortuna/ical4j/model/tz.alias")) {
            if (null != in) {
                ALIASES.load(in);
            }
        }
        catch (IOException ioe) {
            LoggerFactory.getLogger(TimeZoneRegistryImpl.class).warn(
                    "Error loading timezone aliases: {}", ioe.getMessage());
        }

        try (InputStream resourceStream = ResourceLoader.getResourceAsStream("tz.alias")) {
            if (null != resourceStream) {
                ALIASES.load(resourceStream);
            }
        }
        catch (Exception e) {
            LoggerFactory.getLogger(TimeZoneRegistryImpl.class).debug(
        			"Error loading custom timezone aliases: {}", e.getMessage());
        }
    }

    private final Map<String, TimeZone> timezones;

    private final String resourcePrefix;

    /**
     * Default constructor.
     */
    public TimeZoneRegistryImpl() {
        this(DEFAULT_RESOURCE_PREFIX);
    }

    /**
     * Creates a new instance using the specified resource prefix.
     * @param resourcePrefix a prefix prepended to classpath resource lookups for default timezones
     */
    public TimeZoneRegistryImpl(final String resourcePrefix) {
        this.resourcePrefix = resourcePrefix;
        timezones = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void register(final TimeZone timezone) {
    	// for now we only apply updates to included definitions by default..
    	register(timezone, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void register(final TimeZone timezone, boolean update) {
    	if (update) {
            // load any available updates for the timezone..
            timezones.put(timezone.getID(), new TimeZone(updateDefinition(timezone.getVTimeZone())));
    	}
    	else {
            timezones.put(timezone.getID(), timezone);
    	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void clear() {
        timezones.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final TimeZone getTimeZone(final String id) {
        TimeZone timezone = timezones.get(id);
        if (timezone == null) {
            ConcurrentMap<String, TimeZoneLoader> cachedTimezones = DEFAULT_TIMEZONES.get(resourcePrefix);
            if (cachedTimezones == null) {
                ConcurrentMap<String, TimeZoneLoader> newCachedTimezones = new ConcurrentHashMap<>();
                cachedTimezones = DEFAULT_TIMEZONES.putIfAbsent(resourcePrefix, newCachedTimezones);
                if (cachedTimezones == null) {
                    cachedTimezones = newCachedTimezones;
                }
            }
            TimeZoneLoader loader = cachedTimezones.get(id);
            if (loader == null) {
                TimeZoneLoader newLoader = new TimeZoneLoader(id, this);
                loader = cachedTimezones.putIfAbsent(id, newLoader);
                if (loader == null) {
                    loader = newLoader;
                }
            }
            timezone = loader.loadTimeZone();
        }
        return timezone;
    }

    /**
     * Helper class to cache loaded time zone information.
     */
    private static final class TimeZoneLoader {

        private final String id;
        private final TimeZoneRegistryImpl registry;
        private final AtomicReference<Reference<TimeZone>> timeZoneReference;
        private final AtomicReference<Reference<VTimeZone>> loadedVTimeZone;

        /**
         * Initializes a new {@link TimeZoneLoader}.
         * 
         * @param id The time zone identifier
         * @param registry The registry instance to use
         */
        TimeZoneLoader(String id, net.fortuna.ical4j.model.TimeZoneRegistryImpl registry) {
            super();
            this.id = id;
            this.registry = registry;
            timeZoneReference = new AtomicReference<>();
            loadedVTimeZone = new AtomicReference<>();
        }

        TimeZone loadTimeZone() {
            Reference<TimeZone> optTimeZone = timeZoneReference.get();
            if (optTimeZone != null) {
                return optTimeZone.get();
            }

            final String alias = ALIASES.getProperty(id);
            if (alias != null) {
                return registry.getTimeZone(alias);
            }

            boolean error = true;
            try {
                VTimeZone vTimeZone = getVTimeZone();
                if (vTimeZone != null) {
                    // XXX: temporary kludge..
                    // ((TzId) vTimeZone.getProperties().getProperty(Property.TZID)).setValue(id);
                    TimeZone timezone = new TimeZone(vTimeZone);
                    timeZoneReference.set(Reference.referenceFor(timezone));
                    return timezone;
                } else if (CompatibilityHints.isHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING)) {
                    // strip global part of id and match on default tz..
                    Matcher matcher = TZ_ID_SUFFIX.matcher(id);
                    if (matcher.find()) {
                        return registry.getTimeZone(matcher.group());
                    }
                }
                error = false;
            } catch (Exception e) {
                Logger log = LoggerFactory.getLogger(TimeZoneRegistryImpl.class);
                log.warn("Error occurred loading VTimeZone", e);
            }

            if (!error) {
                // only cache if no error
                timeZoneReference.set(Reference.referenceFor(null));
            }
            return null;
        }

        private VTimeZone getVTimeZone() throws IOException, ParserException {
            Reference<VTimeZone> optVTimeZone = loadedVTimeZone.get();
            if (optVTimeZone == null) {
                synchronized (this) {
                    optVTimeZone = loadedVTimeZone.get();
                    if (optVTimeZone == null) {
                        // need to load
                        VTimeZone vTimeZone = registry.loadVTimeZone(id);
                        optVTimeZone = Reference.referenceFor(vTimeZone);
                        loadedVTimeZone.set(optVTimeZone);
                    }
                }
            }
            return optVTimeZone.get();
        }
    }

    /**
     * Loads an existing VTimeZone from the classpath corresponding to the specified Java timezone.
     */
    private VTimeZone loadVTimeZone(final String id) throws IOException, ParserException {
        final URL resource = ResourceLoader.getResource(resourcePrefix + id + ".ics");
        if (resource != null) {
            final CalendarBuilder builder = new CalendarBuilder();
            final Calendar calendar = builder.build(resource.openStream());
            final VTimeZone vTimeZone = (VTimeZone) calendar.getComponent(Component.VTIMEZONE);
            // load any available updates for the timezone.. can be explicility disabled via configuration
            if (null != vTimeZone && !"false".equals(Configurator.getProperty(UPDATE_ENABLED))) {
                return updateDefinition(vTimeZone);
            }
            return vTimeZone;
        }
        return null;
    }

    /**
     * @param vTimeZone
     * @return
     */
    private VTimeZone updateDefinition(VTimeZone vTimeZone) {
        final TzUrl tzUrl = vTimeZone.getTimeZoneUrl();
        if (tzUrl != null) {
            HttpClientService service = HttpClientServiceProvider.getHttpClientService();
            if (service == null) {
                logFailedRetrieval(vTimeZone.getTimeZoneId().getValue(), new Exception("No such service: " + HttpClientService.class.getName()));
                return vTimeZone;
            }

            HttpClient httpClient = service.getHttpClient(TimeZoneRegistryHttpProperties.getHttpClientId());

            HttpGet request = null;
            HttpResponse httpResponse = null;
            InputStream inputStream = null;
            try {
                request = new HttpGet(tzUrl.getUri());
                httpResponse = httpClient.execute(request);

                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    logFailedRetrieval(vTimeZone.getTimeZoneId().getValue(), new Exception("Invalid status code: " + httpResponse.getStatusLine()));
                    return vTimeZone;
                }

                HttpEntity entity = httpResponse.getEntity();
                if (entity == null) {
                    logFailedRetrieval(vTimeZone.getTimeZoneId().getValue(), new Exception("Missing request body"));
                    return vTimeZone;
                }

                inputStream = entity.getContent();
                final CalendarBuilder builder = new CalendarBuilder();
                final Calendar calendar = builder.build(inputStream);
                final VTimeZone updatedVTimeZone = (VTimeZone) calendar.getComponent(Component.VTIMEZONE);
                if (updatedVTimeZone != null) {
                    return updatedVTimeZone;
                }
            }
            catch (Exception e) {
                logFailedRetrieval(vTimeZone.getTimeZoneId().getValue(), e);
            }
            finally {
                if (null != inputStream) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        LoggerFactory.getLogger(TimeZoneRegistryImpl.class).debug("Error closing stream", e);
                    }
                }
            }
        }
        return vTimeZone;
    }
    
    private static void logFailedRetrieval(String timeZoneId, Exception e ) {
        Logger log = LoggerFactory.getLogger(TimeZoneRegistryImpl.class);
        log.warn("Unable to retrieve updates for timezone: {}", timeZoneId, e);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class Reference<T> {

        @SuppressWarnings("rawtypes")
        private static final Reference EMPTY_REFERENCE = new Reference<>(null) {

            @Override
            Object get() {
                return null;
            }
        };

        /**
         * Gets the reference for given value or empty reference if value is <code>null</code>.
         *
         * @param <T> The type of the value
         * @param value The value or <code>null</code>
         * @return The reference or empty
         */
        @SuppressWarnings("unchecked")
        static final <T> Reference<T> referenceFor(T value) {
            return value == null ? (Reference<T>) EMPTY_REFERENCE : new Reference<>(value);
        }

        // ------------------------------------------------------------------------------------

        private final T value;

        /**
         * Initializes a new reference.
         * 
         * @param value The value or <code>null</code>
         */
        private Reference(T value) {
            super();
            this.value = value;
        }

        /**
         * Gets the value
         *
         * @return The value or <code>null</code>
         */
        T get() {
            return value;
        }
    }

}
