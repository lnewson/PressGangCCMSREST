package org.jboss.pressgang.ccms.seam.utils.structures.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.jboss.pressgang.ccms.utils.common.CollectionUtilities;
import org.jboss.pressgang.ccms.model.Category;
import org.jboss.pressgang.ccms.model.Filter;
import org.jboss.pressgang.ccms.model.FilterCategory;
import org.jboss.pressgang.ccms.model.FilterTag;
import org.jboss.pressgang.ccms.model.Project;
import org.jboss.pressgang.ccms.model.Tag;
import org.jboss.pressgang.ccms.model.TagToCategory;
import org.jboss.pressgang.ccms.model.TagToProject;
import org.jboss.pressgang.ccms.model.Topic;
import org.jboss.pressgang.ccms.rest.v1.constants.CommonFilterConstants;
import org.jboss.pressgang.ccms.utils.structures.Pair;
import org.jboss.seam.Component;

import org.jboss.pressgang.ccms.restserver.utils.Constants;
import org.jboss.pressgang.ccms.seam.utils.EntityUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the collection of projects that form the top level of
 * the tagging structure, as used by the GUI. These classes differ from the
 * underlying entities in their relationships (e.g. the project and category
 * database entities have no relationship, while the UI tag display does have a
 * direct relationship between projects and categories), and include extra data
 * such as whether a tag/category has been selected or not.
 */
public class UIProjectsData
{
    private static Logger log = LoggerFactory.getLogger(UIProjectsData.class);
    
	/**
	 * A collection of UIProjectCategoriesData objects, which represent the
	 * categories that hold tags assigned to a project. A PriorityQueue is used
	 * to get automatic sorting.
	 */
	private List<UIProjectData> projectCategories = new ArrayList<UIProjectData>();

	public void populateTagTags(final Tag tag)
	{
		this.populateTags(tag.getTags(), null, true);
	}
	
	public void populateTagTags(final Category category)
	{
		this.populateTags(category.getTags(), null, true);
	}
	
	public List<UIProjectData> getProjectCategories()
	{
		return projectCategories;
	}

	public void setProjectCategories(final List<UIProjectData> categories)
	{
		this.projectCategories = categories;
	}

	public void clear()
	{
		projectCategories.clear();
	}

	/**
	 * @return A collection of Tags that were selected in the UI
	 */
	public List<Tag> getSelectedTags()
	{
		final List<Tag> selectedTagObjects = new ArrayList<Tag>();

		for (final UIProjectData project : this.getProjectCategories())
		{
			for (final UICategoryData cat : project.getCategories())
			{
				// find the selected tags
				for (final UITagData tagId : cat.getTags())
				{
					// if tag is selected
					if (tagId.isSelected())
						selectedTagObjects.add(EntityUtilities.getTagFromId(tagId.getId()));
				}
			}
		}

		return selectedTagObjects;
	}

	/**
	 * @return A collection of Tags that were selected in the UI, paired with a
	 *         UITagData object
	 */
	public List<Pair<Tag, UITagData>> getExtendedSelectedTags()
	{
		final List<Pair<Tag, UITagData>> selectedTagObjects = new ArrayList<Pair<Tag, UITagData>>();

		for (final UIProjectData project : this.getProjectCategories())
		{
			for (final UICategoryData cat : project.getCategories())
			{
				// find the selected tags
				for (final UITagData tagId : cat.getTags())
				{
					// if tag is selected
					if (tagId.isSelected())
						selectedTagObjects.add(Pair.newPair(EntityUtilities.getTagFromId(tagId.getId()), tagId));
				}
			}
		}

		return selectedTagObjects;
	}

