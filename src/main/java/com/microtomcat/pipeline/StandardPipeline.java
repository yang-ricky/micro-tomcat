package com.microtomcat.pipeline;

import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.lifecycle.LifecycleBase;
import com.microtomcat.lifecycle.LifecycleException;
// import com.microtomcat.pipeline.valve.Valve;
import com.microtomcat.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.microtomcat.lifecycle.Lifecycle;

public class StandardPipeline extends LifecycleBase implements Pipeline {
    private final List<Valve> valves = new ArrayList<>();
    private Valve basic = null;

    @Override
    protected void initInternal() throws LifecycleException {
        log("Initializing pipeline with " + valves.size() + " valves");
        // 初始化所有阀门
        for (Valve valve : valves) {
            log("Initializing valve: " + valve.getClass().getSimpleName());
            if (valve instanceof Lifecycle) {
                ((Lifecycle) valve).init();
            }
        }
        if (basic instanceof Lifecycle) {
            log("Initializing basic valve: " + basic.getClass().getSimpleName());
            ((Lifecycle) basic).init();
        }
    }

    @Override
    protected void startInternal() throws LifecycleException {
        log("Starting pipeline with " + valves.size() + " valves");
        // 启动所有阀门
        for (Valve valve : valves) {
            log("Starting valve: " + valve.getClass().getSimpleName());
            if (valve instanceof Lifecycle) {
                ((Lifecycle) valve).start();
            }
        }
        if (basic instanceof Lifecycle) {
            log("Starting basic valve: " + basic.getClass().getSimpleName());
            ((Lifecycle) basic).start();
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        log("Stopping pipeline with " + valves.size() + " valves");
        // 停止所有阀门
        for (Valve valve : valves) {
            log("Stopping valve: " + valve.getClass().getSimpleName());
            if (valve instanceof Lifecycle) {
                ((Lifecycle) valve).stop();
            }
        }
        if (basic instanceof Lifecycle) {
            log("Stopping basic valve: " + basic.getClass().getSimpleName());
            ((Lifecycle) basic).stop();
        }
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        log("Destroying pipeline with " + valves.size() + " valves");
        // 销毁所有阀门
        for (Valve valve : valves) {
            log("Destroying valve: " + valve.getClass().getSimpleName());
            if (valve instanceof Lifecycle) {
                ((Lifecycle) valve).destroy();
            }
        }
        if (basic instanceof Lifecycle) {
            log("Destroying basic valve: " + basic.getClass().getSimpleName());
            ((Lifecycle) basic).destroy();
        }
    }

    @Override
    public void addValve(Valve valve) {
        valves.add(valve);
    }

    @Override
    public Valve[] getValves() {
        return valves.toArray(new Valve[0]);
    }

    @Override
    public void removeValve(Valve valve) {
        valves.remove(valve);
    }

    @Override
    public Valve getBasic() {
        return basic;
    }

    @Override
    public void setBasic(Valve valve) {
        this.basic = valve;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        // 创建阀门上下文并开始处理
        StandardPipelineValveContext context = new StandardPipelineValveContext();
        context.invokeNext(request, response);
    }

    private void log(String message) {
        System.out.println("[Pipeline] " + message);
    }

    // 内部阀门上下文类
    private class StandardPipelineValveContext implements ValveContext {
        private int stage = 0;

        @Override
        public void invokeNext(Request request, Response response) 
                throws IOException, ServletException {
            int subscript = stage;
            stage++;
            
            // 首先调用所有普通阀门
            if (subscript < valves.size()) {
                valves.get(subscript).invoke(request, response, this);
            }
            // 最后调用基础阀门
            else if (subscript == valves.size() && basic != null) {
                basic.invoke(request, response, this);
            }
        }
    }
} 