"""Install the same interactive chart fixture on the local source branch."""
from pathlib import Path
import runpy
import sys

root = Path(__file__).resolve().parents[2]
runpy.run_path(str(root / "dev/porting/prepare_cfa_item_reference.py"), run_name="__main__")
reference = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else root / "build/porting/reference-frost-1.21"
relative = "dev/dubhe/anvilcraft/porting/CelestialMapClientScene.java"
scene = (root / "dev/porting/java" / relative).read_text(encoding="utf-8")
scene = scene.replace("import net.minecraft.client.input.MouseButtonEvent;\n", "")
scene = scene.replace("import net.minecraft.client.input.MouseButtonInfo;\n", "")
scene = scene.replace("cfa-map-26.1-", "cfa-map-1.21-")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("screen.resize(screen.width, screen.height)", "screen.resize(client, screen.width, screen.height)")
scene = scene.replace("        var event = new MouseButtonEvent(guide.getX() + x / 2.0, guide.getY() + y / 2.0, new MouseButtonInfo(0, 0));\n", "")
scene = scene.replace("screen.mouseClicked(event, false)", "screen.mouseClicked(guide.getX() + x / 2.0, guide.getY() + y / 2.0, 0)")
scene = scene.replace("screen.mouseReleased(event)", "screen.mouseReleased(guide.getX() + x / 2.0, guide.getY() + y / 2.0, 0)")
# Source restricts clicks in clicked(), while isMouseOver() still covers the full rectangle.
scene = scene.replace("guide.isMouseOver(guide.getX() + 60, guide.getY() + 60)", "guide.mouseClicked(guide.getX() + 60, guide.getY() + 60, 0)")
(reference / "src/main/java" / relative).write_bytes(scene.replace("\n", "\r\n").encode("utf-8"))
wrapper = reference / "src/main/java/dev/dubhe/anvilcraft/porting/CfaItemReferenceScene.java"
text = wrapper.read_text(encoding="utf-8").replace("CelestialAnvilItemClientScene.frame(client)", "CelestialMapClientScene.frame(client)")
wrapper.write_bytes(text.replace("\n", "\r\n").encode("utf-8"))
