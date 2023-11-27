package template;

import centralized.PD_Action;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

import java.util.*;

import logist.plan.Action;

// Representation of the state. Attributes are pretty clear.
public class State implements Comparable<State>{
	private final City currentCity;
	private final List<Task> carriedTasks;
	private final List<Task> availableTasks;
	private LinkedList<Action> actionsToReach;
	private List<PD_Action> PDPlan;
	private double costToReach;
	private int currentVehicleCapacity;

	public State (City currentCity, List<Task> carriedTasks, List<Task> availableTasks, int currentVehicleCapacity) {
		this.currentCity = currentCity;
		this.carriedTasks = carriedTasks;
		this.availableTasks = availableTasks;
		this.currentVehicleCapacity = currentVehicleCapacity;
		this.actionsToReach = new LinkedList<Action>();
		this.PDPlan = new ArrayList<>();
		this.costToReach = 0;
	}
	
	public State(Vehicle vehicle, List<Task> available) {
		this (vehicle, available, new ArrayList<>());
	}
	
	public State(Vehicle vehicle, List<Task> available, List<Task> carriedTasks) {
		this (vehicle.getCurrentCity(), carriedTasks, available, vehicle.capacity());
	}
	
	public boolean isFinal() {
		return this.carriedTasks.isEmpty() && this.availableTasks.isEmpty();
	}

	public LinkedList<State> generateChildren(){
		LinkedList<State> children = this.generateDeliveryChildren();
		children.addAll(this.generatePickUpChildren());
		return children;
	}
	
	public LinkedList<State> generateDeliveryChildren(){
		LinkedList<State> children = new LinkedList<State>();
		
		// Go through every task
		for (Task task: this.getCarriedTasks()) {
			// We do a deep copy
			List<Task> childCarriedTasks = new ArrayList<>(this.getCarriedTasks());
			childCarriedTasks.remove(task);
			List<Task> childAvailableTasks = new ArrayList<>(this.getAvailableTasks());
			
			// Create the state
			State child = new State (
					task.deliveryCity,
					childCarriedTasks,
					childAvailableTasks,
					this.getCurrentVehicleCapacity() + task.weight);
			
			// We add the action move to every cities in the path
			LinkedList<Action> branchActions = new LinkedList<Action>(this.getActionsToReach());
			for (City city: currentCity.pathTo(task.deliveryCity)) {
				branchActions.add(new Action.Move(city));
			}
			// And finally, the delivery action
			branchActions.add(new Action.Delivery(task));
			child.setActionsToReach(branchActions);

			List<PD_Action> branchPDActions = new ArrayList<>(this.getPDPlan());
			branchPDActions.add(new PD_Action(false,task));
			child.setPDPlan(branchPDActions);

			// Compute the distance
			double branchCost = currentCity.distanceTo(task.deliveryCity);
			child.setCostToReach(this.getCostToReach()+branchCost);
			
			children.add(child);	
		}
						
		return children;
	}
	
	// Similar to before but this time the PickUp node
	public LinkedList<State> generatePickUpChildren(){
		LinkedList<State> children = new LinkedList<State>();
				
		for (Task task: this.getAvailableTasks()) {
			if (this.getCurrentVehicleCapacity() >= task.weight) {
				List<Task> childCarriedTasks = new ArrayList<>(this.getCarriedTasks());
				childCarriedTasks.add(task);
				List<Task> childAvailableTasks = new ArrayList<>(this.getAvailableTasks());
				childAvailableTasks.remove(task);
				
				State child = new State (
						task.pickupCity,
						childCarriedTasks,
						childAvailableTasks,
						this.getCurrentVehicleCapacity() - task.weight);
				
				LinkedList<Action> branchActions = new LinkedList<Action>(this.getActionsToReach());
				for(City city : currentCity.pathTo(task.pickupCity)) {
					branchActions.add(new Action.Move(city));
				}
				branchActions.add(new Action.Pickup(task));
				child.setActionsToReach(branchActions);

				List<PD_Action> branchPDActions = new ArrayList<>(this.getPDPlan());
				branchPDActions.add(new PD_Action(true,task));
				child.setPDPlan(branchPDActions);
				
				double branchCost = currentCity.distanceTo(task.pickupCity);
				child.setCostToReach(this.getCostToReach()+branchCost);
				
				children.add(child);
			}
		}
		
		return children;
	}
	
