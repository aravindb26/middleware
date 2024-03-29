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

package com.openexchange.user.json.mapping;

import static com.openexchange.java.Autoboxing.I;
import java.util.EnumMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.tools.mappings.json.ArrayMapping;
import com.openexchange.groupware.tools.mappings.json.DefaultJsonMapper;
import com.openexchange.groupware.tools.mappings.json.DefaultJsonMapping;
import com.openexchange.groupware.tools.mappings.json.IntegerMapping;
import com.openexchange.groupware.tools.mappings.json.JsonMapping;
import com.openexchange.groupware.tools.mappings.json.StringMapping;
import com.openexchange.java.Strings;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.mime.utils.MimeMessageUtility;
import com.openexchange.session.Session;
import com.openexchange.user.User;
import com.openexchange.user.json.field.UserField;
import com.openexchange.user.json.parser.ParsedUser;

/**
 * {@link UserMapper}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class UserMapper extends DefaultJsonMapper<User, UserField> {

    private static final UserMapper INSTANCE = new UserMapper();

    private UserField[] allFields = null;

    /**
     * Gets the UserMapper instance.
     *
     * @return The UserMapper instance.
     */
    public static UserMapper getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes a new {@link UserMapper}.
     */
    private UserMapper() {
        super();
    }

	@Override
	public User newInstance() {
		return new ParsedUser();
	}

	@Override
	public UserField[] newArray(int size) {
		return new UserField[size];
	}

	@Override
	public JsonMapping<? extends Object, User> get(final UserField field) throws OXException {
		if (null == field) {
			throw new IllegalArgumentException("field");
		}
		final JsonMapping<? extends Object, User> mapping = getMappings().get(field);
		if (null == mapping) {
			throw OXException.notFound(field.toString());
		}
		return mapping;
	}

    public UserField[] getAllFields() {
    	if (null == allFields) {
    		this.allFields = this.mappings.keySet().toArray(newArray(this.mappings.keySet().size()));
    	}
    	return this.allFields;
    }

	@Override
	protected EnumMap<UserField, JsonMapping<? extends Object, User>> createMappings() {
		final EnumMap<UserField, JsonMapping<? extends Object, User>> mappings = new
				EnumMap<UserField, JsonMapping<? extends Object, User>>(UserField.class);

		mappings.put(UserField.ID, new IntegerMapping<User>("id", I(1)) {

			@Override
			public boolean isSet(User object) {
				return -1 != object.getId();
			}

			@Override
			public void set(User object, Integer value) throws OXException {
				if (!(object instanceof ParsedUser)) {
					throw new UnsupportedOperationException();
				}
                ((ParsedUser)object).setId(null != value ? value.intValue() : -1);
			}

			@Override
			public Integer get(User object) {
				return Integer.valueOf(object.getId());
			}

			@Override
			public void remove(User object) {
				if (!(object instanceof ParsedUser)) {
					throw new UnsupportedOperationException();
				}
                ((ParsedUser)object).setId(-1);
			}
		});

		mappings.put(UserField.ALIASES, new ArrayMapping<String, User>("aliases", I(610)) {

			@Override
			public boolean isSet(User object) {
				return null != object.getAliases();
			}

			@Override
			public void set(User object, String[] value) throws OXException {
				throw new UnsupportedOperationException();
			}

			@Override
			public String[] get(User object) {
				final String[] aliases = object.getAliases();
				if (null == aliases || 0 == aliases.length) {
                    return aliases;
                }
				final int length = aliases.length;
                final String[] ret = new String[length];
                for (int i = 0; i < length; i++) {
                    ret[i] = addr2String(aliases[i]);
                }
                return ret;
			}

			@Override
			public void remove(User object) {
				throw new UnsupportedOperationException();
			}

			@Override
			public String[] newArray(int size) {
				return new String[size];
			}

			@Override
			protected String deserialize(JSONArray array, int index) throws JSONException, OXException {
				throw new UnsupportedOperationException();
			}
		});

		mappings.put(UserField.TIME_ZONE, new StringMapping<User>("timezone", I(611)) {

			@Override
			public boolean isSet(User object) {
				return null != object.getTimeZone();
			}

			@Override
			public void set(User object, String value) throws OXException {
				if (!(object instanceof ParsedUser)) {
					throw new UnsupportedOperationException();
				}
                ((ParsedUser)object).setTimeZone(value);
			}

			@Override
			public String get(User object) {
				return object.getTimeZone();
			}

			@Override
			public void remove(User object) {
				if (!(object instanceof ParsedUser)) {
					throw new UnsupportedOperationException();
				}
                ((ParsedUser)object).setTimeZone(null);
			}
		});

		mappings.put(UserField.LOCALE, new DefaultJsonMapping<Locale, User>("locale", I(612)) {

			@Override
			public void deserialize(JSONObject from, User to) throws JSONException, OXException {
				if (from.hasAndNotNull(getAjaxName())) {
					if (!(to instanceof ParsedUser)) {
						throw new UnsupportedOperationException();
					}
                    ((ParsedUser)to).setLocale(parseLocaleString(from.getString(getAjaxName())));
				}
			}

			@Override
			public boolean isSet(User object) {
				return null != object.getLocale();
			}

			@Override
			public void set(User object, Locale value) throws OXException {
				if (!(object instanceof ParsedUser)) {
					throw new UnsupportedOperationException();
				}
                ((ParsedUser)object).setLocale(value);
			}

			@Override
			public Locale get(User object) {
				return object.getLocale();
			}

			@Override
            public void remove(User object) {
                if (!(object instanceof ParsedUser)) {
                    throw new UnsupportedOperationException();
                }
                ((ParsedUser) object).setLocale(null);
            }
		});

		mappings.put(UserField.GROUPS, new DefaultJsonMapping<int[], User>("groups", I(613)) {

			@Override
			public void deserialize(JSONObject from, User to) throws JSONException, OXException {
				throw new UnsupportedOperationException();
			}

			@Override
			public Object serialize(User from, TimeZone timeZone, Session session) throws JSONException {
		        int[] value = this.get(from);
		        if (null == value) {
		            return JSONObject.NULL;
		        }
                final JSONArray jsonArray = new JSONArray(value.length);
                for (final int group : value) {
                    jsonArray.put(group);
                }
                return jsonArray;
		    }

			@Override
			public boolean isSet(User object) {
				return null != object.getGroups();
			}

			@Override
			public void set(User object, int[] value) throws OXException {
				throw new UnsupportedOperationException();
			}

			@Override
			public int[] get(User object) {
				return object.getGroups();
			}

			@Override
			public void remove(User object) {
				throw new UnsupportedOperationException();
			}
		});

		mappings.put(UserField.CONTACT_ID, new IntegerMapping<User>("contact_id", I(614)) {

			@Override
			public boolean isSet(User object) {
				return -1 != object.getContactId();
			}

			@Override
			public void set(User object, Integer value) throws OXException {
				throw new UnsupportedOperationException();
			}

			@Override
			public Integer get(User object) {
				return Integer.valueOf(object.getContactId());
			}

			@Override
			public void remove(User object) {
				throw new UnsupportedOperationException();
			}
		});

        mappings.put(UserField.LOGIN_INFO, new StringMapping<User>("login_info", I(615)) {

            @Override
            public boolean isSet(User object) {
                return null != object.getLoginInfo();
            }

            @Override
            public void set(User object, String value) throws OXException {
                throw new UnsupportedOperationException();
            }

            @Override
            public String get(User object) {
                return object.getLoginInfo();
            }

            @Override
            public void remove(User object) {
                throw new UnsupportedOperationException();
            }
        });

        mappings.put(UserField.GUEST_CREATED_BY, new IntegerMapping<User>("guest_created_by", I(616)) {

            @Override
            public boolean isSet(User object) {
                return 0 < object.getCreatedBy();
            }

            @Override
            public void set(User object, Integer value) throws OXException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Integer get(User object) {
                return Integer.valueOf(object.getCreatedBy());
            }

            @Override
            public void remove(User object) {
                throw new UnsupportedOperationException();
            }
        });

		return mappings;
	}

	private static final Pattern identifierPattern = Pattern.compile("(\\p{Lower}{2})(?:[_-]([a-zA-Z]{2}))?(?:[_-]([a-zA-Z]{2}))?");

    /**
     * Parses given locale string into an instance of {@link Locale}
     *
     * @param localeStr The locale string to parse
     * @return The parsed instance of {@link Locale} or <code>null</code>
     */
    static Locale parseLocaleString(final String localeStr) {
        if (null == localeStr) {
            return null;
        }
        final Matcher match = identifierPattern.matcher(localeStr);
        Locale retval = null;
        if (match.matches()) {
            final String country = match.group(2);
            final String variant = match.group(3);
            retval = new Locale(toLowerCase(match.group(1)), country == null ? "" : toUpperCase(country), variant == null ? "" : variant);
        }
        return retval;
    }

    /**
     * An own implementation of toLowerCase() to avoid circularity problems between Locale and String. The most straightforward algorithm is
     * used. Look at optimizations later.
     */
    private static String toLowerCase(final CharSequence chars) {
        if (null == chars) {
            return null;
        }
        final int length = chars.length();
        final StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            final char c = chars.charAt(i);
            builder.append((c >= 'A') && (c <= 'Z') ? (char) (c ^ 0x20) : c);
        }
        return builder.toString();
    }

    /**
     * An own implementation of toUpperCase() to avoid circularity problems between Locale and String. The most straightforward algorithm is
     * used. Look at optimizations later.
     */
    private static String toUpperCase(final CharSequence chars) {
        if (null == chars) {
            return null;
        }
        final int length = chars.length();
        final StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            final char c = chars.charAt(i);
            builder.append((c >= 'a') && (c <= 'z') ? (char) (c & 0x5f) : c);
        }
        return builder.toString();
    }

    static String addr2String(final String primaryAddress) {
        if (null == primaryAddress) {
            return primaryAddress;
        }
        try {
            final QuotedInternetAddress addr = new QuotedInternetAddress(primaryAddress);
            final String sAddress = addr.getAddress();
            final int pos = null == sAddress ? 0 : sAddress.indexOf('/');
            if (pos <= 0) {
                // No slash character present
                return addr.toUnicodeString();
            }

            @SuppressWarnings("null") String suffix = sAddress.substring(pos); // Guarded by 'pos'
            if (!"/TYPE=PLMN".equals(Strings.toUpperCase(suffix))) {
                // Not an MSISDN address
                return addr.toUnicodeString();
            }

            // A MSISDN address; e.g. "+491234567890/TYPE=PLMN"
            StringBuilder sb = new StringBuilder(32);
            String personal = addr.getPersonal();
            if (null == personal) {
                sb.append(MimeMessageUtility.prepareAddress(sAddress.substring(0, pos)));
            } else {
                sb.append(preparePersonal(personal));
                sb.append(" <").append(MimeMessageUtility.prepareAddress(sAddress.substring(0, pos))).append('>');
            }
            return sb.toString();
        } catch (Exception e) {
            return primaryAddress;
        }
    }

    /**
     * Prepares specified personal string by surrounding it with quotes if needed.
     *
     * @param personal The personal
     * @return The prepared personal
     */
    static String preparePersonal(final String personal) {
        return MimeMessageUtility.quotePhrase(personal, false);
    }

}
