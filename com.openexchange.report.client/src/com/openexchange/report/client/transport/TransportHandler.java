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

package com.openexchange.report.client.transport;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.openmbean.CompositeData;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONServices;
import com.openexchange.java.Streams;
import com.openexchange.report.client.configuration.ReportConfiguration;
import com.openexchange.report.client.container.ClientLoginCount;
import com.openexchange.report.client.container.ContextDetail;
import com.openexchange.report.client.container.ContextModuleAccessCombination;
import com.openexchange.report.client.container.MacDetail;
import com.openexchange.report.client.container.Total;
import com.openexchange.tools.encoding.Base64;

/**
 * {@link TransportHandler}
 *
 * @author <a href="mailto:vitali.sjablow@open-xchange.com">Vitali Sjablow</a>
 * @since v7.8.0
 */
public class TransportHandler {

    private static final String REPORT_SERVER_URL = "activation.open-xchange.com";

    private static final String REPORT_SERVER_CLIENT_AUTHENTICATION_STRING = "BKvKJhhjBuz6fUGvKGjGhgCkjvchgvjgaypHFGc7tvjVc76Vja";

    private static final String POST_CLIENT_AUTHENTICATION_STRING_KEY = "clientauthenticationstring";

    private static final String POST_LICENSE_KEYS_KEY = "license_keys";

    private static final String POST_METADATA_KEY = "client_information";

    private static final String URL_ENCODING = "UTF-8";

    public TransportHandler() {}

    public void sendReport(final List<Total> totals, final List<MacDetail> macDetails, final List<ContextDetail> contextDetails, Map<String, String> serverConfiguration, final String[] versions, final ClientLoginCount clc, final ClientLoginCount clcYear, final boolean savereport) throws Exception {
        final JSONObject metadata = buildJSONObject(totals, macDetails, contextDetails, serverConfiguration, versions, clc, clcYear);

        send(metadata, savereport);
    }

