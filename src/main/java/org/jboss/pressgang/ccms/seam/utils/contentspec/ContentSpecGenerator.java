package org.jboss.pressgang.ccms.seam.utils.contentspec;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.jboss.pressgang.ccms.contentspec.Chapter;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.Section;
import org.jboss.pressgang.ccms.contentspec.SpecTopic;
import org.jboss.pressgang.ccms.contentspec.TextNode;
import org.jboss.pressgang.ccms.docbook.compiling.DocbookBuildingOptions;
import org.jboss.pressgang.ccms.docbook.constants.DocbookBuilderConstants;
import org.jboss.pressgang.ccms.utils.common.CollectionUtilities;
import org.jboss.pressgang.ccms.utils.common.ExceptionUtilities;
import org.jboss.pressgang.ccms.utils.common.ZipUtilities;
import org.jboss.seam.Component;

import org.jboss.pressgang.ccms.model.Category;
import org.jboss.pressgang.ccms.model.Tag;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.model.TranslatedTopicData;

public class ContentSpecGenerator
{
	private EntityManager entityManager;
	
	public ContentSpecGenerator()
	{
		this.entityManager = (EntityManager) Component.getInstance("entityManager");
	}
	
	public ContentSpecGenerator(final EntityManager entityManager)
	{
		this.entityManager = entityManager;
	}
	
