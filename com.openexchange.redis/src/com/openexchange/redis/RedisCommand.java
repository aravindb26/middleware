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

package com.openexchange.redis;

/**
 * {@link RedisCommand} -The enumeration og known Redis commands.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public enum RedisCommand {

    /** ACL CAT - List the ACL categories or the commands inside a category */
    ACL_CAT("ACL CAT"),

    /** ACL DELUSER - Remove the specified ACL users and the associated rules */
    ACL_DELUSER("ACL DELUSER"),

    /** ACL DRYRUN - Returns whether the user can execute the given command without executing the command. */
    ACL_DRYRUN("ACL DRYRUN"),

    /** ACL GENPASS - Generate a pseudorandom secure password to use for ACL users */
    ACL_GENPASS("ACL GENPASS"),

    /** ACL GETUSER - Get the rules for a specific ACL user */
    ACL_GETUSER("ACL GETUSER"),

    /** ACL LIST - List the current ACL rules in ACL config file format */
    ACL_LIST("ACL LIST"),

    /** ACL LOAD - Reload the ACLs from the configured ACL file */
    ACL_LOAD("ACL LOAD"),

    /** ACL LOG - List latest events denied because of ACLs in place */
    ACL_LOG("ACL LOG"),

    /** ACL SAVE - Save the current ACL rules in the configured ACL file */
    ACL_SAVE("ACL SAVE"),

    /** ACL SETUSER - Modify or create the rules for a specific ACL user */
    ACL_SETUSER("ACL SETUSER"),

    /** ACL USERS - List the username of all the configured ACL rules */
    ACL_USERS("ACL USERS"),

    /** ACL WHOAMI - Return the name of the user associated to the current connection */
    ACL_WHOAMI("ACL WHOAMI"),

    /** APPEND - Append a value to a key */
    APPEND("APPEND"),

    /** ASKING - Sent by cluster clients after an -ASK redirect */
    ASKING("ASKING"),

    /** AUTH - Authenticate to the server */
    AUTH("AUTH"),

    /** BF.ADD - Adds an item to a Bloom Filter */
    BF_ADD("BF.ADD"),

    /** BF.CARD - Returns the cardinality of a Bloom filter */
    BF_CARD("BF.CARD"),

    /** BF.EXISTS - Checks whether an item exists in a Bloom Filter */
    BF_EXISTS("BF.EXISTS"),

    /** BF.INFO - Returns information about a Bloom Filter */
    BF_INFO("BF.INFO"),

    /** BF.INSERT - Adds one or more items to a Bloom Filter. A filter will be created if it does not exist */
    BF_INSERT("BF.INSERT"),

    /** BF.LOADCHUNK - Restores a filter previously saved using SCANDUMP */
    BF_LOADCHUNK("BF.LOADCHUNK"),

    /** BF.MADD - Adds one or more items to a Bloom Filter. A filter will be created if it does not exist */
    BF_MADD("BF.MADD"),

    /** BF.MEXISTS - Checks whether one or more items exist in a Bloom Filter */
    BF_MEXISTS("BF.MEXISTS"),

    /** BF.RESERVE - Creates a new Bloom Filter */
    BF_RESERVE("BF.RESERVE"),

    /** BF.SCANDUMP - Begins an incremental save of the bloom filter */
    BF_SCANDUMP("BF.SCANDUMP"),

    /** BGREWRITEAOF - Asynchronously rewrite the append-only file */
    BGREWRITEAOF("BGREWRITEAOF"),

    /** BGSAVE - Asynchronously save the dataset to disk */
    BGSAVE("BGSAVE"),

    /** BITCOUNT - Count set bits in a string */
    BITCOUNT("BITCOUNT"),

    /** BITFIELD - Perform arbitrary bitfield integer operations on strings */
    BITFIELD("BITFIELD"),

    /** BITFIELD_RO - Perform arbitrary bitfield integer operations on strings. Read-only variant of BITFIELD */
    BITFIELD_RO("BITFIELD_RO"),

    /** BITOP - Perform bitwise operations between strings */
    BITOP("BITOP"),

    /** BITPOS - Find first bit set or clear in a string */
    BITPOS("BITPOS"),

    /** BLMOVE - Pop an element from a list, push it to another list and return it; or block until one is available */
    BLMOVE("BLMOVE"),

    /** BLMPOP - Pop elements from a list, or block until one is available */
    BLMPOP("BLMPOP"),

    /** BLPOP - Remove and get the first element in a list, or block until one is available */
    BLPOP("BLPOP"),

    /** BRPOP - Remove and get the last element in a list, or block until one is available */
    BRPOP("BRPOP"),

    /** BRPOPLPUSH - Pop an element from a list, push it to another list and return it; or block until one is available */
    BRPOPLPUSH("BRPOPLPUSH"),

    /** BZMPOP - Remove and return members with scores in a sorted set or block until one is available */
    BZMPOP("BZMPOP"),

    /** BZPOPMAX - Remove and return the member with the highest score from one or more sorted sets, or block until one is available */
    BZPOPMAX("BZPOPMAX"),

    /** BZPOPMIN - Remove and return the member with the lowest score from one or more sorted sets, or block until one is available */
    BZPOPMIN("BZPOPMIN"),

    /** CF.ADD - Adds an item to a Cuckoo Filter */
    CF_ADD("CF.ADD"),

    /** CF.ADDNX - Adds an item to a Cuckoo Filter if the item did not exist previously. */
    CF_ADDNX("CF.ADDNX"),

    /** CF.COUNT - Return the number of times an item might be in a Cuckoo Filter */
    CF_COUNT("CF.COUNT"),

    /** CF.DEL - Deletes an item from a Cuckoo Filter */
    CF_DEL("CF.DEL"),

    /** CF.EXISTS - Checks whether one or more items exist in a Cuckoo Filter */
    CF_EXISTS("CF.EXISTS"),

    /** CF.INFO - Returns information about a Cuckoo Filter */
    CF_INFO("CF.INFO"),

    /** CF.INSERT - Adds one or more items to a Cuckoo Filter. A filter will be created if it does not exist */
    CF_INSERT("CF.INSERT"),

    /** CF.INSERTNX - Adds one or more items to a Cuckoo Filter if the items did not exist previously. A filter will be created if it does not exist */
    CF_INSERTNX("CF.INSERTNX"),

    /** CF.LOADCHUNK - Restores a filter previously saved using SCANDUMP */
    CF_LOADCHUNK("CF.LOADCHUNK"),

    /** CF.MEXISTS - Checks whether one or more items exist in a Cuckoo Filter */
    CF_MEXISTS("CF.MEXISTS"),

    /** CF.RESERVE - Creates a new Cuckoo Filter */
    CF_RESERVE("CF.RESERVE"),

    /** CF.SCANDUMP - Begins an incremental save of the bloom filter */
    CF_SCANDUMP("CF.SCANDUMP"),

    /** CLIENT CACHING - Instruct the server about tracking or not keys in the next request */
    CLIENT_CACHING("CLIENT CACHING"),

    /** CLIENT GETNAME - Get the current connection name */
    CLIENT_GETNAME("CLIENT GETNAME"),

    /** CLIENT GETREDIR - Get tracking notifications redirection client ID if any */
    CLIENT_GETREDIR("CLIENT GETREDIR"),

    /** CLIENT ID - Returns the client ID for the current connection */
    CLIENT_ID("CLIENT ID"),

    /** CLIENT INFO - Returns information about the current client connection. */
    CLIENT_INFO("CLIENT INFO"),

    /** CLIENT KILL - Kill the connection of a client */
    CLIENT_KILL("CLIENT KILL"),

    /** CLIENT LIST - Get the list of client connections */
    CLIENT_LIST("CLIENT LIST"),

    /** CLIENT NO-EVICT - Set client eviction mode for the current connection */
    CLIENT_NO_EVICT("CLIENT NO-EVICT"),

    /** CLIENT PAUSE - Stop processing commands from clients for some time */
    CLIENT_PAUSE("CLIENT PAUSE"),

    /** CLIENT REPLY - Instruct the server whether to reply to commands */
    CLIENT_REPLY("CLIENT REPLY"),

    /** CLIENT SETNAME - Set the current connection name */
    CLIENT_SETNAME("CLIENT SETNAME"),

    /** CLIENT TRACKING - Enable or disable server assisted client side caching support */
    CLIENT_TRACKING("CLIENT TRACKING"),

    /** CLIENT TRACKINGINFO - Return information about server assisted client side caching for the current connection */
    CLIENT_TRACKINGINFO("CLIENT TRACKINGINFO"),

    /** CLIENT UNBLOCK - Unblock a client blocked in a blocking command from a different connection */
    CLIENT_UNBLOCK("CLIENT UNBLOCK"),

    /** CLIENT UNPAUSE - Resume processing of clients that were paused */
    CLIENT_UNPAUSE("CLIENT UNPAUSE"),

    /** CLUSTER ADDSLOTS - Assign new hash slots to receiving node */
    CLUSTER_ADDSLOTS("CLUSTER ADDSLOTS"),

    /** CLUSTER ADDSLOTSRANGE - Assign new hash slots to receiving node */
    CLUSTER_ADDSLOTSRANGE("CLUSTER ADDSLOTSRANGE"),

    /** CLUSTER BUMPEPOCH - Advance the cluster config epoch */
    CLUSTER_BUMPEPOCH("CLUSTER BUMPEPOCH"),

    /** CLUSTER COUNT-FAILURE-REPORTS - Return the number of failure reports active for a given node */
    CLUSTER_COUNT_FAILURE_REPORTS("CLUSTER COUNT-FAILURE-REPORTS"),

    /** CLUSTER COUNTKEYSINSLOT - Return the number of local keys in the specified hash slot */
    CLUSTER_COUNTKEYSINSLOT("CLUSTER COUNTKEYSINSLOT"),

    /** CLUSTER DELSLOTS - Set hash slots as unbound in receiving node */
    CLUSTER_DELSLOTS("CLUSTER DELSLOTS"),

    /** CLUSTER DELSLOTSRANGE - Set hash slots as unbound in receiving node */
    CLUSTER_DELSLOTSRANGE("CLUSTER DELSLOTSRANGE"),

    /** CLUSTER FAILOVER - Forces a replica to perform a manual failover of its master. */
    CLUSTER_FAILOVER("CLUSTER FAILOVER"),

    /** CLUSTER FLUSHSLOTS - Delete a node's own slots information */
    CLUSTER_FLUSHSLOTS("CLUSTER FLUSHSLOTS"),

    /** CLUSTER FORGET - Remove a node from the nodes table */
    CLUSTER_FORGET("CLUSTER FORGET"),

    /** CLUSTER GETKEYSINSLOT - Return local key names in the specified hash slot */
    CLUSTER_GETKEYSINSLOT("CLUSTER GETKEYSINSLOT"),

    /** CLUSTER INFO - Provides info about Redis Cluster node state */
    CLUSTER_INFO("CLUSTER INFO"),

    /** CLUSTER KEYSLOT - Returns the hash slot of the specified key */
    CLUSTER_KEYSLOT("CLUSTER KEYSLOT"),

    /** CLUSTER LINKS - Returns a list of all TCP links to and from peer nodes in cluster */
    CLUSTER_LINKS("CLUSTER LINKS"),

    /** CLUSTER MEET - Force a node cluster to handshake with another node */
    CLUSTER_MEET("CLUSTER MEET"),

    /** CLUSTER MYID - Return the node id */
    CLUSTER_MYID("CLUSTER MYID"),

    /** CLUSTER MYSHARDID - Return the node shard id */
    CLUSTER_MYSHARDID("CLUSTER MYSHARDID"),

    /** CLUSTER NODES - Get Cluster config for the node */
    CLUSTER_NODES("CLUSTER NODES"),

    /** CLUSTER REPLICAS - List replica nodes of the specified master node */
    CLUSTER_REPLICAS("CLUSTER REPLICAS"),

    /** CLUSTER REPLICATE - Reconfigure a node as a replica of the specified master node */
    CLUSTER_REPLICATE("CLUSTER REPLICATE"),

    /** CLUSTER RESET - Reset a Redis Cluster node */
    CLUSTER_RESET("CLUSTER RESET"),

    /** CLUSTER SAVECONFIG - Forces the node to save cluster state on disk */
    CLUSTER_SAVECONFIG("CLUSTER SAVECONFIG"),

    /** CLUSTER SET-CONFIG-EPOCH - Set the configuration epoch in a new node */
    CLUSTER_SET_CONFIG_EPOCH("CLUSTER SET-CONFIG-EPOCH"),

    /** CLUSTER SETSLOT - Bind a hash slot to a specific node */
    CLUSTER_SETSLOT("CLUSTER SETSLOT"),

    /** CLUSTER SHARDS - Get array of cluster slots to node mappings */
    CLUSTER_SHARDS("CLUSTER SHARDS"),

    /** CLUSTER SLAVES - List replica nodes of the specified master node */
    CLUSTER_SLAVES("CLUSTER SLAVES"),

    /** CLUSTER SLOTS - Get array of Cluster slot to node mappings */
    CLUSTER_SLOTS("CLUSTER SLOTS"),

    /** CMS.INCRBY - Increases the count of one or more items by increment */
    CMS_INCRBY("CMS.INCRBY"),

    /** CMS.INFO - Returns information about a sketch */
    CMS_INFO("CMS.INFO"),

    /** CMS.INITBYDIM - Initializes a Count-Min Sketch to dimensions specified by user */
    CMS_INITBYDIM("CMS.INITBYDIM"),

    /** CMS.INITBYPROB - Initializes a Count-Min Sketch to accommodate requested tolerances. */
    CMS_INITBYPROB("CMS.INITBYPROB"),

    /** CMS.MERGE - Merges several sketches into one sketch */
    CMS_MERGE("CMS.MERGE"),

    /** CMS.QUERY - Returns the count for one or more items in a sketch */
    CMS_QUERY("CMS.QUERY"),

    /** COMMAND - Get array of Redis command details */
    COMMAND("COMMAND"),

    /** COMMAND COUNT - Get total number of Redis commands */
    COMMAND_COUNT("COMMAND COUNT"),

    /** COMMAND DOCS - Get array of specific Redis command documentation */
    COMMAND_DOCS("COMMAND DOCS"),

    /** COMMAND GETKEYS - Extract keys given a full Redis command */
    COMMAND_GETKEYS("COMMAND GETKEYS"),

    /** COMMAND GETKEYSANDFLAGS - Extract keys and access flags given a full Redis command */
    COMMAND_GETKEYSANDFLAGS("COMMAND GETKEYSANDFLAGS"),

    /** COMMAND INFO - Get array of specific Redis command details, or all when no argument is given. */
    COMMAND_INFO("COMMAND INFO"),

    /** COMMAND LIST - Get an array of Redis command names */
    COMMAND_LIST("COMMAND LIST"),

    /** CONFIG GET - Get the values of configuration parameters */
    CONFIG_GET("CONFIG GET"),

    /** CONFIG RESETSTAT - Reset the stats returned by INFO */
    CONFIG_RESETSTAT("CONFIG RESETSTAT"),

    /** CONFIG REWRITE - Rewrite the configuration file with the in memory configuration */
    CONFIG_REWRITE("CONFIG REWRITE"),

    /** CONFIG SET - Set configuration parameters to the given values */
    CONFIG_SET("CONFIG SET"),

    /** COPY - Copy a key */
    COPY("COPY"),

    /** DBSIZE - Return the number of keys in the selected database */
    DBSIZE("DBSIZE"),

    /** DECR - Decrement the integer value of a key by one */
    DECR("DECR"),

    /** DECRBY - Decrement the integer value of a key by the given number */
    DECRBY("DECRBY"),

    /** DEL - Delete a key */
    DEL("DEL"),

    /** DISCARD - Discard all commands issued after MULTI */
    DISCARD("DISCARD"),

    /** DUMP - Return a serialized version of the value stored at the specified key. */
    DUMP("DUMP"),

    /** ECHO - Echo the given string */
    ECHO("ECHO"),

    /** EVAL - Execute a Lua script server side */
    EVAL("EVAL"),

    /** EVAL_RO - Execute a read-only Lua script server side */
    EVAL_RO("EVAL_RO"),

    /** EVALSHA - Execute a Lua script server side */
    EVALSHA("EVALSHA"),

    /** EVALSHA_RO - Execute a read-only Lua script server side */
    EVALSHA_RO("EVALSHA_RO"),

    /** EXEC - Execute all commands issued after MULTI */
    EXEC("EXEC"),

    /** EXISTS - Determine if a key exists */
    EXISTS("EXISTS"),

    /** EXPIRE - Set a key's time to live in seconds */
    EXPIRE("EXPIRE"),

    /** EXPIREAT - Set the expiration for a key as a UNIX timestamp */
    EXPIREAT("EXPIREAT"),

    /** EXPIRETIME - Get the expiration Unix timestamp for a key */
    EXPIRETIME("EXPIRETIME"),

    /** FAILOVER - Start a coordinated failover between this server and one of its replicas. */
    FAILOVER("FAILOVER"),

    /** FCALL - Invoke a function */
    FCALL("FCALL"),

    /** FCALL_RO - Invoke a read-only function */
    FCALL_RO("FCALL_RO"),

    /** FLUSHALL - Remove all keys from all databases */
    FLUSHALL("FLUSHALL"),

    /** FLUSHDB - Remove all keys from the current database */
    FLUSHDB("FLUSHDB"),

    /** FT._LIST - Returns a list of all existing indexes */
    FT_LIST("FT._LIST"),

    /** FT.AGGREGATE - Run a search query on an index and perform aggregate transformations on the results */
    FT_AGGREGATE("FT.AGGREGATE"),

    /** FT.ALIASADD - Adds an alias to the index */
    FT_ALIASADD("FT.ALIASADD"),

    /** FT.ALIASDEL - Deletes an alias from the index */
    FT_ALIASDEL("FT.ALIASDEL"),

    /** FT.ALIASUPDATE - Adds or updates an alias to the index */
    FT_ALIASUPDATE("FT.ALIASUPDATE"),

    /** FT.ALTER - Adds a new field to the index */
    FT_ALTER("FT.ALTER"),

    /** FT.CONFIG GET - Retrieves runtime configuration options */
    FT_CONFIG_GET("FT.CONFIG GET"),

    /** FT.CONFIG SET - Sets runtime configuration options */
    FT_CONFIG_SET("FT.CONFIG SET"),

    /** FT.CREATE - Creates an index with the given spec */
    FT_CREATE("FT.CREATE"),

    /** FT.CURSOR DEL - Deletes a cursor */
    FT_CURSOR_DEL("FT.CURSOR DEL"),

    /** FT.CURSOR READ - Reads from a cursor */
    FT_CURSOR_READ("FT.CURSOR READ"),

    /** FT.DICTADD - Adds terms to a dictionary */
    FT_DICTADD("FT.DICTADD"),

    /** FT.DICTDEL - Deletes terms from a dictionary */
    FT_DICTDEL("FT.DICTDEL"),

    /** FT.DICTDUMP - Dumps all terms in the given dictionary */
    FT_DICTDUMP("FT.DICTDUMP"),

    /** FT.DROPINDEX - Deletes the index */
    FT_DROPINDEX("FT.DROPINDEX"),

    /** FT.EXPLAIN - Returns the execution plan for a complex query */
    FT_EXPLAIN("FT.EXPLAIN"),

    /** FT.EXPLAINCLI - Returns the execution plan for a complex query */
    FT_EXPLAINCLI("FT.EXPLAINCLI"),

    /** FT.INFO - Returns information and statistics on the index */
    FT_INFO("FT.INFO"),

    /** FT.PROFILE - Performs a `FT.SEARCH` or `FT.AGGREGATE` command and collects performance information */
    FT_PROFILE("FT.PROFILE"),

    /** FT.SEARCH - Searches the index with a textual query, returning either documents or just ids */
    FT_SEARCH("FT.SEARCH"),

    /** FT.SPELLCHECK - Performs spelling correction on a query, returning suggestions for misspelled terms */
    FT_SPELLCHECK("FT.SPELLCHECK"),

    /** FT.SUGADD - Adds a suggestion string to an auto-complete suggestion dictionary */
    FT_SUGADD("FT.SUGADD"),

    /** FT.SUGDEL - Deletes a string from a suggestion index */
    FT_SUGDEL("FT.SUGDEL"),

    /** FT.SUGGET - Gets completion suggestions for a prefix */
    FT_SUGGET("FT.SUGGET"),

    /** FT.SUGLEN - Gets the size of an auto-complete suggestion dictionary */
    FT_SUGLEN("FT.SUGLEN"),

    /** FT.SYNDUMP - Dumps the contents of a synonym group */
    FT_SYNDUMP("FT.SYNDUMP"),

    /** FT.SYNUPDATE - Creates or updates a synonym group with additional terms */
    FT_SYNUPDATE("FT.SYNUPDATE"),

    /** FT.TAGVALS - Returns the distinct tags indexed in a Tag field */
    FT_TAGVALS("FT.TAGVALS"),

    /** FUNCTION DELETE - Delete a function by name */
    FUNCTION_DELETE("FUNCTION DELETE"),

    /** FUNCTION DUMP - Dump all functions into a serialized binary payload */
    FUNCTION_DUMP("FUNCTION DUMP"),

    /** FUNCTION FLUSH - Deleting all functions */
    FUNCTION_FLUSH("FUNCTION FLUSH"),

    /** FUNCTION KILL - Kill the function currently in execution. */
    FUNCTION_KILL("FUNCTION KILL"),

    /** FUNCTION LIST - List information about all the functions */
    FUNCTION_LIST("FUNCTION LIST"),

    /** FUNCTION LOAD - Create a function with the given arguments (name, code, description) */
    FUNCTION_LOAD("FUNCTION LOAD"),

    /** FUNCTION RESTORE - Restore all the functions on the given payload */
    FUNCTION_RESTORE("FUNCTION RESTORE"),

    /** FUNCTION STATS - Return information about the function currently running (name, description, duration) */
    FUNCTION_STATS("FUNCTION STATS"),

    /** GEOADD - Add one or more geospatial items in the geospatial index represented using a sorted set */
    GEOADD("GEOADD"),

    /** GEODIST - Returns the distance between two members of a geospatial index */
    GEODIST("GEODIST"),

    /** GEOHASH - Returns members of a geospatial index as standard geohash strings */
    GEOHASH("GEOHASH"),

    /** GEOPOS - Returns longitude and latitude of members of a geospatial index */
    GEOPOS("GEOPOS"),

    /** GEORADIUS - Query a sorted set representing a geospatial index to fetch members matching a given maximum distance from a point */
    GEORADIUS("GEORADIUS"),

    /** GEORADIUS_RO - A read-only variant for GEORADIUS */
    GEORADIUS_RO("GEORADIUS_RO"),

    /** GEORADIUSBYMEMBER - Query a sorted set representing a geospatial index to fetch members matching a given maximum distance from a member */
    GEORADIUSBYMEMBER("GEORADIUSBYMEMBER"),

    /** GEORADIUSBYMEMBER_RO - A read-only variant for GEORADIUSBYMEMBER */
    GEORADIUSBYMEMBER_RO("GEORADIUSBYMEMBER_RO"),

    /** GEOSEARCH - Query a sorted set representing a geospatial index to fetch members inside an area of a box or a circle. */
    GEOSEARCH("GEOSEARCH"),

    /** GEOSEARCHSTORE - Query a sorted set representing a geospatial index to fetch members inside an area of a box or a circle, and store the result in another key. */
    GEOSEARCHSTORE("GEOSEARCHSTORE"),

    /** GET - Get the value of a key */
    GET("GET"),

    /** GETBIT - Returns the bit value at offset in the string value stored at key */
    GETBIT("GETBIT"),

    /** GETDEL - Get the value of a key and delete the key */
    GETDEL("GETDEL"),

    /** GETEX - Get the value of a key and optionally set its expiration */
    GETEX("GETEX"),

    /** GETRANGE - Get a substring of the string stored at a key */
    GETRANGE("GETRANGE"),

    /** GETSET - Set the string value of a key and return its old value */
    GETSET("GETSET"),

    /** GRAPH.CONFIG GET - Retrieves a RedisGraph configuration */
    GRAPH_CONFIG_GET("GRAPH.CONFIG GET"),

    /** GRAPH.CONFIG SET - Updates a RedisGraph configuration */
    GRAPH_CONFIG_SET("GRAPH.CONFIG SET"),

    /** GRAPH.DELETE - Completely removes the graph and all of its entities */
    GRAPH_DELETE("GRAPH.DELETE"),

    /** GRAPH.EXPLAIN - Returns a query execution plan without running the query */
    GRAPH_EXPLAIN("GRAPH.EXPLAIN"),

    /** GRAPH.LIST - Lists all graph keys in the keyspace */
    GRAPH_LIST("GRAPH.LIST"),

    /** GRAPH.PROFILE - Executes a query and returns an execution plan augmented with metrics for each operation's execution */
    GRAPH_PROFILE("GRAPH.PROFILE"),

    /** GRAPH.QUERY - Executes the given query against a specified graph */
    GRAPH_QUERY("GRAPH.QUERY"),

    /** GRAPH.RO_QUERY - Executes a given read only query against a specified graph */
    GRAPH_RO_QUERY("GRAPH.RO_QUERY"),

    /** GRAPH.SLOWLOG - Returns a list containing up to 10 of the slowest queries issued against the given graph */
    GRAPH_SLOWLOG("GRAPH.SLOWLOG"),

    /** HDEL - Delete one or more hash fields */
    HDEL("HDEL"),

    /** HELLO - Handshake with Redis */
    HELLO("HELLO"),

    /** HEXISTS - Determine if a hash field exists */
    HEXISTS("HEXISTS"),

    /** HGET - Get the value of a hash field */
    HGET("HGET"),

    /** HGETALL - Get all the fields and values in a hash */
    HGETALL("HGETALL"),

    /** HINCRBY - Increment the integer value of a hash field by the given number */
    HINCRBY("HINCRBY"),

    /** HINCRBYFLOAT - Increment the float value of a hash field by the given amount */
    HINCRBYFLOAT("HINCRBYFLOAT"),

    /** HKEYS - Get all the fields in a hash */
    HKEYS("HKEYS"),

    /** HLEN - Get the number of fields in a hash */
    HLEN("HLEN"),

    /** HMGET - Get the values of all the given hash fields */
    HMGET("HMGET"),

    /** HMSET - Set multiple hash fields to multiple values */
    HMSET("HMSET"),

    /** HRANDFIELD - Get one or multiple random fields from a hash */
    HRANDFIELD("HRANDFIELD"),

    /** HSCAN - Incrementally iterate hash fields and associated values */
    HSCAN("HSCAN"),

    /** HSET - Set the string value of a hash field */
    HSET("HSET"),

    /** HSETNX - Set the value of a hash field, only if the field does not exist */
    HSETNX("HSETNX"),

    /** HSTRLEN - Get the length of the value of a hash field */
    HSTRLEN("HSTRLEN"),

    /** HVALS - Get all the values in a hash */
    HVALS("HVALS"),

    /** INCR - Increment the integer value of a key by one */
    INCR("INCR"),

    /** INCRBY - Increment the integer value of a key by the given amount */
    INCRBY("INCRBY"),

    /** INCRBYFLOAT - Increment the float value of a key by the given amount */
    INCRBYFLOAT("INCRBYFLOAT"),

    /** INFO - Get information and statistics about the server */
    INFO("INFO"),

    /** JSON.ARRAPPEND - Append one or more json values into the array at path after the last element in it. */
    JSON_ARRAPPEND("JSON.ARRAPPEND"),

    /** JSON.ARRINDEX - Returns the index of the first occurrence of a JSON scalar value in the array at path */
    JSON_ARRINDEX("JSON.ARRINDEX"),

    /** JSON.ARRINSERT - Inserts the JSON_scalar(s) value at the specified index in the array at path */
    JSON_ARRINSERT("JSON.ARRINSERT"),

    /** JSON.ARRLEN - Returns the length of the array at path */
    JSON_ARRLEN("JSON.ARRLEN"),

    /** JSON.ARRPOP - Removes and returns the element at the specified index in the array at path */
    JSON_ARRPOP("JSON.ARRPOP"),

    /** JSON.ARRTRIM - Trims the array at path to contain only the specified inclusive range of indices from start to stop */
    JSON_ARRTRIM("JSON.ARRTRIM"),

    /** JSON.CLEAR - Clears all values from an array or an object and sets numeric values to '0' */
    JSON_CLEAR("JSON.CLEAR"),

    /** JSON.DEBUG - Debugging container command */
    JSON_DEBUG("JSON.DEBUG"),

    /** JSON.DEBUG MEMORY - Reports the size in bytes of a key */
    JSON_DEBUG_MEMORY("JSON.DEBUG MEMORY"),

    /** JSON.DEL - Deletes a value */
    JSON_DEL("JSON.DEL"),

    /** JSON.FORGET - Deletes a value */
    JSON_FORGET("JSON.FORGET"),

    /** JSON.GET - Gets the value at one or more paths in JSON serialized form */
    JSON_GET("JSON.GET"),

    /** JSON.MGET - Returns the values at a path from one or more keys */
    JSON_MGET("JSON.MGET"),

    /** JSON.NUMINCRBY - Increments the numeric value at path by a value */
    JSON_NUMINCRBY("JSON.NUMINCRBY"),

    /** JSON.NUMMULTBY - Multiplies the numeric value at path by a value */
    JSON_NUMMULTBY("JSON.NUMMULTBY"),

    /** JSON.OBJKEYS - Returns the JSON keys of the object at path */
    JSON_OBJKEYS("JSON.OBJKEYS"),

    /** JSON.OBJLEN - Returns the number of keys of the object at path */
    JSON_OBJLEN("JSON.OBJLEN"),

    /** JSON.RESP - Returns the JSON value at path in Redis Serialization Protocol (RESP) */
    JSON_RESP("JSON.RESP"),

    /** JSON.SET - Sets or updates the JSON value at a path */
    JSON_SET("JSON.SET"),

    /** JSON.STRAPPEND - Appends a string to a JSON string value at path */
    JSON_STRAPPEND("JSON.STRAPPEND"),

    /** JSON.STRLEN - Returns the length of the JSON String at path in key */
    JSON_STRLEN("JSON.STRLEN"),

    /** JSON.TOGGLE - Toggles a boolean value */
    JSON_TOGGLE("JSON.TOGGLE"),

    /** JSON.TYPE - Returns the type of the JSON value at path */
    JSON_TYPE("JSON.TYPE"),

    /** KEYS - Find all keys matching the given pattern */
    KEYS("KEYS"),

    /** LASTSAVE - Get the UNIX time stamp of the last successful save to disk */
    LASTSAVE("LASTSAVE"),

    /** LATENCY DOCTOR - Return a human readable latency analysis report. */
    LATENCY_DOCTOR("LATENCY DOCTOR"),

    /** LATENCY GRAPH - Return a latency graph for the event. */
    LATENCY_GRAPH("LATENCY GRAPH"),

    /** LATENCY HISTOGRAM - Return the cumulative distribution of latencies of a subset of commands or all. */
    LATENCY_HISTOGRAM("LATENCY HISTOGRAM"),

    /** LATENCY HISTORY - Return timestamp-latency samples for the event. */
    LATENCY_HISTORY("LATENCY HISTORY"),

    /** LATENCY LATEST - Return the latest latency samples for all events. */
    LATENCY_LATEST("LATENCY LATEST"),

    /** LATENCY RESET - Reset latency data for one or more events. */
    LATENCY_RESET("LATENCY RESET"),

    /** LCS - Find longest common substring */
    LCS("LCS"),

    /** LINDEX - Get an element from a list by its index */
    LINDEX("LINDEX"),

    /** LINSERT - Insert an element before or after another element in a list */
    LINSERT("LINSERT"),

    /** LLEN - Get the length of a list */
    LLEN("LLEN"),

    /** LMOVE - Pop an element from a list, push it to another list and return it */
    LMOVE("LMOVE"),

    /** LMPOP - Pop elements from a list */
    LMPOP("LMPOP"),

    /** LOLWUT - Display some computer art and the Redis version */
    LOLWUT("LOLWUT"),

    /** LPOP - Remove and get the first elements in a list */
    LPOP("LPOP"),

    /** LPOS - Return the index of matching elements on a list */
    LPOS("LPOS"),

    /** LPUSH - Prepend one or multiple elements to a list */
    LPUSH("LPUSH"),

    /** LPUSHX - Prepend an element to a list, only if the list exists */
    LPUSHX("LPUSHX"),

    /** LRANGE - Get a range of elements from a list */
    LRANGE("LRANGE"),

    /** LREM - Remove elements from a list */
    LREM("LREM"),

    /** LSET - Set the value of an element in a list by its index */
    LSET("LSET"),

    /** LTRIM - Trim a list to the specified range */
    LTRIM("LTRIM"),

    /** MEMORY DOCTOR - Outputs memory problems report */
    MEMORY_DOCTOR("MEMORY DOCTOR"),

    /** MEMORY MALLOC-STATS - Show allocator internal stats */
    MEMORY_MALLOC_STATS("MEMORY MALLOC-STATS"),

    /** MEMORY PURGE - Ask the allocator to release memory */
    MEMORY_PURGE("MEMORY PURGE"),

    /** MEMORY STATS - Show memory usage details */
    MEMORY_STATS("MEMORY STATS"),

    /** MEMORY USAGE - Estimate the memory usage of a key */
    MEMORY_USAGE("MEMORY USAGE"),

    /** MGET - Get the values of all the given keys */
    MGET("MGET"),

    /** MIGRATE - Atomically transfer a key from a Redis instance to another one. */
    MIGRATE("MIGRATE"),

    /** MODULE LIST - List all modules loaded by the server */
    MODULE_LIST("MODULE LIST"),

    /** MODULE LOAD - Load a module */
    MODULE_LOAD("MODULE LOAD"),

    /** MODULE LOADEX - Load a module with extended parameters */
    MODULE_LOADEX("MODULE LOADEX"),

    /** MODULE UNLOAD - Unload a module */
    MODULE_UNLOAD("MODULE UNLOAD"),

    /** MONITOR - Listen for all requests received by the server in real time */
    MONITOR("MONITOR"),

    /** MOVE - Move a key to another database */
    MOVE("MOVE"),

    /** MSET - Set multiple keys to multiple values */
    MSET("MSET"),

    /** MSETNX - Set multiple keys to multiple values, only if none of the keys exist */
    MSETNX("MSETNX"),

    /** MULTI - Mark the start of a transaction block */
    MULTI("MULTI"),

    /** OBJECT ENCODING - Inspect the internal encoding of a Redis object */
    OBJECT_ENCODING("OBJECT ENCODING"),

    /** OBJECT FREQ - Get the logarithmic access frequency counter of a Redis object */
    OBJECT_FREQ("OBJECT FREQ"),

    /** OBJECT IDLETIME - Get the time since a Redis object was last accessed */
    OBJECT_IDLETIME("OBJECT IDLETIME"),

    /** OBJECT REFCOUNT - Get the number of references to the value of the key */
    OBJECT_REFCOUNT("OBJECT REFCOUNT"),

    /** PERSIST - Remove the expiration from a key */
    PERSIST("PERSIST"),

    /** PEXPIRE - Set a key's time to live in milliseconds */
    PEXPIRE("PEXPIRE"),

    /** PEXPIREAT - Set the expiration for a key as a UNIX timestamp specified in milliseconds */
    PEXPIREAT("PEXPIREAT"),

    /** PEXPIRETIME - Get the expiration Unix timestamp for a key in milliseconds */
    PEXPIRETIME("PEXPIRETIME"),

    /** PFADD - Adds the specified elements to the specified HyperLogLog. */
    PFADD("PFADD"),

    /** PFCOUNT - Return the approximated cardinality of the_set(s) observed by the HyperLogLog at_key(s). */
    PFCOUNT("PFCOUNT"),

    /** PFDEBUG - Internal commands for debugging HyperLogLog values */
    PFDEBUG("PFDEBUG"),

    /** PFMERGE - Merge N different HyperLogLogs into a single one. */
    PFMERGE("PFMERGE"),

    /** PFSELFTEST - An internal command for testing HyperLogLog values */
    PFSELFTEST("PFSELFTEST"),

    /** PING - Ping the server */
    PING("PING"),

    /** PSETEX - Set the value and expiration in milliseconds of a key */
    PSETEX("PSETEX"),

    /** PSUBSCRIBE - Listen for messages published to channels matching the given patterns */
    PSUBSCRIBE("PSUBSCRIBE"),

    /** PSYNC - Internal command used for replication */
    PSYNC("PSYNC"),

    /** PTTL - Get the time to live for a key in milliseconds */
    PTTL("PTTL"),

    /** PUBLISH - Post a message to a channel */
    PUBLISH("PUBLISH"),

    /** PUBSUB CHANNELS - List active channels */
    PUBSUB_CHANNELS("PUBSUB CHANNELS"),

    /** PUBSUB NUMPAT - Get the count of unique patterns pattern subscriptions */
    PUBSUB_NUMPAT("PUBSUB NUMPAT"),

    /** PUBSUB NUMSUB - Get the count of subscribers for channels */
    PUBSUB_NUMSUB("PUBSUB NUMSUB"),

    /** PUBSUB SHARDCHANNELS - List active shard channels */
    PUBSUB_SHARDCHANNELS("PUBSUB SHARDCHANNELS"),

    /** PUBSUB SHARDNUMSUB - Get the count of subscribers for shard channels */
    PUBSUB_SHARDNUMSUB("PUBSUB SHARDNUMSUB"),

    /** PUNSUBSCRIBE - Stop listening for messages posted to channels matching the given patterns */
    PUNSUBSCRIBE("PUNSUBSCRIBE"),

    /** QUIT - Close the connection */
    QUIT("QUIT"),

    /** RANDOMKEY - Return a random key from the keyspace */
    RANDOMKEY("RANDOMKEY"),

    /** READONLY - Enables read queries for a connection to a cluster replica node */
    READONLY("READONLY"),

    /** READWRITE - Disables read queries for a connection to a cluster replica node */
    READWRITE("READWRITE"),

    /** RENAME - Rename a key */
    RENAME("RENAME"),

    /** RENAMENX - Rename a key, only if the new key does not exist */
    RENAMENX("RENAMENX"),

    /** REPLCONF - An internal command for configuring the replication stream */
    REPLCONF("REPLCONF"),

    /** REPLICAOF - Make the server a replica of another instance, or promote it as master. */
    REPLICAOF("REPLICAOF"),

    /** RESET - Reset the connection */
    RESET("RESET"),

    /** RESTORE - Create a key using the provided serialized value, previously obtained using DUMP. */
    RESTORE("RESTORE"),

    /** RESTORE-ASKING - An internal command for migrating keys in a cluster */
    RESTORE_ASKING("RESTORE-ASKING"),

    /** ROLE - Return the role of the instance in the context of replication */
    ROLE("ROLE"),

    /** RPOP - Remove and get the last elements in a list */
    RPOP("RPOP"),

    /** RPOPLPUSH - Remove the last element in a list, prepend it to another list and return it */
    RPOPLPUSH("RPOPLPUSH"),

    /** RPUSH - Append one or multiple elements to a list */
    RPUSH("RPUSH"),

    /** RPUSHX - Append an element to a list, only if the list exists */
    RPUSHX("RPUSHX"),

    /** SADD - Add one or more members to a set */
    SADD("SADD"),

    /** SAVE - Synchronously save the dataset to disk */
    SAVE("SAVE"),

    /** SCAN - Incrementally iterate the keys space */
    SCAN("SCAN"),

    /** SCARD - Get the number of members in a set */
    SCARD("SCARD"),

    /** SCRIPT DEBUG - Set the debug mode for executed scripts. */
    SCRIPT_DEBUG("SCRIPT DEBUG"),

    /** SCRIPT EXISTS - Check existence of scripts in the script cache. */
    SCRIPT_EXISTS("SCRIPT EXISTS"),

    /** SCRIPT FLUSH - Remove all the scripts from the script cache. */
    SCRIPT_FLUSH("SCRIPT FLUSH"),

    /** SCRIPT KILL - Kill the script currently in execution. */
    SCRIPT_KILL("SCRIPT KILL"),

    /** SCRIPT LOAD - Load the specified Lua script into the script cache. */
    SCRIPT_LOAD("SCRIPT LOAD"),

    /** SDIFF - Subtract multiple sets */
    SDIFF("SDIFF"),

    /** SDIFFSTORE - Subtract multiple sets and store the resulting set in a key */
    SDIFFSTORE("SDIFFSTORE"),

    /** SELECT - Change the selected database for the current connection */
    SELECT("SELECT"),

    /** SET - Set the string value of a key */
    SET("SET"),

    /** SETBIT - Sets or clears the bit at offset in the string value stored at key */
    SETBIT("SETBIT"),

    /** SETEX - Set the value and expiration of a key */
    SETEX("SETEX"),

    /** SETNX - Set the value of a key, only if the key does not exist */
    SETNX("SETNX"),

    /** SETRANGE - Overwrite part of a string at key starting at the specified offset */
    SETRANGE("SETRANGE"),

    /** SHUTDOWN - Synchronously save the dataset to disk and then shut down the server */
    SHUTDOWN("SHUTDOWN"),

    /** SINTER - Intersect multiple sets */
    SINTER("SINTER"),

    /** SINTERCARD - Intersect multiple sets and return the cardinality of the result */
    SINTERCARD("SINTERCARD"),

    /** SINTERSTORE - Intersect multiple sets and store the resulting set in a key */
    SINTERSTORE("SINTERSTORE"),

    /** SISMEMBER - Determine if a given value is a member of a set */
    SISMEMBER("SISMEMBER"),

    /** SLAVEOF - Make the server a replica of another instance, or promote it as master. */
    SLAVEOF("SLAVEOF"),

    /** SLOWLOG GET - Get the slow log's entries */
    SLOWLOG_GET("SLOWLOG GET"),

    /** SLOWLOG LEN - Get the slow log's length */
    SLOWLOG_LEN("SLOWLOG LEN"),

    /** SLOWLOG RESET - Clear all entries from the slow log */
    SLOWLOG_RESET("SLOWLOG RESET"),

    /** SMEMBERS - Get all the members in a set */
    SMEMBERS("SMEMBERS"),

    /** SMISMEMBER - Returns the membership associated with the given elements for a set */
    SMISMEMBER("SMISMEMBER"),

    /** SMOVE - Move a member from one set to another */
    SMOVE("SMOVE"),

    /** SORT - Sort the elements in a list, set or sorted set */
    SORT("SORT"),

    /** SORT_RO - Sort the elements in a list, set or sorted set. Read-only variant of SORT. */
    SORT_RO("SORT_RO"),

    /** SPOP - Remove and return one or multiple random members from a set */
    SPOP("SPOP"),

    /** SPUBLISH - Post a message to a shard channel */
    SPUBLISH("SPUBLISH"),

    /** SRANDMEMBER - Get one or multiple random members from a set */
    SRANDMEMBER("SRANDMEMBER"),

    /** SREM - Remove one or more members from a set */
    SREM("SREM"),

    /** SSCAN - Incrementally iterate Set elements */
    SSCAN("SSCAN"),

    /** SSUBSCRIBE - Listen for messages published to the given shard channels */
    SSUBSCRIBE("SSUBSCRIBE"),

    /** STRLEN - Get the length of the value stored in a key */
    STRLEN("STRLEN"),

    /** SUBSCRIBE - Listen for messages published to the given channels */
    SUBSCRIBE("SUBSCRIBE"),

    /** SUBSTR - Get a substring of the string stored at a key */
    SUBSTR("SUBSTR"),

    /** SUNION - Add multiple sets */
    SUNION("SUNION"),

    /** SUNIONSTORE - Add multiple sets and store the resulting set in a key */
    SUNIONSTORE("SUNIONSTORE"),

    /** SUNSUBSCRIBE - Stop listening for messages posted to the given shard channels */
    SUNSUBSCRIBE("SUNSUBSCRIBE"),

    /** SWAPDB - Swaps two Redis databases */
    SWAPDB("SWAPDB"),

    /** SYNC - Internal command used for replication */
    SYNC("SYNC"),

    /** TDIGEST.ADD - Adds one or more observations to a t-digest sketch */
    TDIGEST_ADD("TDIGEST.ADD"),

    /** TDIGEST.BYRANK - Returns, for each input rank, an estimation of the value (floating-point) with that rank */
    TDIGEST_BYRANK("TDIGEST.BYRANK"),

    /** TDIGEST.BYREVRANK - Returns, for each input reverse rank, an estimation of the value (floating-point) with that reverse rank */
    TDIGEST_BYREVRANK("TDIGEST.BYREVRANK"),

    /** TDIGEST.CDF - Returns, for each input value, an estimation of the fraction (floating-point) of (observations smaller than the given value + half the observations equal to the given value) */
    TDIGEST_CDF("TDIGEST.CDF"),

    /** TDIGEST.CREATE - Allocates memory and initializes a new t-digest sketch */
    TDIGEST_CREATE("TDIGEST.CREATE"),

    /** TDIGEST.INFO - Returns information and statistics about a t-digest sketch */
    TDIGEST_INFO("TDIGEST.INFO"),

    /** TDIGEST.MAX - Returns the maximum observation value from a t-digest sketch */
    TDIGEST_MAX("TDIGEST.MAX"),

    /** TDIGEST.MERGE - Merges multiple t-digest sketches into a single sketch */
    TDIGEST_MERGE("TDIGEST.MERGE"),

    /** TDIGEST.MIN - Returns the minimum observation value from a t-digest sketch */
    TDIGEST_MIN("TDIGEST.MIN"),

    /** TDIGEST.QUANTILE - Returns, for each input fraction, an estimation of the value (floating point) that is smaller than the given fraction of observations */
    TDIGEST_QUANTILE("TDIGEST.QUANTILE"),

    /** TDIGEST.RANK - Returns, for each input value (floating-point), the estimated rank of the value (the number of observations in the sketch that are smaller than the value + half the number of observations that are equal to the value) */
    TDIGEST_RANK("TDIGEST.RANK"),

    /** TDIGEST.RESET - Resets a t-digest sketch: empty the sketch and re-initializes it. */
    TDIGEST_RESET("TDIGEST.RESET"),

    /** TDIGEST.REVRANK - Returns, for each input value (floating-point), the estimated reverse rank of the value (the number of observations in the sketch that are larger than the value + half the number of observations that are equal to the value) */
    TDIGEST_REVRANK("TDIGEST.REVRANK"),

    /** TDIGEST.TRIMMED_MEAN - Returns an estimation of the mean value from the sketch, excluding observation values outside the low and high cutoff quantiles */
    TDIGEST_TRIMMED_MEAN("TDIGEST.TRIMMED_MEAN"),

    /** TIME - Return the current server time */
    TIME("TIME"),

    /** TOPK.ADD - Increases the count of one or more items by increment */
    TOPK_ADD("TOPK.ADD"),

    /** TOPK.COUNT - Return the count for one or more items are in a sketch */
    TOPK_COUNT("TOPK.COUNT"),

    /** TOPK.INCRBY - Increases the count of one or more items by increment */
    TOPK_INCRBY("TOPK.INCRBY"),

    /** TOPK.INFO - Returns information about a sketch */
    TOPK_INFO("TOPK.INFO"),

    /** TOPK.LIST - Return full list of items in Top K list */
    TOPK_LIST("TOPK.LIST"),

    /** TOPK.QUERY - Checks whether one or more items are in a sketch */
    TOPK_QUERY("TOPK.QUERY"),

    /** TOPK.RESERVE - Initializes a TopK with specified parameters */
    TOPK_RESERVE("TOPK.RESERVE"),

    /** TOUCH - Alters the last access time of a_key(s). Returns the number of existing keys specified. */
    TOUCH("TOUCH"),

    /** TS.ADD - Append a sample to a time series */
    TS_ADD("TS.ADD"),

    /** TS.ALTER - Update the retention, chunk size, duplicate policy, and labels of an existing time series */
    TS_ALTER("TS.ALTER"),

    /** TS.CREATE - Create a new time series */
    TS_CREATE("TS.CREATE"),

    /** TS.CREATERULE - Create a compaction rule */
    TS_CREATERULE("TS.CREATERULE"),

    /** TS.DECRBY - Decrease the value of the sample with the maximal existing time stamp, or create a new sample with a value equal to the value of the sample with the maximal existing timestamp with a given decrement */
    TS_DECRBY("TS.DECRBY"),

    /** TS.DEL - Delete all samples between two timestamps for a given time series */
    TS_DEL("TS.DEL"),

    /** TS.DELETERULE - Delete a compaction rule */
    TS_DELETERULE("TS.DELETERULE"),

    /** TS.GET - Get the last sample */
    TS_GET("TS.GET"),

    /** TS.INCRBY - Increase the value of the sample with the maximal existing time stamp, or create a new sample with a value equal to the value of the sample with the maximal existing timestamp with a given increment */
    TS_INCRBY("TS.INCRBY"),

    /** TS.INFO - Returns information and statistics for a time series */
    TS_INFO("TS.INFO"),

    /** TS.MADD - Append new samples to one or more time series */
    TS_MADD("TS.MADD"),

    /** TS.MGET - Get the last samples matching a specific filter */
    TS_MGET("TS.MGET"),

    /** TS.MRANGE - Query a range across multiple time series by filters in forward direction */
    TS_MRANGE("TS.MRANGE"),

    /** TS.MREVRANGE - Query a range across multiple time-series by filters in reverse direction */
    TS_MREVRANGE("TS.MREVRANGE"),

    /** TS.QUERYINDEX - Get all time series keys matching a filter list */
    TS_QUERYINDEX("TS.QUERYINDEX"),

    /** TS.RANGE - Query a range in forward direction */
    TS_RANGE("TS.RANGE"),

    /** TS.REVRANGE - Query a range in reverse direction */
    TS_REVRANGE("TS.REVRANGE"),

    /** TTL - Get the time to live for a key in seconds */
    TTL("TTL"),

    /** TYPE - Determine the type stored at key */
    TYPE("TYPE"),

    /** UNLINK - Delete a key asynchronously in another thread. Otherwise it is just as DEL, but non blocking. */
    UNLINK("UNLINK"),

    /** UNSUBSCRIBE - Stop listening for messages posted to the given channels */
    UNSUBSCRIBE("UNSUBSCRIBE"),

    /** UNWATCH - Forget about all watched keys */
    UNWATCH("UNWATCH"),

    /** WAIT - Wait for the synchronous replication of all the write commands sent in the context of the current connection */
    WAIT("WAIT"),

    /** WATCH - Watch the given keys to determine execution of the MULTI/EXEC block */
    WATCH("WATCH"),

    /**
     * XACK - Marks a pending message as correctly processed, effectively removing it from the pending entries list of the consumer group. Return value of the command is the number of messages successfully acknowledged, that is, the IDs we were
     * actually able to resolve in the PEL.
     */
    XACK("XACK"),

    /** XADD - Appends a new entry to a stream */
    XADD("XADD"),

    /** XAUTOCLAIM - Changes (or acquires) ownership of messages in a consumer group, as if the messages were delivered to the specified consumer. */
    XAUTOCLAIM("XAUTOCLAIM"),

    /** XCLAIM - Changes (or acquires) ownership of a message in a consumer group, as if the message was delivered to the specified consumer. */
    XCLAIM("XCLAIM"),

    /** XDEL - Removes the specified entries from the stream. Returns the number of items actually deleted, that may be different from the number of IDs passed in case certain IDs do not exist. */
    XDEL("XDEL"),

    /** XGROUP CREATE - Create a consumer group. */
    XGROUP_CREATE("XGROUP CREATE"),

    /** XGROUP CREATECONSUMER - Create a consumer in a consumer group. */
    XGROUP_CREATECONSUMER("XGROUP CREATECONSUMER"),

    /** XGROUP DELCONSUMER - Delete a consumer from a consumer group. */
    XGROUP_DELCONSUMER("XGROUP DELCONSUMER"),

    /** XGROUP DESTROY - Destroy a consumer group. */
    XGROUP_DESTROY("XGROUP DESTROY"),

    /** XGROUP SETID - Set a consumer group to an arbitrary last delivered ID value. */
    XGROUP_SETID("XGROUP SETID"),

    /** XINFO CONSUMERS - List the consumers in a consumer group */
    XINFO_CONSUMERS("XINFO CONSUMERS"),

    /** XINFO GROUPS - List the consumer groups of a stream */
    XINFO_GROUPS("XINFO GROUPS"),

    /** XINFO STREAM - Get information about a stream */
    XINFO_STREAM("XINFO STREAM"),

    /** XLEN - Return the number of entries in a stream */
    XLEN("XLEN"),

    /** XPENDING - Return information and entries from a stream consumer group pending entries list, that are messages fetched but never acknowledged. */
    XPENDING("XPENDING"),

    /** XRANGE - Return a range of elements in a stream, with IDs matching the specified IDs interval */
    XRANGE("XRANGE"),

    /** XREAD - Return never seen elements in multiple streams, with IDs greater than the ones reported by the caller for each stream. Can block. */
    XREAD("XREAD"),

    /** XREADGROUP - Return new entries from a stream using a consumer group, or access the history of the pending entries for a given consumer. Can block. */
    XREADGROUP("XREADGROUP"),

    /** XREVRANGE - Return a range of elements in a stream, with IDs matching the specified IDs interval, in reverse order (from greater to smaller IDs) compared to XRANGE */
    XREVRANGE("XREVRANGE"),

    /** XSETID - An internal command for replicating stream values */
    XSETID("XSETID"),

    /** XTRIM - Trims the stream to (approximately if '~' is passed) a certain size */
    XTRIM("XTRIM"),

    /** ZADD - Add one or more members to a sorted set, or update its score if it already exists */
    ZADD("ZADD"),

    /** ZCARD - Get the number of members in a sorted set */
    ZCARD("ZCARD"),

    /** ZCOUNT - Count the members in a sorted set with scores within the given values */
    ZCOUNT("ZCOUNT"),

    /** ZDIFF - Subtract multiple sorted sets */
    ZDIFF("ZDIFF"),

    /** ZDIFFSTORE - Subtract multiple sorted sets and store the resulting sorted set in a new key */
    ZDIFFSTORE("ZDIFFSTORE"),

    /** ZINCRBY - Increment the score of a member in a sorted set */
    ZINCRBY("ZINCRBY"),

    /** ZINTER - Intersect multiple sorted sets */
    ZINTER("ZINTER"),

    /** ZINTERCARD - Intersect multiple sorted sets and return the cardinality of the result */
    ZINTERCARD("ZINTERCARD"),

    /** ZINTERSTORE - Intersect multiple sorted sets and store the resulting sorted set in a new key */
    ZINTERSTORE("ZINTERSTORE"),

    /** ZLEXCOUNT - Count the number of members in a sorted set between a given lexicographical range */
    ZLEXCOUNT("ZLEXCOUNT"),

    /** ZMPOP - Remove and return members with scores in a sorted set */
    ZMPOP("ZMPOP"),

    /** ZMSCORE - Get the score associated with the given members in a sorted set */
    ZMSCORE("ZMSCORE"),

    /** ZPOPMAX - Remove and return members with the highest scores in a sorted set */
    ZPOPMAX("ZPOPMAX"),

    /** ZPOPMIN - Remove and return members with the lowest scores in a sorted set */
    ZPOPMIN("ZPOPMIN"),

    /** ZRANDMEMBER - Get one or multiple random elements from a sorted set */
    ZRANDMEMBER("ZRANDMEMBER"),

    /** ZRANGE - Return a range of members in a sorted set */
    ZRANGE("ZRANGE"),

    /** ZRANGEBYLEX - Return a range of members in a sorted set, by lexicographical range */
    ZRANGEBYLEX("ZRANGEBYLEX"),

    /** ZRANGEBYSCORE - Return a range of members in a sorted set, by score */
    ZRANGEBYSCORE("ZRANGEBYSCORE"),

    /** ZRANGESTORE - Store a range of members from sorted set into another key */
    ZRANGESTORE("ZRANGESTORE"),

    /** ZRANK - Determine the index of a member in a sorted set */
    ZRANK("ZRANK"),

    /** ZREM - Remove one or more members from a sorted set */
    ZREM("ZREM"),

    /** ZREMRANGEBYLEX - Remove all members in a sorted set between the given lexicographical range */
    ZREMRANGEBYLEX("ZREMRANGEBYLEX"),

    /** ZREMRANGEBYRANK - Remove all members in a sorted set within the given indexes */
    ZREMRANGEBYRANK("ZREMRANGEBYRANK"),

    /** ZREMRANGEBYSCORE - Remove all members in a sorted set within the given scores */
    ZREMRANGEBYSCORE("ZREMRANGEBYSCORE"),

    /** ZREVRANGE - Return a range of members in a sorted set, by index, with scores ordered from high to low */
    ZREVRANGE("ZREVRANGE"),

    /** ZREVRANGEBYLEX - Return a range of members in a sorted set, by lexicographical range, ordered from higher to lower strings. */
    ZREVRANGEBYLEX("ZREVRANGEBYLEX"),

    /** ZREVRANGEBYSCORE - Return a range of members in a sorted set, by score, with scores ordered from high to low */
    ZREVRANGEBYSCORE("ZREVRANGEBYSCORE"),

    /** ZREVRANK - Determine the index of a member in a sorted set, with scores ordered from high to low */
    ZREVRANK("ZREVRANK"),

    /** ZSCAN - Incrementally iterate sorted sets elements and associated scores */
    ZSCAN("ZSCAN"),

    /** ZSCORE - Get the score associated with the given member in a sorted set */
    ZSCORE("ZSCORE"),

    /** ZUNION - Add multiple sorted sets */
    ZUNION("ZUNION"),

    /** ZUNIONSTORE - Add multiple sorted sets and store the resulting sorted set in a new key */
    ZUNIONSTORE("ZUNIONSTORE"),
    ;

    private final String command;

    private RedisCommand(String command) {
        this.command = command;
    }

    /**
     * Gets the command.
     *
     * @return The command
     */
    public String getCommand() {
        return command;
    }

}
