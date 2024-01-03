/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package javax.mail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import javax.mail.osgi.BundleResourceLoader;
import javax.mail.util.LineInputStream;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Utility class for loading protocol providers from resource files.
 *
 * @author Thorben Betten
 */
public class ProviderResourceUtility {

    private static final String confDir;

    static {
        String dir = null;
        try {
            dir = AccessController.doPrivileged(new PrivilegedAction<String>() {

                @Override
                public String run() {
                    String home = System.getProperty("java.home");
                    String newdir = home + File.separator + "conf";
                    File conf = new File(newdir);
                    return conf.exists() ? (newdir + File.separator) : (home + File.separator + "lib" + File.separator);
                }
            });
        } catch (Exception ex) {
            // ignore any exceptions
        }
        confDir = dir;
    }

    private static final AtomicReference<List<Provider>> loadedProviders = new AtomicReference<List<Provider>>(null);
    private static final AtomicReference<Map<String, String>> loadedAddressMap = new AtomicReference<Map<String, String>>(null);

    /**
     * Initializes a new {@link ProviderResourceUtility}.
     */
    private ProviderResourceUtility() {
        super();
    }
    
    /**
     * Resets the state of this utility class.
     */
    public static void reset() {
        loadedProviders.set(null);
        loadedAddressMap.set(null);
    }

    /**
     * load maps in reverse order of preference so that the preferred
     * map is loaded last since its entries will override the previous ones
     */
    static void loadAddressMap(Class<?> cl, Session session, MailLogger logger) {
        Map<String, String> loadedAddressMap = ProviderResourceUtility.loadedAddressMap.get();
        if (loadedAddressMap == null) {
            synchronized (ProviderResourceUtility.loadedAddressMap) {
                loadedAddressMap = ProviderResourceUtility.loadedAddressMap.get();
                if (loadedAddressMap == null) {
                    final Properties addressMap = new Properties();
                    StreamLoader loader = new StreamLoader() {

                        @Override
                        public void load(InputStream is) throws IOException {
                            addressMap.load(is);
                        }
                    };

                    // load default META-INF/javamail.default.address.map from mail.jar
                    loadResource("/META-INF/javamail.default.address.map", cl, loader, true, logger);

                    // load the META-INF/javamail.address.map file supplied by an app
                    loadAllResources("META-INF/javamail.address.map", cl, loader, logger);

                    // load system-wide javamail.address.map from the
                    // <java.home>/{conf,lib} directory
                    try {
                        if (confDir != null) {
                            loadFile(confDir + "javamail.address.map", loader, logger);
                        }
                    } catch (SecurityException ex) {
                        // Ignore
                    }

                    if (addressMap.isEmpty()) {
                        logger.config("failed to load address map, using defaults");
                        addressMap.put("rfc822", "smtp");
                    }
                    loadedAddressMap = new HashMap<String, String>(addressMap.size());
                    for (Map.Entry<Object, Object> addressMapEntry : addressMap.entrySet()) {
                        loadedAddressMap.put(addressMapEntry.getKey().toString(), addressMapEntry.getValue().toString());
                    }
                    ProviderResourceUtility.loadedAddressMap.set(ImmutableMap.copyOf(loadedAddressMap));
                }
            }
        }
        
        session.addAddressMap(loadedAddressMap);
    }

