package org.jboss.pressgang.ccms.seam.session;

import static ch.lambdaj.Lambda.filter;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.collection.LambdaCollections.with;
import static org.hamcrest.Matchers.equalTo;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;
import org.drools.WorkingMemory;
import org.jboss.pressgang.ccms.restserver.entity.Tag;
import org.jboss.pressgang.ccms.restserver.entity.Topic;
import org.jboss.pressgang.ccms.restserver.entity.TopicToTag;
import org.jboss.pressgang.ccms.restserver.exceptions.CustomConstraintViolationException;
import org.jboss.pressgang.ccms.seam.session.base.GroupedTopicListBase;
import org.jboss.pressgang.ccms.seam.utils.Constants;
import org.jboss.pressgang.ccms.seam.utils.EntityUtilities;
import org.jboss.pressgang.ccms.seam.utils.TopicUtilities;
import org.jboss.pressgang.ccms.seam.utils.structures.DroolsEvent;
import org.jboss.pressgang.ccms.seam.utils.structures.GroupedList;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UICategoryData;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UIProjectData;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UIProjectsData;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UITagData;
import org.jboss.pressgang.ccms.utils.common.HTTPUtilities;
import org.jboss.pressgang.ccms.utils.common.MIMEUtilities;
import org.jboss.pressgang.ccms.utils.common.ZipUtilities;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.security.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Name("groupedTopicTagsList")
public class GroupedTopicTagsList extends GroupedTopicListBase
{
    private static final Logger log = LoggerFactory.getLogger(GroupedTopicTagsList.class);
    
	@In
	private Identity identity;
	@In
	private WorkingMemory businessRulesWorkingMemory;
	@In
	private EntityManager entityManager;
	
	/** A list of quick tag ids, which will be populated by a drools rule */
	private ArrayList<UITagData> quickTags = new ArrayList<UITagData>();

	public String doBackToSearchLink()
	{
		return "/CustomSearchTopics.seam?" + urlVars;
	}

	public void setQuickTags(final ArrayList<UITagData> quickTags)
	{
		this.quickTags = quickTags;
	}

	public ArrayList<UITagData> getQuickTags()
	{
		return quickTags;
	}

	@Override
	@Create
	public void create()
	{
		super.create();

		// populate the bulk tag database
		selectedTags = new UIProjectsData();
		selectedTags.populateTopicTags();

		loadQuickTags();
	}

	/********************************************************************************/

	private void loadQuickTags()
	{
		// Use drools to populate the quick tags
		businessRulesWorkingMemory.setGlobal("quickTags", quickTags);
		EntityUtilities.injectSecurity(businessRulesWorkingMemory, identity);
		businessRulesWorkingMemory.insert(new DroolsEvent("PopulateQuickTags"));
		businessRulesWorkingMemory.fireAllRules();
	}

	/**
	 * This function will apply or remove the bulk tags that were selected
	 * @throws CustomConstraintViolationException 
	 */
	public void applyBulkTags() throws CustomConstraintViolationException
	{
		final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
		final List<Topic> bulkTagList = entityManager.createQuery(getSelectAllQuery()).getResultList();

		/*
		 * Loop through once, and remove all the tags. then loop through again
		 * and add the tags. We do this because of the validation rules that
		 * prevent multiple tags from being added in a mutually exclusive
		 * category
		 */
		for (final Boolean remove : new Boolean[] { true, false })
		{
			// loop through each topic
			for (final Topic topic : bulkTagList)
			{
				for (final UIProjectData project : selectedTags.getProjectCategories())
				{
					// loop through each selected tag category
					for (final UICategoryData entry : project.getCategories())
					{
						// loop through each tag in the category
						for (final UITagData tagDetails : entry.getTags())
						{
							// we are only interested in those that have been
							// selected
							if ((tagDetails.isNotSelected() && remove) || (tagDetails.isSelected() && !remove))
							{
								manageTag(topic, tagDetails, remove);
							}
						}
					}
				}
			}
		}
	}

	protected void manageTag(final Topic topic, final UITagData tagDetails, final boolean remove) throws CustomConstraintViolationException
	{
		final Tag tag = entityManager.getReference(Tag.class, tagDetails.getId());
		final Topic persistTopic = entityManager.find(Topic.class, topic.getTopicId());

		// remove tags
		if (remove && tagDetails.isNotSelected())
		{
		    persistTopic.removeTag(tag);
		}
		// add tags
		else if (!remove && !tagDetails.isNotSelected())
		{
			persistTopic.addTag(tag);
		}

		entityManager.persist(persistTopic);
		entityManager.flush();
	}

	public void downloadCSV()
	{
		String csv = TopicUtilities.getCSVHeaderRow(entityManager);

		final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
		final List<Topic> bulkTagList = entityManager.createQuery(getSelectAllQuery()).getResultList();
		{
			// loop through each topic
			for (final Topic topic : bulkTagList)
				csv += "\n" + TopicUtilities.getCSVRow(entityManager, topic);
		}

		try
		{
			HTTPUtilities.writeOutContent(csv.getBytes("UTF-8"), "Topics.csv");
		}
		catch (UnsupportedEncodingException e)
		{
			/* UTF-8 is a valid format so this should exception should never get thrown */
		}
	}

