package org.jboss.pressgang.ccms.seam.session;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.jboss.pressgang.ccms.restserver.entity.*;
import org.jboss.pressgang.ccms.seam.utils.EntityUtilities;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UICategoryData;

import org.jboss.seam.Component;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.framework.EntityHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Name("propertyTagHome")
public class PropertyTagHome extends EntityHome<PropertyTag>
{
    private static final Logger log = LoggerFactory.getLogger(PropertyTagHome.class);
	private static final long serialVersionUID = -519951425611644705L;

	private List<UICategoryData> categories = new ArrayList<UICategoryData>();

	public void setPropertyTagPropertyTagId(final Integer id)
	{
		setId(id);
	}

	public Integer getPropertyTagPropertyTagId()
	{
		return (Integer) getId();
	}

	@Override
	protected PropertyTag createInstance()
	{
		final PropertyTag retValue = new PropertyTag();
		return retValue;
	}

	public void load()
	{
		if (isIdDefined())
		{
			wire();
		}
	}

	public void wire()
	{
		getInstance();
	}

	public boolean isWired()
	{
		return true;
	}

	public PropertyTag getDefinedInstance()
	{
		return isIdDefined() ? getInstance() : null;
	}

	public List<UICategoryData> getCategories()
	{
		return categories;
	}

	public void populate()
	{
		EntityUtilities.populatePropertyTagCategories(this.getInstance(), categories);
	}
	
	@Override
	public String update()
	{		
		updateCategories();
		return super.update();
	}
	
	@Override
	public String persist()
	{		
		updateCategories();
		return super.persist();
	}

	private void updateCategories()
	{
		try
		{
			final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
			final PropertyTag tag = this.getInstance();

			final ArrayList<PropertyTagToPropertyTagCategory> removeCategoryies = new ArrayList<PropertyTagToPropertyTagCategory>();

			// find categories that we need to add
			for (final UICategoryData category : categories)
			{
				final Integer categoryId = category.getId();
				final Integer sortValue = category.getSort();
				final PropertyTagToPropertyTagCategory existingTagToCategory = tag.getCategory(categoryId);

				// if the mapping does not already exist, create it
				if (category.isSelected() && existingTagToCategory == null)
				{
					final PropertyTagCategory propertyTagCategory = entityManager.find(PropertyTagCategory.class, categoryId);

					final PropertyTagToPropertyTagCategory tagToCategory = new PropertyTagToPropertyTagCategory();
					tagToCategory.setPropertyTag(tag);
					tagToCategory.setPropertyTagCategory(propertyTagCategory);
					tagToCategory.setSorting(sortValue);

					tag.getPropertyTagToPropertyTagCategories().add(tagToCategory);
				}
				// if the mapping does exist, update it
				else if (category.isSelected() && existingTagToCategory != null)
				{
					existingTagToCategory.setSorting(sortValue);
				}
				/*
				 * if the mapping is to be removed, add it to the intermediate
				 * removeCategoryies collection
				 */
				else if (!category.isSelected() && existingTagToCategory != null)
				{
					removeCategoryies.add(existingTagToCategory);
				}

			}

			// remove the category mapping, from both the tag and the category
			for (final PropertyTagToPropertyTagCategory removeCategory : removeCategoryies)
			{
				tag.getPropertyTagToPropertyTagCategories().remove(removeCategory);
				removeCategory.getPropertyTagCategory().getPropertyTagToPropertyTagCategories().remove(removeCategory);
			}

		}
		catch (final Exception ex)
		{
			log.error("Probably could not find the tag", ex);
		}
	}

}
