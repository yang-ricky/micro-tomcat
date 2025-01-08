package com.microtomcat.filter;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FilterManager {
    // 使用 LinkedHashMap 保持插入顺序
    private final Map<String, Filter> filterMap = new LinkedHashMap<>();
    private final Map<String, String> filterMappings = new LinkedHashMap<>();
    private final ServletContext servletContext;
    
    public FilterManager(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
    
    public void addFilter(String filterName, Filter filter) {
        try {
            // 初始化filter
            FilterConfig filterConfig = new ApplicationFilterConfig(filterName, servletContext);
            filter.init(filterConfig);
            filterMap.put(filterName, filter);
            System.out.println("Added filter: " + filterName); // 添加日志
        } catch (ServletException e) {
            throw new RuntimeException("Failed to initialize filter: " + filterName, e);
        }
    }
    
    public void addFilterMapping(String urlPattern, String filterName) {
        // 规范化 URL 模式
        if (!urlPattern.startsWith("/") && !urlPattern.startsWith("*")) {
            urlPattern = "/" + urlPattern;
        }
        filterMappings.put(filterName, urlPattern); // 注意：键值对调换了
        System.out.println("Added filter mapping: " + filterName + " -> " + urlPattern); // 添加日志
    }
    
    public ApplicationFilterChain createFilterChain(String requestUri) {
        ApplicationFilterChain filterChain = new ApplicationFilterChain();
        System.out.println("Creating filter chain for URI: " + requestUri);
        
        // 遍历所有过滤器，按添加顺序处理
        for (Map.Entry<String, Filter> entry : filterMap.entrySet()) {
            String filterName = entry.getKey();
            String urlPattern = filterMappings.get(filterName);
            
            System.out.println("Checking filter: " + filterName + " with pattern: " + urlPattern);
            
            if (urlPattern != null && matchFiltersURL(requestUri, urlPattern)) {
                Filter filter = entry.getValue();
                filterChain.addFilter(filter);
            }
        }
        
        return filterChain;
    }
    
    private boolean matchFiltersURL(String uri, String urlPattern) {
        
        // 处理 /* 模式
        if (urlPattern.equals("/*")) {
            return true;
        }
        
        // 处理 /path/* 模式
        if (urlPattern.endsWith("/*")) {
            String prefix = urlPattern.substring(0, urlPattern.length() - 2);
            // 确保 uri 以 prefix 开头，并且后面跟着 / 或者是完全匹配
            boolean matches = uri.equals(prefix) || 
                            (uri.startsWith(prefix) && uri.charAt(prefix.length()) == '/');
            return matches;
        }
        
        // 处理 *.extension 模式
        if (urlPattern.startsWith("*.")) {
            String suffix = urlPattern.substring(2); // 改为从第2个字符开始，跳过 *.
            boolean matches = uri.endsWith("." + suffix);
            return matches;
        }
        
        // 精确匹配
        boolean matches = uri.equals(urlPattern);
        return matches;
    }
    
    public void destroy() {
        for (Filter filter : filterMap.values()) {
            try {
                filter.destroy();
            } catch (Exception e) {
                System.err.println("Error destroying filter: " + e.getMessage());
            }
        }
        filterMap.clear();
        filterMappings.clear();
    }
} 