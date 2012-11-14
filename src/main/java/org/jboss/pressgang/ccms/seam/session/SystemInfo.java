package org.jboss.pressgang.ccms.seam.session;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.jboss.pressgang.ccms.restserver.utils.Constants;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class is used to provide information on the state of the server. */
@Name("systemInfo")
public class SystemInfo {
    private static final Logger log = LoggerFactory.getLogger(SystemInfo.class);

    @In
    private EntityManager entityManager;
    private String url;

    /**
     * @return The current database connection url
     */
    private String getConnectionUrl() {
        try {
            url = "";
            final Session sess = (Session) entityManager.getDelegate();
            sess.doWork(new Work() {
                @Override
                public void execute(Connection connection) throws SQLException {
                    final DatabaseMetaData dbmd = connection.getMetaData();
                    url = dbmd.getURL();
                }
            });
        } catch (final Exception ex) {
            log.error("Probably an error getting the details of the database connection", ex);
        }

        return url;
    }

    /**
     * @return true if the system is connected to the live database, and false otherwise
     */
    public boolean isLiveDatabase() {
        return this.getConnectionUrl().indexOf(Constants.LIVE_SQL_SERVER) != -1;

    }
}
