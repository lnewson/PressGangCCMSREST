package org.jboss.pressgang.ccms.restserver.servlet.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortRedirectFilter implements Filter {
    private static Logger log = LoggerFactory.getLogger(PortRedirectFilter.class);
    private String sourcePort;
    private String destinationPort;
    private List<String> ignoreSourceHosts = new ArrayList<String>();
    boolean enabled = false;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.debug("Initialising the Port Redirect Filter");

        // Load the source and destination ports
        sourcePort = filterConfig.getInitParameter("sourcePort");
        destinationPort = filterConfig.getInitParameter("destPort");
        String ignoreHosts = filterConfig.getInitParameter("ignoreHost");
        if (ignoreHosts != null) {
            String[] hosts = ignoreHosts.split("\\s*,\\s*");
            for (String host : hosts) {
                ignoreSourceHosts.add(host);
            }
        }

        // ignore local host
        ignoreSourceHosts.add("127.0.0.1");
        ignoreSourceHosts.add("localhost");

        // If the source and destination port is specified than enable the filter
        if (sourcePort != null && destinationPort != null) {
            enabled = true;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (enabled && request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            final HttpServletRequest httpReq = (HttpServletRequest) request;

            // If the host should be ignored then continue along the filter chain otherwise redirect
            if (ignoreSourceHosts.contains(request.getRemoteHost())) {
                filterChain.doFilter(request, response);
            } else {
                String redirectTarget = httpReq.getRequestURL().toString() + "?" + httpReq.getQueryString();
                redirectTarget = redirectTarget.replaceFirst(":" + sourcePort, ":" + destinationPort);

                ((HttpServletResponse) response).sendRedirect(redirectTarget);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        log.debug("Destroying the Port Redirect Filter");
        enabled = false;
        sourcePort = null;
        destinationPort = null;
        ignoreSourceHosts.clear();
    }
}
