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

package com.openexchange.config;

import static org.slf4j.LoggerFactory.getLogger;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.io.Closeables;
import com.openexchange.annotation.NonNull;
import com.openexchange.annotation.Nullable;
import com.openexchange.exception.OXException;

/**
 * {@link MockConfigurationService}
 *
 * @author <a href="mailto:pascal.bleser@open-xchange.com">Pascal Bleser</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class MockConfigurationService implements ConfigurationService, AutoCloseable, Closeable {

    private static final String DEFAULT_PROP_FILENAME = MockConfigurationService.class.getSimpleName() + ".properties";
    private static final File DEFAULT_PROP_FILE = new File(DEFAULT_PROP_FILENAME);
    
    /**
     * The logger constant.
     */
    private static final Logger LOG = getLogger(MockConfigurationService.class);

    @SuppressWarnings("unused")
    private static final class PropertyFileFilter implements FileFilter {

        private final String ext;
        private final String mpasswd;

        PropertyFileFilter() {
            super();
            ext = ".properties";
            mpasswd = "mpasswd";
        }

        @Override
        public boolean accept(final File pathname) {
            return pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(ext) || mpasswd.equals(pathname.getName());
        }

    }
    
    private final Lock fakeRootLock = new ReentrantLock();
    private final AtomicReference<Path> fakeRoot = new AtomicReference<Path>(null);
    
    public void setupFakeRootDir() {
        getFakeRootDir();
    }
    
    private Path getFakeRootDir() {
        fakeRootLock.lock();
        try {
            final Path root = fakeRoot.get();
            if (root == null) {
                final Path path = Files.createTempDirectory(this.getClass().getName() + "-fake-config-root-");
                final File dir = path.toFile();
                LOG.info("creating fake config root under temporary directory '{}'", dir.getAbsolutePath());
                if (System.getProperty("openexchange.propdir") == null) {
                    System.setProperty("openexchange.propdir", dir.getAbsolutePath());
                }
                dir.deleteOnExit();
                fakeRoot.set(path);
                for (Map.Entry<File, Properties> e : this.propertiesByFile.entrySet()) {
                    final File origFile = e.getKey();
                    final File actualFile = new File(dir, origFile.getName());
                    try (FileWriter fw = new FileWriter(actualFile)) {
                        e.getValue().store(fw, "generated by " + this.getClass().getName());
                        LOG.info("+ fake properties file '{}'", actualFile.getAbsolutePath());
                    }
                }
                for (final String rpath : this.extraFiles.keySet()) {
                    final File rFile = new File(rpath);
                    final File extraDir = new File(dir, rFile.getParent());
                    final File actualFile = new File(extraDir, rFile.getName());
                    extraDir.mkdir();
                    extraDir.deleteOnExit();
                    try (FileOutputStream fos = new FileOutputStream(actualFile)) {
                        fos.write(this.extraFiles.get(rpath).getBytes(Charset.forName("UTF-8")));
                        LOG.info("+ fake properties file '{}'", actualFile.getAbsolutePath());
                    }
                }
                return path;
            } else {
                return root;
            }
        } catch (final IOException e) {
            throw new IOError(e);
        } finally {
            fakeRootLock.unlock();
        }
    }
    
    private void cleanupFakeRootDir() {
        fakeRootLock.lock();
        try {
            final Path root = fakeRoot.get();
            if (root != null) {
                // Delete root recursively (rm -rf).
                // https://www.baeldung.com/java-delete-directory
                // there is a more concise way with Java 8 and NIO2 but using a
                // NIO 1 (Java 7) implementation here to be able to use this for
                // core 7.8.x as well:
                LOG.info("deleting fake config root temporary directory '{}'", root.toAbsolutePath().toString());
                Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        LOG.info("- deleting directory '{}'", dir.toAbsolutePath().toString());
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        LOG.info("- deleting file      '{}'", file.toAbsolutePath().toString());
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                });
                fakeRoot.set(null);
            }
        } catch (final IOException e) {
            throw new IOError(e);
        } finally {
            fakeRootLock.unlock();
        }
    }
    

    private /*static*/ final String[] getDirectories() {
        return new String[] {getFakeRootDir().toAbsolutePath().toString()};
        // Collect "openexchange.propdir" system properties
        /*
        List<String> properties = new ArrayList<String>(4);
        properties.add(SYSTEM_PROP_OPENXCHANGE_PROPDIR);
        boolean checkNext;
        int i = 2;
        do {
            checkNext = false;
            String sysProp = System.getProperty(new StringBuilder("openexchange.propdir").append(i++).toString());
            if (null != sysProp) {
                properties.add(sysProp);
                checkNext = true;
            }
        } while (checkNext);
        return properties.toArray(new String[properties.size()]);
        */
    }

    private static interface FileNameMatcher {

        boolean matches(String filename, File file);
    }

    private static final FileNameMatcher PATH_MATCHER = new FileNameMatcher() {

        @Override
        public boolean matches(String filename, File file) {
            return file.getPath().endsWith(filename);
        }
    };

    private static final FileNameMatcher NAME_MATCHER = new FileNameMatcher() {

        @Override
        public boolean matches(String filename, File file) {
            return file.getName().equals(filename);
        }
    };

    /*-
     * -------------------------------------------------- Member stuff ---------------------------------------------------------
     */

    /** The <code>ForcedReloadable</code> services. */
    private final List<ForcedReloadable> forcedReloadables;

    /** The <code>Reloadable</code> services in this list match all properties. */
    private final List<Reloadable> matchingAllProperties;

    /**
     * This is a map for exact property name matches. The key is the topic,
     * the value is a list of <code>Reloadable</code> services.
     */
    private final Map<String, List<Reloadable>> matchingProperty;

    /**
     * This is a map for wild-card property names. The key is the prefix of the property name,
     * the value is a list of <code>Reloadable</code> services
     */
    private final Map<String, List<Reloadable>> matchingPrefixProperty;

    /**
     * This is a map for file names. The key is the file name,
     * the value is a list of <code>Reloadable</code> services
     */
    private final Map<String, List<Reloadable>> matchingFile;

    private final Map<String, String> texts;

    private final File[] dirs;

    /** Maps file paths of the .properties file to their properties. */
    private final Map<File, Properties> propertiesByFile;

    /** Maps file paths of the .properties file to their properties. */
    private final Map<File, Properties> TBRPropertiesByFile;

    /** Maps property names to their values. */
    private final Map<String, String> properties;

    /** Maps property names to their values. which will be moved to properties once reloadConfiguration is executed */
    private final Map<String, String> TBRProperties;

    /** Maps property names to the file path of the .properties file containing the property. */
    private final Map<String, String> propertiesFiles;

    /** Maps property names to the file path of the .properties file containing the property once reloadConfiguration is executed . */
    private final Map<String, String> TBRPropertiesFiles;

    private final Map<String, String> extraFiles;

    public static final @NonNull MockConfigurationService forValues(final Object... parameters) {
        final ImmutableList.Builder<ConfigurationService> delegateBuilder = ImmutableList.builder();
        final Map<String, String> b = new HashMap<>();
        int i = 0;
        while (i < parameters.length) {
            final Object obj = parameters[i];
            if (obj != null && obj instanceof ConfigurationService) {
                delegateBuilder.add((ConfigurationService) obj);
                i++;
            } else {
                final String key = obj != null ? obj.toString() : null;
                i++;
                if (i < parameters.length) {
                    final @Nullable String value = parameters[i++].toString();
                    if (key != null && value != null) {
                        b.put(key, value);
                    }
                } else {
                    throw new IllegalArgumentException("uneven number of parameters");
                }
            }
        }
        return new MockConfigurationService(b, delegateBuilder.build());
    }

    @SuppressWarnings("unused")
	private final Collection<ConfigurationService> delegates;

    public MockConfigurationService() {
    	this(new HashMap<String, String>(), new ArrayList<ConfigurationService>());
    }
    
    public MockConfigurationService(final Map<String, String> properties) {
    	this(properties, new ArrayList<ConfigurationService>());
    }
    
    public MockConfigurationService(final Map<String, String> properties, final Collection<ConfigurationService> delegates) {
        this.properties = properties;
        this.TBRProperties = new HashMap<>(properties);
        this.forcedReloadables = new ArrayList<>();
        this.matchingAllProperties = new ArrayList<>();
        this.matchingProperty = new HashMap<>();
        this.matchingPrefixProperty = new HashMap<>();
        this.matchingFile = new HashMap<>();
        this.texts = new HashMap<>();
        this.dirs = new File[0];
        this.propertiesByFile = new HashMap<>();
        this.TBRPropertiesByFile = new HashMap<>();
        this.propertiesFiles = new HashMap<>();
        this.TBRPropertiesFiles = new HashMap<>();
        this.delegates = delegates;
        this.extraFiles = new HashMap<>();
        
        final Properties props = new Properties();
        final Properties TBRProps = new Properties();
        props.putAll(properties);
        TBRProps.putAll(properties);
        this.propertiesByFile.put(DEFAULT_PROP_FILE, props);
        this.TBRPropertiesByFile.put(DEFAULT_PROP_FILE, TBRProps);
        for (final Map.Entry<String, String> e : properties.entrySet()) {
            this.propertiesFiles.put(e.getKey(), DEFAULT_PROP_FILENAME);
            this.TBRPropertiesFiles.put(e.getKey(), DEFAULT_PROP_FILENAME);
        }
    }

    public void addExtraPropertyFile(final String relativePath, final String content) {
        this.extraFiles.put(relativePath, content);
    }

    @Override
    public Filter getFilterFromProperty(final String name) {
        final String value = properties.get(name);
        if (null == value) {
            return null;
        }
        return new WildcardFilter(value);
    }

    @Override
    public String getProperty(final String name) {
        return properties.get(name);
    }

    @Override
    public String getProperty(final String name, final String defaultValue) {
        final String value = properties.get(name);
        return null == value ? defaultValue : value;
    }

    @Override
    public List<String> getProperty(String name, String defaultValue, String separator) {
        String property = getProperty(name, defaultValue);
        return splitAndTrim(property, separator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getFile(final String filename) {

        boolean isPath = filename.indexOf(File.separatorChar) >= 0;
        FileNameMatcher matcher = isPath ? PATH_MATCHER : NAME_MATCHER;

        for (Map.Entry<File, Properties> entry : propertiesByFile.entrySet()) {
            if (matcher.matches(filename, entry.getKey())) {
                Properties retval = new Properties();
                retval.putAll(entry.getValue());
                return retval;
            }
        }

        return new Properties();
    }

    @Override
    public Map<String, String> getProperties(final PropertyFilter filter) throws OXException {
        if (null == filter) {
            return new HashMap<String, String>(properties);
        }
        final Map<String, String> ret = new LinkedHashMap<String, String>(32);
        for (final Entry<String, String> entry : this.properties.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (filter.accept(key, value)) {
                ret.put(key, value);
            }
        }
        return ret;
    }

    @Override
    public Properties getPropertiesInFolder(final String folderName) {
        Properties retval = new Properties();
        for (File dir : dirs) {
            String fldName = dir.getAbsolutePath() + File.separatorChar + folderName + File.separatorChar;
            for (Iterator<Entry<String, String>> iter = propertiesFiles.entrySet().iterator(); iter.hasNext();) {
                Map.Entry<String, String> entry = iter.next();
                if (entry.getValue().startsWith(fldName)) {
                    String value = getProperty(entry.getKey());
                    retval.put(entry.getKey(), value);
                }
            }
        }
        return retval;
    }

    @Override
    public boolean getBoolProperty(final String name, final boolean defaultValue) {
        final String prop = properties.get(name);
        if (null != prop) {
            return Boolean.parseBoolean(prop.trim());
        }
        return defaultValue;
    }

    @Override
    public int getIntProperty(final String name, final int defaultValue) {
        final String prop = properties.get(name);
        if (prop != null) {
            try {
                return Integer.parseInt(prop.trim());
            } catch (final NumberFormatException e) {
                LOG.trace("", e);
            }
        }
        return defaultValue;
    }

    @SuppressWarnings("null")
    @Override
    public Iterator<String> propertyNames() {
        return Iterators.unmodifiableIterator(properties.keySet().iterator());
    }

    @Override
    public int size() {
        return properties.size();
    }

    @Override
    public File getFileByName(final String fileName) {
        if (null == fileName) {
            return null;
        }
        for (final String dir : getDirectories()) {
            final File f = traverseForFile(new File(dir), fileName);
            if (f != null) {
                return f;
            }
        }
        /*
         * Try guessing the filename separator
         */
        String fn;
        int pos;
        if ((pos = fileName.lastIndexOf('/')) >= 0 || (pos = fileName.lastIndexOf('\\')) >= 0) {
            fn = fileName.substring(pos + 1);
        } else {
            LOG.warn("No such file: {}", fileName);
            return null;
        }
        for (final String dir : getDirectories()) {
            final File f = traverseForFile(new File(dir), fn);
            if (f != null) {
                return f;
            }
        }
        LOG.warn("No such file: {}", fileName);
        return null;
    }

    private File traverseForFile(final File file, final String fileName) {
        if (null == file) {
            return null;
        }
        if (file.isFile()) {
            if (fileName.equals(file.getName())) {
                // Found
                return file;
            }
            return null;
        }
        final File[] subs = file.listFiles();
        if (subs != null) {
            for (final File sub : subs) {
                final File f = traverseForFile(sub, fileName);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }

    @Override
    public File getDirectory(final String directoryName) {
        if (null == directoryName) {
            return null;
        }
        for (final String dir : getDirectories()) {
            final File fdir = traverseForDir(new File(dir), directoryName);
            if (fdir != null) {
                return fdir;
            }
        }
        LOG.warn("No such directory: {}", directoryName);
        return null;
    }

    private File traverseForDir(final File file, final String directoryName) {
        if (null == file) {
            return null;
        }
        if (file.isDirectory() && directoryName.equals(file.getName())) {
            // Found
            return file;
        }
        final File[] subDirs = file.listFiles(new FileFilter() {

            @Override
            public boolean accept(final File file) {
                return file.isDirectory();
            }
        });
        if (subDirs != null) {
            // Check first-level sub-directories first
            for (final File subDir : subDirs) {
                if (subDir.isDirectory() && directoryName.equals(subDir.getName())) {
                    return subDir;
                }
            }
            // Then check recursively
            for (final File subDir : subDirs) {
                final File dir = traverseForDir(subDir, directoryName);
                if (dir != null) {
                    return dir;
                }
            }
        }
        return null;
    }

    @Override
    public String getText(final String fileName) {
        final String text = texts.get(fileName);
        if (text != null) {
            return text;
        }

        for (final String dir : getDirectories()) {
            final String s = traverse(new File(dir), fileName);
            if (s != null) {
                texts.put(fileName, s);
                return s;
            }
        }
        return null;
    }

    private String traverse(final File file, final String filename) {
        if (null == file) {
            return null;
        }
        if (file.isFile()) {
            if (file.getName().equals(filename)) {
                return readFile(file);
            }
            return null;
        }
        final File[] files = file.listFiles();
        if (files != null) {
            for (final File f : files) {
                final String s = traverse(f, filename);
                if (s != null) {
                    return s;
                }
            }
        }
        return null;
    }

    String readFile(final File file) {
        Reader reader = null;
        try {
            reader = new FileReader(file);

            StringBuilder builder = new StringBuilder((int) file.length());
            int buflen = 8192;
            char[] cbuf = new char[buflen];

            for (int read; (read = reader.read(cbuf, 0, buflen)) > 0;) {
                builder.append(cbuf, 0, read);
            }
            return builder.toString();
        } catch (final IOException x) {
            LOG.error("Can't read file: {}", file, x);
            return null;
        } finally {
            if (reader != null) {
                Closeables.closeQuietly(reader);
            }
        }
    }

    @Override
    public Object getYaml(final String filename) {
        throw new UnsupportedOperationException("getYaml");
    }

    @Override
    public Map<String, Object> getYamlInFolder(final String folderName) {
        throw new UnsupportedOperationException("getYamlInFolder");
    }

    /**
     * Propagates the reloaded configuration among registered listeners.
     */
    @SuppressWarnings("unchecked")
    public void reloadConfiguration() {
        LOG.info("Reloading configuration...");

        // Copy current content to get associated files on check for expired PropertyWatchers
        final Map<File, Properties> oldPropertiesByFile = new HashMap<File, Properties>(propertiesByFile);

        // Clear maps
        properties.clear();
        propertiesByFile.clear();
        propertiesFiles.clear();
        texts.clear();

        // (Re-)load configuration
        properties.putAll(TBRProperties);
        propertiesByFile.putAll(TBRPropertiesByFile);
        propertiesFiles.putAll(TBRPropertiesFiles);

        // replace To be reloaded with new copies
        for (Entry<File, Properties> entry : propertiesByFile.entrySet()) {
            Properties props = new Properties();
            props.putAll((Map<? extends Object, ? extends Object>) entry.getValue().clone());
            TBRPropertiesByFile.put(entry.getKey(), props);
        }

        // Check if properties have been changed, execute only forced ones if not
        Set<String> namesOfChangedProperties = new HashSet<>(512);
        Set<File> changes = getChanges(oldPropertiesByFile, namesOfChangedProperties);

        Set<Reloadable> toTrigger = new LinkedHashSet<>(32);

        // Collect forced ones
        for (ForcedReloadable reloadable : forcedReloadables) {
            toTrigger.add(reloadable);
        }

        if (!changes.isEmpty()) {
            // Continue to reload
            LOG.info("Detected changes in the following configuration files: {}", changes);

            // Collect the ones interested in all
            for (Reloadable reloadable : matchingAllProperties) {
                toTrigger.add(reloadable);
            }

            // Collect the ones interested in properties
            for (String nameOfChangedProperties : namesOfChangedProperties) {
                // Now check for prefix matches
                if (!matchingPrefixProperty.isEmpty()) {
                    int pos = nameOfChangedProperties.lastIndexOf('.');
                    while (pos > 0) {
                        String prefix = nameOfChangedProperties.substring(0, pos);
                        List<Reloadable> interestedReloadables = matchingPrefixProperty.get(prefix);
                        if (null != interestedReloadables) {
                            toTrigger.addAll(interestedReloadables);
                        }
                        pos = prefix.lastIndexOf('.');
                    }
                }

                // Add the subscriptions for matching topic names
                {
                    List<Reloadable> interestedReloadables = matchingProperty.get(nameOfChangedProperties);
                    if (null != interestedReloadables) {
                        toTrigger.addAll(interestedReloadables);
                    }
                }
            }

            // Collect the ones interested in files
            for (File file : changes) {
                List<Reloadable> interestedReloadables = matchingFile.get(file.getName());
                if (null != interestedReloadables) {
                    toTrigger.addAll(interestedReloadables);
                }
            }
        } else {
            LOG.info("No changes in *.properties, *.xml, or *.yaml configuration files detected");
        }

        // Trigger collected ones
        for (Reloadable reloadable : toTrigger) {
            try {
                reloadable.reloadConfiguration(this);
            } catch (Exception e) {
                LOG.warn("Failed to let reloaded configuration be handled by: {}", reloadable.getClass().getName(), e);
            }
        }
    }

    @NonNull
    private Set<File> getChanges(Map<File, Properties> oldPropertiesByFile, Set<String> namesOfChangedProperties) {
        Set<File> result = new HashSet<File>(oldPropertiesByFile.size());

        // Check for changes in .properties files
        for (Map.Entry<File, Properties> newEntry : propertiesByFile.entrySet()) {
            File pathname = newEntry.getKey();
            Properties newProperties = newEntry.getValue();
            Properties oldProperties = oldPropertiesByFile.get(pathname);
            if (null == oldProperties) {
                // New .properties file
                result.add(pathname);
                for (Object propertyName : newProperties.keySet()) {
                    namesOfChangedProperties.add(propertyName.toString());
                }
            } else if (!newProperties.equals(oldProperties)) {
                // Changed .properties file
                result.add(pathname);

                // Removed ones
                {
                    Set<Object> removedKeys = new HashSet<>(oldProperties.keySet());
                    removedKeys.removeAll(newProperties.keySet());
                    for (Object removedKey : removedKeys) {
                        namesOfChangedProperties.add(removedKey.toString());
                    }
                }

                // New ones or changed value
                Iterator<Map.Entry<Object, Object>> i = newProperties.entrySet().iterator();
                while (i.hasNext()) {
                    Map.Entry<Object, Object> e = i.next();
                    Object key = e.getKey();
                    Object value = e.getValue();
                    if (value == null) {
                        if (oldProperties.get(key) != null || !oldProperties.containsKey(key)) {
                            namesOfChangedProperties.add(key.toString());
                        }
                    } else {
                        if (!value.equals(oldProperties.get(key))) {
                            namesOfChangedProperties.add(key.toString());
                        }
                    }
                }
            }
        }
        {
            Set<File> removedFiles = new HashSet<File>(oldPropertiesByFile.keySet());
            removedFiles.removeAll(propertiesByFile.keySet());
            result.addAll(removedFiles);
        }

        return result;
    }

    private boolean isInterestedInAll(String[] propertiesOfInterest) {
        if (null == propertiesOfInterest || 0 == propertiesOfInterest.length) {
            // Reloadable does not indicate the properties of interest
            return true;
        }

        for (String poi : propertiesOfInterest) {
            if ("*".equals(poi)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds specified <code>Reloadable</code> instance.
     *
     * @param service The instance to add
     * @return <code>true</code> if successfully added; otherwise <code>false</code> if already present
     */
    @SuppressWarnings("null")
    public synchronized boolean addReloadable(Reloadable service) {
        if ((service instanceof ForcedReloadable)) {
            forcedReloadables.add((ForcedReloadable) service);
            return true;
        }

        Interests interests = service.getInterests();
        String[] propertiesOfInterest = null == interests ? null : interests.getPropertiesOfInterest();
        String[] configFileNames = null == interests ? null : interests.getConfigFileNames();

        boolean hasInterestForProperties = (null != propertiesOfInterest && propertiesOfInterest.length > 0);
        boolean hasInterestForFiles = (null != configFileNames && configFileNames.length > 0);

        // No interests at all?
        if (!hasInterestForProperties && !hasInterestForFiles) {
            // A Reloadable w/o any interests... Assume all
            matchingAllProperties.add(service);
            return true;
        }

        // Check interest for concrete properties
        if (hasInterestForProperties) {
            if (isInterestedInAll(propertiesOfInterest)) {
                matchingAllProperties.add(service);
            } else if (propertiesOfInterest != null) {
                for (String propertyName : propertiesOfInterest) {
                    Reloadables.validatePropertyName(propertyName);
                    if (propertyName.endsWith(".*")) {
                        // Wild-card property name: we remove the .*
                        String prefix = propertyName.substring(0, propertyName.length() - 2);
                        List<Reloadable> list = matchingPrefixProperty.get(prefix);
                        if (null == list) {
                            List<Reloadable> newList = new CopyOnWriteArrayList<>();
                            matchingPrefixProperty.put(prefix, newList);
                            list = newList;
                        }
                        list.add(service);
                    } else {
                        // Exact match
                        List<Reloadable> list = matchingProperty.get(propertyName);
                        if (null == list) {
                            List<Reloadable> newList = new CopyOnWriteArrayList<>();
                            matchingProperty.put(propertyName, newList);
                            list = newList;
                        }
                        list.add(service);
                    }
                }
            }
        }

        // Check interest for files
        if (hasInterestForFiles) {
        	if (configFileNames != null) {
	            for (String configFileName : configFileNames) {
	                Reloadables.validateFileName(configFileName);
	                List<Reloadable> list = matchingFile.get(configFileName);
	                if (null == list) {
	                    List<Reloadable> newList = new CopyOnWriteArrayList<>();
	                    matchingFile.put(configFileName, newList);
	                    list = newList;
	                }
	                list.add(service);
	            }
        	}
        }

        return true;
    }

    /**
     * Removes specified <code>Reloadable</code> instance.
     *
     * @param service The instance to remove
     */
    public synchronized void removeReloadable(Reloadable service) {
        matchingAllProperties.remove(service);

        for (Iterator<List<Reloadable>> it = matchingPrefixProperty.values().iterator(); it.hasNext();) {
            List<Reloadable> reloadables = it.next();
            if (reloadables.remove(service)) {
                if (reloadables.isEmpty()) {
                    it.remove();
                }
            }
        }

        for (Iterator<List<Reloadable>> it = matchingProperty.values().iterator(); it.hasNext();) {
            List<Reloadable> reloadables = it.next();
            if (reloadables.remove(service)) {
                if (reloadables.isEmpty()) {
                    it.remove();
                }
            }
        }

        for (Iterator<List<Reloadable>> it = matchingFile.values().iterator(); it.hasNext();) {
            List<Reloadable> reloadables = it.next();
            if (reloadables.remove(service)) {
                if (reloadables.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Gets all currently tracked <code>Reloadable</code> instances.
     *
     * @return The <code>Reloadable</code> instances
     */
    public Collection<Reloadable> getReloadables() {
        Set<Reloadable> tracked = new LinkedHashSet<>(32);
        tracked.addAll(forcedReloadables);
        tracked.addAll(matchingAllProperties);
        for (List<Reloadable> reloadables : matchingPrefixProperty.values()) {
            tracked.addAll(reloadables);
        }
        for (List<Reloadable> reloadables : matchingProperty.values()) {
            tracked.addAll(reloadables);
        }
        for (List<Reloadable> reloadables : matchingFile.values()) {
            tracked.addAll(reloadables);
        }
        return tracked;
    }

    private static List<String> splitAndTrim(String input, String separator) {
        if (input == null) {
            throw new IllegalArgumentException("Missing input");
        }
        if (isEmpty(input)) {
            return Collections.emptyList();
        }
        if (isEmpty(separator)) {
            throw new IllegalArgumentException("Missing separator");
        }

        try {
            String[] splits = input.split(separator);
            ArrayList<String> trimmedSplits = new ArrayList<String>(splits.length);
            for (String string : splits) {
                trimmedSplits.add(string.trim());
            }
            return trimmedSplits;
        } catch (PatternSyntaxException pse) {
            throw new IllegalArgumentException("Illegal pattern syntax", pse);
        }
    }

    private static boolean isEmpty(final String str) {
        if (null == str) {
            return true;
        }
        final int len = str.length();
        boolean isWhitespace = true;
        for (int i = len; isWhitespace && i-- > 0;) {
            isWhitespace = isWhitespace(str.charAt(i));
        }
        return isWhitespace;
    }

    private static boolean isWhitespace(final char c) {
        switch (c) {
            case 9: // 'unicode: 0009
            case 10: // 'unicode: 000A'
            case 11: // 'unicode: 000B'
            case 12: // 'unicode: 000C'
            case 13: // 'unicode: 000D'
            case 28: // 'unicode: 001C'
            case 29: // 'unicode: 001D'
            case 30: // 'unicode: 001E'
            case 31: // 'unicode: 001F'
            case ' ': // Space
                // case Character.SPACE_SEPARATOR:
                // case Character.LINE_SEPARATOR:
            case Character.PARAGRAPH_SEPARATOR:
                return true;
            default:
                return false;
        }
    }

    public void put(@NonNull String key, String value) {
        this.properties.put(key, value);
        this.propertiesFiles.put(key, DEFAULT_PROP_FILENAME);
        if (! this.propertiesByFile.containsKey(DEFAULT_PROP_FILE)) {
            this.propertiesByFile.put(DEFAULT_PROP_FILE, new Properties());
        }
        this.propertiesByFile.get(DEFAULT_PROP_FILE).put(key, value);
        putDelayed(key, value);
    }

    public void put(@NonNull String file, @NonNull String key, String value) {
        final File f = new File(file);
        this.properties.put(key, value);
        if (! this.propertiesByFile.containsKey(f)) {
            this.propertiesByFile.put(f, new Properties());
        }
        this.propertiesByFile.get(f).put(key, value);
        this.propertiesFiles.put(key, file);
        putDelayed(file, key, value);
    }

    public void remove(@NonNull String key) {
        this.properties.remove(key);
        removeDelayed(key);
    }

    public void removeDelayed(@NonNull String key) {
        this.TBRProperties.remove(key);
    }
    
    @Override
    public void close() throws IOException {
        cleanupFakeRootDir();
    }

    public void putAll(@NonNull String file, @NonNull Map<String, String> properties) {
        this.properties.putAll(properties);
        final File f = new File(file);
        if (! this.propertiesByFile.containsKey(f)) {
            this.propertiesByFile.put(f, new Properties());
        }
        for (final Map.Entry<String, String> e : properties.entrySet()) {
            this.propertiesFiles.put(e.getKey(), file);
            this.propertiesByFile.get(f).put(e.getKey(), e.getValue());
        }
        putAllDelayed(file, properties);
    }

    public void putDelayed(@NonNull String key, String value) {
        this.TBRProperties.put(key, value);
        this.TBRPropertiesFiles.put(key, DEFAULT_PROP_FILENAME);
        if (! this.TBRPropertiesByFile.containsKey(DEFAULT_PROP_FILE)) {
            this.TBRPropertiesByFile.put(DEFAULT_PROP_FILE, new Properties());
        }
        this.TBRPropertiesByFile.get(DEFAULT_PROP_FILE).put(key, value);
    }

    public void putDelayed(@NonNull String file, @NonNull String key, String value) {
        final File f = new File(file);
        this.TBRProperties.put(key, value);
        if (! this.TBRPropertiesByFile.containsKey(f)) {
            this.TBRPropertiesByFile.put(f, new Properties());
        }
        this.TBRPropertiesByFile.get(f).put(key, value);
        this.TBRPropertiesFiles.put(key, file);
    }

    public void putAllDelayed(@NonNull String file, @NonNull Map<String, String> properties) {
        this.TBRProperties.putAll(properties);
        final File f = new File(file);
        if (! this.TBRPropertiesByFile.containsKey(f)) {
            this.TBRPropertiesByFile.put(f, new Properties());
        }
        for (final Map.Entry<String, String> e : properties.entrySet()) {
            this.TBRPropertiesFiles.put(e.getKey(), file);
            this.TBRPropertiesByFile.get(f).put(e.getKey(), e.getValue());
        }
    }
}
