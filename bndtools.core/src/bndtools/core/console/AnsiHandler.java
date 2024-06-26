package bndtools.core.console;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

/**
 * Provides same extremely rudimentary support for ansi codes. So far only clear
 *
 * @deprecated Not used anymore Eclipse already supports ANSI now
 */
@Deprecated
public class AnsiHandler implements IPatternMatchListener {

	private TextConsole console;

	@Override
	public void connect(TextConsole console) {
		this.console = console;

	}

	@Override
	public void disconnect() {
		console = null;
	}

	@Override
	public void matchFound(PatternMatchEvent event) {
		Display.getDefault()
			.asyncExec(() -> {
				try {
					if (console == null) {
						// ignore
						return;
					}
					console.getDocument()
						.replace(0, event.getOffset() + event.getLength(), "");
				} catch (BadLocationException e) {
					// ignore
				}
			});

	}

	@Override
	public String getPattern() {
		return "\u001B[2J";
	}

	@Override
	public int getCompilerFlags() {
		return 0;
	}

	@Override
	public String getLineQualifier() {
		return null;
	}

}
