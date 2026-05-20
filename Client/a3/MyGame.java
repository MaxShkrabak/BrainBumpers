package a3;

import com.jogamp.opengl.awt.GLCanvas;
import org.joml.Random;
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
import java.util.*;
import java.util.List;

import static tage.GameObject.spawnObject;

public class MyGame extends VariableFrameRateGame {
    private static Engine engine;
    private InputManager im;
    private GhostManager gm;

    private CameraOrbit3D orbitController;
    private ViewportController viewportController;

    private RotationController rc;
    private JumpController jc;

    private GameObject avatar, backLeftTire, backRightTire, frontLeftTire, frontRightTire, terr;
    private GameObject x, y, z; // world axes
    private GameObject tallBuilding, casino, casinoSign,
            twoStoryWide1, twoStoryWide2, twoStoryWide3, twoStoryWide4, twoStoryWide5, twoStoryWide6,
            rHome1, rHome2, rHome3, rHome4, rHome5, rHome6, house1, house2, house3, house4, house5,
            twoStoryHome1, twoStoryHome2, twoStoryHome3, twoStoryHome4, twoStoryHome5, twoStoryHome6,
            twoStoryRoof1, twoStoryRoof2, twoStoryRoof3, twoStoryRoof4, twoStoryRoof5, twoStoryRoof6, twoStoryRoof7,
            threeStoryBalcony1, pharmacySign1, twoStoryBalcony1, hardwareSign1, twoStoryColumns1, barbershopSign1, oneStory1, bakerySign1, twoStoryDouble1, bookshopSign1,
            threeStoryBalcony2, pharmacySign2, twoStoryBalcony2, hardwareSign2, twoStoryColumns2, barbershopSign2, oneStory2, bakerySign2, twoStoryDouble2, bookshopSign2;

    private AnimatedShape zombieS;
    private ObjShape avatarS, ghostS, leftTireS, rightTireS, terrS, rHomeS, houseS;
    private ObjShape linxS, linyS, linzS;
    private ObjShape tallBuildingS, casinoS, casinoSignS, twoStoryWideS, twoStoryHomeS, twoStoryRoofS,
            threeStoryBalconyS, pharmacySignS, twoStoryBalconyS, hardwareSignS, twoStoryColumnsS, barbershopSignS,
            oneStoryS, bakerySignS, twoStoryDoubleS, bookshopSignS;

    private TextureImage tireT, zombieT, hills, road;
    private TextureImage[] avatarT = new TextureImage[3];
    private int currSkinOpt = 0;

    private TextureImage tallBuildingT, casinoT, casinoSignT, twoStoryWideT1, twoStoryWideT2, rHomeT, houseT;

    private Light light1, casinoLight, headLightsL, headLightsR;

    private PlayerHealth playerHealth = new PlayerHealth();
    private static final float KILL_SPEED_THRESHOLD = 5.0f;

