package org.jboss.pressgang.ccms.seam.session;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.PersistenceException;

import org.hibernate.exception.ConstraintViolationException;
import org.jboss.pressgang.ccms.restserver.entity.ImageFile;
import org.jboss.pressgang.ccms.restserver.entity.LanguageImage;
import org.jboss.pressgang.ccms.restserver.utils.Constants;
import org.jboss.pressgang.ccms.utils.common.StringUtilities;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.framework.EntityHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Name("imageFileHome")
public class ImageFileHome extends EntityHome<ImageFile> implements DisplayMessageInterface {
    private static final Logger log = LoggerFactory.getLogger(ImageFileHome.class);

    /** Serializable version identifier */
    private static final long serialVersionUID = 554234315046093282L;
    /** The message to be displayed to the user */
    private String displayMessage;
    /** The locale displayed in the "new language" drop down box */
    private String locale;

    private LanguageImage newLanguageImage = new LanguageImage();
    private List<String> entityLocales = new ArrayList<String>();

    public void populate() {
        if (this.instance.getLanguageImages() != null) {
            for (final LanguageImage langImg : this.instance.getLanguageImages()) {
                entityLocales.add(langImg.getLocale());
            }
        }
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(final String locale) {
        this.locale = locale;
    }

    public void setImageFileImageFileId(final Integer id) {
        setId(id);
    }

    public Integer getImageFileImageFileId() {
        return (Integer) getId();
    }

    @Override
    protected ImageFile createInstance() {
        ImageFile imageFile = new ImageFile();
        return imageFile;
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

    public ImageFile getDefinedInstance() {
        return isIdDefined() ? getInstance() : null;
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

    @Override
    public String persist() {
        try {
            return super.persist();
        } catch (final PersistenceException ex) {
            if (ex.getCause() instanceof ConstraintViolationException) {
                this.setDisplayMessage("The image violated a constraint");
                log.warn("Probably a constraint violation", ex);
            } else {
                log.error("Probably an error updating a ImageFile or LanguageFile entity", ex);
                this.setDisplayMessage("The image could not be saved. " + Constants.GENERIC_ERROR_INSTRUCTIONS);
                this.setDisplayMessage("The image could not be saved. You may be trying to save translated images with different file extensions. Otherwise "
                        + StringUtilities.uncapatiliseFirstCharatcer(Constants.GENERIC_ERROR_INSTRUCTIONS));
            }
        } catch (final Exception ex) {
            log.error("Probably an error updating a ImageFile or LanguageFile entity", ex);
            this.setDisplayMessage("The image could not be saved. You may be trying to save translated images with different file extensions. Otherwise "
                    + StringUtilities.uncapatiliseFirstCharatcer(Constants.GENERIC_ERROR_INSTRUCTIONS));
        }

        return null;
    }

    @Override
    public String update() {
        try {
            return super.update();
        } catch (final PersistenceException ex) {
            if (ex.getCause() instanceof ConstraintViolationException) {
                this.setDisplayMessage("The image violated a constraint");
                log.warn("Probably a constraint violation", ex);
            } else {
                this.setDisplayMessage("The image could not be saved. You may be trying to save translated images with different file extensions. Otherwise "
                        + StringUtilities.uncapatiliseFirstCharatcer(Constants.GENERIC_ERROR_INSTRUCTIONS));
                log.error("Probably an error updating a ImageFile or LanguageFile entity", ex);
            }
        } catch (final Exception ex) {
            log.error("Probably an error updating a ImageFile or LanguageFile entity", ex);
            this.setDisplayMessage("The image could not be saved. You may be trying to save translated images with different file extensions. Otherwise "
                    + StringUtilities.uncapatiliseFirstCharatcer(Constants.GENERIC_ERROR_INSTRUCTIONS));
        }

        return null;
    }

    public void saveNewLanguageImage() {
        if (!this.entityLocales.contains(this.locale)) {
            newLanguageImage.setImageFile(this.instance);
            newLanguageImage.setLocale(locale);
            this.getInstance().getLanguageImages().add(newLanguageImage);
            this.entityLocales.add(locale);
            newLanguageImage = new LanguageImage();
        } else {
            this.displayMessage = "Language \"" + locale + "\" already exists";
        }
    }

    public void removeLanguageImage(final String removeLocale) {
        for (final LanguageImage langImg : this.instance.getLanguageImages()) {
            if (langImg.getLocale().equals(removeLocale)) {
                this.instance.getLanguageImages().remove(langImg);
                this.entityLocales.remove(removeLocale);
                break;
            }
        }
    }

    public LanguageImage getNewLanguageImage() {
        return newLanguageImage;
    }

    public void setNewLanguageImage(LanguageImage newLanguageImage) {
        this.newLanguageImage = newLanguageImage;
    }

    public List<String> getEntityLocales() {
        return entityLocales;
    }

    public void setEntityLocales(List<String> entityLocales) {
        this.entityLocales = entityLocales;
    }
}
