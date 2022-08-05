function search() {
    doAction('search', {word: $('#search_key_word').val()});
}

function api() {
    doAction('api', {url: $('#diy_api_url').val()});
}

function push() {
    doAction('push', {url: $('#push_url').val()});
}

function doAction(action, kv) {
    kv['do'] = action;
    $.post('/action', kv, function (data) {
        console.log(data);
    });
    return false;
}

function warnToast(msg) {
    $('#warnToastContent').html(msg);
    $('#warnToast').show();
    setTimeout(() => {
        $('#warnToast').hide();
    }, 1000);
}

function showPanel(id) {
    let tab = $('#tab' + id)[0];
    $(tab).attr('aria-selected', 'true').addClass('weui-bar__item_on');
    $(tab).siblings('.weui-bar__item_on').removeClass('weui-bar__item_on').attr('aria-selected', 'false');
    var panelId = '#' + $(tab).attr('aria-controls');
    $(panelId).css('display', 'block');
    $(panelId).siblings('.weui-tab__panel').css('display', 'none');
}

$(function () {
    $('.weui-tabbar__item').on('click', function () {
        showPanel(parseInt($(this).attr('id').substr(3)));
    });
});