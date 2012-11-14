package org.jboss.pressgang.ccms.seam.session;

import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.jboss.pressgang.ccms.rest.v1.constants.CommonFilterConstants;
import org.jboss.pressgang.ccms.restserver.entity.Filter;
import org.jboss.pressgang.ccms.restserver.entity.Topic;
import org.jboss.pressgang.ccms.restserver.filter.TopicFieldFilter;
import org.jboss.pressgang.ccms.restserver.filter.builder.TopicFilterQueryBuilder;
import org.jboss.pressgang.ccms.seam.utils.EntityUtilities;
import org.jboss.pressgang.ccms.seam.utils.FilterUtilities;
import org.jboss.seam.Component;

/**
 * This is a base class extended by other classes that need to display a list of
 * topics with an extended filter set.
 */
public class ExtendedTopicList extends TopicList
{
	/** Serializable version identifier */
	private static final long serialVersionUID = -4553573868560054166L;

	public ExtendedTopicList()
	{
		this(null, null, null);
	}

	public ExtendedTopicList(final int limit)
	{
		this(limit, null, null);
	}

	public ExtendedTopicList(final Integer limit, final CriteriaQuery<Topic> crtieriaQuery)
	{
		this(limit, crtieriaQuery, null);
	}

	public ExtendedTopicList(final Integer limit, final CriteriaQuery<Topic> crtieriaQuery, final TopicFieldFilter topic)
	{
		super(limit, crtieriaQuery == null ? constructCriteriaQuery() : crtieriaQuery, topic);
	}

	private static CriteriaQuery<Topic> constructCriteriaQuery()
	{
	    final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
	    
		// initialize filter home
		final Filter filter = EntityUtilities.populateFilter(entityManager, FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap(),
		        CommonFilterConstants.FILTER_ID,
		        CommonFilterConstants.MATCH_TAG,
		        CommonFilterConstants.GROUP_TAG,
		        CommonFilterConstants.CATEORY_INTERNAL_LOGIC,
		        CommonFilterConstants.CATEORY_EXTERNAL_LOGIC,
		        CommonFilterConstants.MATCH_LOCALE,
                new TopicFieldFilter());

		/*
		 * the filter may be null if an invalid variable was passed in the
		 * URL
		 */
		if (filter != null)
			// add the "and" and or "categories" clause to the default statement
			return FilterUtilities.buildQuery(filter, new TopicFilterQueryBuilder(entityManager));
		
		final CriteriaQuery<Topic> criteriaQuery = entityManager.getCriteriaBuilder().createQuery(Topic.class);
		final Root<Topic> from = criteriaQuery.from(Topic.class);
		
		return criteriaQuery.select(from);
	}

}