    private void send(JSONObject metadata, boolean savereport) throws Exception {
        final ReportConfiguration reportConfiguration = new ReportConfiguration();

        try {
            if (metadata.getBoolean("needsComposition") == true) {
                metadata.remove("macdetail");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if ("true".equals(reportConfiguration.getUseProxy().trim())) {
            System.setProperty("https.proxyHost", reportConfiguration.getProxyAddress().trim());
            System.setProperty("https.proxyPort", reportConfiguration.getProxyPort().trim());
        }

        if (savereport) {
            saveReportToHardDrive(metadata);
        } else {
            sendReport(true, reportConfiguration, metadata);
        }
    }

    /**
     * Try to send the report to the saved REPORT_SERVER_URL over https, if the given parameter is set. If
     * https fails, try it with http. If that fails, save the report to hard-drive and save location
     * to logfile.
     *
     * @param isHttps
     * @param reportConfiguration
     * @param report
     * @throws MalformedURLException
     * @throws IOException
     * @throws JSONException
     */
    private void sendReport(boolean isHttps, ReportConfiguration reportConfiguration, JSONObject metadata) throws MalformedURLException, IOException, JSONException {

        String reportString = createReportString(metadata);
        final StringBuffer report = new StringBuffer();
        report.append(POST_CLIENT_AUTHENTICATION_STRING_KEY);
        report.append('=');
        report.append(URLEncoder.encode(REPORT_SERVER_CLIENT_AUTHENTICATION_STRING, URL_ENCODING));
        report.append('&');
        report.append(POST_LICENSE_KEYS_KEY);
        report.append('=');
        report.append(URLEncoder.encode(reportConfiguration.getLicenseKeys(), URL_ENCODING));
        report.append('&');
        report.append(POST_METADATA_KEY);
        report.append('=');
        report.append(URLEncoder.encode(reportString, URL_ENCODING));

        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL("http://" + REPORT_SERVER_URL + "/").openConnection();
        if (isHttps) {
            httpURLConnection = (HttpsURLConnection) new URL("https://" + REPORT_SERVER_URL + "/").openConnection();
        }
        httpURLConnection.setConnectTimeout(2500);
        httpURLConnection.setReadTimeout(2500);
        httpURLConnection.setUseCaches(false);
        httpURLConnection.setDoOutput(true);
        if (metadata.getBoolean("needsComposition")) {
            httpURLConnection.setChunkedStreamingMode(-1); //This enforces 'sun.net.www.protocol.http.HttpURLConnection' to use a "streaming" output stream in chunked streaming mode with default chunk size.
        } else {
            httpURLConnection.setFixedLengthStreamingMode(report.length()); // This enforces 'sun.net.www.protocol.http.HttpURLConnection' to use a "streaming" output stream; otherwise outgoing data is stored into a ByteArrayOutputStream (likely causing an OOME; see Bug 57260)
        }
        httpURLConnection.setDoInput(true);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        if ("true".equals(reportConfiguration.getUseProxy().trim()) && "true".equals(reportConfiguration.getProxyAuthRequired().trim())) {
            final String proxyAutorizationProperty = "Basic " + Base64.encode((reportConfiguration.getProxyUsername().trim() + ":" + reportConfiguration.getProxyPassword().trim()).getBytes(StandardCharsets.US_ASCII));

            Authenticator.setDefault(new ProxyAuthenticator(reportConfiguration.getProxyUsername().trim(), reportConfiguration.getProxyPassword().trim()));

            httpURLConnection.setRequestProperty("Proxy-Authorization", proxyAutorizationProperty);
        }

        DataOutputStream stream = null;
        try {
            stream = new DataOutputStream(httpURLConnection.getOutputStream());
            stream.writeBytes(report.toString());
            if (metadata.getBoolean("needsComposition")) {
                appendStoredContentToOutputstream(metadata, stream, true);
            }
            stream.flush();
            stream.close();
            stream = null;
        } catch (SSLException e) {
            e.printStackTrace();
            System.err.println("Report sending failure, unable to send with SSL.");
            if (isHttps) {
                System.out.println("Trying to send without SSL.");
                sendReport(false, reportConfiguration, metadata);
                return;
            } else {
                saveReportToHardDrive(metadata);
            }
        } finally {
            Streams.close(stream);
        }

        if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            System.err.println("Report sending failure, unable to send via http...");
            if (isHttps) {
                sendReport(false, reportConfiguration, metadata);
            } else {
                saveReportToHardDrive(metadata);
            }
            throw new MalformedURLException("Problem contacting report server: " + httpURLConnection.getResponseCode());
        }
        String charset = httpURLConnection.getContentType();
        if (null != charset) {
            Pattern charsetPattern = Pattern.compile("charset= *([a-zA-Z-0-9_]+)(;|$)");
            Matcher m = charsetPattern.matcher(charset);
            charset = m.find() ? m.group(1) : null;
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), null == charset ? "UTF-8" : charset));) {
            StringBuilder sb = new StringBuilder(REPORT_SERVER_URL).append(" said: ");
            int reslen = sb.length();
            for (String line; (line = in.readLine()) != null;) {
                sb.setLength(reslen);
                System.out.println(sb.append(line).toString());
            }
        }
    }

    private String createReportString(JSONObject metadata) throws JSONException {
        String reportString = metadata.toString();
        if (metadata.getBoolean("needsComposition")) {
            reportString = reportString.substring(0, reportString.lastIndexOf("}")) + ",";
        }
        return reportString;
    }

    private void appendStoredContentToOutputstream(JSONObject metadata, DataOutputStream stream, boolean useURLEncoding) throws IOException, JSONException {
        FileInputStream is = null;
        Scanner sc = null;
        try {
            is = new FileInputStream(metadata.getString("storageFolderPath") + "/" + metadata.getString("uuid") + ".report");
            sc = new Scanner(is, "UTF-8");
            while (sc.hasNext()) {
                String line = sc.nextLine();
                if (useURLEncoding) {
                    line = URLEncoder.encode(line, URL_ENCODING);
                }
                stream.writeBytes(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                is.close();
            }
            if (sc != null) {
                sc.close();
            }
        }
        //end of report
        String endOfReport = "}";
        if (useURLEncoding) {
            endOfReport = URLEncoder.encode(endOfReport, URL_ENCODING);
        }
        stream.writeBytes(endOfReport);
    }

    /**
     * Save the given report to hard-drive and print the location into the logfile.
     *
     * @param report
     * @throws IOException
     * @throws JSONException
     */
    private void saveReportToHardDrive(final JSONObject metadata) throws IOException, JSONException {
        String reportString = createReportString(metadata);
        File tmpfile = File.createTempFile("oxreport", ".json", new File(metadata.getString("storageFolderPath")));
        System.out.println("Saving report to " + tmpfile.getAbsolutePath());
        // Compose report from stored data, depending on the report-type
        DataOutputStream tfo = new DataOutputStream(new FileOutputStream(tmpfile));
        try {
            tfo.writeBytes(reportString);
            if (metadata.getBoolean("needsComposition")) {
                appendStoredContentToOutputstream(metadata, tfo, false);
            }
        } finally {
            Streams.close(tfo);
        }
    }

    @SuppressWarnings("null")
    private JSONObject buildJSONObject(final List<Total> totals, final List<MacDetail> macDetails, final List<ContextDetail> contextDetails, Map<String, String> serverConfiguration, final String[] versions, final ClientLoginCount clc, final ClientLoginCount clcYear) throws JSONException {
        final JSONObject retval = new JSONObject();
        final JSONObject total = new JSONObject();
        final JSONObject macdetail = new JSONObject();
        final JSONObject detail = new JSONObject();
        final JSONObject version = new JSONObject();
        final JSONObject clientlogincount = new JSONObject();
        final JSONObject clientlogincountyear = new JSONObject();
        final JSONObject configuration = new JSONObject();

        final boolean wantsdetails = (null != contextDetails);

        if (wantsdetails) {
            total.put("report-format", "long");
        } else {
            total.put("report-format", "short");
        }

        for (final Total tmp : totals) {
            total.put("contexts", tmp.getContexts());
            total.put("users", tmp.getUsers());
            total.put("guests", tmp.getGuests());
            total.put("links", tmp.getLinks());
        }

        for (final MacDetail tmp : macDetails) {
            final JSONObject macDetailObjectJSON = new JSONObject();
            macDetailObjectJSON.put("id", tmp.getId());
            macDetailObjectJSON.put("count", tmp.getCount());
            macDetailObjectJSON.put("adm", tmp.getNrAdm());
            macDetailObjectJSON.put("disabled", tmp.getNrDisabled());
            macdetail.put(tmp.getId(), macDetailObjectJSON);
        }

        if (wantsdetails) {
            for (final ContextDetail tmp : contextDetails) { // Guarded by 'wantsdetails'
                final JSONObject contextDetailObjectJSON = new JSONObject();
                contextDetailObjectJSON.put("id", tmp.getId());
                contextDetailObjectJSON.put("age", tmp.getAge());
                contextDetailObjectJSON.put("created", tmp.getCreated());
                contextDetailObjectJSON.put("adminmac", tmp.getAdminmac());

                final JSONObject moduleAccessCombinations = new JSONObject();
                for (final ContextModuleAccessCombination moduleAccessCombination : tmp.getModuleAccessCombinations()) {
                    final JSONObject moduleAccessCombinationJSON = new JSONObject();
                    moduleAccessCombinationJSON.put("mac", moduleAccessCombination.getUserAccessCombination());
                    moduleAccessCombinationJSON.put("users", moduleAccessCombination.getUserCount());
                    moduleAccessCombinationJSON.put("inactive", moduleAccessCombination.getInactiveCount());
                    moduleAccessCombinations.put(moduleAccessCombination.getUserAccessCombination(), moduleAccessCombinationJSON);
                }
                contextDetailObjectJSON.put("macs", moduleAccessCombinations);
                detail.put(tmp.getId(), contextDetailObjectJSON);
            }
        }

        version.put("version", versions[0]);
        version.put("buildDate", versions[1]);

        clientlogincount.put("usm-eas", clc.getUsmeas());
        clientlogincount.put("olox2", clc.getOlox2());
        clientlogincount.put("mobileapp", clc.getMobileapp());
        clientlogincount.put("carddav", clc.getCarddav());
        clientlogincount.put("caldav", clc.getCarddav());

        clientlogincountyear.put("usm-eas", clcYear.getUsmeas());
        clientlogincountyear.put("olox2", clcYear.getOlox2());
        clientlogincountyear.put("mobileapp", clcYear.getMobileapp());
        clientlogincountyear.put("carddav", clcYear.getCarddav());
        clientlogincountyear.put("caldav", clcYear.getCarddav());

        retval.put("total", total);
        if (wantsdetails) {
            retval.put("detail", detail);
        }

        if (serverConfiguration != null) {
            for (Entry<String, String> config : serverConfiguration.entrySet()) {
                configuration.put(config.getKey(), config.getValue());
            }
            retval.put("configs", configuration);
        }
        retval.put("version", version);
        retval.put("clientlogincount", clientlogincount);
        retval.put("clientlogincountyear", clientlogincountyear);
        retval.put("macdetail", macdetail);
        return retval;
    }

    public void sendASReport(CompositeData report, boolean savereport) throws Exception {
        send(JSONServices.parseObject((String) report.get("data")), savereport);
    }
}
