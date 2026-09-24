"""Check source recipe conversion/transfer fidelity and native integration evidence."""
from pathlib import Path
import json
import re
import subprocess

root = Path(__file__).resolve().parents[2]
ref = '1260db54f6e2b9f88d0a79b8108459d346344e44'
files = ['util/StructureScannerRecipes.java', 'integration/jei/transfer/StructureScannerRecipeTransferHandler.java']
for tail in files:
    path = 'src/main/java/dev/dubhe/anvilcraft/' + tail
    source = subprocess.check_output(['git', 'show', f'{ref}:{path}'], cwd=root).decode('utf-8')
    source = source.replace('ResourceLocation', 'Identifier').replace('RecipeType<R>', 'IRecipeType<R>')
    source = source.replace('import mezz.jei.api.recipe.RecipeType;\n', '')
    source = source.replace('import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;',
                            'import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;\n'
                            'import mezz.jei.api.recipe.types.IRecipeType;')
    source = source.replace('recipe.id())', 'recipe.id().identifier())')
    assert source == (root / path).read_text(encoding='utf-8'), path
tests = (root / 'build/porting/tests-scanner-recipe-final.log').read_text(encoding='utf-8', errors='replace')
assert 'All dimensions are saved' in tests
passed = re.search(r'All (\d+) required tests passed', tests)
assert passed and int(passed[1]) >= 567
catalog = re.search(r'PORT_SCANNER_RECIPE_CATALOG_PASSED: crafting=(\d+), conversion=(\d+)', tests)
assert catalog
client = (root / 'build/porting/client-scanner-recipe-final.log').read_text(encoding='utf-8', errors='replace')
for marker in ['PORT_SCANNER_RECIPE_CLIENT_PASSED', 'All dimensions are saved', 'BUILD SUCCESSFUL']:
    assert marker in client, marker
screenshots = [root / f'run/port-validation/client/screenshots/scanner-recipe-26.1-{name}.png'
               for name in ['crafting', 'conversion']]
assert all(path.is_file() for path in screenshots)
report = {
    'source_commit': ref,
    'source_files_equal_after_native_api_mapping': files,
    'required_tests_passed': int(passed[1]),
    'catalog': {'crafting': int(catalog[1]), 'conversion': int(catalog[2])},
    'checks': ['Predicate states and copied block entity NBT', 'Input rather than conversion output',
               '16/17-block size boundary and safe filenames',
               'Normalized coordinates and legacy rotation/upside-down loading',
               'Actual registered JEI handlers, dry run and busy guard',
               'Native client/server preview round trip without disk consumption', 'Missing recipe rejection'],
    'limits': ['The client fixture invokes registered JEI handlers directly; the JEI plus button is not clicked.',
               'The first client fixture opened the menu before chunk synchronization and was terminated after disconnect.']
}
(root / 'build/porting/scanner-recipe-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
