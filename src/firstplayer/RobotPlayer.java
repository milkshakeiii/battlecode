package firstplayer;

import battlecode.common.*;

import java.util.Random;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    static int NONE_SENTINEL = 65535;

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6147);

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static Direction saved_direction = directions[rng.nextInt(directions.length)];

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        if (turnCount == 0) {
            for (int i = 0; i < 64; i++) {
                rc.writeSharedArray(i, NONE_SENTINEL);
            }
        }

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!
            // System.out.println("Age: " + turnCount + "; Location: " + rc.getLocation());

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the RobotType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                switch (rc.getType()) {
                    case ARCHON:     runArchon(rc);  break;
                    case MINER:      runMiner(rc);   break;
                    case SOLDIER:    runSoldier(rc); break;
                    case LABORATORY: // Examplefuncsplayer doesn't use any of these robot types below.
                    case WATCHTOWER: // You might want to give them a try!
                    case BUILDER:
                    case SAGE:       break;
                }
            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    /**
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runArchon(RobotController rc) throws GameActionException {
        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rng.nextBoolean()) {
            // Let's try to build a miner.
            rc.setIndicatorString("Trying to build a miner");
            if (rc.canBuildRobot(RobotType.MINER, dir)) {
                rc.buildRobot(RobotType.MINER, dir);
            }
        } else {
            // Let's try to build a soldier.
            rc.setIndicatorString("Trying to build a soldier");
            if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                rc.buildRobot(RobotType.SOLDIER, dir);
            }
        }
    }

    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runMiner(RobotController rc) throws GameActionException {
        // Try to mine on squares around us.
        MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
                while (rc.canMineLead(mineLocation)) {
                    rc.mineLead(mineLocation);
                }
            }
        }

        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
            // System.out.println("I moved!");
        }
    }

    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runSoldier(RobotController rc) throws GameActionException {
        int target_x = rc.readSharedArray(0);
        int target_y = rc.readSharedArray(1);
        int target_priority = rc.readSharedArray(2);
        int radius = rc.getType().visionRadiusSquared;

        // if target is in range but no longer valid, update shared info
        if (target_x != NONE_SENTINEL) {
            MapLocation target_location = new MapLocation(target_x, target_y);
            if (target_location.distanceSquaredTo(rc.getLocation()) <= radius) {
                RobotInfo target_robot = rc.senseRobotAtLocation(target_location);
                if (target_robot == null || target_robot.getTeam() == rc.getTeam()) {
                    target_x = NONE_SENTINEL;
                    rc.writeSharedArray(0, target_x);
                    target_y = NONE_SENTINEL;
                    rc.writeSharedArray(1, target_y);
                    target_priority = 10;
                    rc.writeSharedArray(2, 10);
                }
            }
        }

        // check for better targets and update shared info if a better target is found
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, rc.getTeam().opponent());
        for (int i = 0; i < enemies.length; i++) {
            RobotInfo enemy = enemies[i];
            int priority;
            if (enemy.getType() == RobotType.WATCHTOWER) {
                priority = 1;
            }
            else if (enemy.getType() == RobotType.SAGE) {
                priority = 2;
            }
            else if (enemy.getType() == RobotType.SOLDIER) {
                priority = 3;
            }
            else if (enemy.getType() == RobotType.ARCHON) {
                priority = 4;
            }
            else {
                priority = 5;
            }
            if (priority < target_priority) {
                target_x = enemy.location.x;
                rc.writeSharedArray(0, target_x);
                target_y = enemy.location.y;
                rc.writeSharedArray(1, target_y);
                target_priority = priority;
                rc.writeSharedArray(2, priority);
            }
        }

        // move towards or attack target
        if (target_x != NONE_SENTINEL) {
            MapLocation  target_location = new MapLocation(target_x, target_y);
            int attack_radius = rc.getType().actionRadiusSquared;
            if (target_location.distanceSquaredTo(rc.getLocation()) > attack_radius) {
                Direction direction_to = rc.getLocation().directionTo(target_location);
                if (rc.canMove(direction_to)) {
                    rc.move(direction_to);
                }
            }
            else {
                if (rc.canAttack(target_location)) {
                    rc.attack(target_location);
                }
            }
        }
        else { // or move in a line if no target
            if (rc.canMove(saved_direction)) {
                rc.move(saved_direction);
            }
            else if (!rc.onTheMap(rc.getLocation().add(saved_direction))) {
                saved_direction = directions[rng.nextInt(directions.length)];
            }
        }
    }
}
