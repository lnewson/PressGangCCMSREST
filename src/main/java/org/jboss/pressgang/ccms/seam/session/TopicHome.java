package org.jboss.pressgang.ccms.seam.session;

import static ch.lambdaj.Lambda.*;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceException;

import org.drools.WorkingMemory;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.pressgang.ccms.model.ImageFile;
import org.jboss.pressgang.ccms.model.PropertyTag;
import org.jboss.pressgang.ccms.model.PropertyTagCategory;
import org.jboss.pressgang.ccms.model.PropertyTagToPropertyTagCategory;
import org.jboss.pressgang.ccms.model.Tag;
import org.jboss.pressgang.ccms.model.TagToCategory;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.model.TopicSourceUrl;
import org.jboss.pressgang.ccms.model.TopicToPropertyTag;
import org.jboss.pressgang.ccms.model.TopicToTag;
import org.jboss.pressgang.ccms.model.TopicToTopicSourceUrl;
import org.jboss.pressgang.ccms.model.exceptions.CustomConstraintViolationException;
import org.jboss.pressgang.ccms.restserver.envers.EnversLoggingBean;
import org.jboss.pressgang.ccms.restserver.envers.LoggingRevisionEntity;
import org.jboss.pressgang.ccms.seam.utils.Constants;
import org.jboss.pressgang.ccms.seam.utils.EntityUtilities;
import org.jboss.pressgang.ccms.seam.utils.structures.DroolsEvent;
import org.jboss.pressgang.ccms.seam.utils.structures.PropertyTagUISelection;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UICategoryData;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UIProjectsData;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UITagData;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UIProjectData;
import org.jboss.pressgang.ccms.seam.utils.TopicUtilities;
import org.jboss.pressgang.ccms.restserver.utils.EnversUtilities;
import org.jboss.pressgang.ccms.restserver.utils.topicrenderer.TopicQueueRenderer;
import org.jboss.pressgang.ccms.restserver.zanata.ZanataPushTopicThread;
import org.jboss.pressgang.ccms.docbook.constants.DocbookBuilderConstants;
import org.jboss.pressgang.ccms.docbook.messaging.TopicRendererType;
import org.jboss.pressgang.ccms.utils.common.CollectionUtilities;
import org.jboss.pressgang.ccms.utils.concurrency.WorkQueue;
import org.jboss.pressgang.ccms.rest.v1.constants.CommonFilterConstants;
import org.jboss.pressgang.ccms.utils.structures.Pair;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.security.AuthorizationException;
import org.jboss.seam.security.Identity;
import org.jboss.seam.security.NotLoggedInException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Name("topicHome")
public class TopicHome extends VersionedEntityHome<Topic> {
    private static final Logger log = LoggerFactory.getLogger(TopicHome.class);
    /** Serializable version identifier */
    private static final long serialVersionUID = 1701692331956663275L;

    @In
    private WorkingMemory businessRulesWorkingMemory;
    @In
    private Identity identity;
    @In
    private EntityManager entityManager;
    private ImageFile newImageFile = new ImageFile();
    private TopicSourceUrl newTopicSourceUrl = new TopicSourceUrl();
    private ArrayList<Integer> processedTopics = new ArrayList<Integer>();
    private UIProjectsData selectedTags;
    private HashMap<Integer, ArrayList<Integer>> tagExclusions;
    /**
     * The name of the tab that is to be selected when the tab panel is displayed
     */
    private String selectedTab;
    /** The selected PropertyTag ID */
    private String newPropertyTagId;
    /** The value for the new TagToPropertyTag */
    private String newPropertyTagValue;
    /** A list of the available PropertyTags */
    private PropertyTagUISelection properties;

    public List<SelectItem> getProperties() {
        return properties.getProperties();
    }

    public TopicHome() {
        this.setRevision(null);
        setMessages();
    }

    @Override
    protected Topic createInstance() {
        Topic topic = new Topic();
        this.selectedTags = null;
        return topic;
    }

    public Topic getDefinedInstance() {
        return isIdDefined() ? getInstance() : null;
    }

    public String getExclusionArray(final Integer id) {
        String values = "";
        if (tagExclusions.containsKey(id)) {
            for (final Integer exclusion : tagExclusions.get(id)) {
                if (values.length() != 0)
                    values += ", ";

                values += exclusion.toString();
            }
        }

        return "[" + values + "]";
    }

