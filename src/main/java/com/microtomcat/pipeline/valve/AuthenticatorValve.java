package com.microtomcat.pipeline.valve;

import com.microtomcat.pipeline.Valve;
import com.microtomcat.pipeline.ValveContext;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.session.Session;
import java.io.IOException;
import com.microtomcat.servlet.ServletException;

public class AuthenticatorValve implements Valve {
    @Override
    public void invoke(Request request, Response response, ValveContext context) 
            throws IOException, ServletException {
        System.out.println("[AuthenticatorValve] Processing request ..");
        
        // 获取或创建会话
        Session session = request.getSession(true);
        
        // 检查是否需要认证
        // TODO: 在未来实现更复杂的认证逻辑
        
        // 继续处理管道中的下一个阀门
        context.invokeNext(request, response);
    }
} 