"""Install identical fixed sky views on the latest isolated 1.21 Mun reference checkout."""
from pathlib import Path
import runpy

root = Path(__file__).resolve().parents[2]
runpy.run_path(str(root / "dev/porting/prepare_mun_terrain_reference.py"), run_name="__main__")
reference = root / "build/porting/reference-mun-1.21"
base = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/MunSkyClientScene.java").read_text(encoding="utf8")
scene = scene.replace("import net.minecraft.world.entity.Relative;\n", "").replace("import java.util.Set;\n", "")
scene = scene.replace(", Set.<Relative>of()", "").replace(", -90, false);", ", -90);")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("mun-sky-26.1-", "mun-sky-1.21-").replace(".setTimeFromServer(", ".setGameTime(")
start = scene.index("        client.level.clockManager()")
end = scene.index("        var rotation =", start)
scene = scene[:start] + "        client.level.setDayTime(sample.time);\n" + scene[end:]
(base / "MunSkyClientScene.java").write_bytes(scene.replace("\n", "\r\n").encode())
wrapper = (base / "MunTerrainReferenceScene.java").read_text(encoding="utf8")
start = wrapper.index("        if (failure != null)")
wrapper = wrapper[:start] + '''        if (client.player != null && client.level != null && client.getSingleplayerServer() != null) {
            MunSkyClientScene.frame(client);
        }
    }
}
'''
wrapper = wrapper.replace("MunTerrainReferenceScene", "MunSkyReferenceScene")
wrapper = wrapper.replace("anvilcraft.portMunTerrain", "anvilcraft.portMunSkyScene")
wrapper = wrapper.replace("mun-terrain-reference-", "mun-sky-reference-")
(base / "MunSkyReferenceScene.java").write_bytes(wrapper.replace("\n", "\r\n").encode())
p = reference / "build.gradle"
s = p.read_text(encoding="utf8").replace("systemProperty 'anvilcraft.portMunTerrain', 'true'",
                                       "systemProperty 'anvilcraft.portMunTerrain', 'false'")
if "systemProperty 'anvilcraft.portMunSkyScene'" not in s:
    s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portMunSkyScene', 'true' }\n"
p.write_bytes(s.replace("\n", "\r\n").encode())
