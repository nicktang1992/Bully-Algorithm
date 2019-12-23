package bully;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {
	public static int hashIP(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] hash = md.digest(s.getBytes());
			int hashVal = 0;
			for (int i = 1;i<=4;i++) {
				byte mbyte = hash[hash.length-i];
				hashVal = hashVal*256;
				hashVal = hashVal + Byte.toUnsignedInt(mbyte);
			}
			return hashVal;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return Integer.MIN_VALUE;
	}
}
