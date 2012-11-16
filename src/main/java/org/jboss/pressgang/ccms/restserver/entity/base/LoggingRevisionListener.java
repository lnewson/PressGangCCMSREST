package org.jboss.pressgang.ccms.restserver.entity.base;

import org.hibernate.envers.RevisionListener;
import org.jboss.pressgang.ccms.restserver.ejb.EnversLoggingBean;
import org.jboss.seam.Component;

/**
 * A Envers Revision Listener that will add content to a Envers Revision Entity when new data is persisted. It will pull the
 * Data from the RequestScoped EnversLoggingBean.
 * 
 * @author lnewson
 * 
 */
public class LoggingRevisionListener implements RevisionListener {

    /**
     * Add content to a new Envers Revision Entity.
     */
    @Override
    public void newRevision(Object o) {
        final LoggingRevisionEntity revEntity = (LoggingRevisionEntity) o;

        final EnversLoggingBean enversLoggingBean = (EnversLoggingBean) Component.getInstance("enversLoggingBean");
        
        if (enversLoggingBean != null) {
            revEntity.setLogFlag(enversLoggingBean.getFlag());
            revEntity.setLogMessage(enversLoggingBean.getLogMessage());
            revEntity.setUserName(enversLoggingBean.getUsername());
        }
    }
}
