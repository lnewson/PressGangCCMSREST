package org.jboss.pressgang.ccms.restserver.rest.v1.interceptor;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import java.util.List;

import org.jboss.pressgang.ccms.rest.v1.constants.RESTv1Constants;
import org.jboss.pressgang.ccms.rest.v1.jaxrsinterfaces.RESTInterfaceV1;
import org.jboss.pressgang.ccms.utils.common.VersionUtilities;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;

@ServerInterceptor
public class RESTVersionInterceptor implements PreProcessInterceptor {
    private static final int UPGRADE_STATUS_CODE = 426;
    private static final String REST_VERSION = VersionUtilities.getAPIVersion(RESTInterfaceV1.class);

    private static final String REST_VERSION_ERROR_MSG = "The REST Client Implementation is out of date, " +
            "" + "and no longer supported. Please update the REST Client library.";
    private static final String CSP_VERSION_ERROR_MSG = "The csprocessor application is out of date, " +
            "" + "and no longer compatible. Please update the csprocessor application.";

    @Override
    public ServerResponse preProcess(HttpRequest request, ResourceMethod method) throws Failure, WebApplicationException {
        if (request != null && request.getHttpHeaders() != null && request.getHttpHeaders().getRequestHeaders() != null) {
            final HttpHeaders headers = request.getHttpHeaders();
            if (headers.getRequestHeaders() != null) {
                // REST API Version Check
                final List<String> RESTVersions = headers.getRequestHeader(RESTv1Constants.X_VERSION_HEADER);
                if (RESTVersions != null) {
                    for (final String version : RESTVersions) {
                        if (!isValidVersion(version)) {
                            return new ServerResponse(REST_VERSION_ERROR_MSG, UPGRADE_STATUS_CODE, new Headers<Object>());
                        }
                    }
                }

                // CSP Version Check
                final List<String> CSPVersions = headers.getRequestHeader(RESTv1Constants.X_CSP_VERSION_HEADER);
                if (CSPVersions != null) {
                    for (final String version : CSPVersions) {
                        if (!isValidCSPVersion(version)) {
                            return new ServerResponse(CSP_VERSION_ERROR_MSG, UPGRADE_STATUS_CODE, new Headers<Object>());
                        }
                    }
                }
            }

            return null;
        }

        return null;
    }

    /**
     * Checks to see if the REST Version is valid and compatible with the server.
     *
     * @param version The REST Version from the "X-Version" HTTP Header.
     * @return True if the REST Version is compatible, otherwise false.
     */
    protected boolean isValidVersion(final String version) {
        if (isUnknownVersion(version)) return true;
        if (!isSnapshotVersion(REST_VERSION) && isSnapshotVersion(version)) return false;

        final Integer majorVersion = getMajorVersion(version);
        final Integer minorVersion = getMinorVersion(version);

        switch (majorVersion) {
            case 1:
                // Check that the minor version is 0 or higher
                return (minorVersion == null || minorVersion >= 0);
            default:
                return false;
        }
    }

    protected boolean isValidCSPVersion(final String version) {
        if (isUnknownVersion(version)) return true;
        if (isSnapshotVersion(version)) return false;

        final Integer majorVersion = getMajorVersion(version);
        final Integer minorVersion = getMinorVersion(version);

        // Check that the version is 0.28 or higher.
        return (majorVersion != null && majorVersion >= 0) && (minorVersion == null || minorVersion >= 28);
    }

    protected Integer getMajorVersion(final String version) {
        // Remove any extra information, ie -SNAPSHOT
        final String cleanedVersion = version.replaceAll("-.*", "");
        String[] tmp = cleanedVersion.split("\\.");
        return Integer.parseInt(tmp[0]);
    }

    protected Integer getMinorVersion(final String version) {
        // Remove any extra information, ie -SNAPSHOT
        final String cleanedVersion = version.replaceAll("-.*", "");
        String[] tmp = cleanedVersion.split("\\.");
        return tmp.length >= 2 ? Integer.parseInt(tmp[1]) : null;
    }

    protected boolean isSnapshotVersion(final String version) {
        return version.contains("-SNAPSHOT");
    }

    protected boolean isUnknownVersion(final String version) {
        return version.toLowerCase().equals("unknown");
    }
}
