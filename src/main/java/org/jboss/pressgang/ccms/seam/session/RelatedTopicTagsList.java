package org.jboss.pressgang.ccms.seam.session;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jboss.pressgang.ccms.filter.TopicFieldFilter;
import org.jboss.pressgang.ccms.model.Filter;
import org.jboss.pressgang.ccms.model.FilterField;
import org.jboss.pressgang.ccms.model.RelationshipTag;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.rest.v1.constants.CommonFilterConstants;
import org.jboss.pressgang.ccms.restserver.utils.EntityUtilities;
import org.jboss.pressgang.ccms.seam.session.base.GroupedTopicListBase;
import org.jboss.pressgang.ccms.seam.utils.FilterUtilities;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UIProjectsData;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
@Name("relatedTopicTagsList")
public class RelatedTopicTagsList extends GroupedTopicListBase implements DisplayMessageInterface, Serializable {
    private static final Logger log = LoggerFactory.getLogger(RelatedTopicTagsList.class);
    private static final long serialVersionUID = 1724809677704029918L;
    /**
     * The id of the main topic
     */
    private Integer topicTopicId;
    /**
     * The actual Topic object found with the topicTopicId
     */
    private Topic instance;
    /**
     * The object that holds the filter field values
     */
    private TopicFieldFilter topic = new TopicFieldFilter();
    /**
     * A list of the current relationships tags
     */
    private List<SelectItem> relationshipTags = new ArrayList<SelectItem>();
    /**
     * The selected RelationshipTag ID
     */
    private Integer selectedRelationshipTagID;
    /**
     * The message to be displayed to the user
     */
    private String displayMessage;
    @In
    private EntityManager entityManager;

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

    public RelatedTopicTagsList() {

    }

    @Override
    @Create
    public void create() {
        super.create();

        // build up a Filter object from the URL variables
        final Filter filter = EntityUtilities.populateFilter(entityManager,
                FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap(), CommonFilterConstants.FILTER_ID,
                CommonFilterConstants.MATCH_TAG, CommonFilterConstants.GROUP_TAG, CommonFilterConstants.CATEORY_INTERNAL_LOGIC,
                CommonFilterConstants.CATEORY_EXTERNAL_LOGIC, CommonFilterConstants.MATCH_LOCALE, new TopicFieldFilter());

        /*
         * preselect the tags on the web page that relate to the tags selected by the filter
         */
        selectedTags = new UIProjectsData();
        selectedTags.populateTopicTags(filter, false);

        /* sync up the filter field values */
        for (final FilterField field : filter.getFilterFields())
            this.topic.setFieldValue(field.getField(), field.getValue());

        /* Get the list of RelationshipTags */
        final List<RelationshipTag> tags = entityManager.createQuery(RelationshipTag.SELECT_ALL_QUERY).getResultList();
        for (final RelationshipTag tag : tags) {
            this.relationshipTags.add(new SelectItem(tag.getRelationshipTagId(), tag.getRelationshipTagName()));
            if (this.selectedRelationshipTagID == null) this.selectedRelationshipTagID = tag.getRelationshipTagId();
        }
    }

    public void oneWayToAll() {
        try {
            final Topic mainTopic = entityManager.find(Topic.class, topicTopicId);
            final RelationshipTag relationshipTag = entityManager.getReference(RelationshipTag.class, this.selectedRelationshipTagID);
            final List<Topic> topics = entityManager.createQuery(getSelectAllQuery()).getResultList();
            for (final Topic topic : topics) {
                final boolean isChild = mainTopic.isRelatedTo(topic, relationshipTag);

                if (!isChild && !mainTopic.equals(topic)) mainTopic.addRelationshipTo(topic, relationshipTag);
            }
            entityManager.persist(mainTopic);
        } catch (final Exception ex) {
            log.error("Probably an issue with the topic with id" + topicTopicId, ex);
        }
    }

