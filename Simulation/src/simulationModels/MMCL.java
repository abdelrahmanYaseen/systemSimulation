package simulationModels;

import java.util.ArrayList;

import components.Job;
import components.Server;
import queues_analytical.M_M_c_L;
import randomGens.ExponentialGenerator;

public class MMCL extends Simulation {

	private int maxLength;
	

	public MMCL(int numberOfServers, int queueLength) {
		super(numberOfServers);

		// initialize the servers
		for (int i = 0; i < numberOfServers; i++) {
			servers.add(new Server());
		}
		// change the queue length
		this.maxLength = queueLength;
		

	}

	public void showLogs() {
		System.out.println("Showing the results : " + servedJobs.size() + "\n");
		for (int i = 0; i < servedJobs.size(); ++i) {
			System.out.println("Job ID : " + Integer.toString(servedJobs.get(i).getId()) + ",The waiting time is : "
					+ Double.toString(servedJobs.get(i).getTimeInQueue()) + " arrival: "
					+ servedJobs.get(i).getArrivalTime() + " service start, end: "
					+ servedJobs.get(i).getServiceStartTime() + ", " + servedJobs.get(i).getServiceEndTime());
		}

		System.out.println("List of dropped jobs :" + droppedJobs.size() + "\n");
		for (int i = 0; i < droppedJobs.size(); ++i) {
			System.out.println("Job ID : " + Integer.toString(droppedJobs.get(i).getId()) + ",The waiting time is : "
					+ Double.toString(droppedJobs.get(i).getTimeInQueue()) + " arrival: "
					+ droppedJobs.get(i).getArrivalTime() + " service start, end: "
					+ droppedJobs.get(i).getServiceStartTime() + ", " + droppedJobs.get(i).getServiceEndTime());
		}
	}

	// OLD : public void startSimulation(double meanInterArrivalTime, double meanServiceTime, int numberOfJobs) {
	//NEW
	public void startSimulation(double meanInterArrivalTime, double meanServiceTime, int numberOfJobs, int c, int maxLength) {
	//
		
		//NEW
		double lambda = 1/(double)meanInterArrivalTime;
		double mu =1/(double)meanServiceTime;
		this.TMQL=M_M_c_L.Calc_E_n(lambda,mu,c,maxLength);
		//
		
		reset();
		ExponentialGenerator interArrivalTimeGenerator = new ExponentialGenerator(meanInterArrivalTime);
		ExponentialGenerator sericeTimeGenerator = new ExponentialGenerator(meanServiceTime);
		this.clock = 0;
		double previousClock = 0;
		int nextServerID;
		int[] serverStatus; // holds index of first empty server and the number of empty servers

		int jobCount = 0;

		// System.out.println("Start Simulation Function !!!");
		
		Job nextJob = new Job(0.0, sericeTimeGenerator.generate());
		double nextJobArrivalTime = 0;
		jobCount++;

		
		//NEW
		while (!isInSteadyState()) {
		//
		// OLD : while (servedJobs.size() + droppedJobs.size() < numberOfJobs) {
		
			
			/**
			 * Need to know what is the next event and what time it is.
			 */
			// System.out.println("Iteration!");

			nextServerID = getNextServer(); // the id of the next server going to finish
			if (jobCount <= numberOfJobs)
				nextJobArrivalTime = nextJob.getArrivalTime(); // The time of the next job arrival
			else
				nextJobArrivalTime = Double.POSITIVE_INFINITY;

			// Check the status of all servers
			serverStatus = checkServers();

			// in case all servers are empty and there is a job going to arrive
			// in case the following server is not empty (as in case of all empty servers
			// this condition will be satisfied)
			// or there is more than one job with the same arrival time, so compare it with
			// the clock time
			if ((nextJobArrivalTime < Double.POSITIVE_INFINITY && serverStatus[1] == servers.size())
					|| (servers.get(nextServerID).isEmptyStatus() == false
							&& nextJobArrivalTime <= servers.get(nextServerID).getJobBeingServed().getServiceEndTime())
					|| (nextJobArrivalTime == this.clock)) {

				previousClock = clock;
				this.clock = nextJobArrivalTime; // Change the time
				updateStateAndServerTimes(clock, previousClock);
				
				// Check that the maximum length is not exceeded
				if (queue.size() + (numberOfServers - serverStatus[1]) >= maxLength) {
					droppedJobs.add(nextJob); // add the new job to the dropped list
					//System.out.println("Job (dropped): " + Integer.toString(currentJobID));
				} else {
					queue.add(nextJob); // add the new arrived job to the queue
					//System.out.println("Job (queue): " + Integer.toString(currentJobID));
				}
				
				nextJob = new Job(clock + interArrivalTimeGenerator.generate(), sericeTimeGenerator.generate());
				jobCount++;
				// System.out.println("Arrival");

			} else // Look to get the next job after checking the queue, and current idle servers
			if ((servers.get(nextServerID).isEmptyStatus() == false
					&& nextJobArrivalTime > servers.get(nextServerID).getJobBeingServed().getServiceEndTime())
					|| (servers.get(nextServerID).getJobBeingServed().getServiceEndTime() == this.clock)) {

				previousClock = clock;
				this.clock = servers.get(nextServerID).getJobBeingServed().getServiceEndTime();
				updateStateAndServerTimes(clock, previousClock);

				servedJobs.add(servers.get(nextServerID).getJobBeingServed());

				servers.get(nextServerID).finishJob();
				// System.out.println("Departure");
			}

			// Push the jobs waiting in the queue to the servers if they are Idle
			int i = 0;
			while (queue.size() > 0 && i < servers.size()) {
				// If the server is empty and there is a job, add the job to the server
				if (servers.get(i).isEmptyStatus() == true) {
					servers.get(i).addJob(queue.get(0), this.clock); // current system time
					queue.remove(0);
				}
				// System.out.println("Push from the queue");
				i++;
			}
			
			
			//NEW
			this.CMQL = getMeanQueueLength();
			//
                                         
		}
	}

	/*public boolean isEndSimulation() {
		if (servedJobs.size() + droppedJobs.size() == numberOfJobs)
			return true;
		return false;
	}*/

	
	public int getNextServer() {

		int nextServer = 0;
		double minimumTime = Double.POSITIVE_INFINITY;
		int i = 0;
		while (i < servers.size()) {
			if (servers.get(i).isEmptyStatus() == false
					&& servers.get(i).getJobBeingServed().getServiceEndTime() < minimumTime) {
				nextServer = i;
				minimumTime = servers.get(i).getJobBeingServed().getServiceEndTime();
			}
			i++;
		}

		return nextServer;

	}
	
	
	public double getNumberOfJobsSoFar() {
		return servedJobs.size() + droppedJobs.size();
	}
	
	public void calculateMetrics(queues_analytical.Queue theoritical) {
		System.out.println("---------------- Simulation Results ----------------\n");
		System.out.println("Total Running Time: " + clock);
		int total =  droppedJobs.size() + servedJobs.size();
		System.out.println("Total Number of Jobs Encountered: " + total);
		System.out.println("Number of Dropped Jobs: " + droppedJobs.size());
		System.out.println("Dropping Probability: " + droppedJobs.size() / (double)total);
		super.calculateMetrics(theoritical);
	}

}
