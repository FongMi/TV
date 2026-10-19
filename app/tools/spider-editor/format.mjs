export async function format(text, language) {
    const {format} = await import('prettier/standalone');
    const plugins = language === 'java' ? [(await import('prettier-plugin-java')).default] : [await import('prettier/plugins/babel'), await import('prettier/plugins/estree')];
    return format(text, {parser: language === 'java' ? 'java' : 'babel', plugins, tabWidth: 4});
}
