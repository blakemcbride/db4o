/* Copyright (C) 2004 - 2006  Versant Inc.  http://www.db4o.com */

package com.db4o.internal.marshall;

import java.util.*;

import com.db4o.*;
import com.db4o.internal.*;


public class PrimitiveMarshaller0 extends PrimitiveMarshaller {
    
    public boolean useNormalClassRead(){
        return true;
    }
    
    public Date readDate(ByteArrayBuffer bytes) {
		final long value = bytes.readLong();
		if (value == Long.MAX_VALUE) {
			return MarshallingConstants0.NULL_DATE;
		}
		return new Date(value);
	}
    
    public Object readInteger(ByteArrayBuffer bytes) {
		final int value = bytes.readInt();
		if (value == Integer.MAX_VALUE) {
			return null;
		}
		return Integer.valueOf(value);
	}

	public Object readFloat(ByteArrayBuffer bytes) {
		Float value = unmarshallFloat(bytes);
		if (value.isNaN()) {
			return null;
		}
		return value;
	}
	
	public Object readDouble(ByteArrayBuffer buffer) {
		Double value = unmarshalDouble(buffer);
		if (value.isNaN()) {
			return null;
		}
		return value;
	}	

	public Object readLong(ByteArrayBuffer buffer) {
		long value = buffer.readLong();
		if (value == Long.MAX_VALUE) {
			return null;
		}
		return Long.valueOf(value);
	}
	
	public Object readShort(ByteArrayBuffer buffer) {
		short value = unmarshallShort(buffer);
		if (value == Short.MAX_VALUE) {
			return null;
		}
		return Short.valueOf(value);
	}
	
	public static Double unmarshalDouble(ByteArrayBuffer buffer) {
		return Double.valueOf(Platform4.longToDouble(buffer.readLong()));
	}

	public static Float unmarshallFloat(ByteArrayBuffer buffer) {
		return Float.valueOf(Float.intBitsToFloat(buffer.readInt()));
	}	
	
	public static short unmarshallShort(ByteArrayBuffer buffer){
		int ret = 0;
		if (Deploy.debug){
			buffer.readBegin(Const4.YAPSHORT);
		}
		for (int i = 0; i < Const4.SHORT_BYTES; i++){
			ret = (ret << 8) + (buffer._buffer[buffer._offset++] & 0xff);
		}
		if (Deploy.debug){
			buffer.readEnd();
		}
		return (short)ret;
	}
}
