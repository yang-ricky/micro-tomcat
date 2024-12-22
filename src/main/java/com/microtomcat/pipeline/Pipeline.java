package com.microtomcat.pipeline;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import java.io.IOException;
import com.microtomcat.servlet.ServletException;

public interface Pipeline {
    void addValve(Valve valve);
    Valve[] getValves();
    void removeValve(Valve valve);
    Valve getBasic();
    void setBasic(Valve valve);
    void invoke(Request request, Response response) throws IOException, ServletException;
} 