/*
 This file is part of Cyclos.

 Cyclos is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 Cyclos is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Cyclos; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 */
package nl.strohalm.cyclos.setup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import nl.strohalm.cyclos.utils.JDBCWrapper;
import nl.strohalm.cyclos.utils.XmlHelper;
import nl.strohalm.cyclos.utils.conversion.LocaleConverter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.DatasourceConnectionProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class used to manage database configuration, validate the connection, generate the database when in embedded mode and apply automatic schema
 * updates.
 * @author luis
 */
public class DataBaseConfiguration {
    public static boolean       SKIP = false;
    private static final Log    LOG  = LogFactory.getLog(DataBaseConfiguration.class);
    private final Configuration configuration;
    private SessionFactory      sessionFactory;

    public DataBaseConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void run() {
        final Properties properties = configuration.getProperties();

        // Define some fixed Cyclos properties for Hibernate
        initCommonProperties(properties);

        // Check if all tests must be skipped
        final String skipTestsProperty = StringUtils.trimToNull(properties.getProperty("cyclos.database.skipTests"));
        final boolean skipTests = SKIP || skipTestsProperty == null || Boolean.valueOf(skipTestsProperty);
        if (skipTests) {
            if (!SKIP && skipTestsProperty == null) {
                LOG.info("Skipping Cyclos database check, as cyclos.properties doesn't set cyclos.database.skipTests=false");
            }
            return;
        }

        // Retrieve the connection
        final String dataSource = StringUtils.trimToNull(properties.getProperty(Environment.DATASOURCE));
        Connection connection;
        String connectionLocation;
        if (dataSource != null) {
            // Use Hibernate's own DatasourceConnectionProvider when using a JNDI datasource
            final ConnectionProvider provider = new DatasourceConnectionProvider();
            provider.configure(properties);
            try {
                connection = provider.getConnection();
            } catch (final SQLException e) {
                final String msg = "Error connecting to datasource at " + dataSource;
                LOG.error(msg);
                throw new RuntimeException(msg, e);
            }
            connectionLocation = dataSource;
        } else {
            // Manually verify the connection
            final String driverClass = StringUtils.trimToNull(properties.getProperty(Environment.DRIVER));
            validateDriver(driverClass);
            final String url = StringUtils.trimToNull(properties.getProperty(Environment.URL));
            final String username = StringUtils.trimToNull(properties.getProperty(Environment.USER));
            final String password = StringUtils.trimToNull(properties.getProperty(Environment.PASS));
            connection = validateConnection(url, username, password);
            connectionLocation = url;
        }

        final JDBCWrapper jdbc = new JDBCWrapper(connection);

        // Check whether we will create the database if necessary
        final boolean embedded = Boolean.valueOf(properties.getProperty("cyclos.embedded.enable", "false"));

        // Get the connection meta data
        boolean dataBaseExists = true;
        String dataBaseName;
        String dataBaseVersion;
        try {
            final DatabaseMetaData metaData = connection.getMetaData();
            dataBaseName = metaData.getDatabaseProductName();
            dataBaseVersion = metaData.getDatabaseProductVersion();
        } catch (final SQLException e) {
            throw new RuntimeException("Error reading database metadata", e);
        }

        // Check if the database exists, by reading the current version
        String currentVersion;
        try {
            currentVersion = readCurrentVersion(jdbc);
            if (currentVersion != null) {
                LOG.info(String.format("Cyclos database version %s found on %s version %s", currentVersion, dataBaseName, dataBaseVersion));
            }
            dataBaseExists = currentVersion != null;
            // Check for new versions schema upgrade
            if (dataBaseExists) {
                final boolean autoUpgrade = Boolean.valueOf(properties.getProperty("cyclos.autoSchemaUpgrade.enable", "false"));
                if (autoUpgrade) {
                    final String newVersion = upgradeSchema(currentVersion, jdbc);
                    if (!currentVersion.equals(newVersion)) {
                        // The version has changed. Add a custom property on the configuration properties for the cache
                        // manager to clear the current cache, because new entities may be incompatible with cached versions
                        properties.setProperty("cyclos.versionHasChanged", "true");
                    }
                }
            } else {
                // For MySQL connections, we should ensure that the database is set to utf8
                if (embedded && dataBaseName.toLowerCase().equals("mysql")) {
                    try {
                        jdbc.commit();
                        jdbc.execute("alter database character set utf8");
                    } catch (final SQLException e) {
                        e.printStackTrace();
                        // Ignore
                    }
                }
            }
        } finally {
            // Close the connection
            try {
                connection.close();
            } catch (final SQLException e) {
            }
        }

        // Run the setup if needed
        if (!dataBaseExists) {
            if (embedded) {
                LOG.info("Database is empty. Running setup to populate it");
                sessionFactory = configuration.buildSessionFactory();
                final Locale locale = LocaleConverter.instance().valueOf(properties.getProperty("cyclos.embedded.locale", "en_US"));
                final Setup setup = new Setup(configuration, sessionFactory);
                setup.setLocale(locale);
                setup.setCreateDataBase(true);
                setup.setCreateBasicData(true);
                setup.setCreateInitialData(true);
                setup.setForce(true);
                setup.execute();
            } else {
                throw new RuntimeException("Cyclos database not found at " + connectionLocation);
            }
        }
    }

