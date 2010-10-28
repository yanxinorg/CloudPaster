package util;

import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import models.BinaryFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.mylyn.internal.wikitext.textile.core.block.PreformattedBlock;
import org.eclipse.mylyn.internal.wikitext.textile.core.phrase.EscapeTextilePhraseModifier;
import org.eclipse.mylyn.internal.wikitext.textile.core.phrase.HyperlinkPhraseModifier;
import org.eclipse.mylyn.internal.wikitext.textile.core.phrase.ImageTextilePhraseModifier;
import org.eclipse.mylyn.internal.wikitext.textile.core.phrase.SimpleTextilePhraseModifier;
import org.eclipse.mylyn.internal.wikitext.textile.core.phrase.SimpleTextilePhraseModifier.Mode;
import org.eclipse.mylyn.wikitext.core.parser.DocumentBuilder.SpanType;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.core.parser.markup.Block;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.mylyn.wikitext.core.parser.markup.phrase.HtmlEndTagPhraseModifier;
import org.eclipse.mylyn.wikitext.core.parser.markup.phrase.HtmlStartTagPhraseModifier;
import org.eclipse.mylyn.wikitext.textile.core.TextileLanguage;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import play.Logger;
import play.ns.com.jhlabs.image.ShearFilter;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

public class WikiUtil {

	public static String cleanUpAndConvertImages(String orgin, String email) {
		Document parse = Jsoup.parse(orgin);
		Elements imgs = parse.getElementsByTag("img");
		Iterator<Element> iterator = imgs.iterator();
		while (iterator.hasNext()) {
			Element img = iterator.next();
			String src = img.attr("src");
			processImg(img, src, email);
		}
		String res = Jsoup.clean(parse.body().html(), Whitelist.basic().addTags("img").addAttributes("img", "align", "alt", "height", "src", "title", "width"));
		return res;
	}

	private static void processImg(Element img, String src, String email) {
		if (src.startsWith("data:image/")) {
			BinaryFile file = BinaryFile.create(null, email);
			if (file == null) {
				img.remove();
				return;
			}
			int dataofffset = src.indexOf(";base64,");
			if (dataofffset > -1) {
				String data = src.substring(dataofffset + ";base64,".length());
				try {
					byte[] decode = Base64.decode(data);
					FileUtils.writeByteArrayToFile(new File(file.path), decode);
					img.attr("src", "/" + file.path.replace('\\', '/'));
				} catch (IOException e) {
					Logger.error(e, "process image faild");
				}
			}
			file.save();
		} else if (src.startsWith("http://") || src.startsWith("https://")) {
			BinaryFile file = BinaryFile.create(null, email);
			if (file == null) {
				img.remove();
				return;
			}
			try {
				Response request = Jsoup.connect(src).referrer(src).userAgent("Mozilla/5.0 (Windows NT 5.1; rv:2.0b6) Gecko/20100101 Firefox/4.0b6").execute();
				FileUtils.writeByteArrayToFile(new File(file.path), request.bodyAsBytes());
				img.attr("src", "/" + file.path.replace('\\', '/'));
			} catch (IOException e) {
				Logger.error(e, "process image faild");
			}
			file.save();
		}
	}
	private static void resizeImage() {

	}
	public static String parse(String text) throws UnsupportedEncodingException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		HtmlDocumentBuilder builder = new HtmlDocumentBuilder(new OutputStreamWriter(out, "UTF-8"));
		builder.setEncoding("UTF-8");
		builder.setEmitAsDocument(false);
		MarkupParser parser = new MarkupParser();
		parser.setMarkupLanguage(textileLanguage);
		parser.setBuilder(builder);
		parser.parse(text);
		return out.toString("UTF-8");
	}

	private static MarkupLanguage textileLanguage = new TextileLanguage() {
		@Override
		protected void addStandardPhraseModifiers(PatternBasedSyntax phraseModifierSyntax) {
			boolean escapingHtml = configuration == null ? false : configuration.isEscapingHtmlAndXml();

			phraseModifierSyntax.add(new HtmlEndTagPhraseModifier(escapingHtml));
			phraseModifierSyntax.add(new HtmlStartTagPhraseModifier(escapingHtml));
			phraseModifierSyntax.beginGroup("(?:(?<=[\\s\\.,\\\"'?!;:\\)\\(\\{\\}\\[\\]])|^)(?:", 0); //$NON-NLS-1$
			phraseModifierSyntax.add(new EscapeTextilePhraseModifier());
			phraseModifierSyntax.add(new SimpleTextilePhraseModifier("**", SpanType.BOLD, Mode.NESTING)); //$NON-NLS-1$
			//phraseModifierSyntax.add(new SimpleTextilePhraseModifier("??", SpanType.CITATION, Mode.NESTING)); //$NON-NLS-1$
			phraseModifierSyntax.add(new SimpleTextilePhraseModifier("__", SpanType.ITALIC, Mode.NESTING)); //$NON-NLS-1$
			//phraseModifierSyntax.add(new SimpleTextilePhraseModifier("_", SpanType.EMPHASIS, Mode.NESTING)); //$NON-NLS-1$
			phraseModifierSyntax.add(new SimpleTextilePhraseModifier("*", SpanType.STRONG, Mode.NESTING)); //$NON-NLS-1$
			//phraseModifierSyntax.add(new SimpleTextilePhraseModifier("+", SpanType.INSERTED, Mode.NESTING)); //$NON-NLS-1$
			phraseModifierSyntax.add(new SimpleTextilePhraseModifier("~", SpanType.SUBSCRIPT, Mode.NORMAL)); //$NON-NLS-1$
			phraseModifierSyntax.add(new SimpleTextilePhraseModifier("^", SpanType.SUPERSCRIPT, Mode.NORMAL)); //$NON-NLS-1$
			//phraseModifierSyntax.add(new SimpleTextilePhraseModifier("@", SpanType.CODE, Mode.SPECIAL)); //$NON-NLS-1$
			//phraseModifierSyntax.add(new SimpleTextilePhraseModifier("%", SpanType.SPAN, Mode.NESTING)); //$NON-NLS-1$
			phraseModifierSyntax.add(new SimpleTextilePhraseModifier("-", SpanType.DELETED, Mode.NESTING)); //$NON-NLS-1$
			phraseModifierSyntax.add(new LinkTextilePhraseModifier("/view/%s")); //$NON-NLS-1$
			phraseModifierSyntax.add(new ImageTextilePhraseModifier());
			phraseModifierSyntax.add(new HyperlinkPhraseModifier());
			// hyperlinks are actually a phrase modifier see bug 283093
			phraseModifierSyntax.endGroup(")(?=\\W|$)", 0); //$NON-NLS-1$
		}

		@Override
		protected void addStandardBlocks(List<Block> blocks, List<Block> paragraphBreakingBlocks) {
			blocks.add(new PreformattedBlock());
			// blocks.add(new QuoteBlock());
			// blocks.add(new CodeBlock());
			// blocks.add(new FootnoteBlock());
		}
	};

}
