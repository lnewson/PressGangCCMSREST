package org.jboss.pressgang.ccms.seam.utils;

import static ch.lambdaj.Lambda.filter;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.jboss.pressgang.ccms.docbook.compiling.DocbookBuildingOptions;
import org.jboss.pressgang.ccms.model.Category;
import org.jboss.pressgang.ccms.model.Filter;
import org.jboss.pressgang.ccms.model.FilterCategory;
import org.jboss.pressgang.ccms.model.FilterField;
import org.jboss.pressgang.ccms.model.FilterLocale;
import org.jboss.pressgang.ccms.model.FilterOption;
import org.jboss.pressgang.ccms.model.FilterTag;
import org.jboss.pressgang.ccms.model.Project;
import org.jboss.pressgang.ccms.model.Tag;
import org.jboss.pressgang.ccms.rest.v1.constants.CommonFilterConstants;
import org.jboss.pressgang.ccms.restserver.filter.base.IFieldFilter;
import org.jboss.pressgang.ccms.seam.utils.structures.locales.UILocale;
import org.jboss.pressgang.ccms.seam.utils.structures.locales.UILocales;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UICategoryData;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UIProjectData;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UIProjectsData;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UITagData;
import org.jboss.seam.Component;

public class FilterUtilities extends org.jboss.pressgang.ccms.restserver.utils.FilterUtilities {

