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

package com.openexchange.dav.carddav.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Date;
import java.util.Map;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.client.methods.ReportMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.junit.jupiter.api.Assertions;
import com.openexchange.dav.Headers;
import com.openexchange.dav.PropertyNames;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.carddav.Photos;
import com.openexchange.dav.carddav.VCardResource;
import com.openexchange.dav.carddav.reports.AddressbookMultiGetReportInfo;
import com.openexchange.groupware.container.Contact;
import net.sourceforge.cardme.vcard.VCard;
import net.sourceforge.cardme.vcard.arch.EncodingType;
import net.sourceforge.cardme.vcard.types.PhotoType;
import net.sourceforge.cardme.vcard.types.media.ImageMediaType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link ImageURITest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.3
 */
public class ImageURITest extends CardDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateOnServerAsURI(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        testCreateOnServer("photo=uri");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateOnServerBinary(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        testCreateOnServer("photo=binary");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateOnClientAsURI(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        testCreateOnClient("photo=uri");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateOnClientBinary(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        testCreateOnClient("photo=binary");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testUpdateOnClientAsURI(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        testUpdateOnClient("photo=uri");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testUpdateOnClientBinary(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        testUpdateOnClient("photo=binary");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRemoveOnClientAsURI(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        testRemoveOnClient("photo=uri");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRemoveOnClientBinary(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        testRemoveOnClient("photo=binary");
    }

    private void testCreateOnServer(String prefer) throws Exception {
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * create contact on server
         */
        String uid = randomUID();
        Contact contact = new Contact();
        contact.setImage1(Photos.PNG_100x100.getBytes());
        contact.setImageContentType("image/png");
        contact.setUid(uid);
        rememberForCleanUp(create(contact));
        /*
         * sync collection
         */
        Map<String, String> eTags = syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        String href = eTags.keySet().iterator().next();
        /*
         * get & verify photo in vCard
         */
        verifyPhoto(href, contact.getImage1(), prefer);
    }

    private void testCreateOnClient(String prefer) throws Exception {
        /*
         * create contact on client
         */
        String uid = randomUID();
        String href = buildVCardHref(uid);
        VCardResource vCard = new VCardResource(
            "BEGIN:VCARD" + "\r\n" +
            "PRODID:-//Example Inc.//Example Client 1.0//EN" + "\r\n" +
            "VERSION:3.0" + "\r\n" +
            "UID:" + uid + "\r\n" +
            "REV:" + formatAsUTC(new Date()) + "\r\n" +
            "END:VCARD" + "\r\n", href, null)
        ;
        PhotoType photo = new PhotoType();
        photo.setImageMediaType(ImageMediaType.PNG);
        photo.setEncodingType(EncodingType.BINARY);
        photo.setPhoto(Photos.PNG_100x100.getBytes());
        vCard.getVCard().addPhoto(photo);
        PutMethod put = null;
        try {
            put = new PutMethod(getBaseUri() + href);
            put.addRequestHeader("Prefer", prefer);
            put.addRequestHeader(Headers.IF_NONE_MATCH, "*");
            put.setRequestEntity(new StringRequestEntity(vCard.toString(), "text/vcard", "UTF-8"));
            assertEquals(StatusCodes.SC_CREATED, webDAVClient.executeMethod(put), "Response code wrong");
        } finally {
            release(put);
        }
        /*
         * get & verify contact on server
         */
        Contact contact = getContact(uid);
        assertNotNull(contact);
        Assertions.assertArrayEquals(Photos.PNG_100x100.getBytes(), contact.getImage1(), "image data wrong");
        /*
         * get & verify photo in vCard
         */
        verifyPhoto(href, contact.getImage1(), prefer);
    }

    private void testUpdateOnClient(String prefer) throws Exception {
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * create contact on server
         */
        String uid = randomUID();
        Contact contact = new Contact();
        contact.setImage1(Photos.PNG_100x100.getBytes());
        contact.setImageContentType("image/png");
        contact.setUid(uid);
        rememberForCleanUp(create(contact));
        /*
         * sync collection
         */
        Map<String, String> eTags = syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        String href = eTags.keySet().iterator().next();
        /*
         * get & verify photo in vCard
         */
        VCardResource vCard = verifyPhoto(href, contact.getImage1(), prefer);
        /*
         * update photo on client
         */
        vCard.getVCard().removePhoto(vCard.getVCard().getPhotos().get(0));
        PhotoType photo = new PhotoType();
        photo.setImageMediaType(ImageMediaType.PNG);
        photo.setEncodingType(EncodingType.BINARY);
        photo.setPhoto(Photos.PNG_200x200.getBytes());
        vCard.getVCard().addPhoto(photo);
        putVCardUpdate(uid, vCard.toString(), vCard.getETag());
        /*
         * get & verify contact on server
         */
        contact = getContact(uid);
        assertNotNull(contact);
        Assertions.assertArrayEquals(Photos.PNG_200x200.getBytes(), contact.getImage1(), "image data wrong");
        /*
         * get & verify photo in vCard
         */
        verifyPhoto(href, Photos.PNG_200x200.getBytes(), prefer);
    }

    private void testRemoveOnClient(String prefer) throws Exception {
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * create contact on server
         */
        String uid = randomUID();
        Contact contact = new Contact();
        contact.setImage1(Photos.PNG_100x100.getBytes());
        contact.setImageContentType("image/png");
        contact.setUid(uid);
        rememberForCleanUp(create(contact));
        /*
         * sync collection
         */
        Map<String, String> eTags = syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        String href = eTags.keySet().iterator().next();
        /*
         * get & verify photo in vCard
         */
        VCardResource vCard = verifyPhoto(href, contact.getImage1(), prefer);
        /*
         * remove photo on client
         */
        vCard.getVCard().removePhoto(vCard.getVCard().getPhotos().get(0));
        putVCardUpdate(uid, vCard.toString(), vCard.getETag());
        /*
         * get & verify contact on server
         */
        contact = getContact(uid);
        assertNotNull(contact, "Contact should not be null");
        assertNull(contact.getImage1(), "Contact image should be null");
        /*
         * get & verify photo in vCard
         */
        verifyPhoto(href, null, prefer);
    }

    private void verifyPhoto(byte[] expectedPhoto, VCard vCard, String prefer) throws Exception {
        if (null == expectedPhoto) {
            assertTrue(null == vCard.getPhotos() || 0 == vCard.getPhotos().size(), "PHOTO wrong");
        } else {
            assertTrue(null != vCard.getPhotos() && 0 < vCard.getPhotos().size(), "PHOTO wrong");
            PhotoType photoProperty = vCard.getPhotos().get(0);
            if ("photo=uri".equals(prefer)) {
                URI photoURI = photoProperty.getPhotoURI();
                assertNotNull(photoURI, "POHTO wrong");
                Assertions.assertArrayEquals(expectedPhoto, downloadPhoto(photoURI), "image data wrong");
            } else {
                byte[] vCardPhoto = photoProperty.getPhoto();
                assertNotNull(vCardPhoto, "POHTO wrong");
                Assertions.assertArrayEquals(expectedPhoto, vCardPhoto, "image data wrong");
            }
        }
    }

    private byte[] downloadPhoto(URI uri) throws Exception {
        GetMethod get = null;
        try {
            get = new GetMethod(uri.toString());
            assertEquals(StatusCodes.SC_OK, webDAVClient.executeMethod(get));
            return get.getResponseBody();
        } finally {
            release(get);
        }
    }

    private VCardResource verifyPhoto(String href, byte[] expectedPhoto, String prefer) throws Exception {
        /*
         * get & verify vCard via plain GET
         */
        VCardResource card = get(href, prefer);
        assertNotNull(card);
        verifyPhoto(expectedPhoto, card.getVCard(), prefer);
        /*
         * get & verify vCard via addressbook-multiget REPORT
         */
        card = addressbookMultiGet(href, prefer);
        assertNotNull(card);
        verifyPhoto(expectedPhoto, card.getVCard(), prefer);
        /*
         * get & verify vCard via PROPFIND
         */
        card = propFind(href, prefer);
        assertNotNull(card);
        verifyPhoto(expectedPhoto, card.getVCard(), prefer);
        return card;
    }

    private VCardResource get(String href, String prefer) throws Exception {
        GetMethod get = null;
        try {
            get = new GetMethod(webDAVClient.getBaseURI() + href);
            get.setRequestHeader("Prefer", prefer);
            String vCard = webDAVClient.doGet(get);
            if (null == vCard) {
                return null;
            }
            Header eTagHeader = get.getResponseHeader("ETag");
            String eTag = null != eTagHeader ? eTagHeader.getValue() : null;
            return new VCardResource(vCard, href, eTag);
        } finally {
            release(get);
        }
    }

    private VCardResource addressbookMultiGet(String href, String prefer) throws Exception {
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.GETETAG);
        props.add(PropertyNames.ADDRESS_DATA);
        ReportInfo reportInfo = new AddressbookMultiGetReportInfo(new String[] { href }, props);
        ReportMethod report = null;
        MultiStatusResponse response = null;
        try {
            report = new ReportMethod(webDAVClient.getBaseURI() + buildCollectionHref(getDefaultCollectionName()), reportInfo);
            report.setRequestHeader("Prefer", prefer);
            response = assertSingleResponse(webDAVClient.doReport(report, StatusCodes.SC_MULTISTATUS));
        } finally {
            release(report);
        }
        String eTag = extractTextContent(PropertyNames.GETETAG, response);
        String data = extractTextContent(PropertyNames.ADDRESS_DATA, response);
        return new VCardResource(data, href, eTag);
    }

    private VCardResource propFind(String href, String prefer) throws Exception {
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.GETETAG);
        props.add(PropertyNames.ADDRESS_DATA);
        MultiStatusResponse response = null;
        PropFindMethod propFind = null;
        try {
            propFind = new PropFindMethod(getBaseUri() + href, DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
            propFind.setRequestHeader("Prefer", prefer);
            response = assertSingleResponse(webDAVClient.doPropFind(propFind, StatusCodes.SC_MULTISTATUS));
        } finally {
            release(propFind);
        }
        String eTag = extractTextContent(PropertyNames.GETETAG, response);
        String data = extractTextContent(PropertyNames.ADDRESS_DATA, response);
        return new VCardResource(data, href, eTag);
    }

}
