import SimpleEconomyModel.SimpleEconomyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simudyne.core.exec.runner.ModelRunner;
import simudyne.core.exec.runner.RunnerBackend;
import simudyne.core.exec.runner.definition.BatchDefinitionsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MainNoConsole {
    private static final Logger logger = LoggerFactory.getLogger("main");

    public static void main(String[] args) {
        try {
            RunnerBackend runnerBackend = RunnerBackend.create();
            ModelRunner modelRunner = runnerBackend.forModel(SimpleEconomyModel.class);

            Map<String, String> _config;
            Map<String, Object> _input;
            if(args.length == 2) {
                _config = getConfigMap(args[0]);
                _input = getMap(args[1]);
            }
            else {
                // Default config and input parameters
                _config = getConfigMap("simudyne.core.export-path=output");
                _input = getMap("I=2,H=1000,alpha=1000,w=1000");
            }

            String _seed = System.getProperty("seed", "123");
            String _runs = System.getProperty("runs", "1");
            String _ticks = System.getProperty("ticks", "12");
            long seed = Long.parseLong(_seed);
            int n_runs = Integer.parseInt(_runs);

            long n_ticks = Long.parseLong(_ticks);
            _input.put("nSteps", n_ticks);

            long startTime = System.currentTimeMillis();

            BatchDefinitionsBuilder runDefinitionBuilder =
                    BatchDefinitionsBuilder.create()
                            .forRuns(n_runs)
                            .forTicks(n_ticks)
                            .forSeeds(seed)
                            .withInputs(_input);

            logger.warn("Simulation Starting...");
            modelRunner.forRunDefinitionBuilder(runDefinitionBuilder).withConfigMap(_config);
            modelRunner.run().awaitOutput();

            logger.warn("Simulation Complete.");

            long endTime = System.currentTimeMillis();
            logger.warn("time for simulation = " + (double)(endTime - startTime) / 1000.0 + " s.");

            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, String> getConfigMap(String str) {
        Map<String, Object> _map = getMap(str);
        return _map.entrySet().stream() .collect(Collectors.toMap(Map.Entry::getKey, e -> (String)e.getValue()));
    }

    public static Map<String, Object> getMap(String str) {
        Map<String, Object> _map = new HashMap<>();
        if(str.isEmpty())
            return _map;

        for(String s0 : str.split(",")) {
            String[] s1 = s0.split("=");
            if(s1.length != 2)
                throw  new IllegalArgumentException("Unknown input format, " + s0);

            _map.put(s1[0], s1[1]);
        }

        return _map;
    }

}

