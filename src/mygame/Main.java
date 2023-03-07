package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

import java.util.ArrayList;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 *
 * @author normenhansen
 * @author willfeighner
 *
 * This program simulates a Bocce Ball game. Use WASD to move camera. Press R/F
 * to increase/decrease throw power level. Left-click mouse to throw ball. At
 * end of game press Y to restart, press N to exit game.
 *
 * The object of the game is to throw your team's balls as close as possible to
 * the target ball, or jack. The balls and jack can collide and move. The jack
 * is thrown first and must land in the play area between the lines on the far
 * side of the court. After each throw, the team whose ball is not closest to
 * the jack throws next. After both teams have thrown all four balls each, the
 * score is calculated. The team whose ball is closest to the jack gets one
 * point for the first ball and one additional point for each of their balls
 * that is closer to the jack then the opponents first closest ball. The round
 * resets and the game continues until one team gets 7 or more points.
 */
public class Main extends SimpleApplication implements PhysicsCollisionListener {

    // Feilds
    Spatial[] blueBalls = new Spatial[4];
    Spatial[] redBalls = new Spatial[4];
    Spatial jack;
    Vector3f jackPos = Vector3f.ZERO;
    int redScore = 0;
    int blueScore = 0;
    BitmapText scoreGUI;
    String nextTurn = "";
    String message = "";
    float powerLevel = 0f;
    ArrayList<Spatial> allBalls = new ArrayList<>();
    boolean ballThrown = false;
    boolean gameOver = false;
    private Node immobile;
    private Node mobile;
    private Spatial court;
    private BulletAppState bulletAppState;
    private boolean turnDisplayed = false;
    private boolean areBallsMoving = true;

    final private ActionListener actionListener = (String name, boolean keyPressed, float tpf) -> {
        if (name.equals("shoot") && !keyPressed && !areBallsMoving) {
            ballThrown = true;
            message = "";
        }
        if (name.equals("PowerUp") && !keyPressed) {
            powerLevel++;
            if (powerLevel >= 30) {
                powerLevel = 30;
            }
        }
        if (name.equals("PowerDown") && !keyPressed) {
            powerLevel--;
            if (powerLevel <= 0) {
                powerLevel = 0;
            }
        }
        if (name.equals("Yes") && !keyPressed && gameOver) {
            gameOver = false;
            newGame();
        }
        if (name.equals("No") && !keyPressed && gameOver) {
            System.exit(0);
        }
    };

    private BitmapText powerLabel;
    private Geometry powerMeter;
    private int redOddEven = 0;
    private int blueOddEven = 0;

    /**
     * Main method for Game
     *
     * @param args
     */
    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    /**
     * Initializer for the Bocce Ball game
     */
    @Override
    public void simpleInitApp() {
        // init Physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        // Set up cameras and viewports
        flyCam.setMoveSpeed(5);
        flyCam.setRotationSpeed(0.5f);

        cam.setLocation(new Vector3f(-13f, 2f, 0f));
        cam.lookAt(new Vector3f(-0f, 0, 0), Vector3f.UNIT_Y);
        cam.setViewPort(0f, 0.8f, 0f, 1f);
        viewPort.setBackgroundColor(new ColorRGBA(0.5f, 0.8f, 1f, 1f));

        Camera cam2 = cam.clone();

        cam2.resize(20, 100, true);
        cam2.setViewPort(0.8f, 1, 0, 1);
        cam2.setLocation(new Vector3f(6.9f, 17f, 0));
        cam2.lookAt(new Vector3f(6.875f, 0f, 0f), Vector3f.UNIT_X);

        ViewPort view2 = renderManager.createMainView("Camera 2 view", cam2);
        view2.setClearFlags(true, true, true);
        view2.attachScene(rootNode);
        view2.setBackgroundColor(new ColorRGBA(0.5f, 0.8f, 1f, 1f));

        // Load court model and add physics
        court = assetManager.loadModel("Models/bocce_extrude2/bocce_extrude2.j3o");
        CollisionShape courtShape = CollisionShapeFactory.createMeshShape(court);
        RigidBodyControl courtPhy = new RigidBodyControl(courtShape, 0.0f);
        courtPhy.setFriction(1.0f);
        court.addControl(courtPhy);

        bulletAppState.getPhysicsSpace().add(court);
        bulletAppState.getPhysicsSpace().addCollisionListener(this);

        // Scene graph nodes
        immobile = new Node("immobile");
        mobile = new Node("mobile");

        immobile.attachChild(court);
        rootNode.attachChild(immobile);

        // Add sun lighting
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(1.0f, -0.8f, -0f).normalizeLocal());
        rootNode.addLight(sun);

