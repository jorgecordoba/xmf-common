package com.devfields.xmf.common.configuration.impl.impl

import com.devfields.xmf.common.configuration.configuration.*
import com.devfields.xmf.common.configuration.impl.LocalConfigStore
import com.devfields.xmf.common.logging.XmfLoggerFactory
import com.devfields.xmf.common.types.Version
import com.mongodb.*
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.joda.time.DateTime

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import java.util.concurrent.*
import java.util.regex.Pattern

/**
 * Created by jco27 on 19/02/2017.
 */
class MongoDbStore : ObservableConfigurationStore {

    private var mongoClient: MongoClient? = null
    private var mongoDatabase: MongoDatabase? = null
    private val logger = XmfLoggerFactory.getLogger(this.javaClass)

    private val HOSTS_KEY = "database.mongo.hosts"
    private val USER_KEY = "database.mongo.user"
    private val PASSWORD_KEY = "database.mongo.password"
    private val ADMINDB_KEY = "database.mongo.admin-database"
    private val WATCH_DELAY_INTERVAL_KEY = "database.mongo.watch-delay"
    private val WATCH_DELAY_INTERVAL_DEFAULT = 30
    private val ADMINDB_DEFAULT = "admin"
    private val DB_NAME = "xmf-configuration"
    private val syncrhonizer = Any()
    private val observersMap = HashMap<String, ValueObserversPair>()
    private val localConfigStore = LocalConfigStore("conf")
    private val localDocuments = ConcurrentHashMap<String, Document>()
    private var executor: ScheduledExecutorService? = null
    private val serviceName: String
    private val instanceName: String

    override val local: ConfigurationStore
        get() = localConfigStore

    private fun getSeeds(hostString: String): List<ServerAddress> {
        val seeds = mutableListOf<ServerAddress>()
        val hosts = hostString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (host in hosts) {
            try {
                val uri = URI("my://$host")
                seeds.add(ServerAddress(uri.host, uri.port))
            } catch (ex: URISyntaxException) {
                // TODO: Log and continue
            }

        }
        return seeds
    }

    private fun initialize(adminDatabase: String, hosts: String, user: String, password: String) {

        logger.xmf_debug_log(this.serviceName, this.instanceName, "Initializing database instance to:" +
                "\n  HOSTS: key " + hosts +
                "\n  USER: key " + user)


        val credentials = mutableListOf<MongoCredential>()
        credentials.add(
                MongoCredential.createScramSha1Credential(
                        user,
                        adminDatabase,
                        password.toCharArray()
                )
        )

        val seeds = getSeeds(hosts)

        if (seeds.isEmpty()) {
            throw ConfigurationException("Invalid configuration. Unable to find valid host entries for the database")
        }

        try {
            this.mongoClient = MongoClient(seeds, credentials)
            this.mongoDatabase = mongoClient!!.getDatabase(DB_NAME)
            logger.xmf_debug_log(this.serviceName, this.instanceName, "Database configuration initialised")
        } catch (exception: MongoException) {
            throw ConfigurationException("Unable to connect to the database", exception)
        }

    }

    private fun refreshLocalCopy() {

        logger.xmf_trace_log(this.serviceName, this.instanceName, "Refreshing local properties")

        val collections = mongoDatabase!!.listCollectionNames()
        for (collection in collections) {
            /* Calling get document will put the latest version in the localDocuments
               automatically, it will also invoke the change handler for observers */
            getDocument(collection)
        }
    }

    private fun getDocument(docName: String): Document? {
        var doc: Document? = null
        try {
            doc = mongoDatabase!!.getCollection(docName).find().first()
            if (doc != null)
                localDocuments[docName] = doc
            notifyWatchers(docName, doc)
            return doc
        } catch (ex: MongoException) {
            // TODO: Log the exception at the trace level

        }

        return if (doc == null && localDocuments.containsKey(docName)) {
            localDocuments[docName]
        } else null

    }

    override fun readAsInteger(key: String, defaultValue: Int?): Int? {
        val `val` = readValue(key) ?: return defaultValue

        try {
            return Integer.valueOf(`val`)
        } catch (ex: NumberFormatException) {
            return defaultValue
        }

    }

    private fun notifyWatchers(collection: String?, doc: Document?) {
        logger.xmf_trace_log(this.serviceName, this.instanceName, "Running notification of config change to watchers")
        if (observersMap.size == 0)
            return

        val executor = Executors.newCachedThreadPool()
        /* We need to notify all watchers on a thread (one per watcher) coming
           from the thread pool so that different watchers execute simultaneously
         */
        for (key in observersMap.keys) {
            val groupName = getPropertyGroupName(key)
            val keyName = getPropertyName(key)

            if (groupName == collection) {
                val currentValue: String?
                if (doc != null)
                    currentValue = doc.getString(keyName)
                else
                    currentValue = null

                val observerList = observersMap[key]!!

                if (valueHasChanged(currentValue, observerList)) {
                    observerList.value = currentValue
                    for (handler in observerList.handlers)
                        executor.execute {
                            try {
                                logger.xmf_trace_log("MongoDB", "Notifiying of change in $key to $currentValue")
                                handler(key, observerList.value, currentValue!!)
                            } catch (e: Exception) {
                                // TODO: Log the error but ignore
                                logger.xmf_warn_log(serviceName, instanceName, "Error notifying watcher with key: $key")
                            }
                        }
                }
            }
        }
    }

    private fun valueHasChanged(currentValue: String?, observerList: ValueObserversPair?): Boolean {
        if (observerList != null) {

            if (observerList.value != null && observerList.value != currentValue)
                return true
            else if (currentValue != null && currentValue != observerList.value)
                return true
        }
        return false
    }

