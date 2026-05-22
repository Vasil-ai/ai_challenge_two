package com.atc.mcp.scheduler;

import com.atc.mcp.domain.Flight;
import com.atc.mcp.domain.OperationType;
import com.atc.mcp.domain.Priority;
import com.atc.mcp.domain.RunwayRequirements;
import com.atc.mcp.domain.UnscheduledReason;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyResolverTest {

    @Test
    void respectsDependenciesAndPrioritisesHighWithinTopoLevels() {
        Flight a = flight("A", Priority.LOW);
        Flight b = flight("B", Priority.HIGH, "A");
        Flight c = flight("C", Priority.MEDIUM);

        DependencyResolver.Result result = DependencyResolver.resolve(List.of(a, b, c), Set.of());

        // C (MEDIUM) and A (LOW) are both ready first; the priority-aware topo
        // sort prefers C, then A, and finally B once A unblocks it.
        assertThat(result.ordered())
                .extracting(Flight::flightNumber)
                .containsExactly("C", "A", "B");
        assertThat(result.excluded()).isEmpty();
    }

    @Test
    void detectsCycle() {
        Flight x = flight("X", Priority.HIGH, "Y");
        Flight y = flight("Y", Priority.HIGH, "X");

        DependencyResolver.Result result = DependencyResolver.resolve(List.of(x, y), Set.of());

        assertThat(result.ordered()).isEmpty();
        assertThat(result.excluded()).containsValues(
                UnscheduledReason.DEPENDENCY_CYCLE, UnscheduledReason.DEPENDENCY_CYCLE);
    }

    @Test
    void marksUnknownDependencyAsMissing() {
        Flight a = flight("A", Priority.HIGH, "Z");

        DependencyResolver.Result result = DependencyResolver.resolve(List.of(a), Set.of());

        assertThat(result.excluded()).containsEntry("A", UnscheduledReason.DEPENDENCY_MISSING);
        assertThat(result.ordered()).isEmpty();
    }

    @Test
    void marksCancelledDependencyClearly() {
        Flight a = flight("A", Priority.HIGH, "Z");

        DependencyResolver.Result result = DependencyResolver.resolve(List.of(a), Set.of("Z"));

        assertThat(result.excluded()).containsEntry("A", UnscheduledReason.DEPENDENCY_CANCELLED);
    }

    private static Flight flight(String number, Priority priority, String... deps) {
        return new Flight(number, OperationType.DEPARTURE, priority, List.of(deps),
                RunwayRequirements.NONE, hashOrder(number));
    }

    private static long hashOrder(String number) {
        return number.hashCode() & 0xffffffffL;
    }
}
