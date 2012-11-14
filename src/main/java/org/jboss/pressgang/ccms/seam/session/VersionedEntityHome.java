package org.jboss.pressgang.ccms.seam.session;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.jboss.seam.framework.EntityHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a convenient way to access an historical version of an Entity.
 * 
 */
public abstract class VersionedEntityHome<E> extends EntityHome<E> implements DisplayMessageInterface {
    private static final Logger log = LoggerFactory.getLogger(VersionedEntityHome.class);
    /** Serializable version identifier */
    private static final long serialVersionUID = -6283886098844950609L;
    /**
     * The Envers revision number, or null if we are looking at the current version
     */
    private Number revision;
    /** An instance of the historical Envers Entity */
    protected E revisionInstance;
    /** The current revision of the entity being edited */
    private Number currentRevision;
    /** The message to be displayed to the user */
    protected String displayMessage;

    @Override
    public String getDisplayMessage() {
        return displayMessage;
    }

    @Override
    public String getDisplayMessageAndClear() {
        final String retValue = this.displayMessage;
        this.displayMessage = null;
        return retValue;
    }

    public void setDisplayMessage(final String displayMessage) {
        this.displayMessage = displayMessage;
    }

    public void wire() {
        getInstance();

        try {
            if (this.isIdDefined()) {
                final AuditReader reader = AuditReaderFactory.get(this.getEntityManager());
                currentRevision = (Number) reader.createQuery().forRevisionsOfEntity(getEntityClass(), false, true)
                        .addProjection(AuditEntity.revisionNumber().max()).add(AuditEntity.id().eq(this.getId()))
                        .getSingleResult();
            }
        } catch (final Exception ex) {
            log.error("Could not get latest revision for entity", ex);
        }

    }

    @Override
    public String update() {
        /* See if this entity was modified in the mean time */
        final AuditReader reader = AuditReaderFactory.get(this.getEntityManager());
        final Number newCurrentRevision = (Number) reader.createQuery().forRevisionsOfEntity(getEntityClass(), false, true)
                .addProjection(AuditEntity.revisionNumber().max()).add(AuditEntity.id().eq(this.getId())).getSingleResult();

        /* Save the entity */
        final String retValue = super.update();

        /* No changes have been made, so all is good */
        if (newCurrentRevision.equals(currentRevision))
            return retValue;

        return org.jboss.pressgang.ccms.restserver.utils.Constants.CONCURRENT_EDIT;
    }

    /**
     * Set the revision of the object that is to be loaded
     * 
     * @param revision null if no historical Entity is to be loaded, or the Envers revision number
     */
    public void setRevision(final String revision) {
        try {
            final Integer newRevision = revision == null || revision.trim().length() == 0 ? null : Integer.parseInt(revision);
            if (setDirty(this.revision, newRevision))
                setInstance(null);
            this.revision = newRevision;

            if (this.revision == null) {
                revisionInstance = null;
            } else {
                final AuditReader reader = AuditReaderFactory.get(this.getEntityManager());
                revisionInstance = reader.find(getEntityClass(), this.getId(), this.revision);
            }

        } catch (final Exception ex) {
            log.error("Probably an error retrieving an Envers historical entity", ex);
            revisionInstance = null;
        }
    }

    /** Get the revision number */
    public String getRevision() {
        return revision == null ? "" : this.revision.toString();
    }

    /** Get the historical instance of the Entity */
    public E getRevisionInstance() {
        return revisionInstance;
    }
}
