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

package com.openexchange.file.storage.json;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.search.AndTerm;
import com.openexchange.file.storage.search.CameraApertureTerm;
import com.openexchange.file.storage.search.CameraExposureTimeTerm;
import com.openexchange.file.storage.search.CameraFocalLengthTerm;
import com.openexchange.file.storage.search.CameraIsoSpeedTerm;
import com.openexchange.file.storage.search.CameraMakeTerm;
import com.openexchange.file.storage.search.CameraModelTerm;
import com.openexchange.file.storage.search.CaptureDateTerm;
import com.openexchange.file.storage.search.CategoriesTerm;
import com.openexchange.file.storage.search.ColorLabelTerm;
import com.openexchange.file.storage.search.ComparablePattern;
import com.openexchange.file.storage.search.ComparisonType;
import com.openexchange.file.storage.search.ContentTerm;
import com.openexchange.file.storage.search.CreatedByTerm;
import com.openexchange.file.storage.search.CreatedTerm;
import com.openexchange.file.storage.search.CurrentVersionTerm;
import com.openexchange.file.storage.search.DescriptionTerm;
import com.openexchange.file.storage.search.FileMd5SumTerm;
import com.openexchange.file.storage.search.FileMimeTypeTerm;
import com.openexchange.file.storage.search.FileNameTerm;
import com.openexchange.file.storage.search.FileSizeTerm;
import com.openexchange.file.storage.search.HeightTerm;
import com.openexchange.file.storage.search.LastModifiedTerm;
import com.openexchange.file.storage.search.LastModifiedUtcTerm;
import com.openexchange.file.storage.search.LockedUntilTerm;
import com.openexchange.file.storage.search.MediaDateTerm;
import com.openexchange.file.storage.search.MetaTerm;
import com.openexchange.file.storage.search.ModifiedByTerm;
import com.openexchange.file.storage.search.NotTerm;
import com.openexchange.file.storage.search.NumberOfVersionsTerm;
import com.openexchange.file.storage.search.OrTerm;
import com.openexchange.file.storage.search.SearchTerm;
import com.openexchange.file.storage.search.SequenceNumberTerm;
import com.openexchange.file.storage.search.TitleTerm;
import com.openexchange.file.storage.search.UrlTerm;
import com.openexchange.file.storage.search.VersionCommentTerm;
import com.openexchange.file.storage.search.VersionTerm;
import com.openexchange.file.storage.search.WidthTerm;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.Operand;
import com.openexchange.search.SearchExceptionMessages;
import com.openexchange.search.SingleSearchTerm;

/**
 * {@link FileSearchTermParser}
 *
 * @author <a href="mailto:alexander.schulze-ardey@open-xchange.com">Alexander Schulze-Ardey</a>
 * @since v7.10.5
 */
public class FileSearchTermParser {

    private FileSearchTermParser() {}

    /**
     * Parses com.openexchange.search.SearchTerm<?> to com.openexchange.file.storage.search.SearchTerm<?>
     *
     * @param term
     * @return Parsed term
     * @throws OXException
     */
    public static SearchTerm<?> parseSearchTerm(final com.openexchange.search.SearchTerm<?> term) throws OXException {
        if (null == term) {
            return null;
        }

        if ((term instanceof SingleSearchTerm)) {
            return convertTerm((SingleSearchTerm) term);
        } else if ((term instanceof CompositeSearchTerm)) {
            return convertTerm((CompositeSearchTerm) term);
        } else {
            throw SearchExceptionMessages.SEARCH_FAILED.create("Term is neither single or complex: " + term);
        }
    }

    /**
     * Converts a com.openexchange.search.CompositeSearchTerm to com.openexchange.file.storage.search.SearchTerm<T>.
     *
     * @param term
     * @return
     * @throws OXException
     */
    private static SearchTerm<?> convertTerm(CompositeSearchTerm term) throws OXException {
        List<SearchTerm<?>> terms = new ArrayList<SearchTerm<?>>();

        for (com.openexchange.search.SearchTerm<?> operand : term.getOperands()) {
            terms.add(parseSearchTerm(operand));
        }

        CompositeSearchTerm.CompositeOperation operation = term.getOperation();

        switch (operation) {
            case OR:
                return new OrTerm(terms);
            case AND:
                return new AndTerm(terms);
            case NOT:
                if (terms.size() != 1) {
                    throw SearchExceptionMessages.PARSING_FAILED_INVALID_SEARCH_TERM.create();
                }
                return new NotTerm(terms.get(0));
            default:
                throw SearchExceptionMessages.PARSING_FAILED_INVALID_SEARCH_TERM.create();
        }
    }

