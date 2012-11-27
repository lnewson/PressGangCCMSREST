package org.jboss.pressgang.ccms.seam.utils;

public class Constants extends org.jboss.pressgang.ccms.restserver.utils.Constants {
    /**
     * The Skynet build number, displayed on the top bar of all Skynet pages. Is in the format yyyymmdd-hhmm
     */
    public static final String BUILD = "20121127-1502";

    /** The initial name for a translated topic revision */
    public static final String INITIAL_TRANSLATED_TOPIC_REVISION_NAME = "Initial Untranslated Revision";

    /** The HTML returned when a Topic's XML could not be transformed */
    public static final String XSL_ERROR_TEMPLATE = "<html><head><title>ERROR</title></head><body>The topic could not be transformed into HTML</body></html>";

    /** The base URL from which the REST interface can be accessed */
    public static final String BASE_REST_PATH = "/seam/resource/rest";

    /*
     * TODO: These tag and category ids should probably come from a configuration file instead of being hard coded. Any changes
     * to the tags will break the docbook compilation, and require this source code to be modified to reflect the new tag ids.
     * 
     * Generally speaking, tags referenced here should eventually become fields on a topic.
     */
    public static final Integer TYPE_CATEGORY_ID = 4;
    public static final Integer TECHNOLOGY_CATEGORY_ID = 3;
    public static final Integer RELEASE_CATEGORY_ID = 15;
    public static final Integer WRITER_CATEGORY_ID = 12;
    public static final Integer COMMON_NAME_CATEGORY_ID = 17;
    public static final String TECHNOLOGY_CATEGORY_NAME = "Technologies";
    public static final Integer CONCERN_CATEGORY_ID = 2;
    public static final String CONCERN_CATEGORY_NAME = "Concerns";
    public static final Integer LIFECYCLE_CATEGORY_ID = 5;
}
