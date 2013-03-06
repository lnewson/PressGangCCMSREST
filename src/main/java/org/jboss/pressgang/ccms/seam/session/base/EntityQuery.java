package org.jboss.pressgang.ccms.seam.session.base;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.pressgang.ccms.filter.utils.JPAUtils;
import org.jboss.seam.annotations.Transactional;

public class EntityQuery<T> extends org.jboss.seam.framework.EntityQuery<T> {

    private static final long serialVersionUID = -6445123344140549298L;

    protected CriteriaQuery<T> query = null;
    protected CriteriaQuery<Long> countQuery = null;
    protected Map<String, Object> parameters = new HashMap<String, Object>();

    public void setCriteriaQuery(final CriteriaQuery<T> criteriaQuery) {
        this.query = criteriaQuery;
        this.countQuery = JPAUtils.countCriteria(getEntityManager(), criteriaQuery);
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameter(final String key, final Object value) {
        if (key != null && value != null) parameters.put(key, value);
    }

    public void setParameters(final Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    protected Query createQuery() {
        if (this.query != null) {
            joinTransaction();

            final Query query = getEntityManager().createQuery(this.query);

            setParameters(query, getParameters());

            if (getFirstResult() != null) query.setFirstResult(getFirstResult());
            if (getMaxResults() != null) query.setMaxResults(getMaxResults() + 1); // add one, so we can tell if there is another page
            if (getHints() != null) {
                for (Map.Entry<String, String> me : getHints().entrySet()) {
                    query.setHint(me.getKey(), me.getValue());
                }
            }

            return query;
        } else {
            return super.createQuery();
        }
    }

    @Override
    protected javax.persistence.Query createCountQuery() {
        if (this.countQuery != null) {
            joinTransaction();

            final Query query = getEntityManager().createQuery(this.countQuery);

            setParameters(query, getParameters());

            return query;
        } else {
            return super.createCountQuery();
        }
    }

    private void setParameters(final Query query, Map<String, Object> parameters) {
        for (final Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    @Override
    @Transactional
    public boolean isNextExists() {
        /*
         * This is a fixed version of the isNextExists() method, since it doesn't initialise the resultList before checking if a
         * next page exists
         */
        getResultList();
        return super.isNextExists();
    }
}
