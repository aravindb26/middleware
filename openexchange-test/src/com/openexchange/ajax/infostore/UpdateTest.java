
package com.openexchange.ajax.infostore;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import com.google.common.collect.Iterables;
import com.openexchange.ajax.InfostoreAJAXTest;
import com.openexchange.ajax.framework.AbstractAJAXResponse;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.infostore.utils.Metadata;
import com.openexchange.test.common.test.OXTestToolkit;
import com.openexchange.test.common.test.TestInit;

public class UpdateTest extends InfostoreAJAXTest {

    public static final int SIZE = 15; // Size of the large file in Megabytes

    String LOREM_IPSUM = "[32] Sed ut perspiciatis, unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam eaque ipsa, quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt, explicabo. Nemo enim ipsam voluptatem, quia voluptas sit, aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos, qui ratione voluptatem sequi nesciunt, neque porro quisquam est, qui dolorem ipsum, quia dolor sit, amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt, ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit, qui in ea voluptate velit esse, quam nihil molestiae consequatur, vel illum, qui dolorem eum fugiat, quo voluptas nulla pariatur? [33] At vero eos et accusamus et iusto odio dignissimos ducimus, qui blanditiis praesentium voluptatum deleniti atque corrupti, quos dolores et quas molestias excepturi sint, obcaecati cupiditate non provident, similique sunt in culpa, qui officia deserunt mollitia animi, id est laborum et dolorum fuga. Et harum quidem rerum facilis est et expedita distinctio. Nam libero tempore, cum soluta nobis est eligendi optio, cumque nihil impedit, quo minus id, quod maxime placeat, facere possimus, omnis voluptas assumenda est, omnis dolor repellendus. Temporibus autem quibusdam et aut officiis debitis aut rerum necessitatibus saepe eveniet, ut et voluptates repudiandae sint et molestiae non recusandae. Itaque earum rerum hic tenetur a sapiente delectus, ut aut reiciendis voluptatibus maiores alias consequatur aut perferendis doloribus asperiores repellat.";

    @Test
    public void testBasic() throws Exception {
        final String id = Iterables.get(itm.getCreatedEntities(), 0).getId();
        com.openexchange.file.storage.File file = itm.getAction(id);
        final String description = file.getDescription();
        file.setTitle("test knowledge updated");
        file.setColorLabel(1);

        itm.updateAction(file, new com.openexchange.file.storage.File.Field[] { com.openexchange.file.storage.File.Field.TITLE, com.openexchange.file.storage.File.Field.COLOR_LABEL }, new Date(Long.MAX_VALUE));
        checkResponse();
        assertNotNull(itm.getLastResponse().getTimestamp());

        com.openexchange.file.storage.File object = itm.getAction(id);

        assertEquals("test knowledge updated", object.getTitle());
        assertEquals(description, object.getDescription());
        assertEquals(1, object.getColorLabel());
        assertEquals("1", object.getVersion());

    }

    @Test
    public void testLongDescription() throws Exception {
        descriptionRoundtrip(LOREM_IPSUM);
    }

    @Test
    public void testCharset() throws Exception {
        descriptionRoundtrip("H\u00f6l\u00f6\u00f6\u00f6\u00f6\u00f6\u00f6\u00f6");
    }

    private void descriptionRoundtrip(final String desc) throws Exception {
        final String origId = Iterables.get(itm.getCreatedEntities(), 0).getId();
        com.openexchange.file.storage.File file = itm.getAction(origId);
        file.setDescription(desc);
        itm.updateAction(file, new com.openexchange.file.storage.File.Field[] { com.openexchange.file.storage.File.Field.DESCRIPTION }, new Date(Long.MAX_VALUE));
        checkResponse();
        assertNotNull(itm.getLastResponse().getTimestamp());

        com.openexchange.file.storage.File obj = itm.getAction(origId);
        assertEquals(desc, obj.getDescription());
    }