    /**
     * Converts a com.openexchange.search.SingleSearchTerm to com.openexchange.file.storage.search.SearchTerm<T>.
     *
     * @param term
     * @return
     * @throws OXException
     */
    private static SearchTerm<?> convertTerm(SingleSearchTerm term) throws OXException {
        SingleSearchTerm.SingleOperation operation = term.getOperation();
        Field field = null;
        Object query = null;

        for (Operand<?> operand : term.getOperands()) {
            switch (operand.getType()) {
                case COLUMN:
                    field = Field.get((String) operand.getValue());
                    break;
                case CONSTANT:
                    query = operand.getValue();
                    break;
                default:
                    throw SearchExceptionMessages.PARSING_FAILED_INVALID_SEARCH_TERM.create();
            }
        }

        if (field == null) {
            throw SearchExceptionMessages.PARSING_FAILED_INVALID_SEARCH_TERM.create();
        }

        final ComparisonType comparison = resolveComparison(operation);

        switch (field) {
            case CAMERA_APERTURE:
                return new CameraApertureTerm(getComparableNumberPattern(File.Field.CAMERA_APERTURE, query, comparison));
            case CAMERA_EXPOSURE_TIME:
                return new CameraExposureTimeTerm(getComparableNumberPattern(File.Field.CAMERA_EXPOSURE_TIME, query, comparison));
            case CAMERA_FOCAL_LENGTH:
                return new CameraFocalLengthTerm(getComparableNumberPattern(File.Field.CAMERA_FOCAL_LENGTH, query, comparison));
            case CAMERA_ISO_SPEED:
                return new CameraIsoSpeedTerm(getComparableNumberPattern(File.Field.CAMERA_ISO_SPEED, query, comparison));
            case CAMERA_MAKE:
                ensureComparisonType(File.Field.CAMERA_MAKE, comparison, ComparisonType.EQUALS);
                return new CameraMakeTerm(String.valueOf(query));
            case CAMERA_MODEL:
                ensureComparisonType(File.Field.CAMERA_MODEL, comparison, ComparisonType.EQUALS);
                return new CameraModelTerm(String.valueOf(query));
            case CAPTURE_DATE:
                return new CaptureDateTerm(getComparableDatePattern(File.Field.CAPTURE_DATE, query, comparison));
            case CATEGORIES:
                ensureComparisonType(File.Field.CATEGORIES, comparison, ComparisonType.EQUALS);
                return new CategoriesTerm(String.valueOf(query));
            case COLOR_LABEL:
                return new ColorLabelTerm(getComparableNumberPattern(File.Field.COLOR_LABEL, query, comparison));
            case CONTENT:
                ensureComparisonType(File.Field.CONTENT, comparison, ComparisonType.EQUALS);
                return new ContentTerm(String.valueOf(query), true, true);
            case CREATED:
                return new CreatedTerm(getComparableDatePattern(File.Field.CREATED, query, comparison));
            case CREATED_BY:
                return new CreatedByTerm(getComparableNumberPattern(File.Field.CREATED_BY, query, comparison));
            case CURRENT_VERSION:
                boolean currentVersion = Boolean.valueOf(String.valueOf(query)).booleanValue();
                return new CurrentVersionTerm(currentVersion);
            case DESCRIPTION:
                ensureComparisonType(File.Field.DESCRIPTION, comparison, ComparisonType.EQUALS);
                return new DescriptionTerm(String.valueOf(query), true, true);
            case FILENAME:
                ensureComparisonType(File.Field.FILENAME, comparison, ComparisonType.EQUALS);
                return new FileNameTerm(String.valueOf(query));
            case FILE_MD5SUM:
                ensureComparisonType(File.Field.FILE_MD5SUM, comparison, ComparisonType.EQUALS);
                return new FileMd5SumTerm(String.valueOf(query));
            case FILE_MIMETYPE:
                ensureComparisonType(File.Field.FILE_MIMETYPE, comparison, ComparisonType.EQUALS);
                return new FileMimeTypeTerm(String.valueOf(query), true, true);
            case FILE_SIZE:
                return new FileSizeTerm(getComparableNumberPattern(File.Field.FILE_SIZE, query, comparison));
            case HEIGHT:
                return new HeightTerm(getComparableNumberPattern(File.Field.HEIGHT, query, comparison));
            case LAST_MODIFIED:
                return new LastModifiedTerm(getComparableDatePattern(File.Field.LAST_MODIFIED, query, comparison));
            case LAST_MODIFIED_UTC:
                return new LastModifiedUtcTerm(getComparableDatePattern(File.Field.LAST_MODIFIED_UTC, query, comparison));
            case LOCKED_UNTIL:
                return new LockedUntilTerm(getComparableDatePattern(File.Field.LOCKED_UNTIL, query, comparison));
            case MEDIA_DATE:
                return new MediaDateTerm(getComparableDatePattern(File.Field.MEDIA_DATE, query, comparison));
            case META:
                ensureComparisonType(File.Field.META, comparison, ComparisonType.EQUALS);
                return new MetaTerm(String.valueOf(query));
            case MODIFIED_BY:
                return new ModifiedByTerm(getComparableNumberPattern(File.Field.MODIFIED_BY, query, comparison));
            case NUMBER_OF_VERSIONS:
                return new NumberOfVersionsTerm(getComparableNumberPattern(File.Field.NUMBER_OF_VERSIONS, query, comparison));
            case SEQUENCE_NUMBER:
                return new SequenceNumberTerm(getComparableNumberPattern(File.Field.SEQUENCE_NUMBER, query, comparison));
            case TITLE:
                ensureComparisonType(File.Field.TITLE, comparison, ComparisonType.EQUALS);
                return new TitleTerm(String.valueOf(query), true, true);
            case URL:
                ensureComparisonType(File.Field.URL, comparison, ComparisonType.EQUALS);
                return new UrlTerm(String.valueOf(query), true, true);
            case VERSION:
                ensureComparisonType(File.Field.VERSION, comparison, ComparisonType.EQUALS);
                return new VersionTerm(String.valueOf(query));
            case VERSION_COMMENT:
                ensureComparisonType(File.Field.VERSION_COMMENT, comparison, ComparisonType.EQUALS);
                return new VersionCommentTerm(String.valueOf(query), true);
            case WIDTH:
                return new WidthTerm(getComparableNumberPattern(File.Field.WIDTH, query, comparison));
            case FOLDER_ID:
            case GEOLOCATION:
            case ID:
            case MEDIA_META:
            case MEDIA_STATUS:
            case OBJECT_PERMISSIONS:
            case ORIGIN:
            case SHAREABLE:
            case UNIQUE_ID:
            default:
                // Unknown term for field.
                throw SearchExceptionMessages.PARSING_FAILED_INVALID_SEARCH_TERM.create();
        }
    }

