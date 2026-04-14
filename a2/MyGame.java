package a2;

import com.jogamp.opengl.awt.GLCanvas;
import tage.*;
import tage.input.InputManager;
import tage.nodeControllers.JumpController;
import tage.nodeControllers.RotationController;
import tage.shapes.*;

import java.awt.*;
import java.lang.Math;
import java.awt.event.*;

import org.joml.*;

import javax.swing.*;

import static tage.GameObject.spawnObject;

public class MyGame extends VariableFrameRateGame {
    private static Engine engine;
    private InputManager im;
    private CameraOrbit3D orbitController;
    private ViewportController viewportController;

    private RotationController khafreRc, khufuRc, menkaureRc;
    private JumpController houseJc;

    private GameObject dol, saddle, house, floor;
    private GameObject pyramidKhafre, pyramidKhufu, pyramidMenkaure;
    private GameObject photo1, photo2, photo3;
    private GameObject x, y, z; // world axes
    private GameObject[] pyramids, photos;

    private ObjShape dolS, saddleS, planeS, houseS, pyramidS, floorS;
    private ObjShape linxS, linyS, linzS;

    private TextureImage doltx, saddlet, brick, floorT, khafreT, khufuT;
    private TextureImage[] pyramidTextures;

    private Light light1, khafreLight, khufuLight, menkaureLight;

    private int score = 0;
    private double lastFrameTime, currFrameTime, elapsTime;
    private boolean canTransfer = false, isGameOver = false, isGameWon = false, showAxes = false;
    private boolean[] isPhotoTaken = {false, false, false};
    private static final int FLOOR_TEXTURE_TILE = 20;

    private String actionMsg = "";
    private float actionTimer = 0.0f;

    private GLCanvas canvas;
    private Robot robot;
    private float curMouseX, curMouseY, centerX, centerY;
    private float prevMouseX, prevMouseY;
    private boolean isRecentering;
    private boolean mouseModeInitiated = false;

    public MyGame() {
        super();
    }

    public static void main(String[] args) {
        MyGame game = new MyGame();
        engine = new Engine(game);
        engine.initializeSystem();
        game.buildGame();
        game.startGame();
    }

    @Override
    public void loadShapes() {
        dolS = new ImportedModel("Car.obj");
        saddleS = new ImportedModel("Tire.obj");
        houseS = new DolphinHouse();

        pyramidS = new ManualPyramid();
        planeS = new Plane(); // for pictures
        floorS = new Plane();

        // world axes
        linxS = new Line(new Vector3f(0f, 0.01f, 0f), new Vector3f(3f, 0.01f, 0f));
        linyS = new Line(new Vector3f(0f, 0.01f, 0f), new Vector3f(0f, 3.01f, 0f));
        linzS = new Line(new Vector3f(0f, 0.01f, 0f), new Vector3f(0f, 0.01f, -3f));
    }

    @Override
    public void loadTextures() {
        doltx = new TextureImage("CarTexture.png");
        saddlet = new TextureImage("TireTexture.png");

        khafreT = new TextureImage("khafreTexture.jpg");
        brick = new TextureImage("brick1.jpg");
        khufuT = new TextureImage("khufuTexture.png");
        pyramidTextures = new TextureImage[]{khafreT, khufuT, brick};

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

        // spawns dolphin in the center of the window just above the floor
        dol = spawnObject(GameObject.root(), dolS, doltx, 0f, 1f, 0f, 3.0f, 135.0f);
        saddle = spawnObject(dol, saddleS, saddlet, 0f, 0f, 0f, 1f, 0f);

        // spawn dolphins home
        house = spawnObject(GameObject.root(), houseS, brick, 18f, 2.01f, 2f, 2f, 0f);

        // spawn three pyramids
        pyramidKhafre = spawnObject(GameObject.root(), pyramidS, khafreT, 70f, 13.01f, 65f, 13f, 0f);
        pyramidKhufu = spawnObject(GameObject.root(), pyramidS, khufuT, -15f, 18.01f, -75f, 18f, 0f);
        pyramidMenkaure = spawnObject(GameObject.root(), pyramidS, brick, -55f, 8.01f, 35f, 8f, 0f);
        pyramids = new GameObject[]{pyramidKhafre, pyramidKhufu, pyramidMenkaure};

        // spawn three pyramid photos
        photo1 = initPhoto();
        photo2 = initPhoto();
        photo3 = initPhoto();
        photos = new GameObject[]{photo1, photo2, photo3};
    }

