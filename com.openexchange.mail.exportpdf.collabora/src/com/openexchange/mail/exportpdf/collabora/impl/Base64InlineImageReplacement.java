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

package com.openexchange.mail.exportpdf.collabora.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.MatchResult;
import org.apache.commons.codec.binary.Base64InputStream;
import com.openexchange.java.Charsets;
import com.openexchange.mail.exportpdf.converter.ReplacingInputStream.Replacement;

/**
 * {@link Base64InlineImageReplacement}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
class Base64InlineImageReplacement implements Replacement {

    private final Collection<InlineImage> inlineImages;

    /**
     * Initializes a new {@link Base64InlineImageReplacement}.
     *
     * @param inlineImages A collection of inline images
     */
    public Base64InlineImageReplacement(Collection<InlineImage> inlineImages) {
        this.inlineImages = inlineImages;
    }

    /**
     * Internal method to get the inline image by it's CID
     *
     * @param cid The CID to get the {@link InlineImage} for
     * @return The {@link InlineImage} with the given CID, or null if no image with the given CID was present
     */
    private InlineImage getInlineImageByCID(String cid) {
        Optional<InlineImage> inlineImage = inlineImages.stream().filter(image -> image.getCid().equalsIgnoreCase(cid)).findFirst();
        return inlineImage.orElse(null);
    }

    @Override
    public InputStream getReplacementData(int matchCount, MatchResult match) {
        if (match.groupCount() < 1) {
            return null;
        }

        final var image = getInlineImageByCID(match.group(1));
        if (image == null) {
            return null;
        }

        //@formatter:off
        List<InputStream> streams = Arrays.asList(
            new ByteArrayInputStream("src=\"data:%s;base64,".formatted(image.getData()).getBytes(Charsets.UTF_8)),
            new Base64InputStream(image.getData(), true),
            new ByteArrayInputStream("\"".getBytes(Charsets.UTF_8))
        );
        //@formatter:on
        return new SequenceInputStream(Collections.enumeration(streams));
    }
}
