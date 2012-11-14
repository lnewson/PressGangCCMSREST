package org.jboss.pressgang.ccms.seam.session;

import java.util.List;

import javax.persistence.EntityManager;

import org.jboss.pressgang.ccms.restserver.entity.RelationshipTag;
import org.jboss.pressgang.ccms.restserver.utils.EntityUtilities;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides a number of constant'ish values to the web layer */
@Name("constants")
public class Constants {
    private static final Logger log = LoggerFactory.getLogger(Constants.class);

    @In
    private EntityManager entityManager;

    public List<String> getLocales() {
        return EntityUtilities.getLocales(entityManager);
    }

    public String getLoginMessage() {
        final String retValue = System
                .getProperty(org.jboss.pressgang.ccms.restserver.utils.Constants.LOGIN_MESSAGE_SYSTEM_PROPERTY);
        return retValue == null ? "" : retValue;
    }

    public String getBugzillaUrl() {
        final String retValue = System.getProperty(CommonConstants.BUGZILLA_URL_PROPERTY);
        return retValue == null ? "" : retValue;
    }

    public List<RelationshipTag> getRelationshipTags() {
        try {
            @SuppressWarnings("unchecked")
            final List<RelationshipTag> retValue = entityManager.createQuery(RelationshipTag.SELECT_ALL_QUERY).getResultList();

            return retValue;
        } catch (final Exception ex) {
            log.error("Probably an error retrieving a list of RelationshipTags");
        }

        return null;
    }
}
