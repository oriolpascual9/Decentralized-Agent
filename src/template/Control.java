package template;

import centralized.Candidate;
import centralized.CentralizedTemplate;
import centralized.PD_Action;
import datastructures.Pair;
import logist.agent.Agent;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static algorithms.AStar.aStarPlan;
import static centralized.Candidate.ComputeCost;

public class Control {
    /** CONTROL MAPS**/
    /**
     *  - assignedTasks keeps track of the task that each vehicle is taken in an unorder manner
     *  - plans contains the so far computed plan for each vehicle
     */
    private static HashMap<Vehicle, List<Task>> assignedTasks;
    private static HashMap<Vehicle, List<PD_Action>> plans;

    /**********************/

    /** TMP VARIABLES**/
    /**
     * Used to store the information regarding the plan with lowest marginal cost
     * selectedPlan and selectedVehicle are used to update the control maps if task was to be assigned
     */
    private static List<PD_Action> selectedPlan;
    private static Vehicle selectedVehicle;

    /**********************/

    Control(Agent agent) {
        this.assignedTasks = new HashMap<>();
        this.plans = new HashMap<>();
        this.selectedPlan = new ArrayList<>();
        this.selectedVehicle = null;
        for (Vehicle vehicle : agent.vehicles()) {
            assignedTasks.put(vehicle, new ArrayList<>());
            plans.put(vehicle, new ArrayList<>());
        }
    }

    public double getLowestMarginalCost(Task task, long timeout_bid) {
        double lowestMarginalCost = Double.MAX_VALUE;
        double vehicleMarginalCost;
        List<PD_Action> newPlan;
        Pair vehicleResults;

        // Compute marginal cost for all vehicles and keep the lowest
        for (Vehicle vehicle : plans.keySet()) {
            // newPlan will be updated with the new computed plan
            vehicleResults = computeMarginalCost(vehicle, task, timeout_bid/plans.size());
            vehicleMarginalCost = (double)vehicleResults.t;
            newPlan = (List<PD_Action>) vehicleResults.u;

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

    public static List<Plan> definitivePlans(List<Vehicle> vehicles, ArrayList<Task> tasks, long timeout_plan) {
        // create a list of integers with the task id to be able to recover the id of the tasks
        List<List<Integer>> tasksIDsList = new ArrayList<>();
        for (List<Task> orginalTasks: assignedTasks.values()) {
            List<Integer> taskIDs = new ArrayList<>();
            for (Task task : orginalTasks) {
                taskIDs.add(task.id);
            }
            tasksIDsList.add(taskIDs);
        }

        List<List<Task>> newTasks = new ArrayList<>();
        for(List<Integer> taskIDofVeh: tasksIDsList) {
            List<Task> listNewTasks = new ArrayList<>();
            for (Task task : tasks) {
                if (taskIDofVeh.contains(task.id) ){
                    listNewTasks.add(task);
                }
            }
            newTasks.add(listNewTasks);
        }

        // update PD_Action struct in plans with the renovated tasks
        for(List<PD_Action> vehPlan : plans.values()) {
            for (PD_Action act : vehPlan) {
                for(Task task : tasks) {
                    if(task.id == act.task.id) {
                        act.task = task;
                    }
                }
            }
        }
        // And transform the HashMap to List<List<PD_Action>>
        List<List<PD_Action>> plansList = new ArrayList<>();
        plansList.addAll(plans.values());

        double cost = 0;
        for (Vehicle vehicle : vehicles) {
            cost += ComputeCost(vehicle, plans.get(vehicle));
        }

        Candidate candidate = new Candidate(vehicles, plansList, newTasks, cost);
        CentralizedTemplate centralizedTemplate = new CentralizedTemplate();
        // set new instance of centralized template with new timeout for planning
        centralizedTemplate.setTimeout_plan(timeout_plan);
        List<List<PD_Action>> newPDPlan = centralizedTemplate.SLS(vehicles,tasks, candidate);

        Candidate definitive = new Candidate(vehicles, newPDPlan, newTasks, cost);

        return centralizedTemplate.PlanFromSolution(definitive);
    }

    public void updateControlVariablesIfTaskWon(Task task) {
        List<Task> newVehicleTasks = assignedTasks.get(selectedVehicle);
        newVehicleTasks.add(task);
        assignedTasks.replace(selectedVehicle, newVehicleTasks);
        plans.replace(selectedVehicle,selectedPlan);
        System.out.println("task " + task.id + " won by " + selectedVehicle.name());
    }

    private Pair computeMarginalCost(Vehicle vehicle, Task task, long timeout_bid) {
        // add the new task to the already assigned tasks for that vehicle
        List<Task> tmpAssignedTasks = new ArrayList<>(assignedTasks.get(vehicle));
        tmpAssignedTasks.add(task);

        Plan newPlan;
        List<PD_Action> newPDPlan;
        // when having less than 6 tasks ASTAR works well
        if (tmpAssignedTasks.size() < 7) {
            State initialState = new State(vehicle, tmpAssignedTasks);
            newPDPlan = aStarPlan(vehicle, tmpAssignedTasks, initialState);
        }
        else { // use centralized agents algortihm
            // create temporal list of just one vehicle
            List<Vehicle> justOneVehicle = new ArrayList<>();
            justOneVehicle.add(vehicle);

            // create a temporal plan with the new task at the end
            List<PD_Action> tmpPlan = new ArrayList<>(plans.get(vehicle));
            tmpPlan.add(0, new PD_Action(true, task));
            tmpPlan.add(new PD_Action(false, task));
            List<List<PD_Action>> justOnePlan = new ArrayList<>();
            justOnePlan.add(tmpPlan);

            // list of the assigned tasks to that vehicle
            List<List<Task>> justOneTaskList = new ArrayList<>();
            justOneTaskList.add(tmpAssignedTasks);

            Candidate candidate = new Candidate(justOneVehicle, justOnePlan,justOneTaskList,ComputeCost(vehicle, plans.get(vehicle)));
            CentralizedTemplate centralizedTemplate = new CentralizedTemplate();
            centralizedTemplate.setTimeout_plan(timeout_bid);
            newPDPlan = centralizedTemplate.SLS(justOneVehicle,tmpAssignedTasks, candidate).get(0);
        }

        // compute marginal cost to deliver the new task compared to the already assigned plan
        double marginalCost = (ComputeCost(vehicle, newPDPlan) - ComputeCost(vehicle, plans.get(vehicle)));
        return new Pair(marginalCost, newPDPlan);
    }
}
