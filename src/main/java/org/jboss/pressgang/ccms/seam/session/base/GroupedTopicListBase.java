package org.jboss.pressgang.ccms.seam.session.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;

import org.jboss.pressgang.ccms.rest.v1.constants.CommonFilterConstants;
import org.jboss.pressgang.ccms.restserver.entity.Filter;
import org.jboss.pressgang.ccms.restserver.entity.RelationshipTag;
import org.jboss.pressgang.ccms.restserver.entity.Tag;
import org.jboss.pressgang.ccms.restserver.entity.Topic;
import org.jboss.pressgang.ccms.restserver.filter.TopicFieldFilter;
import org.jboss.pressgang.ccms.restserver.filter.builder.TopicFilterQueryBuilder;
import org.jboss.pressgang.ccms.seam.session.ExtendedTopicList;
import org.jboss.pressgang.ccms.seam.sort.GroupedTopicListNameComparator;
import org.jboss.pressgang.ccms.seam.utils.Constants;
import org.jboss.pressgang.ccms.seam.utils.EntityUtilities;
import org.jboss.pressgang.ccms.seam.utils.FilterUtilities;
import org.jboss.pressgang.ccms.seam.utils.structures.GroupedList;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UIProjectsData;
import org.jboss.seam.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupedTopicListBase extends GroupedListBase<ExtendedTopicList>
{
    private static Logger log = LoggerFactory.getLogger(GroupedTopicListBase.class);
    
    protected TopicFilterQueryBuilder queryBuilder;
    private Predicate filterConditions;
	/**
	 * a mapping of category details to tag details with sorting and selection
	 * information
	 */
	protected UIProjectsData selectedTags;

	/** Used by the action links */
	protected Integer actionTopicId;

	/** Used by the remove relationship link */
	protected Integer otherTopicId;

	/** Used by the remove relationship link */
	protected Integer relationshipTagId;

	/** Used by the remove relationship link */
	private Integer newRelationshipTagId;
	
	/** URL used by the .page.xml file to redirect the user */
	private String externalURL;
	
	public String getExternalURL()
	{
		return externalURL;
	}

	public void setExternalURL(final String externalURL)
	{
		this.externalURL = externalURL;
	}

	public Integer getNewRelationshipTagId()
	{
		return newRelationshipTagId;
	}

	public void setNewRelationshipTagId(Integer newRelationshipTagId)
	{
		this.newRelationshipTagId = newRelationshipTagId;
	}

	public void setOtherTopicId(final Integer otherTopicId)
	{
		this.otherTopicId = otherTopicId;
	}

	public Integer getOtherTopicId()
	{
		return otherTopicId;
	}

	public void setActionTopicId(final Integer actionTopicID)
	{
		this.actionTopicId = actionTopicID;
	}

	public Integer getActionTopicId()
	{
		return actionTopicId;
	}

	public Integer getRelationshipTagId()
	{
		return relationshipTagId;
	}

	public void setRelationshipTagId(final Integer relationshipTag)
	{
		this.relationshipTagId = relationshipTag;
	}

	public void setSelectedTags(final UIProjectsData selectedTags)
	{
		this.selectedTags = selectedTags;
	}

	public UIProjectsData getSelectedTags()
	{
		return selectedTags;
	}

	public void create()
	{
	    final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
	    
		final Map<String, String> urlParameters = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		final Filter filter = EntityUtilities.populateFilter(entityManager, urlParameters,
		        CommonFilterConstants.FILTER_ID,
		        CommonFilterConstants.MATCH_TAG,
		        CommonFilterConstants.GROUP_TAG,
		        CommonFilterConstants.CATEORY_INTERNAL_LOGIC,
		        CommonFilterConstants.CATEORY_EXTERNAL_LOGIC,
		        CommonFilterConstants.MATCH_LOCALE,
                new TopicFieldFilter());
		
		queryBuilder = new TopicFilterQueryBuilder(entityManager);
		filterConditions = FilterUtilities.buildQueryConditions(filter, queryBuilder);

		// get a map of variable names to variable values
		filterVars = FilterUtilities.getUrlVariables(filter);

		filterURLVars = FilterUtilities.buildFilterUrlVars(filter);

		// get the heading to display over the list of topics
		searchTagHeading = FilterUtilities.getFilterTitle(filter);

		/*
		 * get a string that can be appended to a url that contains the url
		 * variables
		 */
		urlVars = FilterUtilities.buildFilterUrlVars(filter);

		/* The tags included in the URL query parameters */
		final List<Tag> urlGroupedTags = getGroupedTags();
		/* The tag IDs that have some Topics tagged against them */
		final List<Integer> groupedTags = new ArrayList<Integer>();

		for (final Tag tag : urlGroupedTags)
		{
			/*
			 * build the query, and add a new restriction that forces the group
			 * tag to be present
			 */
			final Predicate matchesTagCondition = queryBuilder.getMatchTagString(tag.getTagId());
			final Predicate condition;
			if (filterConditions != null) {
			    condition = queryBuilder.getCriteriaBuilder().and(filterConditions, matchesTagCondition);
			} else {
			    condition = matchesTagCondition;
			}
			
			final ExtendedTopicList topics = new ExtendedTopicList(Constants.DEFAULT_PAGING_SIZE, getQuery(condition));
			if (topics.getResultCount() != 0)
			{
				final GroupedList<ExtendedTopicList> groupedTopicList = new GroupedList<ExtendedTopicList>();
				groupedTopicList.setGroup(tag.getTagName());
				groupedTopicList.setEntityList(topics);

				groupedLists.add(groupedTopicList);

				if (pagingEntityQuery == null || pagingEntityQuery.getResultCount() < topics.getResultCount())
					pagingEntityQuery = topics;

				groupedTags.add(tag.getTagId());
			}
		}

		/* sort by tag name, and then add the unsorted topics on the end */
		Collections.sort(groupedLists, new GroupedTopicListNameComparator<ExtendedTopicList>());

		/*
		 * we didn't have any groups, so just find all the matching topics and
		 * dump them in the default group
		 */
		if (groupedTags.size() == 0)
		{
			final ExtendedTopicList topics = new ExtendedTopicList(Constants.DEFAULT_PAGING_SIZE, getSelectAllQuery());

			final GroupedList<ExtendedTopicList> groupedTopicList = new GroupedList<ExtendedTopicList>();
			groupedTopicList.setGroup(Constants.UNGROUPED_RESULTS_TAB_NAME);
			groupedTopicList.setEntityList(topics);

			groupedLists.add(groupedTopicList);
			pagingEntityQuery = topics;
		}
		/*
		 * Find that topics that are part of the query, but couldn't be matched
		 * in any group
		 */
		else
		{
		    // Create the NOT EXISTS Tag conditions
		    final List<Predicate> conditions = new ArrayList<Predicate>();
			for (final Integer tagID : groupedTags)
			{
			    conditions.add(queryBuilder.getMatchTagString(tagID));
			}
			
			// Add the filter conditions if any exist
			if (filterConditions != null) {
			    conditions.add(filterConditions);
			}
			
			// Generate the final condition
			final Predicate condition;
			if (!conditions.isEmpty()) {
    			final Predicate[] predicates = conditions.toArray(new Predicate[conditions.size()]);
    	        condition = queryBuilder.getCriteriaBuilder().and(predicates);
			} else {
			    condition = null;
			}
			
	        // Execute the search
			final ExtendedTopicList topics = new ExtendedTopicList(Constants.DEFAULT_PAGING_SIZE, getQuery(condition));

			if (topics.getResultCount() != 0)
			{
				final GroupedList<ExtendedTopicList> groupedTopicList = new GroupedList<ExtendedTopicList>();
				groupedTopicList.setGroup(Constants.UNGROUPED_RESULTS_TAB_NAME);
				groupedTopicList.setEntityList(topics);

				groupedLists.add(groupedTopicList);
				
				if (pagingEntityQuery == null || pagingEntityQuery.getResultCount() < topics.getResultCount())
					pagingEntityQuery = topics;
			}
		}

		if (groupedLists.size() != 0)
			this.tab = groupedLists.get(0).getGroup();
	}

	public void removeRelationship()
	{
		try
		{
			if (!(actionTopicId != null && otherTopicId != null && relationshipTagId != null))
				throw new IllegalArgumentException();

			final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
			final Topic topic = entityManager.find(Topic.class, actionTopicId);
			final Topic otherTopic = entityManager.find(Topic.class, otherTopicId);
			final RelationshipTag relationshipTag = entityManager.find(RelationshipTag.class, relationshipTagId);

			topic.removeRelationshipTo(otherTopic, relationshipTag);
			entityManager.persist(topic);

			entityManager.flush();

		}
		catch (final IllegalArgumentException ex)
		{
			log.warn(Constants.PRECONDITION_CHECK_FAILED_MESSAGE, ex);
		}
		catch (final Exception ex)
		{
			log.error("Probably an issue removing a relationship", ex);
		}
		finally
		{
			resetAjaxVars();
		}
	}

	public String viewRelatedTopic()
	{
		try
		{
			if (otherTopicId != null)
				return "/Topic.xhtml?topicTopicId=" + otherTopicId;

			return null;
		}
		finally
		{
			this.resetAjaxVars();
		}
	}

	public void convertRelationshipType()
	{
		try
		{
			if (!(actionTopicId != null && otherTopicId != null && relationshipTagId != null && newRelationshipTagId != null))
				throw new IllegalArgumentException();

			final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
			final Topic topic = entityManager.find(Topic.class, actionTopicId);
			final Topic otherTopic = entityManager.find(Topic.class, otherTopicId);
			final RelationshipTag relationshipTag = entityManager.find(RelationshipTag.class, relationshipTagId);
			final RelationshipTag newRelationshipTag = entityManager.find(RelationshipTag.class, newRelationshipTagId);

			topic.changeTopicToTopicRelationshipTag(newRelationshipTag, otherTopic, relationshipTag);
			entityManager.persist(topic);

			entityManager.flush();

		}
		catch (final IllegalArgumentException ex)
        {
            log.warn(Constants.PRECONDITION_CHECK_FAILED_MESSAGE, ex);
        }
        catch (final Exception ex)
        {
            log.error("Probably an issue gettign a relationship", ex);
        }
		finally
		{
			resetAjaxVars();
		}
	}

	protected void resetAjaxVars()
	{
		actionTopicId = null;
		otherTopicId = null;
		relationshipTagId = null;
		newRelationshipTagId = null;
	}
	
	protected CriteriaQuery<Topic> getSelectAllQuery() {
	    return getQuery(filterConditions);
	}

	protected CriteriaQuery<Topic> getQuery(final Predicate condition) {
	    final CriteriaQuery<Topic> query;
        if (condition != null) {
            query = queryBuilder.getBaseCriteriaQuery().where(condition);
        } else {
            query = queryBuilder.getBaseCriteriaQuery();
        }
        return query;
	}

}
