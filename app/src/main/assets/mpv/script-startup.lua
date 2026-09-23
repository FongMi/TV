-- Preserve the original script name and report initialization before entering mpv's event loop.
local mp = require 'mp'
local name = mp.get_script_name()

-- loadfile keeps the wrapper's script context; expose the imported script's directory instead.
package.path = directory .. '/?.lua;' .. package.path
mp.get_script_directory = function() return directory end

local function publish(state, message)
    mp.set_property_native(status, {name = name, generation = generation, state = state, message = message or ''})
end

publish('loading')
local chunk, err = loadfile(source)
local ok = false
if chunk then
    ok, err = xpcall(chunk, function(err) return debug.traceback(tostring(err), 2) end)
end
if not ok then
    publish('error', err)
    error(err, 0)
end
publish('loaded')