    @Test
    public void testUpload() throws Exception {
        final File upload = new File(TestInit.getTestProperty("ajaxPropertiesFile"));

        com.openexchange.file.storage.File urOrig = Iterables.get(itm.getCreatedEntities(), 0);
        final String id = urOrig.getId();

        com.openexchange.file.storage.File org = itm.getAction(id);
        itm.updateAction(org, upload, new com.openexchange.file.storage.File.Field[] {}, new Date(Long.MAX_VALUE));
        checkResponse();

        com.openexchange.file.storage.File obj = itm.getAction(id);

        assertEquals("2", obj.getVersion());
        assertEquals(urOrig.getFileMIMEType(), obj.getFileMIMEType());
        assertEquals(upload.getName(), obj.getFileName());

        InputStream is = null;
        InputStream is2 = null;
        try {
            is = new FileInputStream(upload);
            is2 = itm.document(Integer.toString(folderId), id, "1");
            OXTestToolkit.assertSameContent(is, is2);
        } finally {
            if (is != null) {
                is.close();
            }
            if (is2 != null) {
                is2.close();
            }
        }
    }

    @Test
    public void testUploadEmptyFile() throws IOException, JSONException, OXException {
        final File emptyFile = File.createTempFile("infostore-new-test", ".txt");

        final String id = Iterables.get(itm.getCreatedEntities(), 0).getId();

        com.openexchange.file.storage.File org = itm.getAction(id);
        itm.updateAction(org, emptyFile, new com.openexchange.file.storage.File.Field[] {}, new Date(Long.MAX_VALUE));
        checkResponse();

        com.openexchange.file.storage.File obj = itm.getAction(id);
        assertEquals("2", obj.getVersion());
        assertEquals("text/plain", obj.getFileMIMEType());
        assertEquals(emptyFile.getName(), obj.getFileName());
        emptyFile.delete();
    }

    //Bug 4120
    @Test
    public void testUniqueFilenamesOnUpload() throws Exception {
        final File upload = new File(TestInit.getTestProperty("ajaxPropertiesFile"));

        final String id = Iterables.get(itm.getCreatedEntities(), 0).getId();

        com.openexchange.file.storage.File org = itm.getAction(id);
        itm.updateAction(org, upload, new com.openexchange.file.storage.File.Field[] {}, new Date(Long.MAX_VALUE));
        checkResponse();

        final String id2 = itm.createFileOnServer(folderId, "otherFile", "text/javascript").getId();

        com.openexchange.file.storage.File org2 = itm.getAction(id2);
        itm.updateAction(org2, upload, new com.openexchange.file.storage.File.Field[] {}, new Date(Long.MAX_VALUE));
        checkResponse();

        com.openexchange.file.storage.File obj = itm.getAction(id2);
        assertFalse(upload.getName().equals(obj.getFileName()));
    }

    //Bug 4120
    @Test
    public void testUniqueFilenamesOnSwitchVersions() throws Exception {
        final File upload = new File(TestInit.getTestProperty("ajaxPropertiesFile"));

        com.openexchange.file.storage.File orig = createFile(folderId, "firstfile");
        orig.setFileMIMEType("text/plain");
        orig.setDescription("first_desc");
        orig.setFileName("first.txt");
        itm.newAction(orig, upload);
        final String id = orig.getId();

        com.openexchange.file.storage.File org = itm.getAction(id);
        org.setFileName("theFile.txt");
        itm.updateAction(org, upload, new com.openexchange.file.storage.File.Field[] {com.openexchange.file.storage.File.Field.FILENAME}, new Date(Long.MAX_VALUE));
        checkResponse();

        itm.updateAction(org, upload, new com.openexchange.file.storage.File.Field[] {}, new Date(Long.MAX_VALUE));
        checkResponse();

        com.openexchange.file.storage.File id2 = createFile(folderId, "otherFile");
        id2.setFileMIMEType("text/plain");
        id2.setDescription("other_desc");
        id2.setFileName("theFile.txt");
        itm.newAction(id2, upload);

        itm.getAction(id);
        org.setTitle("otherTitle");
        org.setVersion("1");
        itm.updateAction(org, new com.openexchange.file.storage.File.Field[] { com.openexchange.file.storage.File.Field.VERSION, com.openexchange.file.storage.File.Field.TITLE }, new Date(Long.MAX_VALUE));
        assertFalse(itm.getLastResponse().hasConflicts());

        com.openexchange.file.storage.File reloaded = itm.getAction(id);

        assertEquals(orig.getFileName(), reloaded.getFileName());
    }

