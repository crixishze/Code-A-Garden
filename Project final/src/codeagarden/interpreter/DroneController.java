package codeagarden.interpreter;

import codeagarden.ui.FarmPanel;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * DESIGN PATTERN: Adapter.
 * The interpreter only knows "call a function with this name and these arguments".
 * The farm screen has real methods (move, plant, harvest, canHarvest). This class
 * translates one into the other, so neither side needs to know about the other.
 *
 * The commands the player can use in their code:
 *   move(North)  move(South)  move(East)  move(West)
 *   plant(Carrot)  plant(Corn)  plant(Tomato)  plant(Pumpkin)
 *   harvest()      canHarvest()   (true if the plot under the drone is ready)
 */
public class DroneController {

    private final FarmPanel farm;

    /** Connects the drone to the farm screen it controls. */
    public DroneController(FarmPanel farm) {
        this.farm = farm;
    }

    /**
     * Called from the script thread for every command in the player's code.
     * The command itself runs on the Swing thread (all screen changes must happen there),
     * and this method waits for it to finish before the script continues.
     */
    public Object run(String name, List<Object> args) {
        Object[] result = {0};
        try {
            SwingUtilities.invokeAndWait(() -> result[0] = execute(name, args));
        } catch (Exception e) {
            throw new Interpreter.Stop();   // interrupted or the farm code crashed: end the script
        }
        return result[0];
    }

    // Runs on the Swing thread: picks the farm method that matches the command name.
    private Object execute(String name, List<Object> args) {
        switch (name) {
            case "move":
                farm.moveDrone(firstArgument(args));
                break;
            case "plant":
                farm.plantHere(firstArgument(args));
                break;
            case "harvest":
                farm.harvestHere();
                break;
            case "canHarvest":
                return farm.canHarvest();
            default:
                farm.showMessage("Unknown command: " + name + "(). Use move, plant, harvest or canHarvest.");
                break;
        }
        return 0;
    }

    // The text inside the brackets, e.g. "North" for move(North). Empty if nothing was written.
    private static String firstArgument(List<Object> args) {
        return args.isEmpty() ? "" : String.valueOf(args.get(0));
    }
}
