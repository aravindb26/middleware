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

package com.openexchange.mail.search.service;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.mail.internet.InternetAddress;
import com.google.common.collect.ImmutableMap;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailJSONField;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.search.BccTerm;
import com.openexchange.mail.search.BodyTerm;
import com.openexchange.mail.search.CcTerm;
import com.openexchange.mail.search.ComparisonType;
import com.openexchange.mail.search.FlagTerm;
import com.openexchange.mail.search.FromTerm;
import com.openexchange.mail.search.HeaderTerm;
import com.openexchange.mail.search.ORTerm;
import com.openexchange.mail.search.ReceivedDateTerm;
import com.openexchange.mail.search.SearchTerm;
import com.openexchange.mail.search.SentDateTerm;
import com.openexchange.mail.search.SizeTerm;
import com.openexchange.mail.search.SubjectTerm;
import com.openexchange.mail.search.TextTerm;
import com.openexchange.mail.search.ToTerm;
import com.openexchange.mail.search.UserFlagTerm;
import com.openexchange.search.SearchAttributeFetcher;
import com.openexchange.search.SingleSearchTerm.SingleOperation;

/**
 * {@link MailAttributeFetcher} - The attribute fetcher for mail module.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailAttributeFetcher implements SearchAttributeFetcher<MailMessage> {

    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MailAttributeFetcher.class);

    private static interface AttributeGetter {

        public Object getObject(MailMessage candidate);

        public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant);

    }

    private static final Map<String, AttributeGetter> GETTERS;

    static {
        final Map<String, AttributeGetter> m = new HashMap<String, AttributeGetter>(25);

        m.put(MailJSONField.CID.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                return candidate.getContentId();
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                if (SingleOperation.EQUALS != operation) {
                    throw new IllegalArgumentException("Unsupported operation for header search: " + operation);
                }
                return new HeaderTerm(MessageHeaders.HDR_CONTENT_ID, constant.toString());
            }

        });
        m.put(MailJSONField.CONTENT.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                try {
                    return candidate.getContent();
                } catch (OXException e) {
                    LOG.error("", e);
                    return null;
                }
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                if (SingleOperation.EQUALS != operation) {
                    throw new IllegalArgumentException("Unsupported operation for header search: " + operation);
                }
                return new BodyTerm(constant.toString());
            }

        });
        m.put(MailJSONField.CONTENT_TYPE.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                return candidate.getContentType().toString();
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                if (SingleOperation.EQUALS != operation) {
                    throw new IllegalArgumentException("Unsupported operation for header search: " + operation);
                }
                return new HeaderTerm(MessageHeaders.HDR_CONTENT_TYPE, constant.toString());
            }

        });
        m.put(MailJSONField.DISPOSITION.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                return candidate.getContentDisposition().getDisposition();
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                if (SingleOperation.EQUALS != operation) {
                    throw new IllegalArgumentException("Unsupported operation for header search: " + operation);
                }
                return new HeaderTerm(MessageHeaders.HDR_CONTENT_DISPOSITION, constant.toString());
            }

        });
        m.put(MailJSONField.DISPOSITION_NOTIFICATION_TO.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                final InternetAddress dispositionNotification = candidate.getDispositionNotification();
                return null == dispositionNotification ? null : dispositionNotification.toUnicodeString();
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                if (SingleOperation.EQUALS != operation) {
                    throw new IllegalArgumentException("Unsupported operation for header search: " + operation);
                }
                return new HeaderTerm(MessageHeaders.HDR_DISP_NOT_TO, constant.toString());
            }

        });
        m.put(MailJSONField.FLAGS.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                return Integer.valueOf(candidate.getFlags());
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                if (SingleOperation.EQUALS != operation) {
                    throw new IllegalArgumentException("Unsupported operation for flag search: " + operation);
                }
                try {
                    return new FlagTerm(Integer.parseInt(constant.toString()), true);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Unsupported value for flag search: " + constant.toString());
                }
            }

        });
        m.put("user_flags", new AttributeGetter() {

            private boolean isArray(Object aObject) {
                // Too slow: return aObject.getClass().isArray();
                return (null != aObject && '[' == aObject.getClass().getName().charAt(0));
            }

            @Override
            public Object getObject(MailMessage candidate) {
                return candidate.getUserFlags();
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                if (SingleOperation.EQUALS != operation) {
                    throw new IllegalArgumentException("Unsupported operation for flag search: " + operation);
                }
                if (constant instanceof String) {
                    return new UserFlagTerm(Strings.splitByComma(constant.toString()), true);
                }
                if (constant instanceof Collection) {
                    Collection<String> col = (Collection<String>) constant;
                    return new UserFlagTerm(col.toArray(new String[col.size()]), true);
                }
                if (isArray(constant)) {
                    return new UserFlagTerm((String[]) constant, true);
                }
                throw new IllegalArgumentException("Unsupported constant type: " + constant.getClass().getName());
            }

        });
        m.put(MailJSONField.FROM.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                final InternetAddress[] from = candidate.getFrom();
                return null == from || from.length == 0 ? null : from[0].toUnicodeString();
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                if (SingleOperation.EQUALS != operation) {
                    throw new IllegalArgumentException("Unsupported operation for from search: " + operation);
                }
                return new FromTerm(constant.toString());
            }

        });
        m.put(MailJSONField.SENDER.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                final InternetAddress[] sender = candidate.getSender();
                return null == sender || sender.length == 0 ? null : sender[0].toUnicodeString();
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                if (SingleOperation.EQUALS != operation) {
                    throw new IllegalArgumentException("Unsupported operation for from search: " + operation);
                }
                return new HeaderTerm(MessageHeaders.HDR_SENDER, constant.toString());
            }

        });
        m.put(MailJSONField.REPLY_TO.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                final InternetAddress[] replyTo = candidate.getReplyTo();
                return null == replyTo || replyTo.length == 0 ? null : replyTo[0].toUnicodeString();
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                if (SingleOperation.EQUALS != operation) {
                    throw new IllegalArgumentException("Unsupported operation for from search: " + operation);
                }
                return new HeaderTerm(MessageHeaders.HDR_REPLY_TO, constant.toString());
            }

        });
        m.put(MailJSONField.PRIORITY.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                return Integer.valueOf(candidate.getPriority());
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                if (SingleOperation.EQUALS != operation) {
                    throw new IllegalArgumentException("Unsupported operation for header search: " + operation);
                }
                int parsedPrio;
                final String string = constant.toString();
                try {
                    parsedPrio = Integer.parseInt(string);
                } catch (NumberFormatException e) {
                    parsedPrio = -1;
                }
                final String importance;
                if (parsedPrio >= 0) {
                    if (MailMessage.PRIORITY_NORMAL == parsedPrio) {
                        importance = "Normal";
                    } else if (parsedPrio > MailMessage.PRIORITY_NORMAL) {
                        importance = "Low";
                    } else {
                        importance = "High";
                    }
                } else {
                    importance = "Normal";
                }
                SearchTerm<?> importanceTerm;
                if ("Normal".equals(importance)) {
                    importanceTerm = new ORTerm(new HeaderTerm(MessageHeaders.HDR_IMPORTANCE, importance), new HeaderTerm(MessageHeaders.HDR_IMPORTANCE, "Medium"));
                } else {
                    importanceTerm = new HeaderTerm(MessageHeaders.HDR_IMPORTANCE, importance);
                }
                return new ORTerm(importanceTerm, new HeaderTerm(MessageHeaders.HDR_X_PRIORITY, string));
            }

        });
        m.put(MailJSONField.RECEIVED_DATE.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                final Date d = candidate.getReceivedDate();
                return null == d ? null : Long.valueOf(d.getTime());
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                ComparisonType ct;
                if (SingleOperation.EQUALS == operation) {
                    ct = ComparisonType.EQUALS;
                } else if (SingleOperation.GREATER_THAN == operation) {
                    ct = ComparisonType.GREATER_THAN;
                } else {
                    ct = ComparisonType.LESS_THAN;
                }
                try {
                    return new ReceivedDateTerm(ct, new Date(Long.parseLong(constant.toString())));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Unsupported value for received date search: " + constant.toString());
                }
            }

        });
        m.put(MailJSONField.RECIPIENT_BCC.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                final InternetAddress[] internetAddresses = candidate.getBcc();
                if (null == internetAddresses) {
                    return null;
                }
                final int length = internetAddresses.length;
                if (length == 0) {
                    return null;
                }
                final StringBuilder sb = new StringBuilder(length * 16);
                sb.append(internetAddresses[0].toUnicodeString());
                for (int i = 1; i < length; i++) {
                    sb.append(',').append(internetAddresses[i].toUnicodeString());
                }
                return sb.toString();
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                if (SingleOperation.EQUALS != operation) {
                    throw new IllegalArgumentException("Unsupported operation for BCC search: " + operation);
                }
                return new BccTerm(constant.toString());
            }

        });
        m.put(MailJSONField.RECIPIENT_CC.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                final InternetAddress[] internetAddresses = candidate.getCc();
                if (null == internetAddresses) {
                    return null;
                }
                final int length = internetAddresses.length;
                if (length == 0) {
                    return null;
                }
                final StringBuilder sb = new StringBuilder(length * 16);
                sb.append(internetAddresses[0].toUnicodeString());
                for (int i = 1; i < length; i++) {
                    sb.append(',').append(internetAddresses[i].toUnicodeString());
                }
                return sb.toString();
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                if (SingleOperation.EQUALS != operation) {
                    throw new IllegalArgumentException("Unsupported operation for CC search: " + operation);
                }
                return new CcTerm(constant.toString());
            }

        });
        m.put(MailJSONField.RECIPIENT_TO.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                final InternetAddress[] internetAddresses = candidate.getTo();
                if (null == internetAddresses) {
                    return null;
                }
                final int length = internetAddresses.length;
                if (length == 0) {
                    return null;
                }
                final StringBuilder sb = new StringBuilder(length * 16);
                sb.append(internetAddresses[0].toUnicodeString());
                for (int i = 1; i < length; i++) {
                    sb.append(',').append(internetAddresses[i].toUnicodeString());
                }
                return sb.toString();
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                if (SingleOperation.EQUALS != operation) {
                    throw new IllegalArgumentException("Unsupported operation for TO search: " + operation);
                }
                return new ToTerm(constant.toString());
            }

        });
        m.put(MailJSONField.SENT_DATE.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                final Date d = candidate.getSentDate();
                return null == d ? null : Long.valueOf(d.getTime());
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                ComparisonType ct;
                if (SingleOperation.EQUALS == operation) {
                    ct = ComparisonType.EQUALS;
                } else if (SingleOperation.GREATER_THAN == operation) {
                    ct = ComparisonType.GREATER_THAN;
                } else {
                    ct = ComparisonType.LESS_THAN;
                }
                try {
                    return new SentDateTerm(ct, new Date(Long.parseLong(constant.toString())));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Unsupported value for sent date search: " + constant.toString());
                }

            }

        });
        m.put(MailJSONField.SIZE.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                return Long.valueOf(candidate.getSize());
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                ComparisonType ct;
                if (SingleOperation.EQUALS == operation) {
                    ct = ComparisonType.EQUALS;
                } else if (SingleOperation.GREATER_THAN == operation) {
                    ct = ComparisonType.GREATER_THAN;
                } else {
                    ct = ComparisonType.LESS_THAN;
                }
                try {
                    return new SizeTerm(ct, (int) Long.parseLong(constant.toString()));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Unsupported value for size search: " + constant.toString());
                }

            }

        });
        m.put(MailJSONField.SUBJECT.getKey(), new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                return candidate.getSubject();
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                if (SingleOperation.EQUALS != operation) {
                    throw new IllegalArgumentException("Unsupported operation for subject search: " + operation);
                }
                return new SubjectTerm(constant.toString());
            }

        });
        m.put("text", new AttributeGetter() {

            @Override
            public Object getObject(MailMessage candidate) {
                try {
                    return candidate.getSource();
                } catch (Exception e) {
                    LOG.warn("Failed to get source text", e);
                    return "";
                }
            }

            @Override
            public SearchTerm<?> getSearchTerm(SingleOperation operation, Object constant) {
                if (SingleOperation.EQUALS != operation) {
                    throw new IllegalArgumentException("Unsupported operation for text search: " + operation);
                }
                return new TextTerm(constant.toString());
            }

        });

        GETTERS = ImmutableMap.copyOf(m);
    }

    private static final MailAttributeFetcher instance = new MailAttributeFetcher();

    /**
     * Gets the mail attribute fetcher instance.
     *
     * @return The mail attribute fetcher instance.
     */
    public static MailAttributeFetcher getInstance() {
        return instance;
    }

    /*-
     * Member section
     */

    /**
     * Initializes a new {@link MailAttributeFetcher}.
     */
    private MailAttributeFetcher() {
        super();
    }

    @Override
    public <T> T getAttribute(String attributeName, MailMessage candidate) {
        final AttributeGetter getter = GETTERS.get(attributeName);
        if (null == getter) {
            LOG.info("No getter for field: {}", attributeName);
            return null;
        }
        @SuppressWarnings("unchecked") final T retval = (T) getter.getObject(candidate);
        return retval;
    }

    /**
     * Gets the mail search term for given simple search term identifier
     *
     * @param attributeName The attribute name
     * @param operation The operation
     * @param constant The constant to compare to
     * @return The mail search term
     * @throws IllegalArgumentException If search term cannot be returned
     */
    public SearchTerm<?> getSearchTerm(String attributeName, SingleOperation operation, Object constant) {
        final AttributeGetter getter = GETTERS.get(attributeName);
        if (null == getter) {
            LOG.info("No getter for field: {}", attributeName);
            return null;
        }
        return getter.getSearchTerm(operation, constant);
    }

}
