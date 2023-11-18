package template;

import datastructures.Pair;
import logist.agent.Agent;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static algorithms.AStar.aStarPlan;

public class Control {
    /** CONTROL MAPS**/
    /**
     *  - assignedTasks keeps track of the task that each vehicle is taken in an unorder manner
     *  - plans contains the so far computed plan for each vehicle
     */
    private static HashMap<Vehicle, List<Task>> assignedTasks;
    private static HashMap<Vehicle, Plan> plans;

    /**********************/

    /** TMP VARIABLES**/
    /**
     * Used to store the information regarding the plan with lowest marginal cost
     * selectedPlan and selectedVehicle are used to update the control maps if task was to be assigned
     */
    private static Plan selectedPlan;
    private static Vehicle selectedVehicle;

    /**********************/

    Control(Agent agent) {
        this.assignedTasks = new HashMap<>();
        this.plans = new HashMap<>();
        this.selectedPlan = Plan.EMPTY;
        this.selectedVehicle = null;
        for (Vehicle vehicle : agent.vehicles()) {
            assignedTasks.put(vehicle, new ArrayList<>());
            plans.put(vehicle, new Plan(vehicle.homeCity(), new ArrayList<>()));
        }
    }

    public double getLowestMarginalCost(Task task) {
        double lowestMarginalCost = Double.MAX_VALUE;
        double vehicleMarginalCost;
        Plan newPlan;
        Pair vehicleResults;

        // Compute marginal cost for all vehicles and keep the lowest
        for (Vehicle vehicle : plans.keySet()) {
            // newPlan will be updated with the new computed plan
            vehicleResults = computeMarginalCost(vehicle, task);
            vehicleMarginalCost = (double)vehicleResults.t;
            newPlan = (Plan) vehicleResults.u;

            System.out.println(vehicle.name() + " marginal cost: " + vehicleMarginalCost);
            // if marginal cost is lower than current is good for us
            if (vehicleMarginalCost < lowestMarginalCost) {
                lowestMarginalCost = vehicleMarginalCost;

                // store best plan and vehicle for the task to be assigned
                // they will be used later if the bid is won
                selectedPlan = newPlan;
                selectedVehicle = vehicle;
            }
        }
        return lowestMarginalCost;
    }

    public void updateControlVariablesIfTaskWon(Task task) {
        List<Task> newVehicleTasks = assignedTasks.get(selectedVehicle);
        newVehicleTasks.add(task);
        assignedTasks.put(selectedVehicle, newVehicleTasks);
        plans.replace(selectedVehicle,selectedPlan);
        System.out.println("task " + task.id + " won by " + selectedVehicle.name());
    }

    private Pair computeMarginalCost(Vehicle vehicle, Task task) {
        // add the new task to the already assigned tasks for that vehicle
        List<Task> tmpAssignedTasks = assignedTasks.get(vehicle);
        tmpAssignedTasks.add(task);

        // compute new plan with new task
        State initialState = new State(vehicle, tmpAssignedTasks);
        Plan newPlan = aStarPlan(vehicle, tmpAssignedTasks, initialState);
        // compute marginal cost to deliver the new task compared to the already assigned plan
        double marginalCost = (newPlan.totalDistance() - plans.get(vehicle).totalDistance()) * vehicle.costPerKm();
        return new Pair(marginalCost, newPlan);
    }
}