    @Test
    public void testSwitchVersion() throws Exception {
        final File upload = new File(TestInit.getTestProperty("ajaxPropertiesFile"));

        final String id = Iterables.get(itm.getCreatedEntities(), 0).getId();
        com.openexchange.file.storage.File org = itm.getAction(id);

        itm.updateAction(org, upload, new com.openexchange.file.storage.File.Field[] {}, new Date(Long.MAX_VALUE)); // V2
        checkResponse();

        itm.updateAction(org, upload, new com.openexchange.file.storage.File.Field[] {}, new Date(Long.MAX_VALUE)); // V3
        checkResponse();

        itm.updateAction(org, upload, new com.openexchange.file.storage.File.Field[] {}, new Date(Long.MAX_VALUE)); // V4
        checkResponse();

        com.openexchange.file.storage.File org2 = itm.getAction(id);
        org2.setVersion("2");

        itm.updateAction(org2, new com.openexchange.file.storage.File.Field[] { com.openexchange.file.storage.File.Field.VERSION }, itm.getLastResponse().getTimestamp());
        checkResponse();

        com.openexchange.file.storage.File obj = itm.getAction(id);
        assertEquals("2", obj.getVersion(), "Version does not match.");

        itm.versions(id, new int[] { Metadata.VERSION, Metadata.CURRENT_VERSION });
        checkResponse();

        VersionsTest.assureVersions(new Integer[] { I(1), I(2), I(3), I(4) }, itm.getLastResponse(), I(2));
    }

    private void checkResponse() {
        assertFalse(itm.getLastResponse().hasError(), "Expected no error but has one: " + itm.getLastResponse().getErrorMessage());
    }

    @Test
    public void testUpdateCurrentVersionByDefault() throws Exception {
        final String id = Iterables.get(itm.getCreatedEntities(), 0).getId();

        com.openexchange.file.storage.File file = itm.getAction(id);
        itm.updateAction(file, new com.openexchange.file.storage.File.Field[] {}, new Date(Long.MAX_VALUE));
        AbstractAJAXResponse response = itm.getLastResponse();
        assertFalse(response.hasConflicts());
        checkResponse();

        itm.updateAction(file, new com.openexchange.file.storage.File.Field[] {}, new Date(Long.MAX_VALUE));
        response = itm.getLastResponse();
        assertFalse(response.hasConflicts());
        checkResponse();

        file.setDescription("New Description");
        itm.updateAction(file, new com.openexchange.file.storage.File.Field[] { com.openexchange.file.storage.File.Field.DESCRIPTION }, new Date(Long.MAX_VALUE));
        response = itm.getLastResponse();

        com.openexchange.file.storage.File obj = itm.getAction(id);

        assertEquals("New Description", obj.getDescription());
    }

    // Bug 3928
    @Test
    public void testVersionCommentForNewVersion() throws Exception {
        final File upload = new File(TestInit.getTestProperty("ajaxPropertiesFile"));

        final String id = Iterables.get(itm.getCreatedEntities(), 0).getId();

        com.openexchange.file.storage.File org = itm.getAction(id);
        org.setVersionComment("Version Comment");
        itm.updateAction(org, upload, new com.openexchange.file.storage.File.Field[] { com.openexchange.file.storage.File.Field.VERSION_COMMENT }, new Date(Long.MAX_VALUE));
        checkResponse();

        com.openexchange.file.storage.File obj = itm.getAction(id);
        assertEquals("Version Comment", obj.getVersionComment());
    }

    //Bug 4269
    @Test
    public void testVirtualFolder() throws Exception {
        for (int folderId : virtualFolders) {
            virtualFolderTest(folderId);
        }
    }

    // Bug 4269
    public void virtualFolderTest(int folderId) throws Exception {
        final String id = Iterables.get(itm.getCreatedEntities(), 0).getId();
        com.openexchange.file.storage.File file = itm.getAction(id);
        file.setFolderId(String.valueOf(folderId));
        itm.updateAction(file, new com.openexchange.file.storage.File.Field[] { com.openexchange.file.storage.File.Field.FOLDER_ID }, new Date(Long.MAX_VALUE));
        AbstractAJAXResponse response = itm.getLastResponse();
        assertTrue(response.hasError());
    }

}