    /**
     * Resolve com.openexchange.search.SingleSearchTerm.SingleOperation to ComparisonType.
     *
     * @param operation
     * @return
     * @throws OXException
     */
    private static ComparisonType resolveComparison(SingleSearchTerm.SingleOperation operation) throws OXException {
        final ComparisonType comparison;

        SingleSearchTerm.SingleOperation singleOperation = SingleSearchTerm.SingleOperation.getSingleOperation(operation.getOperation());
        if(null == singleOperation) {
            throw SearchExceptionMessages.UNKNOWN_OPERATION.create();
        }
        switch (singleOperation) {
            case EQUALS:
                comparison = ComparisonType.EQUALS;
                break;
            case GREATER_THAN:
                comparison = ComparisonType.GREATER_THAN;
                break;
            case LESS_THAN:
                comparison = ComparisonType.LESS_THAN;
                break;
            case NOT_EQUALS:
            case LESS_OR_EQUAL:
            case ISNULL:
            case GREATER_OR_EQUAL:
            default:
                throw SearchExceptionMessages.UNKNOWN_OPERATION.create(String.valueOf(singleOperation));
        }

        return comparison;
    }

    /**
     * Ensures ComparisonType for term.
     *
     * @param field The {@link Field} to check the {@link ComparisonType} for
     * @param comparison The {@link ComparisonType} to check
     * @param allowed The allowed {@link ComparisonType}
     * @throws OXException
     */
    private static void ensureComparisonType(File.Field field, ComparisonType comparison, ComparisonType allowed) throws OXException {
        if (!comparison.equals(allowed)) {
            throw SearchExceptionMessages.SEARCH_FAILED.create(String.format("Unallowed comparison '%s' for field '%s'. Allowed comparison: '%s'", comparison.name(), field.getName(), allowed.name()));
        }
    }

    /**
     * Get pattern for Number comparisons.
     *
     * @param field The {@link Field} to get the {@link ComparablePattern} for
     * @param query The query
     * @param comparison The {@link ComparisonType}
     * @return The {@link ComparablePattern}
     * @throws OXException in case the query is not a number
     */
    private static ComparablePattern<Number> getComparableNumberPattern(File.Field field, Object query, ComparisonType comparison) throws OXException {
        Number value;
        if ((query instanceof Number)) {
            value = (Number) query;
        } else {
            try {
                value = Long.valueOf(String.valueOf(query));
            } catch (NumberFormatException nfe) {
                throw SearchExceptionMessages.SEARCH_FAILED.create(nfe, String.format("Unable to parse query '%s' for field '%s' and comparison '%s'", query, field.getName(), comparison.name()));
            }
        }
        return new ComparablePattern<Number>() {

            @Override
            public ComparisonType getComparisonType() {
                return comparison;
            }

            @Override
            public Number getPattern() {
                return value;
            }
        };
    }

    /**
     * Get pattern for date comparisons.
     *
     * @param query
     * @param comparison
     * @return
     * @throws OXException
     */
    private static ComparablePattern<Date> getComparableDatePattern(File.Field field, Object query, ComparisonType comparison) throws OXException {
        Number value;
        if ((query instanceof Number)) {
            value = (Number) query;
        } else {
            try {
                value = Long.valueOf(String.valueOf(query));
            } catch (NumberFormatException nfe) {
                throw SearchExceptionMessages.SEARCH_FAILED.create(nfe, String.format("Unable to parse query '%s' for field '%s' and comparison '%s'", query, field.getName(), comparison.name()));
            }
        }
        return new ComparablePattern<Date>() {

            @Override
            public ComparisonType getComparisonType() {
                return comparison;
            }

            @Override
            public Date getPattern() {
                return null == value ? null : new Date(value.longValue());
            }
        };

    }
}