    // helper for initializing photo objects
    private GameObject initPhoto() {
        GameObject photo = spawnObject(dol, planeS, null, 0, 0f, 0f, 0.025f, 0f);
        photo.setLocalRotation(new Matrix4f().rotationX(235.0f));
        photo.getRenderStates().disableRendering();
        return photo;
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

        controlActions();

        // rotation controller
        khafreRc = new RotationController(engine, new Vector3f(0, 1, 0), 0.001f);
        khufuRc = new RotationController(engine, new Vector3f(0, 1, 0), 0.001f);
        menkaureRc = new RotationController(engine, new Vector3f(0, 1, 0), 0.001f);
        khafreRc.addTarget(pyramidKhafre);
        khufuRc.addTarget(pyramidKhufu);
        menkaureRc.addTarget(pyramidMenkaure);
        (engine.getSceneGraph()).addNodeController(khafreRc);
        (engine.getSceneGraph()).addNodeController(khufuRc);
        (engine.getSceneGraph()).addNodeController(menkaureRc);

        // jump controller
        houseJc = new JumpController(engine);
        houseJc.addTarget(house);
        (engine.getSceneGraph()).addNodeController(houseJc);
    }

    @Override
    public void createViewports() {
        viewportController = new ViewportController(engine);
        viewportController.setupViewports();
    }

    @Override
    public void update() {
        if (!mouseModeInitiated) initMouseMode();

        if (!isGameOver) {
            lastFrameTime = currFrameTime;
            currFrameTime = System.currentTimeMillis();
            double moveTime = (currFrameTime - lastFrameTime) / 1000.0;
            elapsTime += moveTime;

            im.update((float) moveTime);
            orbitController.updateCameraPosition();

            if (!isGameWon) updatePhotoPositions();
        }

        updateHud();
    }

    private void updatePhotoPositions() {
        Vector3f fwd = dol.getWorldForwardVector();
        Vector3f up = dol.getWorldUpVector();
        Vector3f right = dol.getWorldRightVector();

        photo1.setLocalLocation(new Vector3f(up).mul(0.7f).add(new Vector3f(fwd).mul(-1.0f)).add(new Vector3f(right).mul(-0.2f)));
        photo2.setLocalLocation(new Vector3f(up).mul(0.7f).add(new Vector3f(fwd).mul(-1.0f)).add(new Vector3f(right).mul(0.0f)));
        photo3.setLocalLocation(new Vector3f(up).mul(0.7f).add(new Vector3f(fwd).mul(-1.0f)).add(new Vector3f(right).mul(0.2f)));
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
            float dist = getPyramidDistance();
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
        Vector3f dolLoc = dol.getWorldLocation();
        int xloc = (int) dolLoc.x;
        int zloc = (int) dolLoc.z;
        int yloc = (int) dolLoc.y;
        (engine.getHUDmanager()).setHUD5("X:" + xloc + " Y:" + yloc + " Z:" + zloc, new Vector3f(1, 1, 1), vrX, vrY);
    }

    // check if player is close enough to a pyramid to take a picture
    public void takePyramidPicture() {
        int i = getClosestPyramid();
        String[] pyramidNames = {"Khafre", "Khufu", "Menkaure"};

        float pyramidDistance = getPyramidDistance();

        float photoRange = 10.0f;

        if (pyramidDistance < photoRange) {
            if (isPhotoTaken[i]) {
                displayAction("You already took a picture of " + pyramidNames[i] + "!", 2.0f);
            } else {
                isPhotoTaken[i] = true;
                score++;
                displayAction("Snap! Picture taken of: " + pyramidNames[i], 2.0f);
                updatePhotoTexture(i, pyramidTextures[i]);

                RotationController[] rcs = {khafreRc, khufuRc, menkaureRc};
                rcs[i].enable();
            }
        } else {
            displayAction("Not close enough to " + pyramidNames[i] + " for a photo.", 2.0f);
        }
    }

    public void tryTransferPictures() {
        if (canTransfer && wonGame()) {
            isGameWon = true;
            transferPictures();
            houseJc.enable();
        } else if (!canTransfer) {
            displayAction("Get back home to transfer!", 2.0f);
        } else {
            displayAction("You need to photograph the 3 pyramids first!", 2.0f);
        }
    }

    // transfers pictures into the dolphins house if player got all 3
    public void transferPictures() {
        if (score == 3) {
            for (int i = 0; i < photos.length; i++) {
                photos[i].setParent(house);

                float xOffset = 1.9f;
                float yOffset = 0.2f;
                float zOffset = -1f + (i * 1f);

                photos[i].setLocalLocation(new Vector3f(xOffset, yOffset, zOffset));

                photos[i].setLocalRotation(
                        new Matrix4f()
                                .rotationX((float) Math.toRadians(90.0f))
                                .rotateZ((float) Math.toRadians(90.0f))
                );
                photos[i].setLocalScale(new Matrix4f().scaling(0.2f));
            }
        }
    }

