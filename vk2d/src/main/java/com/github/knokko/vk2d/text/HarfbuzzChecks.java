package com.github.knokko.vk2d.text;

public class HarfbuzzChecks {

	public static long assertHbSuccess(long result, String functionName) {
		if (result == 0L) throw new RuntimeException("hb_" + functionName + " returned 0");
		return result;
	}

	public static <T> T assertHbSuccess(T result, String functionName) {
		if (result == null) throw new RuntimeException("hb_" + functionName + " returned null");
		return result;
	}

	public static void assertHbSuccess(boolean result, String functionName) {
		if (!result) throw new RuntimeException("hb_" + functionName + " returned false");
	}
}
