package org.kvj.vimtouch;

import org.kvj.vimtouch.TransferableData;

interface IntegrationProvider {
    void sendEvent(in int type, in TransferableData data);
    int nextSubscription();
}