    public boolean isMovSafe(Vector3f newPosition) {
        float length = 2.4f;
        float width = 0.8f;

        Vector3f fwd = dol.getWorldForwardVector();
        Vector3f right = dol.getWorldRightVector();

        Vector3f center = new Vector3f(newPosition);
        Vector3f snout = new Vector3f(fwd).mul(length).add(newPosition);
        Vector3f tail = new Vector3f(fwd).mul(-length).add(newPosition);
        Vector3f rightFin = new Vector3f(right).mul(width).add(newPosition);
        Vector3f leftFin = new Vector3f(right).mul(-width).add(newPosition);

        Vector3f[] dolphinHitbox = {center, snout, tail, rightFin, leftFin};

        for (GameObject pyramid : pyramids) {
            if (isHitboxColliding(dolphinHitbox, pyramid, 1.0f)) {
                isGameOver = true;
                return false;
            }
        }

        canTransfer = isHitboxColliding(dolphinHitbox, house, 3.0f);
        return !isHitboxColliding(dolphinHitbox, house, 1.2f); // move is safe
    }

    // helper to check if dolphin is colliding with an object
    private boolean isHitboxColliding(Vector3f[] hitbox, GameObject target, float scaleMultiplier) {
        Vector3f loc = target.getWorldLocation();
        float halfWidth = target.getWorldScale().get(0, 0) * scaleMultiplier;

        float minX = loc.x - halfWidth;
        float maxX = loc.x + halfWidth;
        float minZ = loc.z - halfWidth;
        float maxZ = loc.z + halfWidth;

        for (Vector3f point : hitbox) {
            if (point.x > minX && point.x < maxX && point.z > minZ && point.z < maxZ) {
                return true;
            }
        }
        return false;
    }

    // calculate distance between dolphin and pyramid
    public float getPyramidDistance() {
        int pyramidIndex = getClosestPyramid();
        Vector3f dolLoc = dol.getWorldLocation();
        Vector3f pyrLoc = pyramids[pyramidIndex].getWorldLocation();

        float halfWidth = pyramids[pyramidIndex].getWorldScale().get(0, 0);

        float nearestX = Math.max(pyrLoc.x - halfWidth, Math.min(dolLoc.x, pyrLoc.x + halfWidth));
        float nearestZ = Math.max(pyrLoc.z - halfWidth, Math.min(dolLoc.z, pyrLoc.z + halfWidth));

        float dx = dolLoc.x - nearestX;
        float dz = dolLoc.z - nearestZ;
        float distToEdge = (float) Math.sqrt(dx * dx + dz * dz);

        float dolphinRadius = 2.4f;
        float surfaceDist = distToEdge - dolphinRadius;
        if (surfaceDist < 0) surfaceDist = 0;

        return surfaceDist;
    }

    // helper to get the closest pyramid's index
    private int getClosestPyramid() {
        int closestIndex = 0;
        float minDist = dol.getWorldLocation().distance(pyramids[0].getWorldLocation());

        for (int i = 1; i < pyramids.length; i++) {
            float dist = dol.getWorldLocation().distance(pyramids[i].getWorldLocation());
            if (dist < minDist) {
                minDist = dist;
                closestIndex = i;
            }
        }
        return closestIndex;
    }

    private void updatePhotoTexture(int i, TextureImage tex) {
        photos[i].setTextureImage(tex);
        photos[i].getRenderStates().enableRendering();
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

    private boolean wonGame() {
        return score == 3;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!isGameOver) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_SPACE:
                    tryTransferPictures();
                    break;
                case KeyEvent.VK_P:
                    takePyramidPicture();
                    break;
                case KeyEvent.VK_X:
                    toggleAxes();
                    break;
            }
        }
        super.keyPressed(e);
    }

    private void controlActions() {
        im = engine.getInputManager();
        String gpName = im.getFirstGamepadName();
        Camera leftCam = viewportController.getLeftCamera();
        orbitController = new CameraOrbit3D(leftCam, dol, gpName, engine);

        // gamepad controls
        GamepadAction takePhotoGamepad = new GamepadAction(this, 'y');
        GamepadAction toggleAxesGamepad = new GamepadAction(this, 'x');
        GamepadAction transferPicturesGamepad = new GamepadAction(this, 'b');
        MovAction movGamepad = new MovAction(this, -1.0f);
        TurnAction turnGamepad = new TurnAction(this, -1.0f);

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
                net.java.games.input.Component.Identifier.Button._3, takePhotoGamepad,
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateActionWithAllGamepads(
                net.java.games.input.Component.Identifier.Button._1, transferPicturesGamepad,
                InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        // keyboard controls
        TurnAction turnLeftAction = new TurnAction(this, 1.0f);
        TurnAction turnRightAction = new TurnAction(this, -1.0f);
        MovAction fwdAction = new MovAction(this, 1.0f);
        MovAction backAction = new MovAction(this, -1.0f);

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
    public void mouseMoved(MouseEvent e) {    // if robot is recentering and the MouseEvent location is in the center,
        // then this event was generated by the robot
        if (mouseModeInitiated) {
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

    private void recenterMouse() {    // use the robot to move the mouse to the center point.
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
        return dol;
    }

    public Engine getEngine() {
        return engine;
    }
}