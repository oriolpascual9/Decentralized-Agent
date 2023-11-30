package centralized;
import java.util.*;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;


//The Candidate class holds a conceptual candidate solution and methods associated with it.

public class Candidate {

    public final Double cost;	// cost of the plan
    public final List<Vehicle> vehicles;	// list of vehicles
    public final List<List<PD_Action>> plans;	// lists of plans for each vehicle
    public final List<List<Task>> taskLists;	// lists of tasks for each vehicle



    public Candidate(List<Vehicle> vehicles, List<List<PD_Action>> plans, List<List<Task>> taskLists, Double cost) {
        this.vehicles = vehicles;
        this.plans = plans;
        this.taskLists = taskLists;
        this.cost = cost;
    }

    // MAIN OPERATIONS: Choose neighbours, select initial solution

    // Function that generates neighbours
    public List<Candidate> ChooseNeighbours(Random random) {

        // Outputs for monitoring
        // System.out.println("Generating neighbours...");



        // 1 - GENERATE NEIGHBOURS BY CHANGING VEHICLES OF TASKS

        // System.out.println("Generating neighbours by changing vehicles of task");

        List<Candidate> neighs = new ArrayList<>();	// List to hold generated neighbours


        int num_vehicles = vehicles.size();

        if (num_vehicles > 1) {
            // Loop over all source vehicle ids
            for (int vid_i = 0; vid_i < num_vehicles; vid_i++) {

                List<Task> vehicle_tasks = taskLists.get(vid_i);    // Get tasks of the vehicle

                // Pass if the vehicle is empty
                if (vehicle_tasks.size() == 0) {
                    continue;
                }


                // Get the first task of the vehicle
                int task_id = 0;
                double task_weight = vehicle_tasks.get(task_id).weight;    // Get task weight

                // Randomly choose another suitable vehicle
                int vid_j = random.nextInt(num_vehicles);
                // Loop until finding a suitable vehicle
                while (vid_i == vid_j || vehicles.get(vid_j).capacity() < task_weight) {
                    vid_j = random.nextInt(num_vehicles);
                }


                // Add a change of t from vehicle i to vehicle j
                neighs.add(ChangingVehicle(random, task_id, vid_i, vid_j));


                // A potential improvement would be to make this change for each task of the vehicle, not only for the first
                // one (i.e. loop over all task indices instead of taking the first one) and also to give to each possible
                // other vehicle, resulting in having tried all combinations.


            }
        }


        // 2 - GENERATE NEIGHBOURS BY CHANGING TASK ORDERS

        // System.out.println("Generating neighbours by changing task orders...");

        // Loop over all source vehicle ids
        for (int vid_i = 0; vid_i < num_vehicles; vid_i++) {

            List<Task> vehicle_tasks = taskLists.get(vid_i);	// Get tasks of the vehicle

            // Pass if the vehicle has less than two tasks
            if (vehicle_tasks.size()<2) {
                continue;
            }


            // Get a task from the vehicle randomly
            int task_id;
            if (num_vehicles > 1)
                task_id = random.nextInt(vehicle_tasks.size());
            // last task will be the inserted one
            else
                task_id = vehicle_tasks.size() - 1;

            // Change the position of pickup and delivery actions of the task and add to neighbours
            for(int i = 0; i < 10; i++) {
                neighs.add(ChangingTaskOrder(random, task_id, vid_i));
            }

            // A potential improvement would be to make this change for each task of the vehicle, not only a random
            // one (i.e. loop over all task indices instead of taking a random task) and also to generate each potential
            // combination.
        }


        return neighs;
    }


    //Create initial candidate solution: All tasks assigned to the largest vehicle
    public static Candidate SelectInitialSolution(Random random, List<Vehicle> vehicles, List<Task> tasks) {

        int num_vehicles = vehicles.size();

// Initialise plans and tasks variables
        List<List<PD_Action>> plans = new ArrayList<>();
        List<List<Task>> taskLists = new ArrayList<>();
        List<Task> allTasks = new ArrayList<>(tasks);


// initialize plans and task list
        for (int i = 0; i < num_vehicles; i++) {
            plans.add(new ArrayList<>());
            taskLists.add(new ArrayList<>());
        }


// Get the vehicle with the largest capacity
        double vehicle_capacities[];
        vehicle_capacities = new double[num_vehicles];
        int largest_vehicle = MaxIndex(vehicle_capacities);


// Assign all the tasks to the largest vehicle
        for (Task t : allTasks) {


            List<PD_Action> plan = plans.get(largest_vehicle);
            List<Task> tasks_vehicle = taskLists.get(largest_vehicle);

            // Outputs for monitoring
            //System.out.println("taskLists:");
            //System.out.println(taskLists);
            //System.out.println("plans:");
            //System.out.println(plans);
            //System.out.println("Current task:");
            //System.out.println(t);

            // Add tasks to the end of current plan
            plan.add(new PD_Action(true, t));
            plan.add(new PD_Action(false, t));


            tasks_vehicle.add(t);
        }

// calculate the cost of initial candidate solution
        double initial_cost = 0.0;
        // accumulate the cost borne by each vehicle
        for (int i = 0; i < vehicles.size(); i++) {
            initial_cost += ComputeCost(vehicles.get(i), plans.get(i));
        }

        Candidate Initial_Solution = new Candidate(vehicles, plans, taskLists, initial_cost);

// Return the generated initial candidate solution
        return Initial_Solution;
    }







// HELPER FUNCTIONS