    public String getMultipleUpdateUrl() {
        final String retvalue = "/CustomSearchTopicList.seam?topicIds=" + getTopicList();
        return retvalue;
    }

    public ImageFile getNewImageFile() {
        return newImageFile;
    }

    public TopicSourceUrl getNewTopicSourceUrl() {
        return newTopicSourceUrl;
    }

    public UIProjectsData getSelectedTags() {
        return selectedTags;
    }

    public HashMap<Integer, ArrayList<Integer>> getTagExclusions() {
        return tagExclusions;
    }

    protected Tag getTagFromId(final Integer tagId) {
        final TagHome tagHome = new TagHome();
        tagHome.setId(tagId);
        return tagHome.getInstance();
    }

    protected String getTopicList() {
        String retValue = "";
        for (final Integer topicId : processedTopics) {
            if (retValue.length() != 0)
                retValue += ",";
            retValue += topicId;
        }
        return retValue;
    }

    public Integer getTopicTopicId() {
        return (Integer) getId();
    }

    public boolean isDoingMultipleUpdates() {
        return processedTopics.size() != 0;
    }

    public boolean isWired() {
        return true;
    }

    public void load() {
        if (isIdDefined()) {
            wire();
        }
    }

    @Override
    public String persist() {

        if (validateEntity()) {
            try {
                updateTags();
            } catch (CustomConstraintViolationException ex) {
                log.warn("Probably an error with mutually exclusive tags", ex);
                this.displayMessage = "There was an error saving the Tags.";
                return "false";
            }
            prePersist();

            /*
             * The initial approach to having the status messages updated with the topic details was to use:
             * 
             * @Override
             * 
             * @Factory(value = "topic") public Topic getInstance() { return super.getInstance(); }
             * 
             * The problem with this is that the "topic" object is not refreshed, so the messages all reprint the details of the
             * first created object. The next step I took to try and fix this was to refresh the context in an override of
             * clearInstance() like so:
             * 
             * Contexts.getConversationContext().set("topic", null);
             * 
             * The problem with this is that the messages generated by the EntityHome object actually calculate their EL values
             * in a thread. So by the time the message was calculated, the clearInstance function had set the "topic" object to
             * null.
             * 
             * Manually setting the EL object "lasttopic" before the persist ensures that the messages have time to calculate
             * their variables before the object is reset.
             */

            Contexts.getConversationContext().set("lasttopic", this.getInstance());
            final String retValue = super.persist();
            
            TopicUtilities.render(getInstance());
            
            return retValue;
        } else {
            return "false";
        }
    }

    public String persistEx(final boolean addMore) {
        try {
            if (validateEntity()) {
                persist();
                processedTopics.add(this.getInstance().getTopicId());

                this.clearInstance();

                if (addMore)
                    return EntityUtilities.buildEditNewTopicUrl(selectedTags);

                return "backToList";
            } else {
                return "false";
            }
        } catch (final PersistenceException ex) {
            if (ex.getCause() instanceof ConstraintViolationException) {
                this.setDisplayMessage("The Topic violated a constraint");
                log.warn("Probably a constraint violation", ex);
            } else {
                this.setDisplayMessage("The Topic could not be saved. " + Constants.GENERIC_ERROR_INSTRUCTIONS);
                log.error("Probably an error updating a Tag entity", ex);
            }
        } catch (final Exception ex) {
            log.error("Probably an error persisting a ImageFile entity", ex);
            this.setDisplayMessage("The Topic could not be saved. " + Constants.GENERIC_ERROR_INSTRUCTIONS);
        }

        return null;
    }

    public void populate() {
        populateTags();
        properties = new PropertyTagUISelection(entityManager);
    }

    public void populateTags() {
        if (selectedTags == null) {
            selectedTags = new UIProjectsData();
            selectedTags.populateTopicTags(this.getInstance());
        } else {
            selectedTags.updateTags(this.getInstance().getTags(), true);
        }
        tagExclusions = EntityUtilities.populateExclusionTags(entityManager);
        EntityUtilities.populateMutuallyExclusiveCategories(selectedTags);
    }

    protected void prePersist() {
        final Topic topic = this.getInstance();
        TopicUtilities.syncXML(entityManager, topic);
        TopicUtilities.validateXML(entityManager, topic, DocbookBuilderConstants.ROCBOOK_DTD_BLOB_ID);
    }

