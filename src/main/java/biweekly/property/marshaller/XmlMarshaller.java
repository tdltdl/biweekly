package biweekly.property.marshaller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.OutputKeys;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import biweekly.io.CannotParseException;
import biweekly.io.xml.XCalElement;
import biweekly.io.xml.XCalNamespaceContext;
import biweekly.parameter.ICalParameters;
import biweekly.property.Xml;
import biweekly.util.XmlUtils;

/*
 Copyright (c) 2013, Michael Angstadt
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met: 

 1. Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer. 
 2. Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution. 

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Marshals {@link Xml} properties.
 * @author Michael Angstadt
 */
public class XmlMarshaller extends ICalPropertyMarshaller<Xml> {
	//TODO on writing to plain text: convert to base64 if the string contains values that are illegal within a plain text value (p.17)
	public XmlMarshaller() {
		super(Xml.class, "XML");
	}

	@Override
	protected String _writeText(Xml property) {
		Document value = property.getValue();
		if (value == null) {
			return "";
		}

		Map<String, String> props = new HashMap<String, String>();
		props.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
		String xml = XmlUtils.toString(value, props);
		return escape(xml);
	}

	@Override
	protected Xml _parseText(String value, ICalParameters parameters, List<String> warnings) {
		value = unescape(value);
		try {
			return new Xml(value);
		} catch (SAXException e) {
			throw new CannotParseException("Cannot parse value as XML: " + value);
		}
	}

	@Override
	protected void _writeXml(Xml property, XCalElement element) {
		super._writeXml(property, element);
		//Xml properties are handled as a special case when writing xCal documents, so this method should never get called (see: XCalDocument class)
	}

	@Override
	protected Xml _parseXml(XCalElement element, ICalParameters parameters, List<String> warnings) {
		Xml xml = new Xml(element.getElement());

		//remove the <parameters> element
		Element root = XmlUtils.getRootElement(xml.getValue());
		for (Element child : XmlUtils.toElementList(root.getChildNodes())) {
			if ("parameters".equals(child.getLocalName()) && XCalNamespaceContext.XCAL_NS.equals(child.getNamespaceURI())) {
				root.removeChild(child);
			}
		}

		return xml;
	}
}