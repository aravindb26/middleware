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

package com.openexchange.ajax.contact;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.openexchange.groupware.container.Contact;

public class Bug18862Test extends AbstractManagedContactTest {

    /**
     * Size of the contact image in bytes, should be larger than the
     * configured <code>max_image_size</code>.
     */
    String expectedTitles[] = {"UPL-0017","CON-0172"};
    List<String> expectedTitlesList = Arrays.asList(expectedTitles);

    private static byte[] bytes;

    public Bug18862Test() {
        super();
    }

    @Test
    public void testUploadTooLargeImage() {
        try {
            String filePath = "testData/18862.jpg";
            bytes = Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        final Contact contact = super.generateContact();
        contact.setImage1(bytes);
        contact.setImageContentType("image/jpg");
        contact.setNumberOfImages(1);
        super.cotm.newAction(contact);
        assertNotNull(super.cotm.getLastResponse(), "got no response");
        assertNotNull(super.cotm.getLastResponse().getException(), "no exception thrown");
        assertTrue(expectedTitlesList.contains((super.cotm.getLastResponse().getException().getErrorCode())), "unexpected error code");
    }
}
