package com.spotifycontroller;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedString;

final class UnicodeText
{
	private static final String[] FALLBACK_FAMILIES =
	{
		"Segoe UI Emoji",
		"Apple Color Emoji",
		"Noto Color Emoji",
		"Noto Emoji",
		"Segoe UI Symbol",
		"Arial Unicode MS",
		Font.DIALOG,
		Font.SANS_SERIF
	};

	private UnicodeText()
	{
	}

	static int stringWidth(Graphics2D graphics, Font preferred, String text)
	{
		if (text == null || text.isEmpty())
		{
			return 0;
		}
		return (int) Math.ceil(layout(graphics, preferred, text).getAdvance());
	}

	static void drawString(Graphics2D graphics, Font preferred, String text, float x, float baselineY)
	{
		if (text == null || text.isEmpty())
		{
			return;
		}
		layout(graphics, preferred, text).draw(graphics, x, baselineY);
	}

	static Font singleFontForText(Font preferred, String text)
	{
		if (text == null || text.isEmpty() || preferred.canDisplayUpTo(text) < 0)
		{
			return preferred;
		}

		for (String family : FALLBACK_FAMILIES)
		{
			Font candidate = new Font(family, preferred.getStyle(), 1).deriveFont(preferred.getSize2D());
			if (candidate.canDisplayUpTo(text) < 0)
			{
				return candidate;
			}
		}
		return new Font(Font.DIALOG, preferred.getStyle(), 1).deriveFont(preferred.getSize2D());
	}

	private static TextLayout layout(Graphics2D graphics, Font preferred, String text)
	{
		AttributedString attributed = new AttributedString(text);
		attributed.addAttribute(TextAttribute.FONT, preferred);
		for (int index = 0; index < text.length();)
		{
			int codePoint = text.codePointAt(index);
			int nextIndex = index + Character.charCount(codePoint);
			if (!preferred.canDisplay(codePoint) || isEmojiCodePoint(codePoint))
			{
				attributed.addAttribute(TextAttribute.FONT,
					fallbackFor(codePoint, preferred), index, nextIndex);
			}
			index = nextIndex;
		}
		return new TextLayout(attributed.getIterator(), graphics.getFontRenderContext());
	}

	private static Font fallbackFor(int codePoint, Font preferred)
	{
		for (String family : FALLBACK_FAMILIES)
		{
			Font candidate = new Font(family, preferred.getStyle(), 1).deriveFont(preferred.getSize2D());
			if (candidate.canDisplay(codePoint))
			{
				return candidate;
			}
		}
		return new Font(Font.DIALOG, preferred.getStyle(), 1).deriveFont(preferred.getSize2D());
	}

	private static boolean isEmojiCodePoint(int codePoint)
	{
		return codePoint >= 0x1F000 ||
			(codePoint >= 0x2600 && codePoint <= 0x27BF) ||
			codePoint == 0x200D ||
			codePoint == 0xFE0F;
	}
}
