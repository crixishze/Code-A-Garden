package codeagarden;

import java.util.List;
import javax.swing.SwingUtilities;

//Bridges the script interpreter and the farm.
//The script thread calls run(); each command is executed on the Swing thread
public class DroneController {

    private final FarmPlot plot;

    public DroneController(FarmPlot plot) {
        this.plot = plot;
    }

    // called from the script thread
    public Object run(String name, List<Object> args) {
        Object[] result = {0};
        try {
            SwingUtilities.invokeAndWait(() -> result[0] = execute(name, args));
        } catch (Exception e) {
            throw new Interpreter.Stop();   // interrupted or the farm code crashed: end the script
        }
        return result[0];
    }

    // runs on the Swing thread
    private Object execute(String name, List<Object> args) {
        switch (name) {
            case "move": plot.move(String.valueOf(args.get(0))); break;
            case "plant": plot.plantHere();   break;
            case "harvest": plot.harvestHere(); break;
            case "canHarvest": return plot.canHarvest();
        }
        return 0;
    }
}