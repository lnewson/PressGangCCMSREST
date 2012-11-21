package org.jboss.pressgang.ccms.seam.session;

import javax.persistence.EntityManager;

import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.model.TranslatedTopic;
import org.jboss.pressgang.ccms.restserver.utils.TopicUtilities;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

@Name("entityHelper")
public class EntityHelper {

    @In
    private EntityManager entityManager;
    
    public String getTopicTagsList(final Topic topic) {
        return TopicUtilities.getTagsList(topic, true);
    }
    
    public String getTopicCommaSeparatedTagList(final Topic topic) {
        return TopicUtilities.getCommaSeparatedTagList(topic);
    }
    
    public String getTranslatedTopicTitle(final TranslatedTopic translatedTopic) {
        return translatedTopic.getEnversTopic(entityManager).getTopicTitle();
    }
    
    public String getTranslatedTopicTagsList(final TranslatedTopic translatedTopic) {
        return getTopicTagsList(translatedTopic.getEnversTopic(entityManager));
    }
}
