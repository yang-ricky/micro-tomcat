package com.microtomcat.servlet;

import javax.servlet.ServletConfig;

@Deprecated
public interface Servlet extends javax.servlet.Servlet {
    // 保留无参的init方法用于向后兼容
    @Deprecated
    default void init() throws javax.servlet.ServletException {
        init(null);  // 调用标准的init(ServletConfig)
    }
}