    private File getCacheDir() {
        final File classes = FileUtils.toFile(getClass().getResource("/")); // WEB-INF/classes
        final File webInf = classes.getParentFile(); // WEB-INF
        if (webInf.exists() && webInf.canWrite()) {
            // We have write access - Enable disk cache
            final File cacheDir = new File(webInf, "cache"); // WEB-INF/cache
            cacheDir.mkdirs();
            return cacheDir;
        } else {
            return null;
        }
    }

    /**
     * Initialize some common properties for Hibernate
     */
    private void initCommonProperties(final Properties properties) {
        // The transaction isolation level
        if (StringUtils.isEmpty(properties.getProperty("hibernate.connection.isolation"))) {
            properties.setProperty("hibernate.connection.isolation", "" + Connection.TRANSACTION_READ_COMMITTED);
        }

        final boolean secondLevelCacheEnabled = Boolean.parseBoolean(properties.getProperty("hibernate.cache.use_second_level_cache", "false"));
        if (secondLevelCacheEnabled) {
            // The second level cache provider
            if (StringUtils.isEmpty(properties.getProperty("hibernate.cache.region.factory_class"))) {
                properties.setProperty("hibernate.cache.region.factory_class", "net.sf.ehcache.hibernate.EhCacheRegionFactory");
            }
            // Manually set the disk store to WEB-INF/cache
            final String ehcacheConfig = properties.getProperty("net.sf.ehcache.configurationResourceName");
            if (StringUtils.isEmpty(ehcacheConfig)) {
                final InputStream in = getClass().getResourceAsStream("/cyclos-ehcache.xml");
                if (in != null) {
                    final Document config = XmlHelper.readDocument(in);
                    final List<Element> elements = XmlHelper.getChilden(config.getDocumentElement(), "diskStore");
                    Element diskStore = CollectionUtils.isEmpty(elements) ? null : elements.iterator().next();
                    if (diskStore == null) {
                        diskStore = config.createElement("diskStore");
                        config.getDocumentElement().appendChild(diskStore);
                    }
                    XmlHelper.clear(diskStore);
                    final File cacheDir = getCacheDir();
                    String cachePath;
                    if (cacheDir == null) {
                        cachePath = "java.io.tmpdir";
                    } else {
                        if (Boolean.parseBoolean(properties.getProperty("cyclos.versionHasChanged"))) {
                            // If the version has changed, clear the cache dir, to avoid the possibility of incompatible cached classes
                            try {
                                FileUtils.deleteDirectory(cacheDir);
                            } catch (final IOException e) {
                                // Ignore
                            }
                        }
                        cachePath = cacheDir.getAbsolutePath();
                    }
                    diskStore.setAttribute("path", cachePath);
                    try {
                        final File configFile = File.createTempFile("cyclos_", "_ehcache.xml");
                        configFile.deleteOnExit();
                        final FileOutputStream out = new FileOutputStream(configFile);
                        try {
                            XmlHelper.writeNode(config, out);
                        } finally {
                            IOUtils.closeQuietly(out);
                        }
                        properties.setProperty("net.sf.ehcache.configurationResourceName", "file://" + configFile.getAbsolutePath());
                    } catch (final IOException e) {
                        // Silently ignore
                    }
                }
            }
        }

        // For MySQL, set the character encoding to utf8
        String url = StringUtils.trimToNull(properties.getProperty("hibernate.connection.url"));
        if (url != null && url.toLowerCase().startsWith("jdbc:mysql:")) {
            if (!url.contains("useUnicode")) {
                url += (url.contains("?") ? "&" : "?") + "useUnicode=true";
            }
            if (!url.contains("characterEncoding")) {
                url += (url.contains("?") ? "&" : "?") + "characterEncoding=utf8";
            }
            properties.setProperty("hibernate.connection.url", url);
        }
    }