    private fun getLocalPropertyName(key: String): String? {
        val index = key.indexOf('.')
        return if (-1 == index) null else key.substring(index + 1, key.length)

    }

    constructor(serviceName: String = "MongoDB", instanceName: String = "Configuration") {
        this.serviceName = serviceName
        this.instanceName = instanceName

        val hosts = localConfigStore.readValue(HOSTS_KEY)
        val user = localConfigStore.readValue(USER_KEY)
        val adminDatabase = localConfigStore.readValue(ADMINDB_KEY, ADMINDB_DEFAULT)
        val password = localConfigStore.readValue(PASSWORD_KEY)

        if (hosts == null)
            throw ConfigurationException("Unable to get the hosts configuration string on $HOSTS_KEY")
        if (user == null)
            throw ConfigurationException("Unable to get the user name for the connection on $USER_KEY")
        if (password == null)
            throw ConfigurationException("Unable to get the password for the conneciton on $PASSWORD_KEY")

        initialize(adminDatabase, hosts, user, password)
    }

    constructor(configfile: String) : this("MongoDB", "Configuration", configfile) {}

    constructor(serviceName: String, instanceName: String, configFile: String) {
        this.serviceName = serviceName
        this.instanceName = instanceName
        val properties = Properties()
        try {
            this.javaClass.getResourceAsStream(configFile).use { stream -> properties.load(stream) }
        } catch (ex: IOException) {
            throw ConfigurationException("Unable to load properties from specified file", ex)
        }

        val hosts = properties.getProperty(getLocalPropertyName(HOSTS_KEY))
        val adminDatabase = properties.getProperty(getLocalPropertyName(ADMINDB_KEY), ADMINDB_DEFAULT)
        val user = properties.getProperty(getLocalPropertyName(USER_KEY))
        val password = properties.getProperty(getLocalPropertyName(PASSWORD_KEY))

        initialize(adminDatabase, hosts, user, password)
    }

    constructor(adminDatabase: String, hosts: String, user: String, password: String) : this("MongoDB", "Configuration", adminDatabase, hosts, user, password) {}

    constructor(serviceName: String, instanceName: String, adminDatabase: String, hosts: String, user: String, password: String) {
        this.serviceName = serviceName
        this.instanceName = instanceName
        initialize(adminDatabase, hosts, user, password)
    }

    private fun getPropertyName(key: String): String? {
        val index = key.indexOf('.')
        return if (-1 == index) null else key.substring(index + 1, key.length).replace('.', '_')

    }

    private fun getPropertyGroupName(key: String): String? {
        val index = key.indexOf('.')
        return if (-1 == index) null else key.substring(0, index)

    }

    override fun readValue(key: String): String? {
        val entry = read(key)
        return entry?.value
    }

    override fun readValue(key: String, defaultValue: String): String {
        val `val` = readValue(key)
        return `val` ?: defaultValue
    }

    private fun constructPattern(keyPattern: String): Pattern {
        val tokens = keyPattern.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val bld = StringBuilder()
        for (i in 1 until tokens.size) {
            if (tokens[i] == "*")
                bld.append("(.+)")
            else
                bld.append(tokens[i])

            if (i < tokens.size - 1) {
                bld.append("\\.")
            }
        }
        return Pattern.compile(bld.toString())
    }

    override fun readValues(keyPattern: String): List<ConfigurationEntry> {
        val result = ArrayList<ConfigurationEntry>()
        val docName = getPropertyGroupName(keyPattern) ?: return result
        val doc = getDocument(docName) ?: return result

        val ptn = constructPattern(keyPattern)
        for (key in doc.keys) {
            if (ptn.matcher(key).find()) {
                val entry = getConfigurationEntry(key, doc)
                if (entry != null)
                    result.add(entry)
            }
        }

        return result
    }

    override fun read(key: String): ConfigurationEntry? {
        val docName = getPropertyGroupName(key) ?: return null
        val doc = getDocument(docName) ?: return null

        return getConfigurationEntry(key, doc)
    }

    private fun getConfigurationEntry(key: String, doc: Document): ConfigurationEntry? {
        try {
            val value = doc.getString(getPropertyName(key))
            val version: Version
            val modifiedDate: DateTime
            if (doc.containsKey("_version"))
                version = doc.get("_version", Version::class.java)
            else
                version = Version(1, 0)

            if (doc.containsKey("_modifiedDate"))
                modifiedDate = doc.get("_modifiedDate", DateTime::class.java)
            else
                modifiedDate = DateTime.now()

            return ConfigurationEntry(key, value, modifiedDate, version)
        } catch (ex: ClassCastException) {
            // TODO: Log the error
            return null
        }

    }

    override fun watch(key: String, handler: ConfigChangeHandler) {
        // Get the most up to date value of the key, if the key does not exist use null
        if (!observersMap.containsKey(key))
            observersMap[key] = ValueObserversPair(readValue(key))

        val observers = observersMap[key]!!
        observers.handlers.add(handler)
        logger.xmf_debug_log(this.serviceName, this.instanceName, "Registered new watcher for key " + key + ". Total: " + observers.handlers.size)

        synchronized(syncrhonizer) {
            if (executor == null) {
                executor = Executors.newSingleThreadScheduledExecutor()
                executor!!.scheduleWithFixedDelay({ refreshLocalCopy() }, 0, this.readAsInteger(WATCH_DELAY_INTERVAL_KEY, WATCH_DELAY_INTERVAL_DEFAULT)!!.toLong(), TimeUnit.SECONDS)
            }
        }
    }

    override fun unwatch(key: String, handler: ConfigChangeHandler) {
        if (!observersMap.containsKey(key))
            return

        val observers = observersMap[key]!!
        observers.handlers.remove(handler)
    }
}
