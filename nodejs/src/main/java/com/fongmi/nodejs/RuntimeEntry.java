package com.fongmi.nodejs;

import android.content.Context;
import android.content.Intent;

import com.github.catvod.Runtime;
import com.github.catvod.runtime.NodeRuntimeApi;

public final class RuntimeEntry implements NodeRuntimeApi {

    @Override
    public Client create(Context context, Callback callback) {
        return new NodeClient(context, callback);
    }

    @Override
    public void check() throws Exception {
        new NodeClient(Runtime.context(), null).check();
    }

    @Override
    public void startProcess(Context context, Intent intent) {
        new NodeService(context).start(intent);
    }
}
