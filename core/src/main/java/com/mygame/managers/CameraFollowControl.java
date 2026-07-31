package com.mygame.managers;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;

/**
 * Control для плавного следования камеры за узлом.
 * Добавляется на узел персонажа ПОСЛЕ BetterCharacterControl,
 * чтобы обновляться после физики.
 */
public class CameraFollowControl extends AbstractControl {
    private final Camera cam;
    private final Vector3f offset;
    private final Vector3f target = new Vector3f();

    public CameraFollowControl(Camera cam, Vector3f offset) {
        this.cam = cam;
        this.offset = offset;
    }

    @Override
    protected void controlUpdate(float tpf) {
        Spatial player = getSpatial();
        if (player == null) return;
        
        target.set(player.getWorldTranslation()).addLocal(offset);
        cam.setLocation(target);
        cam.lookAt(player.getWorldTranslation(), Vector3f.UNIT_Y);
    }

    @Override
    protected void controlRender(com.jme3.renderer.RenderManager rm, com.jme3.renderer.ViewPort vp) {}

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        return new CameraFollowControl(cam, offset);
    }
}