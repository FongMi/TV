import {format} from './format.mjs';

self.onmessage = async ({data}) => {
    try {
        self.postMessage({text: await format(data.text, data.language)});
    } catch (error) {
        self.postMessage({error: error.message || String(error)});
    }
};