    protected void preUpdate() {
        final Topic topic = this.getInstance();
        businessRulesWorkingMemory.setGlobal("topic", topic);
        EntityUtilities.injectSecurity(businessRulesWorkingMemory, identity);
        businessRulesWorkingMemory.insert(new DroolsEvent("UpdateTopicHome"));
        businessRulesWorkingMemory.fireAllRules();

        TopicUtilities.syncXML(entityManager, topic);
        TopicUtilities.validateXML(entityManager, topic, DocbookBuilderConstants.ROCBOOK_DTD_BLOB_ID);
    }

    public void removeTopicURL(final TopicToTopicSourceUrl url) {
        this.getInstance().getTopicToTopicSourceUrls().remove(url);
        url.getTopicSourceUrl().getTopicToTopicSourceUrls().remove(url);
    }

    public void saveNewTopicSourceUrl() {
        try {
            entityManager.persist(newTopicSourceUrl);

            final TopicToTopicSourceUrl topicToTopicSourceUrl = new TopicToTopicSourceUrl(newTopicSourceUrl, this.getInstance());

            this.getInstance().getTopicToTopicSourceUrls().add(topicToTopicSourceUrl);
            newTopicSourceUrl = new TopicSourceUrl();
        } catch (final Exception ex) {
            log.error("Probably an error saving a TopicToTopicSourceUrl entity", ex);
        }
    }

    public void setNewImageFile(ImageFile newImageFile) {
        this.newImageFile = newImageFile;
    }

    public void setNewTopicSourceUrl(final TopicSourceUrl newTopicSourceUrl) {
        this.newTopicSourceUrl = newTopicSourceUrl;
    }

    public void setSelectedTags(UIProjectsData value) {
        selectedTags = value;
    }

    public void setTagExclusions(HashMap<Integer, ArrayList<Integer>> tagExclusions) {
        this.tagExclusions = tagExclusions;
    }

    public void setTopicTopicId(Integer id) {
        final boolean managed = isManaged();
        final boolean idDefined = isIdDefined();
        final Object curId = getId();
        if (managed && idDefined && !curId.equals(id)) {
            this.selectedTags = null;
        }
        setId(id);
    }

    public void triggerCreateEvent() {
        final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
        final Topic topic = this.getInstance();

        if (!this.isManaged()) {
            // get the tags from the url, and add them by default
            final FacesContext context = FacesContext.getCurrentInstance();
            final Map<String, String> paramMap = context.getExternalContext().getRequestParameterMap();

            for (final String key : paramMap.keySet()) {
                try {
                    if (key.startsWith(CommonFilterConstants.MATCH_TAG)
                            && Integer.parseInt(paramMap.get(key)) == CommonFilterConstants.MATCH_TAG_STATE) {
                        final Integer tagID = Integer.parseInt(key.replace(CommonFilterConstants.MATCH_TAG, ""));
                        topic.addTag(entityManager, tagID);
                    }
                } catch (final NumberFormatException ex) {
                    log.debug("Probably an invalid integer value in the URL query parameters. Key = {}, Value = {}", key,
                            key.replace(CommonFilterConstants.MATCH_TAG, ""));
                } catch (CustomConstraintViolationException ex) {
                    log.warn("Probably an error with mutually exclusive tags", ex);
                    this.displayMessage = "There was an error saving the Tags.";
                }
            }

            final EnversLoggingBean enversLoggingBean = (EnversLoggingBean) Component.getInstance("enversLoggingBean");
            enversLoggingBean.setMinorChangeFlag(false);
            enversLoggingBean.setMajorChangeFlag(true);
        }

        // now run drools to modify the topic as needed
        businessRulesWorkingMemory.setGlobal("topic", topic);
        EntityUtilities.injectSecurity(businessRulesWorkingMemory, identity);

        if (this.isManaged())
            businessRulesWorkingMemory.insert(new DroolsEvent("LoadTopicHome"));
        else
            businessRulesWorkingMemory.insert(new DroolsEvent("NewTopicHome"));

        businessRulesWorkingMemory.fireAllRules();
    }

    @Override
    public String update() {
        try {
            if (validateEntity()) {
                updateTags();
                preUpdate();

                // See https://hibernate.onjira.com/browse/HHH-7329?focusedCommentId=46833#comment-46833
                this.getEntityManager().setFlushMode(FlushModeType.AUTO);
                final String result = super.update();
                
                TopicUtilities.render(getInstance());
                
                return result;
            }
        } catch (CustomConstraintViolationException ex) {
            log.warn("Probably an error with mutually exclusive tags", ex);
            this.displayMessage = "There was an error saving the Tags.";
        }

        return "false";
    }