    /**
     * Read the current version from the database
     */
    private String readCurrentVersion(final JDBCWrapper jdbc) {
        try {
            return jdbc.readScalarAsString("select version from application");
        } catch (final SQLException e) {
            return null;
        }
    }

    /**
     * Apply automatic upgrades on the schema
     */
    private String upgradeSchema(final String originalVersion, final JDBCWrapper jdbc) {
        String currentVersion = originalVersion;
        final VersionHistory history = new VersionHistoryReader().read();
        final List<Version> intermediateVersions = history.upgrade(originalVersion);
        if (intermediateVersions == null) {
            LOG.warn("Unknown version on database: " + originalVersion);
            return originalVersion;
        }
        String databaseName;
        try {
            databaseName = jdbc.getConnection().getMetaData().getDatabaseProductName();
        } catch (final SQLException e) {
            throw new RuntimeException("Error reading database name", e);
        }
        for (final Version version : intermediateVersions) {
            final String newVersion = version.getLabel();

            // Apply the statements
            final List<String> statements = version.getStatements(databaseName);
            LOG.info(String.format("Upgrading schema from version %s to version %s", currentVersion, newVersion));
            if (CollectionUtils.isNotEmpty(statements)) {
                int executedOk = 0;
                int totalUpdatedRows = 0;

                for (final String statement : statements) {
                    LOG.info(String.format("Executing statement %s", statement));
                    try {
                        final int updatedRows = jdbc.execute(statement);
                        if (updatedRows > 0) {
                            LOG.info("Updated rows: " + updatedRows);
                            totalUpdatedRows += updatedRows;
                        }
                        executedOk++;
                    } catch (final SQLException e) {
                        LOG.warn(String.format("Error applying automatic schema upgrade on version %s when executing statement [%s]: %s", newVersion, statement, e.getMessage()));
                    }
                }

                String msg = "Statements executed (ok / total): " + executedOk + " / " + statements.size() + ".";
                if (totalUpdatedRows == 0) {
                    msg += " None of the executed statements has modified data.";
                }
                LOG.info(msg);
            }

            // Apply the migrations
            try {
                final List<Class<Migration>> migrations = version.getMigrations(databaseName);
                if (CollectionUtils.isNotEmpty(migrations)) {
                    int executedOk = 0;
                    int totalUpdatedRows = 0;

                    for (final Class<Migration> clazz : migrations) {
                        LOG.info(String.format("Executing migration class %s", clazz.getName()));
                        Migration migration;
                        try {
                            migration = clazz.newInstance();
                        } catch (final Exception e) {
                            LOG.warn(String.format("Error instantiating the migration class %s", clazz.getName()), e);
                            continue;
                        }
                        try {
                            if (migration instanceof UntraceableMigration) {
                                ((UntraceableMigration) migration).execute(jdbc);
                            } else {// (TraceableMigration)
                                final int updatedRows = ((TraceableMigration) migration).execute(jdbc);
                                if (updatedRows > 0) {
                                    LOG.info("Updated rows: " + updatedRows);
                                    totalUpdatedRows += updatedRows;
                                }
                            }
                            executedOk++;
                        } catch (final Exception e) {
                            LOG.warn(String.format("Error upgrading to version %s when executing migration [%s]", newVersion, migration), e);
                        }
                    }

                    String msg = "Migrations executed (ok / total): " + executedOk + " / " + migrations.size() + ".";
                    if (totalUpdatedRows == 0) {
                        msg += " None of the executed migrations has modified data.";
                    }
                    LOG.info(msg);
                }

                // Perform a commit
                try {
                    jdbc.commit();
                } catch (final SQLException e) {
                    LOG.warn("Error while committing", e);
                }
            } catch (final Exception e) {
                try {
                    jdbc.rollback();
                } catch (final SQLException e1) {
                    LOG.warn("Error while rolling back", e1);
                }
            }

            // Remove the unused translation messages
            final List<String> removedTranslationKeys = version.getRemovedTranslationKeys();
            if (CollectionUtils.isNotEmpty(removedTranslationKeys)) {
                for (String key : removedTranslationKeys) {
                    key = StringUtils.trimToEmpty(key);
                    try {
                        final int deletedRows = jdbc.execute("delete from translation_messages where msg_key = ?", key);
                        if (deletedRows > 0) {
                            LOG.info(String.format("Removing unused translation message: %s", key));
                        } else {
                            LOG.info(String.format("Unused translation message: %s was not found in the database", key));
                        }
                    } catch (final Exception e) {
                        // Keeping an unused translation message is not that serious... Move on ;-)
                        LOG.warn(String.format("Error removing unused translation message: %s", key), e);
                    }
                }
                try {
                    jdbc.commit();
                } catch (final SQLException e) {
                    LOG.warn("Error while committing", e);
                }
            }

            // Update the version
            currentVersion = newVersion;
            try {
                jdbc.execute("update application set version = ?", currentVersion);
                jdbc.commit();
            } catch (final SQLException e) {
                LOG.warn(String.format("Error while updating the current version table to %s", currentVersion), e);
                try {
                    jdbc.rollback();
                } catch (final SQLException e1) {
                    LOG.warn("Error while rolling back", e1);
                }
            }

        }
        if (!originalVersion.equals(currentVersion)) {
            LOG.info(String.format("The database has been upgraded to version %s", currentVersion));
        }
        return currentVersion;
    }

