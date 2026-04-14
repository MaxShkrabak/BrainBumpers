package a2;

import com.jogamp.opengl.awt.GLCanvas;
import tage.*;
import tage.input.InputManager;
import tage.input.action.AbstractInputAction;
import tage.nodeControllers.JumpController;
import tage.nodeControllers.RotationController;
import tage.shapes.*;

import java.awt.*;
import java.lang.Math;
import java.awt.event.*;
import org.joml.*;
import javax.swing.*;

import tage.networking.IGameConnection.ProtocolType;

import java.io.*;
import java.net.InetAddress;

import java.net.UnknownHostException;

import static tage.GameObject.spawnObject;

public class MyGame extends VariableFrameRateGame {
    private static Engine engine;
    private InputManager im;
    private GhostManager gm;

    private CameraOrbit3D orbitController;
    private ViewportController viewportController;

    private RotationController rc;
    private JumpController jc;

    private GameObject avatar, backTire, frontLeftTire, frontRightTire, zombie, house, floor;
    private GameObject x, y, z; // world axes

    private ObjShape avatarS, ghostS, backTireS, frontLeftTireS, frontRightTireS, zombieS, houseS, floorS;
    private ObjShape linxS, linyS, linzS;

    private TextureImage avatarT, ghostT, tireT, zombieT, brick, floorT;
    private TextureImage[] pyramidTextures;

    private Light light1, khafreLight, khufuLight, menkaureLight;

    private int score = 0;
    private double lastFrameTime, currFrameTime, elapsTime;
    private boolean isGameOver = false, isGameWon = false, showAxes = false;
    private static final int FLOOR_TEXTURE_TILE = 20;

    private String actionMsg = "";
    private float actionTimer = 0.0f;

    private GLCanvas canvas;
    private Robot robot;
    private float curMouseX, curMouseY, centerX, centerY;
    private float prevMouseX, prevMouseY;
    private boolean isRecentering;
    private boolean mouseModeInitiated = false;

    private int lakeIslands;

    private boolean isMultiplayerMode;
    private String serverAddress;
    private int serverPort;
    private ProtocolType serverProtocol;
    private ProtocolClient protClient;
    private boolean isClientConnected = false;

    public MyGame(String serverAddress, int serverPort, String protocol)
    {	super();
        gm = new GhostManager(this);
        isMultiplayerMode = true;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        if (protocol.toUpperCase().compareTo("TCP") == 0)
            this.serverProtocol = ProtocolType.TCP;
        else
            this.serverProtocol = ProtocolType.UDP;
    }

    public MyGame() {
        super();
        isMultiplayerMode = false;
    }

    public static void main(String[] args) {
        MyGame game;
        if (Boolean.parseBoolean(args[0])){
            game = new MyGame(args[1], Integer.parseInt(args[2]), args[3]);
        } else {
            game = new MyGame();
        }
        engine = new Engine(game);
        engine.initializeSystem();
        game.buildGame();
        game.startGame();
    }

    @Override
    public void loadSkyBoxes()
    {	lakeIslands = (engine.getSceneGraph()).loadCubeMap("lakeIslands");
        (engine.getSceneGraph()).setActiveSkyBoxTexture(lakeIslands);
        (engine.getSceneGraph()).setSkyBoxEnabled(true);
    }

    @Override
    public void loadShapes() {
        avatarS = new ImportedModel("Car.obj");
        ghostS = new ImportedModel("Car.obj");
        backTireS = new ImportedModel("Tire.obj");
        frontLeftTireS = new ImportedModel("FrontRightTire1.obj");
        frontRightTireS = new ImportedModel("FrontRightTire2.obj");
        zombieS = new ImportedModel("Zombie.obj");

        houseS = new DolphinHouse();

        floorS = new Plane();

        // world axes
        linxS = new Line(new Vector3f(0f, 0.01f, 0f), new Vector3f(3f, 0.01f, 0f));
        linyS = new Line(new Vector3f(0f, 0.01f, 0f), new Vector3f(0f, 3.01f, 0f));
        linzS = new Line(new Vector3f(0f, 0.01f, 0f), new Vector3f(0f, 0.01f, -3f));
    }