	public void downloadXML()
	{
		final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
		final List<Topic> bulkTagList = entityManager.createQuery(getSelectAllQuery()).getResultList();

		// build up the files that will make up the zip file
		final HashMap<String, byte[]> files = new HashMap<String, byte[]>();

		for (final Topic topic : bulkTagList)
		{
			try
			{
				files.put(topic.getTopicId() + ".xml", topic.getTopicXML() == null ? "".getBytes("UTF-8") : topic.getTopicXML().getBytes("UTF-8"));
			}
			catch (UnsupportedEncodingException e)
			{
				/* UTF-8 is a valid format so this should exception should never get thrown */
			}
		}

		byte[] zipFile = null;
		try
		{
			zipFile = ZipUtilities.createZip(files);
		}
		catch (final Exception ex)
		{
			log.error("An error occurred creating a ZIP file", ex);
			zipFile = null;
		}

		HTTPUtilities.writeOutContent(zipFile, "XML.zip", MIMEUtilities.ZIP_MIME_TYPE);
	}

	public String createPopulatedTopic()
	{
		return "/TopicEdit.seam?" + urlVars;
	}

	public void addTagById(final Integer topicID, final Integer tagID) throws CustomConstraintViolationException
	{

		final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
		final Tag tag = entityManager.getReference(Tag.class, tagID);
		final Topic topic = entityManager.find(Topic.class, topicID);

		/* validity checking */
		if (tag == null)
			return;

		if (topic == null)
			return;

		if (filter(having(on(TopicToTag.class).getTag(), equalTo(tag)), topic.getTopicToTags()).size() != 0)
		{
			with(topic.getTopicToTags()).remove(having(on(TopicToTag.class).getTag(), equalTo(tag)));
		}

		topic.addTag(tag);

		entityManager.persist(topic);
		entityManager.flush();

		/*
		 * we might have added a tag that excludes this topic from the previous
		 * search, so we have to refresh the list
		 */
		for (final GroupedList<ExtendedTopicList> groupedTopicList : groupedLists)
			groupedTopicList.getEntityList().refresh();
	}

	public String getRelatedTopicsUrl()
	{
		try
		{
			if (actionTopicId == null)
				throw new IllegalArgumentException();
			
			return "/CustomSearchTopicList.seam?topicRelatedFrom=" + actionTopicId;
		}
		catch (final IllegalArgumentException ex)
		{
			log.error(Constants.PRECONDITION_CHECK_FAILED_MESSAGE, ex);
		}
		finally
		{
			resetAjaxVars();
		}
		
		return null;
	}
	
	public String getIncomingRelatedTopicsUrl()
	{
		try
		{
			if (actionTopicId == null)
				throw new IllegalArgumentException();
			
			return "/CustomSearchTopicList.seam?topicRelatedTo=" + actionTopicId;
		}
		catch (final IllegalArgumentException ex)
		{
		    log.warn(Constants.PRECONDITION_CHECK_FAILED_MESSAGE, ex);
		}
		finally
		{
			resetAjaxVars();
		}
		
		return null;
	}

	public void removeOutgoingRelationships()
	{
		try
		{
			if (actionTopicId == null)
				throw new IllegalArgumentException();

			final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
			final Topic topic = entityManager.find(Topic.class, actionTopicId);
			topic.getParentTopicToTopics().clear();
			entityManager.persist(topic);
			entityManager.flush();

		}
		catch (final IllegalArgumentException ex)
		{
		    log.warn(Constants.PRECONDITION_CHECK_FAILED_MESSAGE, ex);
		}
		catch (final Exception ex)
		{
			log.error("Probably a problem retrieving or updating a Topic entity", ex);
		}
		finally
		{
			resetAjaxVars();
		}
	}
	
	public void removeIncomingRelationships()
	{
		try
		{
			if (actionTopicId == null)
				throw new IllegalArgumentException();

			final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
			final Topic topic = entityManager.find(Topic.class, actionTopicId);
			topic.getChildTopicToTopics().clear();
			entityManager.persist(topic);
			entityManager.flush();

		}
		catch (final IllegalArgumentException ex)
		{
		    log.warn(Constants.PRECONDITION_CHECK_FAILED_MESSAGE, ex);
		}
		catch (final Exception ex)
		{
			log.error("Probably a problem retrieving or updating a Topic entity", ex);
		}
		finally
		{
			resetAjaxVars();
		}
	}
	
	public void removeAllRelationships()
	{
		try
		{
			if (actionTopicId == null)
				throw new IllegalArgumentException();

			final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
			final Topic topic = entityManager.find(Topic.class, actionTopicId);
			topic.getChildTopicToTopics().clear();
			topic.getParentTopicToTopics().clear();
			entityManager.persist(topic);
			entityManager.flush();

		}
		catch (final IllegalArgumentException ex)
		{
		    log.warn(Constants.PRECONDITION_CHECK_FAILED_MESSAGE, ex);
		}
		catch (final Exception ex)
		{
			log.error("Probably a problem retrieving or updating a Topic entity");
		}
		finally
		{
			resetAjaxVars();
		}
	}

}
