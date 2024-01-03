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

package com.openexchange.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Properties;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import com.openexchange.config.utils.TokenReplacingReader;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;

/**
 * {@link ConfigurationServices} - A utility class for {@link ConfigurationService}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public class ConfigurationServices {

    /**
     * Initializes a new {@link ConfigurationServices}.
     */
    private ConfigurationServices() {
        super();
    }

    /**
     * Loads the properties from specified file.
     *
     * @param file The file to read from
     * @return The properties or <code>null</code> (if no such file exists)
     * @throws IOException If reading from file yields an I/O error
     * @throws IllegalArgumentException If file is invalid
     */
    public static Properties loadPropertiesFrom(File file) throws IOException {
        if (null == file) {
            return null;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return loadPropertiesFrom(fis);
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            Streams.close(fis);
        }
    }

    /**
     * Loads the properties from specified input stream.
     *
     * @param in The input stream to read from
     * @return The properties
     * @throws IOException If reading from input stream yields an I/O error
     * @throws IllegalArgumentException If input stream is invalid
     */
    public static Properties loadPropertiesFrom(InputStream in) throws IOException {
        return loadPropertiesFrom(in, false);
    }

    /**
     * Loads the properties from specified input stream while optionally discarding empty properties.
     *
     * @param in The input stream to read from
     * @param discardEmptyProperties <code>true</code> to discard such properties carrying an empty (string) value; otherwise <code>false</code> to keep them
     * @return The properties
     * @throws IOException If reading from input stream yields an I/O error
     * @throws IllegalArgumentException If input stream is invalid
     */
    public static Properties loadPropertiesFrom(InputStream in, boolean discardEmptyProperties) throws IOException {
        if (null == in) {
            return null;
        }

        InputStreamReader fr = null;
        BufferedReader br = null;
        TokenReplacingReader trr = null;
        try {
            // Initialize reader
            fr = new InputStreamReader(in, Charsets.UTF_8);
            trr = new TokenReplacingReader((br = new BufferedReader(fr, 2048)));

            // Load properties
            Properties properties = new Properties();
            properties.load(trr);
            if (discardEmptyProperties) {
                for (Iterator<Object> it = properties.values().iterator(); it.hasNext();) {
                    if (Strings.isEmpty(it.next().toString())) {
                        // Discard empty property
                        it.remove();
                    }
                }
            }
            return properties;
        } finally {
            Streams.close(trr, br, fr, in);
        }
    }

    /**
     * Loads the YAML object from given file.
     *
     * @param file The file to read from
     * @return The YAML object or <code>null</code> (if no such file exists)
     * @throws IOException If reading from file yields an I/O error
     * @throws IllegalArgumentException If file is no valid YAML
     */
    public static Object loadYamlFrom(File file) throws IOException {
        FileInputStream fis = null;
        InputStreamReader fr = null;
        BufferedReader br = null;
        TokenReplacingReader trr = null;
        try {
            fr = new InputStreamReader((fis = new FileInputStream(file)), Charsets.UTF_8);
            trr = new TokenReplacingReader((br = new BufferedReader(fr, 2048)));
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            return yaml.load(trr);
        } catch (YAMLException e) {
            throw new IllegalArgumentException("Failed to load YAML file '" + file + ". Please fix any syntax errors in it.", e);
        } finally {
            Streams.close(trr, br, fr, fis);
        }
    }

    /**
     * Loads the YAML object from given stream.
     *
     * @param in The stream to read from
     * @return The YAML object or <code>null</code> (if no such file exists)
     * @throws IOException If reading from stream yields an I/O error
     * @throws IllegalArgumentException If stream data is no valid YAML
     */
    public static Object loadYamlFrom(InputStream in) {
        if (null == in) {
            return null;
        }

        InputStreamReader fr = null;
        BufferedReader br = null;
        TokenReplacingReader trr = null;
        try {
            fr = new InputStreamReader(in, Charsets.UTF_8);
            trr = new TokenReplacingReader((br = new BufferedReader(fr, 2048)));
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            return yaml.load(trr);
        } catch (YAMLException e) {
            throw new IllegalArgumentException("Failed to read YAML content from given input stream.", e);
        } finally {
            Streams.close(trr, br, fr, in);
        }
    }

    /**
     * Parse the only YAML document in a stream and produce the corresponding Java object.
     *
     * @param <T> The class is defined by the second argument
     * @param in The stream to read from
     * @param type The class of the object to be created
     * @return The YAML object or <code>null</code> (if no such file exists)
     * @throws IOException If reading from stream yields an I/O error
     * @throws IllegalArgumentException If stream data is no valid YAML
     */
    public static <T> T loadYamlAs(InputStream in, Class<T> type) {
        if (null == in) {
            return null;
        }

        InputStreamReader fr = null;
        BufferedReader br = null;
        TokenReplacingReader trr = null;
        try {
            fr = new InputStreamReader(in, Charsets.UTF_8);
            trr = new TokenReplacingReader((br = new BufferedReader(fr, 2048)));
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            return yaml.loadAs(trr, type);
        } catch (YAMLException e) {
            throw new IllegalArgumentException("Failed to read YAML content from given input stream.", e);
        } finally {
            Streams.close(trr, br, fr, in);
        }
    }

    /**
     * Computes the <code>"SHA-256"</code> hash from given file.
     *
     * @param file The file to compute the hash from
     * @return The computed hash
     * @throws IllegalStateException If hash cannot be computed; file is corrupt/non-existing
     */
    public static byte[] getHash(File file) {
        return getHash(file, "SHA-256");
    }

    /**
     * Computes the hash using given algorithm from given file.
     *
     * @param file The file to compute the hash from
     * @param algorithm The algorithm to use
     * @return The computed hash
     * @throws IllegalStateException If hash cannot be computed; either algorithm is unknown or file is corrupt/non-existing
     */
    public static byte[] getHash(File file, String algorithm) {
        if (null == file || null == algorithm) {
            return null;
        }

        DigestInputStream digestInputStream = null;
        try {
            digestInputStream = new DigestInputStream(new FileInputStream(file), MessageDigest.getInstance(algorithm));
            int len = 8192;
            byte[] buf = new byte[len];
            while (digestInputStream.read(buf, 0, len) > 0) {
                // Discard
            }
            return digestInputStream.getMessageDigest().digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No such algorithm '" + algorithm + "'.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read file '" + file + "'. Reason: " + e.getMessage(), e);
        } finally {
            Streams.close(digestInputStream);
        }
    }

    /**
     * Reads the content from given file.
     *
     * @param file The file to read from
     * @return The file content or <code>null</code> (if passed file is <code>null</code>)
     * @throws IOException If an I/O error occurs
     */
    public static String readFile(File file) throws IOException {
        if (null == file) {
            return null;
        }

        FileInputStream fis = null;
        InputStreamReader fr = null;
        BufferedReader br = null;
        TokenReplacingReader trr = null;
        try {
            int length = (int) file.length();

            fr = new InputStreamReader((fis = new FileInputStream(file)), Charsets.UTF_8);
            trr = new TokenReplacingReader((br = new BufferedReader(fr, length > 16384 ? 16384 : length)));

            StringBuilder builder = new StringBuilder(length);
            int buflen = 2048;
            char[] cbuf = new char[buflen];
            for (int read; (read = trr.read(cbuf, 0, buflen)) > 0;) {
                builder.append(cbuf, 0, read);
            }
            return builder.toString();
        } finally {
            Streams.close(trr, br, fr, fis);
        }
    }

}
