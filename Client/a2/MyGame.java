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
import tage.audio.*;

import tage.networking.IGameConnection.ProtocolType;
import tage.physics.PhysicsObject;

import java.io.*;
import java.net.InetAddress;

import static tage.GameObject.spawnObject;

public class MyGame extends VariableFrameRateGame {
    private static Engine engine;
    private InputManager im;
    private GhostManager gm;

    private CameraOrbit3D orbitController;
    private ViewportController viewportController;

    private RotationController rc;
    private JumpController jc;

    private GameObject avatar, backLeftTire, backRightTire, frontLeftTire, frontRightTire, zombie, terr;
    private GameObject x, y, z; // world axes
    private GameObject tallBuilding, casino, casinoSign;

    private AnimatedShape zombieS;
    private ObjShape avatarS, ghostS, backLeftTireS, backRightTireS, frontLeftTireS, frontRightTireS, terrS;
    private ObjShape linxS, linyS, linzS;
    private ObjShape tallBuildingS, casinoS, casinoSignS;

    private TextureImage avatarT, ghostT, tireT, zombieT, hills, road;
    private TextureImage tallBuildingT, casinoT, casinoSignT;

    private Light light1, moonLight, headLightsL, headLightsR;

    private int score = 0, zombiesAlive=0;
    private double lastFrameTime, currFrameTime, elapsTime;
    private boolean isGameStarted = false, isGameOver = false, isGameWon = false, showAxes = false, showPhysics = false;
    private boolean wasMoving = false;

    private String actionMsg = "";
    private float actionTimer = 0.0f;

    private GLCanvas canvas;
    private Robot robot;
    private float curMouseX, curMouseY, centerX, centerY;
    private float prevMouseX, prevMouseY;
    private boolean isRecentering;
    private boolean mouseModeInitiated = false;

    private int skybox;

    private CarController carController;

    private final boolean isMultiplayerMode;
    private String serverAddress;
    private int serverPort;
    private ProtocolType serverProtocol;
    private ProtocolClient protClient;
    private boolean isClientConnected = false;

    private IAudioManager audioMgr;
    private Sound carEngineSound, ambientSound;

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
    {	skybox = (engine.getSceneGraph()).loadCubeMap("skybox");
        (engine.getSceneGraph()).setActiveSkyBoxTexture(skybox);
        (engine.getSceneGraph()).setSkyBoxEnabled(true);
    }

    @Override
    public void loadSounds()
    { AudioResource resource1, resource2;
        audioMgr = engine.getAudioManager();
        resource1 = audioMgr.createAudioResource("carEngine.wav", AudioResourceType.AUDIO_SAMPLE);
        resource2 = audioMgr.createAudioResource("creepy.wav", AudioResourceType.AUDIO_SAMPLE);

        carEngineSound = new Sound(resource1, SoundType.SOUND_EFFECT, 50, true);
        ambientSound = new Sound(resource2, SoundType.SOUND_EFFECT, 15, true);

        carEngineSound.initialize(audioMgr);
        ambientSound.initialize(audioMgr);

        carEngineSound.setMaxDistance(10.0f);
        carEngineSound.setMinDistance(0.5f);
        carEngineSound.setRollOff(5.0f);

        ambientSound.setMaxDistance(10.0f);
        ambientSound.setMinDistance(0.5f);
        ambientSound.setRollOff(5.0f);
    }

    @Override
    public void loadShapes() {
        // world axes
        linxS = new Line(new Vector3f(0f, 0.01f, 0f), new Vector3f(3f, 0.01f, 0f));
        linyS = new Line(new Vector3f(0f, 0.01f, 0f), new Vector3f(0f, 3.01f, 0f));
        linzS = new Line(new Vector3f(0f, 0.01f, 0f), new Vector3f(0f, 0.01f, -3f));

        // car shapes
        avatarS = new ImportedModel("Car.obj");
        ghostS = new ImportedModel("Car.obj");
        backLeftTireS = new ImportedModel("LeftTire.obj");
        backRightTireS = new ImportedModel("RightTire.obj");
        frontLeftTireS = new ImportedModel("LeftTire.obj");
        frontRightTireS = new ImportedModel("RightTire.obj");

        // zombie animation
        zombieS = new AnimatedShape("zombieMesh.rkm", "zombieSkeleton.rks");
        zombieS.loadAnimation("RUN", "zombieRunning.rka");

        // building shapes
        tallBuildingS = new ImportedModel("TallBuilding.obj");
        casinoS = new ImportedModel("Casino.obj");
        casinoSignS = new ImportedModel("CasinoSign.obj");

        // terrain shape
        terrS = new TerrainPlane(200); // pixels per axis = 200x200
    }

