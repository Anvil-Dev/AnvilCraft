"""Install the orbital sky fixture without changing source-branch production rendering."""
from pathlib import Path
import runpy

root = Path(__file__).resolve().parents[2]
runpy.run_path(str(root / "dev/porting/prepare_cfa_item_reference.py"), run_name="__main__")
reference = root / "build/porting/reference-frost-1.21"
base = "dev/dubhe/anvilcraft/porting/"
scene = (root / "dev/porting/java" / base / "OrbitalSkyClientScene.java").read_text(encoding="utf8")
scene = scene.replace("import net.minecraft.world.entity.Relative;\n", "").replace("import java.util.Set;\n", "")
scene = scene.replace(", Set.<Relative>of()", "").replace(", -35, false);", ", -35);")
scene = scene.replace(".setTimeFromServer(", ".setGameTime(").replace(".getOverworldClockTime()", ".getDayTime()")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("orbital-sky-26.1-", "orbital-sky-1.21-")
start = scene.index("        client.level.clockManager()")
end = scene.index("        OverworldLikeClientState.update", start)
scene = scene[:start] + "        client.level.setDayTime(6000);\n" + scene[end:]
(reference / "src/main/java" / base / "OrbitalSkyClientScene.java").write_bytes(scene.replace("\n", "\r\n").encode())
p = reference / "src/main/java" / base / "CfaItemReferenceScene.java"
s = p.read_text(encoding="utf8").replace("CelestialAnvilItemClientScene.frame(client)", "OrbitalSkyClientScene.frame(client)")
p.write_bytes(s.replace("\n", "\r\n").encode())

p = reference / "build.gradle"
s = p.read_text(encoding="utf8")
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portOrbitalSkyScene', 'true' }\n"
p.write_bytes(s.replace("\n", "\r\n").encode())
