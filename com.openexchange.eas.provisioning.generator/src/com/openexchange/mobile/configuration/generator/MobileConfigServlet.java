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

package com.openexchange.mobile.configuration.generator;

import static com.openexchange.java.Autoboxing.I;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.operator.OperatorCreationException;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.html.HtmlService;
import com.openexchange.java.AllocatingStringWriter;
import com.openexchange.java.Strings;
import com.openexchange.mobile.configuration.generator.configuration.ConfigurationException;
import com.openexchange.mobile.configuration.generator.configuration.MobileConfigProperties;
import com.openexchange.mobile.configuration.generator.configuration.Property;
import com.openexchange.mobile.configuration.generator.crypto.MobileConfigSigner;
import com.openexchange.mobile.configuration.generator.osgi.Activator;
import com.openexchange.mobile.configuration.generator.osgi.Services;
import com.openexchange.templating.OXTemplate;
import com.openexchange.templating.TemplateService;
import com.openexchange.tools.servlet.http.Tools;

public class MobileConfigServlet extends HttpServlet {

    private static final String PARAMETER_MAIL = "m";

    private static final String PARAMETER_LOGIN = "l";

    private enum Device {
        iPhone, winMob;
    }

    private static enum ErrorMessage {

        MSG_INTERNAL_ERROR("Ein interner Fehler ist aufgetreten, bitte versuchen Sie es sp\u00e4ter noch einmal.", "An internal error occurred. Please try again later."),
        MSG_NO_SUPPORTED_DEVICE_FOUND("Ihr Ger\u00e4t wird nicht unterst\u00fctzt", "Your device is not supported."),
        MSG_PARAMETER_LOGIN_IS_MISSING("Der Parameter \"l\" fehlt", "The \"l\" parameter is missing"),
        MSG_UNSECURE_ACCESS("Unsicherer Zugriff mit http ist nicht erlaubt. Bitte https benutzen.", "Unsecured http access is not allowed. Use https instead."),
        MSG_INVALID_ERROR_PARAMETER("Der \u00fcbergebene \"error\"-Parameter ist ung\u00fcltig.", "Invalid \"error\" parameter.");

        private final String english;

        private final String german;

        private static Map<Integer, ErrorMessage> members = new ConcurrentHashMap<Integer, ErrorMessage>();

        private ErrorMessage(final String german, final String english) {
            this.german = german;
            this.english = english;
        }

        public String getEnglish() {
            return english;
        }

        public String getGerman() {
            return german;
        }

        static {
            for (final ErrorMessage errmsg : ErrorMessage.values()) {
                members.put(I(errmsg.ordinal()), errmsg);
            }
        }

        public static ErrorMessage getErrorMessageByNumber(final int value) {
            return members.get(Integer.valueOf(value));
        }

    }