    /**
     * Load the protocol providers config files.
     */
    static void loadProviders(Class<?> cl, Session session, MailLogger logger) {
        List<Provider> loadedProviders = ProviderResourceUtility.loadedProviders.get();
        if (loadedProviders == null) {
            synchronized (ProviderResourceUtility.loadedProviders) {
                loadedProviders = ProviderResourceUtility.loadedProviders.get();
                if (loadedProviders == null) {
                    List<Provider> providers = new ArrayList<>();
                    
                    StreamLoader loader = new StreamLoader() {
                        
                        @Override
                        public void load(InputStream is) throws IOException {
                            loadProvidersFromStream(is, providers, session, logger);
                        }
                    };
                    
                    // load system-wide javamail.providers from the
                    // <java.home>/{conf,lib} directory
                    try {
                        if (confDir != null) {
                            loadFile(confDir + "javamail.providers", loader, logger);
                        }
                    } catch (SecurityException ex) {
                        // Ignore
                    }
                    
                    // next, add all the non-default services
                    ServiceLoader<Provider> sl = ServiceLoader.load(Provider.class);
                    for (Provider p : sl) {
                        if (!Session.containsDefaultProvider(p)) {
                            providers.add(p);
                        }
                    }
                    
                    // load the META-INF/javamail.providers file supplied by an application
                    loadAllResources("META-INF/javamail.providers", cl, loader, logger);
                    
                    // load default META-INF/javamail.default.providers from mail.jar file
                    loadResource("/META-INF/javamail.default.providers", cl, loader, false, logger);
                    
                    // finally, add all the default services
                    sl = ServiceLoader.load(Provider.class);
                    for (Provider p : sl) {
                        if (Session.containsDefaultProvider(p)) {
                            providers.add(p);
                        }
                    }
                    
                    /*
                     * If we haven't loaded any providers, fake it.
                     */
                    if (providers.isEmpty()) {
                        logger.config("failed to load any providers, using defaults");
                        // failed to load any providers, initialize with our defaults
                        providers.add(new Provider(Provider.Type.STORE, "imap", "com.sun.mail.imap.IMAPStore", "Oracle", Version.version));
                        providers.add(new Provider(Provider.Type.STORE, "imaps", "com.sun.mail.imap.IMAPSSLStore", "Oracle", Version.version));
                        providers.add(new Provider(Provider.Type.STORE, "pop3", "com.sun.mail.pop3.POP3Store", "Oracle", Version.version));
                        providers.add(new Provider(Provider.Type.STORE, "pop3s", "com.sun.mail.pop3.POP3SSLStore", "Oracle", Version.version));
                        providers.add(new Provider(Provider.Type.TRANSPORT, "smtp", "com.sun.mail.smtp.SMTPTransport", "Oracle", Version.version));
                        providers.add(new Provider(Provider.Type.TRANSPORT, "smtps", "com.sun.mail.smtp.SMTPSSLTransport", "Oracle", Version.version));
                    }
                    
                    if (logger.isLoggable(Level.CONFIG)) {
                        // dump the output of the tables for debugging
                        Map<String, Provider> providersByProtocol = new HashMap<>(providers.size());
                        Map<String, Provider> providersByClassName = new HashMap<>(providers.size());
                        for (Provider provider : providers) {
                            providersByClassName.put(provider.getClassName(), provider);
                            if (!providersByProtocol.containsKey(provider.getProtocol())) {
                                providersByProtocol.put(provider.getProtocol(), provider);
                            }
                        }
                        
                        logger.config("Tables of loaded providers");
                        logger.config("Providers Listed By Class Name: " + providersByClassName.toString());
                        logger.config("Providers Listed By Protocol: " + providersByProtocol.toString());
                    }
                    loadedProviders = providers;
                    ProviderResourceUtility.loadedProviders.set(ImmutableList.copyOf(loadedProviders));
                }
            }
        }

        synchronized (session) {            
            for (Provider provider : loadedProviders) {
                session.addProvider(provider);
            }
        }
    }

    static void loadProvidersFromStream(InputStream is, List<Provider> providers, Session session, MailLogger logger) throws IOException {
        if (is != null) {
            LineInputStream lis = session.getStreamProvider().inputLineStream(is, false);
            String currLine;

            // load and process one line at a time using LineInputStream
            while ((currLine = lis.readLine()) != null) {

                if (currLine.startsWith("#")) {
                    continue;
                }
                if (currLine.trim().length() == 0) {
                    continue;   // skip blank line
                }
                Provider.Type type = null;
                String protocol = null, className = null;
                String vendor = null, version = null;

                // separate line into key-value tuples
                StringTokenizer tuples = new StringTokenizer(currLine, ";");
                while (tuples.hasMoreTokens()) {
                    String currTuple = tuples.nextToken().trim();

                    // set the value of each attribute based on its key
                    int sep = currTuple.indexOf('=');
                    if (currTuple.startsWith("protocol=")) {
                        protocol = currTuple.substring(sep + 1);
                    } else if (currTuple.startsWith("type=")) {
                        String strType = currTuple.substring(sep + 1);
                        if (strType.equalsIgnoreCase("store")) {
                            type = Provider.Type.STORE;
                        } else if (strType.equalsIgnoreCase("transport")) {
                            type = Provider.Type.TRANSPORT;
                        }
                    } else if (currTuple.startsWith("class=")) {
                        className = currTuple.substring(sep + 1);
                    } else if (currTuple.startsWith("vendor=")) {
                        vendor = currTuple.substring(sep + 1);
                    } else if (currTuple.startsWith("version=")) {
                        version = currTuple.substring(sep + 1);
                    }
                }

                // check if a valid Provider; else, continue
                if (type == null || protocol == null || className == null || protocol.length() <= 0 || className.length() <= 0) {

                    logger.log(Level.CONFIG, "Bad provider entry: {0}", currLine);
                    continue;
                }
                Provider provider = new Provider(type, protocol, className, vendor, version);

                // add the newly-created Provider to the lookup tables
                providers.add(provider);
            }
        }
    }

