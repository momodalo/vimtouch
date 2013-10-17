package org.kvj.vimtouch;

import org.kvj.vimtouch.TransferableData;

interface BasePlugin {

    String getName();
    
    TransferableData process(in TransferableData data);

}