    @Override
    public void loadTextures() {
        avatarT = new TextureImage("CarTexture.png");
        ghostT = new TextureImage("CarTexture.png");
        tireT = new TextureImage("TireTexture.png");
        zombieT = new TextureImage("ZombieTexture.png");

        brick = new TextureImage("brick1.jpg");
        pyramidTextures = new TextureImage[]{brick};

        floorT = new TextureImage("desert.jpg");
    }

    @Override
    public void buildObjects() {
        // world axes
        x = new GameObject(GameObject.root(), linxS);
        y = new GameObject(GameObject.root(), linyS);
        z = new GameObject(GameObject.root(), linzS);
        (x.getRenderStates()).setColor(new Vector3f(1f, 0f, 0f));
        (y.getRenderStates()).setColor(new Vector3f(0f, 1f, 0f));
        (z.getRenderStates()).setColor(new Vector3f(0f, 0f, 1f));

        // spawns floor surface (desert)
        floor = spawnObject(GameObject.root(), floorS, floorT, 0f, 0f, 0f, 150.0f, 0f);
        floor.getRenderStates().setTiling(1);
        floor.getRenderStates().setTileFactor(FLOOR_TEXTURE_TILE);

        avatar = spawnObject(GameObject.root(), avatarS, avatarT, 0f, 0.65f, 0f, 3.0f, 0.0f);
        backTire = spawnObject(avatar, backTireS, tireT, 0f, 0f, 0f, 1f, 0f);
        //frontLeftTire = spawnObject(avatar, frontLeftTireS, tireT, 0.19f, -0.1f, 0.8f, 1f, 0f);
        //frontRightTire = spawnObject(avatar, frontRightTireS, tireT, -0.19f, -0.1f, 0.8f, 1f, 0f);
        zombie = spawnObject(GameObject.root(), zombieS, zombieT, 4f, 0f, 4f, 1.0f, 270.0f);

        // spawn home
        house = spawnObject(GameObject.root(), houseS, brick, 18f, 2.01f, 2f, 2f, 0f);
    }

    @Override
    public void initializeLights() {
        Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
        light1 = new Light();
        light1.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
        (engine.getSceneGraph()).addLight(light1);

        khafreLight = setLight(new Vector3f(70f, 26f, 65f), 5f, 0f, 3f);

        khufuLight = setLight(new Vector3f(-15f, 36f, -75f), 7f, 3f, 0f);
        khufuLight.setLinearAttenuation(0.01f); // allows light to reach the surface

        menkaureLight = setLight(new Vector3f(-55f, 16f, 35f), 0f, 5f, 1f);
    }

    private Light setLight(Vector3f location, float r, float g, float b) {
        Light light = new Light();
        light.setLocation(location);
        light.setDiffuse(r, g, b);
        light.setLinearAttenuation(0.1f);
        light.setQuadraticAttenuation(0.01f);
        (engine.getSceneGraph()).addLight(light);
        return light;
    }

    @Override
    public void initializeGame() {
        lastFrameTime = System.currentTimeMillis();
        currFrameTime = System.currentTimeMillis();
        elapsTime = 0.0;
        (engine.getRenderSystem()).setWindowDimensions(1900, 1000);

        if (isMultiplayerMode) {
            setupNetworking();
        }

        controlActions();

        // rotation controller
        rc = new RotationController(engine, new Vector3f(0, 1, 0), 0.001f);
        (engine.getSceneGraph()).addNodeController(rc);

        // jump controller
        jc = new JumpController(engine);
        jc.addTarget(house);
        (engine.getSceneGraph()).addNodeController(jc);
    }

    @Override
    public void createViewports() {
        viewportController = new ViewportController(engine);
        viewportController.setupViewports();
    }

    @Override
    public void update() {
        if (!mouseModeInitiated) initMouseMode();

        lastFrameTime = currFrameTime;
        currFrameTime = System.currentTimeMillis();
        double moveTime = (currFrameTime - lastFrameTime) / 1000.0;
        elapsTime += moveTime;

        im.update((float) moveTime);
        orbitController.updateCameraPosition();
        updateHud();

        if (isMultiplayerMode) {
            processNetworking((float)moveTime);
        }
    }

