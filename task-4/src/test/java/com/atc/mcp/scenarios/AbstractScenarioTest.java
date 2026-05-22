package com.atc.mcp.scenarios;

import com.atc.mcp.mcp.tools.AtcToolService;
import com.atc.mcp.store.AirportStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
abstract class AbstractScenarioTest {

    @Autowired
    protected AtcToolService toolService;

    @Autowired
    protected AirportStateStore store;

    @BeforeEach
    void resetAirport() {
        store.clear();
    }
}