    public static void syncFilterWithTags(final Filter filter, final UIProjectsData selectedTags)
    {
        final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");

        /*
         * loop through the list of tags selected via the checkboxes, and update
         * their representation in the Filter
         */

        /*
         * We have to deal with the situation that can arise when a tag is in
         * two different projects or categories. The selectedFilterTags
         * collection will hold all the FilterTags that are created in response
         * to a tag being selected once. We can then reference this collection
         * to prevent the FilterTag from being removed in response to a tag not
         * being selected in another location.
         */
        final List<FilterTag> selectedFilterTags = new ArrayList<FilterTag>();
        for (final UIProjectData project : selectedTags.getProjectCategories())
        {
            for (final UICategoryData category : project.getCategories())
            {
                for (final UITagData tag : category.getTags())
                {
                    final boolean tagSelected = tag.isSelected();
                    final boolean notTagSelected = tag.isNotSelected();
                    final boolean groupBy = tag.isGroupBy();
                    final int state = notTagSelected ? CommonFilterConstants.NOT_MATCH_TAG_STATE : CommonFilterConstants.MATCH_TAG_STATE;

                    if (tagSelected)
                    {
                        boolean found = false;
                        for (final FilterTag filterTag : filter.getFilterTags())
                        {
                            final int tagState = filterTag.getTagState();

                            if (tagState == CommonFilterConstants.NOT_MATCH_TAG_STATE || tagState == CommonFilterConstants.MATCH_TAG_STATE)
                            {
                                final Tag filterTagTag = filterTag.getTag();

                                if (filterTagTag.getTagId().equals(tag.getId()))
                                {
                                    filterTag.setTagState(state);
                                    selectedFilterTags.add(filterTag);
                                    found = true;
                                    break;
                                }
                            }
                        }

                        if (!found)
                        {
                            final Tag dbTag = entityManager.getReference(Tag.class, tag.getId());

                            final FilterTag filterTag = new FilterTag();
                            selectedFilterTags.add(filterTag);
                            filterTag.setFilter(filter);
                            filterTag.setTag(dbTag);
                            filterTag.setTagState(state);

                            filter.getFilterTags().add(filterTag);
                        }
                    }

                    if (groupBy)
                    {
                        boolean found = false;
                        for (final FilterTag filterTag : filter.getFilterTags())
                        {
                            final int tagState = filterTag.getTagState();
                            if (tagState == CommonFilterConstants.GROUP_TAG_STATE)
                            {
                                final Tag filterTagTag = filterTag.getTag();

                                if (filterTagTag.getTagId().equals(tag.getId()))
                                {
                                    found = true;
                                    break;
                                }
                            }
                        }

                        if (!found)
                        {
                            final Tag dbTag = entityManager.getReference(Tag.class, tag.getId());

                            final FilterTag filterTag = new FilterTag();
                            selectedFilterTags.add(filterTag);
                            filterTag.setFilter(filter);
                            filterTag.setTag(dbTag);
                            filterTag.setTagState(CommonFilterConstants.GROUP_TAG_STATE);

                            filter.getFilterTags().add(filterTag);
                        }
                    }
                }
            }
        }

        /*
         * now loop through the tags in the Filter, and remove any that are no
         * longer checked in the gui
         */

        final ArrayList<FilterTag> removeTags = new ArrayList<FilterTag>();

        for (final FilterTag filterTag : filter.getFilterTags())
        {
            /*
             * don't attempt to remove a FilterTag that was specifically added
             * above
             */
            if (!selectedFilterTags.contains(filterTag))
            {
                boolean found = false;
                for (final UIProjectData project : selectedTags.getProjectCategories())
                {
                    for (final UICategoryData category : project.getCategories())
                    {
                        for (final UITagData tag : category.getTags())
                        {
                            /* are we looking at the same ui tag and filter tag? */
                            if (tag.getId().equals(filterTag.getTag().getTagId()))
                            {
                                final boolean tagSelected = tag.isSelected();
                                final boolean isSelectedTag = filterTag.getTagState() == CommonFilterConstants.MATCH_TAG_STATE
                                        || filterTag.getTagState() == CommonFilterConstants.NOT_MATCH_TAG_STATE;
                                final boolean groupBy = tag.isGroupBy();
                                final boolean isGroupByTag = filterTag.getTagState() == CommonFilterConstants.GROUP_TAG_STATE;

                                if ((isSelectedTag && !tagSelected) || (isGroupByTag && !groupBy))
                                {
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                /*
                 * add to a temporary container so we don't modify the
                 * collection we are looping over
                 */
                if (found)
                    removeTags.add(filterTag);
            }
        }

        // now clean out the obsolete tags
        for (final FilterTag filterTag : removeTags)
            filter.getFilterTags().remove(filterTag);
    }

    public static void syncFilterWithFieldUIElements(final Filter filter, final IFieldFilter entityFilter)
    {
        for (final String fieldName : entityFilter.getBaseFieldNames().keySet())
        {
            final String singleFieldValue = entityFilter.getFieldValue(fieldName);
            if (singleFieldValue != null)
            {
                syncFilterField(filter, fieldName, entityFilter.getFieldValue(fieldName), entityFilter.getFieldDesc(fieldName));
            }
            else
            {
                int i = 1;
                String multiFieldValue = null;
                while ((multiFieldValue = entityFilter.getFieldValue(fieldName + i)) != null)
                {
                    if (!multiFieldValue.trim().isEmpty())
                        syncFilterField(filter, fieldName + i, multiFieldValue, entityFilter.getFieldDesc(fieldName + i));
                }
            }
        }
    }

    private static void syncFilterField(final Filter filter, final String fieldName, final String fieldValue, final String fieldDescription)
    {
        // get the database filterfield object that matches the fieldName
        final List<FilterField> filterField = filter(having(on(FilterField.class).getField(), equalTo(fieldName)), filter.getFilterFields());

        /*
         * if fieldValue is set to a value, we need to modify or add a
         * FilterField entity
         */
        if (fieldValue != null && fieldValue.trim().length() != 0)
        {
            final String fixedFieldValue = fieldValue.trim();

            FilterField newField = null;

            // add a new FilterField entity
            if (filterField.size() == 0)
            {
                newField = new FilterField();
                newField.setFilter(filter);
                newField.setField(fieldName);
                newField.setValue(fixedFieldValue);
                newField.setDescription(fieldDescription);
                filter.getFilterFields().add(newField);
            }
            // update a FilterField entity
            else if (filterField.size() == 1)
            {
                newField = filterField.get(0);
                newField.setValue(fixedFieldValue);
                newField.setDescription(fieldDescription);
            }
        }
        else
        {
            // remove the FilterField entity
            if (filterField.size() == 1)
                filter.getFilterFields().remove(filterField.get(0));
        }
    }

    public static void syncFilterWithCategories(final Filter filter, final UIProjectsData selectedTags)
    {
        final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");

        for (final UIProjectData project : selectedTags.getProjectCategories())
        {
            final Integer projKey = project.getId();

            for (final UICategoryData category : project.getCategories())
            {
                final Integer catKey = category.getId();

                /*
                 * match the radioboxes with the internal db flag used to
                 * indicate logic
                 */
                int dbIntCatLogic;
                int dbExtCatLogic;

                final String intCatLogic = category.getInternalLogic();
                final String extCatLogic = category.getExternalLogic();

                if (intCatLogic.equals(Constants.AND_LOGIC))
                    dbIntCatLogic = CommonFilterConstants.CATEGORY_INTERNAL_AND_STATE;
                else
                    dbIntCatLogic = CommonFilterConstants.CATEGORY_INTERNAL_OR_STATE;

                if (extCatLogic.equals(Constants.AND_LOGIC))
                    dbExtCatLogic = CommonFilterConstants.CATEGORY_EXTERNAL_AND_STATE;
                else
                    dbExtCatLogic = CommonFilterConstants.CATEGORY_EXTERNAL_OR_STATE;

                /*
                 * set the existing filter category states to the ones selected
                 * in the gui
                 */
                boolean foundIntCat = false;
                boolean foundExtCat = false;
                final Set<FilterCategory> filterCategories = filter.getFilterCategories();
                for (final FilterCategory filterCategory : filterCategories)
                {
                    final int filterCategoryID = filterCategory.getCategory().getCategoryId();

                    /*
                     * The UIProjectCategoriesData class uses
                     * Constants.COMMON_PROJECT_ID to denote the common project.
                     * The FilterCategory uses a null project to denote the
                     * common project. We handle this special case here.
                     */
                    final Project filterProject = filterCategory.getProject();
                    final int filterProjectID = filterProject == null ? Constants.COMMON_PROJECT_ID : filterCategory.getProject().getProjectId();

                    if (filterCategoryID == catKey && filterProjectID == projKey)
                    {
                        /*
                         * we are looking at the internal logic filter, and it
                         * does not match what the user just selected
                         */
                        if ((filterCategory.getCategoryState() == CommonFilterConstants.CATEGORY_INTERNAL_AND_STATE || filterCategory.getCategoryState() == CommonFilterConstants.CATEGORY_INTERNAL_OR_STATE))
                        {
                            if (filterCategory.getCategoryState() != dbIntCatLogic)
                                filterCategory.setCategoryState(dbIntCatLogic);
                            foundIntCat = true;
                        }

                        /*
                         * we are looking at the external logic filter, and it
                         * does not match what the user just selected
                         */
                        if ((filterCategory.getCategoryState() == CommonFilterConstants.CATEGORY_EXTERNAL_AND_STATE || filterCategory.getCategoryState() == CommonFilterConstants.CATEGORY_EXTERNAL_OR_STATE))
                        {
                            if (filterCategory.getCategoryState() != dbExtCatLogic)
                                filterCategory.setCategoryState(dbExtCatLogic);
                            foundExtCat = true;
                        }
                    }

                    /*
                     * if we find both the internal and external filter
                     * categories, we can exit the loop
                     */
                    if (foundIntCat && foundExtCat)
                        break;
                }

                // create the missing filter categories
                if (!foundIntCat || !foundExtCat)
                {
                    final Category categoryEntity = entityManager.find(Category.class, catKey);
                    final Project projectEntity = entityManager.find(Project.class, projKey);

                    if (!foundIntCat)
                        createFilterCategory(filter, projectEntity, categoryEntity, dbIntCatLogic);
                    if (!foundExtCat)
                        createFilterCategory(filter, projectEntity, categoryEntity, dbExtCatLogic);
                }
            }
        }
    }
    
    public static void syncFilterWithLocales(final Filter filter, final UILocales selectedLocales)
    {
        for (final UILocale locale : selectedLocales.getLocales())
        {
            final boolean localeSelected = locale.isSelected();
            final boolean notLocaleSelected = locale.isNotSelected();
            final int state = notLocaleSelected ? CommonFilterConstants.NOT_MATCH_TAG_STATE : CommonFilterConstants.MATCH_TAG_STATE;
            
            if (localeSelected)
            {
                boolean found = false;
                for (final FilterLocale filterLocale : filter.getFilterLocales())
                {
                    final int localeState = filterLocale.getLocaleState();

                    if (localeState == CommonFilterConstants.NOT_MATCH_TAG_STATE || localeState == CommonFilterConstants.MATCH_TAG_STATE)
                    {
                        if (filterLocale.getLocaleName().equals(locale.getName()))
                        {
                            filterLocale.setLocaleState(state);
                            found = true;
                            break;
                        }
                    }
                }
                
                if (!found)
                {
                    final FilterLocale filterLocale = new FilterLocale();
                    filterLocale.setLocaleName(locale.getName());
                    filterLocale.setLocaleState(state);
                    filterLocale.setFilter(filter);
                    
                    filter.getFilterLocales().add(filterLocale);
                }
            }
        }
    }

    /**
     * A utility function that persists a FilterCategory object to the database
     */
    private static void createFilterCategory(final Filter filter, final Project project, final Category category, final Integer state)
    {
        final FilterCategory filterCategory = new FilterCategory();
        filterCategory.setCategoryState(state);
        filterCategory.setFilter(filter);
        filterCategory.setProject(project);
        filterCategory.setCategory(category);
        filter.getFilterCategories().add(filterCategory);
    }

    public static void syncWithDocbookOptions(final Filter filter, final DocbookBuildingOptions options)
    {
        final List<String> docbookOptions = DocbookBuildingOptions.getOptionNames();
        for (final String option : docbookOptions)
        {
            final String fieldValue = options.getFieldValue(option);
            if (fieldValue != null)
            {
                boolean found = false;
                for (final FilterOption filterOption : filter.getFilterOptions())
                {
                    if (filterOption.getFilterOptionName().equals(option))
                    {
                        found = true;
                        filterOption.setFilterOptionValue(fieldValue);
                    }
                }
    
                if (!found)
                {
                    final FilterOption filterOption = new FilterOption();
                    filterOption.setFilter(filter);
                    filterOption.setFilterOptionName(option);
                    filterOption.setFilterOptionValue(fieldValue);
                    filter.getFilterOptions().add(filterOption);
                }
            }
        }

    }

    public static DocbookBuildingOptions createDocbookOptionsFilter(final Filter filter)
    {
        final DocbookBuildingOptions options = new DocbookBuildingOptions();
        for (final FilterOption option : filter.getFilterOptions())
            options.setFieldValue(option.getFilterOptionName(), option.getFilterOptionValue());
        return options;
    }

    public static void updateDocbookOptionsFilter(final Filter filter, final DocbookBuildingOptions options)
    {
        for (final FilterOption option : filter.getFilterOptions())
            options.setFieldValue(option.getFilterOptionName(), option.getFilterOptionValue());
    }
}
