package com.vmware.bdd.web.servlet.filter;

import java.io.File;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.vmware.bdd.exception.BddException;
import com.vmware.bdd.utils.Constants;

public class MaintenanceModeAccessFilter implements Filter {

   private static final Logger logger = Logger.getLogger(MaintenanceModeAccessFilter.class);

   @SuppressWarnings("unused")
   private FilterConfig filterConfig = null;

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
      this.filterConfig = filterConfig;
   }

   @Override
   public void destroy() {
      this.filterConfig = null;
   }

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
         ServletException {

      HttpServletRequest req = (HttpServletRequest) request;
      HttpServletResponse resp = (HttpServletResponse) response;

      File file = new File(Constants.MAINTENANCE_MODE_FLAG_FILE);
      if (file.exists()) {
         // server is in maintenance mode
         String method = req.getMethod();
         if (!method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("HEAD")) {
            String msg = BddException.ACCESS_NOT_ALLOWED_IN_MAINTENANCE_MODE().getLocalizedMessage();
            logger.info(method + " " + req.getPathInfo() + " is not allowed in maintenance mode.");
            resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            resp.getWriter().write(msg);
            resp.getWriter().flush();
            return;
         }
      }

      chain.doFilter(request, response);
   }

}
