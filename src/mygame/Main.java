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
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 *
 * @author normenhansen
 * @author willfeighner
 */
public class Main extends SimpleApplication implements PhysicsCollisionListener {

    // Fields
    private Node immobile;
    private Node mobile;
    private Spatial court;

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

    private BulletAppState bulletAppState;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }
    private boolean turnDisplayed = false;
    private boolean areBallsMoving = true;
    private BitmapText powerLabel;
    private Geometry powerMeter;
    private int redOddEven = 0;
    private int blueOddEven = 0;

    @Override
    public void simpleInitApp() {
        // init Physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        // bulletAppState.setDebugEnabled(inputEnabled);

        flyCam.setMoveSpeed(10);

        cam.setLocation(new Vector3f(-13f, 2f, 0f));
        cam.lookAt(new Vector3f(-0f, 0, 0), Vector3f.UNIT_Y);

        cam.setViewPort(0f, 0.8f, 0f, 1f);

        Camera cam2 = cam.clone();

        Quaternion yaw90 = new Quaternion();
        yaw90.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));

        cam2.resize(20, 100, true);
        cam2.setViewPort(0.8f, 1, 0, 1);
        cam2.setLocation(new Vector3f(6.9f, 17f, 0));
        // cam2.setRotation(new Quaternion(0,0.7071070192004544f,0,0.7071070192004544f));
        cam2.lookAt(new Vector3f(6.875f, 0f, 0f), Vector3f.UNIT_X);

        ViewPort view2 = renderManager.createMainView("Camera 2 view", cam2);
        view2.setClearFlags(true, true, true);
        view2.attachScene(rootNode);
        view2.setBackgroundColor(new ColorRGBA(0.5f, 0.8f, 1f, 1f));

        viewPort.setBackgroundColor(new ColorRGBA(0.5f, 0.8f, 1f, 1f));

        court = assetManager.loadModel("Models/bocce_extrude2/bocce_extrude2.j3o");

        CollisionShape courtShape = CollisionShapeFactory.createMeshShape(court);
        RigidBodyControl courtPhy = new RigidBodyControl(courtShape, 0.0f);
        courtPhy.setFriction(1.0f);
        //courtPhy.setRestitution(0.50f);
        court.addControl(courtPhy);

        bulletAppState.getPhysicsSpace().add(court);

        bulletAppState.getPhysicsSpace().addCollisionListener(this);

        immobile = new Node("immobile");
        mobile = new Node("mobile");

        immobile.attachChild(court);

        rootNode.attachChild(immobile);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(1.0f, -0.8f, -0f).normalizeLocal());
        rootNode.addLight(sun);

        initCrossHairs();
        initInputs();
    }

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
    };

    public Spatial makeBall(Vector3f position, Vector3f speed, ColorRGBA color, float radius, String name) {

        Sphere sphere = new Sphere(64, 64, radius, true, true);
        Spatial ball = new Geometry(name, sphere);
        Material ballMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        ballMat.setColor("Color", color);
        ball.setMaterial(ballMat);

        RigidBodyControl ballPhy = new RigidBodyControl(1.0f);
        ball.addControl(ballPhy);
        bulletAppState.getPhysicsSpace().add(ball);
        ballPhy.setFriction(1.0f);
        ballPhy.setPhysicsLocation(position);
        ballPhy.applyImpulse(speed, Vector3f.ZERO);
        //ballPhy.setRestitution(0.50f);
        //ballPhy.setLinearVelocity(new Vector3f(1f, 0f, 0f));
        mobile.attachChild(ball);
        rootNode.attachChild(mobile);
        ballPhy.setCcdMotionThreshold(0.0001f);
        ballPhy.setCcdSweptSphereRadius(radius / 8);
        return ball;
    }

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
        ballPhys.applyImpulse(speed, Vector3f.ZERO);
        //ballPhy.setRestitution(0.50f);
        //ballPhy.setLinearVelocity(new Vector3f(1f, 0f, 0f));
        mobile.attachChild(ball);
        rootNode.attachChild(mobile);
        ballPhys.setCcdMotionThreshold(0.0001f);
        ballPhys.setCcdSweptSphereRadius(radius / 8);
        return ball;
    }

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
        ballPhys.applyImpulse(speed, Vector3f.ZERO);
        //ballPhy.setRestitution(0.50f);
        //ballPhy.setLinearVelocity(new Vector3f(1f, 0f, 0f));
        mobile.attachChild(ball);
        rootNode.attachChild(mobile);
        ballPhys.setCcdMotionThreshold(0.0001f);
        ballPhys.setCcdSweptSphereRadius(radius / 8);
        return ball;
    }

    @Override
    public void simpleUpdate(float tpf) {
        //bindCamera();

        if (areBallsMoving()) {
            // Wait if balls are moving
        } else {

            if (mobile.hasChild(jack)) {
                jackPos = jack.getLocalTranslation();
                if (allBalls.isEmpty() && (jackPos.x > 11.75f || jackPos.x < 6.75f)) {
                    bulletAppState.getPhysicsSpace().remove(jack.getControl(RigidBodyControl.class));
                    mobile.detachChild(jack);
                    // jack = null;
                    message = "Jack must land\nin play area\nThrow again";
                }
            }

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

            allBalls.sort((Spatial o1, Spatial o2) -> {
                if (calculateDistance(jackPos, o1.getLocalTranslation()) < calculateDistance(jackPos, o2.getLocalTranslation())) {
                    return -1;
                } else {
                    return 1;
                }
            });

            nextTurn();
            if (allBalls.size() < 8 && ballThrown) {
                throwBall();
                ballThrown = false;
                turnDisplayed = false;
                powerLevel = 0;
            }

        }

        calcScore();

        if ((allBalls.size() == 8 && (redScore < 7 && blueScore < 7)) || (allBalls.size() == 8 && ((redScore >= 7 || blueScore >= 7) && (redScore == blueScore)))) {
            resetRound();
        }
        if (allBalls.size() == 8 && ((redScore >= 7 || blueScore >= 7) && (redScore != blueScore))) {
            endGame();
        }
        updateGUI();
    }

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

    private void calcScore() {
        if (allBalls.size() == 8) {
            for (Spatial ball : allBalls) {
                System.out.println(ball.getName());
            }
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
            System.out.println("Red Score: " + redScore);
            System.out.println("Blue Score: " + blueScore);

        }

    }

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

    @Override
    public void simpleRender(RenderManager rm
    ) {
        //TODO: add render code
    }

    @Override
    public void collision(PhysicsCollisionEvent event
    ) {
        // Vector3f output = null;
        float absorbEnergy = 0.95f;

        if (event.getNodeA().getParent().getName().equals("mobile")) {
            final Spatial node = event.getNodeA();

            RigidBodyControl nodeControl = node.getControl(RigidBodyControl.class
            );
            if (nodeControl.getLinearVelocity().length() < 0.01) {
                nodeControl.setLinearVelocity(Vector3f.ZERO);
                nodeControl.setAngularVelocity(Vector3f.ZERO);
                nodeControl.clearForces();

            } else {
                nodeControl.setAngularVelocity(nodeControl.getAngularVelocity().mult(absorbEnergy));

            }
        } else if (event.getNodeB().getParent().getName().equals("mobile")) {
            final Spatial node = event.getNodeA();
            RigidBodyControl nodeControl = node.getControl(RigidBodyControl.class
            );
            if (nodeControl.getLinearVelocity().length() < 0.01) {
                nodeControl.setLinearVelocity(Vector3f.ZERO);
                nodeControl.setAngularVelocity(Vector3f.ZERO);
                nodeControl.clearForces();
            } else {
                nodeControl.setAngularVelocity(nodeControl.getAngularVelocity().mult(absorbEnergy));
            }
        }

    }

    private float calculateDistance(Vector3f jackPos, Vector3f ballPos) {
        return jackPos.subtract(ballPos).length();
    }

    private Vector3f ballForce() {
        return cam.getDirection().mult(powerLevel);
    }

    private Vector3f ballPosition() {
        return cam.getLocation();
    }

    private void throwBall() {

        if (!mobile.hasChild(jack)) {
            jack = makeBall(ballPosition(), ballForce(), ColorRGBA.Yellow, 0.02f, "jack");
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

    private void endGame() {
        Scanner scanner = new Scanner(System.in);

        message = "Game Over";
        if (redScore > blueScore) {
            message += "\nRed Wins";
        } else {
            message += "\nBlue Wins";
        }

        message += "\nPlay Again? (Y/N)";
        String choice = scanner.nextLine();
        if (choice.startsWith("y") || choice.startsWith("Y")) {
            resetRound();
            blueScore = 0;
            redScore = 0;
        } else {
            System.exit(0);
        }
    }

    private void initCrossHairs() {
        setDisplayStatView(false);
        //guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+");        // fake crosshairs :)
        ch.setLocalTranslation( // center
                settings.getWidth() * 0.4f,
                settings.getHeight() / 2, 0);
        guiNode.attachChild(ch);

        scoreGUI = new BitmapText(guiFont);
        scoreGUI.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        scoreGUI.setText("Red: 0\nBlue: 0\n" + nextTurn);
        scoreGUI.setLocalTranslation(settings.getWidth() * 0.05f,
                settings.getHeight() * 0.95f, 0);
        scoreGUI.setColor(ColorRGBA.DarkGray);
        guiNode.attachChild(scoreGUI);

        powerLabel = new BitmapText(guiFont);
        powerLabel.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        powerLabel.setText("Power Level: " + powerLevel);
        powerLabel.setLocalTranslation(settings.getWidth() * 0.05f,
                settings.getHeight() * 0.05f, 0);
        powerLabel.setColor(ColorRGBA.DarkGray);
        guiNode.attachChild(powerLabel);

    }

    private void updateGUI() {
        scoreGUI.setText("Red: " + redScore + "\nBlue: " + blueScore + "\n" + nextTurn + "\n" + message);
        powerLabel.setText("Power Level: " + (int) powerLevel);

        if (guiNode.hasChild(powerMeter)) {
            guiNode.detachChild(powerMeter);
        }

        Quad powerMeterBox = new Quad(50f, 200f);
        powerMeter = new Geometry("PowerMeter", powerMeterBox);
        powerMeter.scale(1, powerLevel / 30, 0);

        Material meterMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        meterMat.setColor("Color", ColorRGBA.Red);
        powerMeter.setMaterial(meterMat);
        powerMeter.setLocalTranslation(settings.getWidth() * 0.05f,
                settings.getHeight() * 0.15f, 0);

        guiNode.attachChild(powerMeter);
    }

    private void initInputs() {
        inputManager.addMapping("shoot",
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "shoot");

        inputManager.addMapping("PowerUp", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("PowerDown", new KeyTrigger(KeyInput.KEY_F));

        /* Add the named mappings to the action listeners. */
        inputManager.addListener(actionListener, "PowerUp", "PowerDown");
    }

}