    @Override
    public void loadTextures() {
        // car textures
        avatarT = new TextureImage("CarTexture.png");
        ghostT = new TextureImage("CarTexture.png");
        tireT = new TextureImage("TireTexture.png");

        // zombie textures
        zombieT = new TextureImage("ZombieSkin.png");

        // building textures
        tallBuildingT = new TextureImage("YlwBuildingTexture.png");
        casinoT = new TextureImage("CasinoTexture.png");
        casinoSignT = new TextureImage("SignsTexture.png");


        // terrain textures
        hills = new TextureImage("CityMap.png");    // height map
        road = new TextureImage("CityTexture.png"); // painted terrain
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

        // car
        avatar = spawnObject(GameObject.root(), avatarS, avatarT, 10f, 20.0f, 15f, 1.0f, 0.0f);
        backLeftTire = spawnObject(avatar, backLeftTireS, tireT, 0.22f, -0.09f, -0.385f, 1f, 0f);
        backRightTire = spawnObject(avatar, backRightTireS, tireT, -0.19f, -0.09f, -0.385f, 1f, 0f);
        frontLeftTire = spawnObject(avatar, frontLeftTireS, tireT, 0.22f, -0.09f, 0.315f, 1f, 0f);
        frontRightTire = spawnObject(avatar, frontRightTireS, tireT, -0.19f, -0.09f, 0.315f, 1f, 0f);

        backLeftTire.applyParentRotationToPosition(true);
        backRightTire.applyParentRotationToPosition(true);
        frontLeftTire.applyParentRotationToPosition(true);
        frontRightTire.applyParentRotationToPosition(true);

        // zombies
        zombie = spawnObject(GameObject.root(), zombieS, zombieT, 25f, 15f, 14f, 0.50f, 230.0f);

        // buildings
        tallBuilding = spawnObject(GameObject.root(), tallBuildingS, tallBuildingT, 25f, 0f, 14f, 2f, 0f);
        casino = spawnObject(GameObject.root(), casinoS, casinoT, 50f, 0f, 32f, 2f, 0f);
        casinoSign = spawnObject(casino, casinoSignS, casinoSignT, 0, 0f, 0f, 1f, 0f);

        // terrain
        Matrix4f initialTranslation, initialScale;
        terr = new GameObject(GameObject.root(), terrS, road);
        initialTranslation = (new Matrix4f()).translation(0f,0f,0f);
        terr.setLocalTranslation(initialTranslation);
        initialScale = (new Matrix4f()).scaling(100.0f, 15.0f, 100.0f);
        terr.setLocalScale(initialScale);
        terr.setHeightMap(hills);
        terr.getRenderStates().setTiling(1);
        terr.getRenderStates().setTileFactor(1);
    }

    @Override
    public void initializeLights() {
        Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
        light1 = new Light();
        light1.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
        (engine.getSceneGraph()).addLight(light1);

        moonLight = setLight(new Vector3f(0f, 10f, 0f), 5f, 0f, 3f); // TODO: If we change the skybox to include a moon

        headLightsL = new Light();
        headLightsR = new Light();
        setupHeadlight(headLightsL, 1.0f, 0.95f, 0.8f, avatar.getWorldLocation(), new Vector3f(0f, -0.2f, -1f));
        setupHeadlight(headLightsR, 1.0f, 0.95f, 0.8f, avatar.getWorldLocation(), new Vector3f(0f, -0.2f, -1f));
    }

    private void setupHeadlight(Light hl, float diffR, float diffG, float diffB, Vector3f loc, Vector3f dir){
        hl.setType(Light.LightType.SPOTLIGHT);

        hl.setDiffuse(diffR, diffG, diffB);
        hl.setAmbient(0.1f, 0.1f, 0.1f);

        hl.setCutoffAngle(35.0f);
        hl.setOffAxisExponent(5.0f);

        hl.setConstantAttenuation(1.0f);
        hl.setLinearAttenuation(0.09f);
        hl.setQuadraticAttenuation(0.02f);

        hl.setLocation(loc);
        hl.setDirection(dir);

        hl.disable();
        (engine.getSceneGraph()).addLight(hl);
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

        carController = new CarController(avatar, frontLeftTire, frontRightTire, backLeftTire, backRightTire, protClient, isMultiplayerMode);

        carEngineSound.setLocation(avatar.getWorldLocation());
        ambientSound.setLocation(tallBuilding.getWorldLocation());
        setEarParameters();
        carEngineSound.play();
        ambientSound.play();

        controlActions();
    }

