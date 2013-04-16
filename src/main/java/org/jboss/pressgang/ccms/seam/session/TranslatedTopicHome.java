package org.jboss.pressgang.ccms.seam.session;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.pressgang.ccms.model.TranslatedTopic;
import org.jboss.pressgang.ccms.model.TranslatedTopicData;
import org.jboss.pressgang.ccms.restserver.utils.EnversUtilities;
import org.jboss.pressgang.ccms.restserver.utils.TopicUtilities;
import org.jboss.pressgang.ccms.restserver.utils.TranslatedTopicUtilities;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.pressgang.ccms.zanata.ZanataConstants;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

@Name("translatedTopicHome")
public class TranslatedTopicHome extends VersionedEntityHome<TranslatedTopic> implements DisplayMessageInterface {

    private static final long serialVersionUID = 1303303878602135396L;

    @In(create = true)
    private TranslatedTopicDataHome translatedTopicDataHome;
    @In
    private EntityManager entityManager;

    /** The current locale being viewed */
    private String translatedTopicDataLocale = CommonConstants.DEFAULT_LOCALE;
    /** The message to be displayed to the user */
    private String displayMessage;
    /**
     * The name of the tab that is to be selected when the tab panel is displayed
     */
    private String selectedTab;
    /** The string representation of the list of tags for the translated topic */
    private String topicTags;

    public void setTranslatedTopicId(Integer id) {
        setId(id);
    }

    public Integer getTranslatedTopicId() {
        return (Integer) getId();
    }

    @Override
    protected TranslatedTopic createInstance() {
        TranslatedTopic translatedTopic = new TranslatedTopic();
        return translatedTopic;
    }

    public void load() {
        if (isIdDefined()) {
            wire();
        }
    }

    public void wire() {
        getInstance();
    }

    public boolean isWired() {
        return true;
    }

    public TranslatedTopic getDefinedInstance() {
        return isIdDefined() ? getInstance() : null;
    }

    public void populate() {
        refreshEntity();

        /* Compare all the translations to find the translation that matches the translated topic and locale */
        for (TranslatedTopicData translatedData : getInstance().getTranslatedTopicDatas()) {
            if (translatedData.getTranslatedTopic().getId().equals(getId())
                    && translatedData.getTranslationLocale().equals(translatedTopicDataLocale)) {
                translatedTopicDataHome.setTranslatedTopicDataId(translatedData.getId());
                break;
            }
        }

        /* Check that a translation was found */
        if (translatedTopicDataHome.getInstance() == null) {
            throw new EntityNotFoundException();
        }

        translatedTopicDataHome.refreshEntity();

        /* populate the topic tags */
        topicTags = TopicUtilities.getTagsList(
                translatedTopicDataHome.getInstance().getTranslatedTopic().getEnversTopic(entityManager), true);
    }

    public List<String> getLocaleList() {
        if (translatedTopicDataHome.getInstance() == null)
            populate();

        /* populate the list of locales from the translation data for the topic */
        return getInstance().getTranslatedTopicDataLocales();
    }

    public String getTranslatedTopicDataLocale() {
        return translatedTopicDataLocale.toString();
    }

    public void setTranslatedTopicDataLocale(String locale) {
        /* Default to en_US if no locale is specified */
        if (locale == null)
            locale = CommonConstants.DEFAULT_LOCALE;

        if (setDirty(this.translatedTopicDataLocale, locale))
            setInstance(null);
        this.translatedTopicDataLocale = locale;

        /* the locale is the only extra requirement to be able to populate the fields */
        populate();
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

    public void setDisplayMessage(final String displayMessage) {
        this.displayMessage = displayMessage;
    }

    public void redirectToZanata() {
        /* Get the zanata properties from the server properties */
        final String zanataServerUrl = System.getProperty(ZanataConstants.ZANATA_SERVER_PROPERTY);
        final String zanataProject = System.getProperty(ZanataConstants.ZANATA_PROJECT_PROPERTY);
        final String zanataVersion = System.getProperty(ZanataConstants.ZANATA_PROJECT_VERSION_PROPERTY);

        /* Create the zanata url for the translated topic and locale */
        final String link = zanataServerUrl + "/webtrans/Application.html?project=" + zanataProject + "&iteration="
                + zanataVersion + "&localeId=" + translatedTopicDataLocale + "&doc=" + getInstance().getZanataId()
                + "#view:doc;doc:" + getInstance().getZanataId();

        /* Redirect the user to the zanata Url */
        final ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        try {
            externalContext.redirect(link);
            FacesContext.getCurrentInstance().responseComplete();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getSelectedTab() {
        return selectedTab;
    }

    public void setSelectedTab(String selectedTab) {
        this.selectedTab = selectedTab;
    }

    public String getRenderedUrl() {
        String url = "RenderTranslatedTopic.seam?translatedTopicDataId="
                + translatedTopicDataHome.getInstance().getTranslatedTopicDataId();
        if (translatedTopicDataHome.getRevision() != null)
            url += "&amp;translatedTopicDataRevision=" + translatedTopicDataHome.getRevision();
        return url;
    }

    public String getTopicTags() {
        return topicTags;
    }

    public void setTopicTags(String topicTags) {
        this.topicTags = topicTags;
    }

    public void syncWithZanata() {
        TranslatedTopicUtilities.pullFromZanata(this.getInstance());
    }

    public List<Number> getTranslatedTopicDataRevisions() {
        if (translatedTopicDataHome.getInstance() == null)
            populate();

        List<Number> revisions = new ArrayList<Number>(EnversUtilities.getRevisions(entityManager, translatedTopicDataHome.getInstance()));
        return revisions;
    }

    public void refreshEntity() {
        if (this.isManaged())
            this.getEntityManager().refresh(this.getInstance());
    }
}