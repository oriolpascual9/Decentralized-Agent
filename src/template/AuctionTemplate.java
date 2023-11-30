package template;

//the list of imports
import java.util.*;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import static algorithms.AStar.aStarPlan;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionTemplate implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	private Control control;
	private QTableV2 level_badness;
	private Integer nr_bids;
	private double avg_min;


	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();
		this.control = new Control(agent);

		this.level_badness = new QTableV2(topology, distribution, 0.85 );
		this.nr_bids = 0;
		this.avg_min = 0;


		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);

		System.out.println("The average is: " + this.level_badness.getAvg_badness() );
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) {
			control.updateControlVariablesIfTaskWon(previous);
		}

		double smallest_bid = Double.POSITIVE_INFINITY;
        for (Long bid : bids)
            smallest_bid = Math.min(smallest_bid, bid);

		this.nr_bids += 1;
		this.avg_min = ((this.nr_bids-1)*this.avg_min +  smallest_bid)/this.nr_bids;
	}
	
	@Override
	public Long askPrice(Task task) {
		double r = 0;

		double R = this.level_badness.getCurr_badness(task) / this.level_badness.getAvg_badness();
		System.out.println("The Ratio is: " + R);

		double marg_cost = control.getLowestMarginalCost(task);

		double bid;
		if(this.avg_min < marg_cost) // lowest we bid is avg_min
			bid = Math.max(this.avg_min, R*marg_cost);
		else // lowest we bid is marg_cost
			bid = Math.max(R*this.avg_min, marg_cost);

		return (long) Math.round(bid);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);

		Plan planVehicle1 = naivePlan(vehicle, tasks);

		List<Plan> plans = new ArrayList<Plan>();
		plans.add(planVehicle1);
		while (plans.size() < vehicles.size())
			plans.add(Plan.EMPTY);

		return plans;
	}

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}
}
