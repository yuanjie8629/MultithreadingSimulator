import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.*;

public class Program {
	private static StorageBag bagA = new StorageBag();
	private static StorageBag bagB = new StorageBag();
	private static Queue<Character> queue = new LinkedList<>();
	
	private static Lock lc = new ReentrantLock();
	private static Condition newItemArrived = lc.newCondition();
	private static Condition bagAFull = lc.newCondition();
	private static Condition bagBFull = lc.newCondition();
	
	public static void main(String[] args) {
		ExecutorService executor = Executors.newFixedThreadPool(4);
		executor.execute(new DispatcherATask());
		executor.execute(new DispatcherBTask());
		executor.execute(new SorterTask());
		executor.execute(new GenerateItemTask());
		executor.shutdown();
		
	}
	
	private static class StorageBag {
		private static int capacity = 10;
		int numberOfItem = 0;
		
		public StorageBag() {
		}
		
		public void addItem() {
			if (!isFull()) {
				numberOfItem++;
			}
		}
		
		public boolean isFull() {
			return numberOfItem == capacity;
		}
		
		public void dispatch() {
			numberOfItem = 0;
		}
		
	}
	
	public static class GenerateItemTask implements Runnable {
		private static char newItem;
		
		@Override
		public void run() {
			while (true) {
				try {
					//Wait for random time ranged from 3 to 6 seconds
					Thread.sleep((long)((Math.random() * 3000) + 3000)); 
					lc.lock();
					
					Random rand = new Random();
					
					newItem = (char)('A' + rand.nextInt(2));  //Random generate Item A or Item B
					System.out.println("Arrival: Item " + newItem);
					queue.offer(newItem);
					newItemArrived.signalAll();
				}
				catch(InterruptedException ex){
					ex.printStackTrace();
					}
				finally {
					lc.unlock();
				}
			}
		}
		
	}

	public static class SorterTask implements Runnable {
		char arrivedItem;
		
		@Override
		public void run() {
			while (true) {
				try {
					//Wait for random time ranged from 2 to 5 seconds
					Thread.sleep((long)((Math.random() * 2000) + 3000)); 
					lc.lock();
					
					while (queue.peek() == null) {  
						System.out.println("Sorter : waiting item...");
						newItemArrived.await();
					}
					
					arrivedItem = queue.poll();
					
					if (arrivedItem == 'A') {
						bagA.addItem();
						System.out.println("Sorter : To bag A (" + bagA.numberOfItem + ")");
						if (bagA.isFull()) {
							System.out.println("Sorter : Bag A Full");
							bagAFull.signalAll();
						}
					}
					else {
						bagB.addItem();
						System.out.println("Sorter : To bag B (" + bagB.numberOfItem + ")");
						if (bagB.isFull()) {
							System.out.println("Sorter : Bag B Full");
							bagBFull.signalAll();
						}
					}
				}
				catch(InterruptedException ex){
					ex.printStackTrace();
				}
				finally {
					lc.unlock();
				}
			}
		}
		
	}
	

	public static class DispatcherATask implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					//Wait for random time ranged from 2 to 5 seconds
					Thread.sleep((long)((Math.random() * 2000) + 3000)); 
					lc.lock();
					while(!bagA.isFull()) {
						System.out.println("Dispatcher A: waiting bag A to be full...");
						bagAFull.await();
					}
					
					if (bagA.isFull()) {
						System.out.println("Dispatcher A: moving bag A to Conveyor...");
						Thread.sleep(1000);
						bagA.dispatch();
						System.out.println("Bag A has been moved to Conveyor. New supply of bag A is added.");
					}
				}
				catch(InterruptedException ex){
					ex.printStackTrace();
				}
				finally {
					lc.unlock();
				}
			}
		}
		
	}
	
	public static class DispatcherBTask implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					//Wait for random time ranged from 2 to 5 seconds
					Thread.sleep((long)((Math.random() * 2000) + 3000));
					lc.lock();
					while(!bagB.isFull()) {
						System.out.println("Dispatcher B: waiting bag B to be full...");
						bagBFull.await();
					}
					
					if (bagB.isFull()) {
						System.out.println("Dispatcher B: moving bag B to Conveyor...");
						Thread.sleep(1000);
						bagB.dispatch(); 
						System.out.println("Bag B has been moved to Conveyor. New supply of bag B is added.");
					}
				}
				catch(InterruptedException ex){
					ex.printStackTrace();
				}
				finally {
					lc.unlock();
				}
			}
		}
		
	}
	
}