package net.momodalo.app.vimtouch.ext;

public interface Transferable {
	public enum FieldType {
		String('s'), Number('i'), Double('d'), List('l'), Map('m'), Transferable(
				'o'), Boolean('b'), Null('n');

		private char code;

		private FieldType(char code) {
			this.code = code;
		}

		public char getCode() {
			return code;
		}
	}

	public void readFrom(IncomingTransfer t);

	public void writeTo(OutgoingTransfer t);
}

