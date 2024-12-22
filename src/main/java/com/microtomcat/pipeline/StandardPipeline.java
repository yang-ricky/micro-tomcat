package com.microtomcat.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.microtomcat.connector.Request;
import com.microtomcat.connector.Response;
import com.microtomcat.servlet.ServletException;

public class StandardPipeline implements Pipeline {
    private final List<Valve> valves = new ArrayList<>();
    private Valve basic = null;

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
    public void invoke(Request request, Response response) 
            throws IOException, ServletException {
        StandardPipelineValveContext context = 
            new StandardPipelineValveContext(valves, basic);
        context.invokeNext(request, response);
    }

    private static class StandardPipelineValveContext implements ValveContext {
        private final Valve[] valves;
        private final Valve basic;
        private int stage = 0;

        public StandardPipelineValveContext(List<Valve> valveList, Valve basic) {
            this.valves = valveList.toArray(new Valve[0]);
            this.basic = basic;
        }

        @Override
        public void invokeNext(Request request, Response response) 
                throws IOException, ServletException {
            int subscript = stage++;
            
            if (subscript < valves.length) {
                valves[subscript].invoke(request, response, this);
            } else if (basic != null) {
                basic.invoke(request, response, this);
            }
        }
    }
} 