package template;

import logist.agent.Agent;
import logist.task.TaskDistribution;
import logist.topology.Topology.City;
import logist.topology.Topology;

import java.util.ArrayList;
import java.util.List;

public class QTable {
//    Goal of this function is to determine the "Badness" of a package's delivering city.
//      The "Badness" of a delivery city is given by the worst probability of another package not being
//      able to get picked up at the delivery city. We are being Optimistic in this function.
//          - In practice, once this Q-table has been built, we should try to look at the Q-values for all the cities
//          that were visited when delivering the package.
//          - The Q-value that is closest to 0, is the best. Because our table will consist of negative Q-values, the
//          more negative the value, the more "bad" the city is
//      To build this problem as a Q-table State-space we have defined the following:
//          - States:
//              1) Consist of the currently observed City (i.e. the city where we would deliver a package)
//          - Actions:
//              1) Indicates the city the vehicle would move to in-case there is no package available

//      The Q-value is given by the equation:
//          - Q(s,a) = ( p(no-task) )*travel_cost + (discount) * p(no-task) * max_a( Q(s', a) )

//              - p(no-task): Is the probability of there being no task to pickup at the current city
//              - travel_cost: is the travel cost from being at current city (state) and applying an action to move to city a
//              - discount  : It's our discount factor
//              - s'    : Is the next we city we end up in, after applying action a
//              - max_a : Is, for a given action, the maximum Q-value of the next state s'. We are being a bit
//                optimistic and saying "on the chance that I have no task to pick-up here (with a probability
//                of p(no-task)), then I will move to my next city and see how bad things are over there".
//
//              - We add the immediate badness and future badness*discount to sort of say "This state is really bad if
//                I have no package here, and none in the next city" or the city after that or the one after that,
//                and so on"
//                  - I'm not sure if addition or multiplication is the best way. But as long as we are consistent,
//                    it's okay

//    Variables I need are:
//      - Topology
//      - TaskDistribution
//      - Agent
//      - discount
//      - Vehicle id

    // Defining global variables
    private final double avg_badness;
    private final double[] V;

    //////////////////////////////////////////////////////////
    //////////////////// Initialize Q table //////////////////
    QTable(Topology topology1, TaskDistribution td1, Double discount){
        //////////////////////////////////////////////////////////
        /////////////////////// Create State Space ///////////////

        // Size of states and cities is 1 to 1
        int nr_states = topology1.size();

        // Extract the list of cities in this Topology
            // Since a state only corresponds to 1 city we can say the list of cities is the same as the state_space
        ArrayList<City> state_space = new ArrayList<>(topology1.cities());

        // Verify that the cities in states space are order from 0 to nr_cities
        for(int i=0; i<nr_states; i++){
            if(state_space.get(i).id != i)
                System.out.println("ERROR! State Space index is NOT the same as city id (Q-Table");
        }

        /////////////////////// Create State Space ///////////////
        //////////////////////////////////////////////////////////
        ////////////////////// Create Action Space ///////////////
        // Explanation: So Cities have id #, the action # represents the action of moving to that city.id #,

        // Get the # of actions
        int nr_actions = topology1.size();

        ////////////////////// Create Action Space ///////////////
        //////////////////////////////////////////////////////////
        //////////////////// Initialize Q table //////////////////

        // Initialize V vector
        V = new double[nr_states];
        for(int i=0; i<nr_states; i++)
            V[i] = 0;


        // Initialize Q array
        double[][] Q = new double[nr_states][nr_actions];

        // While loop until the condition is satisfied
        int MaxIter = 20000;
        int iter    = 0;
        double max_error = 1e-8;

        while (iter != MaxIter){
            // Assume V_value has not changed
            boolean V_updated = false;

            // Iterate through all the states
            for(int s=0; s<nr_states; s++){

                // Iterate through all the actions
                for(int a=0; a<nr_actions; a++){

                    // If action is to Not move to itself (so if in city 1, you don't move to city 1)
                    if(state_space.get(s).id != a){
                        ////////////////////////////////////////////////////
                        /////// Calculate Q matrix for MOVE actions ////////

                        // Calculate the immediate "badness" level of the city
                            // Action says I move to city a. I am at city s
                            // So immediate badness is the probability that, when i move from s to a, I have no package to deliver
                            // Then we multiply this probability with the distance of traveling from s to a
                        double immediate_badness = -(1 - td1.probability(state_space.get(s), state_space.get(a)) ) *
                                state_space.get(s).distanceTo(state_space.get(a));
                            /// Add one where it's the probability of NO package*distance to action city
                        // Calculate the Q value
                        Q[s][a] = immediate_badness + discount*V[a];
//                                td1.probability(state_space.get(s), null)*(V[a]);

                        /////// Calculate Q matrix for MOVE actions ////////
                        ////////////////////////////////////////////////////
                    } else { // Action moves to itself
                        Q[s][a] = Double.NEGATIVE_INFINITY;
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

        avg_badness = avgBadness(Q, nr_states, nr_actions, state_space);

    }


    //////////////// Helpful functions for afterwards ////////////////
    private double avgBadness(double[][] Q, int nr_states, int nr_actions, ArrayList<City> state_space){
        // Initialize the average badness from the Q-table
        double total_badness = 0;

        // Initialize the normalizing variable
        int count = 0;

        // Iterate over all the states
        for(int s=0; s<nr_states; s++){

            // Iterate over all the actions
            for(int a=0; a<nr_actions; a++){
                if(state_space.get(s).id != a){
                    System.out.print("Q Value is: ");
                    System.out.println(Q[s][a]);
                    System.out.println("------");
                    System.out.println("------");
                    total_badness += Q[s][a];
                    count       += 1;
                }
            }
        }

        // Return the average
        return total_badness/count;
    }

    public double bestCityBadness(List<City> visited_cities){
//        For a list of cities that will get visited (visited_cities), the "best badness" refers to the city with
//        the lowest Q-value (so Q-value closest to 0 / the least negative)

        // Initialize the best badness
        double best_badness = Double.NEGATIVE_INFINITY;

        // Iterate over all the cities what are planned to be visited
        for(City obsv_city : visited_cities)
            best_badness = Math.max(best_badness, V[obsv_city.id]);

        // Return the best badness
        return best_badness;
    }

    //////////////// GET functions ////////////////
    public double[] getV() {
        return V;
    }

    public double getAvg_badness() {
        return avg_badness;
    }
}
