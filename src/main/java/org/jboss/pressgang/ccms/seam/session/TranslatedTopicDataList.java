package org.jboss.pressgang.ccms.seam.session;

import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.jboss.pressgang.ccms.filter.TopicFieldFilter;
import org.jboss.pressgang.ccms.filter.builder.TranslatedTopicDataFilterQueryBuilder;
import org.jboss.pressgang.ccms.model.Filter;
import org.jboss.pressgang.ccms.model.TranslatedTopicData;
import org.jboss.pressgang.ccms.rest.v1.constants.CommonFilterConstants;
import org.jboss.pressgang.ccms.restserver.utils.EntityUtilities;
import org.jboss.pressgang.ccms.seam.session.base.EntityQuery;
import org.jboss.pressgang.ccms.seam.utils.FilterUtilities;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

@Name("translatedTopicDataList")
public class TranslatedTopicDataList extends EntityQuery<TranslatedTopicData> {
    private static final long serialVersionUID = 4265050133041173177L;

    @In
    private EntityManager entityManager;

    private CriteriaQuery<TranslatedTopicData> criteriaQuery;

    public TranslatedTopicDataList() {
        this(null);
    }

    public TranslatedTopicDataList(final Integer limit) {
        construct(limit, null, new TranslatedTopicDataFilterQueryBuilder(entityManager));
    }

    public TranslatedTopicDataList(final Integer limit, final CriteriaQuery<TranslatedTopicData> criteriaQuery) {
        construct(limit, criteriaQuery, null);
    }

    protected void construct(final Integer limit, final CriteriaQuery<TranslatedTopicData> criteriaQuery,
            final TranslatedTopicDataFilterQueryBuilder filterQueryBuilder) {
        if (criteriaQuery == null) {
            // initialize filter home
            final Filter filter = EntityUtilities.populateFilter(entityManager,
                    FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap(), CommonFilterConstants.FILTER_ID,
                    CommonFilterConstants.MATCH_TAG, CommonFilterConstants.GROUP_TAG, CommonFilterConstants.CATEORY_INTERNAL_LOGIC,
                    CommonFilterConstants.CATEORY_EXTERNAL_LOGIC, CommonFilterConstants.MATCH_LOCALE, new TopicFieldFilter());

            /*
             * the filter may be null if an invalid variable was passed in the URL
             */
            if (filter != null) {
                // add the and and or categories clause to the default statement
                this.criteriaQuery = FilterUtilities.buildQuery(filter, filterQueryBuilder);
            } else {
                final CriteriaQuery<TranslatedTopicData> newCriteriaQuery = entityManager.getCriteriaBuilder().createQuery(
                        TranslatedTopicData.class);
                final Root<TranslatedTopicData> from = newCriteriaQuery.from(TranslatedTopicData.class);

                this.criteriaQuery = newCriteriaQuery.select(from);
            }
        } else {
            this.criteriaQuery = criteriaQuery;
        }

        if (limit != null && limit != -1) setMaxResults(limit);

        setCriteriaQuery(this.criteriaQuery);
    }
}
