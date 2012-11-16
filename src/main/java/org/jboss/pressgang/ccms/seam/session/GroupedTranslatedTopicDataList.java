package org.jboss.pressgang.ccms.seam.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;

import org.jboss.pressgang.ccms.rest.v1.constants.CommonFilterConstants;
import org.jboss.pressgang.ccms.restserver.entity.Filter;
import org.jboss.pressgang.ccms.restserver.entity.FilterField;
import org.jboss.pressgang.ccms.restserver.entity.FilterLocale;
import org.jboss.pressgang.ccms.restserver.entity.Tag;
import org.jboss.pressgang.ccms.restserver.entity.TranslatedTopic;
import org.jboss.pressgang.ccms.restserver.entity.TranslatedTopicData;
import org.jboss.pressgang.ccms.restserver.filter.TopicFieldFilter;
import org.jboss.pressgang.ccms.restserver.filter.builder.TranslatedTopicDataFilterQueryBuilder;
import org.jboss.pressgang.ccms.restserver.utils.JPAUtils;
import org.jboss.pressgang.ccms.restserver.zanata.ZanataPullTopicThread;
import org.jboss.pressgang.ccms.seam.session.base.GroupedListBase;
import org.jboss.pressgang.ccms.seam.sort.GroupedTopicListNameComparator;
import org.jboss.pressgang.ccms.seam.utils.Constants;
import org.jboss.pressgang.ccms.seam.utils.EntityUtilities;
import org.jboss.pressgang.ccms.seam.utils.FilterUtilities;
import org.jboss.pressgang.ccms.seam.utils.structures.GroupedList;
import org.jboss.pressgang.ccms.zanata.ZanataConstants;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Name("groupedTranslatedTopicDataList")
public class GroupedTranslatedTopicDataList extends GroupedListBase<TranslatedTopicDataList>
{
    private static final Logger log = LoggerFactory.getLogger(GroupedTranslatedTopicDataList.class);
    
    @In
    private EntityManager entityManager;
	/**
	 * The number of results from a normal topic query, that signifies 
	 * the expected number of translated topics.
	 */
	private Integer expectedTotalTranslationCount = null;
	
	/** The locale of this set of translated topic datas. */
	private String locale;
	
	/** The Query Builder to be used to construct the SQL search query */
	protected final TranslatedTopicDataFilterQueryBuilder filterQueryBuilder;
    protected Predicate filterConditions;
    private final GroupedTranslatedTopicDataLocaleList parent;
	
	public GroupedTranslatedTopicDataList()
	{
		filterQueryBuilder = new TranslatedTopicDataFilterQueryBuilder(entityManager);
		parent = null;
	}
	
	public GroupedTranslatedTopicDataList(final String locale, final TranslatedTopicDataFilterQueryBuilder filterQueryBuilder)
	{
		this(locale, filterQueryBuilder, null);
	}
	
	public GroupedTranslatedTopicDataList(final String locale, final TranslatedTopicDataFilterQueryBuilder filterQueryBuilder, final GroupedTranslatedTopicDataLocaleList parent) {
	    this(locale, filterQueryBuilder, parent, null);
    }
	
	public GroupedTranslatedTopicDataList(final String locale, final TranslatedTopicDataFilterQueryBuilder filterQueryBuilder, final GroupedTranslatedTopicDataLocaleList parent, final Integer expectedTotalTranslationCount) {
	    this.locale = locale;
        this.filterQueryBuilder = filterQueryBuilder;
        this.parent = parent;
        this.expectedTotalTranslationCount = expectedTotalTranslationCount;
        create();
	}
	
