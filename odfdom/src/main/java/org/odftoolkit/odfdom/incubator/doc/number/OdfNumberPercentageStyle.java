/************************************************************************
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 *
 * Copyright 2008, 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0. You can also
 * obtain a copy of the License at http://odftoolkit.org/docs/license.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ************************************************************************/
package org.odftoolkit.odfdom.incubator.doc.number;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.odftoolkit.odfdom.pkg.OdfElement;
import org.odftoolkit.odfdom.pkg.OdfFileDom;
import org.odftoolkit.odfdom.dom.element.number.NumberNumberElement;
import org.odftoolkit.odfdom.dom.element.number.NumberPercentageStyleElement;
import org.odftoolkit.odfdom.dom.element.number.NumberTextElement;
import org.odftoolkit.odfdom.dom.element.style.StyleMapElement;
import org.w3c.dom.Node;

/**
 * Convenient functionalty for the parent ODF OpenDocument element
 *
 */
public class OdfNumberPercentageStyle extends NumberPercentageStyleElement {

	public OdfNumberPercentageStyle(OdfFileDom ownerDoc) {
		super(ownerDoc);
	}

	/**
	 * Creates a new instance of OdfNumberPercentageStyle.
	 * @param ownerDoc document that this format belongs to
	 * @param format format string for the date/time
	 * @param styleName name of this style
	 */
	public OdfNumberPercentageStyle(OdfFileDom ownerDoc, String format, String styleName) {
		super(ownerDoc);
		this.setStyleNameAttribute(styleName);
		buildFromFormat(format);
	}

	/**
	 * Get the format string that represents this style.
	 * @return the format string
	 */
	public String getFormat() {
		String result = "";
		Node m = getFirstChild();
		while (m != null) {
			if (m instanceof NumberNumberElement) {
				result += getNumberFormat();
			} else if (m instanceof NumberTextElement) {
				String textcontent = m.getTextContent();
				if (textcontent == null || textcontent.length() == 0) {
					textcontent = " ";
				}
				result += textcontent;
			}
			m = m.getNextSibling();
		}
		return result;
	}

	public String getNumberFormat() {
		String result = "";
		NumberNumberElement number = OdfElement.findFirstChildNode(NumberNumberElement.class, this);
		boolean isGroup = number.getNumberGroupingAttribute();
		int decimalPos = (number.getNumberDecimalPlacesAttribute() == null) ? 0
				: number.getNumberDecimalPlacesAttribute().intValue();
		int minInt = (number.getNumberMinIntegerDigitsAttribute() == null) ? 1
				: number.getNumberMinIntegerDigitsAttribute().intValue();

		int i;
		for (i = 0; i < minInt; i++) {
			if (((i + 1) % 3) == 0 && isGroup) {
				result = ",0" + result;
			} else {
				result = "0" + result;
			}
		}
		while (isGroup && (result.indexOf(',') == -1)) {
			if (((i + 1) % 3) == 0 && isGroup) {
				result = ",#" + result;
			} else {
				result = "#" + result;
			}
			i++;
		}

		result = "#" + result;
		if (decimalPos > 0) {
			result += ".";
			for (i = 0; i < decimalPos; i++) {
				result += "0";
			}
		}
		return result;
	}

	/**
	 * Creates a &lt;number:number-style&gt; element based upon format.
	 * @param format the number format string
	 */
	public void buildFromFormat(String format) {
		/*
		 * Setting ownerDoc won't be necessary once this is folded into
		 * OdfNumberStyle
		 */
		String preMatch;
		String numberSpec;
		String postMatch;
		int pos;
		char ch;
		int nDigits;

		Pattern p = Pattern.compile("[#0,.]+");
		Matcher m;
		NumberNumberElement number;

		/*
		 * If there is a numeric specifcation, then split the
		 * string into the part before the specifier, the specifier
		 * itself, and then part after the specifier. The parts
		 * before and after are just text (which may contain the
		 * currency symbol).
		 */
		if (format != null && !format.equals("")) {
			m = p.matcher(format);
			if (m.find()) {
				preMatch = format.substring(0, m.start());
				numberSpec = format.substring(m.start(), m.end());
				postMatch = format.substring(m.end());

				emitText(preMatch);

				number = new NumberNumberElement((OdfFileDom) this.getOwnerDocument());

				/* Process part before the decimal point (if any) */
				nDigits = 0;
				for (pos = 0; pos < numberSpec.length()
						&& (ch = numberSpec.charAt(pos)) != '.'; pos++) {
					if (ch == ',') {
						number.setNumberGroupingAttribute(new Boolean(true));
					} else if (ch == '0') {
						nDigits++;
					}
				}
				number.setNumberMinIntegerDigitsAttribute(nDigits);

				/* Number of decimal places is the length after the decimal */
				if (pos < numberSpec.length()) {
					number.setNumberDecimalPlacesAttribute(numberSpec.length() - (pos + 1));
				}
				this.appendChild(number);

				emitText(postMatch);
			}
		}
	}

	/**
	 *	Place pending text into a &lt;number:text&gt; element.
	 * @param textBuffer pending text
	 */
	private void emitText(String textBuffer) {
		NumberTextElement textElement;
		if (!textBuffer.equals("")) {
			textElement = new NumberTextElement((OdfFileDom) this.getOwnerDocument());
			textElement.setTextContent(textBuffer);
			this.appendChild(textElement);
		}
	}

	/**
	 * Set &lt;style:map&gt; for positive values to the given style name.
	 * @param mapName the style name to map to
	 */
	public void setMapPositive(String mapName) {
		StyleMapElement map = new StyleMapElement((OdfFileDom) this.getOwnerDocument());
		map.setStyleApplyStyleNameAttribute(mapName);
		map.setStyleConditionAttribute("value()>0");
		this.appendChild(map);
	}

	/**
	 * Set &lt;style:map&gt; for negative values to the given style name.
	 * @param mapName the style name to map to
	 */
	public void setMapNegative(String mapName) {
		StyleMapElement map = new StyleMapElement((OdfFileDom) this.getOwnerDocument());
		map.setStyleApplyStyleNameAttribute(mapName);
		map.setStyleConditionAttribute("value()<0");
		this.appendChild(map);
	}
}