    public String updateAndStay() {
        return update();
    }

    public void updateTags() throws CustomConstraintViolationException {
        final Topic topic = this.getInstance();

        if (topic != null) {
            final ArrayList<Tag> selectedTagObjects = new ArrayList<Tag>();

            for (final UIProjectData project : selectedTags.getProjectCategories()) {
                for (final UICategoryData cat : project.getCategories()) {
                    // is this a mutually exclusive category?
                    if (cat.isMutuallyExclusive()) {
                        /*
                         * if so, the selected tag is stored in the selectedID field of the category GuiInputData this has the
                         * effect of removing any other tags that might be already selected in this category
                         */
                        if (cat.getSelectedTag() != null)
                            selectedTagObjects.add(getTagFromId(cat.getSelectedTag()));

                    } else {
                        /*
                         * otherwise we find the selected tags from the tag GuiInputData objects in the ArrayList
                         */
                        for (final UITagData tagId : cat.getTags()) {
                            // if tag is selected
                            if (tagId.isSelected())
                                selectedTagObjects.add(getTagFromId(tagId.getId()));
                        }
                    }
                }
            }

            // match up selected tags with existing tags
            final Set<TopicToTag> topicToTags = topic.getTopicToTags();

            // make a note of the tags that were removed
            final ArrayList<Tag> removeTags = new ArrayList<Tag>();
            for (final TopicToTag topicToTag : topicToTags) {
                final Tag existingTag = topicToTag.getTag();

                if (!selectedTagObjects.contains(existingTag)) {
                    boolean hasPermission = true;

                    /*
                     * check to see if we have authority to modify this flag thanks to the category(s) it belongs to
                     */
                    for (TagToCategory category : existingTag.getTagToCategories()) {
                        try {
                            Identity.instance().checkRestriction(
                                    "#{s:hasPermission('" + category.getCategory().getCategoryName() + "', 'Enabled', null)}");
                            hasPermission = true;
                            break;
                        } catch (final NotLoggedInException ex) {
                            log.debug("User is not logged in");
                        } catch (final AuthorizationException ex) {
                            log.debug("User does not have permission", ex);
                        }
                    }

                    // otherwise see if we had permission on the tag itself
                    if (!hasPermission) {
                        try {
                            Identity.instance().checkRestriction(
                                    "#{s:hasPermission('" + existingTag.getTagName() + "', 'Enabled', null)}");
                            hasPermission = true;
                            break;
                        } catch (final NotLoggedInException ex) {
                            log.debug("User is not logged in");
                        } catch (final AuthorizationException ex) {
                            log.debug("User does not have permission", ex);
                        }
                    }

                    if (hasPermission) {
                        /*
                         * if we get to this point (i.e. no exception was thrown), we had authority to alter this flag add to
                         * external collection to avoid modifying a collection while looping over it
                         */
                        removeTags.add(existingTag);
                    }

                }
            }

            // now make a note of the additions
            final ArrayList<Tag> addTags = new ArrayList<Tag>();
            for (final Tag selectedTag : selectedTagObjects) {
                if (filter(having(on(TopicToTag.class).getTag(), equalTo(selectedTag)), topicToTags).size() == 0) {
                    addTags.add(selectedTag);
                }
            }

            // only proceed if there are some changes to make
            if (removeTags.size() != 0 || addTags.size() != 0) {
                // remove the deleted tags
                for (final Tag removeTag : removeTags) {
                    topic.removeTag(removeTag);
                }

                // add the created tags
                for (final Tag addTag : addTags) {
                    topic.addTag(addTag);
                }
            }
        }
    }

    private void setMessages() {
        this.setCreatedMessage(createValueExpression("Successfully Created Topic With ID: #{lasttopic.topicId} Title: #{lasttopic.topicTitle}"));
        this.setDeletedMessage(createValueExpression("Successfully Created Topic With ID: #{topic.topicId} Title: #{topic.topicTitle}"));
        this.setUpdatedMessage(createValueExpression("Successfully Created Topic With ID: #{topic.topicId} Title: #{topic.topicTitle}"));
    }

