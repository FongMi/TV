-- Execute mpv input.conf command syntax while reporting the result to the Android UI.
local mp = require 'mp'
local completed = 0

local function publish(state, message)
    mp.set_property_native(status, {
        generation = generation,
        state = state,
        completed = completed,
        message = message or '',
    })
end

mp.add_key_binding(nil, 'run', function()
    publish('running')
    local invoked, success, message = xpcall(function()
        return mp.command(command)
    end, function(value)
        return debug.traceback(tostring(value), 2)
    end)
    local ok = invoked and success
    local err = invoked and (message or '') or success
    completed = completed + 1
    publish(ok and 'done' or 'error', ok and '' or err)
end)

publish('ready')
