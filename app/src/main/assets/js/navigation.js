const WEB_TABS = {
    search: {panel: 1, title: 'search'}, push: {panel: 1, title: 'push'},
    play: {panel: 2, title: 'remoteTitle'}, subtitles: {panel: 3, title: 'subtitles'}, danmaku: {panel: 3, title: 'danmaku'},
    settings: {panel: 4, title: 'settings'}, files: {panel: 5, title: 'local'}
};

function webRoute(search) {
    const params = new URLSearchParams(search), tab = params.get('tab');
    const name = Object.hasOwn(WEB_TABS, tab) ? tab : 'search';
    return {tab: name, panel: WEB_TABS[name].panel, path: name === 'files' ? params.get('path') || '' : ''};
}

function webPageUrl(tab, path = '') {
    const params = new URLSearchParams({tab});
    if (tab === 'files' && path) params.set('path', path);
    return '/?' + params;
}

function remoteReturnRoute(search) {
    const params = new URLSearchParams(search);
    params.set('tab', params.get('from') || 'play');
    return webRoute(params);
}

if (typeof module !== 'undefined') module.exports = {WEB_TABS, webRoute, webPageUrl, remoteReturnRoute};
