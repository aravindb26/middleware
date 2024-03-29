
package com.openexchange.report.appsuite.defaultHandlers;

import static com.openexchange.java.Autoboxing.L;
import static org.junit.Assert.assertEquals;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import com.openexchange.groupware.contexts.impl.ContextImpl;
import com.openexchange.report.appsuite.ContextReport;
import com.openexchange.report.appsuite.serialization.Report;
import com.openexchange.report.appsuite.serialization.ReportConfigs;

public class TotalTest {

    private ContextReport contextReport;
    private Report report;
    private Total total = new Total();

    private final String CAPS1 = "active_sync, autologin, boxcom, caldav, calendar, carddav, client-onboarding, collect_email_addresses, conflict_handling, contacts, delegate_tasks";
    private final String CAPS2 = "active_sync, autologin, boxcom, caldav, calendar, carddav, client-onboarding, collect_email_addresses, conflict_handling, contacts";
    private final String CAPS3 = "active_sync, autologin, boxcom, caldav, calendar, carddav, client-onboarding, collect_email_addresses";

    @Before
    public void setUp() {
        this.initContextReport();
        this.initReport();
    }

    //-------------------Tests-------------------

    /**
     * Test the initial addition of a context to an empty report
     */
     @Test
     public void testAddFirstContextToReport() {
        total.merge(contextReport, report);
        assertEquals(Long.valueOf(8), report.get(Report.TOTAL, Report.USERS, Long.class));
        assertEquals(Long.valueOf(3), report.get(Report.TOTAL, Report.GUESTS, Long.class));
        assertEquals(Long.valueOf(8), report.get(Report.TOTAL, Report.LINKS, Long.class));
        assertEquals(Long.valueOf(1), report.get(Report.TOTAL, Report.CONTEXTS, Long.class));
        assertEquals(Long.valueOf(1), report.get(Report.TOTAL, Report.CONTEXTS_DISABLED, Long.class));
        assertEquals(Long.valueOf(2), report.get(Report.TOTAL, Report.USERS_DISABLED, Long.class));
        assertEquals(Long.valueOf(8), report.get(Report.TOTAL, Report.CONTEXT_USERS_MAX, Long.class));
        assertEquals(Long.valueOf(8), report.get(Report.TOTAL, Report.CONTEXT_USERS_MIN, Long.class));
        assertEquals(Long.valueOf(8), report.get(Report.TOTAL, Report.CONTEXT_USERS_AVG, Long.class));
    }

    /**
     * Test the correct addition of a context to an already filled report. All mathematical operations
     * in the method are tested.
     */
     @Test
     public void testAddContextToReport() {
        fillReport();
        total.merge(contextReport, report);
        assertEquals(Long.valueOf(88), report.get(Report.TOTAL, Report.USERS, Long.class));
        assertEquals(Long.valueOf(43), report.get(Report.TOTAL, Report.GUESTS, Long.class));
        assertEquals(Long.valueOf(58), report.get(Report.TOTAL, Report.LINKS, Long.class));
        assertEquals(Long.valueOf(6), report.get(Report.TOTAL, Report.CONTEXTS, Long.class));
        assertEquals(Long.valueOf(1), report.get(Report.TOTAL, Report.CONTEXTS_DISABLED, Long.class));
        assertEquals(Long.valueOf(22), report.get(Report.TOTAL, Report.USERS_DISABLED, Long.class));
        assertEquals(Long.valueOf(30), report.get(Report.TOTAL, Report.CONTEXT_USERS_MAX, Long.class));
        assertEquals(Long.valueOf(8), report.get(Report.TOTAL, Report.CONTEXT_USERS_MIN, Long.class));
        assertEquals(Long.valueOf(14), report.get(Report.TOTAL, Report.CONTEXT_USERS_AVG, Long.class));
    }

    //-------------------Helpers-------------------
    private void initContextReport() {
        ContextImpl ctx = new ContextImpl(3);
        ctx.setEnabled(false);
        contextReport = new ContextReport(UUID.randomUUID().toString(), "default", ctx);
        Map<String, Object> macdetail = contextReport.getNamespace(Report.MACDETAIL);
        macdetail.put(CAPS1, new HashMap<String, Long>());
        macdetail.put(CAPS2, new HashMap<String, Long>());
        macdetail.put(CAPS3, new HashMap<String, Long>());
        addValuesToMap(macdetail, CAPS1, Report.ADMIN, L(1l));
        addValuesToMap(macdetail, CAPS1, Report.TOTAL, L(1l));
        addValuesToMap(macdetail, CAPS2, Report.TOTAL, L(5l));
        addValuesToMap(macdetail, CAPS2, Report.GUESTS, L(3l));
        addValuesToMap(macdetail, CAPS2, Report.DISABLED, L(2l));
        addValuesToMap(macdetail, CAPS3, Report.TOTAL, L(2l));
        addValuesToMap(macdetail, CAPS3, Report.LINKS, L(8l));
    }

    private void initReport() {
        report = new Report(UUID.randomUUID().toString(), "default", new Date().getTime());
        ReportConfigs rc = new ReportConfigs.ReportConfigsBuilder("default").consideredTimeframeStart(new Date().getTime() - 100000).consideredTimeframeEnd(new Date().getTime()).isConfigTimerange(true).build();
        report.setReportConfig(rc);
    }

    private void fillReport() {
        report.set(Report.TOTAL, Report.USERS, L(80l));
        report.set(Report.TOTAL, Report.GUESTS, L(40l));
        report.set(Report.TOTAL, Report.LINKS, L(50l));
        report.set(Report.TOTAL, Report.CONTEXTS, L(5l));
        report.set(Report.TOTAL, Report.CONTEXTS_DISABLED, L(0l));
        report.set(Report.TOTAL, Report.USERS_DISABLED, L(20l));
        report.set(Report.TOTAL, Report.CONTEXT_USERS_MAX, L(30l));
        report.set(Report.TOTAL, Report.CONTEXT_USERS_MIN, L(10l));
        report.set(Report.TOTAL, Report.CONTEXT_USERS_AVG, L(16l));
    }

    private void addValuesToMap(Map<String, Object> macdetail, String macDetailKey, String insertKey, Long insertValue) {
        HashMap<String, Long> counts = new HashMap<>();
        counts.put(insertKey, insertValue);
        ((HashMap<String, Long>) macdetail.get(macDetailKey)).put(insertKey, insertValue);
    }

}
