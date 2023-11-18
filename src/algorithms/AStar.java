package algorithms;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import template.State;

public class AStar {
    // We define a State comparator to find the state with the minimum cost
    public static class StateComparator implements Comparator<State> {
        @Override
        public int compare(State s1, State s2) {
            return (int) (s1.getCostFunctionValue() - s2.getCostFunctionValue());
        }
    }

    public static Plan aStarPlan(Vehicle vehicle, List<Task> available, State initialState) {
        PriorityQueue<State> Q = new PriorityQueue<State>(new StateComparator()); // Efficient data-structure to get cities that have the lowest score
        HashMap<State, Double> history = new HashMap<State, Double>(); // To store the cost for each node. If we come back to the same node, we will check this to see if we found a better path.

        Q.add(initialState);

        while (!Q.isEmpty()) {
            State node = Q.remove(); // Pop the state with the least cost

            // Check whether the state is a final one. In that case we return the plan
            if (node.isFinal()) {
                return new Plan(vehicle.getCurrentCity(), node.getActionsToReach());
            }

            // We add the note and its children if 1) this is the first time it is visited or 2) the cost has decreased
            if (!history.containsKey(node) || (node.getCostToReach() < history.getOrDefault(node, Double.MAX_VALUE))) {
                history.put(node, node.getCostToReach());
                Q.addAll(node.generateChildren());
            }
        }

        return null; // We should never reach this step
    }

}