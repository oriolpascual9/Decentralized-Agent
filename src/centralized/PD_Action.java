package centralized;

import logist.task.Task;

// Class for each pickup or delivery action: Tuple of pickup/delivery category and associated task
public class PD_Action {
    public final boolean is_pickup;	// boolean variable that takes True if pickup and False if delivery
    public Task task;	// task associated with this action

    public PD_Action(boolean pickup_switch, Task task) {
        this.is_pickup = pickup_switch;
        this.task = task;
    }
}