    private int score = 0, winnerScore = 0, zombiesAlive=0;
    private double lastFrameTime, currFrameTime, elapsTime;
    private boolean isGameStarted = false, isGameOver = false, isGameWon = false, isSpectating = false, showAxes = false, showPhysics = false;
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
    private Sound carEngineSound, ambientSound, splashSound, casinoSound;
    Random rn = new Random();

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
    { AudioResource resource1, resource2, resource3, resource4;
        audioMgr = engine.getAudioManager();
        resource1 = audioMgr.createAudioResource("carEngine.wav", AudioResourceType.AUDIO_SAMPLE); // car sound
        resource2 = audioMgr.createAudioResource("creepy.wav", AudioResourceType.AUDIO_SAMPLE);    // ambient sound
        resource3 = audioMgr.createAudioResource("splash.wav", AudioResourceType.AUDIO_SAMPLE);    // zombie kill sound
        resource4 = audioMgr.createAudioResource("casino.wav", AudioResourceType.AUDIO_SAMPLE);    // casino building sound

        carEngineSound = new Sound(resource1, SoundType.SOUND_EFFECT, 25, true);
        ambientSound = new Sound(resource2, SoundType.SOUND_EFFECT, 20, true);
        splashSound = new Sound(resource3, SoundType.SOUND_EFFECT, 40, false);
        casinoSound = new Sound(resource4, SoundType.SOUND_EFFECT, 25, true);

        carEngineSound.initialize(audioMgr);
        ambientSound.initialize(audioMgr);
        splashSound.initialize(audioMgr);
        casinoSound.initialize(audioMgr);

        carEngineSound.setMaxDistance(100.0f);
        carEngineSound.setMinDistance(2f);
        carEngineSound.setRollOff(1.0f);

        ambientSound.setRollOff(0.0f);

        splashSound.setMaxDistance(100.0f);
        splashSound.setMinDistance(3.0f);
        splashSound.setRollOff(1.0f);

        casinoSound.setMaxDistance(1f);
        casinoSound.setMinDistance(0.25f);
        casinoSound.setRollOff(1.0f);
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
        leftTireS = new ImportedModel("LeftTire.obj");
        rightTireS = new ImportedModel("RightTire.obj");

        // zombie animation
        zombieS = new AnimatedShape("zombieMesh.rkm", "zombieSkeleton.rks");
        zombieS.loadAnimation("RUN", "zombieRunning.rka");

        // building shapes
        tallBuildingS = new ImportedModel("TallBuilding.obj");
        casinoS = new ImportedModel("Casino.obj");
        casinoSignS = new ImportedModel("CasinoSign.obj");
        twoStoryWideS = new ImportedModel("TwoStoryWide.obj");
        rHomeS = new ImportedModel("HomeRoof.obj");
        houseS = new ImportedModel("House.obj");
        twoStoryHomeS = new ImportedModel("TwoStoryHome.obj");
        twoStoryRoofS = new ImportedModel("TwoStoryRoof.obj");
        threeStoryBalconyS = new ImportedModel("ThreeStoryBalcony.obj");
        pharmacySignS = new ImportedModel("SignPharmacy.obj");
        twoStoryBalconyS = new ImportedModel("TwoStoryBalcony.obj");
        hardwareSignS = new ImportedModel("SignHardware.obj");
        twoStoryColumnsS = new ImportedModel("TwoStoryColumns.obj");
        barbershopSignS = new ImportedModel("SignBarbershop.obj");
        oneStoryS = new ImportedModel("OneStory.obj");
        bakerySignS = new ImportedModel("SignBakery.obj");
        twoStoryDoubleS = new ImportedModel("TwoStoryDouble.obj");
        bookshopSignS = new ImportedModel("SignBookshop.obj");

        // terrain shape
        terrS = new TerrainPlane(200); // pixels per axis = 200x200
    }

