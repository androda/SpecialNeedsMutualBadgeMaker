package com.specialneedsmutual.badgemaker.namebadge;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.html.WebColors;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.specialneedsmutual.badgemaker.BadgeMakerWindow42;
import com.specialneedsmutual.badgemaker.utilities.PropertyFileReader;

/**
 * Class which represents a single Special Needs Mutual Name Badge
 * 
 * @author Jacob
 * 
 */
public class SnmNameBadge {

	BadgeType					badgeType;
	BaseColor					badgeColor;
	boolean						detectBadgeTypeFromName;
	boolean						autoScaleImages;
	Font						font;
	File						currentFile;
	File						renamedFile;
	Image						image;
	int							nameLength;
	PdfPTable					badgeTable;
	String						canonicalFilePath;
	String						name;
	String[]					nameParts;
	String						fileExtension;
	String						fileName;
	String						fileNameWithoutPrefix	= null;
	String						filePath;
	String						spaceInNameDelimiter;

	private static final String	fileSeparator			= System.getProperty("file.separator", "/");

	/**
	 * Creates a Badge instance. DOES NOT set the badge background color.
	 * 
	 * @param parentFolderPath The path to the badge's parent folder, like "c:\files\SNM"
	 * @param pathSeparator The path separator, like '/' or '\'
	 * @param fileName The name of the file, like "A_John Doe.jpg" or "Sally Smith.jpg"
	 * @throws IOException Caused by inability to get the image
	 * @throws MalformedURLException Caused by a bad file path
	 * @throws BadElementException Caused by Something I don't really understand
	 */
	public SnmNameBadge(String parentFolderPath,
			String fileName,
			boolean determineBageColorFromFileNaming,
			boolean autoScaleImages) {
		spaceInNameDelimiter = PropertyFileReader.getProperty("spaceInNameDelimiter", "%");
		this.autoScaleImages = autoScaleImages;
		this.fileName = fileName;
		this.filePath = parentFolderPath;
		detectBadgeTypeFromName = determineBageColorFromFileNaming;

		canonicalFilePath = this.filePath + fileSeparator + this.fileName;
	}

	// Found here:
	// http://stackoverflow.com/questions/244164/how-can-i-resize-an-image-using-java
	BufferedImage
			createResizedImage(String filePath, int scaledWidth, int scaledHeight) throws IOException {
		File imageFile;
		BufferedImage originalImageBuffer;
		BufferedImage scaledImageBuffer;
		Graphics2D renderer;
		int imageType = BufferedImage.TYPE_INT_ARGB;

		imageFile = new File(filePath);
		originalImageBuffer = ImageIO.read(imageFile);

		scaledImageBuffer = new BufferedImage(scaledWidth, scaledHeight, imageType);
		renderer = scaledImageBuffer.createGraphics();
		renderer.drawImage(originalImageBuffer, 0, 0, scaledWidth, scaledHeight, null);
		renderer.dispose();
		return scaledImageBuffer;
	}

	/**
	 * Creates the badge table from the settings passed in (auto-determines whether to scale the
	 * image)
	 * 
	 * @throws IOException
	 * @throws BadElementException
	 */
	void buildBadge() throws BadElementException, IOException {
		nameParts = getNameParts();
		nameLength = getNameLength();
		name = getName();
		font = getFont();

		int pictureAbsoluteWidth = Integer.parseInt(PropertyFileReader.getProperty("pictureAbsoluteWidth",
				"126"));
		int pictureAbsoluteHeight = Integer.parseInt(PropertyFileReader.getProperty("pictureAbsoluteHeight",
				"158"));

		if (autoScaleImages) {
			image = Image.getInstance(createResizedImage(canonicalFilePath,
					pictureAbsoluteWidth,
					pictureAbsoluteHeight),
					null);
		} else {
			image = Image.getInstance(canonicalFilePath);
		}

		badgeTable = buildBadgeTable();
	}

	/**
	 * Sets the badge's background color
	 * 
	 * @param backgroundColor The color, like BaseColor.White
	 */
	public void setBadgeBackgroundColor(BaseColor backgroundColor) {
		this.badgeColor = backgroundColor;
	}

	/**
	 * Gets the path to the parent folder of the badge picture
	 * 
	 * @return The path to the badge's parent folder, like "c:\files\SNM"
	 */
	public String getFilePath() {
		return this.filePath;
	}

