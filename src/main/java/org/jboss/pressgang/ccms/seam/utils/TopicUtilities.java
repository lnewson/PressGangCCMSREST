package org.jboss.pressgang.ccms.seam.utils;

import static ch.lambdaj.Lambda.filter;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.Collections;

import javax.persistence.EntityManager;

import org.jboss.pressgang.ccms.model.StringConstants;
import org.jboss.pressgang.ccms.model.Tag;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.model.TopicSourceUrl;
import org.jboss.pressgang.ccms.model.TopicToTag;
import org.jboss.pressgang.ccms.model.TopicToTopic;
import org.jboss.pressgang.ccms.model.TopicToTopicSourceUrl;
import org.jboss.pressgang.ccms.model.sort.TopicToTagTagIDSort;
import org.jboss.pressgang.ccms.model.sort.TopicToTopicRelatedTopicIDSort;
import org.jboss.pressgang.ccms.utils.common.XMLUtilities;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.seam.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class TopicUtilities extends org.jboss.pressgang.ccms.restserver.utils.TopicUtilities {
    private static final  Logger log = LoggerFactory.getLogger(TopicUtilities.class);
    
    /**
     * The string that identifies the automatically generated comment added to
     * the end of the XML
     */
    private static final String DETAILS_COMMENT_NODE_START = " Generated Topic Details";
    /** The string constant that is used as a conceptual overview template */
    private static final Integer CONCEPTUAL_OVERVIEW_TOPIC_STRINGCONSTANTID = 11;
    /** The string constant that is used as a reference template */
    private static final Integer REFERENCE_TOPIC_STRINGCONSTANTID = 12;
    /** The string constant that is used as a task template */
    private static final Integer TASK_TOPIC_STRINGCONSTANTID = 13;
    /** The string constant that is used as a concept template */
    private static final Integer CONCEPT_TOPIC_STRINGCONSTANTID = 14;
    
    public static void initializeFromTemplate(final Topic topic)
    {
        try
        {
            final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");

            if (filter(having(on(TopicToTag.class).getTag().getTagId(), equalTo(Constants.CONCEPT_TAG_ID)), topic.getTopicToTags()).size() != 0)
            {
                topic.setTopicXML(entityManager.find(StringConstants.class, CONCEPT_TOPIC_STRINGCONSTANTID).getConstantValue());
            }
            else if (filter(having(on(TopicToTag.class).getTag().getTagId(), equalTo(Constants.TASK_TAG_ID)), topic.getTopicToTags()).size() != 0)
            {
                topic.setTopicXML(entityManager.find(StringConstants.class, TASK_TOPIC_STRINGCONSTANTID).getConstantValue());
            }
            else if (filter(having(on(TopicToTag.class).getTag().getTagId(), equalTo(Constants.REFERENCE_TAG_ID)), topic.getTopicToTags()).size() != 0)
            {
                topic.setTopicXML(entityManager.find(StringConstants.class, REFERENCE_TOPIC_STRINGCONSTANTID).getConstantValue());
            }
            else if (filter(having(on(TopicToTag.class).getTag().getTagId(), equalTo(Constants.CONCEPTUALOVERVIEW_TAG_ID)), topic.getTopicToTags()).size() != 0)
            {
                topic.setTopicXML(entityManager.find(StringConstants.class, CONCEPTUAL_OVERVIEW_TOPIC_STRINGCONSTANTID).getConstantValue());
            }

            processXML(entityManager, topic);

            addDetailsCommentToXML(topic);
        }
        catch (final Exception ex)
        {
            log.error("Probably couldn't find one of the string constants", ex);
        }
    }
    
    public static void addDetailsCommentToXML(final Topic topic)
    {
        Document doc = null;
        try
        {
            doc = XMLUtilities.convertStringToDocument(topic.getTopicXML());
        }
        catch (Exception ex)
        {
            log.error("Failed to convert a XML String to a DOM Document.", ex);
        }
        if (doc != null)
        {
            String detailsCommentContent = DETAILS_COMMENT_NODE_START + "\n\n" + "GENERAL DETAILS\n" + "\n" + "Topic ID: " + topic.getTopicId() + "\n" + "Topic Title: " + topic.getTopicTitle() + "\nTopic Description: " + topic.getTopicText() + "\n\n" + "TOPIC TAGS\n" + "\n";

            final ArrayList<TopicToTag> sortedTags = new ArrayList<TopicToTag>(topic.getTopicToTags());
            Collections.sort(sortedTags, new TopicToTagTagIDSort());

            for (final TopicToTag topicToTag : sortedTags)
            {
                final Tag tag = topicToTag.getTag();
                detailsCommentContent += tag.getTagId() + ": " + tag.getTagName() + "\n";
            }

            detailsCommentContent += "\nSOURCE URLS\n\n";
            for (final TopicToTopicSourceUrl topicToSourceUrl : topic.getTopicToTopicSourceUrls())
            {
                final TopicSourceUrl url = topicToSourceUrl.getTopicSourceUrl();
                detailsCommentContent += (url.getTitle() == null || url.getTitle().length() == 0 ? "Source URL: " : url.getTitle() + ": ");
                detailsCommentContent += url.getSourceUrl() + "\n";
            }

            final ArrayList<TopicToTopic> sortedTopics = new ArrayList<TopicToTopic>(topic.getParentTopicToTopics());
            Collections.sort(sortedTopics, new TopicToTopicRelatedTopicIDSort());

            detailsCommentContent += "\nRELATED TOPICS\n\n";
            for (final TopicToTopic topicToTopic : sortedTopics)
            {
                final Topic relatedTopic = topicToTopic.getRelatedTopic();
                detailsCommentContent += relatedTopic.getTopicId() + ": " + relatedTopic.getTopicTitle() + "\n";
            }

            detailsCommentContent += "\n";

            final Node detailsComment = doc.createComment(detailsCommentContent);

            boolean foundComment = false;
            for (final Node comment : XMLUtilities.getComments(doc))
            {
                final String commentContent = comment.getTextContent();
                if (commentContent.startsWith(DETAILS_COMMENT_NODE_START))
                {
                    foundComment = true;
                    comment.getParentNode().replaceChild(detailsComment, comment);
                    break;
                }
            }

            if (!foundComment)
            {
                doc.getDocumentElement().appendChild(detailsComment);
            }

            topic.setTopicXML(XMLUtilities.convertDocumentToString(doc, CommonConstants.XML_ENCODING));
        }
    }
}
