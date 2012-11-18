package org.jboss.pressgang.ccms.seam.utils;

import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.drools.ClassObjectFilter;
import org.drools.WorkingMemory;
import org.jboss.pressgang.ccms.model.Category;
import org.jboss.pressgang.ccms.model.Project;
import org.jboss.pressgang.ccms.model.PropertyTag;
import org.jboss.pressgang.ccms.model.PropertyTagCategory;
import org.jboss.pressgang.ccms.model.PropertyTagToPropertyTagCategory;
import org.jboss.pressgang.ccms.model.Tag;
import org.jboss.pressgang.ccms.model.TagToCategory;
import org.jboss.pressgang.ccms.model.TagToProject;
import org.jboss.pressgang.ccms.model.User;
import org.jboss.pressgang.ccms.seam.sort.RoleNameComparator;
import org.jboss.pressgang.ccms.seam.sort.TopicTagCategoryDataNameSorter;
import org.jboss.pressgang.ccms.seam.sort.UserNameComparator;
import org.jboss.pressgang.ccms.seam.utils.structures.roles.UIRoleUserData;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UICategoryData;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UIProjectData;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UIProjectTagData;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UIProjectsData;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UITagData;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UITagProjectData;
import org.jboss.seam.Component;
import org.jboss.seam.security.Identity;
import org.jboss.seam.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityUtilities extends org.jboss.pressgang.ccms.restserver.utils.EntityUtilities {
    private static final Logger log = LoggerFactory.getLogger(EntityUtilities.class);
    
    private static EntityManager getEntityManager() {
        return (EntityManager) Component.getInstance("entityManager");
    }
    
    public static PropertyTag getPropertyTagFromId(final Integer tagId)
    {
        final PropertyTag tag = getEntityManager().find(PropertyTag.class, tagId);
        return tag;
    }
    
    /**
     * When assigning tags to a category, we need to know the sorting order of
     * the tags as it related to a specific category. This is different to the
     * sorting order used to show the tags, because those values are specific to
     * the categories that the tags appear in.
     */
    public static void populateTagTagsSortingForCategory(final Category category, final UIProjectsData selectedTags)
    {
        for (final UIProjectData projectData : selectedTags.getProjectCategories())
        {
            for (final UICategoryData categoryData : projectData.getCategories())
            {
                if (categoryData.getId().equals(category.getCategoryId()))
                {
                    for (final UITagData tagData : categoryData.getTags())
                    {
                        /*
                         * match the sorting order for the tags in the category
                         * with the newSort values for the UI tags
                         */

                        for (final TagToCategory tagToCategory : category.getTagToCategories())
                        {
                            if (tagData.getId().equals(tagToCategory.getTag().getTagId()))
                            {
                                tagData.setNewSort(tagToCategory.getSorting());
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Used to add the current user roles into Drools working memory. This
     * function has been copied from
     * RuleBasedPermissionResolver.synchronizeContext()
     */
    @SuppressWarnings("unchecked")
    public static void injectSecurity(WorkingMemory workingMemory, Identity identity)
    {
        for (final Group sg : identity.getSubject().getPrincipals(Group.class))
        {
            if (Identity.ROLES_GROUP.equals(sg.getName()))
            {
                Enumeration<Principal> e = (Enumeration<Principal>) sg.members();
                while (e.hasMoreElements())
                {
                    Principal role = e.nextElement();

                    boolean found = false;
                    Iterator<Role> iter = (Iterator<Role>) workingMemory.iterateObjects(new ClassObjectFilter(Role.class));
                    while (iter.hasNext())
                    {
                        Role r = iter.next();
                        if (r.getName().equals(role.getName()))
                        {
                            found = true;
                            break;
                        }
                    }

                    if (!found)
                    {
                        workingMemory.insert(new Role(role.getName()));
                    }

                }
            }
        }
    }
    
    public static String buildEditNewTopicUrl(final UIProjectsData selectedTags)
    {
        String tags = "";
        for (final UIProjectData project : selectedTags.getProjectCategories())
        {
            for (final UICategoryData cat : project.getCategories())
            {
                if (cat.isMutuallyExclusive())
                {
                    if (cat.getSelectedTag() != null)
                    {
                        if (tags.length() != 0)
                            tags += "&";
                        tags += "tag" + cat.getSelectedTag() + "=1";
                    }

                }
                else
                {
                    for (final UITagData tag : cat.getTags())
                    {
                        if (tag.isSelected())
                        {
                            if (tags.length() != 0)
                                tags += "&";
                            tags += "tag" + tag.getId() + "=1";
                        }
                    }
                }
            }
        }
        final String retValue = "/TopicEdit.seam?" + tags;
        return retValue;
    }
    
    public static Tag getTagFromId(final Integer tagId)
    {
        final Tag tag = getEntityManager().find(Tag.class, tagId);
        return tag;
    }

    public static org.jboss.pressgang.ccms.model.Role getRoleFromId(final Integer roleId)
    {
        final org.jboss.pressgang.ccms.model.Role role = getEntityManager().find(org.jboss.pressgang.ccms.model.Role.class, roleId);
        return role;
    }

    public static User getUserFromId(final Integer userId)
    {
        final User user = getEntityManager().find(User.class, userId);
        return user;
    }
    
    @SuppressWarnings("unchecked")
    public static User getUserFromUsername(final String username)
    {
        if (username == null) return null;
        
        final Query query = getEntityManager().createQuery(User.SELECT_ALL_QUERY + " where user.userName = '" + username + "'");
        final List<User> users = query.getResultList();
        return users == null ? null : (users.size() == 1 ? users.get(0) : null);
    }

    static public List<UIRoleUserData> getUserRoles(final User user)
    {
        final List<UIRoleUserData> retValue = new ArrayList<UIRoleUserData>();

        @SuppressWarnings("unchecked")
        final List<org.jboss.pressgang.ccms.model.Role> roleList = getEntityManager().createQuery(org.jboss.pressgang.ccms.model.Role.SELECT_ALL_QUERY).getResultList();

        Collections.sort(roleList, new RoleNameComparator());

        for (final org.jboss.pressgang.ccms.model.Role role : roleList)
        {
            final boolean selected = user.isInRole(role);
            final UIRoleUserData roleUserData = new UIRoleUserData(role.getRoleId(), role.getRoleName(), selected);
            retValue.add(roleUserData);
        }

        return retValue;
    }

    static public List<UIRoleUserData> getRoleUsers(final org.jboss.pressgang.ccms.model.Role role)
    {
        final List<UIRoleUserData> retValue = new ArrayList<UIRoleUserData>();

        @SuppressWarnings("unchecked")
        final List<User> userList = getEntityManager().createQuery(User.SELECT_ALL_QUERY).getResultList();

        Collections.sort(userList, new UserNameComparator());

        for (final User user : userList)
        {
            final boolean selected = role.hasUser(user);
            final UIRoleUserData roleUserData = new UIRoleUserData(user.getUserId(), user.getUserName(), selected);
            retValue.add(roleUserData);
        }

        return retValue;
    }
    
    @SuppressWarnings("unchecked")
    static public void populatePropertyTagCategories(final PropertyTag tag, final List<UICategoryData> categories)
    {
        categories.clear();

        final List<PropertyTagCategory> categoryList = getEntityManager().createQuery(PropertyTagCategory.SELECT_ALL_QUERY).getResultList();
        final List<PropertyTagToPropertyTagCategory> tagToCategoryList = getEntityManager().createQuery(PropertyTagToPropertyTagCategory.SELECT_ALL_QUERY).getResultList();

        // then loop through the categories
        for (final PropertyTagCategory category : categoryList)
        {
            final String catName = category.getPropertyTagCategoryName();
            final Integer catID = category.getPropertyTagCategoryId();
            final String catDesc = category.getPropertyTagCategoryDescription();
            final Integer tagId = tag.getPropertyTagId();

            // find out if the tag is already in the category
            final boolean selected = tag.isInCategory(category);

            // get the sorting value.
            Integer sorting = null;
            for (final PropertyTagToPropertyTagCategory tagToCategory : tagToCategoryList)
            {
                if (tagToCategory.getPropertyTagCategory().getPropertyTagCategoryId() == catID && tagToCategory.getPropertyTag().getPropertyTagId() == tagId)
                {
                    sorting = tagToCategory.getSorting();
                    break;
                }
            }

            /*
             * in this case the sort value in the TopicTagCategoryData
             * represents the tags sorting position within the category, not the
             * category's sorting position amongst other categories
             */
            categories.add(new UICategoryData(catName, catDesc, catID, sorting == null ? 0 : sorting, selected, false, false, false));
        }

        // sort the categories by name
        Collections.sort(categories, new TopicTagCategoryDataNameSorter());
    }
    
    @SuppressWarnings("unchecked")
    public static void populateMutuallyExclusiveCategories(final UIProjectsData guiData)
    {
        final List<Category> categoryList = getEntityManager().createQuery(Category.SELECT_ALL_QUERY).getResultList();
        for (final Category category : categoryList)
        {
            for (final UIProjectData project : guiData.getProjectCategories())
            {
                for (final UICategoryData guiInputData : project.getCategories())
                {
                    if (guiInputData.getId().equals(category.getCategoryId()) && category.isMutuallyExclusive())
                    {
                        guiInputData.setMutuallyExclusive(true);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * This function is used to populate the data structures that display the
     * categories that a tag can and does belong to.
     * 
     * @param tag
     *            The Tag being displayed
     * @param categories
     *            A collection of data structures representing the categories
     * @param selectedCategories
     *            A collection of selected categories
     * @param tagSortValues
     *            A collection of data structures representing the tags sorting
     *            order within a category
     */
    @SuppressWarnings("unchecked")
    static public void populateTagCategories(final Tag tag, final List<UICategoryData> categories)
    {
        categories.clear();

        final List<Category> categoryList = getEntityManager().createQuery(Category.SELECT_ALL_QUERY).getResultList();
        final List<TagToCategory> tagToCategoryList = getEntityManager().createQuery(TagToCategory.SELECT_ALL_QUERY).getResultList();

        // then loop through the categories
        for (final Category category : categoryList)
        {
            final String catName = category.getCategoryName();
            final Integer catID = category.getCategoryId();
            final String catDesc = category.getCategoryDescription();
            final Integer tagId = tag.getTagId();

            // find out if the tag is already in the category
            final boolean selected = tag.isInCategory(category);

            // get the sorting value.
            Integer sorting = null;
            for (final TagToCategory tagToCategory : tagToCategoryList)
            {
                if (tagToCategory.getCategory().getCategoryId() == catID && tagToCategory.getTag().getTagId() == tagId)
                {
                    sorting = tagToCategory.getSorting();
                    break;
                }
            }

            /*
             * in this case the sort value in the TopicTagCategoryData
             * represents the tags sorting position within the category, not the
             * category's sorting position amongst other categories
             */
            categories.add(new UICategoryData(catName, catDesc, catID, sorting == null ? 0 : sorting, selected, false, false, false));
        }

        // sort the categories by name
        Collections.sort(categories, new TopicTagCategoryDataNameSorter());
    }
    
    @SuppressWarnings("unchecked")
    public static void populateTagProjects(final Tag mainTag, final List<UITagProjectData> projects)
    {
        try
        {
            projects.clear();

            final List<Project> projectList = getEntityManager().createQuery(Project.SELECT_ALL_QUERY).getResultList();

            for (final Project project : projectList)
            {
                boolean found = false;

                for (final TagToProject tagToProject : mainTag.getTagToProjects())
                {
                    if (tagToProject.getProject().equals(project))
                    {
                        found = true;
                        break;
                    }
                }

                projects.add(new UITagProjectData(project, found));
            }
        }
        catch (final Exception ex)
        {
            log.error("Probably an error retrieving a Project entity", ex);
        }
    }
    
    public static void populateProjectTags(final Project project, final UIProjectsData selectedTags)
    {
        selectedTags.populateTags(project.getTags(), null, true);
    }
    
    @SuppressWarnings("unchecked")
    public static void populateProjectTags(final Project project, final List<UIProjectTagData> tags)
    {
        try
        {
            tags.clear();

            final List<Tag> tagList = getEntityManager().createQuery(Tag.SELECT_ALL_QUERY).getResultList();

            for (final Tag tag : tagList)
            {
                boolean found = false;

                for (final TagToProject tagToProject : project.getTagToProjects())
                {
                    if (tagToProject.getProject().equals(project))
                    {
                        found = true;
                        break;
                    }
                }

                tags.add(new UIProjectTagData(tag, found));
            }
        }
        catch (final Exception ex)
        {
            log.error("Probably an error retrieving a Tag entity", ex);
        }
    }
}