	/**
	 * Returns the filename
	 * 
	 * @return The name of the file, like "A_John Doe.jpg" or "Sally Smith.jpg"
	 */
	public String getFilename() {
		return this.fileName;
	}

	/**
	 * Returns the full canonical file path for this name badge's picture
	 * 
	 * @return The string, like "c:\somewhere\stuff\picture.jpg" or "/usr/bob/pictures/picture.jpg"
	 */
	public String getCanonicalFilepath() {
		return this.canonicalFilePath;
	}

	/**
	 * Returns the name of the file, split on whitespace.
	 * 
	 * @return The sections of the filename
	 */
	String[] getNameParts() {
		// String looks something like:
		// C:\\Wherever\\Path\\To\\Images\\joe bob.jpg
		// OR
		// /usr/bob/home/things/joe bob.jpg
		// I need:
		// Index of last file separator char
		// Index of the last .
		int separatorIndex = -1;
		int dotIndex = -1;
		String substrung;

		separatorIndex = canonicalFilePath.lastIndexOf(fileSeparator);
		dotIndex = canonicalFilePath.lastIndexOf('.');

		fileExtension = canonicalFilePath.substring(dotIndex);

		if (separatorIndex < 0 || dotIndex < 0) {
			throw new IllegalArgumentException(String.format("The file path '%s' did not contain a '%s' or '%s'",
					canonicalFilePath,
					fileSeparator,
					"."));
		} else {
			substrung = canonicalFilePath.substring((separatorIndex + 1), dotIndex);
			return substrung.split("\\s+");
		}
	}

	/**
	 * Returns the full name as a return-delineated string. Cuts out 'A_' (type prefix) and such.
	 * 
	 * @return "Bob\r\n\r\nDole" and such
	 */
	String getNameWithoutTypePrefix() {
		final String adultLeaderBadgeTag = PropertyFileReader.getProperty("adultLeaderBadgeTag",
				"A_");
		final String youthCounselorBadgeTag = PropertyFileReader.getProperty("youthCounselorBadgeTag",
				"Y_");
		final String specialNeedsYouthBadgeTag = PropertyFileReader.getProperty("specialNeedsYouthBadgeTag",
				"S_");
		String name = "";
		for (String namePart : nameParts) {
			name += dealWithSpaceInNameDelimiter(namePart.replace(adultLeaderBadgeTag, "")
					.replace(youthCounselorBadgeTag, "")
					.replace(specialNeedsYouthBadgeTag, "")) + "\r\n\r\n";
		}
		return name;
	}

	/**
	 * Returns the full name as a return-delineated string
	 * 
	 * @return "John\r\n\r\nDoe", "Mary Ann\r\n\r\nClrk", or other such things.
	 */
	String getName() {
		String name = "";
		for (String namePart : nameParts) {
			name += dealWithSpaceInNameDelimiter(namePart) + "\r\n\r\n";
		}
		return name;
	}

	/**
	 * Converts 'spaceInNameDelimiter' into ' '
	 * 
	 * @param namePart The name part chunk (previously split on ' ')
	 * @return The string where all the space delimiters have been replaced with a space
	 */
	String dealWithSpaceInNameDelimiter(String namePart) {
		if (namePart.contains(spaceInNameDelimiter)) {
			return namePart.replace(spaceInNameDelimiter, " ");
		} else {
			return namePart;
		}
	}

	/**
	 * Returns the length of the name based on which namePart is largest.
	 * 
	 * @return The length of the largest namePart
	 */
	int getNameLength() {
		int biggest = -1;
		int biggerCheck = -1;
		for (String namePart : nameParts) {
			biggerCheck = namePart.trim().length();
			if (biggerCheck > biggest) {
				biggest = biggerCheck;
			}
		}
		return biggest;
	}

