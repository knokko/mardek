package playground;

import com.github.knokko.ui.renderer.text.Font;

import java.io.IOException;
import java.util.Objects;

public class TextPlayground {

	public static void main(String[] args) throws IOException {
		var fontInput = Objects.requireNonNull(TextPlayground.class.getClassLoader().getResourceAsStream(
				"com/github/knokko/ui/renderer/Code2003.ttf"
		));
		var fontBytes = fontInput.readAllBytes();
		fontInput.close();

		var font = new Font(fontBytes);
	}
}
