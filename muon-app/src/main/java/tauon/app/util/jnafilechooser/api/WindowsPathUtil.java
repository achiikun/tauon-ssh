package tauon.app.util.jnafilechooser.api;

public class WindowsPathUtil {

	public static String windowsFileDialogPathParse(byte[] bytes){

		// Windows strings are encoded in UTF-16, LittleEndian and null-terminated
		// We must convert it to BigEndian and find that termination.

		StringBuilder buffer = new StringBuilder();

		int pointer = 0;
		while(pointer < bytes.length && (bytes[pointer] != 0 || bytes[pointer+1] != 0)){

			char charr = (char) (((char)bytes[pointer]) | ((char)bytes[pointer+1] << 8));
			buffer.append(charr);

			pointer += 2;
		}

		return buffer.toString();

	}
	
}