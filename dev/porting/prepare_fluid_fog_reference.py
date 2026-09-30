"""Exercise source fog hooks with the same development-only camera scene."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/FluidFogClientScene.java").read_text(encoding="utf-8")
scene = scene.replace("import net.minecraft.client.renderer.fog.FogData;", "import com.mojang.blaze3d.systems.RenderSystem;\n"
    "import com.mojang.blaze3d.shaders.FogShape;\nimport net.minecraft.client.renderer.FogRenderer;")
scene = scene.replace("Vector4f", "Vector3f").replace("event.getBlue(), 1)", "event.getBlue())")
scene = scene.replace("0.1F, 0.2F, 0.3F, 1)", "0.1F, 0.2F, 0.3F)")
scene = scene.replace("        extension().modifyFogColor(", "        color = extension().modifyFogColor(")
scene = scene.replace("            extension.modifyFogColor(", "            color = extension.modifyFogColor(")
scene = scene.replace("extension().modifyFogRender(event.getCamera(), event.getEnvironment(), 128, 0, event.getFogData());",
    "extension().modifyFogRender(event.getCamera(), event.getMode(), 128, 0, event.getNearPlaneDistance(), "
    "event.getFarPlaneDistance(), event.getFogShape());")
start = scene.index("            var data = new FogData();")
end = scene.index("            boolean bypass =", start)
scene = scene[:start] + """            float previousStart = RenderSystem.getShaderFogStart();
            float previousEnd = RenderSystem.getShaderFogEnd();
            RenderSystem.setShaderFogStart(123);
            RenderSystem.setShaderFogEnd(234);
            extension.modifyFogRender(client.gameRenderer.getMainCamera(), FogRenderer.FogMode.FOG_TERRAIN,
                128, 0, 123, 234, FogShape.CYLINDER);
            float start = RenderSystem.getShaderFogStart();
            float end = RenderSystem.getShaderFogEnd();
            RenderSystem.setShaderFogStart(previousStart);
            RenderSystem.setShaderFogEnd(previousEnd);
""" + scene[end:]
start = scene.index("            if (data.skyEnd !=")
end = scene.index("            if (start !=", start)
scene = scene[:start] + scene[end:]
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("fluid-fog-26.1", "fluid-fog-1.21")
(folder / "FluidFogClientScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "FluidFogReferenceScene").replace("MonolithClientScene", "FluidFogClientScene")
wrapper = wrapper.replace("portMonolithScene", "portFluidFogScene")
(folder / "FluidFogReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", p.read_text(encoding="utf-8"))
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portFluidFogScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
