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

package com.openexchange.mail.exportpdf.converters;

import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.mail.exportpdf.InternalConverterProperties;
import com.openexchange.mail.exportpdf.MailExportConverterOptions;
import com.openexchange.mail.exportpdf.MailExportExceptionCode;
import com.openexchange.mail.exportpdf.MailExportMailPartContainer;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult.Status;
import com.openexchange.mail.exportpdf.converter.WriteThroughMailExportConversionResult;

/**
 * {@link ImageMailExportConverter} - Write-through image converter
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class ImageMailExportConverter extends AbstractWriteThroughMailExportConverter {

    private static final String JPEG = "JPEG";
    private static final String PNG = "PNG";
    private static final String GIF = "GIF";

    private static final byte[] JPEG_1_MAGIC_BYTES = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xEE };
    private static final byte[] JPEG_2_MAGIC_BYTES = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0 };
    private static final byte[] PNG_MAGIC_BYTES = { (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A };
    private static final byte[] GIF_87_MAGIC_BYTES = { (byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x37, (byte) 0x61 };
    private static final byte[] GIF_89_MAGIC_BYTES = { (byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x39, (byte) 0x61 };

    /**
     * Initialises a new {@link ImageMailExportConverter}.
     */
    public ImageMailExportConverter(LeanConfigurationService leanConfigService) {
        super(leanConfigService, InternalConverterProperties.IMAGE_FILE_EXTENSIONS);
    }

    @Override
    public MailExportConversionResult convert(MailExportMailPartContainer mailPart, MailExportConverterOptions options) throws OXException {
        try {
            if (isCorrupt(mailPart.getInputStream())) {
                return new WriteThroughMailExportConversionResult(Status.ATTACHMENT_CORRUPT);
            }
        } catch (IOException e) {
            throw MailExportExceptionCode.IO_ERROR.create(e);
        }
        return super.convert(mailPart, options);
    }

    /**
     * Checks for truncated JPEG using the JPEG EOF marker,
     * GIF using an index out-of-bounds exception check,
     * and PNG using an {@link EOFException}.
     *
     * @param stream The stream to check
     * @return <code>true</code> if the image is corrupt, <code>false</code> otherwise
     * @throws IOException if an I/O error is occurred
     */
    private boolean isCorrupt(InputStream stream) throws IOException {
        try {
            ImageInputStream imageInputStream = ImageIO.createImageInputStream(stream);
            Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);
            if (!imageReaders.hasNext()) {
                return true;
            }
            ImageReader imageReader = imageReaders.next();
            imageReader.setInput(imageInputStream);
            BufferedImage image = imageReader.read(0);
            if (image == null) {
                return true;
            }
            image.flush();
            if (!imageReader.getFormatName().equals(JPEG)) {
                return false;
            }
            imageInputStream.seek(imageInputStream.getStreamPosition() - 2);
            byte[] lastTwoBytes = new byte[2];
            imageInputStream.read(lastTwoBytes);
            return lastTwoBytes[0] != (byte) 0xff || lastTwoBytes[1] != (byte) 0xd9;
        } catch (IndexOutOfBoundsException e) {
            return true; // Corrupted GIF
        } catch (IIOException e) {
            if (e.getCause() instanceof EOFException) {
                return true; // Corrupted PNG
            }
            throw e;
        }
    }

}