    //helper function for calculating the maximum of an array
//Method to find the index of maximum element in an array
    public static int MaxIndex( double[] array )
    {
        int max_ind = 0;
        for ( int index = 0; index < array.length; index++ ) {
            if ( array[index] > array[max_ind] ) {
                max_ind = index;
            }
        }
        return max_ind; // position of the first largest found
    }

    //Function to check weight constraint for a single plan
    public static boolean SatisfiesWeightConstraints(List<PD_Action> plan, int vehicle_capacity) {

        // loop over all actions in the plan following the capacity of the vehicle at each point
        for (PD_Action act : plan) {

            if(act.is_pickup) {
                vehicle_capacity = vehicle_capacity - act.task.weight;
            }
            else {
                vehicle_capacity = vehicle_capacity + act.task.weight;
            }

            // constraint not satisfied if capacity goes negative at some point
            if (vehicle_capacity < 0) {
                return false;
            }

        }

        // constraint satisfied if no errors
        return true;

    }


    // Function to compute the cost of individual vehicles
    public static double ComputeCost(Vehicle v, List<PD_Action> plan) {

        double cost = 0.0;

        // Follow the cities on the list of actions
        City current_city = v.getCurrentCity();

        for (PD_Action act : plan) {

            // add the cost to travel to the city
            if(act.is_pickup) {
                cost = cost + current_city.distanceTo(act.task.pickupCity) * v.costPerKm();
                current_city = act.task.pickupCity;
            }
            else {
                cost = cost + current_city.distanceTo(act.task.deliveryCity) * v.costPerKm();
                current_city = act.task.deliveryCity;
            }
        }

        return cost;
    }






// VEHICLE AND TASK ORDER CHANGE OPERATORS

    //Function to change the vehicle of a given task
    public Candidate ChangingVehicle(Random random, int task_id, int vid_i, int vid_j) {

        // Get source (i) and target (j) vehicles
        Vehicle v_i = vehicles.get(vid_i);
        Vehicle v_j = vehicles.get(vid_j);



// 1 - Update task lists

// Compute old task lists for vehicles
        List<Task> i_tasks_old = taskLists.get(vid_i);
        List<Task> j_tasks_old = taskLists.get(vid_j);

// Get the task to change
        Task t = i_tasks_old.get(task_id);

// Create new task list for i by removing the task to change
        List<Task> i_tasks_new = new ArrayList<>(i_tasks_old);
        i_tasks_new.remove(task_id);	// remove the task

// Create new task list for j by adding the task to change
        List<Task> j_tasks_new = new ArrayList<>(j_tasks_old);
        j_tasks_new.add(t);	// insert the task to task list


// Update the task lists
        List<List<Task>> updated_taskLists = new ArrayList<>(taskLists);
        updated_taskLists.set(vid_i, i_tasks_new);
        updated_taskLists.set(vid_j, j_tasks_new);





// 2 - Update plans

// Compute old plans for vehicles
        List<PD_Action> i_plan_old = plans.get(vid_i);
        List<PD_Action> j_plan_old = plans.get(vid_j);


// Create a new plan for i by removing the task to change
        List<PD_Action> i_plan_new = new ArrayList<>(i_plan_old);
// remove actions associated with the task
// Loop over all actions in the plan
        for (int act_ind = 0; act_ind < i_plan_new.size(); ) {

            PD_Action act = i_plan_new.get(act_ind);
            // remove pickup/delivery if it is associated with task t
            if (act.task == t) {
                i_plan_new.remove(act_ind);
            }
            else {	// care not to update the index if a task is removed
                act_ind++;

            }
        }

// Create a new plan for j by adding the task to change
        List<PD_Action> j_plan_new = new ArrayList<>(j_plan_old);

// insert pickup/delivery actions associated with the task to the beginning of the plan
        j_plan_new.add(0, new PD_Action(false,t));
        j_plan_new.add(0, new PD_Action(true, t));
// note that we don't need to check the weight since the vehicle is free initially (assuming capacity is sufficient)

// Insert the new plans of source and target vehicles to the plans of the generated candidate
        List<List<PD_Action>> updated_plans = new ArrayList<>(plans);
        updated_plans.set(vid_i, i_plan_new);
        updated_plans.set(vid_j, j_plan_new);





// 3 - Update costs


// Compute old costs for both vehicles
        double i_cost_old = ComputeCost(v_i, i_plan_old);
        double j_cost_old = ComputeCost(v_j, j_plan_old);


// Compute cost of the new solution
        double i_cost_new = ComputeCost(v_i, i_plan_new);
        double j_cost_new = ComputeCost(v_j, j_plan_new);
        double updated_cost = this.cost - i_cost_old + i_cost_new - j_cost_old + j_cost_new;	// subtract the old costs and add new costs



// 4 - Return the generated candidate solution
        return new Candidate(vehicles, updated_plans, updated_taskLists, updated_cost);


    }



