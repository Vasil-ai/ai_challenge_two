In this task you will build a Model Context Protocol server that works as an AI-ready Air Traffic Control system.

Your goal is to create a lightweight MCP server that can coordinate flight operations at a busy airport. The system should accept incoming flight plans, schedule arrivals and departures safely, manage limited airport resources, react to disruptions, and expose airport state to AI clients through MCP tools and resources.

The focus is on scheduling logic and coordination, not on building a visual interface or simulating real aircraft physics.


Core Airport Operations
Anyone connected to the MCP server should be able to submit new flights.
A submitted flight includes: 
•	flight number
•	operation type (arrivals or departures) 
•	priority (high, medium, or low)
•	dependencies[optional]: flights can depend on other flights. For example, an outbound connecting flight should not depart before its inbound flight has completed.
•	runway requirements[optional]: the server should schedule flights across available runways and gates while avoiding conflicts.


Airport Configuration
Airport limits must be configured through environment variables.
Configuration should include:
•	Runway count
•	Gate count
•	Ground crew count
•	Runway separation buffers for takeoffs, landings, and mixed operations
•	Gate turnaround time
•	Dependency buffer time
•	Maximum scheduling horizon
Invalid configuration should fail clearly at startup.


MCP Interface
Your server must expose MCP tools that allow an AI client to:
•	Submit a new arrival or departure.
•	Generate or refresh the airport schedule — calling this tool replaces the current schedule with a freshly computed one based on the current flight queue and airport configuration. 
•	Get current airport status, including resource usage and flight counts.
•	Cancel a flight and update affected dependent flights.
•	Bottleneck analysis —  identify the sequence of dependent flights that drives the total schedule duration.

Your server must also expose MCP resources that allow an AI client to inspect:
•	The current flight queue, including unscheduled, and cancelled flights.
•	Runway availability and usage information.
•	A chronological timeline of scheduled airport operations.

You can choose the exact tool names, resource names, and data structures, as long as the capabilities are clearly documented and usable from an MCP-compatible client.