    @Override
    public void loadTextures() {
        // car textures
        avatarT[0] = new TextureImage("CarTexture1.png");
        avatarT[1] = new TextureImage("CarTexture2.png");
        avatarT[2] = new TextureImage("CarTexture3.png");
        tireT = new TextureImage("TireTexture.png");

        // zombie textures
        zombieT = new TextureImage("ZombieSkin.png");

        // building textures
        tallBuildingT = new TextureImage("YlwBuildingTexture.png");
        casinoT = new TextureImage("CasinoTexture.png");
        casinoSignT = new TextureImage("SignsTexture.png");
        twoStoryWideT1 = new TextureImage("DarkBuildingTexture.png");
        twoStoryWideT2 = new TextureImage("RedBuildingTexture.png");
        rHomeT = new TextureImage("YlwBuildingTexture.png");
        houseT = new TextureImage("RedBuildingTexture.png");

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
        currSkinOpt = rn.nextInt(3);
        avatar = spawnObject(GameObject.root(), avatarS, avatarT[currSkinOpt], 12f, 15f, -5f, 2f, 0f);
        backLeftTire = spawnObject(avatar, leftTireS, tireT, 0.44f, -0.18f, -0.77f, 1f, 0f);
        backRightTire = spawnObject(avatar, rightTireS, tireT, -0.38f, -0.18f, -0.77f, 1f, 0f);
        frontLeftTire = spawnObject(avatar, leftTireS, tireT, 0.44f, -0.18f, 0.63f, 1f, 0f);
        frontRightTire = spawnObject(avatar, rightTireS, tireT, -0.38f, -0.18f, 0.63f, 1f, 0f);

        backLeftTire.applyParentRotationToPosition(true);
        backRightTire.applyParentRotationToPosition(true);
        frontLeftTire.applyParentRotationToPosition(true);
        frontRightTire.applyParentRotationToPosition(true);

        // zombies
        // zombie = spawnObject(GameObject.root(), zombieS, zombieT, 25f, 15f, 14f, 0.50f, 230.0f);

        // TODO: Update textures
        // buildings
        tallBuilding = spawnObject(GameObject.root(), tallBuildingS, tallBuildingT, -20f, 0f, 10f, 2f, 180f);
        casino = spawnObject(GameObject.root(), casinoS, casinoT, 0f, 0f, -10f, 2f, 0f);
        casinoSign = spawnObject(casino, casinoSignS, casinoSignT, 0f, 0f, 0f, 1f, 0f);
        twoStoryWide1 = spawnObject(GameObject.root(), twoStoryWideS, twoStoryWideT1, -35f, 0f, 10f, 2f, 90f);
        twoStoryWide2 = spawnObject(GameObject.root(), twoStoryWideS, twoStoryWideT2, -35f, 0f, -10f, 2f, 90f);
        twoStoryWide3 = spawnObject(GameObject.root(), twoStoryWideS, twoStoryWideT1, -25f, 0f, -10f, 2f, 270f);
        twoStoryWide4 = spawnObject(GameObject.root(), twoStoryWideS, twoStoryWideT1, -10f, 0f, 20f, 2f, 180f);
        twoStoryWide5 = spawnObject(GameObject.root(), twoStoryWideS, twoStoryWideT1, 3f, 0f, 20f, 2f, 180f);
        twoStoryWide6 = spawnObject(GameObject.root(), twoStoryWideS, twoStoryWideT2, 20f, 0, 10, 2f, 0f);
        house1 = spawnObject(GameObject.root(), houseS, houseT, -30f, 0f, 25f, 2f, 90f);
        rHome1 = spawnObject(GameObject.root(), rHomeS, rHomeT, -30f, 0f, 30f, 2f, 90f);
        house2 = spawnObject(GameObject.root(), houseS, houseT, -30f, 0f, 20f, 2f, 90f);
        house3 = spawnObject(GameObject.root(), houseS, houseT, -20f, 0f, 25f, 2f, 270f);
        house4 = spawnObject(GameObject.root(), houseS, houseT, 38f, 0f, -10f, 2f, 270f);
        house5 = spawnObject(GameObject.root(), houseS, houseT, 38f, 0f, -15f, 2f, 270f);
        rHome2 = spawnObject(GameObject.root(), rHomeS, rHomeT, -20f, 0f, 30f, 2f, 270f);
        rHome3 = spawnObject(GameObject.root(), rHomeS, rHomeT, -20f, 0f, 20f, 2f, 270f);
        rHome4 = spawnObject(GameObject.root(), rHomeS, rHomeT, -25f, 0f, 35f, 2f, 180f);
        rHome5 = spawnObject(GameObject.root(), rHomeS, rHomeT, 40f, 0, 15, 2f, 270f);
        rHome6 = spawnObject(GameObject.root(), rHomeS, rHomeT, 38f, 0, 5, 2f, 270f);
        // TODO: stuff

        twoStoryHome1 = spawnObject(GameObject.root(), twoStoryHomeS, tallBuildingT, 12.5f, 0, 21.5f, 2f, 90f);
        twoStoryHome2 = spawnObject(GameObject.root(), twoStoryHomeS, tallBuildingT, 12.5f, 0, 26.5f, 2f, 90f);
        twoStoryHome3 = spawnObject(GameObject.root(), twoStoryHomeS, tallBuildingT, 12.5f, 0, 31.5f, 2f, 90f);
        twoStoryHome4 = spawnObject(GameObject.root(), twoStoryHomeS, tallBuildingT, 12.5f, 0, 36.5f, 2f, 90f);
        twoStoryHome5 = spawnObject(GameObject.root(), twoStoryHomeS, tallBuildingT, 35f, 0, 10, 2f, 0f);
        twoStoryHome6 = spawnObject(GameObject.root(), twoStoryHomeS, tallBuildingT, 38f, 0, 0, 2f, 270f);

        twoStoryRoof1 = spawnObject(GameObject.root(), twoStoryRoofS, twoStoryWideT1, 25, 0, 21.5f, 2f, 270f);
        twoStoryRoof2 = spawnObject(GameObject.root(), twoStoryRoofS, twoStoryWideT1, 25, 0, 26.5f, 2f, 270f);
        twoStoryRoof3 = spawnObject(GameObject.root(), twoStoryRoofS, twoStoryWideT1, 25, 0, 31.5f, 2f, 270f);
        twoStoryRoof4 = spawnObject(GameObject.root(), twoStoryRoofS, twoStoryWideT1, 25, 0, 36.5f, 2f, 270f);
        twoStoryRoof5 = spawnObject(GameObject.root(), twoStoryRoofS, twoStoryWideT1, 30, 0, 20, 2f, 180f);
        twoStoryRoof6 = spawnObject(GameObject.root(), twoStoryRoofS, twoStoryWideT1, 35, 0, 20, 2f, 180f);
        twoStoryRoof7 = spawnObject(GameObject.root(), twoStoryRoofS, twoStoryWideT1, 38, 0, -5, 2f, 270f);

        // market buildings with signs by tower
        threeStoryBalcony1 = spawnObject(GameObject.root(), threeStoryBalconyS, twoStoryWideT1, -15, 0, 10, 2f, 180f);
        pharmacySign1 = spawnObject(threeStoryBalcony1, pharmacySignS, casinoSignT, 0, 8.55f, -1.9f, 1f, 0f);
        twoStoryBalcony1 = spawnObject(GameObject.root(), twoStoryBalconyS, twoStoryWideT1, -10, 0, 10, 2f, 180f);
        hardwareSign1 = spawnObject(twoStoryBalcony1, hardwareSignS, casinoSignT, 0, 5.7f, -1.9f, 1f, 0f);
        twoStoryColumns1 = spawnObject(GameObject.root(), twoStoryColumnsS, twoStoryWideT1, -5, 0, 10, 2f, 180f);
        barbershopSign1 = spawnObject(twoStoryColumns1, barbershopSignS, casinoSignT, 0, 5.6f, -2.4f, 1f, 0f);
        oneStory1 = spawnObject(GameObject.root(), oneStoryS, twoStoryWideT1, 0, 0, 10, 2f, 180f);
        bakerySign1 = spawnObject(oneStory1, bakerySignS, casinoSignT, 0, 3.8f, -1.9f, 1f, 0f);
        twoStoryDouble1 = spawnObject(GameObject.root(), twoStoryDoubleS, twoStoryWideT1, 5.7f, 0, 10, 2f, 180f);
        bookshopSign1 = spawnObject(twoStoryDouble1, bookshopSignS, casinoSignT, -0.45f, 6.4f, -1.9f, 1f, 0f);

        // other market buildings with signs
        threeStoryBalcony2 = spawnObject(GameObject.root(), threeStoryBalconyS, twoStoryWideT1, 38, 0, -20, 2f, 270f);
        pharmacySign2 = spawnObject(threeStoryBalcony2, pharmacySignS, casinoSignT, -1.9f, 8.55f, 0f, 1f, 0f);
        twoStoryBalcony2 = spawnObject(GameObject.root(), twoStoryBalconyS, twoStoryWideT1, 38, 0, -25, 2f, 270f);
        hardwareSign2 = spawnObject(twoStoryBalcony2, hardwareSignS, casinoSignT, -1.9f, 5.7f, 0f, 1f, 0f);
        twoStoryColumns2 = spawnObject(GameObject.root(), twoStoryColumnsS, twoStoryWideT1, 38, 0, -30, 2f, 270f);
        barbershopSign2 = spawnObject(twoStoryColumns2, barbershopSignS, casinoSignT, -2.4f, 5.6f, 0f, 1f, 0f);
        oneStory2 = spawnObject(GameObject.root(), oneStoryS, twoStoryWideT1, 35, 0, -36, 2f, 315f);
        bakerySign2 = spawnObject(oneStory2, bakerySignS, casinoSignT, -1.2f, 3.8f, 1.4f, 1f, 0f);
        twoStoryDouble2 = spawnObject(GameObject.root(), twoStoryDoubleS, twoStoryWideT1, 28.25f, 0, -38f, 2f, 0f);
        bookshopSign2 = spawnObject(twoStoryDouble2, bookshopSignS, casinoSignT, 0.45f, 6.4f, 1.9f, 1f, 0f);

        // terrain
        Matrix4f initialTranslation, initialScale;
        terr = new GameObject(GameObject.root(), terrS, road);
        initialTranslation = (new Matrix4f()).translation(0f,0f,0f);
        terr.setLocalTranslation(initialTranslation);
        initialScale = (new Matrix4f()).scaling(65.0f, 12.0f, 65.0f);
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

        Vector3f casinoLoc = casino.getWorldLocation();
        casinoLoc.y += 10f;
        casinoLight = setLight(casinoLoc, 5f, 0f, 0f);

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
        ambientSound.setLocation(avatar.getWorldLocation());
        casinoSound.setLocation(casino.getWorldLocation());
        setEarParameters();
        carEngineSound.play();
        ambientSound.play();
        casinoSound.play();

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

        Quaternionf objRotation = new Quaternionf();
        obj.getWorldRotation().getNormalizedRotation(objRotation);

        Vector3f offset = new Vector3f(xO, yO, zO);

        offset.rotate(objRotation);

        // location + offset
        Vector3f finalLoc = new Vector3f(loc).add(offset);

        (engine.getSceneGraph()).addPhysicsBox(m, finalLoc, objRotation, size);
    }

