import {EditorView, crosshairCursor, drawSelection, dropCursor, highlightActiveLine, highlightActiveLineGutter, highlightSpecialChars, keymap, lineNumbers, rectangularSelection} from '@codemirror/view';
import {EditorSelection, EditorState} from '@codemirror/state';
import {defaultKeymap, history, historyKeymap, indentLess, indentMore, isolateHistory, redo, undo} from '@codemirror/commands';
import {autocompletion, closeBrackets, closeBracketsKeymap, completionKeymap} from '@codemirror/autocomplete';
import {highlightSelectionMatches, searchKeymap} from '@codemirror/search';
import {lintKeymap} from '@codemirror/lint';
import {javascript} from '@codemirror/lang-javascript';
import {python} from '@codemirror/lang-python';
import {java} from '@codemirror/lang-java';
import {HighlightStyle, bracketMatching, defaultHighlightStyle, foldGutter, foldKeymap, indentOnInput, indentUnit, syntaxHighlighting} from '@codemirror/language';
import {tags} from '@lezer/highlight';

const PHRASES = {
    'zh-Hant': {
        'Find': '搜尋',
        'Replace': '取代',
        'next': '下一個',
        'previous': '上一個',
        'all': '全選',
        'match case': '區分大小寫',
        'regexp': '正規表示式',
        'by word': '完整單字',
        'replace': '取代',
        'replace all': '全部取代',
        'close': '關閉',
        'Go to line': '跳至行號',
        'go': '前往',
        'current match': '目前符合項目',
        'on line': '所在行號',
        'replaced $ matches': '已取代 $ 個符合項目',
        'replaced match on line $': '已取代第 $ 行的符合項目',
        'Fold line': '收合代碼',
        'Unfold line': '展開代碼',
        'Folded lines': '已收合行號',
        'Unfolded lines': '已展開行號',
        'to': '至',
        'folded code': '已收合的代碼',
        'unfold': '展開',
        'Diagnostics': '診斷訊息',
        'No diagnostics': '沒有診斷訊息',
        'Completions': '自動完成',
        'Control character': '控制字元'
    },
    'zh-Hans': {
        'Find': '搜索',
        'Replace': '替换',
        'next': '下一个',
        'previous': '上一个',
        'all': '全选',
        'match case': '区分大小写',
        'regexp': '正则表达式',
        'by word': '完整单词',
        'replace': '替换',
        'replace all': '全部替换',
        'close': '关闭',
        'Go to line': '跳至行号',
        'go': '前往',
        'current match': '当前匹配项',
        'on line': '所在行号',
        'replaced $ matches': '已替换 $ 个匹配项',
        'replaced match on line $': '已替换第 $ 行的匹配项',
        'Fold line': '收起代码',
        'Unfold line': '展开代码',
        'Folded lines': '已收起行号',
        'Unfolded lines': '已展开行号',
        'to': '至',
        'folded code': '已收起的代码',
        'unfold': '展开',
        'Diagnostics': '诊断信息',
        'No diagnostics': '没有诊断信息',
        'Completions': '自动补全',
        'Control character': '控制字符'
    }
};

const PAIRS = {'()': ')', '[]': ']', '{}': '}', '""': '"', "''": "'", '``': '`'};
const SETUP = [
    lineNumbers(), highlightActiveLineGutter(), highlightSpecialChars(), history(),
    foldGutter({markerDOM(open) {
        const marker = document.createElement('span');
        const label = open ? 'Fold line' : 'Unfold line';
        marker.className = 'cm-foldMarker';
        marker.title = PHRASES[document.documentElement.lang]?.[label] || label;
        marker.innerHTML = '<svg class="cm-foldIcon" viewBox="0 0 24 24" aria-hidden="true" data-open="' + open + '"><use href="icons.svg#chevron"/></svg>';
        return marker;
    }}),
    drawSelection(), dropCursor(), EditorState.allowMultipleSelections.of(true), indentOnInput(),
    syntaxHighlighting(defaultHighlightStyle, {fallback: true}), bracketMatching(), closeBrackets(), autocompletion(),
    rectangularSelection(), crosshairCursor(), highlightActiveLine(), highlightSelectionMatches(),
    keymap.of([...closeBracketsKeymap, ...defaultKeymap, ...searchKeymap, ...historyKeymap, ...foldKeymap, ...completionKeymap, ...lintKeymap])
];