    //Randomly change the place of pickup and delivery actions of one of the tasks in a given vehicle, considering the constraints
    public Candidate ChangingTaskOrder(Random random, int task_id, int vid_i) {



        Vehicle v_i = vehicles.get(vid_i);	// retrieve vehicle

        List<Task> vehicle_tasks = taskLists.get(vid_i);	// retrieve task list of vehicles

        Task t = vehicle_tasks.get(task_id);	// retrieve task whose order is to be changed


// 1 - Update Plans


        List<PD_Action> i_plan_old = plans.get(vid_i);	// retrieve old plan of the vehicle


        List<PD_Action> i_plan_new = new ArrayList<>(i_plan_old);	// create template for new plan

        // remove pickup/delivery actions associated with the task for new plan
        // Loop over all actions in the plan
        for (int act_ind = 0; act_ind < i_plan_new.size(); ) {
            PD_Action act = i_plan_new.get(act_ind);

            // remove action if it is associated with task t
            if (act.task == t) {
                i_plan_new.remove(act_ind);
            }
            else {	// care not to update the index if a task is removed
                act_ind++;
            }
        }

        // insert the pickup action to a suitable place
        int vehicle_capacity = v_i.capacity();
        int pickup_location = 0;
        List<PD_Action> candidate_plan_pickup = new ArrayList<>(i_plan_new);

        // pick a random pickup location
        boolean done = false;
        while (done==false) {

            pickup_location = random.nextInt(i_plan_new.size());

            // add pickup action to candidate plan
            candidate_plan_pickup = new ArrayList<>(i_plan_new);
            candidate_plan_pickup.add(pickup_location, new PD_Action(true, t));

            // check if candidate plan satisfies weight condition
            if(SatisfiesWeightConstraints(candidate_plan_pickup, vehicle_capacity)) {
                done = true;	// found pickup location if satisfies
            }
        }

        // insert the delivery action to a suitable place
        List<PD_Action> candidate_plan_delivery = new ArrayList<>(candidate_plan_pickup);
        // pick a random delivery location
        done = false;
        while (done==false) {

            // do not allow placing before pickup
            int delivery_location_offset = random.nextInt(i_plan_new.size()-pickup_location);
            int delivery_location = pickup_location + 1 + delivery_location_offset;

            // add delivery action to candidate plan
            candidate_plan_delivery = new ArrayList<>(candidate_plan_pickup);
            candidate_plan_delivery.add(delivery_location, new PD_Action(false, t));

            // check if candidate plan satisfies weight condition
            if(SatisfiesWeightConstraints(candidate_plan_delivery, vehicle_capacity)) {
                done = true;	// found delivery location if satisfies
            }
        }

        // Set the new plan to the plan after including the delivery action
        i_plan_new = new ArrayList<>(candidate_plan_delivery);

        // update plans lists
        List<List<PD_Action>> updated_plans = new ArrayList<>(plans);
        updated_plans.set(vid_i, i_plan_new);




// 2 - Update costs



// Compute original values
        double i_cost_old = ComputeCost(v_i, i_plan_old);


// Compute new cost and plans
        double i_cost_new = ComputeCost(v_i, i_plan_new);
        double updated_cost = this.cost - i_cost_old + i_cost_new;

        return new Candidate(vehicles, updated_plans, taskLists, updated_cost);


    }















}