    @Override
    public void initializePhysicsObjects() {
        // terrain mesh
        (engine.getSceneGraph()).addPhysicsStaticTerrainMesh(
            new Vector3f(0f, 0f, 0f), new Quaternionf(), hills, 65.0f, 12.0f, 100);

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

        // Two-story wide building
        addHitbox(twoStoryWide1, 0, 13.3f, 5f, 4.05f, 0f, 2.5f, 0f);
        addHitbox(twoStoryWide2, 0, 13.3f, 5f, 4.05f, 0f, 2.5f, 0f);
        addHitbox(twoStoryWide3, 0, 13.3f, 5f, 4.05f, 0f, 2.5f, 0f);
        addHitbox(twoStoryWide4, 0, 13.3f, 5f, 4.05f, 0f, 2.5f, 0f);
        addHitbox(twoStoryWide5, 0, 13.3f, 5f, 4.05f, 0f, 2.5f, 0f);
        addHitbox(twoStoryWide6, 0, 13.3f, 5f, 4.05f, 0f, 2.5f, 0f);

        // house building
        addHitbox(house1, 0, 4.5f, 4f, 4.5f, 0f, 2f, 0f);
        addHitbox(house2, 0, 4.5f, 4f, 4.5f, 0f, 2f, 0f);
        addHitbox(house3, 0, 4.5f, 4f, 4.5f, 0f, 2f, 0f);
        addHitbox(house4, 0, 4.5f, 4f, 4.5f, 0f, 2f, 0f);
        addHitbox(house5, 0, 4.5f, 4f, 4.5f, 0f, 2f, 0f);

        // two-story homes
        addHitbox(twoStoryHome1, 0, 4.5f, 5f, 4.5f, 0f, 2.5f, 0f);
        addHitbox(twoStoryHome2, 0, 4.5f, 5f, 4.5f, 0f, 2.5f, 0f);
        addHitbox(twoStoryHome3, 0, 4.5f, 5f, 4.5f, 0f, 2.5f, 0f);
        addHitbox(twoStoryHome4, 0, 4.5f, 5f, 4.5f, 0f, 2.5f, 0f);
        addHitbox(twoStoryHome5, 0, 4.5f, 5f, 4.5f, 0f, 2.5f, 0f);
        addHitbox(twoStoryHome6, 0, 4.5f, 5f, 4.5f, 0f, 2.5f, 0f);

        // two-story homes with roof
        addHitbox(twoStoryRoof1, 0, 4.5f, 5f, 4.5f, 0f, 2.5f, 0f);
        addHitbox(twoStoryRoof2, 0, 4.5f, 5f, 4.5f, 0f, 2.5f, 0f);
        addHitbox(twoStoryRoof3, 0, 4.5f, 5f, 4.5f, 0f, 2.5f, 0f);
        addHitbox(twoStoryRoof4, 0, 4.5f, 5f, 4.5f, 0f, 2.5f, 0f);
        addHitbox(twoStoryRoof5, 0, 4.5f, 5f, 4.5f, 0f, 2.5f, 0f);
        addHitbox(twoStoryRoof6, 0, 4.5f, 5f, 4.5f, 0f, 2.5f, 0f);
        addHitbox(twoStoryRoof7, 0, 4.5f, 5f, 4.5f, 0f, 2.5f, 0f);

        // market buildings
        addHitbox(threeStoryBalcony1, 0, 5f, 5f, 4.7f, .25f, 2.5f, 0.15f);
        addHitbox(twoStoryBalcony1, 0, 5f, 5f, 4.65f, .25f, 2.5f, 0.15f);
        addHitbox(twoStoryColumns1, 0, 5f, 5f, 4.65f, .25f, 2.5f, 0.15f);
        addHitbox(oneStory1, 0, 5f, 5f, 4.65f, .25f, 2.5f, 0.15f);
        addHitbox(twoStoryDouble1, 0, 5.5f, 5f, 4.65f, .7f, 2.5f, 0.15f);

        addHitbox(threeStoryBalcony2, 0, 5f, 5f, 4.7f, .25f, 2.5f, 0.15f);
        addHitbox(twoStoryBalcony2, 0, 5f, 5f, 4.65f, .25f, 2.5f, 0.15f);
        addHitbox(twoStoryColumns2, 0, 5f, 5f, 4.65f, .25f, 2.5f, 0.15f);
        addHitbox(oneStory2, 0, 5f, 5f, 4.65f, .25f, 2.5f, 0.15f);
        addHitbox(twoStoryDouble2, 0, 5.5f, 5f, 4.65f, .7f, 2.5f, 0.15f);

        // house with roof building
        addHitbox(rHome1, 0, 4.5f, 4f, 4.5f, 0f, 2f, 0f);
        addHitbox(rHome2, 0, 4.5f, 4f, 4.5f, 0f, 2f, 0f);
        addHitbox(rHome3, 0, 4.5f, 4f, 4.5f, 0f, 2f, 0f);
        addHitbox(rHome4, 0, 4.5f, 4f, 4.5f, 0f, 2f, 0f);
        addHitbox(rHome5, 0, 4.5f, 4f, 4.5f, 0f, 2f, 0f);
        addHitbox(rHome6, 0, 4.5f, 4f, 4.5f, 0f, 2f, 0f);

        // car hitbox
        float[] carSize = {1f, 0.6f, 2.5f};
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
            setEarParameters();
            if (!isSpectating) {
                carController.beginFrame();
                im.update((float) moveTime);

                PhysicsObject carPhysics = avatar.getPhysicsObject();
                carController.update((float) moveTime, carPhysics, terr);

                (engine.getSceneGraph()).getPhysicsEngine().update((float) moveTime);
                (engine.getSceneGraph()).getPhysicsEngine().detectCollisions();

                checkZombieKills();

                // engine pitch
                float speed = Math.abs(carController.getCurrentSpeed());
                float maxSpeed = 14.0f; // Matches MAX_SPEED in CarController

                // starts pitch at 1.0 (idle) and reaches 2.0 at max speed
                float enginePitch = 1.0f + (speed / maxSpeed);
                carEngineSound.setPitch(enginePitch);

                if (carPhysics != null) {
                    Vector3f physLoc = carPhysics.getLocation();
                    Quaternionf avatarRot = new Quaternionf();
                    avatar.getWorldRotation().getNormalizedRotation(avatarRot);
                    Vector3f sideShift = new Vector3f(-0.03f, 0f, 0f).rotate(avatarRot); // fixes hitbox being too far right
                    avatar.setLocalLocation(new Vector3f(physLoc.x + sideShift.x, physLoc.y + 0.05f, physLoc.z + sideShift.z));
                    carPhysics.setTransform(physLoc, avatarRot);
                    carPhysics.setAngularVelocity(new float[]{0f, 0f, 0f});

                    float[] v = carPhysics.getLinearVelocity();
                    carEngineSound.setVelocity(new Vector3f(v[0], v[1], v[2]));
                }

                carEngineSound.setLocation(avatar.getWorldLocation());
                ambientSound.setLocation(tallBuilding.getWorldLocation());

                orbitController.updateCameraPosition();

                boolean isMoving = Math.abs(carController.getCurrentSpeed()) > 0.01f;
                if (isMoving) {
                    updateHeadlights();
                    wasMoving = true;
                } else if (wasMoving) {
                    updateHeadlights();
                    wasMoving = false;
                }

            } else {
                // Spectate mode — follow first alive ghost
                for (GhostAvatar ghost : gm.getGhostAvatars()) {
                    if (ghost.getRenderStates().renderingEnabled()) {
                        Vector3f ghostPos = ghost.getWorldLocation();
                        Vector3f ghostFwd = ghost.getWorldForwardVector();
                        Vector3f camPos = new Vector3f(ghostPos)
                                .sub(new Vector3f(ghostFwd).mul(8f))
                                .add(0f, 5f, 0f);
                        engine.getRenderSystem().getViewport("LEFT").getCamera().setLocation(camPos);
                        engine.getRenderSystem().getViewport("LEFT").getCamera().lookAt(ghost);
                        break;
                    }
                }
            }
        }
        updateHud();

