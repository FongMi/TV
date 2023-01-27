package com.forcetech.service;

import com.forcetech.Util;

public class P4PService extends PxPService {

    @Override
    public int getPort() {
        return Util.P4P;
    }
}
