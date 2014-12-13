package org.kvj.vimtouch.ext.impl.read;

import java.util.HashMap;
import java.util.Map;

import org.kvj.vimtouch.ext.FieldReader;
import org.kvj.vimtouch.ext.FieldReaderException;
import org.kvj.vimtouch.ext.IncomingTransfer;
import org.kvj.vimtouch.ext.Transferable.FieldType;

public abstract class MapFieldReader<T> implements FieldReader<Map<String, T>> {

	private FieldReader<T> itemReader;
	private StringFieldReader nameReader = new StringFieldReader() {

		@Override
		public void set(String value) {
		}
	};

	public MapFieldReader(FieldReader<T> itemReader) {
		this.itemReader = itemReader;
	}

	@Override
	public FieldType getType() {
		return FieldType.Map;
	}

	@Override
	public Map<String, T> read(IncomingTransfer t) throws FieldReaderException {
		int size = t.nextInt();
		Map<String, T> data = new HashMap<String, T>(size);
		set(data);
		for (int i = 0; i < size; i++) {
			String name = t.readWith(nameReader);
			T value = t.readWith(itemReader);
			add(name, value);
		}
		return data;
	}

	abstract public void add(String name, T value);

}
