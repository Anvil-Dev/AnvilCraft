package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.opengl.GlStateManager;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class MunShadowMeshProbe {
    public static void verify() {
        try (var ignored = new MunShadowGlScope()) {
            geometry();
            models();
            fluids();
            colorsAndDraw();
        }
        MunShadowMesh.clearCaches();
        AnvilCraft.LOGGER.info("PORT_MUN_SHADOW_MESH_PASSED: geometry, budgets, model context, ARGB, GPU attributes and disposal");
    }

    private static void geometry() {
        try (var builder = new MunShadowMesh.Builder(36)) {
            check(builder.finish() == null, "Empty mesh allocated GPU data");
        }
        AABB box = new AABB(-3, 7, 2, 5, 7.5, 6);
        try (var builder = new MunShadowMesh.Builder(36)) {
            builder.box(box);
            builder.box(box);
            try (var mesh = Objects.requireNonNull(builder.finish())) {
                check(mesh.vertexCount() == 36 && mesh.bounds().equals(box), "Box bounds or budget changed");
                inspect(mesh);
                int buffer = field(mesh, "buffer");
                int vao = field(mesh, "vertexArray");
                mesh.close();
                mesh.close();
                check(!GL15C.glIsBuffer(buffer) && !GL30C.glIsVertexArray(vao), "Mesh resource leaked");
                boolean rejected = false;
                try {
                    mesh.draw();
                } catch (IllegalStateException expected) {
                    rejected = true;
                }
                check(rejected, "Closed mesh remained drawable");
            }
        }
        int[] columns = new int[256];
        Arrays.fill(columns, -1);
        try (var builder = new MunShadowMesh.Builder(4096)) {
            for (int slice = 0; !builder.columns(columns, 32, slice); slice++) {}
            try (var mesh = Objects.requireNonNull(builder.finish())) {
                check(mesh.vertexCount() == 36, "Solid column volume was not merged to six faces");
                check(mesh.bounds().equals(new AABB(0, 0, 0, 16, 32, 16)), "Column coordinates changed");
                inspect(mesh);
            }
        }
    }

    private static void models() {
        var view = Objects.requireNonNull(Minecraft.getInstance().level);
        var stone = Blocks.STONE.defaultBlockState();
        final var slab = Blocks.STONE_SLAB.defaultBlockState();
        check(MunShadowMesh.fullCube(stone, view, BlockPos.ZERO), "Stone was not recognized as a cube");
        for (var block : List.of(Blocks.STONE_SLAB, Blocks.STONE_STAIRS, Blocks.OAK_FENCE)) {
            check(!MunShadowMesh.fullCube(block.defaultBlockState(), view, BlockPos.ZERO), "Non-cube was collapsed to a cube");
        }
        check(!MunShadowMesh.castsShadow(Blocks.GLASS.defaultBlockState(), view, BlockPos.ZERO), "Glass became opaque");
        check(MunShadowMesh.castsShadow(Blocks.TINTED_GLASS.defaultBlockState(), view, BlockPos.ZERO),
            "Tinted glass stopped blocking light");
        var stoneModel = new SingleVariant(parts(model(stone), stone).getFirst());
        var slabModel = new SingleVariant(parts(model(slab), slab).getFirst());
        check(height(stoneModel, stone, BlockPos.ZERO) == 1, "Stone model height changed");
        check(height(slabModel, stone, BlockPos.ZERO) == 0.5, "Model cache reused geometry from the same block state");
        BlockStateModel contextual = new BlockStateModel() {
            @Override
            public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
                throw new IllegalStateException("Context-free model query");
            }

            @Override
            public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
                                     RandomSource random, List<BlockStateModelPart> output) {
                check(level == view && state == stone, "Model query lost world or state");
                output.addAll(parts(pos.getX() == 0 ? stoneModel : slabModel, state));
            }

            @Override
            public Material.Baked particleMaterial() {
                return stoneModel.particleMaterial();
            }

            @Override
            public int materialFlags() {
                return stoneModel.materialFlags();
            }
        };
        check(height(contextual, stone, BlockPos.ZERO) == 1, "Contextual stone changed");
        check(height(contextual, stone, new BlockPos(1, 0, 0)) == 0.5, "Contextual model was cached across positions");
        try (var builder = new MunShadowMesh.Builder(35)) {
            builder.model(stoneModel, stone, BlockPos.ZERO, Vec3.ZERO, true, view, false);
            check(builder.finish() == null, "Over-budget model was partially emitted");
        }
        var quad = parts(stoneModel, stone).getFirst().getQuads(Direction.UP).getFirst();
        var collection = new QuadCollection.Builder();
        for (int i = 0; i < 1025; i++) collection.addUnculledFace(quad);
        var huge = new SingleVariant(new SimpleModelWrapper(collection.build(), true, stoneModel.particleMaterial()));
        try (var builder = new MunShadowMesh.Builder(10000)) {
            builder.model(huge, stone, BlockPos.ZERO, Vec3.ZERO, true, view, false);
            check(builder.finish() == null, "Model quad cap was bypassed");
        }
    }

    private static void fluids() {
        for (int budget : new int[]{6, 256}) {
            try (var builder = new MunShadowMesh.Builder(budget)) {
                builder.fluid(BlockAndTintGetter.EMPTY, Blocks.WATER.defaultBlockState(), new BlockPos(3, 35, 5), -64);
                try (var mesh = Objects.requireNonNull(builder.finish())) {
                    var box = mesh.bounds();
                    check(box.minX >= 3 && box.maxX <= 4 && box.minZ >= 5 && box.maxZ <= 6,
                        "Fluid chunk-relative X/Z changed");
                    check(box.minY >= 99 && box.maxY <= 100, "Fluid origin Y offset changed");
                    check(mesh.vertexCount() <= budget && mesh.vertexCount() % 6 == 0, "Fluid budget split a triangle pair");
                    inspect(mesh);
                }
            }
        }
    }

    private static void colorsAndDraw() {
        var state = Blocks.STONE.defaultBlockState();
        var part = parts(model(state), state).getFirst();
        var source = part.getQuads(Direction.UP).getFirst();
        var colored = new BakedQuad(source.position0(), source.position1(), source.position2(), source.position3(),
            source.packedUV0(), source.packedUV1(), source.packedUV2(), source.packedUV3(), source.direction(),
            source.materialInfo(), source.bakedNormals(), new BakedColors.PerQuad(0x80CC8040));
        final var model = new SingleVariant(new SimpleModelWrapper(new QuadCollection.Builder().addUnculledFace(colored).build(),
            true, part.particleMaterial()));
        int vertex = shader(GL20C.GL_VERTEX_SHADER, """
            #version 150
            in vec3 Position;
            in vec2 UV0;
            in vec4 Color;
            out vec4 color;
            void main() { gl_Position = vec4(Position.xz * 2.0 - 1.0, 0.0, 1.0); color = Color; }
            """);
        int fragment = shader(GL20C.GL_FRAGMENT_SHADER, """
            #version 150
            in vec4 color;
            out uvec2 result;
            void main() { result = uvec2(round(color.r * 255.0), round(color.a * 255.0)); }
            """);
        int program = GL20C.glCreateProgram();
        GL20C.glAttachShader(program, vertex);
        GL20C.glAttachShader(program, fragment);
        GL20C.glBindAttribLocation(program, 0, "Position");
        GL20C.glBindAttribLocation(program, 1, "UV0");
        GL20C.glBindAttribLocation(program, 2, "Color");
        GL20C.glLinkProgram(program);
        GL20C.glDeleteShader(vertex);
        GL20C.glDeleteShader(fragment);
        check(GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS) != 0, GL20C.glGetProgramInfoLog(program));
        try (var target = new MunShadowTarget(32, true); var builder = new MunShadowMesh.Builder(6)) {
            builder.model(model, state, BlockPos.ZERO, Vec3.ZERO, false,
                Objects.requireNonNull(Minecraft.getInstance().level), true);
            try (var mesh = Objects.requireNonNull(builder.finish())) {
                inspect(mesh);
                target.bind();
                GlStateManager._disableDepthTest();
                GlStateManager._disableCull();
                GlStateManager._disableBlend();
                GlStateManager._colorMask(15);
                GlStateManager._glUseProgram(program);
                mesh.draw();
                GL11C.glReadBuffer(GL30C.GL_COLOR_ATTACHMENT0);
                int[] pixel = new int[2];
                GL11C.glReadPixels(16, 16, 1, 1, GL30C.GL_RG_INTEGER, GL11C.GL_UNSIGNED_INT, pixel);
                check(Arrays.equals(pixel, new int[]{204, 128}), "GPU color or alpha changed: " + Arrays.toString(pixel));
            }
        } finally {
            GL20C.glDeleteProgram(program);
        }
    }

    private static double height(BlockStateModel model, BlockState state, BlockPos pos) {
        try (var builder = new MunShadowMesh.Builder(10000)) {
            builder.model(model, state, pos, Vec3.ZERO, true, Objects.requireNonNull(Minecraft.getInstance().level), false);
            try (var mesh = Objects.requireNonNull(builder.finish())) {
                inspect(mesh);
                return mesh.bounds().getYsize();
            }
        }
    }

    private static BlockStateModel model(BlockState state) {
        return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
    }

    private static List<BlockStateModelPart> parts(BlockStateModel model, BlockState state) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, state, RandomSource.create(0), parts);
        return parts;
    }

    private static void inspect(MunShadowMesh mesh) {
        var data = MemoryUtil.memAlloc(mesh.vertexCount() * 24);
        try (var ignored = new MunShadowGlScope()) {
            GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, field(mesh, "buffer"));
            GL15C.glGetBufferSubData(GL15C.GL_ARRAY_BUFFER, 0, data);
            AABB box = mesh.bounds().inflate(0.000001);
            for (int i = 0; i < mesh.vertexCount(); i++) {
                check(box.contains(data.getFloat(i * 24), data.getFloat(i * 24 + 4), data.getFloat(i * 24 + 8)),
                    "GPU vertex outside recorded bounds");
            }
        } finally {
            MemoryUtil.memFree(data);
        }
    }

    private static int field(MunShadowMesh mesh, String name) {
        try {
            var field = MunShadowMesh.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(mesh);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static int shader(int type, String source) {
        int shader = GL20C.glCreateShader(type);
        GL20C.glShaderSource(shader, source);
        GL20C.glCompileShader(shader);
        check(GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS) != 0, GL20C.glGetShaderInfoLog(shader));
        return shader;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
