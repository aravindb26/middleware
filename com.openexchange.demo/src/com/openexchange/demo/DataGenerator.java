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

package com.openexchange.demo;

import static com.openexchange.demo.DemoProperty.FILESTORE_SCHEME;
import static com.openexchange.demo.DemoProperty.FILESTORE_SIZE;
import static com.openexchange.demo.DemoProperty.MAIL_DOMAIN;
import static com.openexchange.demo.DemoProperty.USER_IMAGE;
import static com.openexchange.demo.DemoProperty.USER_LOCALE;
import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import com.github.javafaker.Faker;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Database;
import com.openexchange.admin.rmi.dataobjects.Filestore;
import com.openexchange.admin.rmi.dataobjects.Group;
import com.openexchange.admin.rmi.dataobjects.Resource;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.java.Streams;

/**
 * {@link DataGenerator}
 *
 * @author <a href="nikolaos.tsapanidis@open-xchange.com">Nikolaos Tsapanidis</a>
 * @since v8.0.0
 */
public final class DataGenerator {

    private final LeanConfigurationService configService;
    private final Faker faker;

    /**
     * Initializes a new {@link DataGenerator}.
     *
     * @param configService The config service
     */
    public DataGenerator(LeanConfigurationService configService) {
        this.configService = configService;
        this.faker = new Faker(Locale.getDefault());
    }

    /**
     * Creates a user
     *
     * @param cid The context identifier
     * @param username An optional username
     * @return User with random data
     */
    public User createUser(int cid, Optional<String> username) {
        //@formatter:off
        String[] locales = Arrays.stream(configService.getProperty(USER_LOCALE).split(","))
                .map(String::trim)
                .filter(s -> s != null && s.length() > 0)
                .toArray(String[]::new);
        //@formatter:on

        Locale locale = null;
        if (locales.length > 0) {
            locale = LocaleUtils.toLocale(locales[ThreadLocalRandom.current().nextInt(locales.length)]);
        } else {
            locale = Locale.ENGLISH;
        }

        String mailDomain = mailDomainFormatter(cid);
        String imapServer = configService.getProperty(DefaultProperty.valueOf("com.openexchange.mail.mailServer", "localhost"));
        String smtpServer = configService.getProperty(DefaultProperty.valueOf("com.openexchange.mail.transportServer", "localhost"));
        String password = configService.getProperty(DemoProperty.USER_PASSWORD);

        //@formatter:off
        String name = StringUtils.stripAccents(username.orElse(faker.name().username())
                .toLowerCase()
                .replace("\u00fc", "ue")
                .replace("\u00f6", "oe")
                .replace("\u00e4", "ae")
                .replace("\u00df", "ss"));
        //@formatter:on

        String firstname;
        String lastname;
        if (name.contains(".")) {
            String[] names = name.split("\\.");
            if (names.length < 2) {
                firstname = faker.name().firstName();
                lastname = faker.name().lastName();
            } else {
                firstname = StringUtils.capitalize(names[0]);
                lastname = StringUtils.capitalize(names[1]);
            }
        } else {
            firstname = StringUtils.capitalize(name);
            lastname = StringUtils.capitalize(StringUtils.reverse(name));
        }

        User user = new User();
        // mandatory settings
        user.setName(name);
        user.setGiven_name(firstname);
        user.setSur_name(lastname);
        user.setDisplay_name(firstname + " " + lastname);
        user.setPrimaryEmail(name + "@" + mailDomain);
        user.setEmail1(name + "@" + mailDomain);
        user.setPassword(password);
        // fake data
        user.setCity_home(faker.address().city());
        user.setCountry_home(faker.address().country());
        user.setPostal_code_home(faker.address().zipCode());
        user.setStreet_home(faker.address().streetAddress());
        user.setTelephone_home1(faker.phoneNumber().phoneNumber());
        user.setLanguage(locale.toString());
        user.setBirthday(faker.date().birthday());
        user.setCompany(faker.company().name());
        user.setProfession(faker.company().profession());
        user.setTitle(faker.name().title());
        user.setMarital_status(faker.demographic().maritalStatus());
        user.setNote(faker.lorem().paragraph());
        // mail settings
        user.setImapLogin(name + "@" + mailDomain);
        user.setImapServer(imapServer);
        user.setSmtpServer(smtpServer);

        if (configService.getBooleanProperty(USER_IMAGE)) {
            if (0 == ThreadLocalRandom.current().nextInt() % 2) {
                String imageUrl = "https://robohash.org/" + name + ".png?size=200x200&set=set5";
                byte[] imageData = null;
                InputStream inputStream = null;
                try {
                    inputStream = new URL(imageUrl).openStream();
                    imageData = Streams.stream2bytes(inputStream);
                } catch (Exception e) {
                    // ignore
                } finally {
                    Streams.close(inputStream);
                }
                user.setImage1(imageData);
                user.setImage1ContentType("image/png");
            }
        }

        return user;
    }

