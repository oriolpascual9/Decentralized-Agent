package template;

//the list of imports
import java.io.File;
import java.util.*;

import logist.LogistPlatform;
import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
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
	private QTable badness;

	private long timeout_setup;
	private long timeout_bid;
	private long timeout_plan;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();
		this.control = new Control(agent);
		this.badness = new QTable(topology, distribution, 0.85 );

		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
		} catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		long timeout_margin = 200;

		// the setup method cannot last more than timeout_setup milliseconds
		timeout_setup = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.SETUP) - timeout_margin;
		// the bid method cannot last more than timeout_bid milliseconds
		timeout_bid = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.BID) - timeout_margin;
		// the plan method cannot last more than timeout_plan milliseconds
		timeout_plan = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.PLAN) - timeout_margin;
		System.out.println("Agent " + agent.id() + ": timeout_setup: " + timeout_setup + ", timeout_bid: " + timeout_bid + ", timeout_plan: " + timeout_plan);

		System.out.println("The average is: " + this.badness.getAvg_badness() );
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) {
			control.updateControlVariablesIfTaskWon(previous);
		}
	}
	
	@Override
	public Long askPrice(Task task) {

		double r;

		City pickup = task.pickupCity;
		City deliver = task.deliveryCity;
		List<City> path = pickup.pathTo(deliver);
		System.out.println(pickup.id == path.get(0).id);

		 r = this.badness.bestCityBadness(path) / this.badness.getAvg_badness() ;

		System.out.println("The Ratio is: " + r);

		double ratio = r;
		double bid = ratio * control.getLowestMarginalCost(task);

		return (long) Math.round(bid);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		ArrayList<Task> taskArrayList = new ArrayList<>(tasks);
		return Control.definitivePlans(vehicles, taskArrayList, timeout_plan);
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
