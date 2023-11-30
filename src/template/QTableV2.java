package template;

import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QTableV2 {
    private final double avg_badness;
    private final double[] badness;
    private final int nr_cities;

    QTableV2(Topology topology1, TaskDistribution td1, Double discount){
        //////////////////////////////////////////////////////////
        /////////////////////// Create State Space ///////////////
        // Get the number of states
        int nr_states = topology1.size() * topology1.size();

        // Define state_space as an array of states, where each element is a class of type State
        StateQV2[] state_space = new StateQV2[nr_states];

        // Extract the list of cities
        List<City> city_list = topology1.cities();
        nr_cities = topology1.size();

        // Create States Space
        int k = 0; // State Space index
        for(int i=0; i < topology1.size(); i++) {

            // Gets City i
            City current_city = city_list.get(i);

            for(int j=0; j<topology1.size(); j++){
                // If a different city then Execute if statement
                if(i!=j){

                    // Get City j
                    City dest_city = city_list.get(j);

                    // create new state
                    state_space[k] = new StateQV2(current_city, dest_city);
                    k++;
                }
            }

            // Create a state where there is no package to be delivered
            state_space[k] = new StateQV2(current_city, null);
            k++;
        }

        /////////////////////// Create State Space ///////////////
        //////////////////////////////////////////////////////////
        ////////////////////// Create Action Space ///////////////
        // Explanation: So Cities have id #, the action # represents the action of moving to that city.id #,
        // then an extra number is added at the end, which represent "Deliver" action.

        // Get number of Actions
        int nr_actions = 1 + topology1.size();

        ////////////////////// Create Action Space ///////////////
        //////////////////////////////////////////////////////////
        //////////////////// Initialize V vector /////////////////

        double[] V = new double[nr_states];
        Arrays.fill(V, 0);

        //////////////////// Initialize V vector /////////////////
        //////////////////////////////////////////////////////////
        //////////////////// Initialize Q table //////////////////

        // Initialize Q array
        double[][] Q = new double[nr_states][nr_actions];

        // While loop until the condition is satisfied
        int MaxIter = 20000;
        int iter    = 0;
        double max_error = 1e-8;

        while(iter != MaxIter){
            // Assume V_value has not changed
            boolean V_updated = false;

            // Iterate through all the states
            for(int s=0; s<nr_states; s++){

                // Iterate over all the actions
                for(int a=0; a<nr_actions; a++){
                    // Get current City
                    City current_city = state_space[s].getCurrent_city();

                    // Get Package City
                    City package_city = state_space[s].getDest_city();

                    // If action is NOT send
                    if(a != topology1.size()) {
                        ////////////////////////////////////////////////////
                        //////// Discover if action # is a neighbor ////////

                        // Get the list of neighboring cities
                        List<City> neighbor_cities = current_city.neighbors();

                        // Boolean to see if action (move to city a) is a neighboring city
                        boolean neighbour = false;

                        // Iterate through neighbour list to find if actions is a neighbouring city
                        for (City neighborCity : neighbor_cities) {
                            // If action city is a neighbor, then set boolean to true
                            if (neighborCity.id == a) {
                                neighbour = true;
                                break;
                            }
                        }
                        //////// Discover if action # is a neighbor ////////
                        ////////////////////////////////////////////////////
                        /////// Calculate Q matrix for MOVE actions ////////

                        // If actions moves is to a neighboring city
                        if(neighbour){
                            // Get City parameter of the neighbor
                            Topology.City next_city = city_list.get(a);

                            // Get distance [km] from current to next city
                            double distance = current_city.distanceTo(next_city);

                            // Calculate net reward [R(s,a)]
                            double net_reward = -distance;

                            // Calculate sum{ T(s,a,s_prime) * V(s_prime) }
                            double future_reward = FutureRewards(state_space, s, a, td1, topology1, nr_actions, V);

                            // Calculate Q(s,a)
                            Q[s][a] = net_reward + discount*future_reward;
                        } else{
                            // Since action does not move us to a neighbouring city, then we set Q(s,a) to [-infinity]
                            Q[s][a] = Double.NEGATIVE_INFINITY;
                        }
                        /////// Calculate Q matrix for MOVE actions ////////
                        ////////////////////////////////////////////////////
                    } else{   // If we have a package to be delivered
                        ////////////////////////////////////////////////////
                        /////// Calculate Q matrix for SEND action ////////

                        // If we have a package to be delivered
                        if(package_city != null){
                            // Get distance [km] from current to next city
                            double distance = current_city.distanceTo(package_city);

                            // Calculate net reward [R(s,a)]
                            double net_reward = -distance * (1 - td1.probability(current_city, package_city));

                            // Calculate sum{ T(s,a,s_prim) * V(s_prime) }
                            double future_reward = FutureRewards(state_space, s, a, td1, topology1, nr_actions, V);

                            // Calculate Q(s,a)
                            Q[s][a] = net_reward + discount*future_reward;
                        } else{ // We have no package to deliver
                            Q[s][a] = Double.NEGATIVE_INFINITY;
                        }
                        /////// Calculate Q matrix for SEND action ////////
                        ////////////////////////////////////////////////////
                    }
                }
                /////////////////////////////////////////////////////////////////
                // Calculate closest value Q-value to 0 for current state V(s) //

                // Track old value in V-vector
                double old_V = V[s];

                // Update V
                for(int a=0; a<nr_actions; a++) {

                    // Update V-vector
                    if (a == 0)
                        V[s] = Q[s][a];
                    else
                        V[s] = Math.max(V[s], Q[s][a]);
                }

                // If V has not updated for any of the states before
                if(!V_updated){
                    // If V has updated for this state
                    if(Math.abs(V[s]-old_V) > max_error)
                        V_updated = true;
                }

                // Calculate closest value Q-value to 0 for current state V(s) //
                /////////////////////////////////////////////////////////////////
            }


            // Increase iteration
            iter += 1;

            // If V-vector is no longer updating, the leave while loop
            if(!V_updated)
                break;
        }

        badness = generateBadness(V, nr_states);
        avg_badness = avgBadness(badness, nr_states- topology1.size());


    }

    // Calculate the badness level
    private double[] generateBadness(double[] V, int nr_states){
        // Should I include the no package state
        boolean remove_no_package = true;

        if(remove_no_package) {
            // Initialize the badness vector
            double[] C = new double[nr_states - nr_cities + 1];
            Arrays.fill(C, 0);

            // Iterate over all the sates and collect the V value only for states where a package needs to be delivered
            int sp = 0;
            for (int s = 0; s < nr_states; s++) {
                if (((s + 1) % nr_cities) != 0) {
                    C[sp] = V[s];
                    sp++;
                }

            }

            return C;
        }
        else
            return V;
    }

    // Find the Avg badness
    private double avgBadness(double[] C, int nr_states){
        // Initialize the average badness from the Q-table
        double total_badness = 0;

        // Initialize the normalizing variable
        int count = 0;

        // Iterate over all the states
        for(int s=0; s<nr_states; s++){

            total_badness += C[s];
            count         += 1;

        }

        // Return the average
        return total_badness/count;
    }

    // Find the Current state, given current and next city
    private int FindCurrState(City curr_city, City pack_city, int nr_actions, StateQV2[] state_space){
        // In case there is an error
        if(curr_city == null){
            System.out.println("ERROR! in FindCurrState NULL");
            return 0;
        }
        // Initialize current_state index
        int curr_state = 0;

        // determine the number of cities
        int nr_cities = nr_actions-1;

        // find the state number for current city
        int curr_city_state = (curr_city.id)*nr_cities;

        if(pack_city == null){
            curr_state = curr_city_state + nr_cities - 1;
        }else {
            // Iterate over the states related to the current city
            // (nr_cities - 2) because we don't have a package delivered to the same city, and we also
            // don't consider the case when there is no package (the previous if statement takes care of that)
            for (int s = curr_city_state; s < (curr_city_state + nr_cities - 2); s++) {
                // get destination of state
                City dest_city = state_space[s].getDest_city();

                // Determine if current state has the correct delivery city
                if (dest_city.id == pack_city.id) {
                    curr_state = s;
                    break;
                }
            }
        }

        return curr_state;
    }

    // Function below is used to calculate  sum{ T(s,a,s_prime) * V(s_prime) }
    private double FutureRewards(StateQV2[] state_array, int curr_s, int curr_a, TaskDistribution td2,
                                 Topology topology2, int nr_actions, double[] V){
        ////////////////////////////////////////////////////
        ///////// Find Next city | Given action a //////////

        // Extract the list of cities
        List<City> city_list = topology2.cities();

        // Initialize the next_city
        City move_to_city;

        // If current action NOT "Deliver package"
        if(curr_a != topology2.size()){
            // Get destination city // In this if statement the value of a is equivalent to the city id # I will move to
            move_to_city = city_list.get(curr_a);

        }else{ // Current action is "Deliver package"
            // We move to where the package takes us
            move_to_city = state_array[curr_s].getDest_city();
        }

        ///////// Find Next city | Given action a //////////
        ////////////////////////////////////////////////////
        /////// sum{ T(s,a,s_prime) * V(s_prime) } /////////

        // Initialize future_reward as 0
        double future_reward = 0;

        // Next_delivery_id = the id # of where I would have to send my next package
        for(int next_delivery_id=0; next_delivery_id < topology2.size(); next_delivery_id++) {

            // Make sure I'm not calculating for when I deliver to myself (can't happen)
            if(curr_a != next_delivery_id) {

                // Obtain T(s,a,s_prime)
                double probability = td2.probability(move_to_city, city_list.get(next_delivery_id));

                // Find the next state s_prime
                int next_state = Find_next_state(state_array, curr_s, curr_a, topology2, next_delivery_id, nr_actions);

                // Calculate future reward
                future_reward += probability*V[next_state];
            }
        }
        /////// sum{ T(s,a,s_prime) * V(s_prime) } /////////
        ////////////////////////////////////////////////////

        return future_reward;
    }
    // Find the s_prime
    private int Find_next_state(StateQV2[] state_array2, int curr_s2, int curr_a2, Topology topology3,
                                int next_delivery_id2, int nr_actions){
        // Get the list of all cities in topology
        List<City> city_list = topology3.cities();

        City new_curr_city;
        // If current action NOT "Deliver package"
        if(curr_a2 != topology3.size()){
            // New current city id # is the city we "Move" to, which would be equivalent to the action #
            new_curr_city = city_list.get(curr_a2);
        }else{ // Current action is "Deliver package"
            // New current city id # is where we deliver our package
            new_curr_city = state_array2[curr_s2].getDest_city();
        }
        if(new_curr_city ==  null){
            System.out.println("ERROR! in Find_next_state");
        }
        // Initialize the new state s_prime
        int s_prime;
        s_prime = FindCurrState(new_curr_city, city_list.get(next_delivery_id2), nr_actions, state_array2);

        return s_prime;
//
//		// Iterate over all states
//		for (int s = 0; s < num_state; s++) {
//
//			// Get the current city of the State
//			City state_curr_city = state_array2[s].getCurrent_city();
//
//			// If current city is NOT where our action took us, then increase s by a larger value
//			if (state_curr_city.id != new_curr_city_id) {
//
//				// This is simply done to increase the speed of the code
//				s += topology3.size() - 1; // -1 because the for loop will increase it by 1 at the end
//			} else {
//				// Get the destination city
//				City dest_city = state_array2[s].getCurrent_city();
//
//				// make sure there is a package to deliver
//				if(dest_city != null) {
//					// State has the correct current cty and destination city
//					if (dest_city.id == next_delivery_id2) {
//						// new state s_prime has been found
//						s_prime = s;
//						break;
//					}
//				}
//			}
//		}

    }

    ////////////////////////////

    public double getAvg_badness() {
        return avg_badness;
    }
    public double getCurr_badness(Task task){
        // Get pick-up and destination city
        City pickup_city = task.pickupCity;
        City deliver_city = task.deliveryCity;

        // Find the state # for the current badness
        int state_nr = (pickup_city.id)*(nr_cities - 1) + (deliver_city.id - 1);

        return this.badness[state_nr];
    }
}
