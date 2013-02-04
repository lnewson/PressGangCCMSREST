package org.jboss.pressgang.ccms.seam.session;

import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaQuery;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.jboss.pressgang.ccms.filter.TopicFieldFilter;
import org.jboss.pressgang.ccms.filter.builder.TranslatedTopicDataFilterQueryBuilder;
import org.jboss.pressgang.ccms.model.Filter;
import org.jboss.pressgang.ccms.model.TranslatedTopicData;
import org.jboss.pressgang.ccms.rest.v1.constants.CommonFilterConstants;
import org.jboss.pressgang.ccms.restserver.utils.EntityUtilities;
import org.jboss.pressgang.ccms.seam.session.base.TopicSearchBase;
import org.jboss.pressgang.ccms.seam.utils.FilterUtilities;
import org.jboss.pressgang.ccms.seam.utils.contentspec.ContentSpecGenerator;
import org.jboss.pressgang.ccms.seam.utils.structures.locales.UILocales;
import org.jboss.pressgang.ccms.utils.common.HTTPUtilities;
import org.jboss.pressgang.ccms.utils.common.MIMEUtilities;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The backing bean for the Translations Search page
 */
@Name("translatedTopicSearch")
@Scope(ScopeType.PAGE)
public class TranslatedTopicSearch extends TopicSearchBase implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(TranslatedTopicSearch.class);
    private static final long serialVersionUID = 7038462233325730801L;

    @In
    private EntityManager entityManager;
    /**
     * A list of locales that can be selected
     */
    private UILocales selectedLocales = new UILocales();

    public UILocales getSelectedLocales() {
        return selectedLocales;
    }

    public void setSelectedLocales(final UILocales selectedLocales) {
        this.selectedLocales = selectedLocales;
    }

    @Create
    public void populate() {
        // If we don't have a filter selected, populate the filter from the url
        // variables
        final Filter filter;
        if (this.selectedFilter == null) {
            // build up a Filter object from the URL variables
            final FacesContext context = FacesContext.getCurrentInstance();
            filter = EntityUtilities.populateFilter(entityManager, context.getExternalContext().getRequestParameterMap(),
                    CommonFilterConstants.FILTER_ID, CommonFilterConstants.MATCH_TAG, CommonFilterConstants.GROUP_TAG,
                    CommonFilterConstants.CATEORY_INTERNAL_LOGIC, CommonFilterConstants.CATEORY_EXTERNAL_LOGIC,
                    CommonFilterConstants.MATCH_LOCALE, new TopicFieldFilter());
        }
        // otherwise load the filter using the filter id
        else {
            filter = entityManager.find(Filter.class, selectedFilter);
        }

        this.selectedTags.populateTopicTags(filter, false);
        this.selectedLocales.loadLocaleCheckboxes(filter);
        this.topic.syncWithFilter(filter);
        FilterUtilities.updateDocbookOptionsFilter(filter, this.docbookBuildingOptions);

        getFilterList();
    }

    @SuppressWarnings("unchecked")
    private void getFilterList() {
        final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");

        // get a list of the existing filters in the database
        filters.clear();
        filters.addAll(entityManager.createQuery(Filter.SELECT_ALL_QUERY).getResultList());
    }

    public String doSearch() {
        final Filter filter = new Filter();
        syncFilterWithUI(filter);

        String params = FilterUtilities.buildFilterUrlVars(filter);

        return "/GroupedTranslatedTopicDataList.seam?" + params;
    }

    /**
     * This function takes the tag selections and uses these to populate a Filter
     *
     * @param filter  The filter to sync with the tag selection
     * @param persist true if this filter is being saved to the db (i.e. the user is actually saving the filter), and false if
     *                it is not (like when the user is building the docbook zip file)
     */
    @Override
    protected void syncFilterWithUI(final Filter filter) {
        FilterUtilities.syncFilterWithTags(filter, this.selectedTags);
        FilterUtilities.syncFilterWithCategories(filter, this.selectedTags);
        FilterUtilities.syncFilterWithFieldUIElements(filter, this.topic);
        FilterUtilities.syncFilterWithLocales(filter, selectedLocales);
        FilterUtilities.syncWithDocbookOptions(filter, this.docbookBuildingOptions);
    }

    /**
     * Loads a Filter object from the database given a FilterID, and selects the appropriate tags in the ui
     */
    @Override
    public void loadFilter() {
        final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");

        // load the filter from the database using the filter id
        Filter filter = null;
        if (selectedFilter != null) filter = entityManager.find(Filter.class, selectedFilter);
        else filter = new Filter();

        this.setSelectedFilterName(filter.getFilterName());

        this.selectedTags.loadTagCheckboxes(filter);
        this.selectedTags.loadCategoryLogic(filter);
        this.selectedLocales.loadLocaleCheckboxes(filter);
        this.topic.loadFilterFields(filter);
        FilterUtilities.updateDocbookOptionsFilter(filter, this.docbookBuildingOptions);
    }

    @Override
    public String loadFilterAndSearch() {
        loadFilter();
        return doSearch();
    }

    /**
     * This function synchronizes the tags selected in the gui with the FilterTags in the Filter object, and saves the changes
     * to the database. This persists the currently selected filter.
     */
    @Override
    public void saveFilter() {
        try {
            final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
            Filter filter;

            // load the filter object if it exists
            if (this.selectedFilter != null) filter = entityManager.find(Filter.class, selectedFilter);
            else
                // get a reference to the Filter object
                filter = new Filter();

            if (validateFilterName(filter, selectedFilterName)) {
                // set the name
                filter.setFilterName(this.selectedFilterName);

                // populate the filter with the options that are selected in the ui
                syncFilterWithUI(filter);

                // save the changes
                entityManager.persist(filter);
                this.selectedFilter = filter.getFilterId();

                getFilterList();
            } else {
                this.setDisplayMessage("The filter requires a unique name");
            }
        } catch (final PersistenceException ex) {
            if (ex.getCause() instanceof ConstraintViolationException) {
                log.warn("Probably a constraint violation", ex);
                this.setDisplayMessage("The filter requires a unique name");
            } else {
                log.error("Probably an error saving the Filter", ex);
                this.setDisplayMessage("The filter could not be saved");
            }
        } catch (final Exception ex) {
            log.error("Probably an error saving the Filter", ex);
            this.setDisplayMessage("The filter could not be saved");
        }
    }

    public void loadFilterAndContentSpec() {
        this.loadFilter();
        this.generateContentSpec();
    }

    public void generateContentSpec() {
        final Filter filter = new Filter();
        this.syncFilterWithUI(filter);

        final CriteriaQuery<TranslatedTopicData> query = FilterUtilities.buildQuery(filter,
                new TranslatedTopicDataFilterQueryBuilder(entityManager));
        final List<TranslatedTopicData> topics = entityManager.createQuery(query).getResultList();

        final ContentSpecGenerator csGenerator = new ContentSpecGenerator(entityManager);
        final byte[] contentSpecs = csGenerator.generateContentSpecFromTranslatedTopics(topics, getDocbookBuildingOptions());

        final DateFormat dateFormatter = new SimpleDateFormat("dd-MM-YYYY");

        HTTPUtilities.writeOutContent(contentSpecs, "ContentSpecTranslations-" + dateFormatter.format(new Date()) + ".zip",
                MIMEUtilities.ZIP_MIME_TYPE);
    }
}