    public String getSelectedTab() {
        return selectedTab;
    }

    public void setSelectedTab(final String selectedTab) {
        this.selectedTab = selectedTab;
    }

    public void reRender() {
        WorkQueue.getInstance().execute(
                TopicQueueRenderer.createNewInstance(this.getTopicTopicId(), TopicRendererType.TOPIC, false));
    }

    public void generateXMLFromTemplate() {
        try {
            this.updateTags();
        } catch (CustomConstraintViolationException ex) {
            log.warn("Probably an error with mutually exclusive tags", ex);
            this.displayMessage = "There was an error updating the Tags.";
        }
        TopicUtilities.initializeFromTemplate(this.getInstance());
    }

    public String getNewPropertyTagId() {
        return newPropertyTagId;
    }

    public void setNewPropertyTagId(final String newPropertyTagId) {
        this.newPropertyTagId = newPropertyTagId;
    }

    public String getNewPropertyTagValue() {
        return newPropertyTagValue;
    }

    public void setNewPropertyTagValue(final String newPropertyTagValue) {
        this.newPropertyTagValue = newPropertyTagValue;
    }

    public void removeProperty(final TopicToPropertyTag tagToPropertyTag) {
        this.getInstance().removePropertyTag(tagToPropertyTag);
    }

    public void saveNewProperty() {
        try {
            if (this.newPropertyTagId.startsWith(Constants.PROPERTY_TAG_CATEGORY_SELECT_ITEM_VALUE_PREFIX)) {
                final String fixedNewPropertyTagId = this.newPropertyTagId.replace(
                        Constants.PROPERTY_TAG_CATEGORY_SELECT_ITEM_VALUE_PREFIX, "");
                final Integer fixedNewPropertyTagIdInt = Integer.parseInt(fixedNewPropertyTagId);
                final PropertyTagCategory propertyTagCategory = entityManager.find(PropertyTagCategory.class,
                        fixedNewPropertyTagIdInt);
                for (final PropertyTagToPropertyTagCategory propertyTagToPropertyTagCategory : propertyTagCategory
                        .getPropertyTagToPropertyTagCategories()) {
                    final PropertyTag propertyTag = propertyTagToPropertyTagCategory.getPropertyTag();

                    /* don't bulk add the same tags twice */
                    if (!this.getInstance().hasProperty(propertyTag)) {
                        final TopicToPropertyTag topicToPropertyTag = new TopicToPropertyTag();
                        topicToPropertyTag.setPropertyTag(propertyTag);
                        topicToPropertyTag.setTopic(this.getInstance());

                        /* if we are adding a unique property tag, make sure the initial value is unique */
                        if (propertyTag.getPropertyTagIsUnique())
                            topicToPropertyTag.setValue(Constants.UNIQUE_PROPERTY_TAG_PREFIX + " " + UUID.randomUUID());

                        this.getInstance().addPropertyTag(topicToPropertyTag);
                    }
                }
            } else if (this.newPropertyTagId.startsWith(Constants.PROPERTY_TAG_SELECT_ITEM_VALUE_PREFIX)) {
                final String fixedNewPropertyTagId = this.newPropertyTagId.replace(
                        Constants.PROPERTY_TAG_SELECT_ITEM_VALUE_PREFIX, "");
                final Integer fixedNewPropertyTagIdInt = Integer.parseInt(fixedNewPropertyTagId);
                final PropertyTag propertyTag = entityManager.find(PropertyTag.class, fixedNewPropertyTagIdInt);
                final TopicToPropertyTag topicToPropertyTag = new TopicToPropertyTag();
                topicToPropertyTag.setPropertyTag(propertyTag);
                topicToPropertyTag.setTopic(this.getInstance());
                topicToPropertyTag.setValue(this.newPropertyTagValue);
                this.getInstance().addPropertyTag(topicToPropertyTag);
            }

        } catch (final Exception ex) {
            log.error(
                    "Probably an issue getting an PropertyTag or PropertyTagCategory entity, or maybe a Integer.parse() issue",
                    ex);
            this.displayMessage = "There was an error saving the Property Tag.";
        }
    }

    public void updateBugs() {
        TopicUtilities.updateBugzillaBugs(entityManager, this.getInstance());
    }

