package org.kvj.vimtouch.ext;

import java.util.HashMap;
import java.util.Map;

import org.kvj.vimtouch.ext.Transferable.FieldType;


public class IncomingTransfer {

	private String data;
	private int index = 0;
	private Map<String, FieldReader<?>> readers = new HashMap<String, FieldReader<?>>();

	public IncomingTransfer(String data) {
		this.data = data;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public void readAs(String field, FieldReader<?> reader) {
		readers.put(field, reader);
	}

	public char nextChar() throws FieldReaderException {
		if (index >= data.length()) {
			throw new FieldReaderException("Out of bounds");
		}
		index++;
		return data.charAt(index - 1);
	}

	public String nextPiece() throws FieldReaderException {
		int comma = data.indexOf(',', index);
		if (-1 == comma) {
			throw new FieldReaderException("Delimiter not found");
		}
		String result = data.substring(index, comma);
		index = comma + 1;
		return result;
	}

	public int nextInt() throws FieldReaderException {
		String i = nextPiece();
		try {
			return Integer.parseInt(i, 10);
		} catch (Exception e) {
			throw new FieldReaderException("Invalid number: " + i);
		}
	}

	public String nextString() throws FieldReaderException {
		int length = nextInt();
		String result = data.substring(index, index + length);
		index += length + 1;
		return result;
	}

	public <T> T readWith(FieldReader<T> reader) throws FieldReaderException {
		char type = nextChar();
		if (FieldType.Null.getCode() == type) {
			// Null
			reader.set(null);
			index++;
			return null;
		}
		T value = reader.read(this);
		reader.set(value);
		return value;
	}

	public void read() throws FieldReaderException {
		while (index < data.length()) {
			String field = nextString();
			if ("".equals(field)) {
				// Last field
				return;
			}
			FieldReader<?> reader = readers.get(field);
			if (null == reader) {
				throw new FieldReaderException("No reader found for: " + field);
			}
			readWith(reader);
		}
	}

	public String getData() {
		return data;
	}

	public int getIndex() {
		return index;
	}

}
