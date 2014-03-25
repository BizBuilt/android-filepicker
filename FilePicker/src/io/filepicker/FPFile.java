/* PBM LLC Description of Fixed Issues
 * All of fixed parts is marked PBM LLC
 *  
 * 1. JSON data parsing is fixed.
 * 
 */

package io.filepicker;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class FPFile implements Parcelable {
	
	private final String localpath;
	private final String fpurl;
	private final long size;
	private final String type;
	private final String key;
	private final String filename;

	/**
	 * Parcelable factory
	 */
	public static final Parcelable.Creator<FPFile> CREATOR =  new Parcelable.Creator<FPFile>() {
		public FPFile createFromParcel(Parcel in) {
			return new FPFile(in);
		}

		public FPFile[] newArray(int size) {
			return new FPFile[size];
		}
	};

	/**
	 * Parcelable constructor
	 * @param in
	 */
	public FPFile(Parcel in) {
		//The order of these variables must match exactly to the order
		//in the parcel writer
		this.localpath = in.readString();
		this.fpurl = in.readString();
		this.size = in.readLong();
		this.type = in.readString();
		this.key = in.readString();
		this.filename = in.readString();
	}

	/**
	 * Explicit constructor
	 * 
	 * @param localpath
	 * @param fpurl
	 * @param size
	 * @param type
	 * @param key
	 * @param filename
	 */
	public FPFile(String localpath, String fpurl, long size, String type, String key, String filename) {
		this.localpath = localpath;
		this.fpurl = fpurl;
		this.size = size;
		this.type = type;
		this.key = key;
		this.filename = filename;
	}

	/**
	 * Construct FPFile based on response. Must be of the format
	 * <pre>
	 * {@code
	 * {
     *   "url": "https://www.filepicker.io/api/file/CAoBl1bORiOXQVZMUyXM",
     *   "data": {
     *   "size": 2287265,
     *     "type": "text/plain",
     *     "key": "498rgTBaQW6rub4rRftq_testfile.file",
     *     "filename": "testfile.file"
     *   }
     * }
	 * </pre>
	 * @param localpath
	 * @param data
	 */
	
/* PBM LLC
 * 
 * There is no "data" field at json data to be got.
 * Original code tried to parse "data" field.
 * So Original code had crash issue by json exception.
 * JSon parse part is fixed. 
 */
	
	public FPFile(String localpath, JSONObject data, boolean isNeedKey, boolean isNeedData) {
		this.localpath = localpath;
		try {
			this.fpurl = data.getString("url");	
			
			if(isNeedData == true){
				JSONObject dataKey = data.getJSONObject("data");
				this.size = dataKey.getLong("size");
				this.type = dataKey.getString("type");
				
				if(isNeedKey)
					this.key = dataKey.getString("key");
				else
					this.key = "";
				
				this.filename = dataKey.getString("filename");
				
			}else{
				
				this.size = data.getLong("size");
				this.type = data.getString("type");
				
				if(isNeedKey)
					this.key = data.getString("key");
				else
					this.key = "";
				
				this.filename = data.getString("filename");
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getLocalPath() {
		return this.localpath;
	}

	public String getFPUrl() {
		return this.fpurl;
	}

	public long getSize() {
		return this.size;
	}

	public String getType() {
		return this.type;
	}

	public String getKey() {
		return this.key;
	}
	
	public String getFilename() {
		return this.filename;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		//The order of these variables must match exactly to the order
		//in the parcel constructor
		out.writeString(localpath);
		out.writeString(fpurl);
		out.writeLong(size);
		out.writeString(type);
		out.writeString(key);
		out.writeString(filename);
	}
	
	@Override
	public String toString() {
		return FPFile.class.getSimpleName() 
				+ ", filename: " + filename
				+ ", type: " + type;
	}
}