        // Initilize inputs and HUD
        initCrossHairs();
        initInputs();
    }

    /**
     * Creates a ball Spatial, adds physics, sets position and initial velocity
     * (i.e. throwing ball)
     *
     * @param position Vector3f - position in game, in meters
     * @param speed Vector3f - initial velocity in meters/sec
     * @param radius float - radius of ball to be created in meters
     * @param name String - name of ball
     * @return Spatial - ball with physics, intial position and velocity (thrown
     * ball)
     */
    public Spatial makeBall(Vector3f position, Vector3f speed, float radius, String name) {

        Spatial ball = assetManager.loadModel("Models/jackBall/jackBall.j3o");
        ball.setName(name);
        SphereCollisionShape ballShape = new SphereCollisionShape(radius);
        RigidBodyControl ballPhy = new RigidBodyControl(ballShape, 0.1f);
        ball.addControl(ballPhy);
        bulletAppState.getPhysicsSpace().add(ball);
        ballPhy.setFriction(1.0f);
        ballPhy.setPhysicsLocation(position);
        ballPhy.setLinearVelocity(speed);
        //ballPhy.setRestitution(0.50f);
        //ballPhy.setLinearVelocity(new Vector3f(1f, 0f, 0f));
        mobile.attachChild(ball);
        rootNode.attachChild(mobile);
        ballPhy.setCcdMotionThreshold(0.0001f);
        ballPhy.setCcdSweptSphereRadius(radius / 8);
        return ball;
    }

    /**
     * Creates red bocce ball, with one of two models adds physics, sets
     * position and initial velocity (i.e. throwing ball)
     *
     * @param position Vector3f - position in game, in meters
     * @param speed Vector3f - initial velocity in meters/sec
     * @param radius float - radius of ball to be created in meters
     * @param name String - name of ball
     * @return Spatial - ball with physics, intial position and velocity (thrown
     * ball)
     */
    public Spatial makeRedBall(Vector3f position, Vector3f speed, float radius, String name) {
        Spatial ball;

        if (redOddEven % 2 == 0) {
            ball = assetManager.loadModel("Models/redBocceBallCircle/redBocceBallCircle.j3o");
        } else {
            ball = assetManager.loadModel("Models/redBocceBallStripe/redBocceBallStripe.j3o");
        }

        redOddEven++;

        ball.setName(name);
        SphereCollisionShape ballShape = new SphereCollisionShape(radius);
        RigidBodyControl ballPhys = new RigidBodyControl(ballShape, 1.0f);
        ball.addControl(ballPhys);
        bulletAppState.getPhysicsSpace().add(ball);

        ballPhys.setFriction(1.0f);
        ballPhys.setPhysicsLocation(position);
        ballPhys.setLinearVelocity(speed);
        mobile.attachChild(ball);

        rootNode.attachChild(mobile);
        ballPhys.setCcdMotionThreshold(0.0001f);
        ballPhys.setCcdSweptSphereRadius(radius / 8);
        return ball;
    }

    /**
     * Creates blue bocce ball, with one of two models adds physics, sets
     * position and initial velocity (i.e. throwing ball)
     *
     * @param position Vector3f - position in game, in meters
     * @param speed Vector3f - initial velocity in meters/sec
     * @param radius float - radius of ball to be created in meters
     * @param name String - name of ball
     * @return Spatial - ball with physics, intial position and velocity (thrown
     * ball)
     */
    public Spatial makeBlueBall(Vector3f position, Vector3f speed, float radius, String name) {
        Spatial ball;

        if (blueOddEven % 2 == 0) {
            ball = assetManager.loadModel("Models/blueBocceBallCircle/blueBocceBallCircle.j3o");
        } else {
            ball = assetManager.loadModel("Models/blueBocceBallStripe/blueBocceBallStripe.j3o");
        }

        blueOddEven++;

        ball.setName(name);
        SphereCollisionShape ballShape = new SphereCollisionShape(radius);

        RigidBodyControl ballPhys = new RigidBodyControl(ballShape, 1.0f);
        ball.addControl(ballPhys);
        bulletAppState.getPhysicsSpace().add(ball);
        ballPhys.setFriction(1.0f);
        ballPhys.setPhysicsLocation(position);
        ballPhys.setLinearVelocity(speed);
        mobile.attachChild(ball);

        rootNode.attachChild(mobile);
        ballPhys.setCcdMotionThreshold(0.0001f);
        ballPhys.setCcdSweptSphereRadius(radius / 8);
        return ball;
    }

    /**
     * Main update loop, runs continuosly to run game
     *
     * @param tpf float - time per frame (tick)
     */
    @Override
    public void simpleUpdate(float tpf) {
        // Restrict Camera/ball throwing to near end of court
        bindCamera();

        if (areBallsMoving()) {
            // Wait if balls are moving
        } else {
            // Test if jack landed in playing area
            if (mobile.hasChild(jack)) {
                jackPos = jack.getLocalTranslation();
                if (allBalls.isEmpty() && (jackPos.x > 11.75f || jackPos.x < 6.75f)) {
                    bulletAppState.getPhysicsSpace().remove(jack.getControl(RigidBodyControl.class));
                    mobile.detachChild(jack);
                    message = "Jack must land\nin play area\nThrow again";
                }
            }
            // Add all balls in play to Arraylist for calculating distances from jack
            for (Spatial blueBall : blueBalls) {
                if (blueBall != null && !allBalls.contains(blueBall)) {
                    allBalls.add(blueBall);
                }
            }
            for (Spatial redBall : redBalls) {
                if (redBall != null && !allBalls.contains(redBall)) {
                    allBalls.add(redBall);
                }
            }

            // Sort list based on distance from jack, nearest is first
            allBalls.sort((Spatial o1, Spatial o2) -> {
                if (calculateDistance(jackPos, o1.getLocalTranslation())
                        < calculateDistance(jackPos, o2.getLocalTranslation())) {
                    return -1;
                } else {
                    return 1;
                }
            });

            // Team not closest to jack throws next, display next team turn
            nextTurn();

            // If no balls are moving, throw next ball
            if (allBalls.size() < 8 && ballThrown) {
                throwBall();
                ballThrown = false;
                turnDisplayed = false;
                powerLevel = 0;
            }
        }

        /* 
        Calculate score, The team closest to the jack gets
        one point for each ball closer the jack the the 
        opponents first closest ball.
         */
        if (!gameOver) {
            calcScore();
        }

        // After all balls are thrown for each round, set up next round or end game, depending on score
        if ((allBalls.size() == 8 && (redScore < 7 && blueScore < 7)) || (allBalls.size() == 8 && ((redScore >= 7 || blueScore >= 7) && (redScore == blueScore)))) {
            resetRound();
        }
        if (allBalls.size() == 8 && ((redScore >= 7 || blueScore >= 7) && (redScore != blueScore))) {
            gameOver = true;
            message = "Game Over";
            if (redScore > blueScore) {
                message += "\nRed Wins";
            } else {
                message += "\nBlue Wins";
            }
            nextTurn = "";
            message += "\nPlay Again? (Y/N)";
        }
        // Update the GUI
        updateGUI();
    }

    /**
     * Determine who throws next. Team not closest to jack throws next unless
     * they have no more balls to throw
     */
    private void nextTurn() {
        if (!allBalls.isEmpty() && !turnDisplayed) {
            if (allBalls.get(0).getName().contains("red")) {
                if (blueBalls[3] != null) {
                    nextTurn = "Red's Turn";
                } else {
                    nextTurn = "Blue's Turn";
                }
            } else if (allBalls.get(0).getName().contains("blue")) {
                if (redBalls[3] != null) {
                    nextTurn = "Blue's Turn";
                } else {
                    nextTurn = "Red's Turn";
                }

            }
            turnDisplayed = true;
        }
        if (allBalls.isEmpty() && !turnDisplayed && mobile.hasChild(jack)) {
            nextTurn = "Red's Turn";
            turnDisplayed = true;
        }
        if (allBalls.isEmpty() && !turnDisplayed && !mobile.hasChild(jack)) {
            nextTurn = "Throw Jack";
            turnDisplayed = true;
        }
    }

    /**
     * Restrict Camera/throwing position to near end of court
     */
    private void bindCamera() {
        if (cam.getLocation().y < 1.5) {
            cam.setLocation(new Vector3f(cam.getLocation().x, 1.5f, cam.getLocation().z));
        }
        if (cam.getLocation().y > 2) {
            cam.setLocation(new Vector3f(cam.getLocation().x, 2f, cam.getLocation().z));
        }
        if (cam.getLocation().x > -6.75) {
            cam.setLocation(new Vector3f(-6.75f, cam.getLocation().y, cam.getLocation().z));
        }
        if (cam.getLocation().x < -13.75) {
            cam.setLocation(new Vector3f(-13.75f, cam.getLocation().y, cam.getLocation().z));
        }
        if (cam.getLocation().z < -2) {
            cam.setLocation(new Vector3f(cam.getLocation().x, cam.getLocation().y, -2f));
        }
        if (cam.getLocation().z > 2) {
            cam.setLocation(new Vector3f(cam.getLocation().x, cam.getLocation().y, 2f));
        }
    }

    /**
     * Calculate score from list of all balls sorted by distance Determine ball
     * closest to jack, loop through list, increment that teams score until the
     * first opponent ball encoutered
     */
    private void calcScore() {
        if (allBalls.size() == 8) {
            if (allBalls.get(0).getName().contains("red")) {
                redScore++;
                for (int i = 1; i < allBalls.size() - 1; i++) {
                    if (allBalls.get(i).getName().contains("red")) {
                        redScore++;
                    } else {
                        break;
                    }
                }
            } else {
                blueScore++;
                for (int i = 1; i < allBalls.size() - 1; i++) {
                    if (allBalls.get(i).getName().contains("blue")) {
                        blueScore++;
                    } else {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Determine if balls are moving All game logic skipped until balls have
     * stopped moving. Check all balls and jacks linear Velocity. If not zero
     * set areBallsMoving to true. If any balls are moving and have a negative
     * position in the Y axis, they have fallen outside the court/play area and
     * must be removed from the game and thrown again.
     *
     * @return boolean - if any balls are moving return true
     */
    public boolean areBallsMoving() {
        areBallsMoving = false;
        RigidBodyControl ballCheck;

        if (mobile.hasChild(jack)) {
            RigidBodyControl jackCheck = jack.getControl(RigidBodyControl.class);
            if (!jackCheck.getLinearVelocity().equals(Vector3f.ZERO)) {
                areBallsMoving = true;
            }
            if (jack.getLocalTranslation().y < 0) {
                bulletAppState.getPhysicsSpace().remove(jack.getControl(RigidBodyControl.class));
                mobile.detachChild(jack);
                message = "Out of Bounds\nThrow again";
            }
        }

        for (int i = 0; i < blueBalls.length; i++) {
            if (blueBalls[i] != null) {
                ballCheck = blueBalls[i].getControl(RigidBodyControl.class);
                if (!ballCheck.getLinearVelocity().equals(Vector3f.ZERO)) {
                    areBallsMoving = true;
                }
                if (blueBalls[i].getLocalTranslation().y < 0) {
                    bulletAppState.getPhysicsSpace().remove(blueBalls[i]);
                    blueBalls[i].removeFromParent();
                    blueBalls[i] = null;
                    message = "Out of Bounds\nThrow again";
                }
            }
        }

        for (int i = 0; i < redBalls.length; i++) {
            if (redBalls[i] != null) {
                ballCheck = redBalls[i].getControl(RigidBodyControl.class);
                if (!ballCheck.getLinearVelocity().equals(Vector3f.ZERO)) {
                    areBallsMoving = true;
                }
                if (redBalls[i].getLocalTranslation().y < 0) {
                    bulletAppState.getPhysicsSpace().remove(redBalls[i]);
                    redBalls[i].removeFromParent();
                    redBalls[i] = null;
                    message = "Out of Bounds\nThrow again";
                }
            }
        }
        return areBallsMoving;
    }

    /**
     * Deals with collisions between balls and balls/court. Slows down balls by
     * simulating friction. Sets linear and angular velcocities to Zero at very
     * low speeds.
     *
     * @param event PhysicsCollisionEvent - from Collisionlistener
     */
    @Override
    public void collision(PhysicsCollisionEvent event) {

        float absorbEnergy = 0.95f;
        if (event.getNodeA().hasAncestor(mobile) || event.getNodeB().hasAncestor(mobile)) {
            if (event.getNodeA().getParent().getName().equals("mobile")) {
                final Spatial node = event.getNodeA();
                RigidBodyControl nodeControl = node.getControl(RigidBodyControl.class);
                if (nodeControl.getLinearVelocity().length() < 0.01) {
                    nodeControl.setLinearVelocity(Vector3f.ZERO);
                    nodeControl.setAngularVelocity(Vector3f.ZERO);
                    nodeControl.clearForces();
                } else {
                    nodeControl.setAngularVelocity(nodeControl.getAngularVelocity().mult(absorbEnergy));
                }
            } else if (event.getNodeB().getParent().getName().equals("mobile")) {
                final Spatial node = event.getNodeB();
                RigidBodyControl nodeControl = node.getControl(RigidBodyControl.class);
                if (nodeControl.getLinearVelocity().length() < 0.01) {
                    nodeControl.setLinearVelocity(Vector3f.ZERO);
                    nodeControl.setAngularVelocity(Vector3f.ZERO);
                    nodeControl.clearForces();
                } else {
                    nodeControl.setAngularVelocity(nodeControl.getAngularVelocity().mult(absorbEnergy));
                }
            }
        }
    }

    // Calculates distance between jack and provided ball
    private float calculateDistance(Vector3f jackPos, Vector3f ballPos) {
        return jackPos.subtract(ballPos).length();
    }

    // Set thrown ball initial speed from power level
    private Vector3f ballForce() {
        return cam.getDirection().mult(powerLevel);
    }

    // Positions ball at camera position for throwing
    private Vector3f ballPosition() {
        return cam.getLocation();
    }

    // Creates and throws ball
    private void throwBall() {

        if (!mobile.hasChild(jack)) {
            jack = makeBall(ballPosition(), ballForce(), 0.02f, "jack");
            return;
        }
        if (allBalls.isEmpty()) {
            redBalls[0] = makeRedBall(ballPosition(), ballForce(), 0.0535f, "red0");
            return;
        }
        if (allBalls.get(0).getName().contains("red") || redBalls[3] != null) {
            for (int i = 0; i < blueBalls.length; i++) {
                if (blueBalls[i] == null) {
                    String name = "blue" + i;
                    blueBalls[i] = makeBlueBall(ballPosition(), ballForce(), 0.0535f, name);
                    return;
                }
            }
        }
        if (allBalls.get(0).getName().contains("blue") || blueBalls[3] != null) {
            for (int i = 0; i < redBalls.length; i++) {
                if (redBalls[i] == null) {
                    String name = "red" + i;
                    redBalls[i] = makeRedBall(ballPosition(), ballForce(), 0.0535f, name);
                    return;
                }
            }
        }
        ballThrown = true;
    }

    // Resets round, clears balls
    private void resetRound() {
        mobile.detachChild(jack);
        bulletAppState.getPhysicsSpace().remove(jack);
        jack = null;

        for (int i = 0; i < blueBalls.length; i++) {
            if (blueBalls[i] != null) {
                bulletAppState.getPhysicsSpace().remove(blueBalls[i]);
                blueBalls[i].removeFromParent();
                blueBalls[i] = null;
            }
        }
        for (int i = 0; i < redBalls.length; i++) {
            if (redBalls[i] != null) {
                bulletAppState.getPhysicsSpace().remove(redBalls[i]);
                redBalls[i].removeFromParent();
                redBalls[i] = null;
            }
        }
        allBalls.clear();
        message = "Next Round\nThrow Jack";
        nextTurn = "";
        redOddEven = 0;
        blueOddEven = 0;
    }

    // Resets game, clears all balls, scores, messages
    private void newGame() {
        bulletAppState.getPhysicsSpace().remove(jack);
        jack = null;
        for (int i = 0; i < blueBalls.length; i++) {
            if (blueBalls[i] != null) {
                bulletAppState.getPhysicsSpace().remove(blueBalls[i]);
                blueBalls[i].removeFromParent();
                blueBalls[i] = null;
            }
        }
        for (int i = 0; i < redBalls.length; i++) {
            if (redBalls[i] != null) {
                bulletAppState.getPhysicsSpace().remove(redBalls[i]);
                redBalls[i].removeFromParent();
                redBalls[i] = null;
            }
        }
        mobile.detachAllChildren();
        allBalls.clear();
        message = "Throw Jack";
        nextTurn = "";
        redOddEven = 0;
        blueOddEven = 0;
        redScore = 0;
        blueScore = 0;
    }

    // Initilize GUI
    private void initCrossHairs() {
        setDisplayStatView(false);
        BitmapText ch = new BitmapText(guiFont);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+");
        ch.setLocalTranslation(settings.getWidth() * 0.4f, settings.getHeight() / 2, 0);
        guiNode.attachChild(ch);

        scoreGUI = new BitmapText(guiFont);
        scoreGUI.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        scoreGUI.setText("Red: 0\nBlue: 0\n" + nextTurn);
        scoreGUI.setLocalTranslation(settings.getWidth() * 0.05f, settings.getHeight() * 0.95f, 0);
        scoreGUI.setColor(ColorRGBA.Black);
        guiNode.attachChild(scoreGUI);

        powerLabel = new BitmapText(guiFont);
        powerLabel.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        powerLabel.setText("Power Level (R/F): " + powerLevel);
        powerLabel.setLocalTranslation(settings.getWidth() * 0.05f, settings.getHeight() * 0.05f, 0);
        powerLabel.setColor(ColorRGBA.Black);
        guiNode.attachChild(powerLabel);
    }

    // Updates GUI, displays power level for throw in number and bar
    private void updateGUI() {
        scoreGUI.setText("Red: " + redScore + "\nBlue: " + blueScore + "\n" + nextTurn + "\n" + message);
        powerLabel.setText("Power Level (R/F): " + (int) powerLevel);

        if (guiNode.hasChild(powerMeter)) {
            guiNode.detachChild(powerMeter);
        }

        Quad powerMeterBox = new Quad(50f, 200f);
        powerMeter = new Geometry("PowerMeter", powerMeterBox);
        powerMeter.scale(1, powerLevel / 30, 0);

        Material meterMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        meterMat.setColor("Color", ColorRGBA.Red);
        powerMeter.setMaterial(meterMat);
        powerMeter.setLocalTranslation(settings.getWidth() * 0.05f, settings.getHeight() * 0.15f, 0);

        guiNode.attachChild(powerMeter);
    }

    // Initalize input button mapping
    private void initInputs() {
        inputManager.addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "shoot");

        inputManager.addMapping("PowerUp", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("PowerDown", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("Yes", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("No", new KeyTrigger(KeyInput.KEY_N));

        /* Add the named mappings to the action listeners. */
        inputManager.addListener(actionListener, "PowerUp", "PowerDown", "Yes", "No");
    }
}
