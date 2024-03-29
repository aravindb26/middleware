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

package com.openexchange.webdav.action;

import static com.openexchange.tools.io.IOTools.reallyBloodySkip;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.webdav.protocol.WebdavPath;
import com.openexchange.webdav.protocol.WebdavProtocolException;
import com.openexchange.webdav.protocol.WebdavResource;

public class WebdavGetAction extends WebdavHeadAction {
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WebdavGetAction.class);
	private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=(\\S+)");

	@Override
	public void perform(final WebdavRequest req, final WebdavResponse res) throws WebdavProtocolException {
		final WebdavResource resource = req.getResource();
		res.setContentType(resource.getContentType());
		res.setHeader("Content-Disposition", "attachment");
		if (!resource.exists()) {
			throw WebdavProtocolException.Code.GENERAL_ERROR.create(req.getUrl(), HttpServletResponse.SC_NOT_FOUND);
		}
		final List<ByteRange> ranges = getRanges(req, res);

		long size = 0;
		long offset = 0;
		for(final ByteRange range : ranges) {
			offset = (range.startOffset < offset) ? offset : range.startOffset;
			if (offset > range.endOffset) {
				continue;
			}
			size += range.endOffset - offset;
			size++;
		}
		head(res,resource,size);

		if (resource.supportsRange()) {
		    BufferedOutputStream out = null;
		    try {
		        out = new BufferedOutputStream(res.getOutputStream());
		        byte[] chunk = new byte[65536];
		        for (ByteRange range : ranges) {
                    InputStream in = resource.getBody(range.startOffset, range.endOffset - range.startOffset + 1);
		            try {
                        for (int read; (read = in.read(chunk, 0, chunk.length)) > 0;) {
                            out.write(chunk, 0, read);
                        }
                    } finally {
                        Streams.close(in);
                    }
		        }
            } catch (IOException e) {
                throw WebdavProtocolException.Code.GENERAL_ERROR.create(req.getUrl(), HttpServletResponse.SC_SERVICE_UNAVAILABLE, e);
            } finally {
                Streams.flush(out);
            }
		    return;
        }

		BufferedOutputStream out = null;
		InputStream in = null;
		try {
		    in = resource.getBody();
		    if (in == null) {
		        LOG.error("The body of the webdav ressource at {} is null!", resource.getUrl());
		        throw WebdavProtocolException.Code.GENERAL_ERROR.create(req.getUrl(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		    }

			out = new BufferedOutputStream(res.getOutputStream());
			byte[] chunk = new byte[65536];
			offset = 0;
			for (ByteRange range : ranges) {
				if (offset < range.startOffset) {
					reallyBloodySkip(in, range.startOffset-offset);
					offset = (int) range.startOffset;
				}
				if (offset > range.endOffset) {
					continue;
				}

				int need = (int) Math.min(chunk.length, range.endOffset - offset + 1);
				for (int read; need > 0 && (read = in.read(chunk, 0, need)) > 0;) {
					out.write(chunk,0,read);
					offset += read;
					need = (int) Math.min(chunk.length, range.endOffset - offset + 1);
				}
			}
		} catch (IOException e) {
			throw WebdavProtocolException.Code.GENERAL_ERROR.create(req.getUrl(), HttpServletResponse.SC_SERVICE_UNAVAILABLE, e);
		} finally {
		    Streams.flush(out);
			Streams.close(in);
		}
	}

	private static List<ByteRange> getRanges(final WebdavRequest req, final WebdavResponse res) throws WebdavProtocolException {
	    WebdavResource resource = req.getResource();
	    if (null == resource || resource.isCollection()) {
	        return Collections.emptyList();
	    }

        Long resourceLength = resource.getLength();
        if (null == resourceLength || 0 >= resourceLength.longValue()) {
            return Collections.emptyList();
        }

		long length = resourceLength.longValue();
		List<ByteRange> retVal = null;

		String byteRanges = req.getHeader("Bytes");
        if (byteRanges != null) {
            for (String range : Strings.splitByComma(byteRanges)) {
                range = range.trim();
                if (retVal == null) {
                    retVal = new ArrayList<ByteRange>();
                }
                retVal.add(parseRange(range, length, req.getUrl()));
            }
        }

		String range = req.getHeader("Range");
		if (range != null) {
			Matcher m = RANGE_PATTERN.matcher(range);
			while (m.find()){
				String br = m.group(1);
				for(final String r : Strings.splitByComma(br)) {
					range = range.trim();
					if (retVal == null) {
	                    retVal = new ArrayList<ByteRange>();
	                }
					retVal.add(parseRange(r, length,req.getUrl()));
				}
			}
		}

		if (retVal == null) {
			res.setStatus(HttpServletResponse.SC_OK);
            return Collections.singletonList(new ByteRange(0, length - 1));
		}

		res.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
		Collections.sort(retVal);
		return retVal;
	}

	private static ByteRange parseRange(final String range, final long length, final WebdavPath url) throws WebdavProtocolException {
		if (range.charAt(0) == '-') {
			final long reqLength = Long.parseLong(range.substring(1));
			if (reqLength > length) {
				return new ByteRange(0, length-1);
			}
			return new ByteRange(length-reqLength,length-1);
		} else if (range.charAt(range.length()-1) == '-') {
			final long startOffset = Long.parseLong(range.substring(0, range.length()-1));
			return new ByteRange(startOffset, length-1);
		} else {
			final String[] startAndEnd = Strings.splitBy(range, '-', true);
			final long startOffset = Long.parseLong(startAndEnd[0]);
			final long endOffset = Long.parseLong(startAndEnd[1]);
			/*if (startOffset>endOffset) {
				return new ByteRange(0,0);
			}*/
			if (startOffset>length) {
				throw WebdavProtocolException.Code.GENERAL_ERROR.create(url, HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			}
            return new ByteRange(startOffset, Math.min(endOffset, length - 1));
		}
	}

    private static final class ByteRange implements Comparable<ByteRange> {

        public final long startOffset;

        public final long endOffset;

        public ByteRange(final long start, final long end) {
            startOffset = start;
            endOffset = end;
        }

        @Override
		public int compareTo(final ByteRange arg0) {
            final ByteRange other = arg0;
            return Long.compare(startOffset, other.startOffset);
        }

        @Override
        public String toString() {
            return String.format("%d-%d", Long.valueOf(startOffset), Long.valueOf(endOffset));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (startOffset ^ (startOffset >>> 32));
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ByteRange)) {
                return false;
            }
            final ByteRange other = (ByteRange) obj;
            if (startOffset != other.startOffset) {
                return false;
            }
            return true;
        }

    }

}