	@Create
	public void create()
	{
		final Map<String, String> urlParameters = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		final Filter filter = EntityUtilities.populateFilter(entityManager, urlParameters,
		        CommonFilterConstants.FILTER_ID,
		        CommonFilterConstants.MATCH_TAG,
		        CommonFilterConstants.GROUP_TAG,
		        CommonFilterConstants.CATEORY_INTERNAL_LOGIC,
		        CommonFilterConstants.CATEORY_EXTERNAL_LOGIC,
		        CommonFilterConstants.MATCH_LOCALE,
                new TopicFieldFilter());

		// If the parent is null then this is being used as a EJB, otherwise just pull this data from the parent
		if (parent == null) {
		    filterConditions = FilterUtilities.buildQueryConditions(filter, filterQueryBuilder);
    
    		// get a map of variable names to variable values
    		filterVars = FilterUtilities.getUrlVariables(filter);
    
    		// get the heading to display over the list of topics
    		searchTagHeading = FilterUtilities.getFilterTitle(filter);
    
    		/*
    		 * get a string that can be appended to a url that contains the url
    		 * variables
    		 */
    		urlVars = FilterUtilities.buildFilterUrlVars(filter);
		} else {
		    filterConditions = parent.filterConditions;
		    
		    filterVars = parent.getFilterVars();
		    
		    searchTagHeading = parent.getSearchTagHeading();
		    
		    urlVars = parent.getUrlVars();
		}
		
		/* add this locale to the query */
        final Predicate localeCondition = filterQueryBuilder.getMatchingLocaleString(locale);
        if (filterConditions != null) {
            filterConditions = filterQueryBuilder.getCriteriaBuilder().and(filterConditions, localeCondition);
        } else {
            filterConditions = localeCondition;
        }

		final List<Integer> groupedTags = buildTagGroups(filterQueryBuilder);

		buildUngroupedResults(filterQueryBuilder, groupedTags);

		if (groupedTags.size() != 0)
			this.tab = groupedLists.get(0).getGroup();
		else
			this.tab = Constants.UNGROUPED_RESULTS_TAB_NAME;
		
		/* Create the basic query to find the total number of translations that should exist */
		if (expectedTotalTranslationCount == null) {
		    this.expectedTotalTranslationCount = buildEstimatedTotalQuery(filter);
		}
	}

	public String getLocale()
	{
		return locale;
	}

	public void setLocale(String locale)
	{
		this.locale = locale;
	}
	
	private List<Integer> buildTagGroups(final TranslatedTopicDataFilterQueryBuilder filterQueryBuilder)
	{
		/* The tags included in the URL query parameters */
		final List<Tag> urlGroupedTags = getGroupedTags();
		/* The tag IDs that have some Topics tagged against them */
		final List<Integer> groupedTags = new ArrayList<Integer>();
		
		for (final Tag tag : urlGroupedTags)
		{

			/*
			 * Find those TranslatedTopic entities that reference an historical instance
			 * of a Topic that is tagged with the group tag
			 */

			final Predicate groupedTagCondition = filterQueryBuilder.getMatchTagString(tag.getTagId());
			
			/* Add the tag search part of the query */
			final Predicate condition;
			if (filterConditions != null)
			{
				condition = filterQueryBuilder.getCriteriaBuilder().and(filterConditions, groupedTagCondition);
			} else {
			    condition = groupedTagCondition;
			}

			final TranslatedTopicDataList translatedTopicDataList = new TranslatedTopicDataList(Constants.DEFAULT_ENVERS_PAGING_SIZE, getQuery(condition));

			if (translatedTopicDataList.getResultCount() != 0)
			{
				final GroupedList<TranslatedTopicDataList> groupedList = new GroupedList<TranslatedTopicDataList>();
				groupedList.setGroup(tag.getTagName());
				groupedList.setEntityList(translatedTopicDataList);

				groupedLists.add(groupedList);

				if (pagingEntityQuery == null || pagingEntityQuery.getResultCount() < translatedTopicDataList.getResultCount())
					pagingEntityQuery = translatedTopicDataList;

				groupedTags.add(tag.getTagId());
			}
		
			/* sort by tag name, and then add the unsorted topics on the end */
			Collections.sort(groupedLists, new GroupedTopicListNameComparator<TranslatedTopicDataList>());
		}
		
		return groupedTags;
	}
	
