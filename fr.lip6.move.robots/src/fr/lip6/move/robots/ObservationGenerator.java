package fr.lip6.move.robots;

import java.util.*;

public class ObservationGenerator {

    public static List<int[]> generateObservations(int n, int k) {
        List<int[]> result = new ArrayList<>();
        generateObservationsHelper(n, k, new int[n], 0, result);
        result.sort((a,b)-> -Arrays.compare(a, b));
        return result;
    }

    private static List<int[]>[] splitSymmetricObservations(List<int[]> result) {
        List<int[]> [] split = (List<int[]>[]) new List<?>[2];
        split[0] = new ArrayList<>();
        split[1] = new ArrayList<>();
        for (int [] obs: result) {
        	if (isSymmetric(obs)) {
        		split[0].add(obs);
        	} else {
        		split[1].add(obs);
        	}
        }
        
        return split;
    }
    
    public static List<int[]>[] generateSplitObservations(int n, int k) {
    	return splitSymmetricObservations(generateObservations(n, k));
    }
    
	private static boolean isSymmetric(int[] observation) {
        for (int i = 1, j = observation.length - 1; i < j; i++, j--) {
            if (observation[i] != observation[j]) {
                return false;
            }
        }
        return true; // the observation is symmetric
    }
	
    private static void generateObservationsHelper(int n, int k, int[] current, int index, List<int[]> result) {
        if (n == 0) {
            if (k == 0 && current[0] != 0) { // Ensure the first index is non-zero
                // Determine whether reversing would result in a lexicographically larger array
                if (isReversedMaximal(current)) {
                    result.add(current.clone()); // We add a clone to avoid modifying the array that is already added
                }
            }
            return;
        }

        for (int i = 0; i <= k; i++) {
            current[index] = i;
            generateObservationsHelper(n - 1, k - i, current, index + 1, result);
        }
    }

    public static boolean isReversedMaximal(int[] observation) {
        for (int i = 1, j = observation.length - 1; i < j; i++, j--) {
            if (observation[i] > observation[j]) {
                return true;
            } else if (observation[i] < observation[j]) {
                return false;
            }
        }
        return true; // the observation is symmetric
    }

    public static void main(String[] args) {
        int maxN = 10;
        int maxK = 10;
        int[][] table = new int[maxN+1][maxK+1];

        for (int N = 1; N <= maxN; N++) {
            for (int K = 1; K <= maxK; K++) {
                List<int[]> observations = generateObservations(N, K);
                table[N][K] = observations.size();
                if (N==4 && K ==3) {                	
                    for (int[] observation : observations) {
                        System.out.println(Arrays.toString(observation));
                    }
                }
            }
        }

        // Print the table
        System.out.print("    ");
        for (int K = 1; K <= maxK; K++) {
            System.out.printf("%7d", K);
        }
        System.out.println();

        for (int N = 1; N <= maxN; N++) {
            System.out.printf("%2d:", N);
            for (int K = 1; K <= maxK; K++) {
                System.out.printf("%7d", table[N][K]);
            }
            System.out.println();
        }
    }
    
    public static Action decideAction(int[] observation, boolean isSymmetric) {
        int max = Integer.MIN_VALUE;
        int maxIndex = -1;
        int maxCount = 0;

        for (int i = 0; i < observation.length; i++) {
            if (observation[i] > max) {
                max = observation[i];
                maxIndex = i;
                maxCount = 1;
            } else if (observation[i] == max) {
                maxCount++;
            }
        }

        if (maxCount > 1) {
            return Action.TOTAL;  // not rigid
        }

        // Assign actions based on the rules
        if (maxIndex == 0) {
            return Action.STAY;  // on the max, stay
        } else if (isSymmetric) {
            if (observation[1] == 0 && observation[observation.length - 1] == 0) {
                return Action.MOVE;  // both cells are empty, move
            }
            return Action.STAY;  // otherwise, stay
        } else {
            // if closer or equidistant to the left, move left unless there's a robot
        	int dleft = (observation.length - 1) - maxIndex;
        	int dright = maxIndex -1;
            if (dleft < dright) {
                return observation.length-1 == maxIndex ||  observation[observation.length - 1] == 0 ? Action.LEFT : Action.STAY;
            } else {
                return maxIndex==1 || observation[1] == 0 ? Action.RIGHT : Action.STAY;  // otherwise, move right unless someone is adjacent in position 1
            }
        }
    }

    
    public static List<int[]>[] filterRigidObservations(List<int[]>[] observations, Map<int[], Action> rigid) {
        @SuppressWarnings("unchecked")
		List<int[]>[] newObservations = new ArrayList[2];

        for (int i = 0; i < 2; i++) {
            newObservations[i] = new ArrayList<>();
            boolean isSymmetric = i == 0;
            for (int j = 0; j < observations[i].size(); j++) {
                int[] observation = observations[i].get(j);
                Action action = decideAction(observation, isSymmetric);
                if (action == Action.TOTAL) {
                    newObservations[i].add(observation);
                } else {
                    rigid.put(observation, action);
                }
            }
        }

        return newObservations;
    }

}