export function create(parent, changed) {
    let setting = false;
    const extensions = (extension, writable) => [SETUP, EditorState.phrases.of(PHRASES[document.documentElement.lang] || {}), extension === 'py' ? python() : extension === 'java' ? java() : javascript(), syntaxHighlighting(HighlightStyle.define([
        {tag: tags.keyword, color: 'var(--md-primary)', fontWeight: '600'},
        {tag: [tags.string, tags.number, tags.bool], color: 'var(--md-primary)'},
        {tag: tags.comment, color: 'var(--md-on-surface-variant)', fontStyle: 'italic'}
    ])), indentUnit.of('    '), EditorView.editable.of(writable), EditorState.readOnly.of(!writable), EditorView.contentAttributes.of({'aria-label': parent.dataset.label, spellcheck: 'false'}), EditorView.theme({
        '&': {height: '100%', color: 'var(--md-on-surface)', backgroundColor: 'var(--md-surface-container)'},
        '.cm-scroller': {fontFamily: 'ui-monospace, SFMono-Regular, Consolas, monospace', fontSize: '14px', lineHeight: '1.65'},
        '.cm-gutters': {backgroundColor: 'var(--md-surface-low)', color: 'var(--md-on-surface-variant)', borderColor: 'var(--md-outline-variant)'},
        '.cm-foldGutter .cm-gutterElement': {display: 'flex', alignItems: 'center', justifyContent: 'center', width: '18px'},
        '.cm-foldGutter .cm-foldMarker': {display: 'block', padding: '0'},
        '.cm-foldIcon': {display: 'block', width: '14px', height: '14px', flexShrink: '0', cursor: 'pointer'},
        '.cm-foldIcon[data-open="true"]': {transform: 'rotate(90deg)'},
        '.cm-activeLine, .cm-activeLineGutter': {backgroundColor: 'var(--md-secondary-container)'},
        '.cm-cursor': {borderLeftColor: 'var(--md-primary)'},
        '&.cm-focused .cm-selectionBackground, .cm-selectionBackground': {backgroundColor: 'var(--md-secondary-container)'},
        '.cm-content': {padding: '12px 0'},
        '&.cm-focused': {outline: 'none'}
    }), EditorView.updateListener.of(update => {if (update.docChanged && !setting) changed(view.state.doc.toString());})];
    const view = new EditorView({parent, extensions: extensions('js', true)});
    return {
        set(text, extension, writable) {
            setting = true;
            try {
                view.setState(EditorState.create({doc: text, extensions: extensions(extension, writable)}));
            } finally {
                setting = false;
            }
        },
        replace(text) {view.dispatch({changes: {from: 0, to: view.state.doc.length, insert: text}, annotations: isolateHistory.of('full')});},
        insert(symbol) {
            if (view.state.readOnly) return;
            const close = PAIRS[symbol];
            view.dispatch(view.state.changeByRange(range => ({
                changes: {from: range.from, to: range.to, insert: close ? symbol[0] + view.state.sliceDoc(range.from, range.to) + close : symbol},
                range: close ? EditorSelection.range(range.anchor + 1, range.head + 1) : EditorSelection.cursor(range.from + symbol.length)
            })), {scrollIntoView: true, userEvent: 'input', annotations: isolateHistory.of('full')});
            view.focus();
        },
        undo() {
            if (view.state.readOnly) return;
            undo(view);
            view.focus();
        },
        redo() {
            if (view.state.readOnly) return;
            redo(view);
            view.focus();
        },
        indent(outdent = false) {
            if (view.state.readOnly) return;
            (outdent ? indentLess : indentMore)(view);
            view.focus();
        },
        revealSelection() {
            if (view.hasFocus) view.dispatch({effects: EditorView.scrollIntoView(view.state.selection.main.head)});
        },
        goTo(line, column = 1) {
            const target = view.state.doc.line(Math.min(view.state.doc.lines, Math.max(1, line)));
            const position = Math.min(target.to, target.from + Math.max(0, column - 1));
            view.dispatch({selection: {anchor: position}, effects: EditorView.scrollIntoView(position, {y: 'center'})});
            view.focus();
        }
    };
}