	public double getCostFunctionValue () {
		return this.getCostToReach() + this.getHeuristicValue();
	}
	
	// Compute the heuristic value
	public double getHeuristicValue () {
		double minCostBetweenCities = 0;
		Set<City> citiesVisited = new HashSet<City>();
		Set<City> citiesToVisit = new HashSet<City>();
		
		// Get all the cities that we will visit
		for (Task task: this.getCarriedTasks())
			citiesToVisit.add(task.deliveryCity);
		for (Task task: this.getAvailableTasks()) {
			citiesToVisit.add(task.pickupCity);
			citiesToVisit.add(task.deliveryCity);
		}
		
		// We remove our current city
		citiesToVisit.remove(this.getCurrentCity());
		citiesVisited.add(this.getCurrentCity());
		
		// Go through all cities to compute the heuristic
		while (!citiesToVisit.isEmpty()) {
			double minDistance = Double.MAX_VALUE;
			City closestCity = null;

			for (City cityVisited: citiesVisited) {
				for (City cityToVisit: citiesToVisit) {
					double distance = cityVisited.distanceTo(cityToVisit);
					if (distance < minDistance) {
						minDistance = distance;
						closestCity = cityToVisit;
					}
				}
			}

			citiesToVisit.remove(closestCity);
			citiesVisited.add(closestCity);
			minCostBetweenCities += minDistance;
		}
		
		return minCostBetweenCities;
	}
	
	@Override
	public int compareTo (State st) {
		if (this.getCostToReach() == st.getCostToReach()) return 0;
		else if (this.getCostToReach() > st.getCostToReach()) return 1;
		else return -1;
	}
	
	public City getCurrentCity() {
		return this.currentCity;
	}


	public List<Task> getCarriedTasks() {
		return this.carriedTasks;
	}


	public List<Task> getAvailableTasks() {
		return this.availableTasks;
	}

	
	public LinkedList<Action> getActionsToReach() {
		return this.actionsToReach;
	}

	public List<PD_Action> getPDPlan() { return PDPlan;}

	public void setActionsToReach(LinkedList<Action> actionsToReach) {
		this.actionsToReach = actionsToReach;
	}

	public void setPDPlan(List<PD_Action> PDPlan) { this.PDPlan = PDPlan;}

	public double getCostToReach() {
		return this.costToReach;
	}
	
	public void setCostToReach(double costToReach) {
		this.costToReach = costToReach;
	}
	
	public int getCurrentVehicleCapacity() {
		return this.currentVehicleCapacity;
	}
	
	public void setCurrentVehicleCapacity(int vehicleCapacity) {
		this.currentVehicleCapacity = vehicleCapacity;
	}
	
	@Override
	public String toString() {
	    return ("currencity=" + getCurrentCity().name + ",Carrying taks=" + getCarriedTasks() +", available tasks=" + getAvailableTasks() + ",cost=" + getCostToReach() + ",actions=" + getActionsToReach() );
	}
	
	@Override
	// Important to define an = operator between two states
	public boolean equals(Object o) {
		if (this == o)
            return true;
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        
		State s = (State) o;
		if (this.currentCity != s.currentCity)
			return false;

		for (Task t: this.availableTasks) {
			if (!(s.availableTasks.contains(t))) {
				return false;
			}
		}
		for (Task t: s.availableTasks) {
			if (!(this.availableTasks.contains(t))) {
				return false;
			}
		}
		
		for (Task t: this.carriedTasks) {
			if (!(s.carriedTasks.contains(t))) {
				return false;
			}
		}
		for (Task t: s.carriedTasks) {
			if (!(this.carriedTasks.contains(t))) {
				return false;
			}
		}
		return true;
	}
	
	// Same as above
	@Override
	public int hashCode() {
		return Objects.hash(currentCity, availableTasks, carriedTasks);
	}
}
