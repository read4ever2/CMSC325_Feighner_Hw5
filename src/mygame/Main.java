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
import com.jme3.light.DirectionalLight;
import com.jme3.math.Vector3f;
import com.jme3.scene.shape.Sphere;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Random;

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

    ArrayList<Spatial> allBalls = new ArrayList<>();
    Spatial[] ballOrder = new Spatial[8];

    Random random = new Random();

    private BulletAppState bulletAppState;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // init Physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.setDebugEnabled(inputEnabled);

        flyCam.setMoveSpeed(15);
        cam.setLocation(new Vector3f(-12f, 1f, 4f));
        cam.lookAt(new Vector3f(-0f, 0, 0), Vector3f.UNIT_Y);

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

        Random random = new Random();

        //Vector3f randForce = new Vector3f(17.5f + randomFloat, -0.5f + randomFloat, 0f + randomFloat);
        /*
        jack = makeBall(new Vector3f(-12, 1, 0f), new Vector3f(17.5f + random.nextFloat(), -0.5f + random.nextFloat(), 0f + random.nextFloat()), ColorRGBA.Yellow, 0.02f, "jack");
        blueBalls[0] = makeBall(new Vector3f(-12, 1, -0.5f), new Vector3f(17.5f + random.nextFloat(), -0.5f + random.nextFloat(), 0f + random.nextFloat()), ColorRGBA.Blue, 0.0535f, "blue0");
        redBalls[0] = makeBall(new Vector3f(-12, 1, 0.5f), new Vector3f(17.5f + random.nextFloat(), -0.5f + random.nextFloat(), 0f + random.nextFloat()), ColorRGBA.Red, 0.0535f, "red0");
        blueBalls[1] = makeBall(new Vector3f(-12, 1, -0.25f), new Vector3f(17.5f + random.nextFloat(), -0.5f + random.nextFloat(), 0f + random.nextFloat()), ColorRGBA.Blue, 0.0535f, "blue1");
        redBalls[1] = makeBall(new Vector3f(-12, 1, 0.25f), new Vector3f(17.5f + random.nextFloat(), -0.5f + random.nextFloat(), 0f + random.nextFloat()), ColorRGBA.Red, 0.0535f, "red1");
        blueBalls[2] = makeBall(new Vector3f(-12, 1, -0.15f), new Vector3f(17.5f + random.nextFloat(), -0.5f + random.nextFloat(), 0f + random.nextFloat()), ColorRGBA.Blue, 0.0535f, "blue2");
        redBalls[2] = makeBall(new Vector3f(-12, 1, 0.15f), new Vector3f(17.5f + random.nextFloat(), -0.5f + random.nextFloat(), 0f + random.nextFloat()), ColorRGBA.Red, 0.0535f, "red2");
        blueBalls[3] = makeBall(new Vector3f(-12, 1, -0.35f), new Vector3f(17.5f + random.nextFloat(), -0.5f + random.nextFloat(), 0f + random.nextFloat()), ColorRGBA.Blue, 0.0535f, "blue3");
        redBalls[3] = makeBall(new Vector3f(-12, 1, 0.35f), new Vector3f(17.5f + random.nextFloat(), -0.5f + random.nextFloat(), 0f + random.nextFloat()), ColorRGBA.Red, 0.0535f, "red3");
         */
    }

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
            System.out.println("Moving");
        } else {
            // System.out.println("Stopped");

            //Vector3f jackPos = Vector3f.ZERO;
            if (mobile.hasChild(jack)) {
                jackPos = jack.getLocalTranslation();
            }

            for (Spatial blueBall : blueBalls) {
                if (blueBall != null && !allBalls.contains(blueBall)) {
                    allBalls.add(blueBall);

                    /*
                    ballPos = blueBall.getLocalTranslation();
                    System.out.println(calculateDistance(jackPos, ballPos));
                    for (int i = 0; i < allBalls.length; i++) {
                        if (allBalls[i] == blueBall) {
                            break;
                        } else if (allBalls[i] == null) {
                            allBalls[i] = blueBall;
                            break;
                        }

                    }
                     */
                }
            }
            for (Spatial redBall : redBalls) {
                if (redBall != null && !allBalls.contains(redBall)) {
                    allBalls.add(redBall);

                    /*
                    //ballPos = blueBall.getLocalTranslation();
                    //System.out.println(calculateDistance(jackPos, ballPos));
                    for (int i = 0; i < allBalls.length; i++) {
                        if (allBalls[i] == redBall) {
                            break;
                        } else if (allBalls[i] == null) {
                            allBalls[i] = redBall;
                            break;
                        }

                    }
                     */
                }
            }

            /*
            for (Spatial ball : allBalls) {
                System.out.println(ball.getName());
            }
             */
            allBalls.sort(new Comparator<Spatial>() {
                @Override
                public int compare(Spatial o1, Spatial o2) {
                    if (calculateDistance(jackPos, o1.getLocalTranslation()) < calculateDistance(jackPos, o2.getLocalTranslation())) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });

            if (allBalls.size() < 8) {
                throwBall();
            }

        }

        /*
        for (Spatial ball : allBalls) {
                System.out.println(ball.getName());
            }
         */
    }

    public boolean areBallsMoving() {
        boolean areBallsMoving = false;
        RigidBodyControl ballCheck;

        if (mobile.hasChild(jack)) {
            RigidBodyControl jackCheck = jack.getControl(RigidBodyControl.class);
            if (!jackCheck.getLinearVelocity().equals(Vector3f.ZERO)) {
                areBallsMoving = true;
            }
        }

        for (Spatial blueBall : blueBalls) {
            if (blueBall != null) {
                ballCheck = blueBall.getControl(RigidBodyControl.class);
                if (!ballCheck.getLinearVelocity().equals(Vector3f.ZERO)) {
                    areBallsMoving = true;
                }
            }
        }

        for (Spatial redBall : redBalls) {
            if (redBall != null) {
                ballCheck = redBall.getControl(RigidBodyControl.class);
                if (!ballCheck.getLinearVelocity().equals(Vector3f.ZERO)) {
                    areBallsMoving = true;
                }
            }
        }

        return areBallsMoving;
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    @Override
    public void collision(PhysicsCollisionEvent event) {
        // Vector3f output = null;
        float absorbEnergy = 0.95f;

        if (event.getNodeA().getParent().getName().equals("mobile")) {
            final Spatial node = event.getNodeA();
            //output = node.getLocalTranslation();
            RigidBodyControl nodeControl = node.getControl(RigidBodyControl.class);
            if (nodeControl.getLinearVelocity().length() < 0.01) {
                nodeControl.setLinearVelocity(Vector3f.ZERO);
                nodeControl.setAngularVelocity(Vector3f.ZERO);
                nodeControl.clearForces();

                //  System.out.println("Stopped");
            } else {
                //nodeControl.setLinearVelocity(nodeControl.getLinearVelocity().mult(absorbEnergy));
                nodeControl.setAngularVelocity(nodeControl.getAngularVelocity().mult(absorbEnergy));

                // System.out.println(nodeControl.getLinearVelocity());
            }
        } else if (event.getNodeB().getParent().getName().equals("mobile")) {
            final Spatial node = event.getNodeA();
            //output = node.getLocalTranslation();
            RigidBodyControl nodeControl = node.getControl(RigidBodyControl.class);
            if (nodeControl.getLinearVelocity().length() < 0.01) {
                nodeControl.setLinearVelocity(Vector3f.ZERO);
                nodeControl.setAngularVelocity(Vector3f.ZERO);
                nodeControl.clearForces();
                //  System.out.println("Stopped");
            } else {
                //nodeControl.setLinearVelocity(nodeControl.getLinearVelocity().mult(absorbEnergy));
                nodeControl.setAngularVelocity(nodeControl.getAngularVelocity().mult(absorbEnergy));
                //System.out.println(nodeControl.getLinearVelocity());
            }
        }
        //System.out.println("Collision");

        //float absorbEnergy = 0.99f;
        //jack.setLinearVelocity(getLinearVelocity().mult(absorbEnergy));
        //setAngularVelocity(getAngularVelocity().mult(absorbEnergy));
    }

    private float calculateDistance(Vector3f jackPos, Vector3f ballPos) {
        return jackPos.subtract(ballPos).length();
    }

    private void throwBall() {
        if (!mobile.hasChild(jack)) {
            jack = makeBall(new Vector3f(-12, 1, 0f), new Vector3f(18f + random.nextFloat(), -0.5f + random.nextFloat(), 0f + random.nextFloat()), ColorRGBA.Yellow, 0.02f, "jack");
            return;
        }
        if (allBalls.isEmpty()) {
            redBalls[0] = redBalls[0] = makeBall(new Vector3f(-12, 1, 0.5f), new Vector3f(17.5f + random.nextFloat(), -0.5f + random.nextFloat(), 0f + random.nextFloat()), ColorRGBA.Red, 0.0535f, "red0");
            return;
        }
        if (allBalls.get(0).getName().contains("red") || redBalls[3] != null) {
            for (int i = 0; i < blueBalls.length; i++) {
                if (blueBalls[i] == null) {
                    String name = "blue" + i;
                    blueBalls[i] = makeBall(new Vector3f(-12, 1, -0.5f), new Vector3f(17.5f + random.nextFloat(), -0.5f + random.nextFloat(), 0f + random.nextFloat()), ColorRGBA.Blue, 0.0535f, name);
                    return;
                }
            }
        }
        if (allBalls.get(0).getName().contains("blue") || blueBalls[3] != null) {
            for (int i = 0; i < redBalls.length; i++) {
                if (redBalls[i] == null) {
                    String name = "red" + i;
                    redBalls[i] = makeBall(new Vector3f(-12, 1, -0.5f), new Vector3f(17.5f + random.nextFloat(), -0.5f + random.nextFloat(), 0f + random.nextFloat()), ColorRGBA.Red, 0.0535f, name);
                    return;
                }
            }
        }
    }
}
