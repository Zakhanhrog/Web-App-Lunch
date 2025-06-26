package com.example.lunchapp.config;

import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        System.out.println("====== AppInitializer: onStartup - BEGIN ======"); // LOG
        super.onStartup(servletContext);
        System.out.println("====== AppInitializer: onStartup - END ======");   // LOG
    }

    @Override
    protected Class<?>[] getRootConfigClasses() {
        System.out.println("====== AppInitializer: getRootConfigClasses ======"); // LOG
        return new Class[]{AppConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        System.out.println("====== AppInitializer: getServletConfigClasses ======"); // LOG
        return new Class[]{WebConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        System.out.println("====== AppInitializer: getServletMappings ======"); // LOG
        return new String[]{"/"};
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        System.out.println("====== AppInitializer: customizeRegistration ======"); // LOG
        registration.setInitParameter("throwExceptionIfNoHandlerFound", "true");
    }

    @Override
    protected Filter[] getServletFilters() {
        System.out.println("====== AppInitializer: getServletFilters ======"); // LOG
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding("UTF-8");
        characterEncodingFilter.setForceEncoding(true);
        return new Filter[]{characterEncodingFilter};
    }
}