    public void pushToZanataConfirm() {
        final List<Pair<Integer, Integer>> topics = new ArrayList<Pair<Integer, Integer>>();
        topics.add(new Pair<Integer, Integer>(this.getInstance().getTopicId(), EnversUtilities
                .getLatestRevision(entityManager, this.getInstance()).intValue()));

        final ZanataPushTopicThread zanataPushTopicThread = new ZanataPushTopicThread(topics, false);
        final Thread thread = new Thread(zanataPushTopicThread);
        thread.start();
    }

    public List<Topic> getZanataTopicList() {
        return CollectionUtilities.toArrayList(this.instance);
    }

    public void refreshEntity() {
        if (this.isManaged())
            this.getEntityManager().refresh(this.getInstance());
    }

    protected boolean validateEntity() {
        final Topic topic = this.getInstance();

        final List<String> errors = new ArrayList<String>();
        if (topic.getTopicTitle() == null || topic.getTopicTitle().isEmpty()) {
            errors.add("Topic Title cannot be blank");
        }

        if (errors.size() > 0) {
            this.displayMessage = "";
            for (final String error : errors) {
                this.displayMessage += error + "\\n";
            }

            this.displayMessage = this.displayMessage.substring(0, this.displayMessage.length() - 2);
        }

        return errors.size() == 0;
    }

    public String getLogMessage() {
        return getLogMessage(null);
    }

    public String getLogMessage(final Integer revision) {
        if (this.getRevision() == null || this.getRevision().isEmpty()) {
            return this.instance == null ? null : EnversUtilities.getLogMessage(entityManager, this.getInstance(), revision);
        } else {
            final Number rev = revision == null ? Integer.parseInt(this.getRevision()) : revision;
            return this.revisionInstance == null ? null : EnversUtilities.getLogMessage(entityManager, this.getRevisionInstance(), rev);
        }
    }

    public String getLogUsername() {
        return getLogUsername(null);
    }

    public String getLogUsername(final Integer revision) {
        final String username;
        if (this.getRevision() == null || this.getRevision().isEmpty()) {
            username = this.instance == null ? null : EnversUtilities.getLogUsername(entityManager, getInstance(), revision);
        } else {
            final Number rev = revision == null ? Integer.parseInt(this.getRevision()) : revision;
            username = this.revisionInstance == null ? null : EnversUtilities.getLogUsername(entityManager, getRevisionInstance(), rev);
        }

        return username == null ? "Unknown" : username;
    }

    public List<String> getLogFlags() {
        return getLogFlags(null);
    }

    public List<String> getLogFlags(final Integer revision) {
        final Integer flag;
        if (this.getRevision() == null || this.getRevision().isEmpty()) {
            flag = this.instance == null ? null : EnversUtilities.getLogFlag(entityManager, getInstance(), revision);
        } else {
            final Number rev = revision == null ? Integer.parseInt(this.getRevision()) : revision;
            flag = this.revisionInstance == null ? null : EnversUtilities.getLogFlag(entityManager, getRevisionInstance(), rev);
        }

        return calcLogFlags(flag);
    }

    private List<String> calcLogFlags(final Integer flag) {
        final List<String> flagStrings = new ArrayList<String>();

        if (flag != null) {
            if ((flag & LoggingRevisionEntity.MINOR_CHANGE_FLAG_BIT) == LoggingRevisionEntity.MINOR_CHANGE_FLAG_BIT) {
                flagStrings.add("Minor Change");
            }
            if ((flag & LoggingRevisionEntity.MAJOR_CHANGE_FLAG_BIT) == LoggingRevisionEntity.MAJOR_CHANGE_FLAG_BIT) {
                flagStrings.add("Major Change");
            }
        }

        return flagStrings;
    }
    
    public String getLogTimestamp() {
        return getLogMessage(null);
    }

    public Date getLogTimestamp(final Integer revision) {
        if (this.getRevision() == null || this.getRevision().isEmpty()) {
            return this.instance == null ? null : new Date(EnversUtilities.getRevisionEntity(entityManager, this.getInstance(), revision).getTimestamp());
        } else {
            final Number rev = revision == null ? Integer.parseInt(this.getRevision()) : revision;
            return this.revisionInstance == null ? null : new Date(EnversUtilities.getRevisionEntity(entityManager, this.getRevisionInstance(), rev).getTimestamp());
        }
    }

    public void init() {
    }
    
    public List<Number> getRevisions() {
        return EnversUtilities.getRevisions(entityManager, getInstance());
    }
}
