package net.createballistics.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.createballistics.CreateBallistics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Native Bedrock geometry parser & renderer for {@code assets/create_ballistics/geo/drone.geo.json}
 * without GeckoLib.
 */
public final class DroneModel {

    public static final Identifier TEXTURE = CreateBallistics.id("textures/entity/drone.png");
    private static final Identifier MODEL = CreateBallistics.id("geo/drone.geo.json");
    private static final String[] ROTOR_NAMES = {"wingFL", "wingFR", "wingBR", "wingBL"};

    private static volatile BakedModel cached;

    private DroneModel() {
    }

    public static void render(PoseStack.Pose rootPose, VertexConsumer consumer, int lightCoords, float rotorSpinRad) {
        BakedModel model = getOrLoad();
        if (model == null) {
            return;
        }
        emitQuads(rootPose, consumer, lightCoords, model.staticQuads);

        PoseStack rotorStack = new PoseStack();
        rotorStack.last().pose().set(rootPose.pose());
        rotorStack.last().normal().set(rootPose.normal());
        for (RotorBone rotor : model.rotors) {
            rotorStack.pushPose();
            rotorStack.translate(rotor.pivotX, rotor.pivotY, rotor.pivotZ);
            rotorStack.mulPose(Axis.YP.rotation(rotorSpinRad));
            rotorStack.translate(-rotor.pivotX, -rotor.pivotY, -rotor.pivotZ);
            emitQuads(rotorStack.last(), consumer, lightCoords, rotor.quads);
            rotorStack.popPose();
        }
    }

    private static void emitQuads(PoseStack.Pose pose, VertexConsumer consumer, int light, List<BakedQuad> quads) {
        Matrix4f posMat = pose.pose();
        Matrix3f normMat = pose.normal();
        Vector3f p = new Vector3f();
        Vector3f n = new Vector3f();
        for (BakedQuad quad : quads) {
            normMat.transform(quad.nx, quad.ny, quad.nz, n);
            for (int i = 0; i < 4; i++) {
                float x = quad.vx[i];
                float y = quad.vy[i];
                float z = quad.vz[i];
                posMat.transformPosition(x, y, z, p);
                consumer.addVertex(p.x, p.y, p.z, -1, quad.u[i], quad.v[i],
                        OverlayTexture.NO_OVERLAY, light, n.x, n.y, n.z);
            }
        }
    }

