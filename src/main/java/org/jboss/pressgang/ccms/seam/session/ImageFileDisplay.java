package org.jboss.pressgang.ccms.seam.session;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.pressgang.ccms.restserver.entity.ImageFile;
import org.jboss.pressgang.ccms.restserver.entity.LanguageImage;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Name("imageFileDisplay")
public class ImageFileDisplay {
    private static final Logger log = LoggerFactory.getLogger(ImageFileDisplay.class);

    @In
    private EntityManager entityManager;
    @In(value = "#{facesContext.externalContext}")
    private ExternalContext extCtx;
    @In(value = "#{facesContext}")
    private FacesContext facesContext;

    private String imageFileId;
    private String language;

    public void download() {
        try {
            final Integer imageFileIdInteger = Integer.parseInt(imageFileId.trim());

            final ImageFile file = entityManager.find(ImageFile.class, imageFileIdInteger);

            final HttpServletResponse response = (HttpServletResponse) extCtx.getResponse();

            response.setContentType(file.getMimeType());

            final ServletOutputStream os = response.getOutputStream();

            /*
             * If no language was specified use the system default image, otherwise find the translated image.
             */
            if (language == null || language.isEmpty()) {
                for (final LanguageImage langImg : file.getLanguageImages()) {
                    if (langImg.getLocale().equals(CommonConstants.DEFAULT_LOCALE)) {
                        os.write(langImg.getImageData());
                        break;
                    }
                }
            } else {
                for (final LanguageImage langImg : file.getLanguageImages()) {
                    if (langImg.getLocale().equals(language)) {
                        os.write(langImg.getImageData());
                        break;
                    }
                }
            }

            os.flush();
            os.close();
            facesContext.responseComplete();
        } catch (final Exception ex) {
            log.error("An error writing an image to the http output stream", ex);
        }
    }

    public String getImageFileId() {
        return imageFileId;
    }

    public void setImageFileId(final String imageFileId) {
        this.imageFileId = imageFileId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

}
