package org.jboss.pressgang.ccms.seam.session;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.exception.ConstraintViolationException;
import org.jboss.pressgang.ccms.utils.common.HTTPUtilities;
import org.jboss.pressgang.ccms.utils.common.MIMEUtilities;
import org.jboss.pressgang.ccms.utils.common.ZipUtilities;
import org.jboss.pressgang.ccms.model.Filter;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.rest.v1.constants.CommonFilterConstants;
import org.jboss.pressgang.ccms.utils.structures.Pair;
import org.jboss.pressgang.ccms.restserver.filter.TopicFieldFilter;
import org.jboss.pressgang.ccms.restserver.filter.builder.TopicFilterQueryBuilder;
import org.jboss.pressgang.ccms.restserver.utils.EnversUtilities;
import org.jboss.pressgang.ccms.restserver.utils.TopicUtilities;
import org.jboss.pressgang.ccms.restserver.zanata.ZanataPushTopicThread;
import org.jboss.pressgang.ccms.seam.session.base.TopicSearchBase;
import org.jboss.pressgang.ccms.seam.utils.EntityUtilities;
import org.jboss.pressgang.ccms.seam.utils.FilterUtilities;
import org.jboss.pressgang.ccms.seam.utils.contentspec.ContentSpecGenerator;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for the functionality provided by the tag search
 * page, including the ability to load and modify filters, and create a docbook
 * zip file from related topics
 */
@Name("customTagSearch")
@Scope(ScopeType.PAGE)
public class CustomTagSearch extends TopicSearchBase implements Serializable
{
    private static final Logger log = LoggerFactory.getLogger(CustomTagSearch.class);
	private static final long serialVersionUID = 9157677472039964070L;

	/** A list of topics that exist for the current filter */
	private List<Topic> filterTopics;
	@In
	private EntityManager entityManager;

	public CustomTagSearch()
	{

	}

	/**
	 * This function is used to populate the list of filters, populate the
	 * current filter by the url variables, and to preselect the tags that match
	 * those selected in the current filter.
	 */
	@Create
	public void populate()
	{
		// If we don't have a filter selected, populate the filter from the url
		// variables
	    final Filter filter;
		if (this.selectedFilter == null)
		{
			// build up a Filter object from the URL variables
			final FacesContext context = FacesContext.getCurrentInstance();
			filter = EntityUtilities.populateFilter(entityManager, context.getExternalContext().getRequestParameterMap(),
			        CommonFilterConstants.FILTER_ID,
			        CommonFilterConstants.MATCH_TAG,
			        CommonFilterConstants.GROUP_TAG,
			        CommonFilterConstants.CATEORY_INTERNAL_LOGIC,
			        CommonFilterConstants.CATEORY_EXTERNAL_LOGIC,
			        CommonFilterConstants.MATCH_LOCALE,
			        new TopicFieldFilter());
		}
		// otherwise load the filter using the filter id
		else
		{
			filter = entityManager.find(Filter.class, selectedFilter);
		}

		// preselect the tags on the web page that relate to the tags selected
		// by the filter
		selectedTags.populateTopicTags(filter, false);

		// sync up the filter field values
		this.topic.syncWithFilter(filter);

		// sync up with the filter options values
		FilterUtilities.updateDocbookOptionsFilter(filter, this.docbookBuildingOptions);

		getFilterList();
	}

	@SuppressWarnings("unchecked")
	private void getFilterList()
	{
		// get a list of the existing filters in the database
		filters.clear();
		filters.addAll(entityManager.createQuery(Filter.SELECT_ALL_QUERY).getResultList());
	}

	/**
	 * This will redirect the browser to the search results. Since we want the
	 * search results to be bookmarkable, we can't use conversations or backing
	 * beans to store the data. Instead we have to provide url parameters that
	 * the TopicTagsList class can used to initialize itself.
	 */
	@End(beforeRedirect = true)
	public String doSearch()
	{
		final String retVal = doTopicSearch("/CustomSearchTopicList.seam");
		return retVal;
	}

