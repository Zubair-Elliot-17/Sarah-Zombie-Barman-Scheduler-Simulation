# How to Run the Code

This project includes a Makefile that compiles and runs the simulation using the required command format.

## Running the Simulation

Use the following command:

make run ARGS="30 2 5 30"


- `30` → Number of patrons  
- `2` → Context switching time  
- `5` → Random seed  
- `30` → Time quantum (used only for Round Robin)

The Makefile automatically compiles all necessary Java files before running the simulation.

## there is a makefile on how to execute the shell scripts