        if (isMultiplayerMode) {
            processNetworking((float)moveTime);
        }

        zombieS.updateAnimation();
    }

    private void updateHud() {
        // game screen size
        int width = engine.getRenderSystem().getGLCanvas().getWidth();
        int height = engine.getRenderSystem().getGLCanvas().getHeight();

        // status message and color
        String gameStatusMsg = "";
        Vector3f statusColor = new Vector3f(0, 1, 0);

        if (isGameOver) {
            if (isGameWon){
                gameStatusMsg = "You WIN with a score of " + score + "!";
            } else {
                if (winnerScore > 0) {
                    gameStatusMsg = "GAME OVER! Winner ended with " + winnerScore + " score.";
                } else {
                    gameStatusMsg = "GAME OVER! There are no winners...";
                }
                statusColor = new Vector3f(1, 0, 0);
            }
        } else if (isSpectating) {
            statusColor = new Vector3f(1,1,1);
            gameStatusMsg = "Spectating a player";
        } else {
            if (isGameStarted) {
                float hpPercent = playerHealth.getPercent();
                if(hpPercent >= 0.75){
                    statusColor = new Vector3f(0, 1, 0);
                } else if (hpPercent >= .40){
                    statusColor = new Vector3f(1, 1, 0);
                } else {
                    statusColor = new Vector3f(1, 0, 0);
                }
                gameStatusMsg = "Health: " + playerHealth.getCurrentHP() + " / " + playerHealth.getMaxHP();
            }
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
        (engine.getHUDmanager()).setHUD3(gameStatusMsg, statusColor, (width / 2) - 65, height - 50); // top middle of window
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

    private void checkZombieKills() {
        if (protClient == null) return;
        PhysicsObject carPhysics = avatar.getPhysicsObject();
        if (carPhysics == null || Math.abs(carController.getCurrentSpeed()) < KILL_SPEED_THRESHOLD) return;

        HashSet<PhysicsObject> collided = carPhysics.getFullCollidedSet();
        if (collided.isEmpty()) return;

        Vector4f fwd = new Vector4f(0f, 0f, 1f, 1f);
        fwd.mul(avatar.getWorldRotation());
        Vector3f carFwd = new Vector3f(fwd.x(), fwd.y(), fwd.z());
        Vector3f carPos = avatar.getWorldLocation();

        List<GhostNPC> toKill = new ArrayList<>();
        for (GhostNPC npc : gm.getGhostNPCs()) {
            PhysicsObject npcPhysics = npc.getPhysicsObject();
            if (npcPhysics == null || !collided.contains(npcPhysics)) continue;
            Vector3f toNPC = new Vector3f(npc.getWorldLocation()).sub(carPos);
            if (toNPC.dot(carFwd) <= 0) continue;
            toKill.add(npc);
        }

        for (GhostNPC npc : toKill) {
            carController.skipSpeedSyncOnce();
            carController.reduceSpeed(2f);
            protClient.sendKillMessage(npc.getID());
        }
    }

    public void zombieKilled() {
        zombiesAlive = Math.max(0, zombiesAlive - 1);
    }

    public void playSplashAt(Vector3f position) {
        splashSound.setLocation(position);
        splashSound.play();
    }

    public void updatePlayerScore(UUID playerId, int newScore) {
        if (protClient != null && protClient.getID().equals(playerId)) {
            score = newScore;
        }
    }

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

    public void toggleHeadlights() {
        if(isGameStarted || !isMultiplayerMode) {
            headLightsL.toggleOnOff();
            headLightsR.toggleOnOff();
            updateHeadlights();
        }
    }

    public void readyUp() {
        if (isMultiplayerMode) {
            protClient.sendReadyMessage();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_Q:
                if (!isGameStarted || !isMultiplayerMode){
                    currSkinOpt = (currSkinOpt + 1) % avatarT.length;
                    avatar.setTextureImage(avatarT[currSkinOpt]);
                    if(isMultiplayerMode && protClient != null){
                        protClient.sendSetTexMessage(currSkinOpt);
                    }
                }
                break;
            case KeyEvent.VK_E:
                if (!isGameStarted || !isMultiplayerMode){
                    currSkinOpt = (currSkinOpt - 1 + avatarT.length) % avatarT.length;
                    avatar.setTextureImage(avatarT[currSkinOpt]);
                    if(isMultiplayerMode && protClient != null){
                        protClient.sendSetTexMessage(currSkinOpt);
                    }
                }
                break;
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
        GamepadAction toggleHeadlightsGamepad = new GamepadAction(this, 'y');
        GamepadAction readyUpGamepad = new GamepadAction(this, 'b');
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
        im.associateActionWithAllGamepads(
                net.java.games.input.Component.Identifier.Button._3, toggleHeadlightsGamepad,
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(
                net.java.games.input.Component.Identifier.Button._1, readyUpGamepad,
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
    public ObjShape getLeftTireShape() { return leftTireS; }
    public ObjShape getRightTireShape() { return rightTireS; }
    public TextureImage getGhostTexture(int opt) { return avatarT[opt]; }
    public int getCurrAvatarSkin() { return currSkinOpt; }
    public TextureImage getTireTexture(){ return tireT; }
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

    public void handleNPCattack(int npcID) {
        playerHealth.takeDamage();
        System.out.println("Hit by NPC " + npcID + "! HP: " + playerHealth.getCurrentHP());

        if (playerHealth.isDead()) {
            handleDeath();
        }
    }

    private void handleDeath() {
        isSpectating = true;
        System.out.println("You died!");

        carController.setThrottle(0f);
        hideAvatar();
        carEngineSound.stop();

        // Notify server
        protClient.sendDeathMessage();
    }
    private void hideAvatar(){
        avatar.getRenderStates().disableRendering();
        backLeftTire.getRenderStates().disableRendering();
        backRightTire.getRenderStates().disableRendering();
        frontLeftTire.getRenderStates().disableRendering();
        frontRightTire.getRenderStates().disableRendering();
    }

    protected void handleGameOver(UUID winnerID, int winnerScore){
        isGameOver = true;
        this.winnerScore = winnerScore;
        if (protClient != null && protClient.getID().equals(winnerID)){
            isGameWon = true;
        }
    }

    protected void handleGameOver() {
        isGameOver = true;
    }

    public PlayerHealth getPlayerHealth() { return playerHealth; }
    public boolean isSpectating() { return isSpectating; }
    public boolean isGameOver() { return isGameOver; }
    public boolean isGameWon() { return isGameWon; }
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

        Vector3f leftOffset  = new Vector3f(right).mul(-0.2f);
        Vector3f rightOffset = new Vector3f(right).mul( 0.2f);
        Vector3f frontOffset = new Vector3f(forward).mul(.65f);
        Vector3f upOffset    = new Vector3f(up).mul(0.1f);

        headLightsL.setLocation(new Vector3f(avatarPos).add(leftOffset).add(frontOffset).add(upOffset));
        headLightsR.setLocation(new Vector3f(avatarPos).add(rightOffset).add(frontOffset).add(upOffset));

        Vector3f lightDir = new Vector3f(forward).add(new Vector3f(up).mul(-0.2f)).normalize();
        headLightsL.setDirection(lightDir);
        headLightsR.setDirection(lightDir);
    }

    public void setThrottle(float input) {
        if (isGameStarted || !isMultiplayerMode) carController.setThrottle(input);
    }
    public void turn(float delta) {
        if (isGameStarted || !isMultiplayerMode) carController.turn(delta);
    }

}

