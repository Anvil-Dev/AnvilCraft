"""Install the live player round-trip fixture on the unchanged local source branch."""
from pathlib import Path
import runpy
import sys

root = Path(__file__).resolve().parents[2]
runpy.run_path(str(root / "dev/porting/prepare_cfa_item_reference.py"), run_name="__main__")
reference = root / "build/porting/reference-frost-1.21"
base = "dev/dubhe/anvilcraft/porting/"
scene = (root / "dev/porting/java" / base / "CelestialTravelClientScene.java").read_text(encoding="utf8")
scene = scene.replace("import net.minecraft.world.entity.Relative;\n", "")
scene = scene.replace("import java.util.Set;\n", "")
scene = scene.replace("CelestialTravelTests.", "CelestialTravelReferenceFixture.")
scene = scene.replace("player.level()", "player.serverLevel()")
scene = scene.replace(", Set.<Relative>of()", "").replace("Set.<Relative>of(), ", "").replace(", false);", ");")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("celestial-travel-26.1-", "celestial-travel-1.21-").replace("void-travel-26.1-", "void-travel-1.21-")
scene = scene.replace("                    portal.findParentCfa().syncToClient();",
                      "                    level.sendBlockUpdated(CENTER, level.getBlockState(CENTER), level.getBlockState(CENTER), 3);")
(reference / "src/main/java" / base / "CelestialTravelClientScene.java").write_bytes(scene.replace("\n", "\r\n").encode())
source = (root / "dev/porting/java" / base / "CelestialTravelTests.java").read_text(encoding="utf8")
methods = []
for signature in ("    public static CelestialTravelData travel(", "    public static CelestialForgingAnvilPortalBlockEntity machine("):
    start = source.index(signature)
    end = source.index("{", start) + 1
    depth = 1
    while depth:
        depth += (source[end] == "{") - (source[end] == "}")
        end += 1
    methods.append(source[start:end])
imports = [line for line in source.splitlines() if line.startswith("import dev.dubhe.anvilcraft.")
           or line.startswith("import net.minecraft.core.BlockPos;") or line.startswith("import net.minecraft.core.Direction;")
           or line.startswith("import net.minecraft.resources.ResourceKey;") or line.startswith("import net.minecraft.server.level.ServerLevel;")
           or line.startswith("import net.minecraft.world.level.Level;") or line.startswith("import net.minecraft.world.level.block.Block;")]
text = "package dev.dubhe.anvilcraft.porting;\n\n" + "\n".join(imports)
text += "\n\npublic final class CelestialTravelReferenceFixture {\n" + "\n\n".join(methods) + "\n}\n"
text = text.replace("dimension.identifier()", "dimension.location()")
text = text.replace("        be.syncToClient();", "        level.sendBlockUpdated(center, be.getBlockState(), be.getBlockState(), 3);")
(reference / "src/main/java" / base / "CelestialTravelReferenceFixture.java").write_bytes(text.replace("\n", "\r\n").encode())
path = Path("data/anvilcraft/dimension/port_travel_void.json")
(reference / "src/main/resources" / path).parent.mkdir(parents=True, exist_ok=True)
(reference / "src/main/resources" / path).write_bytes((root / "dev/porting/resources" / path).read_bytes())
p = reference / "src/main/java" / base / "CfaItemReferenceScene.java"
s = p.read_text(encoding="utf8").replace("CelestialAnvilItemClientScene.frame(client)", "CelestialTravelClientScene.frame(client)")
p.write_bytes(s.replace("\n", "\r\n").encode())

p = reference / "build.gradle"
s = p.read_text(encoding="utf8")
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portVoidPlanetScene', '" + str("--void" in sys.argv).lower() + "' }\n"
p.write_bytes(s.replace("\n", "\r\n").encode())
