package org.jboss.pressgangccms.restserver.zanata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.TransactionManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.jboss.pressgangccms.docbook.messaging.TopicRendererType;
import org.jboss.pressgangccms.restserver.entities.Topic;
import org.jboss.pressgangccms.restserver.entities.TranslatedTopic;
import org.jboss.pressgangccms.restserver.entities.TranslatedTopicData;
import org.jboss.pressgangccms.restserver.entities.TranslatedTopicString;
import org.jboss.pressgangccms.restserver.renderer.TopicQueueRenderer;
import org.jboss.pressgangccms.restserver.utils.SkynetExceptionUtilities;
import org.jboss.pressgangccms.utils.common.XMLUtilities;
import org.jboss.pressgangccms.utils.concurrency.WorkQueue;
import org.jboss.pressgangccms.utils.constants.CommonConstants;
import org.jboss.pressgangccms.zanata.ZanataInterface;
import org.w3c.dom.Document;
import org.zanata.common.LocaleId;
import org.zanata.rest.dto.resource.Resource;
import org.zanata.rest.dto.resource.TextFlow;
import org.zanata.rest.dto.resource.TextFlowTarget;
import org.zanata.rest.dto.resource.TranslationsResource;


public class ZanataPullTopicThread implements Runnable
{
	private List<Integer> translatedTopics;
	private final ZanataInterface zanataInterface = new ZanataInterface();
	
	public ZanataPullTopicThread(final List<Integer> translatedTopics)
	{
		this.translatedTopics = Collections.unmodifiableList(translatedTopics);
	}

	@Override
	public void run()
	{
		synchronized (translatedTopics)
		{

			final int total = translatedTopics.size();
			int current = 0;

			TransactionManager transactionManager = null;

			try
			{
				final InitialContext initCtx = new InitialContext();
				final EntityManagerFactory entityManagerFactory = (EntityManagerFactory) initCtx.lookup("java:jboss/EntityManagerFactory");
				transactionManager = (TransactionManager) initCtx.lookup("java:jboss/TransactionManager");
				
				final List<Integer> processedIds = new ArrayList<Integer>();

				/* for each translated topic... */
				for (final Integer topicId : this.translatedTopics)
				{
					++current;
					final int progress = (int) ((float) current / (float) total * 100.0f);
					System.out.println("Pull from Zanata Progress - Topic ID " + topicId + ": " + current + " of " + total + " (" + progress + "%)");

					/* ... and each locale */
					final List<LocaleId> locales = zanataInterface.getZanataLocales();
					for (final LocaleId locale : locales)
					{
						/* attempt to pull down the latest translations */
						final Integer translatedTopicDataId = processTopic(transactionManager, entityManagerFactory, topicId, locale.toString());
						if (translatedTopicDataId != null)
						{
							processedIds.add(translatedTopicDataId);							
						}
					}
				}
				
				for (final Integer id : processedIds)
				{
					WorkQueue.getInstance().execute(TopicQueueRenderer.createNewInstance(id, TopicRendererType.TRANSLATEDTOPIC));
				}
			}
			catch (final Exception ex)
			{
				SkynetExceptionUtilities.handleException(ex, false, "Probably an error looking up the EntityManagerFactory or the TransactionManager");
			}

		}
	}

