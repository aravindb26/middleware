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

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.osgi.annotation.SingletonService;

/**
 * {@link ConfigurationService} - The service providing access to application's configuration.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@SingletonService
public interface ConfigurationService {

    /**
     * Gets the filter backed by given value.
     *
     * @param value The value
     * @return The filter or <code>null</code> if given value is <code>null</code>
     */
    default Filter getFilterForValue(String value) {
        return null == value ? null : new WildcardFilter(value);
    }

    /**
     * Gets the filter backed by given property's value.
     *
     * @param name The property name
     * @return The filter or <code>null</code> if there is no such property
     */
    Filter getFilterFromProperty(String name);

    /**
     * Gets all properties that fulfills given filter's acceptance criteria.
     *
     * @param filter The filter
     * @return The appropriate properties
     * @throws OXException If properties cannot be returned
     */
    Map<String, String> getProperties(PropertyFilter filter) throws OXException;

    /**
     * Searches for the property with the specified name in this property list. If the name is not found in this property list, the default
     * property list, and its defaults, recursively, are then checked. The method returns <code>null</code> if the property is not found.
     *
     * @param name The property name.
     * @return The value in this property list with the specified key value or <code>null</code>.
     */
    String getProperty(String name);

    /**
     * Searches for the property with the specified name in this property list. If the name is not found in this property list, the default
     * property list, and its defaults, recursively, are then checked. The method returns the default value argument if the property is not
     * found.
     *
     * @param name The property name.
     * @param defaultValue The default value
     * @return The value in this property list with the specified key value or given default value argument.
     */
    String getProperty(String name, String defaultValue);

    /**
     * Searches for the property with the specified name in this property list. If the name is not found or the associated value is an
     * empty string, the given default value is returned.
     *
     * @param name The property name.
     * @param defaultValue The default value
     * @return The value in this property list with the specified key value (if non-empty string) or given default value argument.
     */
    default String getNonEmptyProperty(String name, String defaultValue) {
        String value = getProperty(name);
        return Strings.isEmpty(value) ? defaultValue : value;
    }

    /**
     * Searches for the property with the specified name in this property list. If the name is not found in this property list, the default
     * property list, and its defaults, recursively, are then checked. The method returns the default value argument if the property is not
     * found. If the value can be found it will be split at the given separator and trimmed.
     * <p>
     *
     * @param name The property name.
     * @param defaultValue The default value
     * @param separator the separator as regular expression used to split the input around this separator
     * @return The value in this property list with the specified key value or given default value argument split and trimmed at the given
     * separator
     * @throws IllegalArgumentException - if defaultValue or the separator are missing or if the separator isn't a valid pattern
     */
    List<String> getProperty(String name, String defaultValue, String separator);

    /**
     * Returns all properties defined in a specific properties file. The filename of the properties file must not contains any path
     * segments. If no such property file has been read empty properties will be returned.
     *
     * @param fileName The file name of the properties file.
     * @return the properties from that file or an empty properties if that file was not read.
     *
     * @deprecated <div style="margin-left: 0.1in; margin-right: 0.5in; background-color:#FFDDDD;">
     *             Due to "lean configuration" initiative this method might return a {@code Properties} instance that does not contain
     *             all of the properties that are expected to be contained, as the specified file might miss those properties whose
     *             value does not differ from associated default value. If the denoted file is known to exist and does contain expected
     *             content, this methods may be safely called or by invoking
     *             <code>ConfigurationServices.loadPropertiesFrom(configService.getFileByName("existing.properties"))</code>.
     *             </div>
     */
    @Deprecated
    Properties getFile(String fileName);

    /**
     * Gets the directory denoted by given directory name.
     *
     * @param directoryName The directory name
     * @return The directory or <code>null</code>
     */
    File getDirectory(String directoryName);

    /**
     * Gets the file denoted by given file name.
     * <p>
     * <div style="margin-left: 0.1in; margin-right: 0.5in; background-color:#FFDDDD;">
     * Due to "lean configuration" initiative this method the referenced file might be absent or return an incomplete content
     * in case a <code>.properties</code> file is supposed to be fetched, as the specified file might miss those properties whose
     * value does not differ from associated default value.
     * If the denoted file is known to exist and does contain expected content, this methods may be safely called.
     * </div>
     *
     * @param fileName The file name
     * @return The file or <code>null</code>
     */
    File getFileByName(String fileName);

    /**
     * If no property format is used for configuration data, the text content of a file can be retrieved with this call.
     * <p>
     * <div style="margin-left: 0.1in; margin-right: 0.5in; background-color:#FFDDDD;">
     * Due to "lean configuration" initiative this method the referenced file might be absent or return an incomplete content
     * in case a <code>.properties</code> file is supposed to be fetched, as the specified file might miss those properties whose
     * value does not differ from associated default value.
     * If the denoted file is known to exist and does contain expected content, this methods may be safely called.
     * </div>
     *
     * @param fileName The logical file name of the file to be retrieved.
     * @return The text content of the configuration
     */
    String getText(String fileName);

    /**
     * Retrieves and merges all properties files in below the given folder name and its subfolders (recursively). All properties discovered
     * this way are aggregated in the returned properties object.
     *
     * @param folderName
     * @return Aggregated properties of all properties files below this folder.
     */
    Properties getPropertiesInFolder(String folderName);

    /**
     * Searches for the property with the specified name in this property list. If the name is found in this property list, it is supposed
     * to be a boolean value. If conversion fails or name is not found, the default value is returned.
     * <p>
     * The <code>boolean</code> returned represents the value <code>true</code> if the property is not <code>null</code> and is equal,
     * ignoring case, to the string {@code "true"}.
     *
     * @param name The property name.
     * @param defaultValue The default value
     * @return The boolean value in this property list with the specified key value or given default value argument.
     */
    boolean getBoolProperty(String name, boolean defaultValue);

    /**
     * Searches for the property with the specified name in this property list. If the name is found in this property list, it is supposed
     * to be an integer value. If conversion fails or name is not found, the default value is returned.
     * <p>
     * Parses the property as a signed decimal integer. The characters in the property must all be decimal digits, except that the first
     * character may be an ASCII minus sign <code>'-'</code> (<code>'&#92;u002D'</code>) to indicate a negative value.
     *
     * @param name The property name.
     * @param defaultValue The default value
     * @return The integer value in this property list with the specified key value or given default value argument.
     */
    int getIntProperty(String name, int defaultValue);

    /**
     * Searches for the property with the specified name in this property list. If the name is found in this property list, it is supposed
     * to be an long value. If conversion fails or name is not found, the default value is returned.
     * <p>
     * Parses the property as a signed decimal long. The characters in the property must all be decimal digits, except that the first
     * character may be an ASCII minus sign <code>'-'</code> (<code>'&#92;u002D'</code>) to indicate a negative value.
     *
     * @param name The property name.
     * @param defaultValue The default value
     * @return The long value in this property list with the specified key value or given default value argument.
     */
    default long getLongProperty(String name, long defaultValue) {
        String prop = getProperty(name);
        if (prop != null) {
            try {
                return Long.parseLong(prop.trim());
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return defaultValue;
    }

    /**
     * Returns an iterator of all the keys in this property list.
     *
     * @return The iterator of all the keys in this property list.
     */
    Iterator<String> propertyNames();

    /**
     * Returns the number of properties in this property list.
     *
     * @return The number of properties in this property list.
     */
    int size();

    /**
     * Loads a file and parses it with a YAML parser. The type of object returned depends on the layout of the YAML file and should be known
     * to clients of this service.
     *
     * @param filename
     * @return The parsed data or <code>null</code> if there is no such YAML file
     * @throws IllegalStateException If YAML file cannot be loaded
     */
    Object getYaml(String filename);

    /**
     * Loads all files in a directory and parses them with a YAML parser. The type of the objects returned depends on the layout of the YAML
     * files.
     *
     * @param dirName
     * @return A map mapping filename to the object that was parsed.
     * @throws IllegalStateException If YAML file cannot be loaded
     */
    Map<String, Object> getYamlInFolder(String dirName);

}
