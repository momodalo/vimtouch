package net.momodalo.app.vimtouch.ext.impl.read;

import java.util.ArrayList;
import java.util.List;

import net.momodalo.app.vimtouch.ext.FieldReader;
import net.momodalo.app.vimtouch.ext.FieldReaderException;
import net.momodalo.app.vimtouch.ext.IncomingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable.FieldType;

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