    public void oneWayFromAll() {
        try {
            final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
            final Topic mainTopic = entityManager.find(Topic.class, topicTopicId);
            final RelationshipTag relationshipTag = entityManager.getReference(RelationshipTag.class, this.selectedRelationshipTagID);
            final List<Topic> topics = entityManager.createQuery(getSelectAllQuery()).getResultList();
            for (final Topic topic : topics) {
                final boolean isChild = topic.isRelatedTo(mainTopic, relationshipTag);

                if (!isChild && !mainTopic.equals(topic)) {
                    topic.addRelationshipTo(mainTopic, relationshipTag);
                    entityManager.persist(topic);
                }
            }
        } catch (final Exception ex) {
            log.error("Probably an issue with the topic with id" + topicTopicId, ex);
        }
    }

    public void twoWayWithAll() {
        try {
            final Topic mainTopic = entityManager.find(Topic.class, topicTopicId);
            final RelationshipTag relationshipTag = entityManager.getReference(RelationshipTag.class, this.selectedRelationshipTagID);
            final List<Topic> topics = entityManager.createQuery(getSelectAllQuery()).getResultList();
            for (final Topic topic : topics) {
                final boolean isMainTopicChild = topic.isRelatedTo(mainTopic, relationshipTag);

                if (!isMainTopicChild && !mainTopic.equals(topic)) {
                    topic.addRelationshipTo(mainTopic, relationshipTag);
                    entityManager.persist(topic);
                }

                final boolean isTopicChild = mainTopic.isRelatedTo(topic, relationshipTag);

                if (!isTopicChild && !mainTopic.equals(topic)) mainTopic.addRelationshipTo(topic, relationshipTag);
            }
            entityManager.persist(mainTopic);
        } catch (final Exception ex) {
            log.error("Probably an issue with the topic with id" + topicTopicId, ex);
        }
    }

    public void removeToAll() {
        try {
            final Topic mainTopic = entityManager.find(Topic.class, topicTopicId);
            final RelationshipTag relationshipTag = entityManager.find(RelationshipTag.class, this.selectedRelationshipTagID);
            final List<Topic> topics = entityManager.createQuery(getSelectAllQuery()).getResultList();
            for (final Topic topic : topics) {
                mainTopic.removeRelationshipTo(topic, relationshipTag);
            }
            entityManager.persist(mainTopic);
        } catch (final Exception ex) {
            log.error("Probably an issue with the topic with id" + topicTopicId, ex);
        }
    }

    public void removeFromAll() {
        try {
            final Topic mainTopic = entityManager.find(Topic.class, topicTopicId);
            final RelationshipTag relationshipTag = entityManager.find(RelationshipTag.class, this.selectedRelationshipTagID);
            final List<Topic> topics = entityManager.createQuery(getSelectAllQuery()).getResultList();
            for (final Topic topic : topics) {
                if (topic.removeRelationshipTo(mainTopic, relationshipTag)) entityManager.persist(topic);
            }
        } catch (final Exception ex) {
            log.error("Probably an issue with the topic with id" + topicTopicId, ex);
        }
    }

    public void removeBetweenAll() {
        try {
            final Topic mainTopic = entityManager.find(Topic.class, topicTopicId);
            final RelationshipTag relationshipTag = entityManager.find(RelationshipTag.class, this.selectedRelationshipTagID);
            final List<Topic> topics = entityManager.createQuery(getSelectAllQuery()).getResultList();
            for (final Topic topic : topics) {
                if (topic.removeRelationshipTo(mainTopic, relationshipTag)) {
                    entityManager.persist(topic);
                }

                mainTopic.removeRelationshipTo(topic, relationshipTag);
            }
            entityManager.persist(mainTopic);
        } catch (final Exception ex) {
            log.error("Probably an issue with the topic with id" + topicTopicId, ex);
        }
    }

    public String doSearch() {
        return "/CustomRelatedTopicList.seam?" + getSearchUrlVars();
    }

    protected String getSearchUrlVars() {
        return getSearchUrlVars(null);
    }