	/**
	 * Gets the Font for this name badge based on the length of the person's name
	 * 
	 * @return The Font to use on the badge
	 */
	Font getFont() {
		Font theFont;
		int fontSizeNamesLessThan5Letters = Integer.parseInt(PropertyFileReader.getProperty("fontSizeNamesLessThan5Letters",
				"36"));
		int fontSizeNames6LettersLongOrLess = Integer.parseInt(PropertyFileReader.getProperty("fontSizeNames6LettersLongOrLess",
				"32"));
		int fontSizeNames7or8LettersLong = Integer.parseInt(PropertyFileReader.getProperty("fontSizeNames7or8LettersLong",
				"29"));
		int fontSizeNames9or10LettersLong = Integer.parseInt(PropertyFileReader.getProperty("fontSizeNames9or10LettersLong",
				"26"));
		int fontSizeNames11or12LettersLone = Integer.parseInt(PropertyFileReader.getProperty("fontSizeNames11or12LettersLone",
				"22"));
		int fontSizeNamesMoreThan12LettersLong = Integer.parseInt(PropertyFileReader.getProperty("fontSizeNamesMoreThan12LettersLong",
				"18"));

		if (nameLength < 5) {
			theFont = FontFactory.getFont(BaseFont.HELVETICA, fontSizeNamesLessThan5Letters);
		} else if (nameLength <= 6) {
			theFont = FontFactory.getFont(BaseFont.HELVETICA, fontSizeNames6LettersLongOrLess);
		} else if (nameLength <= 8) {
			theFont = FontFactory.getFont(BaseFont.HELVETICA, fontSizeNames7or8LettersLong);
		} else if (nameLength <= 10) {
			theFont = FontFactory.getFont(BaseFont.HELVETICA, fontSizeNames9or10LettersLong);
		} else if (nameLength <= 12) {
			theFont = FontFactory.getFont(BaseFont.HELVETICA, fontSizeNames11or12LettersLone);
		} else {
			theFont = FontFactory.getFont(BaseFont.HELVETICA, fontSizeNamesMoreThan12LettersLong);
		}

		return theFont;
	}

	/**
	 * Determines the name badge background color based either on the user's selection or by parsing
	 * the filename for tags. Also takes care of adding things to the renameOperations list.
	 * 
	 * @param badge The SnmNameBadge to check
	 * @return The BaseColor which should be used as background
	 */
	BaseColor getBadgeBackgroundColor() {
		String badgeBackgroundColorString = null;
		if (detectBadgeTypeFromName) {
			final String adultLeaderBadgeTag = PropertyFileReader.getProperty("adultLeaderBadgeTag",
					"A_");
			final String youthCounselorBadgeTag = PropertyFileReader.getProperty("youthCounselorBadgeTag",
					"Y_");
			final String specialNeedsYouthBadgeTag = PropertyFileReader.getProperty("specialNeedsYouthBadgeTag",
					"S_");

			if (fileName.contains(adultLeaderBadgeTag)) {
				badgeBackgroundColorString = PropertyFileReader.getProperty("badgeBackgroundColorAdultLeader",
						"#FFFFFF");
				fileNameWithoutPrefix = fileName.replace(adultLeaderBadgeTag, "");
			} else if (fileName.contains(youthCounselorBadgeTag)) {
				badgeBackgroundColorString = PropertyFileReader.getProperty("badgeBackgroundColorYouthCounselor",
						"#FFFA83");
				fileNameWithoutPrefix = fileName.replace(youthCounselorBadgeTag, "");
			} else if (fileName.contains(specialNeedsYouthBadgeTag)) {
				badgeBackgroundColorString = PropertyFileReader.getProperty("badgeBackgroundColorSpecialNeedsYouth",
						"#CADBFE");
				fileNameWithoutPrefix = fileName.replace(specialNeedsYouthBadgeTag, "");
			}

			// If the background color string is still null, then the badge type
			// was not specified.
			// Kill the process.
			if (badgeBackgroundColorString == null || fileNameWithoutPrefix == null) {
				throw new IllegalStateException(String.format("Name badge picture '%s' did not contain a filename tag. Tags are necessary for determining badge type.",
						filePath + fileSeparator + fileName));
			}

			currentFile = new File(filePath + fileSeparator + fileName);
			renamedFile = new File(filePath + fileSeparator + fileNameWithoutPrefix);
			return WebColors.getRGBColor(badgeBackgroundColorString);
		}
		return badgeColor;
	}

