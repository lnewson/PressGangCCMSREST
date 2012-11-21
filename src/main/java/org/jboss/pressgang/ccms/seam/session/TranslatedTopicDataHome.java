package org.jboss.pressgang.ccms.seam.session;

import java.util.List;

import javax.persistence.EntityManager;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import org.jboss.pressgang.ccms.model.TranslatedTopicData;
import org.jboss.pressgang.ccms.restserver.utils.EnversUtilities;

@Name("translatedTopicDataHome")
public class TranslatedTopicDataHome extends VersionedEntityHome<TranslatedTopicData> {
    private static final long serialVersionUID = -3872824824385606193L;

    @In
    private EntityManager entityManager;
    
    public void setTranslatedTopicDataId(Integer id) {
        setId(id);
    }

    public Integer getTranslatedTopicDataId() {
        return (Integer) getId();
    }

    @Override
    protected TranslatedTopicData createInstance() {
        final TranslatedTopicData instance = new TranslatedTopicData();
        return instance;
    }

    public void load() {
        if (isIdDefined()) {
            wire();
        }
    }

    public void wire() {
        getInstance();
    }

    public boolean isWired() {
        return true;
    }

    public TranslatedTopicData getDefinedInstance() {
        return isIdDefined() ? getInstance() : null;
    }

    public void refreshEntity() {
        if (this.isManaged())
            this.getEntityManager().refresh(this.getInstance());
    }
    
    public List<Number> getRevisions() {
        return EnversUtilities.getRevisions(entityManager, getInstance());
    }
}