	private void buildUngroupedResults(final TranslatedTopicDataFilterQueryBuilder filterQueryBuilder, final List<Integer> groupedTags)
	{
		/*
		 * we didn't have any groups, so just find all the matching topics and dump
		 * them in the default group
		 */
		if (groupedTags.size() == 0)
		{
			final TranslatedTopicDataList translatedTopicDataList = new TranslatedTopicDataList(Constants.DEFAULT_ENVERS_PAGING_SIZE, getSelectAllQuery());
	
			if (translatedTopicDataList.getResultCount() != 0)
			{
				final GroupedList<TranslatedTopicDataList> groupedTopicList = new GroupedList<TranslatedTopicDataList>();
				groupedTopicList.setGroup(Constants.UNGROUPED_RESULTS_TAB_NAME);
				groupedTopicList.setEntityList(translatedTopicDataList);
		
				groupedLists.add(groupedTopicList);
				
				pagingEntityQuery = translatedTopicDataList;
			}
		}
		/*
		 * Find that topics that are part of the query, but couldn't be matched in
		 * any group
		 */
		else
		{
			final List<Predicate> conditions = new ArrayList<Predicate>();
			for (final Integer tagID : groupedTags)
			{
				/*
				 * Find those TranslatedTopic entities that reference an historical
				 * instance of a Topic that is not tagged with the group tag
				 */
			    conditions.add(filterQueryBuilder.getNotMatchTagString(tagID));
			}
			
			// Add the filter conditions if any exist
            if (filterConditions != null) {
                conditions.add(filterConditions);
            }
            
            // Generate the final condition
            final Predicate condition;
            if (!conditions.isEmpty()) {
                final Predicate[] predicates = conditions.toArray(new Predicate[conditions.size()]);
                condition = filterQueryBuilder.getCriteriaBuilder().and(predicates);
            } else {
                condition = null;
            }

			TranslatedTopicDataList translatedTopicDataList = new TranslatedTopicDataList(Constants.DEFAULT_ENVERS_PAGING_SIZE, getQuery(condition));

			if (translatedTopicDataList.getResultCount() != 0)
			{
				final GroupedList<TranslatedTopicDataList> groupedTopicList = new GroupedList<TranslatedTopicDataList>();
				groupedTopicList.setGroup(Constants.UNGROUPED_RESULTS_TAB_NAME);
				groupedTopicList.setEntityList(translatedTopicDataList);

				groupedLists.add(groupedTopicList);
				

				if (pagingEntityQuery == null || pagingEntityQuery.getMaxResults() < translatedTopicDataList.getMaxResults())
					pagingEntityQuery = translatedTopicDataList;
			}
		}
	}
	
