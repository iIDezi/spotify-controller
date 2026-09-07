package com.spotifycontroller;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class UnicodeTextTest
{
	@Test
	public void measuresAndDrawsEmojiText()
	{
		BufferedImage image = new BufferedImage(240, 40, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			Font preferred = new Font(Font.MONOSPACED, Font.PLAIN, 16);
			String title = "Jamzful 🎵🔥";
			assertTrue(UnicodeText.stringWidth(graphics, preferred, title) > 0);
			UnicodeText.drawString(graphics, preferred, title, 2, 22);
			assertTrue(UnicodeText.singleFontForText(preferred, title) != null);
		}
		finally
		{
			graphics.dispose();
		}
	}
}
