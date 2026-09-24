"""Install the same material/light scene on the unchanged source Moon renderer."""
from pathlib import Path
import runpy

root = Path(__file__).resolve().parents[2]
runpy.run_path(str(root / "dev/porting/prepare_mun_sky_reference.py"), run_name="__main__")
reference = root / "build/porting/reference-mun-1.21"
base = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/MunSurfaceClientScene.java").read_text(encoding="utf8")
scene = scene.replace("import net.minecraft.world.entity.Relative;\n", "").replace("import java.util.Set;\n", "")
scene = scene.replace("Set.<Relative>of(), ", "").replace(", 15, false);", ", 15);")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("mun-surface-26.1-", "mun-surface-1.21-").replace(".setTimeFromServer(", ".setGameTime(")
start = scene.index("        client.level.clockManager()")
end = scene.index("        BlockPos base = center();", start)
scene = scene[:start] + "        client.level.setDayTime(time());\n" + scene[end:]
start = scene.index('            var field = client.gameRenderer.getClass().getDeclaredField("lightmapRenderStateExtractor");')
end = scene.index("            var flicker =", start)
scene = scene[:start] + "            Object extractor = client.gameRenderer.lightTexture();\n" + scene[end:]
scene = scene.replace('getDeclaredField("blockLightFlicker")', 'getDeclaredField("blockLightRedFlicker")')
scene = scene.replace('getDeclaredField("needsUpdate")', 'getDeclaredField("updateLightTexture")')
start = scene.index("    private static void dumpLightmap")
end = scene.index("    private static void freezeLightmap", start)
scene = scene[:start] + '    private static void dumpLightmap(Minecraft client) {\n        try {\n            var field = client.gameRenderer.lightTexture().getClass().getDeclaredField("lightPixels");\n            field.setAccessible(true);\n            var image = (com.mojang.blaze3d.platform.NativeImage) field.get(client.gameRenderer.lightTexture());\n            image.writeToFile(client.gameDirectory.toPath().resolve("screenshots/mun-lightmap-1.21.png"));\n        } catch (ReflectiveOperationException | java.io.IOException exception) {\n            throw new IllegalStateException(exception);\n        }\n    }\n\n' + scene[end:]
(base / "MunSurfaceClientScene.java").write_bytes(scene.replace("\n", "\r\n").encode())
wrapper = (base / "MunSkyReferenceScene.java").read_text(encoding="utf8")
wrapper = wrapper.replace("MunSkyReferenceScene", "MunSurfaceReferenceScene")
wrapper = wrapper.replace("MunSkyClientScene", "MunSurfaceClientScene")
wrapper = wrapper.replace("portMunSkyScene", "portMunSurfaceScene").replace("mun-sky-reference-", "mun-surface-reference-")
(base / "MunSurfaceReferenceScene.java").write_bytes(wrapper.replace("\n", "\r\n").encode())
p = reference / "build.gradle"
s = p.read_text(encoding="utf8").replace("systemProperty 'anvilcraft.portMunSkyScene', 'true'",
                                       "systemProperty 'anvilcraft.portMunSkyScene', 'false'")
if "systemProperty 'anvilcraft.portMunSurfaceScene'" not in s:
    s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portMunSurfaceScene', 'true' }\n"
p.write_bytes(s.replace("\n", "\r\n").encode())
