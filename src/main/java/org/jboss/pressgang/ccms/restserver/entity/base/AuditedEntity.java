package org.jboss.pressgang.ccms.restserver.entity.base;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Transient;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.jboss.seam.Component;

/**
 * This base class is used to provide consistent access to previous versions of an audited entity
 */
public abstract class AuditedEntity<T extends AuditedEntity<T>> {
    private final Class<T> classType;
    private Date lastModified;

    public AuditedEntity(final Class<T> classType) {
        this.classType = classType;
    }

    /**
     * @return The ID of the database entity
     */
    abstract public Integer getId();

    /**
     * When returning a collection of entity revisions, the lastModified property is set automatically (in the getRevision
     * method). For entities returned from a database query, the last modified date needs to be found manually.
     * 
     * @return either the date saved in the lastModified property, or the latest revision date if lastModified is null
     */
    @Transient
    public Date getFixedLastModifiedDate(final EntityManager entityManager) {
        return lastModified != null ? lastModified : getLatestRevisionDate(entityManager);
    }

    /**
     * @return The last modified date for this entity
     */
    @Transient
    public Date getLastModifiedDate() {
        return lastModified;
    }

    /**
     * @param lastModified The last modified date for this entity
     */
    public void setLastModifiedDate(final Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * @return Returns a collection of revisions
     */
    @Transient
    public Map<Number, T> getRevisionEntities(final EntityManager entityManager) {
        final AuditReader reader = AuditReaderFactory.get(entityManager);
        final List<Number> revisions = reader.getRevisions(classType, this.getId());
        Collections.sort(revisions, Collections.reverseOrder());

        /* Use a LinkedHashMap to preserver the order */
        final Map<Number, T> retValue = new LinkedHashMap<Number, T>();
        for (final Number revision : revisions)
            retValue.put(revision, getRevision(reader, revision));

        return retValue;
    }
    
    /**
     * @return Returns the list of revision numbers for this entity, as maintained by Envers
     */
    @Transient
    public List<Number> getRevisions() {
        final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
        return getRevisions(entityManager);
    }

    /**
     * @return Returns the list of revision numbers for this entity, as maintained by Envers
     */
    @Transient
    public List<Number> getRevisions(EntityManager entityManager) {
        final AuditReader reader = AuditReaderFactory.get(entityManager);
        final List<Number> retValue = reader.getRevisions(classType, this.getId());
        Collections.sort(retValue, Collections.reverseOrder());
        return retValue;
    }

    /**
     * 
     * @param entityManager
     * @param revision
     * @return
     */
    @Transient
    public T getRevision(final EntityManager entityManager, final Number revision) {
        final AuditReader reader = AuditReaderFactory.get(entityManager);
        return getRevision(reader, revision);
    }
    
    @Transient
    private T getRevision(final AuditReader reader, final Number revision) {
        final T entity = reader.find(classType, this.getId(), revision);
        if (entity == null)
            return null;

        final Date revisionLastModified = reader.getRevisionDate(revision);
        entity.setLastModifiedDate(revisionLastModified);

        return entity;
    }

    @Transient
    public Number getLatestRevision(final EntityManager entityManager) {
        final AuditReader reader = AuditReaderFactory.get(entityManager);
        final List<Number> retValue = reader.getRevisions(classType, this.getId());
        Collections.sort(retValue, Collections.reverseOrder());
        return retValue.size() != 0 ? retValue.get(0) : -1;
    }

    /**
     * @return Returns the latest Envers revision number
     */
    @Transient
    public Date getLatestRevisionDate(final EntityManager entityManager) {
        final AuditReader reader = AuditReaderFactory.get(entityManager);
        return reader.getRevisionDate(getLatestRevision(entityManager));
    }

    @Transient
    public LoggingRevisionEntity getRevisionEntity(final EntityManager entityManager, final Number revision) {
        final AuditReader reader = AuditReaderFactory.get(entityManager);
        if (revision == null) {
            return reader.findRevision(LoggingRevisionEntity.class, getLatestRevision(entityManager));
        } else {
            return reader.findRevision(LoggingRevisionEntity.class, revision);
        }
    }
    
    @Transient
    public String getLogMessage(final EntityManager entityManager, final Number revision)
    {
        final LoggingRevisionEntity revEntity = getRevisionEntity(entityManager, revision);
        return revEntity == null ? null : revEntity.getLogMessage();
    }
    
    @Transient
    public Integer getLogFlag(final EntityManager entityManager, final Number revision)
    {
        final LoggingRevisionEntity revEntity = getRevisionEntity(entityManager, revision);
        return revEntity == null ? null : revEntity.getLogFlag();
    }
    
    @Transient
    public String getLogUsername(final EntityManager entityManager, final Number revision)
    {
        final LoggingRevisionEntity revEntity = getRevisionEntity(entityManager, revision);
        return revEntity == null ? null : revEntity.getUserName();
    }
}
