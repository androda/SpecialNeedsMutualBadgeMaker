package com.specialneedsmutual.badgemaker;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.specialneedsmutual.badgemaker.helpers.SourceDirectoryInfo;
import com.specialneedsmutual.badgemaker.namebadge.BadgeType;
import com.specialneedsmutual.badgemaker.namebadge.SnmNameBadge;
import com.specialneedsmutual.badgemaker.utilities.PropertyFileReader;

public class BadgeMakerWindow extends JPanel implements ActionListener,
		PropertyChangeListener {

	private static final long serialVersionUID = 1733336083920378043L;
	private JFrame frmSnmBadgeMaker;

	JButton btnChooseSourceDirectory;
	static final String btnChooseSourceActionCommand = "ChooseSourceDir";
	JButton btnMakeBadges;
	static final String btnMakeBadgesActionCommand = "MakeBadges";
	JButton btnChangeSettings;
	static final String btnChangeSettingsActionCommand = "ChangeSettings";
	JCheckBox chckbxDetectBadgeType;
	static final String chckbxDetectBadgeTypeActionCommand = "DetectBadgeType";
	JCheckBox chckbxAutoscalePictures;
	static final String chckbxAutoScaleActionCommand = "AutoScalePictures";
	boolean detectBadgeTypeFromName = false;
	boolean autoScalePictures = true;
	JTextArea sourceInfoTextPane;
	JTextArea errorsArea;
	BadgeType selectedOutputType = null;
	JProgressBar progressBar;
	JRadioButton rdbtnAdultLeader;
	static final String rdbtnAdultLeaderActionCommand = "AdultLeaderRadio";
	JRadioButton rdbtnYouthCounselor;
	static final String rdbtnYouthCounselorActionCommand = "YouthCounselorRadio";
	JRadioButton rdbtnSpecialNeedsYouth;
	static final String rdbtnSpecialNeedsActionCommand = "SpecialNeedsYouthRadio";
	List<RenameOperation> renamesToPerform = new ArrayList<>();

	SourceDirectoryInfo sourceInfo = null;
	String informationText;
	String determineFromBadgeNaming = "AL_ is for Adult Leaders, YC_ is for Youth Counselors, and SN_ is for Special Needs Youth.  Each picture must have one of these prefixes or the entire operation fails.  The prefix will be automatically removed.";
	final String progressFormat = "Working on Badge %s/%s";

	FutureTask<PdfPTable> getBadgesTableTask;

	//
	// Settings read in from properties file
	//
	// The settings shown here are the defaults, in case no properties file is
	// used/found
	//
	int overallTableWidthPercent = 100;
	int overallTableLeftColumnPercent = 50;
	int overallTableRightColumnPercent = 50;

	Document theDocument;

	public JLabel lblLicenseAgreementAccept;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {

			public void run() {
				try {
					BadgeMakerWindow window = new BadgeMakerWindow();
					window.frmSnmBadgeMaker.setVisible(true);

					int choice = JOptionPane
							.showConfirmDialog(
									window,
									"Do you accept the terms of the GNU Affero GPL?\r\nThe text of this license should be included in the folder which you unzipped.\r\nIf the license is not present, please find it here: https://github.com/androda/SpecialNeedsMutualBadgeMaker/blob/master/LICENSE.\r\nYou may not use this software if you do not agree with the license.",
									"Accept License?",
									JOptionPane.YES_NO_OPTION);
					if (choice == JOptionPane.YES_OPTION) {
						window.lblLicenseAgreementAccept
								.setText("License Agreement Accepted");
					} else {
						System.exit(0);
					}
					//
					// Load the default (built-in) Properties file
					//
					String propertyInitErrors = PropertyFileReader.initialize(BadgeMakerWindow.class
							.getResourceAsStream("/snmBadge.properties"));
					if (propertyInitErrors != null) {
						window.errorsArea.setText(String
								.format("%s%nError when trying to read from properties: %n%s",
										window.errorsArea.getText(),
										propertyInitErrors));
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public BadgeMakerWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmSnmBadgeMaker = new JFrame();
		frmSnmBadgeMaker
				.setIconImage(Toolkit
						.getDefaultToolkit()
						.getImage(
								BadgeMakerWindow.class
										.getResource("/com/sun/java/swing/plaf/windows/icons/TreeOpen.gif")));
		frmSnmBadgeMaker.setTitle("Special Needs Mutual Badge Maker");
		frmSnmBadgeMaker.setResizable(true);
		frmSnmBadgeMaker.setBounds(100, 100, 680, 500);
		frmSnmBadgeMaker.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmSnmBadgeMaker.getContentPane().setLayout(null);

		btnMakeBadges = new JButton("Make Badges");
		btnMakeBadges.addActionListener(BadgeMakerWindow.this);
		btnMakeBadges
				.setToolTipText("Makes the name badges PDF based on your selected settings.");
		btnMakeBadges.setActionCommand(btnMakeBadgesActionCommand);
		btnMakeBadges.setBounds(12, 380, 119, 25);
		frmSnmBadgeMaker.getContentPane().add(btnMakeBadges);

		btnChooseSourceDirectory = new JButton("Choose Source Directory");
		btnChooseSourceDirectory.addActionListener(BadgeMakerWindow.this);
		btnChooseSourceDirectory
				.setToolTipText("The Source Directory is where your scaled-down photos (scaled to 169 by 211 pixels) are located.  The only supported format is .jpg (can also be .JPG).  The output PDF will be placed in this directory.");
		btnChooseSourceDirectory.setActionCommand(btnChooseSourceActionCommand);
		btnChooseSourceDirectory.setBounds(12, 13, 197, 25);
		frmSnmBadgeMaker.getContentPane().add(btnChooseSourceDirectory);

		sourceInfoTextPane = new JTextArea();
		sourceInfoTextPane
				.setToolTipText("Shows information about the selected Source Directory");
		sourceInfoTextPane.setBounds(12, 56, 638, 75);
		frmSnmBadgeMaker.getContentPane().add(sourceInfoTextPane);

		JLabel lblBadgeType = new JLabel("Badge Type:");
		lblBadgeType.setBounds(12, 252, 80, 16);
		frmSnmBadgeMaker.getContentPane().add(lblBadgeType);

		rdbtnAdultLeader = new JRadioButton("Adult Leader");
		rdbtnAdultLeader.setSelected(true);
		rdbtnAdultLeader.addActionListener(BadgeMakerWindow.this);
		rdbtnAdultLeader
				.setToolTipText("Turns all the pictures in the Source Directory into Adult Leader badges.");
		rdbtnAdultLeader.setActionCommand(rdbtnAdultLeaderActionCommand);
		rdbtnAdultLeader.setBounds(12, 280, 127, 25);
		frmSnmBadgeMaker.getContentPane().add(rdbtnAdultLeader);

		rdbtnYouthCounselor = new JRadioButton("Youth Counselor");
		rdbtnYouthCounselor.addActionListener(BadgeMakerWindow.this);
		rdbtnYouthCounselor
				.setToolTipText("Turns all the pictures in the Source Directory into Youth Counselor badges.");
		rdbtnYouthCounselor.setActionCommand(rdbtnYouthCounselorActionCommand);
		rdbtnYouthCounselor.setBounds(12, 308, 127, 25);
		frmSnmBadgeMaker.getContentPane().add(rdbtnYouthCounselor);

		rdbtnSpecialNeedsYouth = new JRadioButton("Special Needs Youth");
		rdbtnSpecialNeedsYouth.addActionListener(BadgeMakerWindow.this);
		rdbtnSpecialNeedsYouth
				.setToolTipText("Turns all the pictures in the Source Directory into Special Needs Youth badges.");
		rdbtnSpecialNeedsYouth.setActionCommand(rdbtnSpecialNeedsActionCommand);
		rdbtnSpecialNeedsYouth.setBounds(12, 338, 155, 25);
		frmSnmBadgeMaker.getContentPane().add(rdbtnSpecialNeedsYouth);

		JLabel lblNoteTheSource = new JLabel(
				"The source directory is the destination for the PDF");
		lblNoteTheSource.setBounds(221, 17, 299, 16);
		frmSnmBadgeMaker.getContentPane().add(lblNoteTheSource);

		errorsArea = new JTextArea();
		errorsArea.setFont(new Font("Tahoma", Font.PLAIN, 13));
		errorsArea
				.setText("Any errors in creating the badges will appear here.");
		errorsArea.setBounds(351, 150, 299, 255);
		errorsArea.setWrapStyleWord(true);
		frmSnmBadgeMaker.getContentPane().add(errorsArea);

		chckbxAutoscalePictures = new JCheckBox("Auto-Scale Pictures");
		chckbxAutoscalePictures.setSelected(true);
		chckbxAutoscalePictures.addActionListener(BadgeMakerWindow.this);
		chckbxAutoscalePictures.setActionCommand(chckbxAutoScaleActionCommand);
		chckbxAutoscalePictures.setBounds(12, 218, 155, 25);
		frmSnmBadgeMaker.getContentPane().add(chckbxAutoscalePictures);

		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		progressBar.setBounds(12, 418, 638, 25);
		progressBar.setMinimum(0);
		frmSnmBadgeMaker.getContentPane().add(progressBar);

		chckbxDetectBadgeType = new JCheckBox(
				"Detect Badge Type From Picture Naming");
		chckbxDetectBadgeType.addActionListener(BadgeMakerWindow.this);
		chckbxDetectBadgeType.setToolTipText(determineFromBadgeNaming);
		chckbxDetectBadgeType
				.setActionCommand(chckbxDetectBadgeTypeActionCommand);
		chckbxDetectBadgeType.setBounds(12, 188, 259, 25);
		frmSnmBadgeMaker.getContentPane().add(chckbxDetectBadgeType);

		lblLicenseAgreementAccept = new JLabel(
				"License Agreement Not Accepted Yet");
		lblLicenseAgreementAccept.setBounds(12, 140, 269, 16);
		frmSnmBadgeMaker.getContentPane().add(lblLicenseAgreementAccept);

		btnChangeSettings = new JButton("Change Settings");
		btnChangeSettings.addActionListener(BadgeMakerWindow.this);
		btnChangeSettings.setActionCommand(btnChangeSettingsActionCommand);
		btnChangeSettings
				.setToolTipText("Allows you to select a new Properties file from which to read name badge settings.  You must use this button every time you make changes to the settings.");
		btnChangeSettings.setBounds(174, 380, 155, 25);
		frmSnmBadgeMaker.getContentPane().add(btnChangeSettings);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final String actionCommand = e.getActionCommand();

		if (actionCommand.equals(btnChooseSourceActionCommand)) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnValue = fileChooser.showOpenDialog(this);
			if (returnValue == JFileChooser.APPROVE_OPTION) {
				File selected = fileChooser.getSelectedFile();
				try {
					sourceInfo = new SourceDirectoryInfo(selected);
					informationText = String.format(
							"Selected directory is:%n%s", sourceInfo.sourceDir);
					informationText += String.format("%nTotal Images: %s",
							Integer.toString(sourceInfo.numberOfImages));
					informationText += String.format("%nNumber of pages: %s",
							sourceInfo.numberOfPages);
					progressBar.setMaximum(100);
					progressBar.setValue(0);
				} catch (IOException e1) {
					errorsArea.setText(String.format("%s%n%s",
							errorsArea.getText(), e1.getMessage()));
					e1.printStackTrace();
				}
				sourceInfoTextPane.setText(informationText);
			}
		} else if (actionCommand.equals(btnMakeBadgesActionCommand)) {
			// Make the badges, IF the source directory exists.
			if (sourceInfo == null) {
				errorsArea
						.setText("Please select a source directory before making badges.");
				return;
			}
			setControlsDisabled();
			errorsArea
					.setText("Any errors in creating the badges will appear here.");
			MakeBadgesDocument makeDoc = new MakeBadgesDocument(sourceInfo,
					detectBadgeTypeFromName, autoScalePictures);
			makeDoc.addPropertyChangeListener(this);
			makeDoc.execute();
		} else if (actionCommand.equals(btnChangeSettingsActionCommand)) {
			// Pop up a file picker
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int returnValue = fileChooser.showOpenDialog(this);
			if (returnValue == JFileChooser.APPROVE_OPTION) {
				try {
					File selected = fileChooser.getSelectedFile();
					// Get the InputStream
					FileInputStream propertiesFile = new FileInputStream(
							selected);

					// Force the PropertyFileReader to refresh
					PropertyFileReader.initialize(propertiesFile);
				} catch (Exception e2) {
					errorsArea.setText(String.format("%s%n%s",
							errorsArea.getText(), e2.getMessage()));
					e2.printStackTrace();
				}
			}
		} else if (actionCommand.equals(chckbxDetectBadgeTypeActionCommand)) {
			if (chckbxDetectBadgeType.isSelected()) {
				detectBadgeTypeFromName = true;
				rdbtnAdultLeader.setEnabled(false);
				rdbtnYouthCounselor.setEnabled(false);
				rdbtnSpecialNeedsYouth.setEnabled(false);
			} else {
				detectBadgeTypeFromName = false;
				rdbtnAdultLeader.setEnabled(true);
				rdbtnYouthCounselor.setEnabled(true);
				rdbtnSpecialNeedsYouth.setEnabled(true);
			}
		} else if (actionCommand.equals(chckbxAutoScaleActionCommand)) {
			autoScalePictures = chckbxAutoscalePictures.isSelected();
		} else if (actionCommand.equals(rdbtnAdultLeaderActionCommand)) {
			selectedOutputType = BadgeType.AdultLeader;
			rdbtnAdultLeader.setSelected(true);
			rdbtnYouthCounselor.setSelected(false);
			rdbtnSpecialNeedsYouth.setSelected(false);
		} else if (actionCommand.equals(rdbtnYouthCounselorActionCommand)) {
			selectedOutputType = BadgeType.YouthCounselor;
			rdbtnYouthCounselor.setSelected(true);
			rdbtnAdultLeader.setSelected(false);
			rdbtnSpecialNeedsYouth.setSelected(false);
		} else if (actionCommand.equals(rdbtnSpecialNeedsActionCommand)) {
			selectedOutputType = BadgeType.SpecialNeedsYouth;
			rdbtnSpecialNeedsYouth.setSelected(true);
			rdbtnAdultLeader.setSelected(false);
			rdbtnYouthCounselor.setSelected(false);
		}
	}

	/**
	 * Sets various controls to the 'disabled' state - use while building badges
	 */
	void setControlsDisabled() {
		btnChooseSourceDirectory.setEnabled(false);
		btnMakeBadges.setEnabled(false);
		btnChangeSettings.setEnabled(false);
		chckbxAutoscalePictures.setEnabled(false);
		chckbxDetectBadgeType.setEnabled(false);
		rdbtnAdultLeader.setEnabled(false);
		rdbtnYouthCounselor.setEnabled(false);
		rdbtnSpecialNeedsYouth.setEnabled(false);
	}

	/**
	 * Sets various controls to the 'enabled' state - use after badges are all
	 * built
	 */
	void setControlsEnabled() {
		btnChooseSourceDirectory.setEnabled(true);
		btnMakeBadges.setEnabled(true);
		btnChangeSettings.setEnabled(true);
		chckbxAutoscalePictures.setEnabled(true);
		chckbxDetectBadgeType.setEnabled(true);
		if (!chckbxDetectBadgeType.isSelected()) {
			rdbtnAdultLeader.setEnabled(true);
			rdbtnYouthCounselor.setEnabled(true);
			rdbtnSpecialNeedsYouth.setEnabled(true);
		}
	}

	/**
	 * The task which makes the badges Document
	 * 
	 * @author jacob.carter
	 * 
	 */
	class MakeBadgesDocument extends SwingWorker<PdfPTable, Object> {

		final SourceDirectoryInfo sourceInfo;
		boolean detectBadgeTypeFromName;
		boolean autoScalePictures;
		List<RenameOperation> renamesToPerform = new ArrayList<>();

		public MakeBadgesDocument(SourceDirectoryInfo sourceInfo,
				boolean detectBadgeTypeFromName, boolean autoScalePictures) {
			this.sourceInfo = sourceInfo;
			this.detectBadgeTypeFromName = detectBadgeTypeFromName;
			this.autoScalePictures = autoScalePictures;
		}

		/*
		 * Main task, invoked in background thread
		 */
		@Override
		protected PdfPTable doInBackground() throws Exception {
			int overallTableWidthPercent;
			int overallTableLeftColumnPercent;
			int overallTableRightColumnPercent;
			PdfPTable badgeTable;
			PdfPTable overallTable;
			SnmNameBadge nameBadge;

			// /
			// /Set progress to 0 at the start
			// /
			int progress = 0;
			setProgress(progress);

			// /
			// /Determine the progress interval / add / thing
			// /
			// The progress bar defaults to a max of 100, so do up to 99%
			int progressInterval = 99 / this.sourceInfo.numberOfImages;

			// /
			// /Read in the relevant properties constants
			// /
			overallTableWidthPercent = Integer.parseInt(PropertyFileReader
					.getProperty("overallTableWidthPercent", "100"));
			overallTableLeftColumnPercent = Integer.parseInt(PropertyFileReader
					.getProperty("overallTableLeftColumnPercent", "50"));
			overallTableRightColumnPercent = Integer
					.parseInt(PropertyFileReader.getProperty(
							"overallTableRightColumnPercent", "50"));

			// /
			// /Create the overall table and prepare it to have child cells
			// added
			// /
			overallTable = new PdfPTable(new float[] {
					overallTableLeftColumnPercent,
					overallTableRightColumnPercent });
			overallTable.setWidthPercentage(overallTableWidthPercent);
			overallTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

			int length = this.sourceInfo.childFiles.length;
			for (int i = 0; i < length; i++) {
				// Iterate the old fashioned way
				nameBadge = new SnmNameBadge(this.sourceInfo.sourceDir,
						this.sourceInfo.childFiles[i],
						this.detectBadgeTypeFromName, this.autoScalePictures);
				if (!detectBadgeTypeFromName) {
					// Get the color based on which radio button is selected
					if (rdbtnAdultLeader.isSelected()) {
						nameBadge.setBadgeBackgroundColor(BadgeType.AdultLeader
								.getColor());
					} else if (rdbtnYouthCounselor.isSelected()) {
						nameBadge
								.setBadgeBackgroundColor(BadgeType.YouthCounselor
										.getColor());
					} else {
						nameBadge
								.setBadgeBackgroundColor(BadgeType.SpecialNeedsYouth
										.getColor());
					}
				}
				badgeTable = nameBadge.getBadgeTable();
				if (this.detectBadgeTypeFromName) {
					RenameOperation op = new RenameOperation();
					op.preRename = nameBadge.getPreRenameFile();
					op.postRename = nameBadge.getPostRenameFile();
					this.renamesToPerform.add(op);
				}

				overallTable.addCell(badgeTable);
				overallTable.addCell(badgeTable);
				progress = progress + progressInterval;
				setProgress(progress);
			}

			return overallTable;
		}

		/*
		 * Executed in event dispatching thread
		 */
		@Override
		public void done() {
			Document badgesDoc;
			PdfPTable badgesTable;
			PdfWriter writer;

			try (FileOutputStream outputStream = new FileOutputStream(
					sourceInfo.sourceDir + "/badgesPdf.pdf")) {
				badgesTable = get();
				badgesDoc = new Document(PageSize.LETTER);
				writer = PdfWriter.getInstance(badgesDoc, outputStream);
				badgesDoc.open();
				badgesDoc.add(badgesTable);
				badgesDoc.close();
				writer.flush();
				writer.close();

				// Now, rename all the badges which were used to create the PDF
				for (RenameOperation op : renamesToPerform) {
					op.preRename.renameTo(op.postRename);
				}

			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				throw new IllegalStateException(
						"Could not get the badges Document even after it is finished being created!",
						e);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new IllegalStateException("File not found exception!", e);
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalStateException("IOException!", e);
			} catch (DocumentException e) {
				e.printStackTrace();
				throw new IllegalStateException("DocumentException!", e);
			} finally {
				setControlsEnabled();
				progressBar.setValue(100);
			}
		}

	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals("progress")) {
			if (event.getNewValue() instanceof Integer) {
				int progress = (int) event.getNewValue();
				progressBar.setValue(progress);
			}
		}
	}

	/**
	 * Used only for renaming Files in batches after the badge creation process
	 * is complete.
	 * 
	 * @author Jacob
	 * 
	 */
	class RenameOperation {

		/**
		 * The File as it exists now
		 */
		public File preRename;
		/**
		 * The File you want to rename to
		 */
		public File postRename;

	}
}