	public void downloadXML()
	{
		final Filter filter = new Filter();
		this.syncFilterWithUI(filter);

		final List<Topic> topicList = EntityUtilities.getTopicsFromFilter(entityManager, filter);

		// build up the files that will make up the zip file
		final HashMap<String, byte[]> files = new HashMap<String, byte[]>();

		for (final Topic topic : topicList)
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
			log.error("Probably a stream error", ex);
			zipFile = null;
		}

		HTTPUtilities.writeOutContent(zipFile, "XML.zip", MIMEUtilities.ZIP_MIME_TYPE);
	}

	public void downloadCSV()
	{
		String csv = TopicUtilities.getCSVHeaderRow(entityManager);

		final Filter filter = new Filter();
		this.syncFilterWithUI(filter);

		final List<Topic> topicList = EntityUtilities.getTopicsFromFilter(entityManager, filter);

		// loop through each topic
		for (final Topic topic : topicList)
			csv += "\n" + TopicUtilities.getCSVRow(entityManager, topic);

		try
		{
			HTTPUtilities.writeOutContent(csv.getBytes("UTF-8"), "Topics.csv");
		}
		catch (UnsupportedEncodingException e)
		{
			/* UTF-8 is a valid format so this should exception should never get thrown */
		}
	}

	/**
	 * This function takes the tags that have been selected, topic field filters
	 * and category boolean logic settings, and encapsulate this information in
	 * URL variables for CustomSearchTopicList.xhtml. The
	 * CustomSearchTopicList.xhtml then reads these variables and generates a
	 * list of Topic objects. By not using a backing bean, this URL can be
	 * copied and pasted without an existing conversation or session.
	 * 
	 * @return The URL containing the search information
	 */
	protected String doTopicSearch(final String url)
	{
		/*
		 * The filter class has a convenient method for creating url variables
		 * that match search queries. So create a new Filter instance, sync it
		 * with the UI (as defined by the values held by the selectedTags
		 * variable), and then pull out the URL variables.
		 */
		final Filter filter = new Filter();
		FilterUtilities.syncFilterWithCategories(filter, selectedTags);
		FilterUtilities.syncFilterWithFieldUIElements(filter, topic);
		FilterUtilities.syncFilterWithTags(filter, selectedTags);

		final String params = FilterUtilities.buildFilterUrlVars(filter);
		return url + "?" + params;
	}

	/**
	 * Loads a Filter object from the database given a FilterID, and selects the
	 * appropriate tags in the ui
	 */
	@Override
	public void loadFilter()
	{
		// load the filter from the database using the filter id
		Filter filter = null;
		if (selectedFilter != null)
			filter = entityManager.find(Filter.class, selectedFilter);
		else
			filter = new Filter();

		this.selectedFilterName = filter.getFilterName();

		this.selectedTags.loadTagCheckboxes(filter);
		this.selectedTags.loadCategoryLogic(filter);
		this.topic.loadFilterFields(filter);
		FilterUtilities.updateDocbookOptionsFilter(filter, this.docbookBuildingOptions);
	}

	@Override
	public String loadFilterAndSearch()
	{
		loadFilter();
		return doSearch();
	}
	
	public void loadFilterAndZanata()
	{
		this.loadFilter();
		this.pushToZanataInit();
	}

	/**
	 * This function takes the tag selections and uses these to populate a
	 * Filter
	 * 
	 * @param filter
	 *            The filter to sync with the tag selection
	 * @param persist
	 *            true if this filter is being saved to the db (i.e. the user is
	 *            actually saving the filter), and false if it is not (like when
	 *            the user is building the docbook zip file)
	 */
	@Override
	protected void syncFilterWithUI(final Filter filter)
	{
	    FilterUtilities.syncFilterWithTags(filter, this.selectedTags);
	    FilterUtilities.syncFilterWithCategories(filter, this.selectedTags);
	    FilterUtilities.syncFilterWithFieldUIElements(filter, this.topic);
	    FilterUtilities.syncWithDocbookOptions(filter, this.docbookBuildingOptions);
	}

	/**
	 * This function synchronizes the tags selected in the gui with the
	 * FilterTags in the Filter object, and saves the changes to the database.
	 * This persists the currently selected filter.
	 */
	@Override
	public void saveFilter()
	{
		try
		{
			// load the filter object if it exists
		    final Filter filter;
			if (this.selectedFilter != null)
				filter = entityManager.find(Filter.class, selectedFilter);
			else
				// get a reference to the Filter object
				filter = new Filter();

			if (validateFilterName(filter, this.selectedFilterName))
			{
				// set the name
				filter.setFilterName(this.selectedFilterName);
	
				// populate the filter with the options that are selected in the ui
				syncFilterWithUI(filter);
				// save the changes
				entityManager.persist(filter);
				this.selectedFilter = filter.getFilterId();
	
				getFilterList();
			}
			else
			{
				this.setDisplayMessage("The filter requires a unique name");
			}
		}
		catch (final PersistenceException ex)
		{
			if (ex.getCause() instanceof ConstraintViolationException) {
			    log.warn("Probably a constraint violation", ex);
				this.setDisplayMessage("The filter requires a unique name");
			} else {
			    log.error("Probably an error saving the Filter", ex);
				this.setDisplayMessage("The filter could not be saved");
			}
		}
		catch (final Exception ex)
		{
			log.error("Probably an error saving the Filter", ex);
			this.setDisplayMessage("The filter could not be saved");
		}
	}

	public String getCreateNewTopicUrl()
	{
		return EntityUtilities.buildEditNewTopicUrl(selectedTags);
	}

	public String doTextSearch()
	{
		return "/CustomSearchTopicList.seam?" + CommonFilterConstants.TOPIC_TEXT_SEARCH_FILTER_VAR + "=" + this.topic.getTopicTextSearch().getData();
	}
	
	private void pushToZanataInit()
	{
		try
		{
			final Filter filter = new Filter();
			this.syncFilterWithUI(filter);

			final CriteriaQuery<Topic> query = FilterUtilities.buildQuery(filter, new TopicFilterQueryBuilder(entityManager));
			filterTopics = entityManager.createQuery(query).getResultList();
		}
		catch (final Exception ex)
		{
			log.error("Probably an error loading the filter for a zanata push.", ex);
		}
	}
	
	public void pushToZanataConfirm()
	{
		final List<Pair<Integer, Integer>> topics = new ArrayList<Pair<Integer, Integer>>();
		for (final Topic topic: filterTopics)
		{
			topics.add(new Pair<Integer, Integer>(topic.getTopicId(), EnversUtilities.getLatestRevision(entityManager, topic).intValue()));
		}
		
		final ZanataPushTopicThread zanataPushTopicThread = new ZanataPushTopicThread(topics, false);
		final Thread thread = new Thread(zanataPushTopicThread);
		thread.start();
	}

	public List<Topic> getFilterTopics()
	{
		return filterTopics;
	}

	public void setFilterTopics(List<Topic> filterTopics)
	{
		this.filterTopics = filterTopics;
	}
	
	public List<Topic> getZanataTopicList()
	{
		return filterTopics;
	}
	
	public void loadFilterAndContentSpec()
	{
		this.loadFilter();
		this.generateContentSpec();
	}
	
	public void generateContentSpec()
	{
		final Filter filter = new Filter();
		this.syncFilterWithUI(filter);
		
		if (validateDocbookBuildOptions(this.docbookBuildingOptions))
		{
			final CriteriaQuery<Topic> query = FilterUtilities.buildQuery(filter, new TopicFilterQueryBuilder(entityManager));
			final List<Topic> topics = entityManager.createQuery(query).getResultList();
			
			final ContentSpecGenerator csGenerator = new ContentSpecGenerator(entityManager);
			final byte[] contentSpecs = csGenerator.generateContentSpecFromTopics(topics, getDocbookBuildingOptions());
			
			final DateFormat dateFormatter = new SimpleDateFormat("dd-MM-YYYY");
			
			HTTPUtilities.writeOutContent(contentSpecs, "ContentSpec-" + dateFormatter.format(new Date()) + ".zip", MIMEUtilities.ZIP_MIME_TYPE);
		}
	}
}