    private static BakedModel getOrLoad() {
        BakedModel existing = cached;
        if (existing != null) {
            return existing;
        }
        synchronized (DroneModel.class) {
            if (cached != null) {
                return cached;
            }
            try {
                var resource = Minecraft.getInstance().getResourceManager().getResource(MODEL);
                if (resource.isEmpty()) {
                    return null;
                }
                try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                    BakedModel loaded = parse(JsonParser.parseReader(reader).getAsJsonObject());
                    cached = loaded;
                    return loaded;
                }
            } catch (Exception e) {
                CreateBallistics.LOGGER.error("Failed to load drone model {}", MODEL, e);
                return null;
            }
        }
    }

    private static BakedModel parse(JsonObject root) {
        JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
        JsonObject geo = geometries.get(0).getAsJsonObject();
        JsonObject desc = geo.getAsJsonObject("description");
        float texW = desc.get("texture_width").getAsFloat();
        float texH = desc.get("texture_height").getAsFloat();

        Map<String, BoneEntry> bones = new LinkedHashMap<>();
        for (JsonElement el : geo.getAsJsonArray("bones")) {
            JsonObject boneObj = el.getAsJsonObject();
            String name = boneObj.get("name").getAsString();
            JsonArray pivArr = boneObj.getAsJsonArray("pivot");
            float pivX = pivArr != null ? -pivArr.get(0).getAsFloat() / 16.0F : 0.0F;
            float pivY = pivArr != null ? pivArr.get(1).getAsFloat() / 16.0F : 0.0F;
            float pivZ = pivArr != null ? pivArr.get(2).getAsFloat() / 16.0F : 0.0F;

            List<BakedQuad> boneQuads = new ArrayList<>();
            if (boneObj.has("cubes")) {
                for (JsonElement cEl : boneObj.getAsJsonArray("cubes")) {
                    addCubeQuads(cEl.getAsJsonObject(), texW, texH, boneQuads);
                }
            }
            bones.put(name, new BoneEntry(name, pivX, pivY, pivZ, boneQuads));
        }

        List<BakedQuad> staticQuads = new ArrayList<>();
        List<RotorBone> rotors = new ArrayList<>();
        Map<String, Boolean> rotorSet = Map.of(
                ROTOR_NAMES[0], true, ROTOR_NAMES[1], true, ROTOR_NAMES[2], true, ROTOR_NAMES[3], true);

        for (BoneEntry b : bones.values()) {
            if (rotorSet.containsKey(b.name)) {
                rotors.add(new RotorBone(b.pivX, b.pivY, b.pivZ, b.quads));
            } else {
                staticQuads.addAll(b.quads);
            }
        }
        return new BakedModel(staticQuads, rotors);
    }

    private static void addCubeQuads(JsonObject cube, float texW, float texH, List<BakedQuad> out) {
        JsonArray origin = cube.getAsJsonArray("origin");
        JsonArray size = cube.getAsJsonArray("size");
        float ox = origin.get(0).getAsFloat();
        float oy = origin.get(1).getAsFloat();
        float oz = origin.get(2).getAsFloat();
        float sx = size.get(0).getAsFloat();
        float sy = size.get(1).getAsFloat();
        float sz = size.get(2).getAsFloat();

        // Bedrock -> MC right-handed X-negated cube bounds
        float x0 = -(ox + sx) / 16.0F;
        float x1 = -ox / 16.0F;
        float y0 = oy / 16.0F;
        float y1 = (oy + sy) / 16.0F;
        float z0 = oz / 16.0F;
        float z1 = (oz + sz) / 16.0F;

        Matrix4f cubeMat = new Matrix4f();
        if (cube.has("rotation")) {
            JsonArray rot = cube.getAsJsonArray("rotation");
            JsonArray piv = cube.has("pivot") ? cube.getAsJsonArray("pivot") : null;
            float px = piv != null ? -piv.get(0).getAsFloat() / 16.0F : 0.0F;
            float py = piv != null ? piv.get(1).getAsFloat() / 16.0F : 0.0F;
            float pz = piv != null ? piv.get(2).getAsFloat() / 16.0F : 0.0F;
            float rx = (float) Math.toRadians(-rot.get(0).getAsFloat());
            float ry = (float) Math.toRadians(-rot.get(1).getAsFloat());
            float rz = (float) Math.toRadians(rot.get(2).getAsFloat());
            cubeMat.translate(px, py, pz)
                    .rotateZ(rz)
                    .rotateY(ry)
                    .rotateX(rx)
                    .translate(-px, -py, -pz);
        }

        JsonObject uvObj = cube.getAsJsonObject("uv");
        if (uvObj == null) {
            return;
        }
        addFaceIfPresent(uvObj, "north", texW, texH, cubeMat, out,
                x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, 0, 0, -1);
        addFaceIfPresent(uvObj, "south", texW, texH, cubeMat, out,
                x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, 0, 0, 1);
        addFaceIfPresent(uvObj, "east", texW, texH, cubeMat, out,
                x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, -1, 0, 0);
        addFaceIfPresent(uvObj, "west", texW, texH, cubeMat, out,
                x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, 1, 0, 0);
        addFaceIfPresent(uvObj, "up", texW, texH, cubeMat, out,
                x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0);
        addFaceIfPresent(uvObj, "down", texW, texH, cubeMat, out,
                x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0);
    }

    private static void addFaceIfPresent(JsonObject uvObj, String face, float texW, float texH,
                                         Matrix4f cubeMat, List<BakedQuad> out,
                                         float ax, float ay, float az,
                                         float bx, float by, float bz,
                                         float cx, float cy, float cz,
                                         float dx, float dy, float dz,
                                         float nx, float ny, float nz) {
        if (!uvObj.has(face)) {
            return;
        }
        JsonObject f = uvObj.getAsJsonObject(face);
        JsonArray uv = f.getAsJsonArray("uv");
        JsonArray uvSize = f.getAsJsonArray("uv_size");
        float u0 = uv.get(0).getAsFloat() / texW;
        float v0 = uv.get(1).getAsFloat() / texH;
        float u1 = (uv.get(0).getAsFloat() + uvSize.get(0).getAsFloat()) / texW;
        float v1 = (uv.get(1).getAsFloat() + uvSize.get(1).getAsFloat()) / texH;

        Vector3f va = cubeMat.transformPosition(new Vector3f(ax, ay, az));
        Vector3f vb = cubeMat.transformPosition(new Vector3f(bx, by, bz));
        Vector3f vc = cubeMat.transformPosition(new Vector3f(cx, cy, cz));
        Vector3f vd = cubeMat.transformPosition(new Vector3f(dx, dy, dz));
        Vector3f vn = cubeMat.transformDirection(new Vector3f(nx, ny, nz)).normalize();

        out.add(new BakedQuad(
                new float[]{va.x, vb.x, vc.x, vd.x},
                new float[]{va.y, vb.y, vc.y, vd.y},
                new float[]{va.z, vb.z, vc.z, vd.z},
                new float[]{u0, u0, u1, u1},
                new float[]{v0, v1, v1, v0},
                vn.x, vn.y, vn.z));
    }

    private record BakedQuad(float[] vx, float[] vy, float[] vz, float[] u, float[] v,
                             float nx, float ny, float nz) {}

    private record BoneEntry(String name, float pivX, float pivY, float pivZ, List<BakedQuad> quads) {}

    private record RotorBone(float pivotX, float pivotY, float pivotZ, List<BakedQuad> quads) {}

    private record BakedModel(List<BakedQuad> staticQuads, List<RotorBone> rotors) {}
}
