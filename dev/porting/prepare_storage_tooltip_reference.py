"""Capture source storage tooltips through their real item/RPC path."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
folder = reference / 'src/main/java/dev/dubhe/anvilcraft/porting'
scene = (root / 'dev/porting/java/dev/dubhe/anvilcraft/porting/StorageTooltipClientScene.java').read_text(encoding='utf-8')
scene = scene.replace('import net.neoforged.neoforge.transfer.item.ItemResource;\n', '')
scene = scene.replace('import net.neoforged.neoforge.transfer.transaction.Transaction;\n', '')
scene = scene.replace('client.resizeGui()', 'client.resizeDisplay()').replace('renderer.getHeight(client.font)', 'renderer.getHeight()')
scene = scene.replace('GuiGraphicsExtractor', 'GuiGraphics').replace('extractRenderState(', 'render(')
scene = scene.replace('client.getMainRenderTarget(), 1,', 'client.getMainRenderTarget(),')
scene = scene.replace('graphics.setTooltipForNextFrame(', 'graphics.renderTooltip(')
scene = scene.replace('storage-tooltip-26.1-', 'storage-tooltip-1.21-')
start = scene.index('            case 11 ->')
end = scene.index('            default ->', start)
scene = scene[:start] + '''            case 11 -> {
                AnvilCraft.LOGGER.info("PORT_STORAGE_TOOLTIP_CLIENT_PASSED: live RPC, finite/infinite, nine icons and refresh");
                stage = 13;
                client.stop();
            }
''' + scene[end:]
start = scene.index('        try (var transaction = Transaction.openRoot())')
end = scene.index('    private static void advance', start)
scene = scene[:start] + '''        if (!storage.getItems().insertItem(new ItemStack(item, count), false).isEmpty()) {
            throw new IllegalStateException("Fixture stock");
        }
    }

''' + scene[end:]
if '    private static final class ScaledPreview' in scene:
    scene = scene[:scene.index('    private static final class ScaledPreview')] + '}\n'
(folder / 'StorageTooltipClientScene.java').write_text(scene, encoding='utf-8', newline='\r\n')
wrapper = (folder / 'MonolithReferenceScene.java').read_text(encoding='utf-8')
wrapper = wrapper.replace('MonolithReferenceScene', 'StorageTooltipReferenceScene').replace('MonolithClientScene', 'StorageTooltipClientScene')
wrapper = wrapper.replace('portMonolithScene', 'portStorageTooltipScene')
(folder / 'StorageTooltipReferenceScene.java').write_text(wrapper, encoding='utf-8', newline='\r\n')
p = reference / 'build.gradle'
s = p.read_text(encoding='utf-8')
s = re.sub(r"(systemProperty 'anvilcraft\.port[^']+', )'true'", r"\1'false'", s)
s += "\nneoForge.runs.client { systemProperty 'anvilcraft.portStorageTooltipScene', 'true' }\n"
p.write_text(s, encoding='utf-8', newline='\r\n')
