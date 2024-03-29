
package com.openexchange.ajax.mail.addresscollector;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.config.actions.GetRequest;
import com.openexchange.ajax.config.actions.GetResponse;
import com.openexchange.ajax.config.actions.SetRequest;
import com.openexchange.ajax.config.actions.SetResponse;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.ajax.framework.AbstractAJAXSession;


public class ConfigurationTest extends AbstractAJAXSession {

    public ConfigurationTest() {
        super();
    }

    @Test
    public void testFolderId() throws Throwable {
        SetRequest setRequest = new SetRequest(Tree.ContactCollectFolder, I(100));
        SetResponse setResponse = getClient().execute(setRequest);
        assertFalse(setResponse.hasError());

        GetRequest getRequest = new GetRequest(Tree.ContactCollectFolder);
        GetResponse getResponse = getClient().execute(getRequest);
        assertEquals(100, getResponse.getInteger());

        setRequest = new SetRequest(Tree.ContactCollectFolder, I(123));
        setResponse = getClient().execute(setRequest);
        assertFalse(setResponse.hasError());

        getRequest = new GetRequest(Tree.ContactCollectFolder);
        getResponse = getClient().execute(getRequest);
        assertEquals(123, getResponse.getInteger());
    }
}
