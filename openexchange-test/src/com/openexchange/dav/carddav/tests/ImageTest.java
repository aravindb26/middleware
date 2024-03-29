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

import static com.openexchange.java.Autoboxing.I;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.carddav.Photos;
import com.openexchange.dav.carddav.VCardResource;
import com.openexchange.groupware.container.Contact;
import net.sourceforge.cardme.vcard.arch.EncodingType;
import net.sourceforge.cardme.vcard.types.PhotoType;
import net.sourceforge.cardme.vcard.types.media.ImageMediaType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link ImageTest} - Tests contact images via the CardDAV interface
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ImageTest extends CardDAVTest {

    public ImageTest() {
        super();
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCroppedImage(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        String syncToken = super.fetchSyncToken();
        /*
         * create contact
         */
        String uid = randomUID();
        String firstName = "bild";
        String lastName = "otto";
        String vCard = "BEGIN:VCARD" + "\r\n" + "VERSION:3.0" + "\r\n" + "PRODID:-//Apple Inc.//Address Book 6.1//EN" + "\r\n" + "N:" + lastName + ";" + firstName + ";;;" + "\r\n" + "FN:" + firstName + " " + lastName + "\r\n" + "PHOTO;ENCODING=b;TYPE=JPEG;X-ABCROP-RECTANGLE=ABClipRect_1&11&11&25&25&ZNtYcAgH/lm2pubKd1ul0g==:" + "\r\n" + " /9j/4AAQSkZJRgABAQAAAQABAAD/4QBARXhpZgAATU0AKgAAAAgAAYdpAAQAAAABAAAAGgAAAA" + "\r\n" + " AAAqACAAQAAAABAAAAMKADAAQAAAABAAAAMAAAAAD/2wBDAAIBAQIBAQICAQICAgICAwUDAwMD" + "\r\n" + " AwYEBAMFBwYHBwcGBgYHCAsJBwgKCAYGCQ0JCgsLDAwMBwkNDg0MDgsMDAv/2wBDAQICAgMCAw" + "\r\n" + " UDAwULCAYICwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsL" + "\r\n" + " Cwv/wAARCAAwADADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8" + "\r\n" + " QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2Jy" + "\r\n" + " ggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhI" + "\r\n" + " WGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl" + "\r\n" + " 5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAg" + "\r\n" + " ECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl" + "\r\n" + " 8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiI" + "\r\n" + " mKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq" + "\r\n" + " 8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD997+/g0qxnutUnitra2jaWaaVwkcSKMszMeAAASSeAB" + "\r\n" + " Xzb8Q/2p/Evxu8KX3/AAyVEttoRXyP+ErvISY7t2YKP7Njz++GMnzm+UZ4BIr5r/4Ltf8ABRjw" + "\r\n" + " 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" + "\r\n" + " O7fWb6u+7V+rbb9D//2Q==" + "\r\n" + "REV:2012-05-24T09:51:40Z" + "\r\n" + "UID:" + uid + "\r\n" + "END:VCARD";
        assertEquals(StatusCodes.SC_CREATED, super.putVCard(uid, vCard), "response code wrong");
        /*
         * verify contact on server
         */
        Contact contact = super.getContact(uid);
        super.rememberForCleanUp(contact);
        assertEquals(uid, contact.getUid(), "uid wrong");
        assertEquals(firstName, contact.getGivenName(), "firstname wrong");
        assertEquals(lastName, contact.getSurName(), "lastname wrong");
        assertEquals(1, contact.getNumberOfImages(), "wrong numer of images");
        assertNotNull(contact.getImage1(), "no image found in contact");
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(contact.getImage1()));
        assertEquals(25, image.getWidth(), "image width wrong");
        assertEquals(25, image.getHeight(), "image height wrong");
        /*
         * verify contact on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken);
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<VCardResource> addressData = super.addressbookMultiget(eTags.keySet());
        VCardResource card = assertContains(uid, addressData);
        assertEquals(firstName, card.getGivenName(), "N wrong");
        assertEquals(lastName, card.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, card.getFN(), "FN wrong");
        assertTrue(0 < card.getVCard().getPhotos().size(), "PHOTO wrong");
        byte[] vCardPhoto = card.getVCard().getPhotos().get(0).getPhoto();
        assertNotNull(vCardPhoto, "POHTO wrong");
        BufferedImage vCardImage = ImageIO.read(new ByteArrayInputStream(vCardPhoto));
        assertEquals(25, vCardImage.getWidth(), "POHTO width wrong");
        assertEquals(25, vCardImage.getHeight(), "POHTO height wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testNegativeCropOffset(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create contact
         */
        String uid = randomUID();
        String firstName = "bild";
        String lastName = "wurst";
        String vCard = "BEGIN:VCARD" + "\r\n" + "VERSION:3.0" + "\r\n" + "PRODID:-//Apple Inc.//Address Book 6.1//EN" + "\r\n" + "N:" + lastName + ";" + firstName + ";;;" + "\r\n" + "FN:" + firstName + " " + lastName + "\r\n" + "PHOTO;ENCODING=b;TYPE=JPEG;X-ABCROP-RECTANGLE=ABClipRect_1&-76&-76&200&200&XKZcdOASW3junIR92qq6RA==:" + "\r\n" + " /9j/4AAQSkZJRgABAQAAAQABAAD/4QBARXhpZgAATU0AKgAAAAgAAYdpAAQAAAABAAAAGgAAAA" + "\r\n" + " AAAqACAAQAAAABAAAAMKADAAQAAAABAAAAMAAAAAD/2wBDAAIBAQIBAQICAQICAgICAwUDAwMD" + "\r\n" + " AwYEBAMFBwYHBwcGBgYHCAsJBwgKCAYGCQ0JCgsLDAwMBwkNDg0MDgsMDAv/2wBDAQICAgMCAw" + "\r\n" + " UDAwULCAYICwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsLCwsL" + "\r\n" + " Cwv/wAARCAAwADADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8" + "\r\n" + " QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2Jy" + "\r\n" + " ggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhI" + "\r\n" + " WGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl" + "\r\n" + " 5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAg" + "\r\n" + " ECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl" + "\r\n" + " 8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiI" + "\r\n" + " mKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq" + "\r\n" + " 8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD96/Fvi3TPAfhm+1nxnf2umaVpkLXF1d3MgjigjUZLMx" + "\r\n" + " 6Cvzp1X/gpT8aP+Cj3jTWPDH/BMHTdM8HeBdKmks9S+JHiGEyB2UHelhBg73CEPjaxUYJZMivD" + "\r\n" + " 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" + "\r\n" + " +SD+1d/FJe7y6K93b//Z" + "\r\n" + "REV:2012-05-24T12:32:30Z" + "\r\n" + "UID:" + uid + "\r\n" + "END:VCARD";
        assertEquals(StatusCodes.SC_CREATED, super.putVCard(uid, vCard), "response code wrong");
        /*
         * verify contact on server
         */
        Contact contact = super.getContact(uid);
        super.rememberForCleanUp(contact);
        assertEquals(uid, contact.getUid(), "uid wrong");
        assertEquals(firstName, contact.getGivenName(), "firstname wrong");
        assertEquals(lastName, contact.getSurName(), "lastname wrong");
        assertEquals(1, contact.getNumberOfImages(), "wrong numer of images");
        assertNotNull(contact.getImage1(), "no image found in contact");
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(contact.getImage1()));
        assertEquals(200, image.getWidth(), "image width wrong");
        assertEquals(200, image.getHeight(), "image height wrong");
        /*
         * verify contact on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<VCardResource> addressData = super.addressbookMultiget(eTags.keySet());
        VCardResource card = assertContains(uid, addressData);
        assertEquals(firstName, card.getGivenName(), "N wrong");
        assertEquals(lastName, card.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, card.getFN(), "FN wrong");
        assertTrue(0 < card.getVCard().getPhotos().size(), "PHOTO wrong");
        byte[] vCardPhoto = card.getVCard().getPhotos().get(0).getPhoto();
        assertNotNull(vCardPhoto, "POHTO wrong");
        BufferedImage vCardImage = ImageIO.read(new ByteArrayInputStream(vCardPhoto));
        assertEquals(200, vCardImage.getWidth(), "POHTO width wrong");
        assertEquals(200, vCardImage.getHeight(), "POHTO height wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testAddPhotoOnServer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create contact on server
         */
        String uid = randomUID();
        String firstName = "test";
        String lastName = "kimberly";
        Contact contact = new Contact();
        contact.setSurName(lastName);
        contact.setGivenName(firstName);
        contact.setDisplayName(firstName + " " + lastName);
        contact.setUid(uid);
        super.rememberForCleanUp(super.create(contact));
        /*
         * verify contact on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<VCardResource> addressData = super.addressbookMultiget(eTags.keySet());
        VCardResource card = assertContains(uid, addressData);
        assertEquals(firstName, card.getGivenName(), "N wrong");
        assertEquals(lastName, card.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, card.getFN(), "FN wrong");
        /*
         * update contact on server
         */
        String updatedFirstName = "test2";
        String udpatedLastName = "waldemar2";
        contact.setSurName(udpatedLastName);
        contact.setGivenName(updatedFirstName);
        contact.setDisplayName(updatedFirstName + " " + udpatedLastName);
        contact.setImage1(Photos.PNG_100x100.getBytes());
        contact.setImageContentType("image/png");
        contact = super.update(contact);
        /*
         * verify contact on client
         */
        eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        addressData = super.addressbookMultiget(eTags.keySet());
        card = assertContains(uid, addressData);
        assertEquals(updatedFirstName, card.getGivenName(), "N wrong");
        assertEquals(udpatedLastName, card.getFamilyName(), "N wrong");
        assertEquals(updatedFirstName + " " + udpatedLastName, card.getFN(), "FN wrong");
        assertTrue(0 < card.getVCard().getPhotos().size(), "PHOTO wrong");
        byte[] vCardPhoto = card.getVCard().getPhotos().get(0).getPhoto();
        assertNotNull(vCardPhoto, "POHTO wrong");
        Assertions.assertArrayEquals(contact.getImage1(), vCardPhoto, "image data wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testAddPhotoOnClient(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create contact on server
         */
        String uid = randomUID();
        String firstName = "test";
        String lastName = "jaqueline";
        Contact contact = new Contact();
        contact.setSurName(lastName);
        contact.setGivenName(firstName);
        contact.setDisplayName(firstName + " " + lastName);
        contact.setUid(uid);
        super.rememberForCleanUp(super.create(contact));
        /*
         * verify contact on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<VCardResource> addressData = super.addressbookMultiget(eTags.keySet());
        VCardResource card = assertContains(uid, addressData);
        assertEquals(firstName, card.getGivenName(), "N wrong");
        assertEquals(lastName, card.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, card.getFN(), "FN wrong");
        /*
         * update contact on client
         */
        PhotoType photo = new PhotoType();
        photo.setImageMediaType(ImageMediaType.PNG);
        photo.setEncodingType(EncodingType.BINARY);
        photo.setPhoto(Photos.PNG_100x100.getBytes());
        card.getVCard().addPhoto(photo);
        assertEquals(StatusCodes.SC_CREATED, super.putVCardUpdate(card.getUID(), card.toString(), "\"" + card.getETag() + "\""), "response code wrong");
        /*
         * verify updated contact on server
         */
        Contact updatedContact = super.getContact(uid);
        super.rememberForCleanUp(updatedContact);
        assertEquals(1, updatedContact.getNumberOfImages(), "wrong numer of images");
        assertNotNull(updatedContact.getImage1(), "no image found in contact");
        /*
         * verify updated contact on client
         */
        eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        addressData = super.addressbookMultiget(eTags.keySet());
        card = assertContains(uid, addressData);
        assertTrue(0 < card.getVCard().getPhotos().size(), "PHOTO wrong");
        byte[] vCardPhoto = card.getVCard().getPhotos().get(0).getPhoto();
        assertNotNull(vCardPhoto, "POHTO wrong");
        Assertions.assertArrayEquals(updatedContact.getImage1(), vCardPhoto, "image data wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRemovePhotoOnServer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create contact on server
         */
        String uid = randomUID();
        String firstName = "test";
        String lastName = "kimberly";
        Contact contact = new Contact();
        contact.setSurName(lastName);
        contact.setGivenName(firstName);
        contact.setDisplayName(firstName + " " + lastName);
        contact.setImage1(Photos.PNG_100x100.getBytes());
        contact.setImageContentType("image/png");
        contact.setUid(uid);
        super.rememberForCleanUp(super.create(contact));
        /*
         * verify contact on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<VCardResource> addressData = super.addressbookMultiget(eTags.keySet());
        VCardResource card = assertContains(uid, addressData);
        assertEquals(firstName, card.getGivenName(), "N wrong");
        assertEquals(lastName, card.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, card.getFN(), "FN wrong");
        assertTrue(0 < card.getVCard().getPhotos().size(), "PHOTO wrong");
        byte[] vCardPhoto = card.getVCard().getPhotos().get(0).getPhoto();
        assertNotNull(vCardPhoto, "POHTO wrong");
        Assertions.assertArrayEquals(contact.getImage1(), vCardPhoto, "image data wrong");
        /*
         * update contact on server
         */
        contact.setImage1(null);
        contact.setNumberOfImages(0);
        contact = super.update(contact);
        /*
         * verify contact on client
         */
        eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        addressData = super.addressbookMultiget(eTags.keySet());
        card = assertContains(uid, addressData);
        assertTrue(null == card.getVCard().getPhotos() || 0 == card.getVCard().getPhotos().size(), "PHOTO wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRemovePhotoOnClient(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create contact on server
         */
        String uid = randomUID();
        String firstName = "test";
        String lastName = "kimberly";
        Contact contact = new Contact();
        contact.setSurName(lastName);
        contact.setGivenName(firstName);
        contact.setDisplayName(firstName + " " + lastName);
        contact.setImage1(Photos.PNG_100x100.getBytes());
        contact.setImageContentType("image/png");
        contact.setUid(uid);
        super.rememberForCleanUp(super.create(contact));
        /*
         * verify contact on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<VCardResource> addressData = super.addressbookMultiget(eTags.keySet());
        VCardResource card = assertContains(uid, addressData);
        assertEquals(firstName, card.getGivenName(), "N wrong");
        assertEquals(lastName, card.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, card.getFN(), "FN wrong");
        assertTrue(0 < card.getVCard().getPhotos().size(), "PHOTO wrong");
        byte[] vCardPhoto = card.getVCard().getPhotos().get(0).getPhoto();
        assertNotNull(vCardPhoto, "POHTO wrong");
        Assertions.assertArrayEquals(contact.getImage1(), vCardPhoto, "image data wrong");
        /*
         * update contact on client
         */
        card.getVCard().removePhoto(card.getVCard().getPhotos().get(0));
        assertEquals(StatusCodes.SC_CREATED, super.putVCardUpdate(card.getUID(), card.toString(), "\"" + card.getETag() + "\""), "response code wrong");
        /*
         * verify updated contact on server
         */
        Contact updatedContact = super.getContact(uid);
        super.rememberForCleanUp(updatedContact);
        assertEquals(0, updatedContact.getNumberOfImages(), "wrong numer of images");
        assertNull(updatedContact.getImage1(), "image found in contact");
        /*
         * verify updated contact on client
         */
        eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        addressData = super.addressbookMultiget(eTags.keySet());
        card = assertContains(uid, addressData);
        assertTrue(null == card.getVCard().getPhotos() || 0 == card.getVCard().getPhotos().size(), "PHOTO wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testScalingImagesOnServer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create contact on server
         */
        String uid = randomUID();
        String firstName = "chantalle";
        String lastName = "dick";
        Contact contact = new Contact();
        contact.setSurName(lastName);
        contact.setGivenName(firstName);
        contact.setDisplayName(firstName + " " + lastName);
        contact.setImage1(Photos.JPG_1000x750.getBytes());
        contact.setImageContentType("image/jpeg");
        contact.setUid(uid);
        super.rememberForCleanUp(super.create(contact));
        /*
         * verify contact on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<VCardResource> addressData = super.addressbookMultiget(eTags.keySet());
        VCardResource card = assertContains(uid, addressData);
        assertEquals(firstName, card.getGivenName(), "N wrong");
        assertEquals(lastName, card.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, card.getFN(), "FN wrong");
        assertTrue(0 < card.getVCard().getPhotos().size(), "PHOTO wrong");
        byte[] vCardPhoto = card.getVCard().getPhotos().get(0).getPhoto();
        assertNotNull(vCardPhoto, "PHOTO wrong");
        try (ByteArrayInputStream imgStream = new ByteArrayInputStream(vCardPhoto)) {
            BufferedImage vcardImage = ImageIO.read(imgStream);
            MatcherAssert.assertThat("Image not scaled", I(1000), greaterThan(I(vcardImage.getWidth())));
            MatcherAssert.assertThat("Image not scaled", I(750), greaterThan(I(vcardImage.getHeight())));
        }

    }

}
