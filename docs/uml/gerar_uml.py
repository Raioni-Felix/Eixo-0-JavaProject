# Gera o diagrama de classes Mermaid a partir do código Java (mesmo formato do .mmd antigo)
import sys, os, javalang

RAIZ = sys.argv[1]          # .../src/main/java/com/jogo
SAIDA = sys.argv[2]

def vis(mods, padrao='~'):
    if 'public' in mods: return '+'
    if 'private' in mods: return '-'
    if 'protected' in mods: return '#'
    return padrao

def tipo_str(t):
    # Converte o tipo do javalang em texto, com genéricos no estilo Mermaid (~T~)
    if t is None: return ''
    if getattr(t, 'sub_type', None):  # nome qualificado (java.util.X): fica só o último
        return tipo_str(t.sub_type) + '[]' * len(t.dimensions or [])
    nome = t.name
    args = getattr(t, 'arguments', None)
    if args:
        partes = []
        for a in args:
            partes.append(tipo_str(a.type) if a.type is not None else '?')
        nome += '~' + ', '.join(partes) + '~'
    nome += '[]' * len(t.dimensions or [])
    return nome

def tipos_internos(t):
    # Todos os nomes de tipo citados (para achar associações)
    if t is None: return []
    if getattr(t, 'sub_type', None): return tipos_internos(t.sub_type)
    nomes = [t.name]
    args = getattr(t, 'arguments', None) or []
    if t.name.endswith('Map') and len(args) == 2: args = args[1:]  # Map: só o valor conta
    for a in args:
        if a.type is not None: nomes += tipos_internos(a.type)
    return nomes

# O javalang só entende Java 8; tiramos os corpos dos métodos (switch novo etc.) com o tree-sitter
import tree_sitter as ts, tree_sitter_java as tsj
_parser = ts.Parser(ts.Language(tsj.language()))

def sem_corpos(fonte):
    trechos = []
    def visita(n):
        if n.type in ('method_declaration', 'constructor_declaration'):
            corpo = n.child_by_field_name('body')
            if corpo is not None: trechos.append((corpo.start_byte, corpo.end_byte)); return
        if n.type == 'static_initializer' or (n.type == 'block' and n.parent and n.parent.type == 'class_body'):
            trechos.append((n.start_byte, n.end_byte)); return
        if n.type == 'variable_declarator' and n.parent and n.parent.type == 'field_declaration':
            valor = n.child_by_field_name('value')
            if valor is not None:  # inicializador vira null (não importa pro diagrama)
                trechos.append((valor.start_byte, valor.end_byte, b'null')); return
        for c in n.children: visita(c)
    visita(_parser.parse(fonte).root_node)
    for t in sorted(trechos, reverse=True):
        fonte = fonte[:t[0]] + (t[2] if len(t) > 2 else b'{}') + fonte[t[1]:]
    return fonte.decode('utf-8')

classes = {}   # nome -> dados
for dirpath, _, arqs in os.walk(RAIZ):
    for a in sorted(arqs):
        if not a.endswith('.java'): continue
        caminho = os.path.join(dirpath, a)
        rel = os.path.relpath(dirpath, RAIZ)
        ns = 'jogo' if rel == '.' else rel.replace(os.sep, '_')
        arv = javalang.parse.parse(sem_corpos(open(caminho, 'rb').read()))
        imports = {i.path.split('.')[-1]: i.path for i in arv.imports}
        for decl in arv.types:  # só tipos de topo, como no diagrama antigo
            classes[decl.name] = dict(decl=decl, ns=ns, imports=imports)

nomes_projeto = set(classes)

