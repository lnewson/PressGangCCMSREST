package org.jboss.pressgang.ccms.seam.session;

import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.restserver.filter.TopicFieldFilter;
import org.jboss.pressgang.ccms.seam.session.base.EntityQuery;
import org.jboss.pressgang.ccms.seam.utils.Constants;
import org.jboss.seam.annotations.Name;

import java.util.Arrays;
import javax.persistence.criteria.CriteriaQuery;

@Name("topicList")
public class TopicList extends EntityQuery<Topic> {
    /** Serializable version identifier */
    private static final long serialVersionUID = 4132574852922621495L;

    private static final String EJBQL = "select topic from Topic topic";

    private static final String[] RESTRICTIONS = {
            // "topic.topicId like concat('%', #{topicList.topic.topicId}, '%')",
            "topic.topicId in (#{topicList.topic.topicIds})",
            "lower(topic.topicTitle) like lower(concat('%', #{topicList.topic.topicTitle}, '%'))",
            "lower(topic.topicText) like lower(concat('%', #{topicList.topic.topicDescription}, '%'))" };

    protected TopicFieldFilter topic = new TopicFieldFilter();

    public TopicList(final Integer limit, final String constructedEJBQL, final TopicFieldFilter topic,
            final boolean useRestrictions) {
        setEjbql(constructedEJBQL);

        if (useRestrictions)
            setRestrictionExpressionStrings(Arrays.asList(RESTRICTIONS));

        /* use the default paging size if no limit is specified */
        if (limit == null)
            setMaxResults(Constants.DEFAULT_PAGING_SIZE);
        /* if the limit is not -1 (or no paging), set the limit */
        else if (limit != -1)
            setMaxResults(limit);

        if (topic != null)
            this.topic = topic;
    }

    public TopicList(final Integer limit, final CriteriaQuery<Topic> query, final TopicFieldFilter topic) {
        setCriteriaQuery(query);

        /* use the default paging size if no limit is specified */
        if (limit == null)
            setMaxResults(Constants.DEFAULT_PAGING_SIZE);
        /* if the limit is not -1 (or no paging), set the limit */
        else if (limit != -1)
            setMaxResults(limit);

        if (topic != null)
            this.topic = topic;
    }

    public TopicList(final int limit, final String constructedEJBQL) {
        this(limit, constructedEJBQL, null, true);
    }

    public TopicList(final int limit) {
        this(limit, EJBQL, null, true);
    }

    public TopicList() {
        this(null, EJBQL, null, true);
    }

    public TopicFieldFilter getTopic() {
        return topic;

    }
}
