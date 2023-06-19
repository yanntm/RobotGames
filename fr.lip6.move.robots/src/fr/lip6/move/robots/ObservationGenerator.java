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
}
