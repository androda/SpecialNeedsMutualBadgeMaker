package com.specialneedsmutual.badgemaker.namebadge;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.html.WebColors;
import com.specialneedsmutual.badgemaker.utilities.PropertyFileReader;

/**
 * Enum which represents the various kinds of badges that can be made
 * 
 * @author Jacob
 * 
 */
public enum BadgeType {

	AdultLeader, YouthCounselor, SpecialNeedsYouth

	;

	/**
	 * Gets the color that this type of badge is supposed to be.
	 * 
	 * @return The BaseColor corresponding with the badge type.
	 */
	public BaseColor getColor() {
		switch (this) {
		case AdultLeader:
			return WebColors.getRGBColor(PropertyFileReader.getProperty(
					"badgeBackgroundColorAdultLeader", "#FFFFFF"));
		case YouthCounselor:
			return WebColors.getRGBColor(PropertyFileReader.getProperty(
					"badgeBackgroundColorYouthCounselor", "#FFFA83"));
		case SpecialNeedsYouth:
			return WebColors.getRGBColor(PropertyFileReader.getProperty(
					"badgeBackgroundColorSpecialNeedsYouth", "#CADBFE"));
		}
		return null;
	}
}
