package net.momodalo.app.vimtouch.ext.manager;

import net.momodalo.app.vimtouch.ext.Transferable;

public interface IntegrationExtension<I extends Transferable, O extends Transferable> {
	
	public String getType();
	
	public I newInput();
	
	public O process(I input) throws IntegrationExtensionException;

}
