"""Install the same colored-atmosphere and special-info fixture on the source branch."""
from pathlib import Path
import runpy
import sys

root = Path(__file__).resolve().parents[2]
runpy.run_path(str(root / "dev/porting/prepare_stellar_ui_reference.py"), run_name="__main__")
reference = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else root / "build/porting/reference-frost-1.21"
relative = "dev/dubhe/anvilcraft/porting/SpecialCelestialVisualFixture.java"
(reference / "src/main/java" / relative).write_bytes((root / "dev/porting/java" / relative).read_bytes())
p = reference / "src/main/java/dev/dubhe/anvilcraft/porting/StellarEvolutionUiClientScene.java"
s = p.read_text(encoding="utf8").replace("special-celestial-26.1-", "special-celestial-1.21-")
p.write_bytes(s.replace("\n", "\r\n").encode())
p = reference / "build.gradle"
s = p.read_text(encoding="utf8") + "\nneoForge.runs.client { systemProperty 'anvilcraft.portSpecialCelestialScene', 'true' }\n"
p.write_bytes(s.replace("\n", "\r\n").encode())