	private Integer processTopic(final TransactionManager transactionManager, final EntityManagerFactory entityManagerFactory, final Integer translatedTopicId,
			final String locale)
	{
		EntityManager entityManager = null;

		try
		{
			Integer retValue = null;

			transactionManager.begin();

			entityManager = entityManagerFactory.createEntityManager();
			final AuditReader reader = AuditReaderFactory.get(entityManager);

			/* find the original TranslatedTopic */
			final TranslatedTopic topic = entityManager.find(TranslatedTopic.class, translatedTopicId);
			
			if (topic != null)
			{
				/* ... find the matching historical envers topic */
				final Topic historicalTopic = reader.find(Topic.class, topic.getTopicId(), topic.getTopicRevision());
				
				/* Don't get translations for the original locale as it won't need translating */
				if (historicalTopic.getTopicLocale().equals(locale))
				{
					transactionManager.commit();
					return null;
				}

				/* find a translation */
				final TranslationsResource translationsResource = zanataInterface.getTranslations(topic.getZanataId(), LocaleId.fromJavaName(locale));
				/* and find the original resource */
				final Resource originalTextResource = zanataInterface.getZanataResource(topic.getZanataId());

				if (translationsResource != null && originalTextResource != null)
				{
					final Set<TranslatedTopicData> translatedTopicDataEntities = topic.getTranslatedTopicDataEntities();

					/* attempt to find an existing TranslatedTopicData entity */
					TranslatedTopicData translatedTopicData = null;

					for (final TranslatedTopicData myTranslatedTopicData : translatedTopicDataEntities)
					{
						if (myTranslatedTopicData.getTranslationLocale().equals(locale))
						{
							translatedTopicData = myTranslatedTopicData;
							break;
						}
					}

					/*
					 * if an existing TranslatedTopicData entity does not
					 * exist, create one
					 */
					if (translatedTopicData == null)
					{
						translatedTopicData = new TranslatedTopicData();
						translatedTopicData.setTranslatedTopic(topic);
						translatedTopicData.setTranslationLocale(locale);
						translatedTopicDataEntities.add(translatedTopicData);
					}

					/*
					 * a mapping of the original strings to their translations
					 */
					final Map<String, String> translations = new HashMap<String, String>();

					final List<TextFlowTarget> textFlowTargets = translationsResource.getTextFlowTargets();
					final List<TextFlow> textFlows = originalTextResource.getTextFlows();
					
					double wordCount = 0;
					double totalWordCount = 0;

					/* map the translation to the original resource */
					for (final TextFlow textFlow : textFlows)
					{
						for (final TextFlowTarget textFlowTarget : textFlowTargets)
						{
							if (textFlowTarget.getResId().equals(textFlow.getId()))
							{
								translations.put(textFlow.getContent(), textFlowTarget.getContent());
								wordCount += textFlow.getContent().split(" ").length;
								break;
							}
						}
						totalWordCount += textFlow.getContent().split(" ").length;
					}
					
					/* Set the translation completion status */
					translatedTopicData.setTranslationPercentage( (int) ( wordCount / totalWordCount * 100.0f) );
					
					/* remove the existing TranslatedTopicString entities */
					translatedTopicData.getTranslatedTopicStrings().clear();
					
					/* save the strings to TranslatedTopicString entities */					
					for (final String originalText : translations.keySet())
					{
						final String translation = translations.get(originalText);
						
						final TranslatedTopicString translatedTopicString = new TranslatedTopicString();
						translatedTopicString.setTranslatedTopicData(translatedTopicData);
						translatedTopicString.setOriginalString(originalText);
						translatedTopicString.setTranslatedString(translation);
						
						translatedTopicData.getTranslatedTopicStrings().add(translatedTopicString);
					}

					/* get a Document from the stored historical XML */
					historicalTopic.syncXML();
					historicalTopic.initializeTempTopicXMLDoc();
					final Document xml = historicalTopic.getTempTopicXMLDoc();

					/*
					 * replace the translated strings, and save the result into
					 * the TranslatedTopicData entity
					 */
					if (xml != null)
					{
						XMLUtilities.replaceTranslatedStrings(xml, translations);
						translatedTopicData.setTranslatedXml(XMLUtilities.convertDocumentToString(xml, CommonConstants.XML_ENCODING));
					}

					/* persist the changes */
					entityManager.persist(translatedTopicData);

					/*
					 * make a note of the TranslatedTopicData entities that
					 * have been changed, so we can render them
					 */
					retValue = translatedTopicData.getTranslatedTopicDataId();
				}
			}

			transactionManager.commit();

			return retValue;
		}
		catch (final Exception ex)
		{
			SkynetExceptionUtilities.handleException(ex, false, "Probably an error looking up the manager objects");

			try
			{
				transactionManager.rollback();
			}
			catch (final Exception ex2)
			{
				SkynetExceptionUtilities.handleException(ex2, false, "There was an exception rolling back the transaction");
			}
		}
		finally
		{
			if (entityManager != null)
				entityManager.close();
		}

		return null;
	}

}