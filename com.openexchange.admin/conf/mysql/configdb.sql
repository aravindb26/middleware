#@(#) Tables for the Configuration Database

CREATE TABLE configdb_sequence (
    id INT4 UNSIGNED NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO configdb_sequence VALUES (0);

CREATE TABLE db_pool (
    db_pool_id INT4 UNSIGNED NOT NULL,
    url VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    driver VARCHAR(128) COLLATE utf8mb4_unicode_ci NOT NULL,
    login VARCHAR(128) COLLATE utf8mb4_unicode_ci NOT NULL,
    password VARCHAR(128) COLLATE utf8mb4_unicode_ci NOT NULL,
    hardlimit INT4,
    max INT4,
    initial INT4,
    name VARCHAR(128) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (db_pool_id),
    INDEX (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE db_cluster (
    cluster_id INT4 UNSIGNED NOT NULL,
    read_db_pool_id INT4 UNSIGNED NOT NULL,
    write_db_pool_id INT4 UNSIGNED NOT NULL,
    weight INT4 UNSIGNED,
    max_units INT4,
    PRIMARY KEY (cluster_id),
    FOREIGN KEY(write_db_pool_id) REFERENCES db_pool (db_pool_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reason_text (
    id INT4 UNSIGNED NOT NULL,
    text VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE context (
    cid INT4 UNSIGNED NOT NULL,
    name VARCHAR(128) COLLATE utf8mb4_unicode_ci NOT NULL,
    enabled BOOLEAN,
    reason_id INT4 UNSIGNED,
    filestore_id INT4 UNSIGNED,
    filestore_name VARCHAR(32) COLLATE utf8mb4_unicode_ci,
    filestore_login VARCHAR(32) COLLATE utf8mb4_unicode_ci,
    filestore_passwd VARCHAR(32) COLLATE utf8mb4_unicode_ci,
    quota_max INT8,
    PRIMARY KEY (cid),
    INDEX (filestore_id),
    UNIQUE KEY `context_name_unique` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE filestore (
    id INT4 UNSIGNED NOT NULL,
    uri VARCHAR(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
    size INT8 UNSIGNED NOT NULL,
    max_context INT4,
    PRIMARY KEY (id),
    INDEX (max_context),
    CONSTRAINT filestore_uri_unique UNIQUE(uri)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE filestore2user (
    cid INT4 UNSIGNED NOT NULL,
    user INT4 UNSIGNED NOT NULL,
    filestore_id INT4 UNSIGNED,
    PRIMARY KEY (cid, user),
    KEY `filestore_id_index` (`filestore_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE server (
    server_id INT4 UNSIGNED NOT NULL,
    name VARCHAR(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
    uuid BINARY(16) NOT NULL,
    PRIMARY KEY (server_id),
    CONSTRAINT server_name_unique UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE login2context (
    cid INT4 UNSIGNED NOT NULL,
    login_info VARCHAR(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
    PRIMARY KEY (`login_info`),
    FOREIGN KEY(`cid`) REFERENCES context (`cid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE context_server2db_pool (
    server_id INT4 UNSIGNED NOT NULL,
    cid INT4 UNSIGNED NOT NULL,
    read_db_pool_id INT4 UNSIGNED NOT NULL,
    write_db_pool_id INT4 UNSIGNED NOT NULL,
    db_schema VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY(`cid`, `server_id`),
    INDEX (server_id),
    INDEX (db_schema),
    INDEX `poolAndSchema` (write_db_pool_id, db_schema),
    FOREIGN KEY(`cid`) REFERENCES context (`cid`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE contexts_per_dbpool (
    db_pool_id INT4 UNSIGNED NOT NULL,
    count INT4 UNSIGNED NOT NULL,
    PRIMARY KEY (db_pool_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE contexts_per_filestore (
    filestore_id INT4 UNSIGNED NOT NULL,
    count INT4 UNSIGNED NOT NULL,
    PRIMARY KEY (filestore_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE contexts_per_dbschema (
    db_pool_id INT4 UNSIGNED NOT NULL,
    schemaname VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL,
    count INT4 UNSIGNED NOT NULL,
    creating_date BIGINT(64) NOT NULL,
    PRIMARY KEY (db_pool_id, schemaname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE dbpool_lock (
    db_pool_id INT4 UNSIGNED NOT NULL,
    PRIMARY KEY (db_pool_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE dbschema_lock (
    db_pool_id INT4 UNSIGNED NOT NULL,
    schemaname VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (db_pool_id, schemaname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE context2push_registration (
    cid INT4 UNSIGNED NOT NULL,
    PRIMARY KEY (cid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE replicationMonitor (
    cid int(10) unsigned NOT NULL,
    transaction bigint(20) NOT NULL,
    PRIMARY KEY (cid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO replicationMonitor (cid,transaction) VALUES (0,0);

CREATE TABLE advertisement_mapping (
    reseller VARCHAR(128) COLLATE utf8mb4_unicode_ci NOT NULL,
    package VARCHAR(128) COLLATE utf8mb4_unicode_ci NOT NULL,
    configId int NOT NULL,
    PRIMARY KEY (reseller, package)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE advertisement_config (
	configId int NOT NULL AUTO_INCREMENT,
	reseller VARCHAR(128) COLLATE utf8mb4_unicode_ci NOT NULL,
	config text COLLATE utf8mb4_unicode_ci NOT NULL,
	PRIMARY KEY (configId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
