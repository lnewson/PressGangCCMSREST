package org.jboss.pressgang.ccms.seam.utils.structures.tags;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.jboss.pressgang.ccms.utils.common.CollectionUtilities;
import org.jboss.pressgang.ccms.utils.structures.Pair;
import org.jboss.seam.Component;

import org.jboss.pressgang.ccms.restserver.entity.PropertyTag;
import org.jboss.pressgang.ccms.restserver.entity.PropertyTagCategory;
import org.jboss.pressgang.ccms.restserver.entity.PropertyTagToPropertyTagCategory;
import org.jboss.pressgang.ccms.restserver.utils.Constants;
import org.jboss.pressgang.ccms.seam.utils.EntityUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a collection of categories and their tags.
 */
public class UICategoriesData
{
    private static final Logger log = LoggerFactory.getLogger(UICategoriesData.class);
    
	private List<UICategoryData> categories = new ArrayList<UICategoryData>();

	public List<UICategoryData> getCategories()
	{
		return categories;
	}

	public void setCategories(final List<UICategoryData> categories)
	{
		this.categories = categories;
	}

	/**
	 * @return A collection of Tags that were selected in the UI
	 */
	public List<PropertyTag> getSelectedTags()
	{
		final List<PropertyTag> selectedTagObjects = new ArrayList<PropertyTag>();

		for (final UICategoryData cat : categories)
		{
			// find the selected tags
			for (final UITagData tagId : cat.getTags())
			{
				// if tag is selected
				if (tagId.isSelected())
					selectedTagObjects.add(EntityUtilities.getPropertyTagFromId(tagId.getId()));
			}
		}

		return selectedTagObjects;
	}

	public List<Pair<PropertyTag, UITagData>> getExtendedSelectedTags()
	{
		final List<Pair<PropertyTag, UITagData>> selectedTagObjects = new ArrayList<Pair<PropertyTag, UITagData>>();

		for (final UICategoryData cat : categories)
		{
			// find the selected tags
			for (final UITagData tagId : cat.getTags())
			{
				// if tag is selected
				if (tagId.isSelected())
					selectedTagObjects.add(Pair.newPair(EntityUtilities.getPropertyTagFromId(tagId.getId()), tagId));
			}
		}

		return selectedTagObjects;
	}

	public List<Pair<PropertyTag, UITagData>> getAddedOrModifiedTags(final Set<PropertyTagToPropertyTagCategory> existingTags)
	{
		final List<Pair<PropertyTag, UITagData>> selectedTags = getExtendedSelectedTags();

		// now make a note of the additions
		final List<Pair<PropertyTag, UITagData>> addTags = new ArrayList<Pair<PropertyTag, UITagData>>();
		for (final Pair<PropertyTag, UITagData> selectedTagData : selectedTags)
		{
			final PropertyTag selectedTag = selectedTagData.getFirst();
			final Integer sorting = selectedTagData.getSecond().getNewSort();

			/*
			 * Loop over the TagToCategory collection, and see if the tag we
			 * have selected exists with the same sorting order
			 */
			boolean found = false;
			for (final PropertyTagToPropertyTagCategory tagToCategory : existingTags)
			{
				if (tagToCategory.getPropertyTag().equals(selectedTag) && CollectionUtilities.isEqual(tagToCategory.getSorting(), sorting))
				{
					found = true;
					break;
				}
			}

			if (!found)
			{
				addTags.add(selectedTagData);
			}
		}

		return addTags;
	}

	public List<PropertyTag> getAddedTags(final Set<PropertyTagToPropertyTagCategory> existingTags)
	{
		final List<PropertyTag> selectedTags = getSelectedTags();

		/* make a note of the additions */
		final ArrayList<PropertyTag> addTags = new ArrayList<PropertyTag>();

		for (final PropertyTag selectedTag : selectedTags)
		{
			boolean found = false;
			for (final PropertyTagToPropertyTagCategory existingTag : existingTags)
			{
				final PropertyTag propertyTag = existingTag.getPropertyTag();
				if (selectedTag.equals(propertyTag))
				{
					found = true;
					break;
				}
			}

			if (!found)
				addTags.add(selectedTag);
		}

		return addTags;
	}

	public List<PropertyTag> getRemovedTags(final Set<PropertyTagToPropertyTagCategory> existingTags)
	{
		final List<PropertyTag> selectedTags = this.getSelectedTags();

		/* make a note of the tags that were removed */
		final List<PropertyTag> removeTags = new ArrayList<PropertyTag>();
		for (final PropertyTagToPropertyTagCategory existingTag : existingTags)
		{
			final PropertyTag propertyTag = existingTag.getPropertyTag();

			if (!selectedTags.contains(propertyTag))
			{
				/*
				 * add to external collection to avoid modifying a collection
				 * while looping over it
				 */
				removeTags.add(propertyTag);
			}

		}

		return removeTags;
	}

	public void populateTopicTags(final PropertyTagCategory displayPropertyTagCategory)
	{
		try
		{
			final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
			
			@SuppressWarnings("unchecked")
			final List<PropertyTagCategory> propertyTagCategories = entityManager.createQuery(PropertyTagCategory.SELECT_ALL_QUERY).getResultList();
			
			@SuppressWarnings("unchecked")
			final List<PropertyTag> propertyTags = entityManager.createQuery(PropertyTag.SELECT_ALL_QUERY).getResultList();

			/* add the uncategorised tags first */
			final UICategoryData uncategorisedUICategoryData = new UICategoryData(Constants.UNCATEGORISED_PROPERTY_TAG_CATEGORY_SELECT_ITEM_LABEL, "", 0, 0, false);
			for (final PropertyTag propertyTag : propertyTags)
			{
				if (propertyTag.getPropertyTagToPropertyTagCategories().size() == 0)
				{
					final UITagData uITagData = new UITagData(propertyTag.getPropertyTagName(), propertyTag.getPropertyTagId());
					uITagData.setSelected(displayPropertyTagCategory.hasPropertyTag(propertyTag));
					uncategorisedUICategoryData.addTag(uITagData);
				}
			}
			if (uncategorisedUICategoryData.getTags().size() != 0)
				this.categories.add(uncategorisedUICategoryData);

			/* now add the categorised tags */
			for (final PropertyTagCategory propertyTagCategory : propertyTagCategories)
			{
				final String name = propertyTagCategory.getPropertyTagCategoryName();
				final String description = propertyTagCategory.getPropertyTagCategoryDescription();
				final Integer id = propertyTagCategory.getPropertyTagCategoryId();

				final UICategoryData uICategoryData = new UICategoryData(name, description, id, 0, false);

				for (final PropertyTagToPropertyTagCategory propertyTagToPropertyTagCategory : propertyTagCategory.getPropertyTagToPropertyTagCategories())
				{
					final PropertyTag propertyTag = propertyTagToPropertyTagCategory.getPropertyTag();
					final UITagData uITagData = new UITagData(propertyTag.getPropertyTagName(), propertyTag.getPropertyTagId());
					uITagData.setSelected(displayPropertyTagCategory.hasPropertyTag(propertyTag));
					uICategoryData.addTag(uITagData);
				}

				this.categories.add(uICategoryData);
			}

		}
		catch (final Exception ex)
		{
			log.error("A catch all Exception", ex);
		}
	}
}
