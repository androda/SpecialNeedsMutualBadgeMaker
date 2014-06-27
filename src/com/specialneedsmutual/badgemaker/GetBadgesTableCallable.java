package com.specialneedsmutual.badgemaker;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.html.WebColors;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.specialneedsmutual.badgemaker.helpers.SourceDirectoryInfo;
import com.specialneedsmutual.badgemaker.namebadge.BadgeType;
import com.specialneedsmutual.badgemaker.namebadge.SnmNameBadge;
import com.specialneedsmutual.badgemaker.utilities.PropertyFileReader;

/**
 * Builds the PdfPTable which contains all of the name badges. Just add this to your Document and
 * go!
 * 
 * @author Jacob
 * 
 */
public class GetBadgesTableCallable implements Callable<PdfPTable> {

	/*
	 * 
	 * Put necessary variables here
	 */

	final SourceDirectoryInfo	sourceInfo;
	final boolean				detectBadgeTypeFromName;
	final boolean				autoScaleImages;
	final BaseColor				badgeBackgroundSelectedColor;
	List<RenameOperation>		renamesToPerform	= new ArrayList<>();
	final BadgeType				selectedBadgeType;

	public GetBadgesTableCallable(SourceDirectoryInfo sourceInfo,
			boolean detectTypeFromName,
			boolean autoScaleImages,
			BaseColor selectedBadgeTypeColor,
			BadgeType selectedBadgeType) {
		this.sourceInfo = sourceInfo;
		this.detectBadgeTypeFromName = detectTypeFromName;
		this.autoScaleImages = autoScaleImages;
		this.badgeBackgroundSelectedColor = selectedBadgeTypeColor;
		this.selectedBadgeType = selectedBadgeType;
	}

	@Override
	public PdfPTable call() throws Exception {
		// TODO Auto-generated method stub
		PdfPTable overallTable = null;

		SnmNameBadge nameBadge;
		PdfPTable badgeTable;
		int overallTableWidthPercent;
		int overallTableLeftColumnPercent;
		int overallTableRightColumnPercent;
		Date startTime = Calendar.getInstance().getTime();
		Date endTime;
		// ImageIO.setUseCache(false);
		// errorsArea
		// .setText("Any errors in creating the badges will appear here.");
		String propertyInitErrors = PropertyFileReader.initialize();
		if (propertyInitErrors != null) {
			// errorsArea.setText(String.format(
			// "%s%nError when trying to read from properties: %n%s",
			// errorsArea.getText(), propertyInitErrors));
		}

		overallTableWidthPercent = Integer.parseInt(PropertyFileReader.getProperty("overallTableWidthPercent",
				"100"));
		overallTableLeftColumnPercent = Integer.parseInt(PropertyFileReader.getProperty("overallTableLeftColumnPercent",
				"50"));
		overallTableRightColumnPercent = Integer.parseInt(PropertyFileReader.getProperty("overallTableRightColumnPercent",
				"50"));

		// badgeBackgroundColorAdultLeader = WebColors
		// .getRGBColor(PropertyFileReader.getProperty(
		// "badgeBackgroundColorAdultLeader", "#FFFFFF"));
		// badgeBackgroundColorYouthCounselor = WebColors
		// .getRGBColor(PropertyFileReader.getProperty(
		// "badgeBackgroundColorYouthCounselor", "#FFFA83"));
		// badgeBackgroundColorSpecialNeedsYouth = WebColors
		// .getRGBColor(PropertyFileReader.getProperty(
		// "badgeBackgroundColorSpecialNeedsYouth", "#CADBFE"));

		overallTable = new PdfPTable(new float[] { overallTableLeftColumnPercent,
				overallTableRightColumnPercent });
		overallTable.setWidthPercentage(overallTableWidthPercent);
		overallTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

		// if (sourceDir == null || sourceDir.isEmpty()) {
		// errorsArea
		// .setText("Please select a source directory before making badges.");
		// return;
		// }

		try {
			for (String filename : sourceInfo.childFiles) {
				nameBadge = new SnmNameBadge(sourceInfo.sourceDir,
						filename,
						detectBadgeTypeFromName,
						autoScaleImages);

				if (!detectBadgeTypeFromName) {
					// Set the background color based on what was passed in
					nameBadge.setBadgeBackgroundColor(badgeBackgroundSelectedColor);
				}

				badgeTable = nameBadge.getBadgeTable();
				if (detectBadgeTypeFromName) {
					RenameOperation op = new RenameOperation();
					op.preRename = nameBadge.getPreRenameFile();
					op.postRename = nameBadge.getPostRenameFile();
					renamesToPerform.add(op);
				}

				overallTable.addCell(badgeTable);
				overallTable.addCell(badgeTable);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// errorsArea.setText(String.format("%s%n%s", errorsArea.getText(),
			// e.getMessage()));
		} finally {
			// TODO: THIS is where you rename all the name badges at once.
			// Don't rename them as you go along, because there might have been
			// an error.
			// An error mid-process would leave you with some badges renamed,
			// and others not.

			endTime = Calendar.getInstance().getTime();
			// errorsArea.setText(String.format(
			// "%s%nStart Time: %s%nEnd Time: %s", errorsArea.getText(),
			// startTime.toString(), endTime.toString()));
		}

		return overallTable;
	}

	/**
	 * Used only for renaming Files in batches after the badge creation process is complete.
	 * 
	 * @author Jacob
	 * 
	 */
	class RenameOperation {

		/**
		 * The File as it exists now
		 */
		public File	preRename;
		/**
		 * The File you want to rename to
		 */
		public File	postRename;

	}

}
