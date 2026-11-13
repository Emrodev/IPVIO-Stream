import io
import sys
from pathlib import Path

ROOT = Path(r'IPVIO-Stream/tvOS/IPVIOStream')
VALID_ESCAPES = set('0\\tnr"\'u(')

def check_file(path: Path):
    text = path.read_text(encoding='utf-8')
    i = 0
    n = len(text)
    line = 1
    stack = []
    errors = []
    while i < n:
        c = text[i]
        if c == '\n':
            line += 1
            i += 1
            continue
        if text.startswith('//', i):
            j = text.find('\n', i)
            i = n if j < 0 else j
            continue
        if text.startswith('/*', i):
            depth = 1
            i += 2
            while i < n and depth:
                if text.startswith('/*', i):
                    depth += 1; i += 2
                elif text.startswith('*/', i):
                    depth -= 1; i += 2
                else:
                    if text[i] == '\n': line += 1
                    i += 1
            if depth:
                errors.append(f'{path}:{line}: unterminated block comment')
            continue
        if c == '"':
            multiline = text.startswith('"""', i)
            i += 3 if multiline else 1
            while i < n:
                if multiline and text.startswith('"""', i):
                    i += 3
                    break
                if not multiline and text[i] == '"':
                    i += 1
                    break
                if not multiline and text[i] == '\n':
                    errors.append(f'{path}:{line}: newline in string')
                    line += 1
                    i += 1
                    break
                if text[i] == '\\':
                    if i + 1 >= n:
                        errors.append(f'{path}:{line}: trailing backslash')
                        break
                    nxt = text[i+1]
                    if nxt not in VALID_ESCAPES:
                        errors.append(f'{path}:{line}: invalid escape \\{nxt}')
                    if nxt == '(':
                        # skip interpolation roughly, tracking nested parens and strings
                        depth = 1
                        i += 2
                        while i < n and depth:
                            if text[i] == '"':
                                # nested string
                                i += 1
                                while i < n and text[i] != '"':
                                    if text[i] == '\\': i += 1
                                    i += 1
                                i += 1
                                continue
                            if text[i] == '(': depth += 1
                            elif text[i] == ')': depth -= 1
                            elif text[i] == '\n': line += 1
                            i += 1
                        if depth:
                            errors.append(f'{path}:{line}: unterminated interpolation')
                        continue
                    if nxt == 'u':
                        # \u{...}
                        j = text.find('}', i + 2)
                        if j < 0:
                            errors.append(f'{path}:{line}: bad unicode escape')
                            i = n
                            break
                        i = j + 1
                        continue
                    if nxt == '\n':
                        line += 1
                    i += 2
                    continue
                if text[i] == '\n':
                    line += 1
                i += 1
            continue
        if c in '([{':
            stack.append((c, line))
            i += 1
            continue
        if c in ')]}':
            if not stack:
                errors.append(f'{path}:{line}: unexpected {c}')
            else:
                open_c, open_line = stack.pop()
                pairs = {'(' : ')', '[': ']', '{': '}'}
                if pairs[open_c] != c:
                    errors.append(f'{path}:{line}: mismatch {open_c} from line {open_line} closed with {c}')
            i += 1
            continue
        i += 1
    if stack:
        errors.append(f'{path}: unclosed {stack[-1][0]} from line {stack[-1][1]}')
    return errors

all_errors = []
for path in sorted(ROOT.rglob('*.swift')):
    all_errors.extend(check_file(path))

if all_errors:
    print('\n'.join(all_errors))
    sys.exit(1)
print('Swift lexical sanity: OK')
