package org.jboss.pressgang.ccms.seam.session.base;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

import org.jboss.pressgang.ccms.docbook.compiling.DocbookBuildingOptions;
import org.jboss.pressgang.ccms.filter.TopicFieldFilter;
import org.jboss.pressgang.ccms.model.Filter;
import org.jboss.pressgang.ccms.seam.session.DisplayMessageInterface;
import org.jboss.pressgang.ccms.seam.utils.structures.tags.UIProjectsData;
import org.jboss.seam.Component;

public abstract class TopicSearchBase implements DisplayMessageInterface {
    /**
     * a mapping of project and category details to tag details with sorting and
     * selection information
     */
    protected UIProjectsData selectedTags = new UIProjectsData();

    /**
     * The id of the Filter object that has been loaded
     */
    protected Integer selectedFilter;
    /**
     * The name of the Filter object that has been loaded
     */
    protected String selectedFilterName;
    /**
     * A list of Filters from the database, used to populate the drop down list
     */
    protected List<Filter> filters = new ArrayList<Filter>();
    /**
     * The data structure that holds the docbook building options
     */
    protected DocbookBuildingOptions docbookBuildingOptions = new DocbookBuildingOptions();
    /**
     * The message to be displayed to the user
     */
    protected String displayMessage;

    protected TopicFieldFilter topic = new TopicFieldFilter();

    public void setDocbookBuildingOptions(final DocbookBuildingOptions docbookBuildingOptions) {
        this.docbookBuildingOptions = docbookBuildingOptions;
    }

    public DocbookBuildingOptions getDocbookBuildingOptions() {
        return docbookBuildingOptions;
    }

    public void setTopic(final TopicFieldFilter topic) {
        this.topic = topic;
    }

    public TopicFieldFilter getTopic() {
        return topic;
    }

    public void setSelectedFilter(final Integer selectedFilter) {
        this.selectedFilter = selectedFilter;
    }

    public Integer getSelectedFilter() {
        return selectedFilter;
    }

    public void setFilters(final List<Filter> filters) {
        this.filters = filters;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public UIProjectsData getSelectedTags() {
        return selectedTags;
    }

    public void setSelectedTags(final UIProjectsData value) {
        selectedTags = value;
    }

    public void setSelectedFilterName(final String selectedFilterName) {
        this.selectedFilterName = selectedFilterName;
    }

    public String getSelectedFilterName() {
        return selectedFilterName;
    }

    @Override
    public String getDisplayMessage() {
        return displayMessage;
    }

    @Override
    public String getDisplayMessageAndClear() {
        final String retValue = this.displayMessage;
        this.displayMessage = null;
        return retValue;
    }

    public void setDisplayMessage(String displayMessage) {
        this.displayMessage = displayMessage;
    }

    protected boolean validateDocbookBuildOptions(final DocbookBuildingOptions docbookBuildingOptions) {
        if (docbookBuildingOptions.isValid()) return true;

        final List<String> errors = new ArrayList<String>();

        if (docbookBuildingOptions.getBookTitle() == null || docbookBuildingOptions.getBookTitle().isEmpty()) {
            errors.add("Book Title must not be empty");
        }

        if (docbookBuildingOptions.getBookProduct() == null || docbookBuildingOptions.getBookProduct().isEmpty()) {
            errors.add("Book Product must not be empty");
        }

        if (docbookBuildingOptions.getBookProductVersion() == null || docbookBuildingOptions.getBookProductVersion().isEmpty() ||
                !docbookBuildingOptions.getBookProductVersion().matches(
                "[0-9]+(.[0-9+](.[0-9]+)?)?")) {
            errors.add("Book Product Version must be a valid version format (x.x.x)");
        }

        if (docbookBuildingOptions.getBookEdition() != null && !docbookBuildingOptions.getBookEdition().isEmpty() &&
                !docbookBuildingOptions.getBookEdition().matches(
                "[0-9]+(.[0-9+](.[0-9]+)?)?")) {
            errors.add("Book Edition must be a valid version format (x.x.x)");
        }

        if (docbookBuildingOptions.getBookPubsnumber() != null && !docbookBuildingOptions.getBookPubsnumber().isEmpty() &&
                !docbookBuildingOptions.getBookPubsnumber().matches(
                "[0-9]+")) {
            errors.add("Book Pubsnumber must be a valid number");
        }

        if (errors.size() > 0) {
            this.displayMessage = "";
            for (final String error : errors) {
                this.displayMessage += error + "\\n";
            }

            this.displayMessage = this.displayMessage.substring(0, this.displayMessage.length() - 2);
        }

        return errors.size() == 0;
    }

    protected boolean validateFilterName(final Filter filter, final String newFilterName) {
        if (filter.getFilterName() != null && filter.getFilterName().equals(newFilterName)) return true;
        else if (filter.getFilterName() == null && newFilterName != null && !newFilterName.isEmpty()) return true;

        final EntityManager entityManager = (EntityManager) Component.getInstance("entityManager");
        final String queryString = Filter.SELECT_ALL_QUERY + " WHERE filter.filterName = :filterName AND filter.filterId != '" + filter
                .getFilterId() + "'";
        final Query query = entityManager.createQuery(queryString);
        query.setParameter("filterName", newFilterName);

        return query.getResultList().size() == 0;
    }

    public abstract void loadFilter();

    public abstract String loadFilterAndSearch();

    public abstract void saveFilter();

    protected abstract void syncFilterWithUI(final Filter filter);
}