    /**
     * Load from the named file.
     */
    private static void loadFile(String name, StreamLoader loader, MailLogger logger) {
        FileInputStream fis = null;
        InputStream clis = null;
        try {
            fis = new FileInputStream(name);
            clis = new BufferedInputStream(fis);
            loader.load(clis);
            logger.log(Level.CONFIG, "successfully loaded file: {0}", name);
        } catch (FileNotFoundException fex) {
            // ignore it
        } catch (IOException e) {
            if (logger.isLoggable(Level.CONFIG)) {
                logger.log(Level.CONFIG, "not loading file: " + name, e);
            }
        } catch (SecurityException sex) {
            if (logger.isLoggable(Level.CONFIG)) {
                logger.log(Level.CONFIG, "not loading file: " + name, sex);
            }
        } finally {
            if (clis != null) {
                try {
                    clis.close();
                } catch (Exception ex) {
                    // Ignore
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception ex) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Load from the named resource.
     */
    private static void loadResource(String name, Class<?> cl, StreamLoader loader, boolean expected, MailLogger logger) {
        InputStream clis = null;
        try {
            clis = getResourceAsStream(cl, name);
            if (clis != null) {
                loader.load(clis);
                logger.log(Level.CONFIG, "successfully loaded resource: {0}", name);
            } else {
                if (expected) {
                    logger.log(Level.WARNING, "expected resource not found: {0}", name);
                }
            }
        } catch (IOException e) {
            logger.log(Level.CONFIG, "Exception loading resource", e);
        } catch (SecurityException sex) {
            logger.log(Level.CONFIG, "Exception loading resource", sex);
        } finally {
            try {
                if (clis != null) {
                    clis.close();
                }
            } catch (IOException ex) {
                // Ignore
            }
        }
    }

    /**
     * Load all of the named resource.
     */
    private static void loadAllResources(String name, Class<?> cl, StreamLoader loader, MailLogger logger) {
        boolean anyLoaded = false;
        try {
            URL[] urls;
            ClassLoader cld = null;
            // First try the "application's" class loader.
            cld = getContextClassLoader();
            if (cld == null) {
                cld = cl.getClassLoader();
            }
            if (cld != null) {
                urls = getResources(cld, name);
            } else {
                urls = getSystemResources(name);
            }
            if (urls != null) {
                for (int i = 0; i < urls.length; i++) {
                    URL url = urls[i];
                    InputStream clis = null;
                    logger.log(Level.CONFIG, "URL {0}", url);
                    try {
                        clis = openStream(url);
                        if (clis != null) {
                            loader.load(clis);
                            anyLoaded = true;
                            logger.log(Level.CONFIG, "successfully loaded resource: {0}", url);
                        } else {
                            logger.log(Level.CONFIG, "not loading resource: {0}", url);
                        }
                    } catch (FileNotFoundException fex) {
                        // ignore it
                    } catch (IOException ioex) {
                        logger.log(Level.CONFIG, "Exception loading resource", ioex);
                    } catch (SecurityException sex) {
                        logger.log(Level.CONFIG, "Exception loading resource", sex);
                    } finally {
                        try {
                            if (clis != null) {
                                clis.close();
                            }
                        } catch (IOException cex) {
                            // Ignore
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.log(Level.CONFIG, "Exception loading resource", ex);
        }

        // if failed to load anything, fall back to old technique, just in case
        if (!anyLoaded) {
            /*
             * logger.config("!anyLoaded");
             */
            loadResource("/" + name, cl, loader, false, logger);
        }
    }

    /*
     * Following are security related methods that work on JDK 1.2 or newer.
     */

    static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

            @Override
            public ClassLoader run() {
                ClassLoader cl = null;
                try {
                    cl = Thread.currentThread().getContextClassLoader();
                } catch (SecurityException ex) {
                    // Ignore
                }
                return cl;
            }
        });
    }

    private static InputStream getResourceAsStream(final Class<?> c, final String name) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {

                @Override
                public InputStream run() throws IOException {
                    try {
                        org.osgi.framework.Bundle bundle = javax.mail.Session.BUNDLE_HOLDER.get();
                        if (null == bundle) {
                            return c.getResourceAsStream(name);
                        }
                        InputStream in = new BundleResourceLoader(bundle).getResourceAsStream(name.startsWith("/") ? name.substring(1) : name);
                        return null == in ? c.getResourceAsStream(name) : in;
                    } catch (RuntimeException e) {
                        // gracefully handle ClassLoader bugs (Tomcat)
                        IOException ioex = new IOException("ClassLoader.getResourceAsStream failed");
                        ioex.initCause(e);
                        throw ioex;
                    }
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    }

    private static URL[] getResources(final ClassLoader cl, final String name) {
        return AccessController.doPrivileged(new PrivilegedAction<URL[]>() {

            @Override
            public URL[] run() {
                URL[] ret = null;
                try {
                    java.util.List<URL> v = java.util.Collections.list(cl.getResources(name));
                    if (!v.isEmpty()) {
                        ret = new URL[v.size()];
                        v.toArray(ret);
                    }
                } catch (IOException ioex) {
                    // Ignore
                } catch (SecurityException ex) {
                    // Ignore
                }
                return ret;
            }
        });
    }

    private static URL[] getSystemResources(final String name) {
        return AccessController.doPrivileged(new PrivilegedAction<URL[]>() {

            @Override
            public URL[] run() {
                URL[] ret = null;
                try {
                    java.util.List<URL> v = java.util.Collections.list(ClassLoader.getSystemResources(name));
                    if (!v.isEmpty()) {
                        ret = new URL[v.size()];
                        v.toArray(ret);
                    }
                } catch (IOException ioex) {
                    // Ignore
                } catch (SecurityException ex) {
                    // Ignore
                }
                return ret;
            }
        });
    }

    private static InputStream openStream(final URL url) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {

                @Override
                public InputStream run() throws IOException {
                    return url.openStream();
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getException();
        }
    }

}
