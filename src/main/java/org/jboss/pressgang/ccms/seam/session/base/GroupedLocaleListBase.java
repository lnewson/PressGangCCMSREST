package org.jboss.pressgang.ccms.seam.session.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import org.jboss.pressgang.ccms.rest.v1.constants.CommonFilterConstants;
import org.jboss.pressgang.ccms.seam.utils.EntityUtilities;
import org.jboss.seam.Component;

@SuppressWarnings("rawtypes")
public class GroupedLocaleListBase<T extends GroupedListBase>
{
	/** Provides the heading that identifies the currently selected filter tags */
	protected String searchTagHeading;
	/** A query that returns all of the elements displayed by all of the tabs. Used for group operations */
	protected String getAllQuery;
	/** holds the URL variables that define a filters options */
	protected String urlVars;
	/**
	 * The query used to find the entities in the list is set in the constructor
	 * using setEjbql. Because we modify this query based on the tags in the
	 * url, the url needs to have all the variables for the tags and categories.
	 * To ensure that the url always has these variables, we parse them out and
	 * save them in the filterVars collection, which is then read with a jstl
	 * foreach tag to place the required params into any link (like next,
	 * previous, first page, last page) that may require a new instance of this
	 * object to be constructed.
	 */
	protected HashMap<String, String> filterVars;
	/** The selected locale tab */
	protected String localeTab;
	
	public String getLocaleTab() {
		return localeTab;
	}

	public void setLocaleTab(String localeTab) {
		this.localeTab = localeTab;
	}
	
	public HashMap<String, String> getFilterVars()
	{
		return filterVars;
	}

	public void setFilterVars(final HashMap<String, String> filterVars)
	{
		this.filterVars = filterVars;
	}
	
	public void setUrlVars(final String urlVars)
	{
		this.urlVars = urlVars;
	}

	public String getUrlVars()
	{
		return urlVars;
	}
	
	public String getSearchTagHeading()
	{
		return searchTagHeading;
	}

	public void setSearchTagHeading(final String value)
	{
		searchTagHeading = value;
	}
	
	protected List<String> getGroupedLocales()
	{
		final List<String> matchedLocales = new ArrayList<String>();
		final List<String> matchedNotLocales = new ArrayList<String>();
		
		/* Get the locales from the url parameters  in the filter */
		for (final String urlParam : filterVars.keySet())
		{
			if (urlParam.matches(CommonFilterConstants.MATCH_LOCALE + "\\d+"))
			{
				final String urlParamValue = filterVars.get(urlParam);
				
				final String localeName = urlParamValue.replaceAll("\\d", "");
				final Integer state = Integer.parseInt(urlParamValue.replaceAll("[^\\d]", ""));
				
				if (state == CommonFilterConstants.MATCH_TAG_STATE)
					matchedLocales.add(localeName);
				else if (state == CommonFilterConstants.NOT_MATCH_TAG_STATE)
					matchedNotLocales.add(localeName);
			}
		}
		
		/* 
		 * If no matched locales were specified then use all the locales 
		 * from the database and remove the "not" locales. 
		 */
		if (matchedLocales.isEmpty())
		{
			final List<String> dbLocales = getAllLocalesFromDatabase();
			for (final String locale : dbLocales)
			{
				if (!matchedNotLocales.contains(locale))
					matchedLocales.add(locale);
			}
		}
		
		return matchedLocales;
	}
	
	@SuppressWarnings("unchecked")
	private List<String> getAllLocalesFromDatabase()
	{
		final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
		final String query = "select distinct translationLocale from TranslatedTopicData translatedTopicData";
		final List<String> locales = entityManager.createQuery(query).getResultList();
		return locales == null || locales.isEmpty() ? EntityUtilities.getLocales(entityManager) : locales;
	}
}
