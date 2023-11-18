### Setup
- precompute hybrid Q-table. We want to know which cities have the highest probabilities.
### askPrice
- Compute cost + plan with newTask(using ASTAR) in all vehicles 
- To cost add the sense of a good city or bad, including the paths to add further value using Q-table.
- Compute lowest cost
- Apply smart ratio strategy?

### plan
- Recompute plans with all vehicles and all tasks to see if there is any improvement
- Use SLS teachers code?