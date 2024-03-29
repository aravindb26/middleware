
package com.openexchange.ajax.user;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.ajax.user.actions.AllRequest;
import com.openexchange.ajax.user.actions.AllResponse;

/**
 * {@link AllTest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class AllTest extends AbstractAJAXSession {

    /**
     * Initializes a new {@link AllTest}.
     *
     * @param name
     */
    public AllTest() {
        super();
    }

    @BeforeEach
    @Test
    public void testAllSimple() throws Exception {

        final AllRequest request = new AllRequest(null);
        final AllResponse response = Executor.execute(getClient(), request);

        final JSONArray users = (JSONArray) response.getData();

        assertTrue(users.length() > 0, "Empty but shouldn't");
    }

    @Test
    public void testAllWithSort() throws Exception {

        final AllRequest request = new AllRequest(new int[] { 615 });
        request.setSortColumn(I(615)); // logininfo
        final AllResponse response = Executor.execute(getClient(), request);

        final JSONArray users = (JSONArray) response.getData();

        assertTrue(users.length() > 0, "Empty but shouldn't: " + users);

        // System.out.println(users);
    }

    @Test
    public void testAllWithSort2() throws Exception {

        final AllRequest request = new AllRequest(new int[] { 555 });
        request.setSortColumn(I(555)); // email1
        final AllResponse response = Executor.execute(getClient(), request);

        final JSONArray users = (JSONArray) response.getData();

        assertTrue(users.length() > 0, "Empty but shouldn't: " + users);

        // System.out.println(users);
    }

}