    public void setEarParameters() {
        Viewport vp = engine.getRenderSystem().getViewport("LEFT");
        if (vp != null) {
            Camera camera = vp.getCamera();

            audioMgr.getEar().setLocation(camera.getLocation());
            audioMgr.getEar().setOrientation(camera.getN(), camera.getV());
        }
    }

    // Helper to add a hitbox around a stationary object
    private void addHitbox(GameObject obj, float m, float w, float h, float d, float xO, float yO, float zO) {
        float[] size = {w, h, d};
        Vector3f loc = obj.getWorldLocation();

        // location + offset
        Vector3f finalLoc = new Vector3f(loc.x + xO, loc.y + yO, loc.z + zO);

        (engine.getSceneGraph()).addPhysicsBox(m, finalLoc, new Quaternionf(), size);
    }

    @Override
    public void initializePhysicsObjects() {
        // terrain mesh
        (engine.getSceneGraph()).addPhysicsStaticTerrainMesh(
            new Vector3f(0f, 0f, 0f), new Quaternionf(), hills, 100.0f, 15.0f, 200);

        (engine.getSceneGraph()).getPhysicsEngine().setGravity(new float[]{0f, -9.81f, 0f});

        // Tall building
        addHitbox(tallBuilding, 0, 4.05f, 16f, 4.05f, 0f, 8f, 0f);
        addHitbox(tallBuilding, 0, 1.3f, 1f, 0.7f, 0f, 0.5f, 2.5f); // Steps

        // Casino building
        addHitbox(casino, 0, 13.3f, 10f, 4.05f, 0f, 5f, 0f);
        addHitbox(casino, 0, 1.7f, 2f, 0.8f, -1.65f, 1.0f, 2.0f); // Left steps
        addHitbox(casino, 0, 1.7f, 2f, 0.8f,  1.65f, 1.0f, 2.0f); // Right steps
        addHitbox(casino, 0, 0.1f, 2f, 0.1f, -2.3f, 1.0f, 3.12f); // Left-Left pillar
        addHitbox(casino, 0, 0.1f, 2f, 0.1f, -1.0f, 1.0f, 3.12f); // Left-Right pillar
        addHitbox(casino, 0, 0.1f, 2f, 0.1f,  1.0f, 1.0f, 3.12f); // Right-Left pillar
        addHitbox(casino, 0, 0.1f, 2f, 0.1f,  2.3f, 1.0f, 3.12f); // Right-Right pillar

        // car hitbox
        float[] carSize = {0.5f, 0.3f, 1.25f};
        PhysicsObject carPhysics = (engine.getSceneGraph()).addPhysicsBox(
            1.0f, avatar.getWorldLocation(), new Quaternionf(), carSize);
        avatar.setPhysicsObject(carPhysics);
        carPhysics.disableSleeping();
        carPhysics.setBounciness(0.15f);
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

        if(isGameStarted || !isMultiplayerMode) {
            carController.beginFrame();
            im.update((float) moveTime);

            PhysicsObject carPhysics = avatar.getPhysicsObject();
            carController.update((float) moveTime, carPhysics);

            (engine.getSceneGraph()).getPhysicsEngine().update((float) moveTime);

            // engine pitch
            float speed = Math.abs(carController.getCurrentSpeed());
            float maxSpeed = 14.0f; // Matches MAX_SPEED in CarController

            // starts pitch at 1.0 (idle) and reaches 2.0 at max speed
            float enginePitch = 1.0f + (speed / maxSpeed);
            carEngineSound.setPitch(enginePitch);

            if (carPhysics != null) {
                Vector3f physLoc = carPhysics.getLocation();
                avatar.setLocalLocation(new Vector3f(physLoc.x, physLoc.y, physLoc.z));

                Quaternionf avatarRot = new Quaternionf();
                avatar.getWorldRotation().getNormalizedRotation(avatarRot);
                carPhysics.setTransform(physLoc, avatarRot);
                carPhysics.setAngularVelocity(new float[]{0f, 0f, 0f});

                float[] v = carPhysics.getLinearVelocity();
                carEngineSound.setVelocity(new Vector3f(v[0], v[1], v[2]));
            }

            // Update positions
            carEngineSound.setLocation(avatar.getWorldLocation());
            ambientSound.setLocation(tallBuilding.getWorldLocation());

            setEarParameters();

            orbitController.updateCameraPosition();


        }

        updateHud();

        if (isMultiplayerMode) {
            processNetworking((float)moveTime);
        }

        zombieS.updateAnimation();

        boolean isMoving = Math.abs(carController.getCurrentSpeed()) > 0.01f;

        if (isMoving) {
            updateHeadlights();
            wasMoving = true;
        } else if (wasMoving) {
            // One final update when car just stopped
            updateHeadlights();
            wasMoving = false;
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
            if (!isGameStarted) {
                actionMsg = "Game has not yet started!";
            } else {
                actionMsg = "Zombies Alive: " + zombiesAlive;
            }
        }

        // main hud
        String speedHud = "Speed: " + Math.round(Math.abs(carController.getCurrentSpeed())) + " km/h";
        String scoreHud = "Total Score: " + score;

        Vector3f timerColor = new Vector3f(1, 1, 0);
        Vector3f scoreColor = new Vector3f(0, 1, 1);
        Vector3f actionColor = new Vector3f(1, 1, 1);
        (engine.getHUDmanager()).setHUD1(speedHud, timerColor, 20, height - 80);
        (engine.getHUDmanager()).setHUD2(scoreHud, scoreColor, 20, height - 40);
        (engine.getHUDmanager()).setHUD3(gameStatusMsg, statusColor, width / 2, height - 50); // top middle of window
        (engine.getHUDmanager()).setHUD4(actionMsg, actionColor, 20, height - 120);

        // mini viewport (bottom right)
        Viewport vr = engine.getRenderSystem().getViewport("RIGHT");
        int vrX = (int) (width * vr.getRelativeLeft() + 10);
        int vrY = (int) (height * (vr.getRelativeBottom() + vr.getRelativeHeight()) - 30);

        // avatars world coordinates
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
                case KeyEvent.VK_L:
                    if(isGameStarted || !isMultiplayerMode) {
                        headLightsL.toggleOnOff();
                        headLightsR.toggleOnOff();
                        updateHeadlights();
                    }
                    break;
                case KeyEvent.VK_X:
                    toggleAxes();
                    break;
                case KeyEvent.VK_Z:
                    if (isMultiplayerMode) {
                        if (protClient != null && isClientConnected)
                        {	protClient.sendByeMessage();
                            setIsConnected(false);
                            shutdown();
                            System.exit(0);
                        }
                    }
                    break;
                case KeyEvent.VK_R:
                    if (isMultiplayerMode) {
                        protClient.sendReadyMessage();
                    }
                    break;
                case KeyEvent.VK_T:
                    toggleRecenter();
                    break;
                case KeyEvent.VK_C:
                 zombieS.stopAnimation();
                    zombieS.playAnimation("RUN", 0.45f,
                            AnimatedShape.EndType.LOOP, 0);
                    break;
                case KeyEvent.VK_SPACE:
                    if (!showPhysics) {
                        engine.enablePhysicsWorldRender();
                        showPhysics = true;
                    } else {
                        engine.disablePhysicsWorldRender();
                        showPhysics = false;
                    }
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
        robot.mouseMove(centerX, centerY);
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
    public AnimatedShape getNPCShape() { return zombieS; }
    public TextureImage getNPCTexture() { return zombieT; }

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
                displayAction("A Player has joined the game!", 2);
                break;
            case "byeMsg":
                displayAction("A Player has left the game!", 2);
                break;
            case "startMsg":
                displayAction("The game has been started, good luck!", 2);
                isGameStarted = true;
                break;
        }
    }
    public void setZombiesAlive(int alive){ zombiesAlive = alive; }

    private class SendCloseConnectionPacketAction extends AbstractInputAction
    {	@Override
    public void performAction(float time, net.java.games.input.Event evt)
    {	if(protClient != null && isClientConnected)
    {	protClient.sendByeMessage();
    }
    }
    }

    public void updateHeadlights(){
        Matrix4f rot = avatar.getLocalRotation();

        Vector3f forward = new Vector3f(rot.m20(), rot.m21(), rot.m22());
        Vector3f right   = new Vector3f(rot.m00(), rot.m01(), rot.m02());
        Vector3f up      = new Vector3f(rot.m10(), rot.m11(), rot.m12());

        Vector3f avatarPos = avatar.getWorldLocation();

        Vector3f leftOffset  = new Vector3f(right).mul(-0.17f);
        Vector3f rightOffset = new Vector3f(right).mul( 0.18f);
        Vector3f frontOffset = new Vector3f(forward).mul(.55f);
        Vector3f upOffset    = new Vector3f(up).mul(0.06f);

        headLightsL.setLocation(new Vector3f(avatarPos).add(leftOffset).add(frontOffset).add(upOffset));
        headLightsR.setLocation(new Vector3f(avatarPos).add(rightOffset).add(frontOffset).add(upOffset));

        Vector3f lightDir = new Vector3f(forward).add(new Vector3f(up).mul(-0.2f)).normalize();
        headLightsL.setDirection(lightDir);
        headLightsR.setDirection(lightDir);
    }

    public void setThrottle(float input) { carController.setThrottle(input); }
    public void turn(float delta) { carController.turn(delta); }

}

