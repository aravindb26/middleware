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

package com.openexchange.templating.impl;

import static com.openexchange.templating.TemplateErrorMessage.IOException;
import static com.openexchange.templating.TemplateErrorMessage.TemplateNotFound;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.io.FileUtils;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.java.util.Pair;
import com.openexchange.mail.mime.MimeType2ExtMap;
import com.openexchange.session.Session;
import com.openexchange.templating.OXTemplate;
import com.openexchange.templating.OXTemplate.TemplateLevel;
import com.openexchange.templating.OXTemplateExceptionHandler;
import com.openexchange.templating.TemplateErrorMessage;
import com.openexchange.templating.TemplateService;
import com.openexchange.templating.TemplatingHelper;
import com.openexchange.tools.encoding.Base64;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

/**
 * {@link TemplateServiceImpl} - The default implementation of {@link TemplateService}.
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class TemplateServiceImpl implements TemplateService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TemplateServiceImpl.class);

    /** The property for file path to templates */
    public static final String PATH_PROPERTY = "com.openexchange.templating.path";

    /** The map for cached tags */
    private static final Map<String, Map<String, Set<String>>> CACHED_TAGS = new ConcurrentHashMap<>();

    private final Object lock;
    private final String defaultTemplatePath;

    /**
     * Initializes a new {@link TemplateServiceImpl}.
     *
     * @param config The configuration service
     */
    public TemplateServiceImpl(final ConfigurationService config) {
        super();
        lock = new Object();
        defaultTemplatePath = config.getProperty(PATH_PROPERTY);
    }

    @Override
    public OXTemplate loadTemplate(final String templateName) throws OXException {
        return loadTemplate(templateName, null);
    }

    @Override
    public OXTemplate loadTemplate(final String templateName, final OXTemplateExceptionHandler exceptionHandler) throws OXException {
        final String templatePath = defaultTemplatePath;
        if (templatePath == null) {
            return null;
        }

        final OXTemplateImpl retval = exceptionHandler == null ? new OXTemplateImpl() : new OXTemplateImpl(exceptionHandler);

        retval.setLevel(TemplateLevel.SERVER);
        Properties properties = new Properties();

        // Load template
        {
            Template template = loadTemplate(templatePath, templateName, properties);
            if (null == template) {
                throw TemplateErrorMessage.TemplateNotFound.create(templatePath + File.separator + templateName);
            }
            retval.setTemplate(template);
        }

        checkTrustLevel(retval);
        retval.setProperties(properties);
        return retval;
    }

    protected Template loadTemplate(final String templatePath, final String templateName, final Properties properties) throws OXException {
        final File path = new File(templatePath);
        if (!path.exists() || !path.isDirectory() || !path.canRead()) {
            return null;
        }
        checkTemplatePath(templatePath);
        checkAdminTemplate(templateName);

        synchronized (lock) {
            Template retval = null;
            try {
                if (existsInFilesystem(templateName)) {
                    Configuration config = createConfiguration(templatePath);
                    String templateText = loadFromFileSystem(templateName);
                    templateText = extractProperties(templateText, properties);
                    retval = new Template(templateName, new StringReader(templateText), config);
                }
            } catch (IOException e) {
                throw IOException.create(e);
            }
            if (retval == null) {
                throw TemplateNotFound.create(templateName);
            }
            return retval;
        }
    }

    private static synchronized Configuration createConfiguration(String templatePath) {
        // Initialize Configuration instance
        String userDir = System.getProperty("user.dir");
        System.setProperty("user.dir", templatePath);
        Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        System.setProperty("user.dir", userDir); // Restore "user.dir"

        // Set attributes
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        config.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        config.setAPIBuiltinEnabled(false);
        return config;
    }

    @Override
    public TemplatingHelper createHelper(final Object rootObject) {
        return new TemplatingHelperImpl(rootObject, this);
    }
    /**
     * Checks whether a given template is an admin template which means that it is defined from the administrator and stored within the
     * backend.
     *
     * @param templateName - the name of the template to check.
     * @throws OXException In case the template is not an admin template
     */
    protected void checkAdminTemplate(String templateName) throws OXException {
        List<String> basicTemplateNames = getBasicTemplateNames(Strings.getEmptyStrings());
        if (!basicTemplateNames.contains(templateName) || !existsInFilesystem(templateName)) {
            throw TemplateErrorMessage.AccessDenied.create();
        }
    }

    private void checkTemplatePath(final String templatePath) throws OXException {
        try {
            if (Strings.isEmpty(templatePath)) {
                return;
            }
            final String defaultTemplatePath = this.defaultTemplatePath;
            if (defaultTemplatePath == null) {
                return;
            }
            if (Strings.toLowerCase(defaultTemplatePath).equals(Strings.toLowerCase(templatePath))) {
                // Equal directory
                return;
            }
            if (isSubDirectory(new File(defaultTemplatePath), new File(templatePath))) {
                return;
            }
            // A file is accessed in a foreign directory
            final OXException e = TemplateErrorMessage.AccessDenied.create();
            LOG.error("{}: Acces to file denied: \"{}\" exceptionID={}", e.getErrorCode(), templatePath, e.getExceptionId());
            throw e;
        } catch (IOException e) {
            throw TemplateErrorMessage.IOException.create(e, e.getMessage());
        }
    }

    /**
     * Checks, whether the child directory is a sub-directory of the base directory.
     *
     * @param base The base directory.
     * @param child The suspected child directory.
     * @return <code>true</code> if the child is a sub-directory of the base directory.
     * @throws IOException If an I/O error occurred during the test.
     */
    private static boolean isSubDirectory(final File base, final File child) throws IOException {
        final File b = base.getCanonicalFile();
        final File c = child.getCanonicalFile();

        File parentFile = c;
        while (parentFile != null) {
            if (b.equals(parentFile)) {
                return true;
            }
            parentFile = parentFile.getParentFile();
        }
        return false;
    }

    private void checkTrustLevel(OXTemplateImpl template) {
        template.setTrusted(template.getLevel() == TemplateLevel.SERVER);
    }

    private static String extractProperties(String text, Properties properties) throws OXException {
        StringBuilder keep = new StringBuilder();
        StringBuilder props = new StringBuilder();
        int state = 0;
        for (String line : Strings.splitByLineSeparator(text)) {
            switch (state) {
                case 0:
                    if (line.startsWith("BEGIN")) {
                        state = 1;
                    } else {
                        keep.append(line).append('\n');
                    }
                    break;
                case 1:
                    if (line.startsWith("END")) {
                        state = 2;
                    } else {
                        props.append(line).append('\n');
                    }
                    break;
                case 2:
                    keep.append(line).append('\n');
                    break;
                default:
                    throw new IllegalStateException("Invalid state: " + state);
            }
        }

        try {
            if (state > 0) {
                loadPropertiesFrom(new StringReader(props.toString()), properties);
            }
        } catch (IOException e) {
            throw TemplateErrorMessage.IOException.create();
        }

        return keep.toString();
    }

    private static void loadPropertiesFrom(Reader reader, Properties originalProperty) throws IOException {
        if (reader != null) {
            try {
                originalProperty.load(reader);
            } finally {
                reader.close();
            }
        }
    }

    protected boolean existsInFilesystem(final String templateName) {
        File templateFile = getTemplateFile(templateName);
        return templateFile.exists() && templateFile.canRead();
    }

    protected String loadFromFileSystem(final String defaultTemplateName) throws OXException {
        File templateFile = getTemplateFile(defaultTemplateName);
        if (!templateFile.exists() || !templateFile.canRead()) {
            throw TemplateErrorMessage.TemplateNotFound.create(defaultTemplateName);
        }
        checkTemplatePath(templateFile.getPath());
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(templateFile), com.openexchange.java.Charsets.UTF_8), 2048);
            final StringBuilder builder = new StringBuilder(2048);
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (IOException e) {
            throw IOException.create(e);
        } finally {
            Streams.close(reader);
        }
    }

    private File getTemplateFile(String defaultTemplateName) {
        defaultTemplateName = new File(defaultTemplateName).getName();
        return new File(defaultTemplatePath, defaultTemplateName);
    }

    @Override
    public List<String> getBasicTemplateNames(final String... filter) {
        final String templatePath = defaultTemplatePath;
        final File templateDir = new File(templatePath);
        if (!templateDir.isDirectory() || !templateDir.exists()) {
            return new ArrayList<String>(0);
        }

        final Set<String> sieve = new HashSet<String>(Arrays.asList(null == filter ? Strings.getEmptyStrings() : filter));

        final Map<String, Set<String>> tagMap = getTagMap(templateDir);

        final File[] files = templateDir.listFiles();
        if (files == null) {
            return new ArrayList<String>(0);
        }
        final Set<String> names = new HashSet<String>();
        final Set<String> defaults = new HashSet<String>();
        for (final File file : files) {
            Set<String> tags = tagMap.get(file.getName());
            if (tags == null) {
                tags = Collections.emptySet();
            }
            if (file.isFile() && file.canRead() && file.getName().endsWith(".tmpl") && (tags.containsAll(sieve))) {
                if (tags.contains("default")) {
                    defaults.add(file.getName());
                } else {
                    names.add(file.getName());
                }

            }
        }
        final List<String> a = new ArrayList<String>(defaults);
        final List<String> b = new ArrayList<String>(names);
        Collections.sort(a);
        Collections.sort(b);
        a.addAll(b);

        return a;
    }

    private static Map<String, Set<String>> getTagMap(final File templateDir) {
        final String absolutePath = templateDir.getAbsolutePath();

        {
            final Map<String, Set<String>> map = CACHED_TAGS.get(absolutePath);
            if (null != map) {
                return map;
            }
        }

        final File[] files = templateDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(final File pathname) {
                return pathname.getName().endsWith(".properties") && pathname.canRead() && pathname.isFile();
            }
        });
        if (files == null) {
            final Map<String, Set<String>> emptyMap = Collections.emptyMap();
            CACHED_TAGS.put(absolutePath, emptyMap);
            return emptyMap;
        }

        final Map<String, Set<String>> tagMap = new HashMap<String, Set<String>>(files.length);
        for (final File file : files) {
            InputStream inStream = null;
            try {
                inStream = new BufferedInputStream(new FileInputStream(file), 2048);
                final Properties index = new Properties();
                index.load(inStream);
                for (final Entry<Object, Object> entry : index.entrySet()) {
                    final String filename = entry.getKey().toString();
                    final String[] categoriesArr = Strings.splitByComma(entry.getValue().toString());
                    final Set<String> categories = new HashSet<String>(Arrays.asList(categoriesArr));
                    tagMap.put(filename, categories);
                }
            } catch (IOException e) {
                LOG.error("", e);
            } finally {
                Streams.close(inStream);
            }
        }
        CACHED_TAGS.put(absolutePath, tagMap);
        return tagMap;

    }

    @Override
    public List<String> getTemplateNames(final Session sess, String... filter) throws OXException {
        return getBasicTemplateNames(filter);

    }

    @Override
    public Pair<String, String> encodeTemplateImage(String imageName) throws OXException {
        try {
            File imageFile = new File(defaultTemplatePath, imageName);
            String contentType = MimeType2ExtMap.getContentType(imageFile);
            byte[] imageBytes = FileUtils.readFileToByteArray(imageFile);
            String imageBase64 = Base64.encode(imageBytes);
            return new Pair<String, String>(contentType, imageBase64);
        } catch (IOException e) {
            throw TemplateErrorMessage.IOException.create(e, e.getMessage());
        }
    }
}
