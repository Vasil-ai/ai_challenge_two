Here are a couple of validation scenarios for you to test your MCP server. Think about other validation scenarios as well to make sure your MCP server covers all the requirements and works as expected.

Scenario 1: Morning Rush
Goal: Verify basic scheduling of mixed arrivals and departures.
Steps:
1.	Start with a clean airport state.
2.	Submit several flights with mixed operation types:
o	One high-priority arrival.
o	One medium-priority departure.
o	One low-priority arrival.
o	One low-priority departure.
3.	Generate the airport schedule.
4.	Inspect the flight queue.
5.	Inspect the operation timeline.
Expected result:
•	All schedulable flights should be scheduled.
•	No runway or gate should have overlapping operations.
•	Higher-priority flights should be scheduled earlier when resources are contested.
•	The flight queue should clearly show whether any flights remain unscheduled.

Scenario 2: Heavy Hauler
Goal: Verify that runway capability constraints are respected.
Steps:
1.	Start with a clean airport state.
2.	Submit a high-priority departure requiring a runway longer than any runway available at the airport.
3.	Generate the airport schedule.
4.	Inspect the flight queue and airport status.
Expected result:
•	The oversized flight should not be scheduled.
•	The flight should remain visible with an unscheduled status.
•	The reason should clearly indicate that no suitable runway is available.
•	Other valid flights, if present, should still be schedulable.

Scenario 3: Connecting Flight
Goal: Verify dependency handling between flights.
Steps:
1.	Start with a clean airport state.
2.	Submit an inbound arrival.
3.	Submit an outbound departure that depends on the inbound flight.
4.	Generate the airport schedule.
5.	Inspect the operation timeline.
Expected result:
•	Both flights should be scheduled if resources are available.
•	The outbound flight should not start before the inbound flight has completed.
•	The configured dependency buffer should be respected.
•	The timeline should make the dependency order clear.

