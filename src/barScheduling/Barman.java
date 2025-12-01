//M. M. Kuttel 2025 mkuttel@gmail.com
package barScheduling;

//added imports for timing and file writing
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

/*
 Barman Thread class.
 */

public class Barman extends Thread {
	

	private CountDownLatch startSignal;
	private BlockingQueue<DrinkOrder> orderQueue;
	int schedAlg =0;
	int q=10000; //really big if not set, so FCFS
	private int switchTime;

	// tracking vars
	private long totalIdleTime = 0;
	private long startTime;
	private long lastOrderCompTime = 0;
	private long completedOrders = 0;
	private List<OrderStats> orderStatsList = new CopyOnWriteArrayList<>();
	private Map<Integer, PatronStats> patronStatMap = new ConcurrentHashMap<>();
	
	// Class to hold order statistics
	private static class OrderStats {
		int patronId;
        String drinkName;
        long arrivalTime;
        private volatile long firstServiceTime;
        private volatile long completionTime;
        int executionTime;

		public OrderStats(DrinkOrder order, long arrTime) {
            String[] parts = order.toString().split(": ");
            this.patronId = Integer.parseInt(parts[0]);
            this.drinkName = parts[1];
            this.executionTime = order.getExecutionTime();
            this.arrivalTime = arrTime;
            this.firstServiceTime = -1;  // Will be set when first served
            this.completionTime = -1;    // Will be set when completed
        }

		public long getTurnaroundTime() {
            return completionTime - arrivalTime;
        }
        
        public long getWaitingTime() {
            return getTurnaroundTime() - executionTime;
        }
        
        public long getResponseTime() {
            return firstServiceTime - arrivalTime;
        }
	}
	
	private static class PatronStats {
		int patronId;
		long firstOrderTime = Long.MAX_VALUE;
		long firstCompletionTime = Long.MAX_VALUE;  // Track first drink completion
		long lastCompletionTime = 0;
		long totalWaitingTime = 0;
		int drinksCompleted = 0;

		public PatronStats(int id) {
			this.patronId = id;
		}

		public long getResponseTime() {
			return firstCompletionTime - firstOrderTime;
		}

		public long getTurnaroundTime() {
			return lastCompletionTime - firstOrderTime;
		}
	}
	
	Barman(  CountDownLatch startSignal,int sAlg) {
		//which scheduling algorithm to use
		this.schedAlg=sAlg;
		if (schedAlg==1) this.orderQueue = new PriorityBlockingQueue<>(5000, Comparator.comparingInt(DrinkOrder::getExecutionTime)); //SJF
		else this.orderQueue = new LinkedBlockingQueue<>(); //FCFS & RR
	    this.startSignal=startSignal;
	}
	
	Barman(  CountDownLatch startSignal,int sAlg,int quantum, int sTime) { //overloading constructor for RR which needs q
		this(startSignal, sAlg);
		q=quantum;
		switchTime=sTime;
	}

	public void placeDrinkOrder(DrinkOrder order) throws InterruptedException {
        OrderStats orderStats = new OrderStats(order, System.currentTimeMillis());
		orderStatsList.add(orderStats);
		orderQueue.put(order);
    }

	private OrderStats findStats(DrinkOrder order) {
		String orderString = order.toString();
		synchronized(orderStatsList) {
			for (OrderStats stats : orderStatsList) {
				String statsString = stats.patronId + ": " + stats.drinkName;
				if (statsString.equals(orderString) && stats.completionTime == -1) {
					return stats;
				}
			}
		}
		return null;
	}

	// update stats, thread-safe
	private synchronized void updateStats(OrderStats stats, boolean firstService, boolean completion) {
		long now = System.currentTimeMillis();
		if (firstService && stats.firstServiceTime == -1) {
			stats.firstServiceTime = now;
			PatronStats patronStats = patronStatMap.computeIfAbsent(stats.patronId, PatronStats::new);
        	patronStats.totalWaitingTime += (now - stats.arrivalTime);
		}
		if (completion) {
			stats.completionTime = now;
			completedOrders++;
			lastOrderCompTime = now;

			PatronStats patronStats = patronStatMap.computeIfAbsent(stats.patronId, PatronStats::new);
			patronStats.firstOrderTime = Math.min(patronStats.firstOrderTime, stats.arrivalTime);
			if (patronStats.firstCompletionTime == Long.MAX_VALUE) {
				patronStats.firstCompletionTime = now;
			}

			patronStats.lastCompletionTime = Math.max(patronStats.lastCompletionTime, now);
			patronStats.drinksCompleted++;
		}
	}
	
