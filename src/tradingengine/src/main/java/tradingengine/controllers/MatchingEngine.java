package tradingengine.controllers;

import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import tradingengine.configs.MatchingEngineConfig;
import tradingengine.jni.MatchingEngineJNI;
import tradingengine.models.Order;

@RestController
public class MatchingEngine {
    private final  MatchingEngineJNI matchingEngineJNI;
    private final MatchingEngineConfig matchingEngineConfig;

    @Autowired
    public MatchingEngine(MatchingEngineJNI matchingEngineJNI, MatchingEngineConfig matchingEngineConfig) {
        this.matchingEngineJNI = matchingEngineJNI;
        this.matchingEngineConfig = matchingEngineConfig;
    }

    @GetMapping("/matchingengine")
    public ResponseEntity<String> testMatchingEngine(@RequestBody Order order) {
        long pointer = matchingEngineConfig.getPointer(order.symbol);
        String trades = matchingEngineJNI
        return Response.ok().build();
    }

}
