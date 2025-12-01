#!/bin/bash
# quantum_test.sh
# Tests RR with different quantum values

PATRONS=30
SEED=42
SWITCH_TIME=1  # Use your determined optimal switch time here

# Array of quantum times to test
QUANTUM_TIMES=(5 10 20 30 50 75 100 150 200)

for q in "${QUANTUM_TIMES[@]}"; do
  echo "Running RR with quantum $q"
  make run ARGS="$PATRONS 2 $SWITCH_TIME $q $SEED"
  echo "------------------------"
done
