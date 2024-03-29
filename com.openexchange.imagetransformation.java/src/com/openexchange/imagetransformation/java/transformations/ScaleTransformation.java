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

package com.openexchange.imagetransformation.java.transformations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import com.openexchange.imagetransformation.ImageInformation;
import com.openexchange.imagetransformation.ImageTransformations;
import com.openexchange.imagetransformation.ScaleType;
import com.openexchange.imagetransformation.TransformationContext;
import com.openexchange.imagetransformation.java.impl.AutoDimensionConstrain;
import com.openexchange.imagetransformation.java.impl.ContainDimensionConstrain;
import com.openexchange.imagetransformation.java.impl.CoverDimensionConstrain;
import com.openexchange.imagetransformation.java.impl.DimensionConstrain;

/**
 * {@link ScaleTransformation}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class ScaleTransformation implements ImageTransformation {

    private final int maxWidth, maxHeight;
    private final ScaleType scaleType;
    private final boolean shrinkOnly;

    /**
     * Initializes a new {@link ScaleTransformation}.
     *
     * @param maxWidth The maximum width of the target image
     * @param maxHeight The maximum height of the target image
     * @param scaleType The scale type to use
     * @param shrinkOnly <code>true</code> to only scale images 'greater than' target size, <code>false</code>, otherwise
     */
    public ScaleTransformation(int maxWidth, int maxHeight, ScaleType scaleType, boolean shrinkOnly) {
        super();
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.scaleType = scaleType;
        this.shrinkOnly = shrinkOnly;
    }

    @Override
    public BufferedImage perform(BufferedImage sourceImage, TransformationContext transformationContext, ImageInformation imageInformation) throws IOException {
        DimensionConstrain constrain;
        boolean shrinkOnly = this.shrinkOnly;
        switch (scaleType) {
            case COVER_AND_CROP:
                shrinkOnly=true;
                constrain = new CoverDimensionConstrain(maxWidth, maxHeight);
                break;
            case COVER:
                constrain = new CoverDimensionConstrain(maxWidth, maxHeight);
                break;
            case CONTAIN_FORCE_DIMENSION:
                // fall-through
            case CONTAIN:
                constrain = new ContainDimensionConstrain(maxWidth, maxHeight);
                break;
            default:
                constrain = new AutoDimensionConstrain(maxWidth, maxHeight);
                break;
        }
        transformationContext.addExpense(ImageTransformations.HIGH_EXPENSE);
        Dimension dimension = constrain.getDimension(new Dimension(sourceImage.getWidth(), sourceImage.getHeight()));
        int targetWidth = (int) dimension.getWidth();
        int targetHeight = (int) dimension.getHeight();
        if (shrinkOnly && maxWidth >= sourceImage.getWidth() && maxHeight >= sourceImage.getHeight()) {
            if (ScaleType.COVER_AND_CROP ==  scaleType){
                return extentImageIfNeeded(sourceImage, maxWidth, maxHeight, transformationContext);
            }
            return sourceImage; // nothing to do
        }

        BufferedImage resized = Scalr.resize(sourceImage, Method.AUTOMATIC, targetWidth, targetHeight);
        if (ScaleType.COVER_AND_CROP ==  scaleType && (resized.getWidth()>maxWidth || resized.getHeight() > maxHeight)){
            if (resized.getWidth()>maxWidth){
                int x = (int) Math.floor((resized.getWidth()-maxWidth) / 2d);
                resized = Scalr.crop(resized, x, 0, maxWidth, maxHeight);
            } else {
                int y = (int) Math.floor((resized.getHeight()-maxHeight) / 2d);
                resized = Scalr.crop(resized, 0, y, maxWidth, maxHeight);
            }
        }

        if (ScaleType.CONTAIN_FORCE_DIMENSION == scaleType || ScaleType.COVER_AND_CROP ==  scaleType) {
            resized = extentImageIfNeeded(resized, maxWidth, maxHeight, transformationContext);
        }
        transformationContext.setTransformed();
        return resized;
    }

    /**
     * Resizes an image to a specific size and adds white lines in respect to the ratio.
     * <p>
     * See <a href="http://www.programcreek.com/java-api-examples/index.php?source_dir=proudcase-master/src/java/com/proudcase/util/ImageScale.java">this code example</a> from which this routine was derived
     *
     * @param resizedImage The previously resized image using {@link ScaleType#CONTAIN CONTAIN} policy
     * @param resultWidth The desired width
     * @param resultHeight The desired height
     * @param transformationContext
     * @return The resized image with smaller sides padded
     */
    private BufferedImage extentImageIfNeeded(BufferedImage resizedImage, int resultWidth, int resultHeight, TransformationContext transformationContext) {
        // First, get the width and the height of the image
        BufferedImage paddedImage = resizedImage;
        int originWidth = paddedImage.getWidth();
        int originHeight = paddedImage.getHeight();

        // Check which sides need padding
        if (originWidth < resultWidth) {
            // Padding on the width axis
            int paddingSize = (resultWidth - originWidth) / 2;
            if (paddingSize > 0) {
                paddedImage = extentImage(paddedImage, paddingSize, true);
                transformationContext.setTransformed();
            }
        }
        if (originHeight < resultHeight) {
            // Padding on the height axis
            int paddingSize = (resultHeight - originHeight) / 2;
            if (paddingSize > 0) {
                paddedImage = extentImage(paddedImage, paddingSize, false);
                transformationContext.setTransformed();
            }
        }

        return paddedImage;
    }

    private BufferedImage extentImage(BufferedImage resizedImage, int paddingSize, boolean extentWidth) {

        // Add the padding to the image
        BufferedImage outputImage = Scalr.pad(resizedImage, paddingSize, Color.white);

        // Crop the image since padding was added to all sides
        int x = 0, y = 0, width = 0, height = 0;
        if (extentWidth) {
            x = 0;
            y = paddingSize;
            width = outputImage.getWidth();
            height = outputImage.getHeight() - (2 * paddingSize);
        } else {
            x = paddingSize;
            y = 0;
            width = outputImage.getWidth() - (2 * paddingSize);
            height = outputImage.getHeight();
        }

        if (width > 0 && height > 0) {
            outputImage = Scalr.crop(outputImage, x, y, width, height);
        }

        // Flush both images
        resizedImage.flush();
        outputImage.flush();

        // Return the final image
        return outputImage;
    }

    @Override
    public boolean needsImageInformation() {
        return false;
    }

    @Override
    public boolean supports(String formatName) {
        return true;
    }

    @Override
    public Dimension getRequiredResolution(Dimension originalResolution) {
        DimensionConstrain constrain;
        switch (scaleType) {
            case COVER_AND_CROP:
                // fall-through
            case COVER:
                constrain = new CoverDimensionConstrain(maxWidth, maxHeight);
                break;
            case CONTAIN_FORCE_DIMENSION:
                // fall-through
            case CONTAIN:
                if (maxWidth >= originalResolution.getWidth() && maxHeight >= originalResolution.getHeight()) {
                    return originalResolution; // nothing to do
                }
                constrain = new ContainDimensionConstrain(maxWidth, maxHeight);
                break;
            default:
                constrain = new AutoDimensionConstrain(maxWidth, maxHeight);
                break;
        }
        return constrain.getDimension(originalResolution);
    }

}