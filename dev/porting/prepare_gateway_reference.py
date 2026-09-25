"""Select the source gateway fixture while preserving production source unchanged."""
from pathlib import Path
import runpy
import sys

root = Path(__file__).resolve().parents[2]
mode = sys.argv[1] if len(sys.argv) > 1 else "ui"
assert mode in ("ui", "ui-control", "world", "item")
control = mode == "ui-control"
if control:
    mode = "ui"
sys.argv = [sys.argv[0]]
preparer = {"ui": "prepare_special_celestial_reference.py", "world": "prepare_special_world_reference.py",
            "item": "prepare_cfa_item_reference.py"}[mode]
runpy.run_path(str(root / "dev/porting" / preparer), run_name="__main__")
reference = root / "build/porting/reference-frost-1.21"
relative = {"ui": "StellarEvolutionUiClientScene.java", "world": "SpecialCelestialWorldScene.java",
            "item": "CelestialAnvilItemClientScene.java"}[mode]
p = reference / "src/main/java/dev/dubhe/anvilcraft/porting" / relative
s = p.read_text(encoding="utf8").replace(".setTimeFromServer(", ".setGameTime(")
s = s.replace("gateway-ui-26.1-", "gateway-ui-1.21-").replace("gateway-world-26.1-", "gateway-world-1.21-")
s = s.replace("gateway-item-26.1-", "gateway-item-1.21-")
s = s.replace("        if (GATEWAY && stage == 1 && client.getOverlay() == null) GatewayItemCacheProbe.sample(client);\n", "")
s = s.replace("        if (GATEWAY && name.equals(\"gallery\")) GatewayItemCacheProbe.verify();\n", "")
if control:
    s = s.replace("gateway-ui-1.21-", "gateway-ui-control-1.21-")
    s = s.replace("PORT_GATEWAY_UI_CAPTURED", "PORT_GATEWAY_UI_CONTROL_CAPTURED")
    s = s.replace("            super.render(graphics, 0, 0, 0);", """            super.render(graphics, 0, 0, 0);
            if (GATEWAY && this.getMenu().getBlockEntity().getCelestialBodyData() != null) {
                graphics.flush();
                graphics.pose().pushPose();
                graphics.pose().translate(this.leftPos, this.topPos, 0);
                try {
                    var method = CelestialForgingAnvilScreen.class.getDeclaredMethod("renderBodyPreview",
                        GuiGraphics.class, dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData.class);
                    method.setAccessible(true);
                    method.invoke(this, graphics, this.getMenu().getBlockEntity().getCelestialBodyData());
                    graphics.flush();
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException(error);
                } finally {
                    graphics.pose().popPose();
                }
            }""")
p.write_bytes(s.replace("\n", "\r\n").encode())
p = reference / "build.gradle"
s = p.read_text(encoding="utf8") + "\nneoForge.runs.client { systemProperty 'anvilcraft.portGatewayScene', 'true' }\n"
p.write_bytes(s.replace("\n", "\r\n").encode())

relative = "dev/dubhe/anvilcraft/porting/SpecialCelestialVisualFixture.java"
(reference / "src/main/java" / relative).write_bytes((root / "dev/porting/java" / relative).read_bytes())