	public void redirectToZanata()
	{
		/* Get the zanata properties from the server properties */
		String zanataServerUrl = System.getProperty(ZanataConstants.ZANATA_SERVER_PROPERTY);
		String zanataProject = System.getProperty(ZanataConstants.ZANATA_PROJECT_PROPERTY);
		String zanataVersion = System.getProperty(ZanataConstants.ZANATA_PROJECT_VERSION_PROPERTY);
		
		/* Create the zanata url for the translated topic and locale */
		String link = zanataServerUrl + "/webtrans/Application.html?project=" + zanataProject + "&iteration=" + zanataVersion + "&localeId=" + locale;
		
		/* Loop through and add the Zanata ID's to be displayed */
		for (final GroupedList<TranslatedTopicDataList> groupedList: groupedLists)
		{
			if (groupedList.getGroup().equals(tab))
			{
				groupedList.getEntityList().setMaxResults(null);
				for (final Object o: groupedList.getEntityList().getResultList())
				{
					final TranslatedTopicData translatedTopicDataList = (TranslatedTopicData) o;
					link += "&doc=" + translatedTopicDataList.getTranslatedTopic().getZanataId();
				}
			}
		}
				
		/* Redirect the user to the zanata Url */
		ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
		try 
		{
			externalContext.redirect(link);
			FacesContext.getCurrentInstance().responseComplete();
			return;
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void pullFromZanata()
	{
		/* Loop through and add the Topic ID's to be pulled from zanata */
		Set<Integer> topicIds = new HashSet<Integer>();
		for (GroupedList<TranslatedTopicDataList> groupedList: groupedLists)
		{
			groupedList.getEntityList().setMaxResults(null);
			if (groupedList.getGroup().equals(tab))
			{
				for (Object o: groupedList.getEntityList().getResultList())
				{
					TranslatedTopicData translatedTopicData = (TranslatedTopicData) o;
					topicIds.add(translatedTopicData.getTranslatedTopic().getTopicId());
				}
			}
		}
		
		if (topicIds.isEmpty()) return;
		
		try
		{
			final ZanataPullTopicThread zanataPullTopicThread = new ZanataPullTopicThread(new ArrayList<Integer>(topicIds));
			final Thread thread = new Thread(zanataPullTopicThread);
			thread.start();
		}
		catch (final Exception ex)
		{
			log.error("Catch all exception handler", ex);
		}
	}
	
	public String doBackToSearchLink()
	{
		return "/TranslatedTopicSearch.seam?" + urlVars;
	}

	public Integer getExpectedTotalTranslationCount()
	{
		return expectedTotalTranslationCount;
	}

	public void setExpectedTotalTranslationCount(final Integer expectedTotalTranslationCount)
	{
		this.expectedTotalTranslationCount = expectedTotalTranslationCount;
	}
	
	public Long getTotalNumberOfSearchResults()
	{
		Long numResults = new Long(0);
		for (final GroupedList<TranslatedTopicDataList> data: this.groupedLists)
		{
			numResults += data.getEntityList().getResultCount();
		}
		return numResults;
	}
	
	public static Integer buildEstimatedTotalQuery(final Filter filter)
	{
	    // Make sure no locales are selected
	    filter.setFilterLocales(new HashSet<FilterLocale>());
	    
	    final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
	    final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		
		// Remove the translated search fields from the filter
		final List<FilterField> excludeFields = new ArrayList<FilterField>();
		for (final FilterField field : filter.getFilterFields())
		{
			if (field.getField().equals(CommonFilterConstants.TOPIC_LATEST_TRANSLATIONS_FILTER_VAR))
			{
				excludeFields.add(field);
			}
			else if (field.getField().equals(CommonFilterConstants.TOPIC_NOT_LATEST_TRANSLATIONS_FILTER_VAR))
			{
				excludeFields.add(field);
			}
			else if (field.getField().equals(CommonFilterConstants.TOPIC_LATEST_COMPLETED_TRANSLATIONS_FILTER_VAR))
			{
				excludeFields.add(field);
			}
			else if (field.getField().equals(CommonFilterConstants.TOPIC_NOT_LATEST_COMPLETED_TRANSLATIONS_FILTER_VAR))
			{
				excludeFields.add(field);
			}
		}
		
		for (final FilterField field : excludeFields)
		{
			filter.getFilterFields().remove(field);
		}
		
		final TranslatedTopicDataFilterQueryBuilder includeQueryBuilder = new TranslatedTopicDataFilterQueryBuilder(entityManager);
        final Predicate includeQueryCondtions = FilterUtilities.buildQueryConditions(filter, includeQueryBuilder);
        final CriteriaQuery<TranslatedTopicData> includeQuery = includeQueryBuilder.getBaseCriteriaQuery();
        final Join<TranslatedTopicData, TranslatedTopic> translatedTopic = JPAUtils.findJoinedType(includeQuery, TranslatedTopicData.class, TranslatedTopic.class);
        if (includeQueryCondtions != null)
            includeQuery.where(includeQueryCondtions);
        includeQuery.groupBy(translatedTopic.get("topicId"), translatedTopic.get("topicRevision"));
		
		/* 
         * Perform the search to get the number of translated topics
         * that match the normal Topic search and the topics that need
         * to be excluded based on the translated topic search.
         */
		final CriteriaQuery<Long> includeCountQuery = JPAUtils.countCriteria(entityManager, includeQuery);
        final Integer totalTopicCount = entityManager.createQuery(includeCountQuery).getResultList().size();
        Integer totalExcludeTopicCount = 0;
        
		if (!excludeFields.isEmpty()) {
		    
		    /* 
             * Add the extra translated search fields not covered by the generic Topic search query.
             * These fields should be negated from there normal operation so that we find the 
             * topics that don't match the existing current search filter.
             */
            final List<Predicate> conditions = new ArrayList<Predicate>();
            for (final FilterField field : excludeFields)
            {
                final String fieldName = field.getField();
                
                if (fieldName.equals(CommonFilterConstants.TOPIC_LATEST_TRANSLATIONS_FILTER_VAR))
                {
                    field.setField(CommonFilterConstants.TOPIC_NOT_LATEST_TRANSLATIONS_FILTER_VAR);
                    filter.getFilterFields().add(field);
                }
                
                else if (fieldName.equals(CommonFilterConstants.TOPIC_NOT_LATEST_TRANSLATIONS_FILTER_VAR))
                {
                    field.setField(CommonFilterConstants.TOPIC_LATEST_TRANSLATIONS_FILTER_VAR);
                    filter.getFilterFields().add(field);
                }
                
                else if (fieldName.equals(CommonFilterConstants.TOPIC_LATEST_COMPLETED_TRANSLATIONS_FILTER_VAR))
                {
                    field.setField(CommonFilterConstants.TOPIC_NOT_LATEST_COMPLETED_TRANSLATIONS_FILTER_VAR);
                    filter.getFilterFields().add(field);
                }
                
                else if (fieldName.equals(CommonFilterConstants.TOPIC_NOT_LATEST_COMPLETED_TRANSLATIONS_FILTER_VAR))
                {
                    field.setField(CommonFilterConstants.TOPIC_LATEST_COMPLETED_TRANSLATIONS_FILTER_VAR);
                    filter.getFilterFields().add(field);
                }
            }
		    
		    final TranslatedTopicDataFilterQueryBuilder excludeQueryBuilder = new TranslatedTopicDataFilterQueryBuilder(entityManager);
            final Predicate excludeQueryConditions = FilterUtilities.buildQueryConditions(filter, excludeQueryBuilder);
            final CriteriaQuery<TranslatedTopicData> excludeQuery = excludeQueryBuilder.getBaseCriteriaQuery();
            final Join<TranslatedTopicData, TranslatedTopic> exTranslatedTopic = JPAUtils.findJoinedType(excludeQuery, TranslatedTopicData.class, TranslatedTopic.class);
            excludeQuery.groupBy(exTranslatedTopic.get("topicId"), exTranslatedTopic.get("topicRevision"));
    		
    		if (excludeQueryConditions != null) {
    		    conditions.add(excludeQueryConditions);
    		}
    		
    		final Predicate condition;
            if (conditions.size() > 1) {
                final Predicate[] predicates = conditions.toArray(new Predicate[conditions.size()]);
                condition = criteriaBuilder.and(predicates);
            } else {
                condition = conditions.get(0);
            }
            
            excludeQuery.where(condition);
            
            final CriteriaQuery<Long> excludeCountQuery = JPAUtils.countCriteria(entityManager, excludeQuery);
            totalExcludeTopicCount = entityManager.createQuery(excludeCountQuery).getResultList().size();
		}
		
		return totalTopicCount - totalExcludeTopicCount;
	}
	
	protected CriteriaQuery<TranslatedTopicData> getSelectAllQuery() {
        return getQuery(filterConditions);
    }

    protected CriteriaQuery<TranslatedTopicData> getQuery(final Predicate condition) {
        final CriteriaQuery<TranslatedTopicData> query;
        if (condition != null) {
            query = filterQueryBuilder.getBaseCriteriaQuery().where(condition);
        } else {
            query = filterQueryBuilder.getBaseCriteriaQuery();
        }
        return query;
    }
}
