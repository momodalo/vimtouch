package org.kvj.vimtouch.ext.manager;

import org.kvj.vimtouch.ext.Transferable;

public interface IntegrationExtension<I extends Transferable, O extends Transferable> {
	
	public static final String PLUGIN_ACTION = "vimtouch.REMOTE_PLUGIN";
	public static final String PROVIDER_ACTION = "vimtouch.REMOTE_PROVIDER";

	public String getType();
	
	public I newInput();
	
	public O process(I input) throws IntegrationExtensionException;

}
