package com.openexchange.ajax.chronos.itip.bugs;

import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleChange;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleEvent;
import static com.openexchange.ajax.chronos.itip.ITipUtil.constructBody;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveIMip;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.itip.AbstractITipAnalyzeTest;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.TestUserConfig;
import com.openexchange.test.common.test.pool.TestContext;
import com.openexchange.testing.httpclient.models.AnalysisChangeNewEvent;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.modules.ConfigApi;

@Execution(ExecutionMode.SAME_THREAD)
public class MWB2093Test extends AbstractITipAnalyzeTest {

	String alias;
	String username;
	ConfigApi configApi;
	/**
	 * Initializes a new {@link MWB2093Test}
	 */

	public MWB2093Test() {
		super();
	}

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        alias = "cheerio+alias";
        username = "cheerio";

        List<String> aliasesList = Arrays.asList(alias);
        TestContext testContextC2 = testContextList.get(1);
        testUserC2 = testContextC2.acquireUser(username, Optional.ofNullable(TestUserConfig.builder().withFakeAliases(aliasesList).build()));
        apiClientC2 = testUserC2.getApiClient();
    }
	
	public TestClassConfig getTestConfig(String alias) {
        return TestClassConfig.builder().withContexts(2).build();
    }

	@Test
	public void testReplyWithAlias() throws Throwable {
		/*
		 * Create event series
		 */
		String summary = "testMWB2093";
		EventData seriesToCreate = EventFactory.createSeriesEvent(0, summary, 10, defaultFolderId);
		prepareCommonAttendees(seriesToCreate);
		String eventInitializer = testUser.getLogin();
		EventData createdEvent = eventManager.createEvent(seriesToCreate, true);

		
		/*
		 * Receive mail as attendee
		 */
		MailData iMip = receiveIMip(apiClientC2, eventInitializer, summary, 0, SchedulingMethod.REQUEST);

		AnalysisChangeNewEvent newEvent = assertSingleChange(analyze(apiClientC2, iMip)).getNewEvent();
		assertNotNull(newEvent);
		assertEquals(createdEvent.getUid(), newEvent.getUid());

		/*
		 * Reply with "accepted"
		 */
		EventData attendeeEvent = assertSingleEvent(accept(apiClientC2, constructBody(iMip), null), createdEvent.getUid());
		assertTrue(attendeeEvent.getAttendees().get(1).getEmail().equals(alias), "Alias user does not reply with his alias.");
	}

	protected Attendee prepareCommonAttendees(EventData event) {
		Attendee replyingAttendee = super.prepareCommonAttendees(event);
		int context = testUserC2.getContextId();
		alias = "cheerio+alias@context" + context + ".ox.test";
		replyingAttendee.setEmail(alias);
		replyingAttendee.setUri(alias);

		return replyingAttendee;
	}
}