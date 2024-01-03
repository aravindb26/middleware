///*
// * @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
// * @license AGPL-3.0
// *
// * This code is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
// *
// * Any use of the work other than as authorized under this license or copyright law is prohibited.
// *
// */
//
//package com.openexchange.ajax.mailcompose;
//
//import static com.openexchange.java.Autoboxing.l;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import java.util.Collections;
//import java.util.UUID;
//import org.json.JSONObject;
//import org.junit.jupiter.api.Test;
//import com.google.gson.Gson;
//import com.openexchange.java.Strings;
//import com.openexchange.testing.httpclient.models.ComposeBody;
//import com.openexchange.testing.httpclient.models.MailComposeAttachmentResponse;
//import com.openexchange.testing.httpclient.models.MailComposeResponse;
//import com.openexchange.testing.httpclient.models.MailComposeResponseMessageModel;
//import com.openexchange.testing.httpclient.models.MailComposeSendResponse;
//import com.openexchange.testing.httpclient.models.MailDestinationData;
//
///**
// * {@link AttachmentsTest}
// *
// * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
// * @since v7.10.2
// */
//public class AttachmentsTest extends AbstractMailComposeTest {
//
//    @Test
//    public void testAttachment() throws Exception {
//        MailComposeResponseMessageModel model = createNewCompositionSpace();
//        check(api.postAttachments(model.getId(), attachment, null));
//        MailComposeResponse response = api.getMailComposeById(model.getId());
//        assertNotNull(response.getData().getAttachments(), "Expected attachments.");
//        check(response);
//        assertEquals(1, response.getData().getAttachments().size(), "Expected one attachment.");
//        assertEquals(attachment.getName(), response.getData().getAttachments().get(0).getName(), "Wrong attachment name.");
//        assertTrue(l(response.getData().getAttachments().get(0).getSize()) > 0L, "Empty file.");
//    }
//
//    @Test
//    public void testReferencedAttachment() throws Exception {
//        MailDestinationData mailWithAttachment = importTestMailWithAttachment();
//        String jAttachment = new JSONObject(4).put("id", mailWithAttachment.getId()).put("folderId", mailWithAttachment.getFolderId()).put("attachmentId", "2").put("origin", "mail") .toString();
//
//        MailComposeResponseMessageModel model = createNewCompositionSpace();
//        check(api.postReferencedAttachments(model.getId(), jAttachment, null));
//
//        MailComposeResponse response = api.getMailComposeById(model.getId());
//        assertNotNull(response.getData().getAttachments(), "Expected attachments.");
//        check(response);
//        assertEquals(1, response.getData().getAttachments().size(), "Expected one attachment.");
//        assertEquals("empty.pdf", response.getData().getAttachments().get(0).getName(), "Wrong attachment name.");
//        assertTrue(l(response.getData().getAttachments().get(0).getSize()) > 0L, "Empty file.");
//    }
//
//    @Test
//    public void testSendWithAttachment() throws Exception {
//        MailComposeResponseMessageModel model = createNewCompositionSpace();
//        check(api.postAttachments(model.getId(), attachment, null));
//
//        MailComposeResponse loaded = api.getMailComposeById(model.getId());
//        check(loaded);
//        assertNotNull(loaded);
//        assertEquals(model.getId(), loaded.getData().getId(), "Wrong Composition Space loaded.");
//
//        model.setFrom(getSender());
//        model.setTo(getRecipient());
//        model.setSubject(UUID.randomUUID().toString());
//        model.setContent(UUID.randomUUID().toString());
//
//        MailComposeSendResponse postMailComposeSend = api.postMailComposeSend(model.getId(), new Gson().toJson(model), null, attachment2);
//        check(postMailComposeSend);
//        assertTrue(Strings.isEmpty(postMailComposeSend.getError()), postMailComposeSend.getErrorDesc());
//
//        loaded = api.getMailComposeById(model.getId());
//        assertEquals("MSGCS-0007", loaded.getCode(), "Error expected.");
//        assertNull(loaded.getData(), "No data expected");
//    }
//
//    @Test
//    public void testReplaceAttachment() throws Exception {
//        MailComposeResponseMessageModel model = createNewCompositionSpace();
//        check(api.postAttachments(model.getId(), attachment, null));
//        MailComposeResponse response = api.getMailComposeById(model.getId());
//        check(response);
//        Long size1 = response.getData().getAttachments().get(0).getSize();
//
//        MailComposeAttachmentResponse updateresponse = api.postAttachmentsById(model.getId(), response.getData().getAttachments().get(0).getId(), attachment2, null);
//
//        check(updateresponse);
//        response = api.getMailComposeById(model.getId());
//        check(response);
//
//        assertNotNull(response.getData().getAttachments(), "Expected attachments.");
//        assertEquals(1, response.getData().getAttachments().size(), "Expected one attachment.");
//        assertEquals(attachment2.getName(), response.getData().getAttachments().get(0).getName(), "Wrong attachment name.");
//        assertTrue(l(response.getData().getAttachments().get(0).getSize()) > 0L, "Empty file.");
//        assertNotEquals(size1, response.getData().getAttachments().get(0).getSize(), "Expected different file size.");
//    }
//
//    @Test
//    public void testOriginalAttachment() throws Exception {
//        MailDestinationData mailWithAttachment = importTestMailWithAttachment();
//
//        ComposeBody body = new ComposeBody();
//        body.setFolderId(mailWithAttachment.getFolderId());
//        body.setId(mailWithAttachment.getId());
//        MailComposeResponse reply = api.postMailCompose("REPLY", null, null, null, Collections.singletonList(body));
//
//        check(reply);
//        MailComposeResponseMessageModel data = reply.getData();
//        compositionSpaceIds.add(data.getId());
//
//        MailComposeResponse response = api.getMailComposeById(data.getId());
//        check(response);
//        assertEquals(0, response.getData().getAttachments().size(), "Expected no attachment.");
//
//        check(api.postAttachmentsOriginal(data.getId(), null));
//        response = api.getMailComposeById(data.getId());
//        check(response);
//        assertNotNull(response.getData().getAttachments(), "Expected attachments.");
//        assertEquals(1, response.getData().getAttachments().size(), "Expected one attachment.");
//        assertTrue(l(response.getData().getAttachments().get(0).getSize()) > 0L, "Empty file.");
//    }
//
//    @Test
//    public void testVcard() throws Exception {
//        MailComposeResponseMessageModel model = createNewCompositionSpace();
//        check(api.postAttachmentsVcard(model.getId(), null));
//        MailComposeResponse response = api.getMailComposeById(model.getId());
//        check(response);
//        assertNotNull(response.getData().getAttachments(), "Expected attachments.");
//        assertEquals(1, response.getData().getAttachments().size(), "Expected one attachment.");
//        assertTrue(response.getData().getAttachments().get(0).getName().endsWith(".vcf"), "Wrong attachment name.");
//        assertTrue(l(response.getData().getAttachments().get(0).getSize()) > 0L, "Empty file.");
//    }
//
//    @Test
//    public void testDeleteAttachment() throws Exception {
//        MailComposeResponseMessageModel model = createNewCompositionSpace();
//        check(api.postAttachments(model.getId(), attachment, null));
//        MailComposeResponse response = api.getMailComposeById(model.getId());
//        check(response);
//        assertNotNull(response.getData().getAttachments(), "Expected attachments.");
//        assertEquals(1, response.getData().getAttachments().size(), "Expected one attachment.");
//        assertEquals(attachment.getName(), response.getData().getAttachments().get(0).getName(), "Wrong attachment name.");
//        assertTrue(l(response.getData().getAttachments().get(0).getSize()) > 0L, "Empty file.");
//
//        MailComposeAttachmentResponse deleteResp = api.deleteAttachmentsById(model.getId(), response.getData().getAttachments().get(0).getId(), null);
//        check(deleteResp);
//        assertEquals(0, deleteResp.getData().getAttachments().size(), "Expected no attachment.");
//        response = api.getMailComposeById(model.getId());
//        check(response);
//        assertEquals(0, response.getData().getAttachments().size(), "Expected no attachment.");
//    }
//
//    @Test
//    public void testGetAttachment() throws Exception {
//        MailComposeResponseMessageModel model = createNewCompositionSpace();
//        MailComposeAttachmentResponse postAttachments = api.postAttachments(model.getId(), attachment, null);
//        check(postAttachments);
//        byte[] attachmentsById = api.getAttachmentsById(model.getId(), postAttachments.getData().getAttachments().get(0).getId());
//        assertTrue(attachmentsById.length > 100, "No data.");
//    }
//}