	/**
	 * Builds the badgeTable based on the image file and name of the person
	 * 
	 * @return The completed badge PdfPTable, ready to be inserted into the overall pdf
	 */
	PdfPTable buildBadgeTable() {
		int badgeTableOverallWidthPercent = Integer.parseInt(PropertyFileReader.getProperty("badgeTableOverallWidthPercent",
				"100"));

		int badgeTableLeftColumnPercent = Integer.parseInt(PropertyFileReader.getProperty("badgeTableLeftColumnPercent",
				"40"));
		int badgeTableRightColumnPercent = Integer.parseInt(PropertyFileReader.getProperty("badgeTableRightColumnPercent",
				"60"));

		int imageTopPadding = Integer.parseInt(PropertyFileReader.getProperty("pictureTopPadding",
				"25"));
		int imageRightPadding = Integer.parseInt(PropertyFileReader.getProperty("pictureRightPadding",
				"0"));
		int imageBottomPadding = Integer.parseInt(PropertyFileReader.getProperty("pictureBottomPadding",
				"25"));
		int imageLeftPadding = Integer.parseInt(PropertyFileReader.getProperty("pictureLeftPadding",
				"25"));

		int nameTopPadding = Integer.parseInt(PropertyFileReader.getProperty("nameTopPadding", "60"));
		int nameRightPadding = Integer.parseInt(PropertyFileReader.getProperty("nameRightPadding",
				"0"));
		int nameBottomPadding = Integer.parseInt(PropertyFileReader.getProperty("nameBottomPadding",
				"0"));
		int nameLeftPadding = Integer.parseInt(PropertyFileReader.getProperty("nameLeftPadding",
				"35"));

		badgeColor = getBadgeBackgroundColor();
		PdfPTable badgeTable = new PdfPTable(new float[] { badgeTableLeftColumnPercent,
				badgeTableRightColumnPercent });

		badgeTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
		badgeTable.getDefaultCell().setPaddingLeft(-10);
		badgeTable.getDefaultCell().setPaddingRight(-10);
		badgeTable.getDefaultCell().setBorderColorLeft(badgeColor);
		badgeTable.getDefaultCell().setBorderColorRight(badgeColor);

		// http://stackoverflow.com/questions/9417054/itextsharp-set-table-cell-border-color
		PdfPCell imageCell = new PdfPCell(image);

		imageCell.setBackgroundColor(badgeColor);
		imageCell.setUseVariableBorders(true);
		imageCell.setBorderColorTop(BaseColor.BLACK);
		imageCell.setBorderColorBottom(BaseColor.BLACK);

		imageCell.setHorizontalAlignment(1);
		imageCell.setVerticalAlignment(1);
		imageCell.setPaddingLeft(imageLeftPadding);
		imageCell.setPaddingRight(imageRightPadding);
		imageCell.setPaddingTop(imageTopPadding);
		imageCell.setPaddingBottom(imageBottomPadding);
		imageCell.setBorder(Rectangle.BOX);
		imageCell.setBorderColorRight(badgeColor);

		Phrase namePhrase;
		if (fileNameWithoutPrefix == null) {
			namePhrase = new Phrase(name, font);
		} else {
			namePhrase = new Phrase(getNameWithoutTypePrefix(), font);
		}

		PdfPCell nameCell = new PdfPCell(namePhrase);

		nameCell.setBackgroundColor(badgeColor);
		nameCell.setUseVariableBorders(true);
		nameCell.setBorderColorTop(BaseColor.BLACK);
		nameCell.setBorderColorRight(BaseColor.BLACK);
		nameCell.setBorderColorBottom(BaseColor.BLACK);
		nameCell.setBorderColorLeft(badgeColor);

		nameCell.setHorizontalAlignment(1);
		nameCell.setPaddingLeft(nameLeftPadding);
		nameCell.setPaddingRight(nameRightPadding);
		nameCell.setPaddingTop(nameTopPadding);
		nameCell.setPaddingBottom(nameBottomPadding);

		badgeTable.setWidthPercentage(badgeTableOverallWidthPercent);
		badgeTable.addCell(imageCell);
		badgeTable.addCell(nameCell);

		return badgeTable;
	}

	/**
	 * 
	 * @return The NameBadge file as it currently exists
	 */
	public File getPreRenameFile() {
		return currentFile;
	}

	/**
	 * 
	 * @return The NameBadge file after removing any particular prefix
	 */
	public File getPostRenameFile() {
		return renamedFile;
	}

	/**
	 * Gets the badge table - only to be used after successful initialization
	 * 
	 * @return The badge table, BACKGROUND NOT COLORED YET
	 * @throws IOException
	 * @throws BadElementException
	 */
	public PdfPTable getBadgeTable() throws BadElementException, IOException {
		buildBadge();
		return this.badgeTable;
	}
}
