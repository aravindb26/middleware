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

package com.openexchange.mail.mime;

import static com.openexchange.java.Strings.toLowerCase;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.Reloadables;
import com.openexchange.configuration.SystemConfig;
import com.openexchange.java.SortableConcurrentList;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.java.util.ImmutablePair;
import com.openexchange.mail.config.MailReloadable;
import com.openexchange.mime.MimeTypeMap;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.systemproperties.SystemPropertiesUtils;

/**
 * {@link MimeType2ExtMap} - Maps MIME types to file extensions and vice versa.
 * <p>
 * This class looks in various places for MIME types file entries. When requests are made to look up MIME types or file extensions, it
 * searches MIME types files in the following order:
 * <ol>
 * <li>The file <i>.mime.types</i> in the user's home directory.</li>
 * <li>The file <i>&lt;java.home&gt;/lib/mime.types</i>.</li>
 * <li>The file or resources named <i>META-INF/mime.types</i>.</li>
 * <li>The file or resource named <i>META-INF/mimetypes.default</i>.</li>
 * <li>The file or resource denoted by property <i>MimeTypeFileName</i>.</li>
 * </ol>
 * <p>
 * Available as OSGi service through {@link MimeTypeMap} interface.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MimeType2ExtMap {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MimeType2ExtMap.class);

    /**
     * The pair of maps.
     * <ul>
     * <li>The file extension to MIME type mapping; e.g. <code>"txt"</code> --&gt; <code>"text/plain"</code>
     * <li>The MIME type to file extensions mapping; e.g. <code>"text/html"</code> --&gt; <code>"html", "htm"</code>
     * </ul>
     */
    private static final AtomicReference<ImmutablePair<ConcurrentMap<String, MimeTypeAssociation>, ConcurrentMap<String, List<String>>>> MAPS_REFERENCE = new AtomicReference<>(null);

    /**
     * No instance.
     */
    private MimeType2ExtMap() {
        super();
    }

    static {
        MailReloadable.getInstance().addReloadable(new Reloadable() {

            @Override
            public void reloadConfiguration(ConfigurationService configService) {
                reset();
                init();
            }

            @Override
            public Interests getInterests() {
                return Reloadables.interestsForProperties("MimeTypeFileName");
            }
        });
    }

    /**
     * Resets MIME type file map.
     */
    public static void reset() {
        MAPS_REFERENCE.set(null);
    }

    /**
     * Initializes the mappings.
     * <ul>
     * <li>The file extension to MIME type mapping; e.g. <code>"txt"</code> --&gt; <code>"text/plain"</code>
     * <li>The MIME type to file extensions mapping; e.g. <code>"text/html"</code> --&gt; <code>"html", "htm"</code>
     * </ul>
     *
     * @return The mappings
     */
    public static ImmutablePair<ConcurrentMap<String, MimeTypeAssociation>, ConcurrentMap<String, List<String>>> init() {
        ImmutablePair<ConcurrentMap<String, MimeTypeAssociation>, ConcurrentMap<String, List<String>>> pair = MAPS_REFERENCE.get();
        if (pair == null) {
            synchronized (MimeType2ExtMap.class) {
                pair = MAPS_REFERENCE.get();
                if (pair == null) {
                    pair = doInitialize();
                    MAPS_REFERENCE.set(pair);
                }
            }
        }
        return pair;
    }

    /**
     * Initializes MIME type file map.
     */
    private static ImmutablePair<ConcurrentMap<String, MimeTypeAssociation>, ConcurrentMap<String, List<String>>> doInitialize() {
        try {
            ConcurrentMap<String, MimeTypeAssociation> typeMapping = new ConcurrentHashMap<String, MimeTypeAssociation>(1024, 0.9f, 1);
            ConcurrentMap<String, List<String>> extMapping = new ConcurrentHashMap<String, List<String>>(1024, 0.9f, 1);

            StringBuilder sb = new StringBuilder(128);
            boolean debugEnabled = LOG.isDebugEnabled();
            {
                String homeDir = SystemPropertiesUtils.getProperty("user.home");
                if (homeDir != null) {
                    File file = new File(sb.append(homeDir).append(File.separatorChar).append(".mime.types").toString());
                    if (file.exists()) {
                        if (debugEnabled) {
                            LOG.debug("Loading MIME type file \"{}\"", file.getPath());
                        }
                        loadInternal(file, typeMapping, extMapping);
                    }
                }
            }
            {
                String javaHome = SystemPropertiesUtils.getProperty("java.home");
                if (javaHome != null) {
                    sb.setLength(0);
                    File file =
                        new File(sb.append(javaHome).append(File.separatorChar).append("lib").append(File.separator).append("mime.types").toString());
                    if (file.exists()) {
                        if (debugEnabled) {
                            LOG.debug("Loading MIME type file \"{}\"", file.getPath());
                        }
                        loadInternal(file, typeMapping, extMapping);
                    }
                }
            }
            {
                for (Enumeration<URL> e = ClassLoader.getSystemResources("META-INF/mime.types"); e.hasMoreElements();) {
                    URL url = e.nextElement();
                    if (debugEnabled) {
                        LOG.debug("Loading MIME type file \"{}\"", url.getFile());
                    }
                    loadInternal(url, typeMapping, extMapping);
                }
            }
            {
                for (Enumeration<URL> e = ClassLoader.getSystemResources("META-INF/mimetypes.default"); e.hasMoreElements();) {
                    URL url = e.nextElement();
                    if (debugEnabled) {
                        LOG.debug("Loading MIME type file \"{}\"", url.getFile());
                    }
                    loadInternal(url, typeMapping, extMapping);
                }
            }
            {
                String mimeTypesFileName = SystemConfig.getProperty(SystemConfig.Property.MimeTypeFileName);
                if ((mimeTypesFileName != null) && ((mimeTypesFileName = mimeTypesFileName.trim()).length() > 0)) {
                    ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    File file = null == service ? null : service.getFileByName(mimeTypesFileName);
                    if (null != file && file.exists()) {
                        if (debugEnabled) {
                            LOG.debug("Loading MIME type file \"{}\"", file.getPath());
                        }
                        loadInternal(file, typeMapping, extMapping);
                    }
                }

            }
            LOG.debug("MIMEType2ExtMap successfully initialized");
            return ImmutablePair.newInstance(typeMapping, extMapping);
        } catch (IOException e) {
            LOG.error("", e);
        }
        return null;
    }

    /**
     * Gets the file extension to MIME type mapping; e.g. <code>"txt"</code> --&gt; <code>"text/plain"</code>
     *
     * @return The file extension to MIME type mapping; e.g. <code>"txt"</code> --&gt; <code>"text/plain"</code>
     */
    private static ConcurrentMap<String, MimeTypeAssociation> getTypeMap() {
        return init().getFirst();
    }

    /**
     * Gets the MIME type to file extensions mapping; e.g. <code>"text/html"</code> --&gt; <code>"html", "htm"</code>
     *
     * @return The MIME type to file extensions mapping; e.g. <code>"text/html"</code> --&gt; <code>"html", "htm"</code>
     */
    private static ConcurrentMap<String, List<String>> getExtMap() {
        return init().getSecond();
    }

    /**
     * Adds specified MIME type to file type mapping; e.g <code>"image/png"</code> -&gt; <code>"png"</code>.
     *
     * @param mimeType The MIME type
     * @param fileExtension The file extension
     */
    public static void addMimeType(String mimeType, String fileExtension) {
        addMimeType(mimeType, Collections.singletonList(fileExtension));
    }

    /**
     * Adds specified MIME type to file type mapping; e.g <code>"image/jpeg"</code> -&gt; [ <code>"jpeg"</code>, <code>"jpg"</code>, <code>"jpe"</code> ].
     *
     * @param mimeType The MIME type
     * @param fileExtensions The file extensions
     */
    public static void addMimeType(String mimeType, List<String> fileExtensions) {
        ConcurrentMap<String, MimeTypeAssociation> tm = getTypeMap();
        for (String ext : fileExtensions) {
            addTypeMapping(ext, mimeType, tm);
        }

        ConcurrentMap<String, List<String>> em = getExtMap();
        List<String> list = em.get(mimeType);
        if (null == list) {
            List<String> nl = new CopyOnWriteArrayList<String>();
            list = em.putIfAbsent(mimeType, nl);
            if (null == list) {
                list = nl;
            }
        }
        for (String ext : fileExtensions) {
            if (!list.contains(ext)) {
                list.add(ext);
            }
        }
    }

    private static final String MIME_APPL_OCTET = MimeTypes.MIME_APPL_OCTET;

    /**
     * Gets the MIME type associated with given file.
     *
     * @param file The file
     * @return The MIME type associated with given file or <code>"application/octet-stream"</code> if none found
     */
    public static String getContentType(File file) {
        return getContentType(file.getName(), MIME_APPL_OCTET);
    }

    /**
     * Gets the MIME type associated with given file.
     *
     * @param file The file
     * @param fallBack The fall-back value to return in case file extension is unknown
     * @return The MIME type associated with given file or <code>"application/octet-stream"</code> if none found
     */
    public static String getContentType(File file, String fallBack) {
        return getContentType(file.getName(), fallBack);
    }

    /**
     * Gets the MIME type associated with given file name.
     * <p>
     * This is a convenience method that invokes {@link #getContentType(String, String)} with latter argument set to <code>"application/octet-stream"</code>.
     *
     * @param fileName The file name; e.g. <code>"file.html"</code>
     * @return The MIME type associated with given file name or <code>"application/octet-stream"</code> if none found
     * @see #getContentType(String, String)
     */
    public static String getContentType(String fileName) {
        return getContentType(fileName, MIME_APPL_OCTET);
    }

    /**
     * Gets the MIME type associated with given file name.
     *
     * @param fileName The file name; e.g. <code>"file.html"</code>
     * @param fallBack The fall-back value to return in case file extension is unknown
     * @return The MIME type associated with given file name or <code>fallBack</code> if none found
     */
    public static String getContentType(String fileName, String fallBack) {
        if (Strings.isEmpty(fileName)) {
            return fallBack;
        }
        String fn = Strings.unquote(fileName);
        int pos = fn.lastIndexOf('.');
        if (pos < 0) {
            return fallBack;
        }
        String s1 = fn.substring(pos + 1);
        if (s1.length() == 0) {
            return fallBack;
        }

        ConcurrentMap<String, MimeTypeAssociation> typeMap = getTypeMap();
        MimeTypeAssociation mta = typeMap.get(toLowerCase(s1));
        if (null == mta) {
            return fallBack;
        }
        String type = mta.get();
        return null == type ? fallBack : type;
    }

    private static final List<String> DEFAULT_MIME_TYPES = Collections.singletonList(MIME_APPL_OCTET);

    /**
     * Gets the MIME type associated with given file name.
     *
     * @param fileName The file name; e.g. <code>"file.html"</code>
     * @return The MIME types associated with given file name or a singleton list with <code>"application/octet-stream"</code>
     */
    public static List<String> getContentTypes(String fileName) {
        return getContentTypes(fileName, DEFAULT_MIME_TYPES);
    }

    /**
     * Gets the MIME type associated with given file name.
     *
     * @param fileName The file name; e.g. <code>"file.html"</code>
     * @param fallBack The fall-back list to return
     * @return The MIME types associated with given file name or <code>fallBack</code> argument
     */
    public static List<String> getContentTypes(String fileName, List<String> fallBack) {
        if (Strings.isEmpty(fileName)) {
            return fallBack;
        }
        String fn = Strings.unquote(fileName);
        int pos = fn.lastIndexOf('.');
        if (pos < 0) {
            return fallBack;
        }
        String s1 = fn.substring(pos + 1);
        if (s1.length() == 0) {
            return fallBack;
        }

        ConcurrentMap<String, MimeTypeAssociation> typeMap = getTypeMap();
        MimeTypeAssociation mta = typeMap.get(toLowerCase(s1));
        if (null == mta) {
            return fallBack;
        }

        List<String> all = mta.getAll();
        return null == all || all.isEmpty() ? fallBack : all;
    }

    /**
     * Gets the MIME type associated with given file extension.
     *
     * <p>
     * This is a convenience method that invokes {@link #getContentTypeByExtension(String, String)} with latter argument set to <code>"application/octet-stream"</code>.
     *
     * @param extension The file extension; e.g. <code>"txt"</code>
     * @param fallBack The fall-back value to return in case file extension is unknown
     * @return The MIME type associated with given file extension or <code>application/octet-stream</code> if none found
     * @see #getContentTypeByExtension(String, String)
     */
    public static String getContentTypeByExtension(String extension) {
        return getContentTypeByExtension(extension, MIME_APPL_OCTET);
    }

    /**
     * Gets the MIME type associated with given file extension.
     *
     * @param extension The file extension; e.g. <code>"txt"</code>
     * @param fallBack The fall-back value to return in case file extension is unknown
     * @return The MIME type associated with given file extension or <code>fallBack</code> if none found
     */
    public static String getContentTypeByExtension(String extension, String fallBack) {
        if (Strings.isEmpty(extension)) {
            return fallBack;
        }

        ConcurrentMap<String, MimeTypeAssociation> typeMap = getTypeMap();
        MimeTypeAssociation mta = typeMap.get(toLowerCase(extension));
        if (null == mta) {
            return fallBack;
        }
        String type = mta.get();
        return null == type ? fallBack : type;
    }

    private static final String DEFAULT_EXT = "dat";

    private static final List<String> DEFAULT_EXTENSIONS = Collections.singletonList(DEFAULT_EXT);

    /**
     * Gets the file extension for given MIME type.
     *
     * @param mimeType The MIME type
     * @return The file extension for given MIME type or a singleton list with <code>dat</code> if none found
     */
    public static List<String> getFileExtensions(String mimeType) {
        if (Strings.isEmpty(mimeType)) {
            return DEFAULT_EXTENSIONS;
        }

        ConcurrentMap<String, List<String>> extMap = getExtMap();
        if (!extMap.containsKey(toLowerCase(mimeType))) {
            return DEFAULT_EXTENSIONS;
        }
        List<String> list = extMap.get(mimeType);
        return null == list ? DEFAULT_EXTENSIONS : Collections.unmodifiableList(list);
    }

    /**
     * Gets the file extension for given MIME type.
     *
     * @param mimeType The MIME type
     * @return The file extension for given MIME type or <code>dat</code> if none found
     */
    public static String getFileExtension(String mimeType) {
        return getFileExtension(mimeType, DEFAULT_EXT);
    }

    /**
     * Gets the file extension for given MIME type.
     *
     * @param mimeType The MIME type
     * @param defaultExt The default extension to return
     * @return The file extension for given MIME type or <code>defaultExt</code> if none found
     */
    public static String getFileExtension(String mimeType, String defaultExt) {
        if (Strings.isEmpty(mimeType)) {
            return defaultExt;
        }

        ConcurrentMap<String, List<String>> extMap = getExtMap();
        if (!extMap.containsKey(toLowerCase(mimeType))) {
            return defaultExt;
        }
        List<String> list = extMap.get(mimeType);
        return null == list || list.isEmpty() ? defaultExt : list.get(0);
    }

    /**
     * Loads the MIME type file specified through <code>fileStr</code>.
     *
     * @param fileStr The MIME type file to load
     * @param extMapping The type mapping to fill
     * @param typeMapping The file extension mapping to fill
     */
    public static void load(String fileStr, ConcurrentMap<String, MimeTypeAssociation> typeMapping, ConcurrentMap<String, List<String>> extMapping) {
        init();
        load(new File(fileStr), typeMapping, extMapping);
    }

    /**
     * Loads the MIME type file specified through given file.
     *
     * @param file The MIME type file to load
     * @param extMapping The type mapping to fill
     * @param typeMapping The file extension mapping to fill
     */
    public static void load(File file, ConcurrentMap<String, MimeTypeAssociation> typeMapping, ConcurrentMap<String, List<String>> extMapping) {
        init();
        loadInternal(file, typeMapping, extMapping);
    }

    /**
     * Loads the MIME type file specified through given file.
     *
     * @param file The MIME type file to load
     * @param extMapping The type mapping to fill
     * @param typeMapping The file extension mapping to fill
     */
    private static void loadInternal(File file, ConcurrentMap<String, MimeTypeAssociation> typeMapping, ConcurrentMap<String, List<String>> extMapping) {
        InputStream stream = null;
        BufferedReader reader = null;
        try {
            stream = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(stream, com.openexchange.java.Charsets.ISO_8859_1));
            parse(reader, typeMapping, extMapping);
        } catch (Exception e) {
            LOG.error("", e);
        } finally {
            Streams.close(reader, stream);
        }
    }

    /**
     * Loads the MIME type file specified through given URL.
     *
     * @param url The URL to a MIME type file
     * @param extMapping The type mapping to fill
     * @param typeMapping The file extension mapping to fill
     */
    private static void loadInternal(URL url, ConcurrentMap<String, MimeTypeAssociation> typeMapping, ConcurrentMap<String, List<String>> extMapping) {
        InputStream stream = null;
        BufferedReader reader = null;
        try {
            stream = url.openStream();
            reader = new BufferedReader(new InputStreamReader(stream, com.openexchange.java.Charsets.ISO_8859_1));
            parse(reader, typeMapping, extMapping);
        } catch (Exception e) {
            LOG.error("", e);
        } finally {
            Streams.close(reader, stream);
        }
    }

    private static void parse(BufferedReader reader, ConcurrentMap<String, MimeTypeAssociation> typeMapping, ConcurrentMap<String, List<String>> extMapping) throws IOException {
        String line = null;
        StringBuilder strBuilder = new StringBuilder(64);
        while ((line = reader.readLine()) != null) {
            int i = strBuilder.length();
            strBuilder.append(line);
            if ((i > 0) && (strBuilder.charAt(i - 1) == '\\')) {
                strBuilder.delete(0, i - 1);
            } else {
                parseEntry(strBuilder.toString().trim(), typeMapping, extMapping);
                strBuilder.setLength(0);
            }
        }
        if (strBuilder.length() > 0) {
            parseEntry(strBuilder.toString().trim(), typeMapping, extMapping);
        }
    }

    private static void parseEntry(String entry, ConcurrentMap<String, MimeTypeAssociation> typeMapping, ConcurrentMap<String, List<String>> extMapping) {
        if (entry.length() == 0) {
            return;
        }
        if (entry.charAt(0) == '#') {
            return;
        }

        if (entry.indexOf('=') > 0) {
            MimeTypeFileLineParser parser = new MimeTypeFileLineParser(entry);
            String type = parser.getType();
             List<String> exts = parser.getExtensions();
            if ((type != null) && (exts != null)) {
                for (String ext : exts) {
                    addTypeMapping(ext, type, typeMapping);
                }
                if (extMapping.containsKey(type)) {
                    extMapping.get(type).addAll(exts);
                } else {
                    extMapping.put(type, exts);
                }
            }
        } else {
            String[] tokens = entry.split("[ \t\n\r\f]+");
            if (tokens.length > 1) {
                String type = toLowerCase(tokens[0]);
                List<String> set = new CopyOnWriteArrayList<String>();
                for (int i = 1; i < tokens.length; i++) {
                    String ext = toLowerCase(tokens[i]);
                    set.add(ext);
                    addTypeMapping(ext, type, typeMapping);
                }
                if (extMapping.containsKey(type)) {
                    extMapping.get(type).addAll(set);
                } else {
                    extMapping.put(type, set);
                }
            }
        }
    }

    private static void addTypeMapping(String ext, String mimeType, ConcurrentMap<String, MimeTypeAssociation> typeMapping) {
        MimeTypeAssociation mta = typeMapping.get(ext);
        if (null == mta) {
            MimeTypeAssociation nmta = new MimeTypeAssociation();
            mta = typeMapping.putIfAbsent(ext, nmta);
            if (null == mta) {
                mta = nmta;
            }
        }
        mta.add(mimeType);
    }

    // -------------------------------------------------- Helper classes --------------------------------------------------------- //

    private static final class MimeTypeAssociation {

        private final SortableConcurrentList<ComparableMimeTypeEntry> mimeTypeEntries;

        MimeTypeAssociation() {
            super();
            mimeTypeEntries = new SortableConcurrentList<ComparableMimeTypeEntry>();
        }

        void add(String mimeType) {
            if (null != mimeType) {
                List<ComparableMimeTypeEntry> snapshot = mimeTypeEntries.getSnapshot();
                for (ComparableMimeTypeEntry comparableMimeTypeEntry : snapshot) {
                    if (comparableMimeTypeEntry.mimeType.equals(mimeType)) {
                        // Already contained
                        return;
                    }
                }
                mimeTypeEntries.addAndSort(new ComparableMimeTypeEntry(mimeType));
            }
        }

        String get() {
            List<ComparableMimeTypeEntry> snapshot = mimeTypeEntries.getSnapshot();
            ComparableMimeTypeEntry e =  snapshot.isEmpty() ? null : snapshot.get(0);
            return null == e ? null : e.mimeType;
        }

        List<String> getAll() {
            List<ComparableMimeTypeEntry> snapshot = mimeTypeEntries.getSnapshot();
            List<String> types = new ArrayList<String>(snapshot.size());
            for (ComparableMimeTypeEntry e : snapshot) {
                types.add(e.mimeType);
            }
            return types;
        }

        @Override
        public String toString() {
            return mimeTypeEntries.toString();
        }
    }

    private static final class ComparableMimeTypeEntry implements Comparable<ComparableMimeTypeEntry> {

        final String mimeType;

        ComparableMimeTypeEntry(String mimeType) {
            super();
            this.mimeType = mimeType;
        }

        @Override
        public int compareTo(ComparableMimeTypeEntry o) {
            int len1 = mimeType.length();
            int len2 = o.mimeType.length();
            return (len1 < len2) ? -1 : ((len1 == len2) ? 0 : 1);
        }

        @Override
        public String toString() {
            return mimeType;
        }
    }

}