    private void updateHud() {
        // game screen size
        int width = engine.getRenderSystem().getGLCanvas().getWidth();
        int height = engine.getRenderSystem().getGLCanvas().getHeight();

        // status message and color
        String gameStatusMsg = "";
        Vector3f statusColor = new Vector3f(0, 1, 0);

        if (isGameOver) {
            mouseModeInitiated = false;
            gameStatusMsg = "You lost.";
            statusColor = new Vector3f(1, 0, 0);
        } else if (isGameWon) {
            gameStatusMsg = "You Win!";
        }

        // action message
        if (actionTimer <= elapsTime) {
            float dist = 5f; // hard coded needs to be fixed TODO if we do action message
            actionMsg = "Closest pyramid is " + (int) dist + " meters away.";
        }

        // main hud
        String timer = "Time = " + Math.round((float) elapsTime);
        String scoreHud = "Total Score: " + score;

        Vector3f timerColor = new Vector3f(1, 1, 0);
        Vector3f scoreColor = new Vector3f(0, 1, 1);
        Vector3f actionColor = new Vector3f(1, 1, 1);
        (engine.getHUDmanager()).setHUD1(timer, timerColor, 20, height - 80);
        (engine.getHUDmanager()).setHUD2(scoreHud, scoreColor, 20, height - 40);
        (engine.getHUDmanager()).setHUD3(gameStatusMsg, statusColor, width / 2, height - 50); // top middle of window
        (engine.getHUDmanager()).setHUD4(actionMsg, actionColor, 20, height - 120);

        // mini viewport (bottom right)
        Viewport vr = engine.getRenderSystem().getViewport("RIGHT");
        int vrX = (int) (width * vr.getRelativeLeft() + 10);
        int vrY = (int) (height * (vr.getRelativeBottom() + vr.getRelativeHeight()) - 30);

        // dolphin's world coordinates
        Vector3f avatarLoc = avatar.getWorldLocation();
        int xloc = (int) avatarLoc.x;
        int zloc = (int) avatarLoc.z;
        int yloc = (int) avatarLoc.y;
        (engine.getHUDmanager()).setHUD5("X:" + xloc + " Y:" + yloc + " Z:" + zloc, new Vector3f(1, 1, 1), vrX, vrY);
    }

    // helper to change action message in hud
    private void displayAction(String msg, float duration) {
        actionMsg = msg;
        actionTimer = (float) elapsTime + duration;
    }

