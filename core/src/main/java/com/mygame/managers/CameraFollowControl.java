package com.mygame.managers;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;

public class CameraFollowControl extends AbstractControl {
    private Camera cam;
    private Node target;
    private float distance = 18f;
    private float height = 22f;
    private float cameraAngle = 0f;
    private float smoothness = 0.15f;
    private Vector3f targetOffset = new Vector3f(0, -0.5f, 0);

    public CameraFollowControl(Camera cam, Node target) {
        this.cam = cam;
        this.target = target;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (target == null || cam == null) return;
if (!enabled) return;
        Vector3f targetPos = target.getWorldTranslation();
        if (targetPos == null) return;

        float x = FastMath.sin(cameraAngle) * distance;
        float z = FastMath.cos(cameraAngle) * distance;
        Vector3f desiredPos = targetPos.add(new Vector3f(x, height, z));

        // Плавное следование
        Vector3f currentPos = cam.getLocation();
        desiredPos.interpolateLocal(currentPos, smoothness);

        cam.setLocation(desiredPos);
        Vector3f lookTarget = targetPos.add(targetOffset);
        cam.lookAt(lookTarget, Vector3f.UNIT_Y);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // Не нужен
    }

    public void setCameraAngle(float angle) {
        this.cameraAngle = angle;
    }
    
    private boolean enabled = true;

public void setEnabled(boolean enabled) {
    this.enabled = enabled;
}

public boolean isEnabled() {
    return enabled;
}
}