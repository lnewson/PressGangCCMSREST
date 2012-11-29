package org.jboss.pressgang.ccms.seam.interceptor;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.bull.javamelody.MonitoringFilter;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.security.Identity;
import org.jboss.seam.servlet.ContextualHttpServletRequest;

/**
 * Reworked filter copied from the Zanata Project's org.zanata.seam.interceptor.MonitoringWrapper class.
 */
public class MonitoringInterceptor extends MonitoringFilter
{

   @Override
   public void doFilter(final ServletRequest request, final ServletResponse response, FilterChain chain) throws IOException, ServletException
   {
      final HttpServletRequest httpRequest = (HttpServletRequest) request;
      final HttpServletResponse httpResponse = (HttpServletResponse) response;

      if (httpRequest.getRequestURI().equals(getMonitoringUrl(httpRequest)))
      {
         new ContextualHttpServletRequest((HttpServletRequest) request)
         {
            @Override
            public void process() throws Exception
            {
               Identity identity = (Identity) Component.getInstance(Identity.class, ScopeType.SESSION);
               if (identity == null || !identity.isLoggedIn())
               {
                  String signInUrl = httpRequest.getContextPath() + "/login.seam";
                  httpResponse.sendRedirect(signInUrl);
               }
               else if (!identity.hasRole("adminRole"))
               {
                  httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Only admin can access monitoring!");
               }
            }
         }.run();
         super.doFilter(request, response, chain);
      }
      else
      {
         chain.doFilter(request, response);
      }
   }

}