-- Each button has its own mpv script context. Only the wrapper runs when loaded.
local mp = require 'mp'
local completed = 0

-- loadfile keeps the wrapper's script context; expose the imported script's directory instead.
package.path = directory .. '/?.lua;' .. package.path
mp.get_script_directory = function() return directory end

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
    local chunk, err = loadfile(source)
    local ok = false
    if chunk then
        ok, err = xpcall(chunk, function(err) return debug.traceback(tostring(err), 2) end)
    end
    completed = completed + 1
    publish(ok and 'done' or 'error', ok and '' or err)
end)

publish('ready')