def membros(nome, d):
    decl = d['decl']
    linhas, assoc, lanca = [], [], []
    eh_interface = isinstance(decl, javalang.tree.InterfaceDeclaration)
    eh_enum = isinstance(decl, javalang.tree.EnumDeclaration)
    eh_abstrata = 'abstract' in decl.modifiers and not eh_interface
    if eh_interface: linhas.append('<<interface>>')
    elif eh_abstrata: linhas.append('<<abstract>>')
    corpo = decl.body.declarations if eh_enum else decl.body
    # Campos (sem constantes static final)
    for m in corpo:
        if isinstance(m, javalang.tree.FieldDeclaration):
            if eh_interface or ('static' in m.modifiers and 'final' in m.modifiers): continue
            for v in m.declarators:
                t = tipo_str(m.type) + '[]' * len(v.dimensions or [])
                linhas.append(f"{vis(m.modifiers)}{v.name} : {t}{'$' if 'static' in m.modifiers else ''}")
                if 'static' not in m.modifiers:
                    citados = [x for x in tipos_internos(m.type) if x in nomes_projeto]
                    if citados:
                        mult = '1' if (m.type.name in nomes_projeto and not m.type.dimensions) else '*'
                        alvo = m.type.name if mult == '1' else citados[-1]
                        assoc.append(f'{nome} --> "{mult}" {alvo} : {v.name}')
    if eh_enum:
        linhas.append(f'+values()$ {nome}[]')
        linhas.append(f'+valueOf(String)$ {nome}')
    construtores = [m for m in corpo if isinstance(m, javalang.tree.ConstructorDeclaration)]
    if not construtores and not eh_interface:
        linhas.append(f"{'-' if eh_enum else '+'}{nome}()")
    for m in corpo:
        if isinstance(m, javalang.tree.ConstructorDeclaration):
            ps = ', '.join(tipo_str(p.type) + ('...' if p.varargs else '') for p in m.parameters)
            linhas.append(f"{vis(m.modifiers, '-' if eh_enum else '~')}{nome}({ps})")
            lanca += m.throws or []
        elif isinstance(m, javalang.tree.MethodDeclaration):
            ps = ', '.join(tipo_str(p.type) + ('...' if p.varargs else '') for p in m.parameters)
            ret = (' ' + tipo_str(m.return_type)) if m.return_type else ''
            abstrato = ('abstract' in m.modifiers) or (eh_interface and m.body is None and 'default' not in m.modifiers and 'static' not in m.modifiers)
            sufixo = '$' if 'static' in m.modifiers else ('*' if abstrato else '')
            v = '+' if eh_interface and vis(m.modifiers) == '~' else vis(m.modifiers)
            linhas.append(f"{v}{m.name}({ps}){sufixo}{ret}")
            lanca += m.throws or []
    lanca_rel = []
    for x in dict.fromkeys(lanca):
        if x in nomes_projeto: lanca_rel.append(f'{nome} ..> {x} : lança')
    return linhas, list(dict.fromkeys(assoc)), lanca_rel

def heranca(nome, d):
    decl, rels, externos = d['decl'], [], {}
    def ext(t):
        caminho = d['imports'].get(t, '')
        if t not in nomes_projeto:
            externos[t] = 'FXGL' if 'fxgl' in caminho else ('JavaFX' if caminho.startswith('javafx') else 'Java')
    if isinstance(decl, javalang.tree.ClassDeclaration):
        if decl.extends: ext(decl.extends.name); rels.append(f'{decl.extends.name} <|-- {nome}')
        for i in decl.implements or []: ext(i.name); rels.append(f'{i.name} <|.. {nome}')
    elif isinstance(decl, javalang.tree.InterfaceDeclaration):
        for i in decl.extends or []: ext(i.name); rels.append(f'{i.name} <|-- {nome}')
    elif isinstance(decl, javalang.tree.EnumDeclaration):
        for i in decl.implements or []: ext(i.name); rels.append(f'{i.name} <|.. {nome}')
    return rels, externos

saida = ['classDiagram', 'direction TB']
assocs, lancas, herancas, externos = [], [], [], {}
for ns in sorted({d['ns'] for d in classes.values()}):
    saida.append(f'namespace {ns} {{')
    for nome in sorted(n for n, d in classes.items() if d['ns'] == ns):
        linhas, a, l = membros(nome, classes[nome])
        saida.append(f'  class {nome} {{')
        saida += ['    ' + x for x in linhas]
        saida.append('  }')
        assocs += a; lancas += l
        h, e = heranca(nome, classes[nome]); herancas += h; externos.update(e)
    saida.append('}')
# Interfaces/classes de fora do projeto (FXGL, JavaFX, Java)
for nome in sorted(externos):
    saida += [f'class {nome} {{', f'  <<{externos[nome]}>>', '}']
saida += assocs + lancas + herancas
open(SAIDA, 'w', encoding='utf-8').write('\n'.join(saida) + '\n')
print(len(classes), 'tipos;', len(assocs), 'associações;', len(herancas), 'heranças;', 'externos:', externos)
