"""Run the same live Jade cursed-gold tooltip and reload check on 1.21."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
assert subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root).strip() == subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=reference).strip()
folder = reference / "src/main/java/dev/dubhe/anvilcraft/porting"
scene = (root / "dev/porting/java/dev/dubhe/anvilcraft/porting/CursedGoldClientScene.java").read_text(encoding="utf-8")
scene = scene.replace("client.resizeGui()", "client.resizeDisplay()")
scene = scene.replace("IWailaConfig.get().overlay()", "IWailaConfig.get().getOverlay()")
scene = scene.replace("OverlayRenderer.animation.rect", "OverlayRenderer.rect.rect")
scene = scene.replace("client.getMainRenderTarget(), 1,", "client.getMainRenderTarget(),")
scene = scene.replace("cursed-gold-26.1", "cursed-gold-1.21")
scene = scene.replace("import snownee.jade.api.ui.TextElement;", "import snownee.jade.impl.ui.TextElement;")
scene = scene.replace("import snownee.jade.impl.Tooltip;", "import snownee.jade.api.ITooltip;")
start = scene.index("    private static List<String> readLines(")
end = scene.index("    private static void save(", start)
scene = scene[:start] + """    private static List<String> readLines(ITooltip tooltip) {
        List<String> result = new ArrayList<>();
        for (int line = 0; line < tooltip.size(); line++) {
            for (var align : snownee.jade.api.ui.IElement.Align.values()) {
                for (var element : tooltip.get(line, align)) {
                    if (element instanceof TextElement text) result.add(text.getMessage());
                }
            }
        }
        return result;
    }

""" + scene[end:]
(folder / "CursedGoldClientScene.java").write_text(scene, encoding="utf-8", newline="\r\n")
wrapper = (folder / "MonolithReferenceScene.java").read_text(encoding="utf-8")
wrapper = wrapper.replace("MonolithReferenceScene", "CursedGoldReferenceScene").replace("MonolithClientScene", "CursedGoldClientScene")
wrapper = wrapper.replace("portMonolithScene", "portCursedGoldScene")
(folder / "CursedGoldReferenceScene.java").write_text(wrapper, encoding="utf-8", newline="\r\n")
p = reference / "build.gradle"
s = p.read_text(encoding="utf-8")
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portCursedGoldScene', 'true' }\n"
p.write_text(s, encoding="utf-8", newline="\r\n")
