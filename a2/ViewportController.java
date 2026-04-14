package a2;

import org.joml.Vector3f;
import tage.Camera;
import tage.Engine;
import tage.Viewport;

public class ViewportController {
    private Engine engine;
    private Camera leftCamera;
    private Camera rightCamera;

    public ViewportController(Engine e) {
        this.engine = e;
    }

    public void setupViewports()
    { (engine.getRenderSystem()).addViewport("LEFT",0,0,1f,1f);
        (engine.getRenderSystem()).addViewport("RIGHT",.75f,0,.25f,.25f);
        Viewport leftVp = (engine.getRenderSystem()).getViewport("LEFT");
        Viewport rightVp = (engine.getRenderSystem()).getViewport("RIGHT");
        leftCamera = leftVp.getCamera();
        rightCamera = rightVp.getCamera();
        rightVp.setHasBorder(true);
        rightVp.setBorderWidth(4);
        rightVp.setBorderColor(0.0f, 1.0f, 0.0f);
        leftCamera.setLocation(new Vector3f(-2,0,2));
        leftCamera.setU(new Vector3f(1,0,0));
        leftCamera.setV(new Vector3f(0,1,0));
        leftCamera.setN(new Vector3f(0,0,-1));
        rightCamera.setLocation(new Vector3f(0,2,0));
        rightCamera.setU(new Vector3f(1,0,0));
        rightCamera.setV(new Vector3f(0,0,-1));
        rightCamera.setN(new Vector3f(0,-1,0));
    }

    public Camera getLeftCamera() {
        return leftCamera;
    }

    public Camera getRightCamera() {
        return rightCamera;
    }
}
