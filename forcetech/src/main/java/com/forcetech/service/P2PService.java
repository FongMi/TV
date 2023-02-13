package com.forcetech.service;

import com.forcetech.Util;

public class P2PService extends PxPService {

    @Override
    public int getPort() {
        return Util.P2P;
    }
}
