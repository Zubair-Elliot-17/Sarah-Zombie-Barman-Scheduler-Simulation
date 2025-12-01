#!/bin/bash
# compare_algorithms.sh
# Runs full comparison of all algorithms with optimized parameters

# Your optimized parameters
PATRONS=30
SWITCH_TIME=1 # Replace with your determined value
OPTIMAL_Q=10 # Replace with your determined value
ITERATIONS=10   # Number of runs per algorithm for statistical significance

# Create a unique subdirectory for this experiment set
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_DIR="results_$TIMESTAMP"
mkdir -p "output/$RESULTS_DIR"

# Run multiple iterations
for i in $(seq 1 $ITERATIONS); do
  SEED=$((42 + $i))  # Vary the seed slightly for each iteration
  
  echo "Running iteration $i with seed $SEED"
  
  # FCFS
  echo "Running FCFS..."
  make run ARGS="$PATRONS 0 $SWITCH_TIME $OPTIMAL_Q $SEED $RESULTS_DIR/fcfs_iter${i}.csv"
  
  # SJF
  echo "Running SJF..."
  make run ARGS="$PATRONS 1 $SWITCH_TIME $OPTIMAL_Q $SEED $RESULTS_DIR/sjf_iter${i}.csv"
  
  # RR
  echo "Running RR..."
  make run ARGS="$PATRONS 2 $SWITCH_TIME $OPTIMAL_Q $SEED $RESULTS_DIR/rr_iter${i}.csv"
  
  echo "Completed iteration $i"
  echo "------------------------"
done

echo "All experiments completed. Results saved in output/$RESULTS_DIR/"