    protected String getSearchUrlVars(final String startRecord) {
        final Filter filter = new Filter();
        FilterUtilities.syncFilterWithCategories(filter, selectedTags);
        FilterUtilities.syncFilterWithFieldUIElements(filter, topic);
        FilterUtilities.syncFilterWithTags(filter, selectedTags);

        final String params = FilterUtilities.buildFilterUrlVars(filter);
        return "topicTopicId=" + topicTopicId + "&" + params;
    }

    public String removeRelationship(final Integer otherTopicId, final boolean to, final boolean from, final boolean returnToSearch) {
        try {
            final Topic thisTopic = entityManager.find(Topic.class, topicTopicId);
            final Topic otherTopic = entityManager.find(Topic.class, otherTopicId);
            final RelationshipTag relationshipTag = entityManager.find(RelationshipTag.class, this.selectedRelationshipTagID);

            if (from) {
                if (thisTopic.removeRelationshipTo(otherTopic, relationshipTag)) {
                    entityManager.persist(thisTopic);
                    instance = thisTopic;
                }
            }

            if (to) {
                if (otherTopic.removeRelationshipTo(thisTopic, relationshipTag)) {
                    entityManager.persist(otherTopic);
                }
            }

            entityManager.flush();
        } catch (final Exception ex) {
            log.error("Probably an error retrieving or persiting Topics in the database", ex);
        }

        final String retValue = returnToSearch ? "/CustomSearchTopicList.xhtml" : null;

        return retValue;
    }

    public String createRelationship(final Integer otherTopicId, final boolean to, final boolean from, final boolean returnToSearch) {
        try {
            final Topic thisTopic = entityManager.find(Topic.class, topicTopicId);
            final Topic otherTopic = entityManager.find(Topic.class, otherTopicId);
            final RelationshipTag relationshipTag = entityManager.getReference(RelationshipTag.class, this.selectedRelationshipTagID);

            if (from) {
                if (!thisTopic.isRelatedTo(otherTopic, relationshipTag) && !thisTopic.equals(otherTopic)) {
                    if (thisTopic.addRelationshipTo(otherTopic, relationshipTag)) {
                        entityManager.persist(thisTopic);
                    }
                }
            }

            if (to) {
                if (!otherTopic.isRelatedTo(thisTopic, relationshipTag) && !thisTopic.equals(otherTopic)) {
                    if (otherTopic.addRelationshipTo(thisTopic, relationshipTag)) {
                        entityManager.persist(otherTopic);
                        entityManager.refresh(thisTopic);
                    }
                }
            }

            instance = thisTopic;
            entityManager.flush();

        } catch (final Exception ex) {
            log.error("Probably an error retrieving or persisting Topics in the database", ex);
        }

        final String retValue = returnToSearch ? "/CustomSearchTopicList.xhtml" : null;

        return retValue;
    }

    public void setTopicTopicId(final Integer topicTopicId) {
        this.topicTopicId = topicTopicId;

        try {
            instance = entityManager.find(Topic.class, topicTopicId);
        } catch (final Exception ex) {
            log.error("Probably an error retrieving a Topic from the database", ex);
        }
    }

    public Integer getTopicTopicId() {
        return topicTopicId;
    }

    public void setInstance(final Topic instance) {
        this.instance = instance;
    }

    public Topic getInstance() {
        return instance;
    }

    public TopicFieldFilter getTopic() {
        return topic;
    }

    public void setTopic(final TopicFieldFilter topic) {
        this.topic = topic;
    }

    public Integer getSelectedRelationshipTagID() {
        return selectedRelationshipTagID;
    }

    public void setSelectedRelationshipTagID(final Integer selectedRelationshipTagID) {
        this.selectedRelationshipTagID = selectedRelationshipTagID;
    }

    public List<SelectItem> getRelationshipTags() {
        return relationshipTags;
    }

    public void setRelationshipTags(List<SelectItem> relationshipTags) {
        this.relationshipTags = relationshipTags;
    }
}
