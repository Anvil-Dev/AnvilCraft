"""Select fixed celestial previews using the same source/target screen fixture."""
from pathlib import Path
import runpy
import sys

root = Path(__file__).resolve().parents[2]
runpy.run_path(str(root / "dev/porting/prepare_stellar_ui_reference.py"), run_name="__main__")
reference = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else root / "build/porting/reference-frost-1.21"
build = reference / "build.gradle"
text = build.read_text(encoding="utf-8")
text += "\nneoForge.runs.client { systemProperty 'anvilcraft.portCfaPreviewScene', 'true' }\n"
build.write_bytes(text.replace("\n", "\r\n").encode("utf-8"))
