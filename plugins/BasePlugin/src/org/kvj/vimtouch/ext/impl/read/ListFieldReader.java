package org.kvj.vimtouch.ext.impl.read;

import java.util.ArrayList;
import java.util.List;

import org.kvj.vimtouch.ext.FieldReader;
import org.kvj.vimtouch.ext.FieldReaderException;
import org.kvj.vimtouch.ext.IncomingTransfer;
import org.kvj.vimtouch.ext.Transferable.FieldType;

public abstract class ListFieldReader<T> implements FieldReader<List<T>> {

	private FieldReader<T> itemReader;

	public ListFieldReader(FieldReader<T> itemReader) {
		this.itemReader = itemReader;
	}

	@Override
	public FieldType getType() {
		return FieldType.List;
	}

	@Override
	public List<T> read(IncomingTransfer t) throws FieldReaderException {
		int size = t.nextInt();
		List<T> data = new ArrayList<T>(size);
		set(data);
		// System.out.println("List size: " + size);
		for (int i = 0; i < size; i++) {
			T value = t.readWith(itemReader);
			// System.out.println("List item: " + value);
			add(value);
		}
		return data;
	}

	abstract public void add(T value);

}
