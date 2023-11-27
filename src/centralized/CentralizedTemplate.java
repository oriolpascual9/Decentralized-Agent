package centralized;

//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;

import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.io.File;



@SuppressWarnings("unused")
public class CentralizedTemplate {
    private long timeout_setup;
    private long timeout_plan;
    private double p; // probability of returning old solution for SLS algorithm
    private Random random;

    // Setup function
    private void setClassVariables() {
        // this code is used to get the timeouts
        this.random = new Random(); // create random seed
        this.p = 0.2; // set p

        // 30 s = 30.000 millseconds
        timeout_plan = 10000;	// We add a little safety margin
    }

    // Solve the optimization problem with the SLS algorithm
    public List<List<PD_Action>> SLS(List<Vehicle> vehicles, List<Task> task_list, Candidate A) {
        setClassVariables();
        System.out.println("Building plan...");

        long time_start = System.currentTimeMillis();

        // Begin SLS Algorithm


        // create initial solution
        if (A == null)
            A = Candidate.SelectInitialSolution(random, vehicles, task_list);


        // Optimization loop - repeat until timeout
        boolean timeout_reached = false;

        while (!timeout_reached) {
            // record old solution
            Candidate A_old = A;

            // generate neighbours
            List<Candidate> N = A_old.ChooseNeighbours(random);

            // Get the solution for the next iteration
            A = LocalChoice(N, A_old);

            // Check timeout condition
            if (System.currentTimeMillis() - time_start > timeout_plan) {
                timeout_reached = true;
            }
        }

        // End SLS Algorithm

        // Informative outputs
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        double cost_plan = A.cost;


        System.out.println("The plan was generated in " + duration + " ms with a cost of " + A.cost);

        return A.plans;
    }

    List<Plan> PDPlantoPlan(Candidate A){
        // Build plans for vehicles from the found solution
        List<Plan> plan = PlanFromSolution(A);

        for (Plan onePlan : plan) {
            System.out.println(plan);
        }
        return plan;
    }


    // Local choice to choose the next solution from the neighbours and the current solution
    public Candidate LocalChoice(List<Candidate> N, Candidate A) {


        if (random.nextFloat() < p) {	// Return A with probability p

            return A;

        }
        else {	// Return the best neightbour with probability 1-p

            int best_cost_index = 0; // index of the neighbour with best cost until now
            double best_cost = N.get(best_cost_index).cost; // cost of the neighbour with best cost until now


            for (int n_ind = 1; n_ind < N.size(); n_ind++ ) {

                // check if current alternative has lower cost than the current best
                if( N.get(n_ind).cost < best_cost )	{
                    // if so, update the best solution
                    best_cost_index = n_ind;
                    best_cost = N.get(best_cost_index).cost;
                }

            }

            // return the best solution
            return N.get(best_cost_index);
        }
    }





    // Build the plan for logist platform from the candidate solution
    public List<Plan> PlanFromSolution(Candidate A) {

        // System.out.println("Constructing plan from solution...");

        List<Plan> plan_list = new ArrayList<>();	// create empty list of plans

        // Build plan for each vehicle
        for (int vehicle_ind = 0; vehicle_ind < A.vehicles.size(); vehicle_ind++) {

            Vehicle v = A.vehicles.get(vehicle_ind);

            // get constructed plan of the vehicle
            List<PD_Action> plan = A.plans.get(vehicle_ind);

            // follow vehicle cities to construct plan
            City current_city = v.getCurrentCity();
            Plan v_plan = new Plan(current_city);

            // Append required primitive actions for each pickup/delivery action
            for (PD_Action act : plan) {

                City next_city;
                if(act.is_pickup) {
                    next_city = act.task.pickupCity;
                }
                else {
                    next_city = act.task.deliveryCity;
                }



                // Append move actions
                for(City move_city : current_city.pathTo(next_city)) {
                    v_plan.appendMove(move_city);
                }
                // Append pickup-delivery actions
                if (act.is_pickup) {
                    v_plan.appendPickup(act.task);
                } else {
                    v_plan.appendDelivery(act.task);
                }
                current_city = next_city;
            }

            // add plan to the list of plans
            plan_list.add(v_plan);
        }
        return plan_list;
    }



}