	// method to write order statistics to a file
	public synchronized void writeStatsToFile(String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("PatronId,DrinkName,ArrivalTime,FirstServiceTime,CompletionTime,ExecutionTime,TurnaroundTime,WaitingTime,ResponseTime\n");
            
			for (OrderStats stats : orderStatsList) {
				if (stats.completionTime == -1) {
					continue;
				}

				writer.write(String.format("%d,%s,%d,%d,%d,%d,%d,%d,%d\n",
					stats.patronId,
					stats.drinkName,
					stats.arrivalTime,
					stats.firstServiceTime,
					stats.completionTime,
					stats.executionTime,
					stats.getTurnaroundTime(),
					stats.getWaitingTime(),
					stats.getResponseTime()
				));
			}

			writer.write("\nPatron Level Statistics:\n");
        	writer.write("PatronId,TotalWaitingTime,ResponseTime,Turnaround,DrinksCompleted\n");
            
			for (PatronStats stats : patronStatMap.values()) {
				writer.write(String.format("%d,%d,%d,%d,%d\n",
					stats.patronId,
					stats.totalWaitingTime,
					stats.getResponseTime(),
					stats.getTurnaroundTime(),
					stats.drinksCompleted
				));
			}

			long totalWaitingTimeAllPatrons = patronStatMap.values().stream()
            .mapToLong(ps -> ps.totalWaitingTime)
            .sum();

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            double cpuUtilization = ((double)(totalTime - totalIdleTime) / totalTime) * 100.0;
            double throughput = ((double)completedOrders / totalTime) * 1000; // Orders per second
            
            writer.write("\nStats Summary:\n");
            writer.write("Total Time: " + totalTime + " ms\n");
            writer.write("Total Waiting Time: " + totalWaitingTimeAllPatrons + " ms\n");
			writer.write("CPU Utilization: " + cpuUtilization + "%\n");
            writer.write("Throughput: " + throughput + " orders/second\n");
            writer.write("Context Switch Time: " + switchTime + " ms\n");
            if (schedAlg == 2) {
                writer.write("Time Quantum: " + q + " ms\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	public void run() {
		int interrupts=0;
		try {
			DrinkOrder currentOrder;
			
			startSignal.countDown(); //barman ready
			startSignal.await(); //check latch - don't start until told to do so

			startTime = System.currentTimeMillis(); // Start time for metrics

			if ((schedAlg==0)||(schedAlg==1)) { //FCFS and non-preemptive SJF
				while(true) {
					long idleStartTime = System.currentTimeMillis();
					currentOrder=orderQueue.take();
					long idleEndTime = System.currentTimeMillis();
					totalIdleTime += (idleEndTime - idleStartTime);

					OrderStats orderStats = findStats(currentOrder);
					if (orderStats != null) {
						updateStats(orderStats, true, false); // Set first service time
					}

					System.out.println("---Barman preparing drink for patron "+ currentOrder.toString());
					sleep(currentOrder.getExecutionTime()); //processing order (="CPU burst")
					
					//update completion time
					if (orderStats != null) {
						updateStats(orderStats, false, true); // Set completion time
					}

					System.out.println("---Barman has made drink for patron "+ currentOrder.toString());
					currentOrder.orderDone();
					sleep(switchTime);//cost for switching orders
				}
			}
			else { // RR 
				int burst=0;
				int timeLeft=0;
				System.out.println("---Barman started with q= "+q);

				while(true) {
					System.out.println("---Barman waiting for next order ");
					long idleStartTime = System.currentTimeMillis();
					currentOrder=orderQueue.take();
					long idleEndTime = System.currentTimeMillis();
					totalIdleTime += (idleEndTime - idleStartTime);

					OrderStats orderStats = findStats(currentOrder);
					if (orderStats != null) {
						updateStats(orderStats, true, false);;
					}

					System.out.println("---Barman preparing drink for patron "+ currentOrder.toString() );
					burst=currentOrder.getExecutionTime();
					if(burst<=q) { //within the quantum
						sleep(burst); //processing complete order ="CPU burst"
						
						//update completion time
						if (orderStats != null) {
							updateStats(orderStats, false, true);;
						}

						System.out.println("---Barman has made drink for patron "+ currentOrder.toString());
						currentOrder.orderDone();
					}
					else {
						sleep(q);
						timeLeft=burst-q;
						System.out.println("--INTERRUPT---preparation of drink for patron "+ currentOrder.toString()+ " time left=" + timeLeft);
						interrupts++;
						currentOrder.setRemainingPreparationTime(timeLeft);
						orderQueue.put(currentOrder); //put back on queue at end
					}
					sleep(switchTime);//switching orders
				}
			}
				
		} catch (InterruptedException e1) {
			System.out.println("---Barman is packing up ");
			System.out.println("---number interrupts="+interrupts);
		}
	}
}
