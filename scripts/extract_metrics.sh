#!/bin/bash
# extract_metrics.sh
# Extracts key metrics from result files and combines them

OUTPUT_FILE="all_metrics_summary.csv"

echo "Algorithm,SwitchTime,Quantum,PatronCount,TotalTime,TotalWaitingTime,CPUUtilization,Throughput,AvgResponseTime,AvgTurnaroundTime,AvgWaitingTime" > $OUTPUT_FILE

# Process all result files
find output -name "*.csv" | while read file; do
  # Extract algorithm from filename
  if [[ $file == *"fcfs"* ]]; then
    ALG="FCFS"
  elif [[ $file == *"sjf"* ]]; then
    ALG="SJF"
  elif [[ $file == *"rr"* ]]; then
    ALG="RR"
  else
    ALG="Unknown"
  fi
  
  # Extract switch time and quantum (if present)
  SWITCH=$(grep "Context Switch Time" $file | awk '{print $4}')
  QUANTUM=$(grep "Time Quantum" $file | awk '{print $3}')
  if [ -z "$QUANTUM" ]; then
    QUANTUM="N/A"
  fi
  
  # Extract patron count from filename or default to 30
  PATRONS=30
  
  # Extract key metrics
  TOTAL_TIME=$(grep "Total Time" $file | awk '{print $3}')
  TOTAL_WAITING=$(grep "Total Waiting Time" $file | awk '{print $4}')
  CPU_UTIL=$(grep "CPU Utilization" $file | awk '{print $3}' | tr -d '%')
  THROUGHPUT=$(grep "Throughput" $file | awk '{print $2}')
  
  # Calculate average response time from Patron Level Statistics section
  # First find the line number where Patron Level Statistics starts
  PATRON_STATS_START=$(grep -n "Patron Level Statistics:" $file | cut -d ':' -f 1)
  
  # Find the line that contains ResponseTime in the header
  HEADER_LINE=$((PATRON_STATS_START + 1))
  
  # Find the column index for ResponseTime
  RESPONSE_TIME_COL=$(head -n $HEADER_LINE $file | tail -n 1 | tr ',' '\n' | grep -n "ResponseTime" | cut -d ':' -f 1)
  
  # Find the column index for TotalWaitingTime
  WAITING_TIME_COL=$(head -n $HEADER_LINE $file | tail -n 1 | tr ',' '\n' | grep -n "TotalWaitingTime" | cut -d ':' -f 1)
  
  # Find the column index for Turnaround
  TURNAROUND_COL=$(head -n $HEADER_LINE $file | tail -n 1 | tr ',' '\n' | grep -n "Turnaround" | cut -d ':' -f 1)
  
  # Find where the patron stats section ends (looking for the next section header)
  STATS_SUMMARY_START=$(grep -n "Stats Summary:" $file | cut -d ':' -f 1)
  if [ -z "$STATS_SUMMARY_START" ]; then
    # If no Stats Summary, then count to the end of file
    PATRON_STATS_END=$(wc -l $file | awk '{print $1}')
  else
    PATRON_STATS_END=$((STATS_SUMMARY_START - 1))
  fi
  
  # Calculate number of patrons from the data
  PATRON_COUNT=$((PATRON_STATS_END - PATRON_STATS_START - 1))
  PATRONS=$PATRON_COUNT
  
  # Extract and calculate averages
  PATRON_DATA=$(head -n $PATRON_STATS_END $file | tail -n $PATRON_COUNT)
  
  # Calculate average response time
  AVG_RESPONSE=$(echo "$PATRON_DATA" | awk -F ',' -v col="$RESPONSE_TIME_COL" '{sum += $col} END {print sum/NR}')
  
  # Calculate average waiting time
  AVG_WAITING=$(echo "$PATRON_DATA" | awk -F ',' -v col="$WAITING_TIME_COL" '{sum += $col} END {print sum/NR}')
  
  # Calculate average turnaround time
  AVG_TURNAROUND=$(echo "$PATRON_DATA" | awk -F ',' -v col="$TURNAROUND_COL" '{sum += $col} END {print sum/NR}')
  
  echo "$ALG,$SWITCH,$QUANTUM,$PATRONS,$TOTAL_TIME,$TOTAL_WAITING,$CPU_UTIL,$THROUGHPUT,$AVG_RESPONSE,$AVG_TURNAROUND,$AVG_WAITING" >> $OUTPUT_FILE
done

echo "Metrics summary saved to $OUTPUT_FILE"