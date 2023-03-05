package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.shape.Sphere;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 *
 * @author normenhansen
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

    ArrayList<Spatial> allBalls = new ArrayList<>();

    boolean ballThrown = false;

    private BulletAppState bulletAppState;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }
    private boolean turnDisplayed = false;
    private boolean areBallsMoving = true;

    @Override
    public void simpleInitApp() {
        // init Physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        //bulletAppState.setDebugEnabled(inputEnabled);

        flyCam.setMoveSpeed(15);
        cam.setLocation(new Vector3f(-17f, 2f, 0f));
        cam.lookAt(new Vector3f(-0f, 0, 0), Vector3f.UNIT_Y);

        cam.setViewPort(0f, 0.8f, 0f, 1f);

        Camera cam2 = cam.clone();

        Quaternion yaw90 = new Quaternion();
        yaw90.fromAngleAxis(FastMath.PI / 2, new Vector3f(0, 1, 0));

        cam2.resize(20, 100, true);
        cam2.setViewPort(0.8f, 1, 0, 1);
        cam2.setLocation(new Vector3f(6.875f, 17f, 0));
        // cam2.setRotation(new Quaternion(0,0.7071070192004544f,0,0.7071070192004544f));
        cam2.lookAt(new Vector3f(6.875f, 0f, 0f), Vector3f.UNIT_X);

        ViewPort view2 = renderManager.createMainView("Camera 2 view", cam2);
        view2.setClearFlags(true, true, true);
        view2.attachScene(rootNode);
        view2.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));

        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));

        court = assetManager.loadModel("Models/bocce_extrude/bocce_extrude.j3o");

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
        sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f).normalizeLocal());
        rootNode.addLight(sun);

        initCrossHairs();
        initInputs();
    }

    final private ActionListener actionListener = (String name, boolean keyPressed, float tpf) -> {
        if (name.equals("shoot") && !keyPressed && !areBallsMoving) {
            ballThrown = true;
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
        ballPhy.setCcdMotionThreshold(0.1f);
        ballPhy.setCcdSweptSphereRadius(radius / 4);
        return ball;
    }

    @Override
    public void simpleUpdate(float tpf) {

        if (areBallsMoving()) {
            // Wait if balls are moving
        } else {

            if (mobile.hasChild(jack)) {
                jackPos = jack.getLocalTranslation();
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

            if (!allBalls.isEmpty() && !turnDisplayed) {
                if (allBalls.get(0).getName().contains("red") && redBalls[3] == null) {
                    nextTurn = "Blue's Turn";
                } else {
                    nextTurn = "Red's Turn";
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
            if (allBalls.size() < 8 && ballThrown) {
                throwBall();
                ballThrown = false;
                turnDisplayed = false;
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
                // jack = null;
                System.out.println("Out of Bounds Rethrowing");
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
                    System.out.println("Out of Bounds Rethrowing");
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
                    System.out.println("Out of Bounds Rethrowing");
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
        return cam.getDirection().mult(25);
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
            redBalls[0] = redBalls[0] = makeBall(ballPosition(), ballForce(), ColorRGBA.Red, 0.0535f, "red0");
            return;
        }
        if (allBalls.get(0).getName().contains("red") || redBalls[3] != null) {
            for (int i = 0; i < blueBalls.length; i++) {
                if (blueBalls[i] == null) {
                    String name = "blue" + i;
                    blueBalls[i] = makeBall(ballPosition(), ballForce(), ColorRGBA.Blue, 0.0535f, name);
                    return;
                }
            }
        }
        if (allBalls.get(0).getName().contains("blue") || blueBalls[3] != null) {
            for (int i = 0; i < redBalls.length; i++) {
                if (redBalls[i] == null) {
                    String name = "red" + i;
                    redBalls[i] = makeBall(ballPosition(), ballForce(), ColorRGBA.Red, 0.0535f, name);
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
    }

    private void endGame() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Game Over");
        if (redScore > blueScore) {
            System.out.println("Red Wins");
        } else {
            System.out.println("Blue Wins");
        }
        System.out.println("Red: " + redScore + " - " + "Blue: " + blueScore);
        System.out.println("Play Again? : ");
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
        guiNode.attachChild(scoreGUI);
    }

    private void updateGUI() {
        scoreGUI.setText("Red: " + redScore + "\nBlue: " + blueScore + "\n" + nextTurn);
    }

    private void initInputs() {
        inputManager.addMapping("shoot",
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "shoot");
    }
}