	/**
	 * Generates a Content Specification and fills it in using a set of topics. Once the content
	 * specification is assembled it then removes any empty sections.
	 * 
	 * Note: All topics should be of the same locale.
	 * 
	 * @param topics The collection of topics to be used in the generate of the Content Specification.
	 * @param docbookBuildingOptions The options that are to be used from a docbook build to generate the content spec.
	 * @return A ContentSpec object that represents the Content Specification. The toString() method can be used to get the text based version.
	 */
	public byte[] generateContentSpecFromTopics(final List<Topic> topics, final DocbookBuildingOptions docbookBuildingOptions)
	{
		assert topics != null;
		assert docbookBuildingOptions != null;
		
		final ContentSpec contentSpec = doFormattedTocPass(topics, docbookBuildingOptions);
		trimEmptySectionsFromContentSpecLevel(contentSpec.getBaseLevel());
		
		final HashMap<String, byte[]> files = new HashMap<String, byte[]>();
		/* Add the content spec to the Zip File */
		try
		{
			files.put("ContentSpec.contentspec", contentSpec.toString().getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException e)
		{
			/* UTF-8 is a valid format so this should exception should never get thrown */
		}
		
		/* Create the zip file */
		byte[] zipFile = null;
		try {
			zipFile = ZipUtilities.createZip(files);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return zipFile;
	}
	
	/**
	 * Generates a Content Specification and fills it in using a set of topics. Once the content
	 * specification is assembled it then removes any empty sections.
	 * 
	 * Note: All topics should be of the same locale.
	 * 
	 * @param topics The collection of topics to be used in the generate of the Content Specification.
	 * @param docbookBuildingOptions The options that are to be used from a docbook build to generate the content spec.
	 * @return A byte array as a ZIP file.
	 */
	public byte[] generateContentSpecFromTranslatedTopics(final List<TranslatedTopicData> topics, final DocbookBuildingOptions docbookBuildingOptions)
	{
		if (topics == null) return new byte[0];
		
		/* Group the topics into their locales */
		final Map<String, List<TranslatedTopicData>> groupedTranslatedTopics = new HashMap<String, List<TranslatedTopicData>>();
		for (final TranslatedTopicData topic : topics)
		{
			if (!groupedTranslatedTopics.containsKey(topic.getTranslationLocale()))
				groupedTranslatedTopics.put(topic.getTranslationLocale(), new ArrayList<TranslatedTopicData>());
			
			groupedTranslatedTopics.get(topic.getTranslationLocale()).add(topic);
		}
		
		final HashMap<String, byte[]> files = new HashMap<String, byte[]>();
		for (final String locale : groupedTranslatedTopics.keySet())
		{
			final ContentSpec contentSpec = doFormattedTranslatedTocPass(groupedTranslatedTopics.get(locale), locale, docbookBuildingOptions);
			trimEmptySectionsFromContentSpecLevel(contentSpec.getBaseLevel());
			
			/* Add the content spec to the Zip File */
			try
			{
				files.put(locale + ".contentspec", contentSpec.toString().getBytes("UTF-8"));
			}
			catch (UnsupportedEncodingException e)
			{
				/* UTF-8 is a valid format so this should exception should never get thrown */
			}
		}
		
		/* Create the zip file */
		byte[] zipFile = null;
		try {
			zipFile = ZipUtilities.createZip(files);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return zipFile;
	}
	
	/**
	 * Removes any levels from a Content Specification level that contain no content.
	 * 
	 * @param level The level to remove empty sections from.
	 */
	private void trimEmptySectionsFromContentSpecLevel(final Level level)
	{
		final List<Level> childLevels = new LinkedList<Level>(level.getChildLevels());
		for (final Level childLevel : childLevels)
		{
			if (!childLevel.hasSpecTopics())
				level.removeChild(childLevel);
			
			trimEmptySectionsFromContentSpecLevel(childLevel);
		}
	}
	
	/**
	 * Populates a content specifications level with all topics that match the
	 * criteria required by the TagRequirements.
	 * 
	 * @param topics The list of topics that can be matched to the level requirements.
	 * @param level The level to populate with topics.
	 * @param childRequirements The TagRequirements for this level based on the child requirements from the levels parent.
	 * @param displayRequirements The TagRequirements to display topics at this level.
	 */
	private void populateContentSpecLevel(final List<Topic> topics, final Level level, final TagRequirements childRequirements, final TagRequirements displayRequirements)
	{
		/*
		 * If this branch has no parent, then it is the top level and we don't
		 * add topics to it
		 */
		if (level.getParent() != null && childRequirements != null && displayRequirements != null && displayRequirements.hasRequirements())
		{
			final TagRequirements requirements = new TagRequirements();
			/* get the tags required to be a child of the parent toc levels */
			requirements.merge(childRequirements);
			/* and add the tags required to be displayed at this level */
			requirements.merge(displayRequirements);

			for (final Topic topic : topics)
			{
				boolean doesMatch = true;
				for (final Tag andTag : requirements.getMatchAllOf())
				{
					if (!topic.isTaggedWith(andTag.getId()))
					{
						doesMatch = false;
						break;
					}
				}

				if (doesMatch && requirements.getMatchOneOf().size() != 0)
				{
					for (final ArrayList<Tag> orBlock : requirements.getMatchOneOf())
					{
						if (orBlock.size() != 0)
						{
							boolean matchesOrBlock = false;
							for (final Tag orTag : orBlock)
							{
								if (topic.isTaggedWith(orTag.getId()))
								{
									matchesOrBlock = true;
									break;
								}
							}

							if (!matchesOrBlock)
							{
								doesMatch = false;
								break;
							}
						}
					}
				}

				if (doesMatch)
				{
					final SpecTopic specTopic = new SpecTopic(topic.getTopicId(), topic.getTopicTitle());
					level.appendSpecTopic(specTopic);
				}
			}
		}
	}

	/**
	 * Uses the technology, common names and concerns to build a basic content specification and then
	 * adds topics that match each levels criteria into the content specification.
	 * 
	 * @param clazz The Class of the list of topics. This should be either TopicV1 or TranslatedTopicV1.
	 * @param topics The collection of topics to be used in the generate of the Content Specification.
	 * @param docbookBuildingOptions The options that are to be used from a docbook build to generate the content spec.
	 * @return A ContentSpec object that represents the assembled Content Specification. The toString() method can be used to get the text based version.
	 */
	private ContentSpec doFormattedTocPass(final List<Topic> topics, final DocbookBuildingOptions docbookBuildingOptions)
	{
		try
		{
			/* The return value is a content specification. The 
			 * content specification defines the structure and 
			 * contents of the TOC.
			 */
			final ContentSpec retValue = new ContentSpec();
			
			/* Setup the basic content specification data */
			retValue.setTitle(docbookBuildingOptions.getBookTitle());
			retValue.setBrand("JBoss");
			retValue.setProduct(docbookBuildingOptions.getBookProduct());
			retValue.setVersion(docbookBuildingOptions.getBookProductVersion());
			retValue.setEdition(docbookBuildingOptions.getBookEdition() == null || docbookBuildingOptions.getBookEdition().isEmpty() ? null : docbookBuildingOptions.getBookEdition());
			retValue.setSubtitle(docbookBuildingOptions.getBookSubtitle() == null || docbookBuildingOptions.getBookSubtitle().isEmpty() ? null : docbookBuildingOptions.getBookSubtitle());
			retValue.setDtd("Docbook 4.5");
			retValue.setCopyrightHolder("Red Hat, Inc");
			retValue.setInjectSurveyLinks(docbookBuildingOptions.getInsertSurveyLink() == null ? false : docbookBuildingOptions.getInsertSurveyLink());
			
			// Add a blank line between chapters
			final TextNode blankLine = new TextNode("\n");
			retValue.getBaseLevel().appendChild(blankLine);

			/* Get the technology and common names categories */
			final Category technologyCategroy = entityManager.find(Category.class, DocbookBuilderConstants.TECHNOLOGY_CATEGORY_ID);
			final Category commonNamesCategory = entityManager.find(Category.class, DocbookBuilderConstants.COMMON_NAME_CATEGORY_ID);

			/*
			 * The top level TOC elements are made up of the technology and
			 * common name tags that are not encompassed by another tag. So here
			 * we get the tags out of the tech and common names categories, and
			 * pull outthose that are not encompassed.
			 */
			final List<Tag> topLevelTags = new ArrayList<Tag>();
			for (final Category category : new Category[]
			{ technologyCategroy, commonNamesCategory })
			{
				for (final Tag tag : category.getTags())
				{
					boolean isEmcompassed = false;
					for (final Tag parentTag : tag.getParentTags())
					{
						for (final Category parentTagCategory : parentTag.getCategories())
						{
							if (parentTagCategory.getId() == DocbookBuilderConstants.TECHNOLOGY_CATEGORY_ID || parentTagCategory.getId() == DocbookBuilderConstants.COMMON_NAME_CATEGORY_ID)
							{
								isEmcompassed = true;
								break;
							}
						}

						if (isEmcompassed)
							break;
					}

					/*
					 * This tag is not encompassed by any other tech or common
					 * name tags, so it is a candidate to appear on the top
					 * level of the TOC
					 */
					if (!isEmcompassed)
					{
						topLevelTags.add(tag);
					}
				}
			}


			/* Get the technology and common names categories */
			final Category concernCategory = entityManager.find(Category.class, DocbookBuilderConstants.CONCERN_CATEGORY_ID);

			/* Get the task reference and concept tag*/
			final Tag referenceTag = entityManager.find(Tag.class, DocbookBuilderConstants.REFERENCE_TAG_ID);
			final Tag conceptTag = entityManager.find(Tag.class, DocbookBuilderConstants.CONCEPT_TAG_ID);
			final Tag conceptualOverviewTag = entityManager.find(Tag.class, DocbookBuilderConstants.CONCEPTUALOVERVIEW_TAG_ID);
			final Tag taskTag = entityManager.find(Tag.class, DocbookBuilderConstants.TASK_TAG_ID);

			/* add TocFormatBranch objects for each top level tag */
			for (final Tag tag : topLevelTags)
			{
				/*
				 * Create the top level tag. This level is represented by the
				 * tags that are not encompased, and includes any topic that has
				 * that tag or any tag that is encompassed by this tag.
				 */
				final TagRequirements topLevelBranchTags = new TagRequirements((Tag) null, new ArrayList<Tag>()
				{
					private static final long serialVersionUID = 7499166852563779981L;

					{
						add(tag);
						addAll(tag.getChildTags());
					}
				});
				
				final Chapter topLevelTagChapter = new Chapter(tag.getTagName());
				retValue.appendChapter(topLevelTagChapter);
				
				populateContentSpecLevel(topics, topLevelTagChapter, topLevelBranchTags, null);

				for (final Tag concernTag : concernCategory.getTags())
				{
					/*
					 * the second level of the toc are the concerns, which will
					 * display the tasks and conceptual overviews beneath them
					 */
					final TagRequirements concernLevelChildTags = new TagRequirements(concernTag, (Tag) null);
					concernLevelChildTags.merge(topLevelBranchTags);
					final TagRequirements concernLevelDisplayTags = new TagRequirements((Tag) null, CollectionUtilities.toArrayList(conceptualOverviewTag, taskTag));
					
					final Section concernSection = new Section(concernTag.getTagName());
					topLevelTagChapter.appendChild(concernSection);
					
					populateContentSpecLevel(topics, concernSection, concernLevelChildTags, concernLevelDisplayTags);
					
					/*
					 * the third levels of the TOC are the concept and reference
					 * topics
					 */
					final Section conceptSection = new Section(conceptTag.getTagName());
					final Section referenceSection = new Section(referenceTag.getTagName());
					
					if (concernSection.getChildNodes().isEmpty())
						concernSection.appendChild(referenceSection);
					else
						concernSection.insertBefore(referenceSection, concernSection.getFirstSpecNode());
					concernSection.insertBefore(conceptSection, referenceSection);
					
					populateContentSpecLevel(topics, conceptSection, concernLevelChildTags, new TagRequirements(conceptTag, (Tag) null));
					populateContentSpecLevel(topics, referenceSection, concernLevelChildTags, new TagRequirements(referenceTag, (Tag) null));
				}
				
				// Add a blank line between chapters
				final TextNode chapterBlankLine = new TextNode("\n");
				topLevelTagChapter.appendChild(chapterBlankLine);
			}

			return retValue;
		}
		catch (final Exception ex)
		{
			ExceptionUtilities.handleException(ex);
			return null;
		}
	}
	
	/**
	 * Populates a content specifications level with all topics that match the
	 * criteria required by the TagRequirements.
	 * 
	 * @param topics The list of topics that can be matched to the level requirements.
	 * @param level The level to populate with topics.
	 * @param childRequirements The TagRequirements for this level based on the child requirements from the levels parent.
	 * @param displayRequirements The TagRequirements to display topics at this level.
	 */
	private void populateTranslatedContentSpecLevel(final List<TranslatedTopicData> topics, final Level level, final TagRequirements childRequirements, final TagRequirements displayRequirements)
	{
		/*
		 * If this branch has no parent, then it is the top level and we don't
		 * add topics to it
		 */
		if (level.getParent() != null && childRequirements != null && displayRequirements != null && displayRequirements.hasRequirements())
		{
			final TagRequirements requirements = new TagRequirements();
			/* get the tags required to be a child of the parent toc levels */
			requirements.merge(childRequirements);
			/* and add the tags required to be displayed at this level */
			requirements.merge(displayRequirements);

			for (final TranslatedTopicData topic : topics)
			{
				boolean doesMatch = true;
				for (final Tag andTag : requirements.getMatchAllOf())
				{
					if (!topic.getTranslatedTopic().getEnversTopic(entityManager).isTaggedWith(andTag.getId()))
					{
						doesMatch = false;
						break;
					}
				}

				if (doesMatch && requirements.getMatchOneOf().size() != 0)
				{
					for (final ArrayList<Tag> orBlock : requirements.getMatchOneOf())
					{
						if (orBlock.size() != 0)
						{
							boolean matchesOrBlock = false;
							for (final Tag orTag : orBlock)
							{
								if (topic.getTranslatedTopic().getEnversTopic(entityManager).isTaggedWith(orTag.getId()))
								{
									matchesOrBlock = true;
									break;
								}
							}

							if (!matchesOrBlock)
							{
								doesMatch = false;
								break;
							}
						}
					}
				}

				if (doesMatch)
				{
					final SpecTopic specTopic = new SpecTopic(topic.getTranslatedTopic().getTopicId(), topic.getTranslatedTopic().getEnversTopic(entityManager).getTopicTitle());
					level.appendSpecTopic(specTopic);
				}
			}
		}
	}

	/**
	 * Uses the technology, common names and concerns to build a basic content specification and then
	 * adds topics that match each levels criteria into the content specification.
	 * 
	 * @param clazz The Class of the list of topics. This should be either TopicV1 or TranslatedTopicV1.
	 * @param topics The collection of topics to be used in the generate of the Content Specification.
	 * @param docbookBuildingOptions The options that are to be used from a docbook build to generate the content spec.
	 * @return A ContentSpec object that represents the assembled Content Specification. The toString() method can be used to get the text based version.
	 */
	private ContentSpec doFormattedTranslatedTocPass(final List<TranslatedTopicData> topics, final String locale, final DocbookBuildingOptions docbookBuildingOptions)
	{
		try
		{
			/* The return value is a content specification. The 
			 * content specification defines the structure and 
			 * contents of the TOC.
			 */
			final ContentSpec retValue = new ContentSpec();
			
			/* Setup the basic content specification data */
			retValue.setTitle(docbookBuildingOptions.getBookTitle());
			retValue.setBrand("JBoss");
			retValue.setProduct(docbookBuildingOptions.getBookProduct());
			retValue.setVersion(docbookBuildingOptions.getBookProductVersion());
			retValue.setEdition(docbookBuildingOptions.getBookEdition() == null || docbookBuildingOptions.getBookEdition().isEmpty() ? null : docbookBuildingOptions.getBookEdition());
			retValue.setSubtitle(docbookBuildingOptions.getBookSubtitle() == null || docbookBuildingOptions.getBookSubtitle().isEmpty() ? null : docbookBuildingOptions.getBookSubtitle());
			retValue.setDtd("Docbook 4.5");
			retValue.setCopyrightHolder("Red Hat, Inc");
			retValue.setInjectSurveyLinks(docbookBuildingOptions.getInsertSurveyLink() == null ? false : docbookBuildingOptions.getInsertSurveyLink());
			
			// Add a blank line between chapters
			final TextNode blankLine = new TextNode("\n");
			retValue.appendChild(blankLine);

			/* Get the technology and common names categories */
			final Category technologyCategroy = entityManager.find(Category.class, DocbookBuilderConstants.TECHNOLOGY_CATEGORY_ID);
			final Category commonNamesCategory = entityManager.find(Category.class, DocbookBuilderConstants.COMMON_NAME_CATEGORY_ID);

			/*
			 * The top level TOC elements are made up of the technology and
			 * common name tags that are not encompassed by another tag. So here
			 * we get the tags out of the tech and common names categories, and
			 * pull outthose that are not encompassed.
			 */
			final List<Tag> topLevelTags = new ArrayList<Tag>();
			for (final Category category : new Category[]
			{ technologyCategroy, commonNamesCategory })
			{
				for (final Tag tag : category.getTags())
				{
					boolean isEmcompassed = false;
					for (final Tag parentTag : tag.getParentTags())
					{
						for (final Category parentTagCategory : parentTag.getCategories())
						{
							if (parentTagCategory.getId() == DocbookBuilderConstants.TECHNOLOGY_CATEGORY_ID || parentTagCategory.getId() == DocbookBuilderConstants.COMMON_NAME_CATEGORY_ID)
							{
								isEmcompassed = true;
								break;
							}
						}

						if (isEmcompassed)
							break;
					}

					/*
					 * This tag is not encompassed by any other tech or common
					 * name tags, so it is a candidate to appear on the top
					 * level of the TOC
					 */
					if (!isEmcompassed)
					{
						topLevelTags.add(tag);
					}
				}
			}


			/* Get the technology and common names categories */
			final Category concernCategory = entityManager.find(Category.class, DocbookBuilderConstants.CONCERN_CATEGORY_ID);

			/* Get the task reference and concept tag*/
			final Tag referenceTag = entityManager.find(Tag.class, DocbookBuilderConstants.REFERENCE_TAG_ID);
			final Tag conceptTag = entityManager.find(Tag.class, DocbookBuilderConstants.CONCEPT_TAG_ID);
			final Tag conceptualOverviewTag = entityManager.find(Tag.class, DocbookBuilderConstants.CONCEPTUALOVERVIEW_TAG_ID);
			final Tag taskTag = entityManager.find(Tag.class, DocbookBuilderConstants.TASK_TAG_ID);

			/* add TocFormatBranch objects for each top level tag */
			for (final Tag tag : topLevelTags)
			{
				/*
				 * Create the top level tag. This level is represented by the
				 * tags that are not encompased, and includes any topic that has
				 * that tag or any tag that is encompassed by this tag.
				 */
				final TagRequirements topLevelBranchTags = new TagRequirements((Tag) null, new ArrayList<Tag>()
				{
					private static final long serialVersionUID = 7499166852563779981L;

					{
						add(tag);
						addAll(tag.getChildTags());
					}
				});

				final Chapter topLevelTagChapter = new Chapter(tag.getTagName());
				retValue.appendChapter(topLevelTagChapter);
				
				populateTranslatedContentSpecLevel(topics, topLevelTagChapter, topLevelBranchTags, null);

				for (final Tag concernTag : concernCategory.getTags())
				{
					/*
					 * the second level of the toc are the concerns, which will
					 * display the tasks and conceptual overviews beneath them
					 */
					final TagRequirements concernLevelChildTags = new TagRequirements(concernTag, (Tag) null);
					concernLevelChildTags.merge(topLevelBranchTags);
					final TagRequirements concernLevelDisplayTags = new TagRequirements((Tag) null, CollectionUtilities.toArrayList(conceptualOverviewTag, taskTag));
					
					final Section concernSection = new Section(concernTag.getTagName());
					topLevelTagChapter.appendChild(concernSection);
					
					populateTranslatedContentSpecLevel(topics, concernSection, concernLevelChildTags, concernLevelDisplayTags);
					
					/*
					 * the third levels of the TOC are the concept and reference
					 * topics
					 */
					final Section conceptSection = new Section(conceptTag.getTagName());
					final Section referenceSection = new Section(referenceTag.getTagName());
					
					if (concernSection.getChildNodes().isEmpty())
						concernSection.appendChild(referenceSection);
					else
						concernSection.insertBefore(referenceSection, concernSection.getFirstSpecNode());
					concernSection.insertBefore(conceptSection, referenceSection);
					
					populateTranslatedContentSpecLevel(topics, conceptSection, concernLevelChildTags, new TagRequirements(conceptTag, (Tag) null));
					populateTranslatedContentSpecLevel(topics, referenceSection, concernLevelChildTags, new TagRequirements(referenceTag, (Tag) null));
				}
				
				// Add a blank line between chapters
				final TextNode chapterBlankLine = new TextNode("\n");
				topLevelTagChapter.appendChild(chapterBlankLine);
			}

			return retValue;
		}
		catch (final Exception ex)
		{
			ExceptionUtilities.handleException(ex);
			return null;
		}
	}
}

/**
 * This class defines the tags that a topic needs to have in order to be
 * displayed in a particular TOC level
 */
class TagRequirements
{
	/** One of these tags needs to be present */
	private final List<ArrayList<Tag>> matchOneOf = new ArrayList<ArrayList<Tag>>();
	/** All of these tags needs to be present */
	private final List<Tag> matchAllOf = new ArrayList<Tag>();

	public List<ArrayList<Tag>> getMatchOneOf()
	{
		return matchOneOf;
	}

	public List<Tag> getMatchAllOf()
	{
		return matchAllOf;
	}

	public TagRequirements(final ArrayList<Tag> matchAllOf, final ArrayList<Tag> matchOneOf)
	{
		if (matchOneOf != null)
			this.matchOneOf.add(matchOneOf);
		
		if (matchAllOf != null)
			this.matchAllOf.addAll(matchAllOf);
	}

	public TagRequirements(final ArrayList<Tag> matchAllOf, final Tag matchOneOf)
	{
		if (matchOneOf != null)
			this.matchOneOf.add(CollectionUtilities.toArrayList(matchOneOf));
		if (matchAllOf != null)
			this.matchAllOf.addAll(matchAllOf);
	}

	public TagRequirements(final Tag matchAllOf, final ArrayList<Tag> matchOneOf)
	{
		if (matchOneOf != null)
			this.matchOneOf.add(matchOneOf);
		if (matchAllOf != null)
			this.matchAllOf.add(matchAllOf);
	}

	public TagRequirements(final Tag matchAllOf, final Tag matchOneOf)
	{
		if (matchOneOf != null)
			this.matchOneOf.add(CollectionUtilities.toArrayList(matchOneOf));
		if (matchAllOf != null)
			this.matchAllOf.add(matchAllOf);
	}

	public TagRequirements()
	{

	}

	/**
	 * This method will merge the tag information stored in another
	 * TagRequirements object with the tag information stored in this object.
	 * 
	 * @param other
	 *            the other TagRequirements object to merge with
	 */
	public void merge(final TagRequirements other)
	{
		if (other != null)
		{
			this.matchAllOf.addAll(other.matchAllOf);
			this.matchOneOf.addAll(other.matchOneOf);
		}
	}
	
	public boolean hasRequirements()
	{
		return this.matchAllOf.size() != 0 || this.matchOneOf.size() != 0;
	}
}