    public void toggleAxes() {
        if (!showAxes) {
            showAxes = true;
            x.getRenderStates().enableRendering();
            y.getRenderStates().enableRendering();
            z.getRenderStates().enableRendering();
        } else {
            showAxes = false;
            x.getRenderStates().disableRendering();
            y.getRenderStates().disableRendering();
            z.getRenderStates().disableRendering();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!isGameOver) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_X:
                    toggleAxes();
                    break;
                case KeyEvent.VK_Z:
                    if (isMultiplayerMode) {
                        if(protClient != null && isClientConnected)
                        {	protClient.sendByeMessage();
                            setIsConnected(false);
                            shutdown();
                            System.exit(0);
                        }
                    }
                    break;
                case KeyEvent.VK_R:
                    toggleRecenter();
                    break;
            }

        }
        super.keyPressed(e);
    }

    private int[] returnVPCoords(Viewport vp, float x, float y){
        int[] newCoords = new int[2];
        float sWidth = engine.getRenderSystem().getWidth();
        float sHeight = engine.getRenderSystem().getHeight();

        float left = vp.getRelativeLeft() * sWidth;
        float bottom = vp.getRelativeBottom() * sHeight + vp.getBorderWidth() + 5f;
        float width = vp.getRelativeWidth() * sWidth;
        float height = vp.getRelativeHeight() * sHeight - vp.getBorderWidth() - 40f;

        newCoords[0] = (int)(left + (x * width));
        newCoords[1] = (int)(bottom + (y * height));

        return newCoords;
    }

    private void controlActions() {
        im = engine.getInputManager();
        String gpName = im.getFirstGamepadName();
        Camera leftCam = viewportController.getLeftCamera();
        orbitController = new CameraOrbit3D(leftCam, avatar, gpName, engine);

        // gamepad controls
        GamepadAction toggleAxesGamepad = new GamepadAction(this, 'x');
        MovAction movGamepad = new MovAction(this, -1.0f, protClient);
        TurnAction turnGamepad = new TurnAction(this, -1.0f, protClient);

        // gamepad actions
        im.associateActionWithAllGamepads(
                net.java.games.input.Component.Identifier.Axis.X, turnGamepad,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(
                net.java.games.input.Component.Identifier.Axis.Y, movGamepad,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(
                net.java.games.input.Component.Identifier.Button._2, toggleAxesGamepad,
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        // keyboard controls
        TurnAction turnLeftAction = new TurnAction(this, 1.0f, protClient);
        TurnAction turnRightAction = new TurnAction(this, -1.0f, protClient);
        MovAction fwdAction = new MovAction(this, 1.0f, protClient);
        MovAction backAction = new MovAction(this, -1.0f, protClient);

        // keyboard actions
        im.associateActionWithAllKeyboards(
                net.java.games.input.Component.Identifier.Key.W, fwdAction,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(
                net.java.games.input.Component.Identifier.Key.S, backAction,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(
                net.java.games.input.Component.Identifier.Key.D, turnRightAction,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(
                net.java.games.input.Component.Identifier.Key.A, turnLeftAction,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        // mini viewport controls
        Camera rightCamera = viewportController.getRightCamera();

        ViewportAction panUp = new ViewportAction(rightCamera, 0, 0, -1);
        ViewportAction panDown = new ViewportAction(rightCamera, 0, 0, 1);
        ViewportAction panLeft = new ViewportAction(rightCamera, -1, 0, 0);
        ViewportAction panRight = new ViewportAction(rightCamera, 1, 0, 0);
        ViewportAction zoomIn = new ViewportAction(rightCamera, 0, -1, 0);
        ViewportAction zoomOut = new ViewportAction(rightCamera, 0, 1, 0);

        // mini viewport actions
        im.associateActionWithAllKeyboards(
                net.java.games.input.Component.Identifier.Key.UP, panUp,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(
                net.java.games.input.Component.Identifier.Key.DOWN, panDown,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(
                net.java.games.input.Component.Identifier.Key.RIGHT, panRight,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(
                net.java.games.input.Component.Identifier.Key.LEFT, panLeft,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(
                net.java.games.input.Component.Identifier.Key.O, zoomOut,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(
                net.java.games.input.Component.Identifier.Key.I, zoomIn,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        // gamepad
        im.associateActionWithAllGamepads(
                net.java.games.input.Component.Identifier.Axis.POV, panUp,
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(
                net.java.games.input.Component.Identifier.Button._8, zoomOut, // left joystick
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllGamepads(
                net.java.games.input.Component.Identifier.Button._9, zoomIn, // right joystick
                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    }

    // ======= Mouse controls =========
    private void initMouseMode() {
        mouseModeInitiated = true;
        RenderSystem rs = engine.getRenderSystem();
        Viewport vw = rs.getViewport("LEFT");
        float left = vw.getActualLeft();
        float bottom = vw.getActualBottom();
        float width = vw.getActualWidth();
        float height = vw.getActualHeight();
        centerX = (int) (left + width / 2);
        centerY = (int) (bottom - height / 2);
        isRecentering = false;
        try // note that some platforms may not support the Robot class
        {
            robot = new Robot();
        } catch (AWTException ex) {
            throw new RuntimeException("Couldn't create Robot!");
        }
        recenterMouse();
        prevMouseX = centerX; // 'prevMouse' defines the initial
        prevMouseY = centerY; // mouse position
        // also change the cursor
        Image faceImage = new
                ImageIcon("./assets/textures/face.gif").getImage();
        Cursor faceCursor = Toolkit.getDefaultToolkit().
                createCustomCursor(faceImage, new Point(0, 0), "FaceCursor");
        canvas = rs.getGLCanvas();
        canvas.setCursor(faceCursor);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // if robot is recentering and the MouseEvent location is in the center,
        // then this event was generated by the robot
        if (mouseModeInitiated) {
            if (wantToRecenter) {
                if (isRecentering &&
                        centerX == e.getXOnScreen() && centerY == e.getYOnScreen()) { // mouse recentered, recentering complete
                    isRecentering = false;
                } else { // event was due to a user mouse-move, and must be processed
                    curMouseX = e.getXOnScreen();
                    curMouseY = e.getYOnScreen();
                    float mouseDeltaX = prevMouseX - curMouseX;
                    float mouseDeltaY = prevMouseY - curMouseY;
                    float frameTime = (float) ((System.currentTimeMillis() - lastFrameTime) / 1000.0);
                    float mouseSensitivity = 30f;

                    orbitController.updateAzimuth(mouseDeltaX * mouseSensitivity, frameTime);
                    orbitController.updateElevation(mouseDeltaY * mouseSensitivity, frameTime);
                    prevMouseX = curMouseX;
                    prevMouseY = curMouseY;
                    // tell robot to put the cursor to the center (since user just moved it)
                    recenterMouse();

                    prevMouseX = centerX; // reset prev to center
                    prevMouseY = centerY;
                }
            }
        }
    }
    private void toggleRecenter() {
        wantToRecenter = !wantToRecenter;
    }
    private boolean wantToRecenter = true;

    private void recenterMouse() {
        // use the robot to move the mouse to the center point.
        // Note that this generates one MouseEvent.
        RenderSystem rs = engine.getRenderSystem();
        Viewport vw = rs.getViewport("LEFT");
        float left = vw.getActualLeft();
        float bottom = vw.getActualBottom();
        float width = vw.getActualWidth();
        float height = vw.getActualHeight();
        int centerX = (int) (left + width / 2.0f);
        int centerY = (int) (bottom - height / 2.0f);
        isRecentering = true;
        robot.mouseMove((int) centerX, (int) centerY);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        float scrollDirection = (float) e.getWheelRotation();
        float frameTime = (float) ((System.currentTimeMillis() - lastFrameTime) / 1000.0);
        if (mouseModeInitiated) {
            orbitController.updateRadius(scrollDirection, frameTime);
        }
    }

    public GameObject getAvatar() {
        return avatar;
    }

    // ---------- NETWORKING SECTION ----------------

    public ObjShape getGhostShape() { return ghostS; }
    public TextureImage getGhostTexture() { return ghostT; }
    public GhostManager getGhostManager() { return gm; }
    public Engine getEngine() { return engine; }

    private void setupNetworking()
    {	isClientConnected = false;
        try {
            protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (protClient == null) {
            System.out.println("[CLIENT]: Missing protocol host");
        } else {
            // Send the initial join message with a unique identifier for this client
            System.out.println("[CLIENT]: Sending join message to protocol host");
            protClient.sendJoinMessage();
        }
    }

    protected void processNetworking(float elapsTime)
    {	// Process packets received by the client from the server
        if (protClient != null)
            protClient.processPackets();
    }

    public Vector3f getPlayerPosition() { return avatar.getWorldLocation(); }
    public Matrix4f getPlayerRotation() { System.out.println(avatar.getWorldRotation()); return avatar.getWorldRotation();}

    public void setIsConnected(boolean value) { this.isClientConnected = value; }
    public void showChatMessage(String msgType){
        switch (msgType) {
            case "joinMsg":
                displayAction("A Player has joined the game!", 5);
                break;
            case "byeMsg":
                displayAction("A Player has left the game!", 5);
                break;
        }
    }

    private class SendCloseConnectionPacketAction extends AbstractInputAction
    {	@Override
    public void performAction(float time, net.java.games.input.Event evt)
    {	if(protClient != null && isClientConnected == true)
    {	protClient.sendByeMessage();
    }
    }
    }
}

