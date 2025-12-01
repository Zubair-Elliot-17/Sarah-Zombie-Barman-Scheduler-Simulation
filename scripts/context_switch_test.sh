#!/bin/bash
# context_switch_test.sh
# Tests different context switch times for all three algorithms

PATRONS=30
SEED=42
BASE_Q=150  # Default quantum for RR during this test

# Array of context switch times to test
SWITCH_TIMES=(1 2 3 4 5)

for s in "${SWITCH_TIMES[@]}"; do
  # Run FCFS with this switch time
  echo "Running FCFS with switch time $s"
  make run ARGS="$PATRONS 0 $s $BASE_Q $SEED"
  
  # Run SJF with this switch time
  echo "Running SJF with switch time $s"
  make run ARGS="$PATRONS 1 $s $BASE_Q $SEED"
  
  # Run RR with this switch time
  echo "Running RR with switch time $s and quantum $BASE_Q"
  make run ARGS="$PATRONS 2 $s $BASE_Q $SEED"
  
  echo "Completed tests for switch time $s"
  echo "------------------------"
done
