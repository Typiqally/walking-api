package org.powbot.dax.engine.interaction;

import org.tribot.api.General;
import org.tribot.api2007.Objects;
import org.tribot.api2007.*;
import org.tribot.api2007.ext.Filters;
import org.tribot.api2007.types.*;
import org.powbot.dax.shared.helpers.RSObjectHelper;
import org.powbot.dax.engine.Loggable;
import org.powbot.dax.engine.WaitFor;
import org.powbot.dax.engine.WalkerEngine;
import org.powbot.dax.engine.bfs.BFS;
import org.powbot.dax.engine.local.PathAnalyzer;
import org.powbot.dax.engine.local.Reachable;
import org.powbot.dax.engine.collision.RealTimeCollisionTile;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class PathObjectHandler implements Loggable {

    private static PathObjectHandler instance;

    private final TreeSet<String> sortedOptions, sortedBlackList, sortedBlackListOptions, sortedHighPriorityOptions;

    private PathObjectHandler(){
        sortedOptions = new TreeSet<>(
		        Arrays.asList("Enter", "Cross", "Pass", "Open", "Close", "Walk-through", "Use", "Pass-through", "Exit",
                "Walk-Across", "Go-through", "Walk-across", "Climb", "Climb-up", "Climb-down", "Climb-over", "Climb over", "Climb-into", "Climb-through",
                "Board", "Jump-from", "Jump-across", "Jump-to", "Squeeze-through", "Jump-over", "Pay-toll(10gp)", "Step-over", "Walk-down", "Walk-up","Walk-Up", "Travel", "Get in",
                "Investigate", "Operate", "Climb-under","Jump","Crawl-down","Crawl-through","Activate","Push","Squeeze-past","Walk-Down",
                "Swing-on", "Climb up","Pass-Through","Jump-up","Jump-down","Swing across"));


        sortedBlackList = new TreeSet<>(Arrays.asList("Coffin","Drawers","null","Ornate railing","Wardrobe"));
        sortedBlackListOptions = new TreeSet<>(Arrays.asList("Chop down"));
        sortedHighPriorityOptions = new TreeSet<>(Arrays.asList("Pay-toll(10gp)","Squeeze-past"));
    }

    private static PathObjectHandler getInstance(){
        return instance != null ? instance : (instance = new PathObjectHandler());
    }

    private enum SpecialObject {
        WEB("Web", "Slash", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return Objects.find(15,
                        Filters.Objects.inArea(new RSArea(destinationDetails.getAssumed(), 1))
                                .and(Filters.Objects.nameEquals("Web"))
                                .and(Filters.Objects.actionsContains("Slash"))).length > 0;
            }
        }),
        ROCKFALL("Rockfall", "Mine", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return Objects.find(15,
                        Filters.Objects.inArea(new RSArea(destinationDetails.getAssumed(), 1))
                                .and(Filters.Objects.nameEquals("Rockfall"))
                                .and(Filters.Objects.actionsContains("Mine"))).length > 0;
            }
        }),
        ROOTS("Roots", "Chop", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return Objects.find(15,
                        Filters.Objects.inArea(new RSArea(destinationDetails.getAssumed(), 1))
                                .and(Filters.Objects.nameEquals("Roots"))
                                .and(Filters.Objects.actionsContains("Chop"))).length > 0;
            }
        }),
        ROCK_SLIDE("Rockslide", "Climb-over", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return Objects.find(15,
                        Filters.Objects.inArea(new RSArea(destinationDetails.getAssumed(), 1))
                                .and(Filters.Objects.nameEquals("Rockslide"))
                                .and(Filters.Objects.actionsContains("Climb-over"))).length > 0;
            }
        }),
        ROOT("Root", "Step-over", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return Objects.find(15,
                        Filters.Objects.inArea(new RSArea(destinationDetails.getAssumed(), 1))
                                .and(Filters.Objects.nameEquals("Root"))
                                .and(Filters.Objects.actionsContains("Step-over"))).length > 0;
            }
        }),
        BRIMHAVEN_VINES("Vines", "Chop-down", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return Objects.find(15,
                        Filters.Objects.inArea(new RSArea(destinationDetails.getAssumed(), 1))
                                .and(Filters.Objects.nameEquals("Vines"))
                                .and(Filters.Objects.actionsContains("Chop-down"))).length > 0;
            }
        }),
        AVA_BOOKCASE ("Bookcase", "Search", new Tile(3097, 3359, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getDestination().getX() >= 3097 && destinationDetails.getAssumed().equals(new Tile(3097, 3359, 0));
            }
        }),
        AVA_LEVER ("Lever", "Pull", new Tile(3096, 3357, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getDestination().getX() < 3097 && destinationDetails.getAssumed().equals(new Tile(3097, 3359, 0));
            }
        }),
        ARDY_DOOR_LOCK_SIDE("Door", "Pick-lock", new Tile(2565, 3356, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return Players.local().tile().getX() >= 2565 && Players.local().tile().distanceTo(new Tile(2565, 3356, 0)) < 3;
            }
        }),
        ARDY_DOOR_UNLOCKED_SIDE("Door", "Open", new Tile(2565, 3356, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return Players.local().tile().getX() < 2565 && Players.local().tile().distanceTo(new Tile(2565, 3356, 0)) < 3;
            }
        }),
        YANILLE_DOOR_LOCK_SIDE("Door", "Pick-lock", new Tile(2601, 9482, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return Players.local().tile().getY() <= 9481 && Players.local().tile().distanceTo(new Tile(2601, 9482, 0)) < 3;
            }
        }),
        YANILLE_DOOR_UNLOCKED_SIDE("Door", "Open", new Tile(2601, 9482, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return Players.local().tile().getY() > 9481 && Players.local().tile().distanceTo(new Tile(2601, 9482, 0)) < 3;
            }
        }),
        EDGEVILLE_UNDERWALL_TUNNEL("Underwall tunnel", "Climb-into", new Tile(3138, 3516, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getAssumed().equals(new Tile(3138, 3516, 0));
            }
        }),
        VARROCK_UNDERWALL_TUNNEL("Underwall tunnel", "Climb-into", new Tile(3141, 3513, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getAssumed().equals(new Tile(3141, 3513, 0 ));
            }
        }),
        GAMES_ROOM_STAIRS("Stairs", "Climb-down", new Tile(2899, 3565, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getDestination().getTile().equals(new Tile(2899, 3565, 0)) &&
                    destinationDetails.getAssumed().equals(new Tile(2205, 4934, 1));
            }
        }),
        FALADOR_GATE("Gate", "Close", new Tile(3031, 3314, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getDestination().getTile().equals(new Tile(3031, 3314, 0));
            }
        }),
        CANIFIS_BASEMENT_WALL("Wall", "Search", new Tile(3480, 9836, 0),new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getDestination().getTile().equals(new Tile(3480, 9836, 0)) ||
                    destinationDetails.getAssumed().equals(new Tile(3480, 9836, 0));
            }
        }),
        BRINE_RAT_CAVE_BOULDER("Cave", "Exit", new Tile(2690, 10125, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getDestination().getTile().equals(new Tile(2690, 10125, 0))
                    && NPCs.find(Filters.NPCs.nameEquals("Boulder").and(Filters.NPCs.actionsContains("Roll"))).length > 0;
            }
        });

        private String name, action;
        private Tile location;
        private SpecialCondition specialCondition;

        SpecialObject(String name, String action, Tile location, SpecialCondition specialCondition){
            this.name = name;
            this.action = action;
            this.location = location;
            this.specialCondition = specialCondition;
        }

        public String getName() {
            return name;
        }

        public String getAction() {
            return action;
        }

        public Tile getLocation() {
            return location;
        }

        public boolean isSpecialCondition(PathAnalyzer.DestinationDetails destinationDetails){
            return specialCondition.isSpecialLocation(destinationDetails);
        }

        public static SpecialObject getValidSpecialObjects(PathAnalyzer.DestinationDetails destinationDetails){
            for (SpecialObject object : values()){
                if (object.isSpecialCondition(destinationDetails)){
                    return object;
                }
            }
            return null;
        }

    }

    private abstract static class SpecialCondition {
        abstract boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails);
    }

    public static boolean handle(PathAnalyzer.DestinationDetails destinationDetails, List<RSTile> path){
        RealTimeCollisionTile start = destinationDetails.getDestination(), end = destinationDetails.getNextTile();

        RSObject[] interactiveObjects = null;

        String action = null;
        SpecialObject specialObject = SpecialObject.getValidSpecialObjects(destinationDetails);
        if (specialObject == null) {
            if ((interactiveObjects = getInteractiveObjects(start.getX(), start.getY(), start.getZ(), destinationDetails)).length < 1 && end != null) {
                interactiveObjects = getInteractiveObjects(end.getX(), end.getY(), end.getZ(), destinationDetails);
            }
        } else {
            action = specialObject.getAction();
            Predicate<RSObject> specialObjectFilter = Filters.Objects.nameEquals(specialObject.getName())
                                                                     .and(Filters.Objects.actionsContains(specialObject.getAction()))
                                                                     .and(Filters.Objects.inArea(new RSArea(specialObject.getLocation() != null ? specialObject.getLocation() : destinationDetails.getAssumed(), 1)));
            interactiveObjects = Objects.findNearest(15, specialObjectFilter);
        }

        if (interactiveObjects.length == 0) {
            return false;
        }

        StringBuilder stringBuilder = new StringBuilder("Sort Order: ");
        Arrays.stream(interactiveObjects).forEach(rsObject -> stringBuilder.append(rsObject.getDefinition().getName()).append(" ").append(
		        Arrays.asList(rsObject.getDefinition().getActions())).append(", "));
        getInstance().log(stringBuilder);

        return handle(path, interactiveObjects[0], destinationDetails, action, specialObject);
    }

    private static boolean handle(List<RSTile> path, RSObject object, PathAnalyzer.DestinationDetails destinationDetails, String action, SpecialObject specialObject){
        PathAnalyzer.DestinationDetails current = PathAnalyzer.furthestReachableTile(path);

        if (current == null){
            return false;
        }

        RealTimeCollisionTile currentFurthest = current.getDestination();
        if (!Player.isMoving() && (!object.isOnScreen() || !object.isClickable())){
            if (!WalkerEngine.getInstance().clickMinimap(destinationDetails.getDestination())){
                return false;
            }
        }
        if (WaitFor.condition(Random.nextInt(5000, 8000), () -> object.isOnScreen() && object.isClickable() ? WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE) != WaitFor.Return.SUCCESS) {
            return false;
        }

        boolean successfulClick = false;

        if (specialObject != null) {
            getInstance().log("Detected Special Object: " + specialObject);
            switch (specialObject){
                case WEB:
                    List<RSObject> webs;
                    int iterations = 0;
                    while ((webs = Arrays.stream(Objects.getAt(object.getPosition()))
                            .filter(object1 -> Arrays.stream(RSObjectHelper.getActions(object1))
                                    .anyMatch(s -> s.equals("Slash"))).collect(Collectors.toList())).size() > 0){
                        RSObject web = webs.get(0);
                        if (canLeftclickWeb()) {
                            InteractionHelper.click(web, "Slash");
                        } else {
                            useBladeOnWeb(web);
                        }
                        if(Game.isUptext("->")){
                            Walking.blindWalkTo(Players.local().tile());
                        }
                        if (web.getPosition().distanceTo(Players.local().tile()) <= 1) {
                            WaitFor.milliseconds(Random.nextIntSD(50, 800, 250, 150));
                        } else {
                            WaitFor.milliseconds(2000, 4000);
                        }
                        if (Reachable.getMap().getParent(destinationDetails.getAssumedX(), destinationDetails.getAssumedY()) != null &&
                                (webs = Arrays.stream(Objects.getAt(object.getPosition())).filter(object1 -> Arrays.stream(RSObjectHelper.getActions(object1))
                                        .anyMatch(s -> s.equals("Slash"))).collect(Collectors.toList())).size() == 0){
                            successfulClick = true;
                            break;
                        }
                        if (iterations++ > 5){
                            break;
                        }
                    }
                    break;
                case ARDY_DOOR_LOCK_SIDE:
                case YANILLE_DOOR_LOCK_SIDE:
                    for (int i = 0; i < Random.nextInt(15, 25); i++) {
                        if (!clickOnObject(object, new String[]{specialObject.getAction()})){
                            continue;
                        }
                        if (Players.local().tile().distanceTo(specialObject.getLocation()) > 1){
                            WaitFor.condition(Random.nextInt(3000, 4000), () -> Players.local().tile().distanceTo(specialObject.getLocation()) <= 1 ? WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE);
                        }
                        if (Players.local().tile().equals(new Tile(2564, 3356, 0))){
                            successfulClick = true;
                            break;
                        }
                    }
                    break;
                case VARROCK_UNDERWALL_TUNNEL:
                    if(!clickOnObject(object,specialObject.getAction())){
                        return false;
                    }
                    successfulClick = true;
                    WaitFor.condition(10000, () ->
                            SpecialObject.EDGEVILLE_UNDERWALL_TUNNEL.getLocation().equals(Players.local().tile()) ?
                                    WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE);
                    break;
                case EDGEVILLE_UNDERWALL_TUNNEL:
                    if(!clickOnObject(object,specialObject.getAction())){
                        return false;
                    }
                    successfulClick = true;
                    WaitFor.condition(10000, () ->
                            SpecialObject.VARROCK_UNDERWALL_TUNNEL.getLocation().equals(Players.local().tile()) ?
                                    WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE);
                    break;
                case FALADOR_GATE:
                    int targetTile = path.indexOf(PathAnalyzer.closestTileInPathToPlayer(path).getTile());
                    targetTile += 10;
                    if(targetTile > path.size()){
                        targetTile = path.size()-1;
                    }
                    return Walking.blindWalkTo(path.get(targetTile));
                case BRINE_RAT_CAVE_BOULDER:
                    RSNPC boulder = InteractionHelper.getRSNPC(Filters.NPCs.nameEquals("Boulder").and(Filters.NPCs.actionsContains("Roll")));
                    if(InteractionHelper.click(boulder, "Roll")){
                        if(WaitFor.condition(12000,
                            () -> NPCs.find(Filters.NPCs.nameEquals("Boulder").and(Filters.NPCs.actionsContains("Roll"))).length == 0 ?
                                WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE) == WaitFor.Return.SUCCESS){
                            WaitFor.milliseconds(3500, 6000);
                        }
                    }
                    break;
            }
        }

        if (!successfulClick){
            try {
                String[] validOptions = action != null ? new String[]{action} : getViableOption(
                        Arrays.stream(object.getDefinition().getActions()).filter(getInstance().sortedOptions::contains).collect(
                                Collectors.toList()), destinationDetails);
                if (!clickOnObject(object, validOptions)) {
                    return false;
                }
            } catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }

        boolean strongholdDoor = isStrongholdDoor(object);

        if (strongholdDoor){
            if (WaitFor.condition(Random.nextInt(6700, 7800), () -> {
                Tile playerPosition = Players.local().tile();
                if (BFS.isReachable(RealTimeCollisionTile.get(playerPosition.getX(), playerPosition.getY(), playerPosition.getPlane()), destinationDetails.getNextTile(), 50)) {
                    WaitFor.milliseconds(500, 1000);
                    return WaitFor.Return.SUCCESS;
                }
                if (NPCInteraction.isConversationWindowUp()) {
                    handleStrongholdQuestions();
                    return WaitFor.Return.SUCCESS;
                }
                return WaitFor.Return.IGNORE;
            }) != WaitFor.Return.SUCCESS){
                return false;
            }
        }

        WaitFor.Return result = WaitFor.condition(Random.nextInt(8500, 11000), () -> {
            DoomsToggle.handleToggle();
            PathAnalyzer.DestinationDetails destinationDetails1 = PathAnalyzer.furthestReachableTile(path);
            if (NPCInteraction.isConversationWindowUp()) {
                NPCInteraction.handleConversation(NPCInteraction.GENERAL_RESPONSES);
            }
            if (destinationDetails1 != null) {
                if (!destinationDetails1.getDestination().equals(currentFurthest)){
                    return WaitFor.Return.SUCCESS;
                }
            }
            if (current.getNextTile() != null){
                PathAnalyzer.DestinationDetails hoverDetails = PathAnalyzer.furthestReachableTile(path, current.getNextTile());
                if (hoverDetails != null && hoverDetails.getDestination() != null && hoverDetails.getDestination().getTile().distanceTo(Players.local().tile()) > 7 && !strongholdDoor && Players.local().tile().distanceTo(object) <= 2){
                    WalkerEngine.getInstance().hoverMinimap(hoverDetails.getDestination());
                }
            }
            return WaitFor.Return.IGNORE;
        });
        if (strongholdDoor){
            General.sleep(800, 1200);
        }
        return result == WaitFor.Return.SUCCESS;
    }

    public static RSObject[] getInteractiveObjects(int x, int y, int z, PathAnalyzer.DestinationDetails destinationDetails){
        RSObject[] objects = Objects.getAll(15, interactiveObjectFilter(x, y, z, destinationDetails));
        final Tile base = new Tile(x, y, z);
        Arrays.sort(objects, (o1, o2) -> {
            int c = Integer.compare(o1.getPosition().distanceTo(base), o2.getPosition().distanceTo(base));
            int assumedZ = destinationDetails.getAssumedZ(), destinationZ = destinationDetails.getDestination().getZ();
            List<String> actions1 = Arrays.asList(o1.getDefinition().getActions());
            List<String> actions2 = Arrays.asList(o2.getDefinition().getActions());

            if (assumedZ > destinationZ){
                if (actions1.contains("Climb-up")){
                    return -1;
                }
                if (actions2.contains("Climb-up")){
                    return 1;
                }
            } else if (assumedZ < destinationZ){
                if (actions1.contains("Climb-down")){
                    return -1;
                }
                if (actions2.contains("Climb-down")){
                    return 1;
                }
            } else if(destinationDetails.getAssumed().distanceTo(destinationDetails.getDestination().getTile()) > 20){
                if(actions1.contains("Climb-up") || actions1.contains("Climb-down")){
                    return -1;
                } else if(actions2.contains("Climb-up") || actions2.contains("Climb-down")){
                    return 1;
                }
            }
//            else if(actions1.contains("Climb-up") || actions1.contains("Climb-down")){
//                return 1;
//            } else if(actions2.contains("Climb-up") || actions2.contains("Climb-down")){
//                return -1;
//            }
//            if(actions1.contains("Open")){
//                if(actions2.contains("Close")){
//                    return -1;
//                }
//            }
//            if(actions2.contains("Open")){
//                if(actions1.contains("Close")){
//                    return 1;
//                }
//            }
            return c;
        });
        StringBuilder a = new StringBuilder("Detected: ");
        Arrays.stream(objects).forEach(object -> a.append(object.getDefinition().getName()).append(" "));
        getInstance().log(a);



        return objects;
    }

    /**
     * Filter that accepts only interactive objects to progress in path.
     *
     * @param x
     * @param y
     * @param z
     * @param destinationDetails context where destination is at
     * @return
     */
    private static Predicate<RSObject> interactiveObjectFilter(int x, int y, int z, PathAnalyzer.DestinationDetails destinationDetails){
        final Tile position = new Tile(x, y, z);
        return new Predicate<RSObject>() {
            @Override
            public boolean test(RSObject rsObject) {
                RSObjectDefinition def = rsObject.getDefinition();
                if (def == null){
                    return false;
                }
                String name = def.getName();
                if (getInstance().sortedBlackList.contains(name)) {
                    return false;
                }
                List<String> actionsList = RSObjectHelper.getActionsList(rsObject);
                if (actionsList.stream().anyMatch(s -> s != null && getInstance().sortedBlackListOptions.contains(s))){
                    return false;
                }
                if (rsObject.getPosition().distanceTo(destinationDetails.getDestination().getTile()) > 5) {
                    return false;
                }
                if (Arrays.stream(rsObject.getAllTiles()).noneMatch(rsTile -> rsTile.distanceTo(position) <= 3)) {
                    return false;
                }
                List<String> options = Arrays.asList(def.getActions());
                return options.stream().anyMatch(getInstance().sortedOptions::contains);
            }
        };
    }

    private static String[] getViableOption(Collection<String> collection, PathAnalyzer.DestinationDetails destinationDetails){
        Set<String> set = new HashSet<>(collection);
        if (set.retainAll(getInstance().sortedHighPriorityOptions) && set.size() > 0){
            return set.toArray(new String[set.size()]);
        }
        if (destinationDetails.getAssumedZ() > destinationDetails.getDestination().getZ()){
            if (collection.contains("Climb-up")){
                return new String[]{"Climb-up"};
            }
        }
        if (destinationDetails.getAssumedZ() < destinationDetails.getDestination().getZ()){
            if (collection.contains("Climb-down")){
                return new String[]{"Climb-down"};
            }
        }
        if (destinationDetails.getAssumedY() > 5000 && destinationDetails.getDestination().getZ() == 0 && destinationDetails.getAssumedZ() == 0){
            if (collection.contains("Climb-down")){
                return new String[]{"Climb-down"};
            }
        }
        String[] options = new String[collection.size()];
        collection.toArray(options);
        return options;
    }

    private static boolean clickOnObject(RSObject object, String... options){
        boolean result;

        if (isClosedTrapDoor(object, options)){
            result = handleTrapDoor(object);
        } else {
            result = InteractionHelper.click(object, options);
            getInstance().log("Interacting with (" + RSObjectHelper.getName(object) + ") at " + object.getPosition() + " with options: " + Arrays.toString(options) + " " + (result ? "SUCCESS" : "FAIL"));
            WaitFor.milliseconds(250,800);
        }

        return result;
    }

    private static boolean isStrongholdDoor(RSObject object){
        List<String> doorNames = Arrays.asList("Gate of War", "Rickety door", "Oozing barrier", "Portal of Death");
        return  doorNames.contains(object.getDefinition().getName());
    }



    private static void handleStrongholdQuestions() {
        NPCInteraction.handleConversation("Use the Account Recovery System.",
                "Nobody.",
                "Don't tell them anything and click the 'Report Abuse' button.",
                "Me.",
                "Only on the RuneScape website.",
                "Report the incident and do not click any links.",
                "Authenticator and two-step login on my registered email.",
                "No way! You'll just take my gold for your own! Reported!",
                "No.",
                "Don't give them the information and send an 'Abuse Report'.",
                "Don't give them my password.",
                "The birthday of a famous person or event.",
                "Through account settings on runescape.com.",
                "Secure my device and reset my RuneScape password.",
                "Report the player for phishing.",
                "Don't click any links, forward the email to reportphishing@jagex.com.",
                "Inform Jagex by emailing reportphishing@jagex.com.",
                "Don't give out your password to anyone. Not even close friends.",
                "Politely tell them no and then use the 'Report Abuse' button.",
                "Set up 2 step authentication with my email provider.",
                "No, you should never buy a RuneScape account.",
                "Do not visit the website and report the player who messaged you.",
                "Only on the RuneScape website.",
                "Don't type in my password backwards and report the player.",
                "Virus scan my device then change my password.",
                "No, you should never allow anyone to level your account.",
                "Don't give out your password to anyone. Not even close friends.",
                "Report the stream as a scam. Real Jagex streams have a 'verified' mark.",
                "Read the text and follow the advice given.",
                "No way! I'm reporting you to Jagex!",
                "Talk to any banker in RuneScape.",
                "Secure my device and reset my RuneScape password.",
                "Don't share your information and report the player.");
    }

    private static boolean isClosedTrapDoor(RSObject object, String[] options){
        return  (object.getDefinition().getName().equals("Trapdoor") && Arrays.asList(options).contains("Open"));
    }

    private static boolean handleTrapDoor(RSObject object){
        if (getActions(object).contains("Open")){
            if (!InteractionHelper.click(object, "Open") && WaitFor.condition(8000, () -> {
                RSObject[] objects = Objects.find(15, Filters.Objects.actionsContains("Climb-down").and(Filters.Objects.inArea(new RSArea(object, 2))));
                if (objects.length > 0 && getActions(objects[0]).contains("Climb-down")){
                    return WaitFor.Return.SUCCESS;
                }
                return WaitFor.Return.IGNORE;
            }) == WaitFor.Return.SUCCESS){
                return false;
            } else {
                RSObject[] objects = Objects.find(15, Filters.Objects.actionsContains("Climb-down").and(Filters.Objects.inArea(new RSArea(object, 2))));
                return objects.length > 0 && handleTrapDoor(objects[0]);
            }
        }
        getInstance().log("Interacting with (" + object.getDefinition().getName() + ") at " + object.getPosition() + " with option: Climb-down");
        return InteractionHelper.click(object, "Climb-down");
    }

    public static List<String> getActions(RSObject object){
        List<String> list = new ArrayList<>();
        if (object == null){
            return list;
        }
        RSObjectDefinition objectDefinition = object.getDefinition();
        if (objectDefinition == null){
            return list;
        }
        String[] actions = objectDefinition.getActions();
        if (actions == null){
            return list;
        }
        return Arrays.asList(actions);
    }

    @Override
    public String getName() {
        return "Object Handler";
    }

    private static List<Integer> SLASH_WEAPONS = new ArrayList<>(Arrays.asList(1,4,9,10,12,17,20,21));

    private static boolean canLeftclickWeb(){
        RSVarBit weaponType = RSVarBit.get(357);
        return (weaponType != null && SLASH_WEAPONS.contains(weaponType.getValue())) || Inventory.find("Knife").length > 0;
    }
    private static boolean useBladeOnWeb(RSObject web){
        if(!Game.isUptext("->")){
            RSItem[] slashable = Inventory.find(Filters.Items.nameContains("whip", "sword", "dagger", "claws", "scimitar", " axe", "knife", "halberd", "machete", "rapier"));
            if(slashable.length == 0 || !slashable[0].click("Use"))
                return false;
        }
        return InteractionHelper.click(web, Game.getUptext());
    }

}
