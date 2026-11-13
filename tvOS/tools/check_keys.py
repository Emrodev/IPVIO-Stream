import io
import re
import collections
from pathlib import Path

root = Path(r'IPVIO-Stream/tvOS/IPVIOStream')
loc = (root / 'Core/Localization.swift').read_text(encoding='utf-8')

# Split tables
en_block = loc.split('private static let en: [String: String] = [', 1)[1].split('\n    ]', 1)[0]
ar_block = loc.split('private static let ar: [String: String] = [', 1)[1].split('\n    ]', 1)[0]
en_keys = re.findall(r'"([^"]+)"\s*:', en_block)
ar_keys = re.findall(r'"([^"]+)"\s*:', ar_block)

print('EN keys', len(en_keys), 'AR keys', len(ar_keys))
for name, keys in [('en', en_keys), ('ar', ar_keys)]:
    dup = [k for k, v in collections.Counter(keys).items() if v > 1]
    print(name, 'duplicates:', dup)

missing_ar = sorted(set(en_keys) - set(ar_keys))
missing_en = sorted(set(ar_keys) - set(en_keys))
print('Missing AR:', missing_ar)
print('Missing EN:', missing_en)

used = set()
for path in root.rglob('*.swift'):
    if path.name == 'Localization.swift':
        continue
    text = path.read_text(encoding='utf-8')
    used.update(re.findall(r'L\.t\("([^"]+)"\)', text))
    used.update(re.findall(r'L\.f\("([^"]+)"', text))
    used.update(re.findall(r'"((?:nav|home|browse|detail|live|player|search|settings|add|load|welcome|error|time|quality|lang|language|toast|misc)\.[^"]+)"', text))

missing = sorted(k for k in used if k not in set(en_keys))
print('Used keys not defined:', missing)

unused = sorted(set(en_keys) - used)
print('Defined but not statically referenced (may be dynamic):', len(unused))
print(unused)