    /**
     * Creates an admin user
     *
     * @param cid The context identifier
     * @return The <i>oxadmin</i> user
     */
    public User createAdmin(int cid) {
        return createUser(cid, Optional.of("oxadmin"));
    }

    /**
     * Creates a group with a couple of user
     *
     * @param userCount The amount of provisioned users in a context
     * @return The Group
     */
    public Group createGroup(int userCount) {
        // the id 2 is reserved for the oxadmin user, so start from 3
        //@formatter:off
        Integer[] members = ThreadLocalRandom.current().ints(userCount, 3, userCount + 3)
                .distinct()
                .boxed()
                .toArray(Integer[]::new);
        //@formatter:on

        Group group = new Group();
        group.setDisplayname("Demo Group");
        group.setName("demo_group");
        group.setMembers(members);

        return group;
    }

    /**
     * Creates a {@link Resource}
     *
     * @param cid The context identifier
     * @return The Resource
     */
    public Resource createResource(int cid) {
        Resource resource = new Resource();
        resource.setName("demo_resource");
        resource.setDisplayname("Demo Resource");
        resource.setEmail("resource@" + mailDomainFormatter(cid));

        return resource;
    }

    /**
     * Creates a {@link Context}
     *
     * @param cid The context identifier
     * @return The context
     */
    public Context createContext(int cid) {
        Context context = new Context(I(cid));
        context.setMaxQuota(L(configService.getLongProperty(FILESTORE_SIZE)));
        context.addLoginMapping(mailDomainFormatter(cid));
        if (cid == 1) {
            context.addLoginMapping("defaultcontext");
        }

        return context;
    }

    /**
     * Creates a {@link Database}
     *
     * @param name The name of the database
     * @param host The host or ip of the database
     * @param user The user of the database
     * @param password The password of the database
     * @param isMaster Whether database is master or not
     * @param maxUnits The maximum amount of units
     * @return The database
     */
    public Database createDatabase(String name, String host, String user, String password, boolean isMaster, Integer maxUnits) {
        Database db = new Database();
        db.setName(name);
        db.setMaster(B(isMaster));
        db.setMaxUnits(maxUnits);
        db.setUrl("jdbc:mysql://" + host);
        db.setLogin(user);
        db.setPassword(password);

        return db;
    }

    /**
     * Creates a {@link Filestore}
     *
     * @param path The path of the filestore
     * @param size The size of the filestore
     * @param maxCtx The maximum amount of contexts
     * @return The filestore
     */
    public Filestore createFilestore(String path, Long size, Integer maxCtx) {
        String url = String.format("%s://%s", configService.getProperty(FILESTORE_SCHEME), path);
        Filestore filestore = new Filestore();
        filestore.setUrl(url);
        filestore.setSize(size);
        filestore.setMaxContexts(maxCtx);

        return filestore;
    }

    /**
     * Formats a mail domain
     *
     * @param cid The context identifier
     * @return The formatted mail domain
     */
    private String mailDomainFormatter(int cid) {
        return String.format("context%d.%s", I(cid), configService.getProperty(MAIL_DOMAIN));
    }
}
