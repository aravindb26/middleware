
package com.openexchange.ajax;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.appointment.AbstractResourceAwareAjaxSession;
import com.openexchange.resource.Resource;

public class ResourceTest extends AbstractResourceAwareAjaxSession {

    @Test
    public void testSearch() throws Exception {
        final List<Resource> resources = resTm.search("*");
        assertTrue(resources.size() > 0, "resource array size is not > 0");
    }

    @Test
    public void testList() throws Exception {
        List<Resource> resources = resTm.search("*");
        assertTrue(resources.size() > 0, "resource array size is not > 0");

        final int[] id = new int[resources.size()];
        for (int a = 0; a < id.length; a++) {
            id[a] = resources.get(a).getIdentifier();
        }

        resources = resTm.list(id);
        assertTrue(resources.size() > 0, "resource array size is not > 0");
    }

    @Test
    public void testGet() throws Exception {
        final List<Resource> resources = resTm.search("*");
        assertTrue(resources.size() > 0, "resource array size is not > 0");
        Resource res = resTm.get(resources.get(0).getIdentifier());
        assertNotNull(res);
    }
}
