package org.kvj.vimtouch;

import android.os.Parcel;
import android.os.Parcelable;

public class TransferableData implements Parcelable {

	private String data = null;

	public TransferableData(String data) {
		this.data = data;
	}

	public static final Parcelable.Creator<TransferableData> CREATOR = new Creator<TransferableData>() {

		@Override
		public TransferableData[] newArray(int len) {
			return new TransferableData[len];
		}

		@Override
		public TransferableData createFromParcel(Parcel p) {
			TransferableData td = new TransferableData(p.readString());
			return td;
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(data);
	}

	public String getData() {
		return data;
	}

}
