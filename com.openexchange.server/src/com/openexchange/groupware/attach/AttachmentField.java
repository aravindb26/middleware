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

package com.openexchange.groupware.attach;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AttachmentField {
	public static final int CREATED_BY = 2;
	public static final int CREATION_DATE = 4;
	public static final int FILE_MIMETYPE = 805;
	public static final int FILE_SIZE = 804;
	public static final int FILENAME = 803;
	public static final int ATTACHED_ID = 801;
	public static final int MODULE_ID = 802;
	public static final int RTF_FLAG = 806;
	public static final int ID = 1;
	public static final int FOLDER_ID = 800;
	public static final int COMMENT = 807;
	public static final int FILE_ID = 880;
	public static final int CHECKSUM = 890;
	public static final int URI = 891;

	public static final AttachmentField CREATED_BY_LITERAL = new AttachmentField(CREATED_BY,"created_by");
	public static final AttachmentField CREATION_DATE_LITERAL = new AttachmentField(CREATION_DATE, "creation_date");
	public static final AttachmentField FILE_MIMETYPE_LITERAL = new AttachmentField(FILE_MIMETYPE,"file_mimetype");
	public static final AttachmentField FILE_SIZE_LITERAL = new AttachmentField(FILE_SIZE,"file_size");
	public static final AttachmentField FILENAME_LITERAL = new AttachmentField(FILENAME,"filename");
	public static final AttachmentField ATTACHED_ID_LITERAL = new AttachmentField(ATTACHED_ID,"attached");
	public static final AttachmentField MODULE_ID_LITERAL = new AttachmentField(MODULE_ID,"module");
	public static final AttachmentField RTF_FLAG_LITERAL = new AttachmentField(RTF_FLAG,"rtf_flag");
	public static final AttachmentField ID_LITERAL = new AttachmentField(ID,"id");
	public static final AttachmentField FOLDER_ID_LITERAL = new AttachmentField(FOLDER_ID,"folder");
	public static final AttachmentField COMMENT_LITERAL = new AttachmentField(COMMENT,"comment");
	public static final AttachmentField FILE_ID_LITERAL = new AttachmentField(FILE_ID,"file_id");
	public static final AttachmentField CHECKSUM_LITERAL = new AttachmentField(CHECKSUM, "checksum");
	public static final AttachmentField URI_LITERAL = new AttachmentField(URI, "uri");

	public static final AttachmentField[] VALUES_ARRAY = new AttachmentField[]{
		CREATED_BY_LITERAL,
		CREATION_DATE_LITERAL,
		FILE_MIMETYPE_LITERAL,
		FILE_SIZE_LITERAL,
		FILENAME_LITERAL,
		ATTACHED_ID_LITERAL,
		MODULE_ID_LITERAL,
		RTF_FLAG_LITERAL,
		ID_LITERAL,
		FOLDER_ID_LITERAL,
		COMMENT_LITERAL,
		FILE_ID_LITERAL,
		CHECKSUM_LITERAL, 
		URI_LITERAL
	};

	private static final AttachmentField[] HTTPAPI_VALUES_ARRAY = new AttachmentField[]{
		CREATED_BY_LITERAL,
		CREATION_DATE_LITERAL,
		FILE_MIMETYPE_LITERAL,
		FILE_SIZE_LITERAL,
		FILENAME_LITERAL,
		ATTACHED_ID_LITERAL,
		MODULE_ID_LITERAL,
		ID_LITERAL,
		COMMENT_LITERAL,
		RTF_FLAG_LITERAL
	};

	public static final List<AttachmentField> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));
	public static final List<AttachmentField> HTTPAPI_VALUES = Collections.unmodifiableList(Arrays.asList(HTTPAPI_VALUES_ARRAY));



	public static AttachmentField get(final int i){
		return switch (i) {
			case CREATED_BY -> CREATED_BY_LITERAL;
			case CREATION_DATE -> CREATION_DATE_LITERAL;
			case FILE_MIMETYPE -> FILE_MIMETYPE_LITERAL;
			case FILE_SIZE -> FILE_SIZE_LITERAL;
			case FILENAME -> FILENAME_LITERAL;
			case ATTACHED_ID -> ATTACHED_ID_LITERAL;
			case MODULE_ID -> MODULE_ID_LITERAL;
			case ID -> ID_LITERAL;
			case FOLDER_ID -> FOLDER_ID_LITERAL;
			case RTF_FLAG -> RTF_FLAG_LITERAL;
			case COMMENT -> COMMENT_LITERAL;
			case FILE_ID -> FILE_ID_LITERAL;
			case CHECKSUM -> CHECKSUM_LITERAL;
			case URI ->  URI_LITERAL;
			default -> null;
		};
	}

	public static AttachmentField get(final String s) {
		for(final AttachmentField field : VALUES) {
			if (field.name.equals(s)) {
				return field;
			}
		}
		return null;
	}

	public Object doSwitch(final AttachmentSwitch sw){
		return switch (id) {
			case CREATED_BY -> sw.createdBy();
			case CREATION_DATE -> sw.creationDate();
			case FILE_MIMETYPE -> sw.fileMIMEType();
			case FILE_SIZE -> sw.fileSize();
			case FILENAME -> sw.fileName();
			case ATTACHED_ID -> sw.attachedId();
			case MODULE_ID -> sw.moduleId();
			case ID -> sw.id();
			case FOLDER_ID -> sw.folderId();
			case RTF_FLAG -> sw.rtfFlag();
			case COMMENT -> sw.comment();
			case FILE_ID -> sw.fileId();
			case CHECKSUM -> sw.checksum();
			case URI ->  sw.uri();
			default -> null;
		};
	}


	private final int id;
	private String name = "";


	private AttachmentField(final int id, final String name){
		this.id=id;
		this.name=name;
	}

	public String getName(){
		return name;
	}

	public int getId(){
		return id;
	}

	@Override
	public String toString(){
		return name;
	}

	public interface AttachmentSwitch {

		Object createdBy();

		Object fileId();

		Object comment();

		Object rtfFlag();

		Object fileName();

		Object folderId();

		Object id();

		Object moduleId();

		Object attachedId();

		Object fileSize();

		Object fileMIMEType();

		Object creationDate();

		Object checksum();

		Object uri();

	}

}
