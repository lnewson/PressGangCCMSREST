package org.jboss.pressgang.ccms.seam.session;

import org.jboss.pressgang.ccms.restserver.entity.*;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.framework.EntityQuery;
import java.util.Arrays;

@Name("tagList")
public class TagList extends EntityQuery<Tag> {
    /** Serializable version identifier */
    private static final long serialVersionUID = -5705132860028816800L;

    private static final String[] RESTRICTIONS = { "lower(tag.tagName) like lower(concat('%',#{tagList.tag.tagName},'%'))",
            "lower(tag.tagDescription) like lower(concat('%',#{tagList.tag.tagDescription},'%'))", };

    private Tag tag = new Tag();

    public TagList() {
        this(org.jboss.pressgang.ccms.restserver.utils.Constants.DEFAULT_PAGING_SIZE);
    }

    public TagList(int limit) {
        setEjbql(Tag.SELECT_ALL_QUERY);
        setRestrictionExpressionStrings(Arrays.asList(RESTRICTIONS));
        if (limit != -1)
            setMaxResults(limit);
    }

    public Tag getTag() {
        return tag;
    }
}