	/**
	 * @param existingTagsExtended
	 *            A collection of the existing TagToCategory objects held by a
	 *            Category
	 * @return A collection of selected Tags paired with the UITagData (that
	 *         includes additional information such as the sorting order) that
	 *         either do not exist in the existingTagsExtended collection, or
	 *         exist in the existingTagsExtended collection with a different
	 *         sorting order
	 */
	public List<Pair<Tag, UITagData>> getAddedOrModifiedTags(final Set<TagToCategory> existingTagsExtended)
	{
		final List<Pair<Tag, UITagData>> selectedTags = getExtendedSelectedTags();

		// now make a note of the additions
		final List<Pair<Tag, UITagData>> addTags = new ArrayList<Pair<Tag, UITagData>>();
		for (final Pair<Tag, UITagData> selectedTagData : selectedTags)
		{
			final Tag selectedTag = selectedTagData.getFirst();
			final Integer sorting = selectedTagData.getSecond().getNewSort();

			/*
			 * Loop over the TagToCategory collection, and see if the tag we
			 * have selected exists with the same sorting order
			 */
			boolean found = false;
			for (final TagToCategory tagToCategory : existingTagsExtended)
			{
				if (tagToCategory.getTag().equals(selectedTag) && CollectionUtilities.isEqual(tagToCategory.getSorting(), sorting))
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

	/**
	 * @param existingTags
	 *            A collection of the existing tags
	 * @return A collection of the tags that are were selected in the UI and do
	 *         not exist in the existingTags collection, paired with a UITagData
	 *         object that contains additional information such as sorting oder
	 */
	public List<Pair<Tag, UITagData>> getExtendedAddedTags(final List<Tag> existingTags)
	{
		final List<Pair<Tag, UITagData>> selectedTags = getExtendedSelectedTags();

		// now make a note of the additions
		final List<Pair<Tag, UITagData>> addTags = new ArrayList<Pair<Tag, UITagData>>();
		for (final Pair<Tag, UITagData> selectedTag : selectedTags)
		{
			if (!existingTags.contains(selectedTag))
			{
				addTags.add(selectedTag);
			}
		}

		return addTags;
	}

	/**
	 * @param existingTags
	 *            A collection of the existing tags
	 * @return A collection of the tags that are were selected in the UI and do
	 *         not exist in the existingTags collection
	 */
	public List<Tag> getAddedTags(final List<Tag> existingTags)
	{
		final List<Tag> selectedTags = getSelectedTags();

		// now make a note of the additions
		final ArrayList<Tag> addTags = new ArrayList<Tag>();
		for (final Tag selectedTag : selectedTags)
		{
			if (!existingTags.contains(existingTags))
			{
				addTags.add(selectedTag);
			}
		}

		return addTags;
	}

	/**
	 * @param existingTags
	 *            A collection of the existing tags
	 * @return A collection of the tags that are were not selected in the UI and
	 *         exist in the existingTags collection
	 */
	public List<Tag> getRemovedTags(final List<Tag> existingTags)
	{
		final List<Tag> selectedTags = getSelectedTags();

		// make a note of the tags that were removed
		final List<Tag> removeTags = new ArrayList<Tag>();
		for (final Tag existingTag : existingTags)
		{
			if (!selectedTags.contains(existingTag))
			{
				// add to external collection to avoid modifying a
				// collection while looping over it
				removeTags.add(existingTag);
			}
		}

		return removeTags;
	}

	public void populateTopicTags(final Topic topic)
	{
		populateTags(topic.getTags(), null, true);
	}

	public void populateTopicTags()
	{
		populateTags(null, null, true);
	}

	public void populateTopicTags(final Filter filter)
	{
		populateTags(null, filter, true);
	}

	public void populateTopicTags(final Filter filter, final boolean setSelectedItemInCategory)
	{
		populateTags(null, filter, setSelectedItemInCategory);
	}

	/**
	 * This function is used to populate the data structures used to display
	 * categories and their tags.
	 * 
	 * @param topic
	 *            If this is not null, it is used to determine which tags are
	 *            currently selected
	 * @param checkedTags
	 *            This is a map of Category data to an ArrayList of Tag data.
	 *            When used to represent categories, the GuiInputData selected
	 *            field is used to indicate whether this is a mutually exclusive
	 *            category. If true, only one tag should be able to be selected.
	 *            If false, many tags can be selected. The decision to make a
	 *            category mutually exclusive is left up to a Drools rule file
	 *            in order to keep this code as process agnostic as possible.
	 *            This function will populate the category GuiInputData objects
	 *            (see the setSelectedItemInCategory param) so they can be used
	 *            either way.
	 * @param filter
	 *            This object represents the filter applied to the page. Like
	 *            topic, this is optionally used to preselect those tags that
	 *            are used in the filter. Either filter or topic can be not
	 *            null, but if both are supplied (and this shouldn't happen) the
	 *            topic is used.
	 * @param setSelectedItemInCategory
	 *            If this is false, the selectedID value in the Key of the
	 *            selectedTags parameter will not be modified to indicate which
	 *            of the children tags has been selected. This needs to be set
	 *            to false to avoid changing the equality of the keys in the
	 *            TreeMap as tag selections change.
	 */
	@SuppressWarnings("unchecked")
	public void populateTags(final List<Tag> checkedTags, final Filter filter, final boolean setSelectedItemInCategory)
	{
		try
		{
			/*
			 * this should be empty anyway, but make sure we start with a clean
			 * slate
			 */
			this.clear();

			final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
			final List<Project> projectList = entityManager.createQuery(Project.SELECT_ALL_QUERY).getResultList();

			/*
			 * First we create an entry for tags that have not been associated
			 * with a project. These will become common tags.
			 * 
			 * Use a HQL query to find those tags that have no associated
			 * project.
			 */
			final List<Tag> commonTagList = entityManager.createQuery(Tag.SELECT_ALL_QUERY + " where tag.tagToProjects is empty").getResultList();

			/* create a "common" project */
			final UIProjectData commonProjectDetails = new UIProjectData(Constants.COMMON_PROJECT_NAME, Constants.COMMON_PROJECT_DESCRIPTION, Constants.COMMON_PROJECT_ID);
			this.getProjectCategories().add(commonProjectDetails);

			/* get a list of the categories that the common tags fall into */
			final List<Category> commonCategories = new ArrayList<Category>();
			for (final Tag tag : commonTagList)
			{
				for (final TagToCategory tagToCategory : tag.getTagToCategories())
				{
					final Category category = tagToCategory.getCategory();
					if (!commonCategories.contains(category))
						commonCategories.add(category);
				}
			}

			/* create the categories under the common project */
			syncProjectCategoryDatas(commonProjectDetails, null, commonCategories, filter, checkedTags, setSelectedItemInCategory);

			/*
			 * This is a three step process: [1] we find the tags assigned to a
			 * product. [2] we find the categories assigned to the tags found in
			 * step 1 [3] we loop over the categories found in step 2, which we
			 * know contain tags assigned to the product, and pull out the tags
			 * that are associated with the product we are looking at.
			 * 
			 * This seems a little redundant, but it is necessary because there
			 * is no direct relationship between a category and a project - a
			 * category is only listed in a project if the tags contained in a
			 * category are assigned to a project.
			 */

			// loop through the projects
			for (final Project project : projectList)
			{
				// create a project
				final UIProjectData projectDetails = new UIProjectData(project.getProjectName(), project.getProjectDescription(), project.getProjectId());
				this.getProjectCategories().add(projectDetails);

				/*
				 * Step 1. find the tags assigned to a product.
				 */
				final Set<TagToProject> tags = project.getTagToProjects();

				/*
				 * Step 2: find the categories that the tags assigned to this
				 * product exist in
				 */
				final List<Category> projectCategories = new ArrayList<Category>();
				for (final TagToProject tagToProject : tags)
				{
					final Tag tag = tagToProject.getTag();
					final Set<TagToCategory> categories = tag.getTagToCategories();

					for (final TagToCategory tagToCategory : categories)
					{
						final Category category = tagToCategory.getCategory();
						if (!projectCategories.contains(category))
							projectCategories.add(category);
					}
				}

				/*
				 * Step 3: loop over the categories found in step 2, which we
				 * know contain tags assigned to the product, and pull out the
				 * tags that are associated with the product we are looking at
				 */
				syncProjectCategoryDatas(projectDetails, project, projectCategories, filter, checkedTags, setSelectedItemInCategory);
			}

			/* The final step is to order the collections */
			for (final UIProjectData project : this.getProjectCategories())
			{
				Collections.sort(project.getCategories());
				for (final UICategoryData category : project.getCategories())
				{
					Collections.sort(category.getTags());
				}

			}

		}
		catch (final Exception ex)
		{
			log.error("A catch all Exception", ex);
		}
	}
	
	/**
	 * Updates the Tag, Category and Property UI Elements, so that any Database entities, that have been removed, updated or added
	 * are reflected in the UI.
	 * 
	 * @param checkedTagst A list of tags that are checked with a topic.
     * @param setSelectedItemInCategory If the SelectedItem attribute should be set for UICategories based on the last added tag.
	 */
	@SuppressWarnings("unchecked")
    public void updateTags(final List<Tag> checkedTags, final boolean setSelectedItemInCategory)
    {
        try
        {
            final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
            final List<Project> projectList = entityManager.createQuery(Project.SELECT_ALL_QUERY).getResultList();

            /*
             * First we create an entry for tags that have not been associated
             * with a project. These will become common tags.
             * 
             * Use a HQL query to find those tags that have no associated
             * project.
             */
            final List<Tag> commonTagList = entityManager.createQuery(Tag.SELECT_ALL_QUERY + " where tag.tagToProjects is empty").getResultList();

            /* get a list of the categories that the common tags fall into */
            final List<Category> commonCategories = new ArrayList<Category>();
            for (final Tag tag : commonTagList)
            {
                for (final TagToCategory tagToCategory : tag.getTagToCategories())
                {
                    final Category category = tagToCategory.getCategory();
                    if (!commonCategories.contains(category))
                        commonCategories.add(category);
                }
            }
            
            // A list of Projects that should be removed.
            final List<UIProjectData> removeProjects = new ArrayList<UIProjectData>();
            
            /*
             * To update the project data we need to do this in various steps.
             * The first step is to go through all the current data and update
             * or remove any invalid data. The next step is to add any data that
             * doesn't exist. 
             */

            /* The first step is to update and remove any existing data */
            for (final UIProjectData projectData : projectCategories)
            {
                Project project = null;
                final List<Category> categories;
                
                // Ignore the Common Project as it should always exist
                if (!projectData.getId().equals(Constants.COMMON_PROJECT_ID))
                {
                    // Check that the project still exists
                    for (final Project tempProject : projectList)
                    {
                        if (tempProject.getId().equals(projectData.getId()))
                        {
                            project = tempProject;
                            break;
                        }
                    }
                    
                    if (project == null)
                    {
                        removeProjects.add(projectData);
                        continue;
                    }
                    
                    // Make sure the data is up to date
                    projectData.setDescription(project.getProjectDescription());
                    projectData.setName(project.getProjectName());
                    
                    categories = new ArrayList<Category>();
                    for (final TagToProject tagToProject : project.getTagToProjects())
                    {
                        final Tag tag = tagToProject.getTag();
                        final Set<TagToCategory> tagCategories = tag.getTagToCategories();

                        for (final TagToCategory tagToCategory : tagCategories)
                        {
                            final Category category = tagToCategory.getCategory();
                            if (!categories.contains(category))
                                categories.add(category);
                        }
                    }
                }
                else
                {
                    categories = commonCategories;
                }
                
                syncProjectCategoryDatas(projectData, project, categories, null, checkedTags, setSelectedItemInCategory);
            }
            
            /* Next we need to add any new projects that have been added */
            for (final Project project : projectList)
            {
                // Check that the project exists already
                UIProjectData projectData = null;
                for (final UIProjectData tempProjectData : this.projectCategories)
                {
                    if (tempProjectData.getId().equals(project.getId()))
                    {
                        projectData = tempProjectData;
                        break;
                    }
                }
                
                if (projectData == null)
                {
                    // create a project
                    projectData = new UIProjectData(project.getProjectName(), project.getProjectDescription(), project.getProjectId());
                    this.getProjectCategories().add(projectData);
                }
                
                /*
                 * Find the categories that the tags assigned to this
                 * product exist in
                 */
                final List<Category> projectCategories = new ArrayList<Category>();
                for (final TagToProject tagToProject : project.getTagToProjects())
                {
                    final Tag tag = tagToProject.getTag();
                    final Set<TagToCategory> categories = tag.getTagToCategories();

                    for (final TagToCategory tagToCategory : categories)
                    {
                        final Category category = tagToCategory.getCategory();
                        if (!projectCategories.contains(category))
                            projectCategories.add(category);
                    }
                }
                
                syncProjectCategoryDatas(projectData, project, projectCategories, null, checkedTags, setSelectedItemInCategory);
                
            }

            /* The final step is to order the collections */
            for (final UIProjectData project : this.getProjectCategories())
            {
                Collections.sort(project.getCategories());
                for (final UICategoryData category : project.getCategories())
                {
                    Collections.sort(category.getTags());
                }
            }

        }
        catch (final Exception ex)
        {
            log.error("A catch all Exception", ex);
        }
    }
	
	/**
	 * Syncs all the categories in a UIProjectData object and updates the categories and their tags based on the list
	 * of categories passed. This method will remove any categories that no longer exist, update the existing
	 * category UI information to match the underlying category if it has changed and add any new entities.
	 * 
	 * @param projectData The UIProjectData to be updated.
	 * @param project The project entity to sync with, or null if it's the common category.
	 * @param categories The List of categories to be used to update the project data.
     * @param filter The filter being used to sync the UI Elements with.
     * @param topicCheckedTags A list of tags that are checked with a topic.
     * @param setSelectedItemInCategory If the SelectedItem attribute should be set for UICategories based on the last added tag.
	 */
	protected void syncProjectCategoryDatas(final UIProjectData projectData, final Project project, final List<Category> categories, final Filter filter,
            final List<Tag> topicCheckedTags, final boolean setSelectedItemInCategory)
	{
	    // A list of UICategoryData that should be removed.
        final List<UICategoryData> removeCategories = new ArrayList<UICategoryData>();
        
        /*
         * At this point we the project should exist and it's details should be up to date.
         * So check that the category data is still valid.
         */
        for (final UICategoryData categoryData : projectData.getCategories())
        {
            Category category = null;
            for (final Category tempCategory : categories)
            {
                if (tempCategory.getId().equals(categoryData.getId()))
                {
                    category = tempCategory;
                    break;
                }
            }
            
            if (category == null)
            {
                removeCategories.add(categoryData);
                continue;
            }
            
            categoryData.setName(category.getCategoryName());
            categoryData.setDescription(category.getCategoryDescription());
            categoryData.setSort(category.getCategorySort() == null ? 0 : category.getCategorySort());
            
            syncCategoryTagDatas(categoryData, project, category, filter, topicCheckedTags, setSelectedItemInCategory);
        }
        
        /* Remove invalid categories from the project data */
        for (final UICategoryData categoryData : removeCategories)
        {
            projectData.getCategories().remove(categoryData);
        }
        
        /* Check if any new categories need to be created */
        for (final Category category : categories)
        {
            UICategoryData categoryData = null;
            for (final UICategoryData tempCategoryData : projectData.getCategories())
            {
                if (tempCategoryData.getId().equals(category.getId()))
                {
                    categoryData = tempCategoryData;
                    break;
                }
            }
            
            if (categoryData == null)
            {
                // Create the Category
                categoryData = createUICategoryData(category, project, null);
                projectData.getCategories().add(categoryData);
            }
            
            // sync category logic states with the filter
            if (filter != null)
            {
                final ArrayList<Integer> categoryStates = filter.hasCategory(category.getCategoryId(), (project == null ? null : project.getId()));

                // override the default "or" state if the filter has
                // saved an "and" state
                if (categoryStates.contains(CommonFilterConstants.CATEGORY_INTERNAL_AND_STATE))
                {
                    categoryData.setInternalLogic(Constants.AND_LOGIC);
                }

                // override the default external "and" state if the
                // filter has saved an "o" state
                if (categoryStates.contains(CommonFilterConstants.CATEGORY_EXTERNAL_OR_STATE))
                {
                    categoryData.setExternalLogic(Constants.OR_LOGIC);
                }
            }
            
            syncCategoryTagDatas(categoryData, project, category, filter, topicCheckedTags, setSelectedItemInCategory);
        }
	}
	
	/**
	 * Syncs a Categories Tags with a UICategoryData object. This method removes any UI Data objects that should
	 * no longer exist, updates any that currently are do exist and adds any missing UI elements.
	 * 
	 * @param categoryData The category data to sync with.
	 * @param project The Project that the UICategoryData exists for, or null if it's the common category.
	 * @param category The Category that the UICategoryData represents.
	 * @param filter The filter being used to sync the UI Elements with.
	 * @param topicCheckedTags A list of tags that are checked with a topic.
	 * @param setSelectedItemInCategory If the SelectedItem attribute should be set for UICategories based on the last added tag.
	 */
	protected void syncCategoryTagDatas(final UICategoryData categoryData, final Project project, final Category category, final Filter filter,
	        final List<Tag> topicCheckedTags, final boolean setSelectedItemInCategory)
	{
	    final List<UITagData> removeTags = new ArrayList<UITagData>();
        
        /*
         * Now check that the tags in the category are still valid.
         */
        final Set<TagToCategory> tagsInCategory = category.getTagToCategories();
        for (final UITagData tagData : categoryData.getTags())
        {
            boolean found = false;
            for (final TagToCategory tagToCategory : tagsInCategory)
            {
                final Tag tag = tagToCategory.getTag();
                if (tag.getId().equals(tagData.getId()))
                {
                    found = true;
                    
                    /* Update the Tag Data */
                    tagData.setName(tag.getTagName());
                    tagData.setDescription(tag.getTagDescription());
                    tagData.setChildrenList(tag.getChildrenList());
                    tagData.setParentList(tag.getParentList());
                    tagData.setProperties(tag.getProperties());
                    
                    // find the sorting order
                    Integer sorting = null;
                    for (final TagToCategory tagTagToCategory : tag.getTagToCategories())
                    {
                        if (tagTagToCategory.getCategory().getCategoryId() == categoryData.getId())
                            sorting = tagToCategory.getSorting();
                    }
                    tagData.setSort(sorting == null ? 0 : sorting);
                    
                    break;
                }
            }
            
            if (!found)
            {
                removeTags.add(tagData);
                continue;
            }
        }
        
        /* Remove invalid tags from the category data */
        for (final UITagData tagData : removeTags)
        {
            categoryData.getTags().remove(tagData);
        }
        
        /* Check if any Tags need to be created */
        for (final TagToCategory tagToCategory : tagsInCategory)
        {
            final Tag tag = tagToCategory.getTag();
            
            if ((project != null && tag.isInProject(project)) || (project == null && tag.getTagToProjects().size() == 0))
            {
                boolean found = false;
                for (final UITagData tagData : categoryData.getTags())
                {
                    if (tagData.getId().equals(tag.getId()))
                    {
                        found = true;
                        break;
                    }
                }
                
                if (!found)
                {
                    final UITagData tagData = createUITagData(tag, category.getCategoryId(), filter, topicCheckedTags);

                    /*
                     * set the selected id in the category to the last
                     * selected tag this is used by the xhtml page when a
                     * category is marked as mutually exclusive, and ignored
                     * otherwise
                     */
                    if (tagData.isSelected() && setSelectedItemInCategory)
                        categoryData.setSelectedTag(tag.getTagId());

                    categoryData.getTags().add(tagData);
                }
            }
        }
	}

	static private UICategoryData createUICategoryData(final Category category, final Project project, final Filter filter)
	{
		final String catName = category.getCategoryName();
		final String catDesc = category.getCategoryDescription();
		final Integer catID = category.getCategoryId();
		final Integer catSort = category.getCategorySort();

		final UICategoryData retValue = new UICategoryData(catName, catDesc, catID, catSort == null ? 0 : catSort, false);

		/* sync the category logic with the filter */

		if (filter != null)
		{
			/*
			 * loop over all the categories, looking for a match with the
			 * project and category used to create the UI data
			 */
			for (final FilterCategory filterCategory : filter.getFilterCategories())
			{
				/*
				 * we have a match if a null project was supplied and a null
				 * project was found or the supplied project matches the filter
				 * project and the categories match.
				 * CollectionUtilities.isEqual() handles equality between null
				 * objects.
				 */

				final boolean projectsMatch = CollectionUtilities.isEqual(project, filterCategory.getProject());
				final boolean categoriesMatch = filterCategory.getCategory().equals(category);

				if (projectsMatch && categoriesMatch)
				{
					if (filterCategory.getCategoryState() == CommonFilterConstants.CATEGORY_INTERNAL_AND_STATE)
						retValue.setInternalLogic(Constants.AND_LOGIC);
					else if (filterCategory.getCategoryState() == CommonFilterConstants.CATEGORY_INTERNAL_OR_STATE)
						retValue.setInternalLogic(Constants.OR_LOGIC);
					else if (filterCategory.getCategoryState() == CommonFilterConstants.CATEGORY_EXTERNAL_AND_STATE)
						retValue.setExternalLogic(Constants.AND_LOGIC);
					if (filterCategory.getCategoryState() == CommonFilterConstants.CATEGORY_EXTERNAL_OR_STATE)
						retValue.setExternalLogic(Constants.OR_LOGIC);
				}
			}
		}

		return retValue;
	}

	@SuppressWarnings("javadoc")
	static private UITagData createUITagData(final Tag tag, final Integer catID, final Filter filter, final List<Tag> checkedTags)
	{
		final Integer tagId = tag.getTagId();
		final String tagName = tag.getTagName();
		final String tagDescription = tag.getTagDescription();
		final String tagChildrenList = tag.getChildrenList();
		final String tagParentList = tag.getParentList();
		final String tagProperties = tag.getProperties();

		/*
		 * find out if this tag is already selected by the topic ...
		 */
		boolean selected = false;
		boolean selectedNot = false;
		boolean groupBy = false;

		if (checkedTags != null)
		{
			selected = checkedTags.contains(tag);
		}
		// ... or by the filter
		else
		{
			if (filter != null)
			{
				final List<Integer> tagStates = filter.hasTag(tagId);

				for (final Integer tagState : tagStates)
				{
					if (tagState == CommonFilterConstants.NOT_MATCH_TAG_STATE)
						selected = selectedNot = true;
					else if (tagState == CommonFilterConstants.MATCH_TAG_STATE)
						selected = true;
					else if (tagState == CommonFilterConstants.GROUP_TAG_STATE)
						groupBy = true;
				}
			}
		}

		// find the sorting order
		Integer sorting = null;
		for (final TagToCategory tagToCategory : tag.getTagToCategories())
		{
			if (tagToCategory.getCategory().getCategoryId() == catID)
				sorting = tagToCategory.getSorting();
		}

		final UITagData retValue = new UITagData(tagName, tagDescription, tagId, sorting == null ? 0 : sorting, selected, selectedNot, groupBy, tagParentList, tagChildrenList, tagProperties);
		return retValue;
	}

	public void loadTagCheckboxes(final Filter filter)
	{
		// sync the Filter with the gui checkboxes
		for (final UIProjectData project : this.getProjectCategories())
		{
			for (final UICategoryData category : project.getCategories())
			{
				for (final UITagData tag : category.getTags())
				{
					boolean found = false;
					for (final FilterTag filterTag : filter.getFilterTags())
					{
						if (tag.getId().equals(filterTag.getTag().getTagId()))
						{
							final int tagState = filterTag.getTagState();
							
							if (tagState == CommonFilterConstants.MATCH_TAG_STATE)		
							{
								tag.setSelected(true);
							}
							else if (tagState == CommonFilterConstants.NOT_MATCH_TAG_STATE)
							{
								tag.setSelected(true);
								tag.setNotSelected(true);
							}
							else if (tagState == CommonFilterConstants.GROUP_TAG_STATE)
							{
								tag.setGroupBy(true);
							}
							
							found = true;
						}
					}

					if (!found)
					{
						tag.setSelected(false);
						tag.setNotSelected(false);
						tag.setGroupBy(false);
					}
				}
			}
		}
	}

	public void loadCategoryLogic(final Filter filter)
	{
		// sync the category logic states
		for (final UIProjectData project : this.getProjectCategories())
		{
			for (final UICategoryData category : project.getCategories())
			{
				for (final FilterCategory filterCategory : filter.getFilterCategories())
				{
					final Integer categoryId = filterCategory.getCategory().getCategoryId();

					if (categoryId.equals(category.getId()))
					{
						final int catgeoryState = filterCategory.getCategoryState();

						if (catgeoryState == CommonFilterConstants.CATEGORY_INTERNAL_AND_STATE)
							category.setInternalLogic(Constants.AND_LOGIC);
						if (catgeoryState == CommonFilterConstants.CATEGORY_INTERNAL_OR_STATE)
							category.setInternalLogic(Constants.OR_LOGIC);

						if (catgeoryState == CommonFilterConstants.CATEGORY_EXTERNAL_AND_STATE)
							category.setExternalLogic(Constants.AND_LOGIC);
						if (catgeoryState == CommonFilterConstants.CATEGORY_EXTERNAL_OR_STATE)
							category.setExternalLogic(Constants.OR_LOGIC);
					}
				}

			}
		}
	}
}
