package template;

import logist.task.Task;
import logist.topology.Topology;
import logist.topology.Topology.City;


public class StateQV2 {
    private final City current_city;
    private final City dest_city;

    StateQV2(Topology.City curr, Topology.City dest) {
        this.current_city = curr;
        this.dest_city = dest;
    }

    City getCurrent_city(){
        return this.current_city;
    }

    City getDest_city(){
        return this.dest_city;
    }

}

