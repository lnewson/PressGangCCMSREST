package org.jboss.pressgang.ccms.seam.session;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;

import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.faces.Redirect;

import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.seam.utils.Constants;

@Name("topBar")
public class TopBar {
    @In
    Redirect redirect;

    private String topicId;

    public String getBuild() {
        return Constants.BUILD;
    }

    public String getTopicId() {
        return topicId;
    }

    public void setTopicId(String topicId) {
        this.topicId = topicId;
    }

    public String getTopicViewUrl() {
        final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
        Topic topic = null;
        try {
            final Integer topicIdInt = Integer.parseInt(topicId.trim());
            topic = entityManager.find(Topic.class, topicIdInt);
        } catch (NumberFormatException ex) {
            throw new EntityNotFoundException();
        }

        if (topic != null) {
            redirect.setViewId("/Topic.xhtml");
            redirect.setParameter("topicTopicId", topicId);
            redirect.setParameter("topicRevision", null);
            redirect.execute();
        } else {
            throw new EntityNotFoundException();
        }

        return null;
    }

    public String getTopicEditUrl() {
        final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
        Topic topic = null;
        try {
            final Integer topicIdInt = Integer.parseInt(topicId.trim());
            topic = entityManager.find(Topic.class, topicIdInt);
        } catch (NumberFormatException ex) {
            throw new EntityNotFoundException();
        }

        if (topic != null) {
            redirect.setViewId("/TopicEdit.xhtml");
            redirect.setParameter("topicTopicId", topicId);
            redirect.execute();
        } else {
            throw new EntityNotFoundException();
        }

        return null;
    }

}
