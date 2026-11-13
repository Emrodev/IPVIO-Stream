import io, re, sys
from pathlib import Path

path = Path(r'IPVIO-Stream/tvOS/IPVIOStream.xcodeproj/project.pbxproj')
text = path.read_text(encoding='utf-8')

ids = set(re.findall(r'\b([0-9A-F]{24})\b', text))
defs = set(re.findall(r'^\t\t([0-9A-F]{24})\s', text, re.M))
print('ids', len(ids), 'defs', len(defs))
print('undefined defs', sorted(defs - ids))
print('defined but never referenced elsewhere? (may be root)', len(ids - defs))

# Basic object references
for key in ['fileRef = ', 'productRef = ', 'package = ']:
    for value in re.findall(re.escape(key) + r'([0-9A-F]{24})', text):
        if value not in defs:
            print('missing target for', key, value)

# Root object
root = re.search(r'rootObject = ([0-9A-F]{24})', text)
print('rootObject defined:', bool(root and root.group(1) in defs))

# Brace balance outside quoted strings
depth = 0
in_string = False
escaped = False
for i, ch in enumerate(text):
    if in_string:
        if escaped:
            escaped = False
        elif ch == '\\':
            escaped = True
        elif ch == '"':
            in_string = False
        continue
    if ch == '"':
        in_string = True
    elif ch == '{':
        depth += 1
    elif ch == '}':
        depth -= 1
        if depth < 0:
            print('negative brace at', i)
            break
print('brace depth at end:', depth, 'in_string:', in_string)

# Scheme target
scheme = Path(r'IPVIO-Stream/tvOS/IPVIOStream.xcodeproj/xcshareddata/xcschemes/IPVIOStream.xcscheme').read_text(encoding='utf-8')
blueprint = re.search(r'BlueprintIdentifier = "([0-9A-F]{24})"', scheme)
print('scheme target defined:', bool(blueprint and blueprint.group(1) in defs))

# project.yml parse if PyYAML is available
try:
    import yaml
    data = yaml.safe_load(Path(r'IPVIO-Stream/tvOS/project.yml').read_text(encoding='utf-8'))
    print('yaml keys:', list(data.keys()))
except Exception as e:
    print('yaml check skipped:', e)