    private static final transient org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MobileConfigServlet.class);

    /**
     *
     */
    private static final long serialVersionUID = 7913468326542861986L;

    public static String write(final String email, final String host, final String username, final String domain) throws OXException {
        final TemplateService service = Services.getService(TemplateService.class);
        final OXTemplate loadTemplate = service.loadTemplate("winMobileTemplate.tmpl");
        final AllocatingStringWriter writer = new AllocatingStringWriter();
        loadTemplate.process(generateHashMap(email, host, username, domain), writer);
        return writer.toString();
    }

    /**
     * Splits the given login into a username and a domain part
     *
     * @param username
     * @return An array. Index 0 is the username. Index 1 is the domain
     * @throws ConfigurationException
     */
    protected static String[] splitUsernameAndDomain(final String username) throws ConfigurationException {
        final String domain_user = MobileConfigProperties.getProperty(Property.DomainUser);
        if (domain_user == null) {
            throw new ConfigurationException("Missing login pattern. Please configure " + Property.DomainUser);
        }
        final String separator = domain_user.replaceAll("\\$USER|\\$DOMAIN", "");
        final String[] split = username.split(Pattern.quote(separator));
        if (split.length > 2) {
            throw new ConfigurationException("Splitting of login returned wrong length. Array is " + Arrays.toString(split));
        }

        if (split.length == 1) {
            return new String[] { split[0], "defaultcontext" };
        }

        if (domain_user.indexOf("$USER") < domain_user.indexOf("$DOMAIN")) {
            return split;
        }

        // change position in array...
        return new String[] { split[1], split[0] };
    }

    private static HashMap<String, String> generateHashMap(final String email, final String host, final String username, final String domain) {
        final HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("email", email);
        hashMap.put("host", host);
        hashMap.put("username", username);
        hashMap.put("domain", domain);
        return hashMap;
    }

    private static void writeMobileConfigWinMob(final OutputStream out, final String email, final String host, final String username, final String domain) throws IOException, OXException {
        CabUtil.writeCabFile(new DataOutputStream(new BufferedOutputStream(out)), write(email, host, username, domain));
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // create a new HttpSession if it's missing
        req.getSession(true);
        super.service(req, resp);
    }

    @Override
    public void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");
        Tools.disableCaching(resp);
        final ConfigurationService service = Services.getService(ConfigurationService.class);
        if (null == service) {
            LOG.error("A configuration exception occurred, which should not happen: No configuration service found");
            printError(req, resp, ErrorMessage.MSG_INTERNAL_ERROR);
            return;
        }

        final String parameter = req.getParameter("error");
        if (null != parameter && parameter.length() != 0) {
            // Error output
            errorOutput(req, resp, parameter);
            return;
        }
        final String iphoneRegEx;
        final String winMobRegEx;
        try {
            iphoneRegEx = MobileConfigProperties.getProperty(service, Property.iPhoneRegex);
            winMobRegEx = MobileConfigProperties.getProperty(service, Property.WinMobRegex);
            final Boolean secureConnect = MobileConfigProperties.getProperty(service, Property.OnlySecureConnect);
            if (secureConnect.booleanValue()) {
                if (!req.isSecure()) {
                    printError(req, resp, ErrorMessage.MSG_UNSECURE_ACCESS);
                    return;
                }
            }
        } catch (ConfigurationException e) {
            LOG.error("A configuration exception occurred, which should not happen", e);
            printError(req, resp, ErrorMessage.MSG_INTERNAL_ERROR);
            return;
        }

        final Device device = detectDevice(req);
        final String login = req.getParameter(PARAMETER_LOGIN);
        if (null == device) {
            if (null == login) {
                printError(req, resp, ErrorMessage.MSG_PARAMETER_LOGIN_IS_MISSING);
                return;
            }
            String mailpart = "";
            final String mail = req.getParameter(PARAMETER_MAIL);
            if (null != mail) {
                mailpart = "&m=" + URLEncoder.encode(mail, "UTF-8");
            }

            final String header = req.getHeader("user-agent");
            if (null != header) {
                if (header.matches(iphoneRegEx)) {
                    // iPhone part
                    resp.sendRedirect(Activator.ALIAS + "/eas.mobileconfig?l=" + URLEncoder.encode(login, "UTF-8") + mailpart);
                    return;
                } else if (header.matches(winMobRegEx)) {
                    // WinMob part
                    resp.sendRedirect(Activator.ALIAS + "/ms.cab?l=" + URLEncoder.encode(login, "UTF-8") + mailpart);
                    return;
                } else {
                    printError(req, resp, ErrorMessage.MSG_NO_SUPPORTED_DEVICE_FOUND);
                    LOG.info("Unsupported device header: \"{}\"", header);
                    return;
                }
            }
        } else {
            try {
                generateConfig(req, resp, login, device);
            } catch (OXException e) {
                LOG.error("A template exception occurred, which should not happen", e);
                printError(req, resp, ErrorMessage.MSG_INTERNAL_ERROR);
                return;
            } catch (IOException e) {
                LOG.error("A template exception occurred, which should not happen", e);
                printError(req, resp, ErrorMessage.MSG_INTERNAL_ERROR);
                return;
            }
        }
    }

    private Device detectDevice(final HttpServletRequest req) {
        final String pathInfo = req.getPathInfo();
        if ("/eas.mobileconfig".equals(pathInfo)) {
            return Device.iPhone;
        } else if ("/ms.cab".equals(pathInfo)) {
            return Device.winMob;
        } else {
            return null;
        }
    }

    /**
     * Reads the language from the header, returns either ENGLISH or GERMAN. No other value can be returned
     *
     * @param req
     * @return
     */
    private Locale detectLanguage(final HttpServletRequest req) {
        final String parameter = req.getHeader("Accept-Language");
        if (null == parameter) {
            return Locale.ENGLISH;
        }
        if (parameter.startsWith("de")) {
            return Locale.GERMAN;
        }
        return Locale.ENGLISH;
    }

    private void errorOutput(final HttpServletRequest req, final HttpServletResponse resp, final String string) {
        final Locale locale = detectLanguage(req);
        ErrorMessage msg = null;
        try {
            msg = ErrorMessage.getErrorMessageByNumber(Integer.parseInt(string));
        } catch (NumberFormatException e1) {
            msg = ErrorMessage.MSG_INVALID_ERROR_PARAMETER;
        }
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter writer;
        try {
            writer = getWriterFromOutputStream(resp.getOutputStream());
        } catch (IOException e) {
            LOG.error("Unable to get output stream to write error message", e);
            return;
        }
        if (ErrorMessage.MSG_PARAMETER_LOGIN_IS_MISSING.equals(msg)) {
            writer.println("<html><head>");
            writer.println("<meta name=\"viewport\" content=\"width=320\" />");
            writer.println("<meta name=\"mobileoptimized\" content=\"0\" />");
            writer.println("<title>Error</title>");
            writer.println("<style type=\"text/css\">");
            writer.println("table { height: 100%; width:100% }");
            writer.println("td { text-align:center; vertical-align:middle; }");
            writer.println("</style>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("<form action=\"" + Activator.ALIAS + "\" method=\"get\" enctype=\"application/x-www-form-urlencoded; charset=UTF-8\" accept-charset=\"UTF-8\">");
            writer.println("<table>");
            writer.println("<tr><td>");
            if (Locale.ENGLISH.equals(locale)) {
                writer.println("<h1>" + "Enter your username for auto-configuring your device." + "</h1>");
            } else if (Locale.GERMAN.equals(locale)) {
                writer.println("<h1>" + "Geben Sie f\u00fcr die automatische Konfiguration Ihres Ger\u00e4tes Ihren Benutzernamen ein." + "</h1>");
            }
            writer.println("<input name=\"" + PARAMETER_LOGIN + "\" type=\"text\" size=\"30\" maxlength=\"100\">");
            if (Locale.ENGLISH.equals(locale)) {
                writer.println("<input type=\"submit\" value=\" Absenden \">");
            } else if (Locale.GERMAN.equals(locale)) {
                writer.println("<input type=\"submit\" value=\" Submit \">");
            }
            writer.println("</td>");
            writer.println("</tr>");
            writer.println("</table>");
            writer.println("</form>");
            writer.println("</body></html>");
            writer.close();
        } else {
            writer.println("<html><head>");
            writer.println("<meta name=\"viewport\" content=\"width=320\" />");
            writer.println("<meta name=\"mobileoptimized\" content=\"0\" />");
            writer.println("<title>Error</title>");
            writer.println("<style type=\"text/css\">");
            writer.println("table { height: 100%; width:100% }");
            writer.println("td { text-align:center; vertical-align:middle; }");
            writer.println("</style>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("<table>");
            writer.println("<tr>");

            HtmlService htmlService = Services.getService(HtmlService.class);
            if (Locale.ENGLISH.equals(locale)) {
                writer.println("<td><h1>" + htmlService.encodeForHTML(String.valueOf(msg.getEnglish())) + "</h1></td>");
            } else if (Locale.GERMAN.equals(locale)) {
                writer.println("<td><h1>" + htmlService.encodeForHTML(String.valueOf(msg.getGerman())) + "</h1></td>");
            }
            writer.println("</tr>");
            writer.println("</table>");
            writer.println("</body></html>");
            writer.close();
        }

    }

    private void generateConfig(final HttpServletRequest req, final HttpServletResponse resp, final String login, final Device device) throws IOException, OXException {
        String mail = login;
        final String parameter = req.getParameter(PARAMETER_MAIL);
        if (null != parameter) {
            mail = parameter;
        }
        final String[] usernameAndDomain;
        try {
            usernameAndDomain = splitUsernameAndDomain(login);
        } catch (ConfigurationException e) {
            throw new OXException(e);
        }
        if (Device.iPhone.equals(device)) {
            resp.setContentType("application/x-apple-aspen-config");
            final ServletOutputStream outputStream = resp.getOutputStream();
            final PrintWriter writer = getWriterFromOutputStream(outputStream);
            writeMobileConfig(writer, outputStream, mail, getHostname(req), usernameAndDomain[0], usernameAndDomain[1]);
            writer.close();
        } else if (Device.winMob.equals(device)) {
            final ServletOutputStream outputStream = resp.getOutputStream();
            writeMobileConfigWinMob(outputStream, mail, getHostname(req), usernameAndDomain[0], usernameAndDomain[1]);
            outputStream.close();
        }
    }

    private String getHostname(final HttpServletRequest req) {
        final String canonicalHostName = req.getServerName();
        return canonicalHostName;
    }

    private PrintWriter getWriterFromOutputStream(final ServletOutputStream outputStream) {
        try {
            return new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(outputStream), Charset.forName("UTF-8")));
        } catch (IllegalCharsetNameException e) {
            LOG.error("", e);
            throw e;
        } catch (UnsupportedCharsetException e) {
            LOG.error("", e);
            throw e;
        }
    }

    /**
     * Prints the error
     *
     * @param req The request
     * @param resp The response
     * @param string The error message to print
     */
    private void printError(final HttpServletRequest req, final HttpServletResponse resp, final ErrorMessage string) throws IOException {
        resp.sendRedirect(Activator.ALIAS + "?error=" + Integer.toString(string.ordinal()));
    }

    /**
     * Gets a {@link FileInputStream} for the file specified by the given property
     *
     * @param fileProperty The property holding the filename
     * @return The {@link FileInputStream} to the file represented by the given property
     * @throws ConfigurationException
     * @throws FileNotFoundException If the property's filename was not found
     */
    private FileInputStream getFileInputStream(Property fileProperty) throws ConfigurationException, FileNotFoundException {
        return getFileInputStream(fileProperty, true);
    }

    /**
     * Gets a {@link FileInputStream} for the file specified by the given property
     *
     * @param fileProperty The property holding the filename
     * @param required <code>True</code> if the filename must be present, <code>false</code> if it can be empty.
     * @return The {@link FileInputStream} to the file represented by the given property; <code>null</code> if the property is empty and <code>required</code> is <code>false</code>
     * @throws ConfigurationException
     * @throws FileNotFoundException If the property's filename was not found and <code> required </code> is <code>true</code>
     */
    private FileInputStream getFileInputStream(Property fileProperty, boolean required) throws ConfigurationException, FileNotFoundException {
        ConfigurationService service = Services.optService(ConfigurationService.class);
        if (null == service) {
            throw new ConfigurationException("No configuration service found");
        }

        String fileName = MobileConfigProperties.getProperty(service, fileProperty);
        if (!required && Strings.isEmpty(fileName)) {
            return null;
        }
        return new FileInputStream(new File(fileName));
    }

    private void writeMobileConfig(final PrintWriter printWriter, final OutputStream outStream, final String email, final String host, final String username, final String domain) throws IOException, OXException {
        try {
            TemplateService service = Services.getService(TemplateService.class);
            OXTemplate loadTemplate = service.loadTemplate("iPhoneTemplate.tmpl");
            Boolean sign = MobileConfigProperties.getProperty(Services.getService(ConfigurationService.class), Property.SignConfig);
            if (null != sign && sign.booleanValue()) {
                try (InputStream signer = getFileInputStream(Property.CertFile); //@formatter:off
                    InputStream key = getFileInputStream(Property.KeyFile);
                    InputStream additionals = getFileInputStream(Property.PemFile, false);
                    MobileConfigSigner writer = new MobileConfigSigner(signer, key, outStream, additionals)) { //@formatter:on
                    loadTemplate.process(generateHashMap(email, host, username, domain), writer);
                }
            } else {
                loadTemplate.process(generateHashMap(email, host, username, domain), printWriter);
            }
        } catch (ConfigurationException | OperatorCreationException | CertificateException | CMSException e) {
            throw new OXException(e);
        } finally {
            printWriter.close();
        }
    }

}