    /**
     * Validates the connection parameters, returning an open connection
     */
    private Connection validateConnection(final String url, final String username, final String password) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url, username, password);
            connection.setAutoCommit(false);
        } catch (final SQLException e) {
            final String msg = "Error connecting to database at " + url;
            LOG.error(msg);
            throw new RuntimeException(msg, e);
        }

        // If we are under HSQLDB for file mode, we must close the connection, on shutdown
        if (url.contains("jdbc:hsqldb:file")) {
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                public void run() {
                    try {
                        final Connection connection = DriverManager.getConnection(url, username, password);
                        new JDBCWrapper(connection).execute("SHUTDOWN");
                        System.out.println("Shutdown on HSQLDB was successful");
                    } catch (final SQLException e) {
                        System.out.println("Shutdown on HSQLDB failed");
                        e.printStackTrace();
                    }
                }
            }));
        }

        return connection;
    }

    /**
     * Validates the given JDBC driver class
     */
    private void validateDriver(final String driverClass) {
        try {
            final Class<?> clazz = Class.forName(driverClass);
            if (!Driver.class.isAssignableFrom(clazz)) {
                throw new Exception();
            }
        } catch (final Exception e) {
            final String msg = "Illegal JDBC driver class on cyclos.properties: " + driverClass;
            LOG.error(msg);
            throw new RuntimeException(msg);
        }
    }
}
